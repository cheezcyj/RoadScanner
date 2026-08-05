import hashlib
import json
import shutil
import uuid
from io import BytesIO
from pathlib import Path
from urllib.parse import urlsplit

import pytest
import requests
from PIL import Image

import ml_service.download_gtsrb_design_icons as downloader
from ml_service.download_gtsrb_design_icons import (
    ABSOLUTE_MAX_SOURCE_BYTES,
    DEFAULT_CLASS_MAP,
    DEFAULT_SOURCE_LOCK,
    ICON_URL_TEMPLATE,
    _decode_and_flatten,
    _decode_and_normalize,
    _download_source,
    _load_classes,
    _load_source_lock,
    _validate_download_options,
    download_icons,
)


ROOT = Path(__file__).resolve().parents[2]


@pytest.fixture
def local_tmp_path():
    path = ROOT / "data" / "test-gtsrb-design-icons" / uuid.uuid4().hex
    path.mkdir(parents=True)
    try:
        yield path
    finally:
        shutil.rmtree(path, ignore_errors=True)


def _png_bytes(image: Image.Image) -> bytes:
    payload = BytesIO()
    image.save(payload, format="PNG")
    return payload.getvalue()


def _transparent_test_png() -> bytes:
    source = Image.new("RGBA", (256, 256), (220, 20, 20, 255))
    source.paste((0, 0, 0, 0), (96, 96, 160, 160))
    return _png_bytes(source)


def _write_test_source_lock(path: Path, source_bytes: bytes) -> None:
    source_sha256 = hashlib.sha256(source_bytes).hexdigest().upper()
    payload = {
        "schema_version": 1,
        "dataset": {"name": "Synset Signset Germany"},
        "sources": [
            {
                "class_id": class_id,
                "source_url": ICON_URL_TEMPLATE.format(class_id=class_id),
                "source_sha256": source_sha256,
                "source_image": {
                    "format": "PNG",
                    "mode": "RGBA",
                    "width": 256,
                    "height": 256,
                },
            }
            for class_id in range(43)
        ],
    }
    path.write_text(json.dumps(payload), encoding="utf-8")


class FakeResponse:
    def __init__(
        self,
        payload: bytes = b"",
        *,
        status_code: int = 200,
        headers: dict[str, str] | None = None,
    ) -> None:
        self.payload = payload
        self.status_code = status_code
        self.headers = (
            {"Content-Length": str(len(payload))} if headers is None else headers
        )
        self.closed = False

    def iter_content(self, chunk_size: int):
        for offset in range(0, len(self.payload), chunk_size):
            yield self.payload[offset : offset + chunk_size]

    def raise_for_status(self) -> None:
        if self.status_code >= 400:
            raise requests.HTTPError(f"HTTP {self.status_code}")

    def close(self) -> None:
        self.closed = True


class FakeSession:
    def __init__(self, responder) -> None:
        self.responder = responder
        self.calls: list[str] = []
        self.responses: list[FakeResponse] = []
        self.closed = False

    def get(self, url: str, **kwargs) -> FakeResponse:
        assert kwargs["stream"] is True
        assert kwargs["allow_redirects"] is False
        assert kwargs["timeout"] > 0
        self.calls.append(url)
        response = self.responder(url, len(self.calls) - 1)
        self.responses.append(response)
        return response

    def close(self) -> None:
        self.closed = True


def test_decode_and_normalize_centres_rgba_on_transparent_and_white_squares() -> None:
    source = Image.new("RGBA", (512, 256), (255, 0, 0, 255))
    source.paste((0, 0, 0, 0), (224, 96, 288, 160))

    transparent, white, metadata = _decode_and_normalize(_png_bytes(source), 256)

    assert transparent.mode == "RGBA"
    assert transparent.size == (256, 256)
    assert white.mode == "RGB"
    assert white.size == (256, 256)
    assert metadata == {"format": "PNG", "mode": "RGBA", "width": 512, "height": 256}
    assert transparent.getpixel((0, 0)) == (255, 255, 255, 0)
    assert transparent.getpixel((128, 128)) == (255, 255, 255, 0)
    assert transparent.convert("RGB").getpixel((0, 0)) == (255, 255, 255)
    assert transparent.getpixel((64, 128))[0] > 240
    assert transparent.getpixel((64, 128))[1] < 20
    assert white.getpixel((0, 0)) == (255, 255, 255)
    assert white.getpixel((128, 128)) == (255, 255, 255)


