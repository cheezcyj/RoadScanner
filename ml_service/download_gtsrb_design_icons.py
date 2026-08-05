"""Download the 43 canonical GTSRB-aligned sign designs for manual validation.

The first 43 classes published by Synset Signset Germany intentionally use the
same numeric IDs as GTSRB. The source PNGs are retained byte-for-byte in an
``original-transparent`` directory. The recommended ``upload-ready-transparent``
variant is centred on a transparent square RGBA canvas whose fully transparent
pixels have white hidden RGB, so Pillow RGB conversion cannot turn the background
black. A white RGB compatibility variant is retained separately. Every upstream
byte payload is checked against a tracked source lock, and a complete staged
generation replaces the destination only after all 43 classes validate.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import shutil
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from io import BytesIO
from pathlib import Path
from typing import Any
from urllib.parse import urljoin, urlsplit

import requests
from PIL import Image, ImageOps
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry


DATASET_NAME = "Synset Signset Germany"
DATASET_URL = "https://synset.de/datasets/synset-signset-ger/"
ICON_URL_TEMPLATE = "https://synset.de/wp-content/uploads/2024/08/{class_id}.png"
ALLOWED_SOURCE_HOST = "synset.de"
LICENSE_NAME = "Creative Commons Attribution 4.0 International"
LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/"
CREATORS = [
    "Anne Sielemann",
    "Lena Loercher",
    "Max-Lion Schumacher",
    "Stefan Wolf",
    "Masoud Roschani",
    "Jens Ziehn",
    "Juergen Beyerer",
]
COPYRIGHT_NOTICE = "Copyright 2024 Fraunhofer IOSB. All rights reserved."

PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CLASS_MAP = PROJECT_ROOT / "ml_service" / "class_map.json"
DEFAULT_SOURCE_LOCK = (
    PROJECT_ROOT / "ml_service" / "gtsrb_design_icon_sources.lock.json"
)
DEFAULT_OUTPUT_DIR = (
    PROJECT_ROOT / "data" / "manual-validation" / "positive" / "gtsrb-design"
)
DEFAULT_MAX_SOURCE_BYTES = 8 * 1024 * 1024
ABSOLUTE_MAX_SOURCE_BYTES = 32 * 1024 * 1024
MIN_CANVAS_SIZE = 256
MAX_CANVAS_SIZE = 4096
MAX_REDIRECTS = 5
DOWNLOAD_CHUNK_SIZE = 64 * 1024
SHA256_PATTERN = re.compile(r"[0-9A-F]{64}")


@dataclass(frozen=True)
class GtsrbClass:
    class_id: int
    result_id: int
    key: str
    name_en: str
    de_sign_code: str


@dataclass(frozen=True)
class LockedSource:
    class_id: int
    source_url: str
    source_sha256: str
    source_image: dict[str, Any]


def _sha256(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest().upper()


def _load_classes(class_map_path: Path) -> list[GtsrbClass]:
    payload = json.loads(class_map_path.read_text(encoding="utf-8"))
    if payload.get("dataset") != "GTSRB" or payload.get("class_count") != 43:
        raise ValueError("class_map.json must describe the 43-class GTSRB catalog")

    raw_classes = payload.get("classes")
    if not isinstance(raw_classes, list) or len(raw_classes) != 43:
        raise ValueError("class_map.json must contain exactly 43 classes")

    classes = [
        GtsrbClass(
            class_id=int(item["class_id"]),
            result_id=int(item["result_id"]),
            key=str(item["key"]),
            name_en=str(item["name_en"]),
            de_sign_code=str(item["de_sign_code"]),
        )
        for item in raw_classes
    ]
    if [item.class_id for item in classes] != list(range(43)):
        raise ValueError("GTSRB class IDs must be contiguous and ordered from 0 to 42")
    if [item.result_id for item in classes] != list(range(1, 44)):
        raise ValueError("RoadScanner result IDs must be contiguous and ordered from 1 to 43")
    for item in classes:
        if re.fullmatch(r"[a-z0-9_]+", item.key) is None:
            raise ValueError(
                f"unsafe RoadScanner class key for class {item.class_id}: {item.key!r}"
            )
    return classes


def _validate_source_url(source_url: str) -> str:
    parsed = urlsplit(source_url)
    if (
        parsed.scheme != "https"
        or parsed.hostname != ALLOWED_SOURCE_HOST
        or parsed.username is not None
        or parsed.password is not None
        or parsed.port not in (None, 443)
        or parsed.fragment
    ):
        raise ValueError(
            "source URL must use HTTPS on the approved synset.de host without "
            f"credentials, a custom port, or a fragment: {source_url!r}"
        )
    return source_url


def _load_source_lock(
    source_lock_path: Path, classes: list[GtsrbClass]
) -> list[LockedSource]:
    payload = json.loads(source_lock_path.read_text(encoding="utf-8"))
    if payload.get("schema_version") != 1:
        raise ValueError("source lock must use schema_version 1")

    dataset = payload.get("dataset")
    if not isinstance(dataset, dict) or dataset.get("name") != DATASET_NAME:
        raise ValueError(f"source lock must describe {DATASET_NAME}")

    raw_sources = payload.get("sources")
    if not isinstance(raw_sources, list) or len(raw_sources) != len(classes):
        raise ValueError("source lock must contain exactly 43 sources")

    locked_sources: list[LockedSource] = []
    for expected_class, item in zip(classes, raw_sources):
        if not isinstance(item, dict):
            raise ValueError("each source lock entry must be an object")
        class_id = int(item["class_id"])
        if class_id != expected_class.class_id:
            raise ValueError("source lock class IDs must be contiguous and ordered from 0 to 42")

        source_url = _validate_source_url(str(item["source_url"]))
        expected_url = ICON_URL_TEMPLATE.format(class_id=class_id)
        if source_url != expected_url:
            raise ValueError(
                f"source URL for class {class_id} must be pinned to {expected_url}"
            )

        source_sha256 = str(item["source_sha256"])
        if SHA256_PATTERN.fullmatch(source_sha256) is None:
            raise ValueError(f"invalid uppercase SHA-256 for class {class_id}")

        source_image = item.get("source_image")
        if not isinstance(source_image, dict):
            raise ValueError(f"missing source image metadata for class {class_id}")
        normalized_image = {
            "format": str(source_image.get("format")),
            "mode": str(source_image.get("mode")),
            "width": int(source_image.get("width")),
            "height": int(source_image.get("height")),
        }
        if normalized_image["format"] != "PNG" or normalized_image["mode"] != "RGBA":
            raise ValueError(f"class {class_id} must be pinned as a transparent RGBA PNG")
        if (
            min(normalized_image["width"], normalized_image["height"]) < 256
            or max(normalized_image["width"], normalized_image["height"]) > 4096
        ):
            raise ValueError(f"invalid locked dimensions for class {class_id}")

        locked_sources.append(
            LockedSource(
                class_id=class_id,
                source_url=source_url,
                source_sha256=source_sha256,
                source_image=normalized_image,
            )
        )
    return locked_sources


def _validate_download_options(
    canvas_size: int, timeout_seconds: float, max_source_bytes: int
) -> None:
    if (
        isinstance(canvas_size, bool)
        or not isinstance(canvas_size, int)
        or not MIN_CANVAS_SIZE <= canvas_size <= MAX_CANVAS_SIZE
    ):
        raise ValueError(
            f"canvas_size must be an integer from {MIN_CANVAS_SIZE} to {MAX_CANVAS_SIZE}"
        )
    if (
        isinstance(timeout_seconds, bool)
        or not isinstance(timeout_seconds, (int, float))
        or not math.isfinite(timeout_seconds)
        or timeout_seconds <= 0
    ):
        raise ValueError("timeout_seconds must be a finite positive number")
    if (
        isinstance(max_source_bytes, bool)
        or not isinstance(max_source_bytes, int)
        or not 1 <= max_source_bytes <= ABSOLUTE_MAX_SOURCE_BYTES
    ):
        raise ValueError(
            "max_source_bytes must be a positive integer no greater than "
            f"{ABSOLUTE_MAX_SOURCE_BYTES}"
        )


def _build_session() -> requests.Session:
    retry = Retry(
        total=4,
        connect=4,
        read=4,
        status=4,
        backoff_factor=0.5,
        status_forcelist=(429, 500, 502, 503, 504),
        allowed_methods=frozenset({"GET"}),
    )
    adapter = HTTPAdapter(max_retries=retry)
    session = requests.Session()
    session.headers.update(
        {
            "User-Agent": (
                "RoadScanner-GTSRB-design-downloader/1.0 "
                "(manual validation; source attribution retained)"
            )
        }
    )
    session.mount("https://", adapter)
    return session


def _read_limited_response(response: requests.Response, max_source_bytes: int) -> bytes:
    content_length = response.headers.get("Content-Length")
    if content_length is not None:
        try:
            declared_length = int(content_length)
        except ValueError as error:
            raise ValueError("source response has an invalid Content-Length") from error
        if declared_length < 0 or declared_length > max_source_bytes:
            raise ValueError(
                f"source response exceeds the {max_source_bytes}-byte download limit"
            )

    payload = bytearray()
    for chunk in response.iter_content(chunk_size=DOWNLOAD_CHUNK_SIZE):
        if not chunk:
            continue
        payload.extend(chunk)
        if len(payload) > max_source_bytes:
            raise ValueError(
                f"source response exceeds the {max_source_bytes}-byte download limit"
            )
    return bytes(payload)


def _download_source(
    session: requests.Session,
    source_url: str,
    *,
    timeout_seconds: float,
    max_source_bytes: int,
) -> bytes:
    current_url = _validate_source_url(source_url)
    for redirect_count in range(MAX_REDIRECTS + 1):
        response = session.get(
            current_url,
            timeout=timeout_seconds,
            stream=True,
            allow_redirects=False,
        )
        try:
            if response.status_code in {301, 302, 303, 307, 308}:
                if redirect_count == MAX_REDIRECTS:
                    raise ValueError(f"source URL exceeded {MAX_REDIRECTS} redirects")
                location = response.headers.get("Location")
                if not location:
                    raise ValueError("source redirect is missing a Location header")
                current_url = _validate_source_url(urljoin(current_url, location))
                continue

            response.raise_for_status()
            return _read_limited_response(response, max_source_bytes)
        finally:
            response.close()

    raise AssertionError("redirect loop terminated unexpectedly")


def _normalize_fully_transparent_rgb(image: Image.Image) -> Image.Image:
    if image.mode != "RGBA":
        raise ValueError("transparent upload-ready image must use RGBA mode")
    red, green, blue, alpha = image.split()
    fully_transparent = alpha.point(lambda value: 255 if value == 0 else 0)
    for channel in (red, green, blue):
        channel.paste(255, mask=fully_transparent)
    return Image.merge("RGBA", (red, green, blue, alpha))


def _decode_and_normalize(
    source_bytes: bytes, canvas_size: int
) -> tuple[Image.Image, Image.Image, dict[str, Any]]:
    if (
        isinstance(canvas_size, bool)
        or not isinstance(canvas_size, int)
        or not MIN_CANVAS_SIZE <= canvas_size <= MAX_CANVAS_SIZE
    ):
        raise ValueError(
            f"canvas_size must be an integer from {MIN_CANVAS_SIZE} to {MAX_CANVAS_SIZE}"
        )
    with Image.open(BytesIO(source_bytes)) as source:
        if source.format != "PNG":
            raise ValueError(f"expected PNG source, got {source.format!r}")
        if min(source.size) < 256 or max(source.size) > 4096:
            raise ValueError(f"unexpected source dimensions: {source.size}")
        source.load()

        source_metadata = {
            "format": source.format,
            "mode": source.mode,
            "width": source.width,
            "height": source.height,
        }
        rgba = source.convert("RGBA")
        alpha_minimum, _ = rgba.getchannel("A").getextrema()
        if alpha_minimum >= 255:
            raise ValueError("source PNG must contain actual transparent pixels")

    contained = ImageOps.contain(
        rgba,
        (canvas_size, canvas_size),
        method=Image.Resampling.LANCZOS,
    )
    transparent_canvas = Image.new(
        "RGBA", (canvas_size, canvas_size), (255, 255, 255, 0)
    )
    offset = (
        (canvas_size - contained.width) // 2,
        (canvas_size - contained.height) // 2,
    )
    transparent_canvas.paste(contained, box=offset)
    transparent_canvas = _normalize_fully_transparent_rgb(transparent_canvas)

    white_canvas = Image.new("RGBA", (canvas_size, canvas_size), (255, 255, 255, 255))
    white_canvas.alpha_composite(transparent_canvas)
    return transparent_canvas, white_canvas.convert("RGB"), source_metadata


def _decode_and_flatten(
    source_bytes: bytes, canvas_size: int
) -> tuple[Image.Image, dict[str, Any]]:
    _, white_canvas, source_metadata = _decode_and_normalize(
        source_bytes, canvas_size
    )
    return white_canvas, source_metadata


def _encode_png(image: Image.Image) -> bytes:
    output = BytesIO()
    image.save(output, format="PNG", optimize=True)
    return output.getvalue()


def _attribution_text(canvas_size: int) -> str:
    creators = ", ".join(CREATORS[:-1]) + f", and {CREATORS[-1]}"
    return f"""# GTSRB design icon attribution

