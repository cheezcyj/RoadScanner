"""Build the pinned canonical-design prototype reference used at inference.

The source artwork is not committed because ``data/`` is intentionally ignored.
Regeneration is nevertheless deterministic: the downloader first reconstructs
the 43 recommended transparent canvases and their white RGB compatibility views
from the tracked per-source lock. This module verifies that RGB conversion is
pixel-equivalent, then embeds the transparent variant with the pinned model.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import zipfile
from io import BytesIO
from pathlib import Path
from typing import Any, Mapping

import numpy as np
from PIL import __version__ as PILLOW_VERSION

from .classifier import (
    OOD_FEATURE_LAYER,
    RECOVERED_CLASS_MAP_SHA256,
    RECOVERED_MODEL_SHA256,
    TrafficSignClassifier,
)
from .download_gtsrb_design_icons import (
    DATASET_NAME,
    DEFAULT_CLASS_MAP,
    DEFAULT_OUTPUT_DIR,
    DEFAULT_SOURCE_LOCK,
    _decode_and_normalize,
    _encode_png,
    _load_classes,
    _load_source_lock,
)


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MODEL = PROJECT_ROOT / "road_scanner.h5"
DEFAULT_ARTIFACT = Path(__file__).with_name("design_prototypes.npz")
DEFAULT_METADATA = Path(__file__).with_name("design_prototypes.json")
PROTOTYPE_COUNT = 43
PROTOTYPE_DIMENSION = 512
FIXED_ZIP_TIMESTAMP = (1980, 1, 1, 0, 0, 0)


def _sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest().upper()


def _sha256_path(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def _validated_hash(actual: str, expected: str, label: str) -> None:
    if actual != expected.upper():
        raise ValueError(f"{label} SHA-256 mismatch: {actual}")


def _npy_bytes(array: np.ndarray) -> bytes:
    output = BytesIO()
    np.save(output, np.asarray(array), allow_pickle=False)
    return output.getvalue()


def build_deterministic_npz(
    prototypes: np.ndarray, class_ids: np.ndarray
) -> bytes:
    """Return byte-stable NPZ data without current-time ZIP metadata."""

    normalized_prototypes = np.asarray(prototypes, dtype=np.float32)
    normalized_class_ids = np.asarray(class_ids, dtype=np.int64)
    if normalized_prototypes.shape != (PROTOTYPE_COUNT, PROTOTYPE_DIMENSION):
        raise ValueError(
            "design prototypes must have shape "
            f"({PROTOTYPE_COUNT}, {PROTOTYPE_DIMENSION})"
        )
    if normalized_class_ids.shape != (PROTOTYPE_COUNT,) or not np.array_equal(
        normalized_class_ids, np.arange(PROTOTYPE_COUNT, dtype=np.int64)
    ):
        raise ValueError("design prototype class IDs must be ordered from 0 to 42")
    if not np.all(np.isfinite(normalized_prototypes)):
        raise ValueError("design prototypes contain non-finite values")

    output = BytesIO()
    with zipfile.ZipFile(output, mode="w", compression=zipfile.ZIP_STORED) as archive:
        for filename, payload in (
            ("prototypes.npy", _npy_bytes(normalized_prototypes)),
            ("class_ids.npy", _npy_bytes(normalized_class_ids)),
        ):
            info = zipfile.ZipInfo(filename=filename, date_time=FIXED_ZIP_TIMESTAMP)
            info.compress_type = zipfile.ZIP_STORED
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            archive.writestr(info, payload)
    return output.getvalue()


def _atomic_write(path: Path, payload: bytes) -> None:
    resolved = path.resolve()
    resolved.parent.mkdir(parents=True, exist_ok=True)
    temporary = resolved.with_name(f"{resolved.name}.part")
    try:
        temporary.write_bytes(payload)
        os.replace(temporary, resolved)
    finally:
        temporary.unlink(missing_ok=True)


def _load_manifest(path: Path) -> Mapping[str, Any]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if payload.get("schema_version") != 1:
        raise ValueError("design-icon manifest must use schema_version 1")
    dataset = payload.get("dataset")
    if not isinstance(dataset, Mapping) or dataset.get("name") != DATASET_NAME:
        raise ValueError(f"design-icon manifest must describe {DATASET_NAME}")
    if payload.get("image_count") != PROTOTYPE_COUNT:
        raise ValueError("design-icon manifest must contain exactly 43 images")
    if payload.get("png_file_count") != PROTOTYPE_COUNT * 3:
        raise ValueError("design-icon manifest must contain all three 43-image variants")
    if payload.get("recommended_variant") != "upload-ready-transparent":
        raise ValueError(
            "design-icon manifest must recommend upload-ready-transparent"
        )
    if not isinstance(payload.get("canvas_size"), int):
        raise ValueError("design-icon manifest is missing canvas_size")
    images = payload.get("images")
    if not isinstance(images, list) or len(images) != PROTOTYPE_COUNT:
        raise ValueError("design-icon manifest must contain 43 ordered image rows")
    return payload


def _canonical_json_bytes(payload: Mapping[str, Any]) -> bytes:
    return (
        json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    ).encode("utf-8")


def generate_design_prototypes(
    *,
    model_path: Path,
    class_map_path: Path,
    source_lock_path: Path,
    design_dir: Path,
    artifact_path: Path,
    metadata_path: Path,
    expected_model_sha256: str = RECOVERED_MODEL_SHA256,
    expected_class_map_sha256: str = RECOVERED_CLASS_MAP_SHA256,
) -> dict[str, Any]:
    model_path = model_path.resolve()
    class_map_path = class_map_path.resolve()
    source_lock_path = source_lock_path.resolve()
    design_dir = design_dir.resolve()
    artifact_path = artifact_path.resolve()
    metadata_path = metadata_path.resolve()
    if artifact_path == metadata_path:
        raise ValueError("artifact and metadata paths must be different")

    model_sha256 = _sha256_path(model_path)
    class_map_sha256 = _sha256_path(class_map_path)
    source_lock_sha256 = _sha256_path(source_lock_path)
    _validated_hash(model_sha256, expected_model_sha256, "model")
    _validated_hash(class_map_sha256, expected_class_map_sha256, "class map")

    classes = _load_classes(class_map_path)
    locked_sources = _load_source_lock(source_lock_path, classes)
    manifest = _load_manifest(design_dir / "manifest.json")
    manifest_source_lock = manifest.get("source_lock")
    if (
        not isinstance(manifest_source_lock, Mapping)
        or manifest_source_lock.get("filename") != source_lock_path.name
        or manifest_source_lock.get("sha256") != source_lock_sha256
    ):
        raise ValueError("design-icon manifest source-lock provenance mismatch")
    canvas_size = int(manifest["canvas_size"])
    manifest_rows = manifest["images"]

    tensors: list[np.ndarray] = []
    source_rows: list[dict[str, Any]] = []
    image_set_digest = hashlib.sha256()
    for item, locked, manifest_row in zip(classes, locked_sources, manifest_rows):
        filename = f"gtsrb_{item.class_id:02d}_{item.key}.png"
        expected_original = f"original-transparent/{filename}"
        expected_transparent = f"upload-ready-transparent/{filename}"
        expected_white = f"upload-ready-white/{filename}"
        if int(manifest_row.get("class_id", -1)) != item.class_id:
            raise ValueError("design-icon manifest class IDs must be ordered from 0 to 42")
        if manifest_row.get("original_relative_path") != expected_original:
            raise ValueError(f"unexpected original path for class {item.class_id}")
        if manifest_row.get("recommended_upload_relative_path") != expected_transparent:
            raise ValueError(
                f"unexpected recommended upload path for class {item.class_id}"
            )
        if (
            manifest_row.get("upload_ready_transparent_relative_path")
            != expected_transparent
        ):
            raise ValueError(
                f"unexpected transparent upload path for class {item.class_id}"
            )
        if manifest_row.get("upload_ready_white_relative_path") != expected_white:
            raise ValueError(
                f"unexpected white upload path for class {item.class_id}"
            )

        original_bytes = (design_dir / expected_original).read_bytes()
        original_sha256 = _sha256_bytes(original_bytes)
        _validated_hash(
            original_sha256,
            locked.source_sha256,
            f"canonical source class {item.class_id}",
        )
        if manifest_row.get("source_sha256") != original_sha256:
            raise ValueError(f"manifest source hash mismatch for class {item.class_id}")

        regenerated_transparent, regenerated_white, source_metadata = (
            _decode_and_normalize(original_bytes, canvas_size)
        )
        if source_metadata != locked.source_image:
            raise ValueError(f"source metadata mismatch for class {item.class_id}")
        transparent_bytes = (design_dir / expected_transparent).read_bytes()
        regenerated_transparent_bytes = _encode_png(regenerated_transparent)
        if transparent_bytes != regenerated_transparent_bytes:
            raise ValueError(
                "transparent upload-ready image is not the locked deterministic "
                f"render for class {item.class_id}"
            )
        transparent_sha256 = _sha256_bytes(transparent_bytes)
        if (
            manifest_row.get("upload_ready_transparent_sha256")
            != transparent_sha256
        ):
            raise ValueError(
                f"manifest transparent hash mismatch for class {item.class_id}"
            )

        white_bytes = (design_dir / expected_white).read_bytes()
        regenerated_white_bytes = _encode_png(regenerated_white)
        if white_bytes != regenerated_white_bytes:
            raise ValueError(
                "white upload-ready image is not the locked deterministic render "
                f"for class {item.class_id}"
            )
        white_sha256 = _sha256_bytes(white_bytes)
        if manifest_row.get("upload_ready_white_sha256") != white_sha256:
            raise ValueError(
                f"manifest white hash mismatch for class {item.class_id}"
            )

        transparent_rgb = np.asarray(
            regenerated_transparent.convert("RGB"), dtype=np.uint8
        )
        white_rgb = np.asarray(regenerated_white, dtype=np.uint8)
        if not np.array_equal(transparent_rgb, white_rgb):
            raise ValueError(
                "recommended transparent RGB conversion differs from the white "
                f"compatibility image for class {item.class_id}"
            )

        tensors.append(TrafficSignClassifier.preprocess(regenerated_transparent)[0])
        source_row = {
            "class_id": item.class_id,
            "key": item.key,
            "filename": filename,
            "source_sha256": original_sha256,
            "prototype_input_sha256": transparent_sha256,
            "upload_ready_transparent_sha256": transparent_sha256,
            "upload_ready_white_sha256": white_sha256,
        }
        source_rows.append(source_row)
        image_set_digest.update(_canonical_json_bytes(source_row))

    import tensorflow as tf

    model = tf.keras.models.load_model(str(model_path), compile=False)
    if tuple(model.input_shape) != TrafficSignClassifier.EXPECTED_INPUT_SHAPE:
        raise ValueError(f"unexpected model input shape: {model.input_shape}")
    try:
        feature_layer = model.get_layer(OOD_FEATURE_LAYER)
    except (AttributeError, ValueError) as error:
        raise ValueError(
            f"model is missing the prototype feature layer: {OOD_FEATURE_LAYER}"
        ) from error
    feature_model = tf.keras.Model(model.inputs[0], feature_layer.output)
    raw_features = np.asarray(
        feature_model(np.stack(tensors).astype(np.float32), training=False),
        dtype=np.float32,
    )
    if raw_features.shape != (PROTOTYPE_COUNT, PROTOTYPE_DIMENSION):
        raise ValueError(f"unexpected prototype feature shape: {raw_features.shape}")
    norms = np.linalg.norm(raw_features, axis=1, keepdims=True)
    if np.any(norms <= 1e-12) or not np.all(np.isfinite(norms)):
        raise ValueError("prototype features must have finite non-zero norms")
    prototypes = np.asarray(raw_features / norms, dtype=np.float32)
    similarities = prototypes @ prototypes.T
    nearest = np.argmax(similarities, axis=1)
    if not np.array_equal(nearest, np.arange(PROTOTYPE_COUNT)):
        raise ValueError("canonical prototypes are not uniquely nearest to their own class")

    artifact_bytes = build_deterministic_npz(
        prototypes, np.arange(PROTOTYPE_COUNT, dtype=np.int64)
    )
    artifact_sha256 = _sha256_bytes(artifact_bytes)
    metadata: dict[str, Any] = {
        "schema_version": 1,
        "artifact": artifact_path.name,
        "artifact_sha256": artifact_sha256,
        "shape": [PROTOTYPE_COUNT, PROTOTYPE_DIMENSION],
        "dtype": "float32",
        "row_l2_normalized": True,
        "artifact_format": "deterministic_npz_zip_stored",
        "feature_layer": OOD_FEATURE_LAYER,
        "model_sha256": model_sha256,
        "class_map_sha256": class_map_sha256,
        "source_lock": source_lock_path.name,
        "source_lock_sha256": source_lock_sha256,
        "source_image_set_sha256": image_set_digest.hexdigest().upper(),
        "source_dataset": DATASET_NAME,
        "source_variant": "upload-ready-transparent",
        "rgb_compatibility_variant": "upload-ready-white",
        "rgb_conversion_pixel_equivalent": True,
        "canvas_size": canvas_size,
        "preprocessing": "RGB decode -> BGR -> bicubic 30x30 -> float32 / 255",
        "generation_runtime": {
            "tensorflow": str(tf.__version__),
            "numpy": str(np.__version__),
            "pillow": str(PILLOW_VERSION),
        },
        "classes": source_rows,
    }
    metadata_bytes = _canonical_json_bytes(metadata)
    _atomic_write(artifact_path, artifact_bytes)
    _atomic_write(metadata_path, metadata_bytes)
    metadata["metadata_sha256"] = _sha256_bytes(metadata_bytes)
    return metadata


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Generate the pinned 43x512 canonical-design prototype reference."
    )
    parser.add_argument("--model", type=Path, default=DEFAULT_MODEL)
    parser.add_argument("--class-map", type=Path, default=DEFAULT_CLASS_MAP)
    parser.add_argument("--source-lock", type=Path, default=DEFAULT_SOURCE_LOCK)
    parser.add_argument("--design-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--output", type=Path, default=DEFAULT_ARTIFACT)
    parser.add_argument("--metadata-output", type=Path, default=DEFAULT_METADATA)
    return parser


def main() -> None:
    args = build_parser().parse_args()
    metadata = generate_design_prototypes(
        model_path=args.model,
        class_map_path=args.class_map,
        source_lock_path=args.source_lock,
        design_dir=args.design_dir,
        artifact_path=args.output,
        metadata_path=args.metadata_output,
    )
    print(
        "Generated "
        f"{metadata['shape'][0]}x{metadata['shape'][1]} design prototypes at "
        f"{args.output.resolve()}"
    )
    print(f"artifact_sha256={metadata['artifact_sha256']}")
    print(f"metadata_sha256={metadata['metadata_sha256']}")


if __name__ == "__main__":
    main()