def test_transparent_normalization_preserves_unscaled_foreground_rgba() -> None:
    source = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    source.paste((10, 20, 30, 255), (64, 32, 448, 224))
    source.putpixel((256, 128), (70, 80, 90, 128))

    transparent, _, _ = _decode_and_normalize(_png_bytes(source), 512)
    centred = transparent.crop((0, 128, 512, 384))

    assert centred.getchannel("A").tobytes() == source.getchannel("A").tobytes()
    assert centred.getpixel((128, 128)) == source.getpixel((128, 128))
    assert centred.getpixel((256, 128)) == source.getpixel((256, 128))
    assert centred.getpixel((0, 0)) == (255, 255, 255, 0)


def test_decode_and_flatten_keeps_the_white_rgb_compatibility_contract() -> None:
    source_bytes = _transparent_test_png()

    white, metadata = _decode_and_flatten(source_bytes, 256)

    assert white.mode == "RGB"
    assert white.size == (256, 256)
    assert white.getpixel((128, 128)) == (255, 255, 255)
    assert metadata == {"format": "PNG", "mode": "RGBA", "width": 256, "height": 256}


def test_decode_and_flatten_rejects_tiny_source() -> None:
    source = Image.new("RGBA", (32, 32), (255, 0, 0, 0))

    with pytest.raises(ValueError, match="unexpected source dimensions"):
        _decode_and_normalize(_png_bytes(source), 256)


def test_decode_and_flatten_rejects_opaque_source() -> None:
    source = Image.new("RGB", (256, 256), (255, 0, 0))

    with pytest.raises(ValueError, match="actual transparent pixels"):
        _decode_and_normalize(_png_bytes(source), 256)


@pytest.mark.parametrize(
    ("canvas_size", "timeout_seconds", "max_source_bytes"),
    [
        (255, 30.0, 1024),
        (4097, 30.0, 1024),
        (256, 0.0, 1024),
        (256, float("nan"), 1024),
        (256, 30.0, 0),
        (256, 30.0, ABSOLUTE_MAX_SOURCE_BYTES + 1),
    ],
)
def test_download_options_are_bounded(
    canvas_size: int, timeout_seconds: float, max_source_bytes: int
) -> None:
    with pytest.raises(ValueError):
        _validate_download_options(canvas_size, timeout_seconds, max_source_bytes)


def test_load_classes_requires_ordered_gtsrb_catalog(local_tmp_path) -> None:
    classes = [
        {
            "class_id": class_id,
            "result_id": class_id + 1,
            "key": f"class_{class_id}",
            "name_en": f"Class {class_id}",
            "de_sign_code": str(class_id),
        }
        for class_id in range(43)
    ]
    classes[1], classes[2] = classes[2], classes[1]
    path = local_tmp_path / "class_map.json"
    path.write_text(
        json.dumps({"dataset": "GTSRB", "class_count": 43, "classes": classes}),
        encoding="utf-8",
    )

    with pytest.raises(ValueError, match="contiguous and ordered"):
        _load_classes(path)


def test_tracked_source_lock_covers_the_ordered_catalog() -> None:
    classes = _load_classes(DEFAULT_CLASS_MAP)

    sources = _load_source_lock(DEFAULT_SOURCE_LOCK, classes)

    assert [source.class_id for source in sources] == list(range(43))
    assert all(len(source.source_sha256) == 64 for source in sources)
    assert all(source.source_image["mode"] == "RGBA" for source in sources)