The 43 sign designs in this directory are based on **{DATASET_NAME}** by
{creators}.

- Dataset: {DATASET_URL}
- Copyright: {COPYRIGHT_NOTICE}
- License: {LICENSE_NAME} ({LICENSE_URL})
- Source class contract: the dataset page states that class IDs `0` through
  `42` directly match the GTSRB traffic-sign IDs.
- `original-transparent`: source PNG bytes preserved unchanged; only filenames
  were changed to include the RoadScanner class ID and key.
- `upload-ready-transparent` (recommended for uploads and regression): source
  RGBA pixels were downscaled as one RGBA image with LANCZOS only when necessary,
  then copied without alpha compositing to the centre of a transparent
  {canvas_size} x {canvas_size} RGBA canvas. Unscaled foreground RGBA is therefore
  byte-identical to the source; when scaling is needed, LANCZOS is the only
  foreground/alpha resampling step. Fully transparent pixels use hidden RGB
  `(255, 255, 255)` so RGB-only decoding produces a white, not black, background.
- `upload-ready-white` (compatibility): the normalized transparent variant was
  alpha-composited onto white and converted to RGB. The sign artwork itself was
  not redrawn.
- Disclaimer: the original authors are neither affiliated with nor responsible
  for these changes or for RoadScanner.
