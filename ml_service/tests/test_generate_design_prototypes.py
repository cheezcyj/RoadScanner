import hashlib
import json
import shutil
import uuid
from io import BytesIO
from pathlib import Path

import numpy as np
import pytest

from ml_service.classifier import (
    RECOVERED_CLASS_MAP_SHA256,
    RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256,
    RECOVERED_DESIGN_PROTOTYPE_SHA256,
    RECOVERED_MODEL_SHA256,
)
from ml_service.generate_design_prototypes import (
    build_deterministic_npz,
    generate_design_prototypes,
)


ROOT = Path(__file__).resolve().parents[2]
ARTIFACT = ROOT / "ml_service" / "design_prototypes.npz"
METADATA = ROOT / "ml_service" / "design_prototypes.json"
SOURCE_LOCK = ROOT / "ml_service" / "gtsrb_design_icon_sources.lock.json"
MODEL = ROOT / "road_scanner.h5"
CLASS_MAP = ROOT / "ml_service" / "class_map.json"
DESIGN_DIR = ROOT / "data" / "manual-validation" / "positive" / "gtsrb-design"


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def test_deterministic_prototype_npz_has_a_strict_array_contract():
    random = np.random.default_rng(20260803)
    prototypes = random.normal(size=(43, 512)).astype(np.float32)
    prototypes /= np.linalg.norm(prototypes, axis=1, keepdims=True)
    class_ids = np.arange(43, dtype=np.int64)

    first = build_deterministic_npz(prototypes, class_ids)
    second = build_deterministic_npz(prototypes.copy(), class_ids.copy())

    assert first == second
    with np.load(BytesIO(first), allow_pickle=False) as payload:
        assert set(payload.files) == {"prototypes", "class_ids"}
        np.testing.assert_array_equal(payload["class_ids"], class_ids)
        np.testing.assert_allclose(payload["prototypes"], prototypes)


@pytest.mark.parametrize(
    ("shape", "message"),
    [((42, 512), "shape"), ((43, 511), "shape")],
)
def test_deterministic_prototype_npz_rejects_wrong_shape(shape, message):
    with pytest.raises(ValueError, match=message):
        build_deterministic_npz(
            np.zeros(shape, dtype=np.float32), np.arange(43, dtype=np.int64)
        )


def test_tracked_prototype_artifact_and_provenance_are_pinned():
    metadata = json.loads(METADATA.read_text(encoding="utf-8"))

    assert _sha256(ARTIFACT) == RECOVERED_DESIGN_PROTOTYPE_SHA256
    assert _sha256(METADATA) == RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256
    assert metadata["artifact_sha256"] == RECOVERED_DESIGN_PROTOTYPE_SHA256
    assert metadata["model_sha256"] == RECOVERED_MODEL_SHA256
    assert metadata["class_map_sha256"] == RECOVERED_CLASS_MAP_SHA256
    assert metadata["source_lock_sha256"] == _sha256(SOURCE_LOCK)
    assert metadata["source_variant"] == "upload-ready-transparent"
    assert metadata["shape"] == [43, 512]
    assert [item["class_id"] for item in metadata["classes"]] == list(range(43))


@pytest.mark.skipif(
    not MODEL.is_file() or not (DESIGN_DIR / "manifest.json").is_file(),
    reason="local recovered model or canonical design sources absent",
)
def test_tracked_prototype_artifact_is_reproducible_from_locked_sources():
    output_dir = ROOT / "data" / "test-design-prototype-build" / uuid.uuid4().hex
    output_dir.mkdir(parents=True)
    artifact = output_dir / ARTIFACT.name
    metadata = output_dir / METADATA.name
    try:
        generated = generate_design_prototypes(
            model_path=MODEL,
            class_map_path=CLASS_MAP,
            source_lock_path=SOURCE_LOCK,
            design_dir=DESIGN_DIR,
            artifact_path=artifact,
            metadata_path=metadata,
        )

        assert artifact.read_bytes() == ARTIFACT.read_bytes()
        assert metadata.read_bytes() == METADATA.read_bytes()
        assert generated["artifact_sha256"] == RECOVERED_DESIGN_PROTOTYPE_SHA256
        assert generated["metadata_sha256"] == (
            RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256
        )
    finally:
        shutil.rmtree(output_dir, ignore_errors=True)