@pytest.mark.parametrize(
    "redirect_url",
    [
        "http://synset.de/wp-content/uploads/2024/08/0.png",
        "https://example.com/0.png",
    ],
)
def test_download_source_rejects_unsafe_redirect(redirect_url: str) -> None:
    session = FakeSession(
        lambda _url, _index: FakeResponse(
            status_code=302, headers={"Location": redirect_url}
        )
    )

    with pytest.raises(ValueError, match="approved synset.de host"):
        _download_source(
            session,
            ICON_URL_TEMPLATE.format(class_id=0),
            timeout_seconds=1.0,
            max_source_bytes=1024,
        )

    assert len(session.calls) == 1
    assert session.responses[0].closed is True


def test_download_source_enforces_streamed_byte_limit() -> None:
    session = FakeSession(
        lambda _url, _index: FakeResponse(b"0123456789", headers={})
    )

    with pytest.raises(ValueError, match="download limit"):
        _download_source(
            session,
            ICON_URL_TEMPLATE.format(class_id=0),
            timeout_seconds=1.0,
            max_source_bytes=5,
        )

    assert session.responses[0].closed is True


def test_download_icons_atomically_replaces_stale_output(
    local_tmp_path, monkeypatch
) -> None:
    source_bytes = _transparent_test_png()
    source_lock = local_tmp_path / "sources.lock.json"
    _write_test_source_lock(source_lock, source_bytes)
    session = FakeSession(lambda _url, _index: FakeResponse(source_bytes))
    monkeypatch.setattr(downloader, "_build_session", lambda: session)
    monkeypatch.setattr(
        downloader,
        "_rename_directory",
        lambda _source, _destination: (_ for _ in ()).throw(
            PermissionError("directory rename denied")
        ),
    )
    committed_files: list[str] = []
    real_replace_or_copy = downloader._replace_or_copy_file

    def record_commit(source_path: Path, destination_path: Path) -> None:
        committed_files.append(source_path.name)
        real_replace_or_copy(source_path, destination_path)

    monkeypatch.setattr(downloader, "_replace_or_copy_file", record_commit)

    output_dir = local_tmp_path / "output"
    output_dir.mkdir()
    (output_dir / "stale.txt").write_text("old generation", encoding="utf-8")

    manifest = download_icons(
        class_map_path=DEFAULT_CLASS_MAP,
        source_lock_path=source_lock,
        output_dir=output_dir,
        canvas_size=256,
        timeout_seconds=1.0,
        max_source_bytes=1024 * 1024,
    )

    assert session.closed is True
    assert len(session.calls) == 43
    assert manifest["image_count"] == 43
    assert manifest["png_file_count"] == 129
    assert manifest["recommended_variant"] == "upload-ready-transparent"
    assert not (output_dir / "stale.txt").exists()
    originals = sorted((output_dir / "original-transparent").glob("*.png"))
    upload_ready_transparent = sorted(
        (output_dir / "upload-ready-transparent").glob("*.png")
    )
    upload_ready_white = sorted((output_dir / "upload-ready-white").glob("*.png"))
    assert len(originals) == 43
    assert len(upload_ready_transparent) == 43
    assert len(upload_ready_white) == 43
    assert all(path.read_bytes() == source_bytes for path in originals)
    with Image.open(upload_ready_transparent[0]) as image:
        image.load()
        assert image.mode == "RGBA"
        assert image.size == (256, 256)
        assert image.getchannel("A").getextrema() == (0, 255)
        assert all(
            pixel[:3] == (255, 255, 255)
            for pixel in image.get_flattened_data()
            if pixel[3] == 0
        )
    with Image.open(upload_ready_white[0]) as image:
        assert image.mode == "RGB"
        assert image.size == (256, 256)
        assert image.getpixel((128, 128)) == (255, 255, 255)
    first_entry = manifest["images"][0]
    assert first_entry["recommended_upload_relative_path"].startswith(
        "upload-ready-transparent/"
    )
    assert first_entry["upload_ready_transparent_sha256"] == hashlib.sha256(
        upload_ready_transparent[0].read_bytes()
    ).hexdigest().upper()
    assert first_entry["upload_ready_white_sha256"] == hashlib.sha256(
        upload_ready_white[0].read_bytes()
    ).hexdigest().upper()
    attribution = (output_dir / "ATTRIBUTION.md").read_text(encoding="utf-8")
    assert "LANCZOS only when necessary" in attribution
    assert "256 x 256" in attribution
    assert "recommended for uploads and regression" in attribution
    assert "`(255, 255, 255)`" in attribution
    stored_manifest = json.loads(
        (output_dir / "manifest.json").read_text(encoding="utf-8")
    )
    assert stored_manifest == manifest
    assert committed_files[-1] == "manifest.json"
    assert not list(output_dir.parent.glob(f"{output_dir.name}-staging-*"))
    assert not list(output_dir.parent.glob(f"{output_dir.name}-backup-*"))