"""


def _tree_members(root: Path) -> tuple[set[Path], set[Path]]:
    directories: set[Path] = set()
    files: set[Path] = set()
    for path in root.rglob("*"):
        if path.is_symlink():
            raise ValueError(f"generated output trees must not contain symlinks: {path}")
        relative_path = path.relative_to(root)
        if path.is_dir():
            directories.add(relative_path)
        elif path.is_file():
            files.add(relative_path)
        else:
            raise ValueError(f"unsupported filesystem entry in generated output: {path}")
    return directories, files


def _replace_or_copy_file(source_path: Path, destination_path: Path) -> None:
    temporary_path = destination_path.with_name(
        f"{destination_path.name}.commit-{uuid.uuid4().hex}.tmp"
    )
    try:
        shutil.copy2(source_path, temporary_path)
        try:
            temporary_path.replace(destination_path)
        except OSError:
            shutil.copy2(source_path, destination_path)
    finally:
        temporary_path.unlink(missing_ok=True)


def _synchronize_directory_tree(source_dir: Path, destination_dir: Path) -> None:
    source_directories, source_files = _tree_members(source_dir)
    destination_dir.mkdir(parents=True, exist_ok=True)
    if destination_dir.is_symlink():
        raise ValueError("destination directory must not be a symbolic link")
    _tree_members(destination_dir)

    for relative_path in sorted(source_directories, key=lambda path: len(path.parts)):
        destination_path = destination_dir / relative_path
        if destination_path.exists() and not destination_path.is_dir():
            destination_path.unlink()
        destination_path.mkdir(parents=True, exist_ok=True)

    for relative_path in source_files:
        destination_path = destination_dir / relative_path
        if destination_path.exists() and destination_path.is_dir():
            shutil.rmtree(destination_path)
        destination_path.parent.mkdir(parents=True, exist_ok=True)

    manifest_path = Path("manifest.json")
    regular_files = sorted(source_files - {manifest_path}, key=lambda path: path.as_posix())
    for relative_path in regular_files:
        _replace_or_copy_file(source_dir / relative_path, destination_dir / relative_path)

    current_directories, current_files = _tree_members(destination_dir)
    for relative_path in sorted(
        current_files - source_files,
        key=lambda path: (len(path.parts), path.as_posix()),
        reverse=True,
    ):
        (destination_dir / relative_path).unlink()
    for relative_path in sorted(
        current_directories - source_directories,
        key=lambda path: (len(path.parts), path.as_posix()),
        reverse=True,
    ):
        stale_path = destination_dir / relative_path
        if stale_path.exists():
            shutil.rmtree(stale_path)

    if manifest_path in source_files:
        _replace_or_copy_file(
            source_dir / manifest_path, destination_dir / manifest_path
        )

    final_directories, final_files = _tree_members(destination_dir)
    if final_directories != source_directories or final_files != source_files:
        raise RuntimeError("file-by-file commit produced an unexpected output tree")
    for relative_path in source_files:
        if _sha256((source_dir / relative_path).read_bytes()) != _sha256(
            (destination_dir / relative_path).read_bytes()
        ):
            raise RuntimeError(f"file-by-file commit verification failed: {relative_path}")


def _rename_directory(source_dir: Path, destination_dir: Path) -> None:
    source_dir.replace(destination_dir)


def _commit_staged_files_with_rollback(staging_dir: Path, output_dir: Path) -> None:
    backup_dir = output_dir.with_name(
        f"{output_dir.name}-backup-{uuid.uuid4().hex}"
    )
    _tree_members(output_dir)
    try:
        shutil.copytree(output_dir, backup_dir, copy_function=shutil.copy2)
    except BaseException:
        shutil.rmtree(backup_dir, ignore_errors=True)
        raise

    try:
        _synchronize_directory_tree(staging_dir, output_dir)
    except BaseException as commit_error:
        try:
            _synchronize_directory_tree(backup_dir, output_dir)
        except BaseException as rollback_error:
            raise RuntimeError(
                "file-by-file commit failed and automatic rollback also failed; "
                f"the complete backup remains at {backup_dir}"
            ) from rollback_error
        shutil.rmtree(backup_dir, ignore_errors=True)
        raise commit_error
    shutil.rmtree(backup_dir, ignore_errors=True)


def _commit_staged_directory(staging_dir: Path, output_dir: Path) -> None:
    if output_dir.is_symlink():
        raise ValueError("output_dir must not be a symbolic link")
    if not output_dir.exists():
        staging_dir.replace(output_dir)
        return
    if not output_dir.is_dir():
        raise ValueError("output_dir must be a directory")

    backup_dir = output_dir.with_name(
        f"{output_dir.name}-backup-{uuid.uuid4().hex}"
    )
    try:
        _rename_directory(output_dir, backup_dir)
    except OSError:
        _commit_staged_files_with_rollback(staging_dir, output_dir)
        return
    try:
        _rename_directory(staging_dir, output_dir)
    except BaseException:
        _rename_directory(backup_dir, output_dir)
        raise
    shutil.rmtree(backup_dir)


def _create_staging_directory(output_dir: Path) -> Path:
    for _ in range(10):
        staging_dir = output_dir.with_name(
            f"{output_dir.name}-staging-{uuid.uuid4().hex}"
        )
        try:
            staging_dir.mkdir()
        except FileExistsError:
            continue
        return staging_dir
    raise RuntimeError("could not allocate a unique staging directory")


def download_icons(
    *,
    class_map_path: Path,
    output_dir: Path,
    canvas_size: int,
    timeout_seconds: float,
    source_lock_path: Path = DEFAULT_SOURCE_LOCK,
    max_source_bytes: int = DEFAULT_MAX_SOURCE_BYTES,
) -> dict[str, Any]:
    _validate_download_options(canvas_size, timeout_seconds, max_source_bytes)
    classes = _load_classes(class_map_path)
    locked_sources = _load_source_lock(source_lock_path, classes)
    source_lock_sha256 = _sha256(source_lock_path.read_bytes())

    output_dir = output_dir.absolute()
    output_dir.parent.mkdir(parents=True, exist_ok=True)
    staging_dir = _create_staging_directory(output_dir)
    original_dir = staging_dir / "original-transparent"
    upload_ready_transparent_dir = staging_dir / "upload-ready-transparent"
    upload_ready_white_dir = staging_dir / "upload-ready-white"
    try:
        original_dir.mkdir()
        upload_ready_transparent_dir.mkdir()
        upload_ready_white_dir.mkdir()
    except BaseException:
        shutil.rmtree(staging_dir, ignore_errors=True)
        raise
    entries: list[dict[str, Any]] = []

    try:
        session = _build_session()
        try:
            for item, locked_source in zip(classes, locked_sources):
                source_bytes = _download_source(
                    session,
                    locked_source.source_url,
                    timeout_seconds=timeout_seconds,
                    max_source_bytes=max_source_bytes,
                )
                source_sha256 = _sha256(source_bytes)
                if source_sha256 != locked_source.source_sha256:
                    raise ValueError(
                        f"source SHA-256 mismatch for class {item.class_id}: "
                        f"expected {locked_source.source_sha256}, got {source_sha256}"
                    )

                (
                    transparent_image,
                    white_image,
                    source_metadata,
                ) = _decode_and_normalize(source_bytes, canvas_size)
                if source_metadata != locked_source.source_image:
                    raise ValueError(
                        f"source image metadata mismatch for class {item.class_id}: "
                        f"expected {locked_source.source_image}, got {source_metadata}"
                    )

                transparent_bytes = _encode_png(transparent_image)
                white_bytes = _encode_png(white_image)
                filename = f"gtsrb_{item.class_id:02d}_{item.key}.png"
                (original_dir / filename).write_bytes(source_bytes)
                (upload_ready_transparent_dir / filename).write_bytes(
                    transparent_bytes
                )
                (upload_ready_white_dir / filename).write_bytes(white_bytes)

                entries.append(
                    {
                        "class_id": item.class_id,
                        "result_id": item.result_id,
                        "key": item.key,
                        "name_en": item.name_en,
                        "de_sign_code": item.de_sign_code,
                        "original_relative_path": f"original-transparent/{filename}",
                        "recommended_upload_relative_path": (
                            f"upload-ready-transparent/{filename}"
                        ),
                        "upload_ready_transparent_relative_path": (
                            f"upload-ready-transparent/{filename}"
                        ),
                        "upload_ready_transparent_sha256": _sha256(
                            transparent_bytes
                        ),
                        "upload_ready_transparent_image": {
                            "format": "PNG",
                            "mode": "RGBA",
                            "width": canvas_size,
                            "height": canvas_size,
                            "fully_transparent_rgb": [255, 255, 255],
                        },
                        "upload_ready_white_relative_path": (
                            f"upload-ready-white/{filename}"
                        ),
                        "upload_ready_white_sha256": _sha256(white_bytes),
                        "upload_ready_white_image": {
                            "format": "PNG",
                            "mode": "RGB",
                            "width": canvas_size,
                            "height": canvas_size,
                        },
                        # Backward-compatible aliases for the original white variant.
                        "upload_ready_relative_path": f"upload-ready-white/{filename}",
                        "source_url": locked_source.source_url,
                        "source_sha256": source_sha256,
                        "source_image": source_metadata,
                        "output_sha256": _sha256(white_bytes),
                        "output_image": {
                            "format": "PNG",
                            "mode": "RGB",
                            "width": canvas_size,
                            "height": canvas_size,
                        },
                    }
                )
        finally:
            session.close()

        manifest = {
            "schema_version": 1,
            "generated_at_utc": datetime.now(timezone.utc).isoformat(),
            "dataset": {
                "name": DATASET_NAME,
                "url": DATASET_URL,
                "license": LICENSE_NAME,
                "license_url": LICENSE_URL,
                "creators": CREATORS,
                "copyright": COPYRIGHT_NOTICE,
            },
            "source_lock": {
                "filename": source_lock_path.name,
                "sha256": source_lock_sha256,
            },
            "class_contract": (
                "Synset Signset Germany IDs 0-42 directly match GTSRB IDs 0-42."
            ),
            "recommended_variant": "upload-ready-transparent",
            "variants": {
                "original-transparent": {
                    "role": "byte-preserved source",
                    "file_count": len(entries),
                },
                "upload-ready-transparent": {
                    "role": "recommended upload and regression input",
                    "file_count": len(entries),
                    "format": "PNG",
                    "mode": "RGBA",
                    "width": canvas_size,
                    "height": canvas_size,
                    "fully_transparent_rgb": [255, 255, 255],
                },
                "upload-ready-white": {
                    "role": "RGB compatibility derivative",
                    "file_count": len(entries),
                    "format": "PNG",
                    "mode": "RGB",
                    "width": canvas_size,
                    "height": canvas_size,
                },
            },
            "transformation": (
                "Original source bytes retained under original-transparent. The "
                "recommended upload-ready-transparent copy preserves aspect ratio, "
                "uses LANCZOS as the only RGBA resampling step when downscaling, and "
                f"copies the result without compositing to the centre of a transparent "
                f"{canvas_size} x {canvas_size} RGBA canvas. Unscaled foreground RGBA "
                "is byte-identical to the source. Fully transparent hidden RGB is "
                "normalized to (255, 255, 255). The "
                "upload-ready-white compatibility copy alpha-composites that normalized "
                "variant onto white and converts it to RGB."
            ),
            "canvas_size": canvas_size,
            "image_count": len(entries),
            "png_file_count": len(entries) * 3,
            "images": entries,
        }
        (staging_dir / "manifest.json").write_text(
            json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        (staging_dir / "ATTRIBUTION.md").write_text(
            _attribution_text(canvas_size), encoding="utf-8"
        )

        expected_filenames = {
            f"gtsrb_{item.class_id:02d}_{item.key}.png" for item in classes
        }
        if {path.name for path in original_dir.glob("*.png")} != expected_filenames:
            raise RuntimeError("staged transparent source file set is incomplete")
        if {
            path.name for path in upload_ready_transparent_dir.glob("*.png")
        } != expected_filenames:
            raise RuntimeError("staged transparent upload-ready file set is incomplete")
        if {
            path.name for path in upload_ready_white_dir.glob("*.png")
        } != expected_filenames:
            raise RuntimeError("staged white upload-ready file set is incomplete")

        _commit_staged_directory(staging_dir, output_dir)
        return manifest
    finally:
        if staging_dir.exists():
            shutil.rmtree(staging_dir, ignore_errors=True)


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Download and normalize the 43 GTSRB-aligned Synset sign designs."
    )
    parser.add_argument("--class-map", type=Path, default=DEFAULT_CLASS_MAP)
    parser.add_argument("--source-lock", type=Path, default=DEFAULT_SOURCE_LOCK)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--canvas-size", type=int, default=1024)
    parser.add_argument("--timeout", type=float, default=30.0)
    parser.add_argument(
        "--max-source-bytes", type=int, default=DEFAULT_MAX_SOURCE_BYTES
    )
    return parser.parse_args()


def main() -> None:
    args = _parse_args()
    manifest = download_icons(
        class_map_path=args.class_map.resolve(),
        source_lock_path=args.source_lock.resolve(),
        output_dir=args.output_dir.absolute(),
        canvas_size=args.canvas_size,
        timeout_seconds=args.timeout,
        max_source_bytes=args.max_source_bytes,
    )
    print(
        f"Downloaded {manifest['image_count']} GTSRB design icons to "
        f"{args.output_dir.resolve()}"
    )


if __name__ == "__main__":
    main()