def test_download_icons_preserves_existing_output_after_midstream_failure(
    local_tmp_path, monkeypatch
) -> None:
    source_bytes = _transparent_test_png()
    source_lock = local_tmp_path / "sources.lock.json"
    _write_test_source_lock(source_lock, source_bytes)

    def respond(url: str, _index: int) -> FakeResponse:
        class_id = int(Path(urlsplit(url).path).stem)
        return FakeResponse(b"corrupt") if class_id == 20 else FakeResponse(source_bytes)

    session = FakeSession(respond)
    monkeypatch.setattr(downloader, "_build_session", lambda: session)
    output_dir = local_tmp_path / "output"
    output_dir.mkdir()
    sentinel = output_dir / "sentinel.txt"
    sentinel.write_text("must survive", encoding="utf-8")

    with pytest.raises(ValueError, match="SHA-256 mismatch for class 20"):
        download_icons(
            class_map_path=DEFAULT_CLASS_MAP,
            source_lock_path=source_lock,
            output_dir=output_dir,
            canvas_size=256,
            timeout_seconds=1.0,
            max_source_bytes=1024 * 1024,
        )

    assert session.closed is True
    assert sentinel.read_text(encoding="utf-8") == "must survive"
    assert list(output_dir.iterdir()) == [sentinel]
    assert not list(output_dir.parent.glob(f"{output_dir.name}-staging-*"))
    assert not list(output_dir.parent.glob(f"{output_dir.name}-backup-*"))


def test_file_commit_fallback_rolls_back_after_partial_copy_failure(
    local_tmp_path, monkeypatch
) -> None:
    source_bytes = _transparent_test_png()
    source_lock = local_tmp_path / "sources.lock.json"
    _write_test_source_lock(source_lock, source_bytes)
    session = FakeSession(lambda _url, _index: FakeResponse(source_bytes))
    monkeypatch.setattr(downloader, "_build_session", lambda: session)
    monkeypatch.setattr(
        downloader,
        "_rename_directory",
        lambda _source, _destination: (_ for _ in ()).throw(
            PermissionError("directory rename denied")
        ),
    )

    real_replace_or_copy = downloader._replace_or_copy_file
    failure_injected = False

    def fail_once_during_staging_commit(
        source_path: Path, destination_path: Path
    ) -> None:
        nonlocal failure_injected
        if (
            not failure_injected
            and any("-staging-" in part for part in source_path.parts)
            and source_path.name == "gtsrb_05_speed_limit_80.png"
        ):
            failure_injected = True
            raise OSError("injected file commit failure")
        real_replace_or_copy(source_path, destination_path)

    monkeypatch.setattr(
        downloader, "_replace_or_copy_file", fail_once_during_staging_commit
    )
    output_dir = local_tmp_path / "output"
    output_dir.mkdir()
    sentinel = output_dir / "sentinel.txt"
    sentinel.write_text("original generation", encoding="utf-8")

    with pytest.raises(OSError, match="injected file commit failure"):
        download_icons(
            class_map_path=DEFAULT_CLASS_MAP,
            source_lock_path=source_lock,
            output_dir=output_dir,
            canvas_size=256,
            timeout_seconds=1.0,
            max_source_bytes=1024 * 1024,
        )

    assert failure_injected is True
    assert list(output_dir.iterdir()) == [sentinel]
    assert sentinel.read_text(encoding="utf-8") == "original generation"
    assert not list(output_dir.parent.glob(f"{output_dir.name}-staging-*"))
    assert not list(output_dir.parent.glob(f"{output_dir.name}-backup-*"))
