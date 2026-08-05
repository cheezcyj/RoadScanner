import io
import json
import random
import shutil
import subprocess
import sys
import uuid
from pathlib import Path

import pytest
import numpy as np
from PIL import Image

from ml_service.train_detector import (
    AugmentationPlan,
    DetectionRecord,
    IMAGE_SIZE,
    LETTERBOX_FILL,
    OpenImagesDetectionDataset,
    apply_augmentation,
    build_detection_target,
    build_parser,
    calculate_detection_metrics,
    checkpoint_contract,
    compute_letterbox_geometry,
    load_dataset_manifest,
    sample_augmentation_plan,
    sha256_bytes,
    transform_normalized_box,
    validate_checkpoint_contract,
    _capture_rng_state,
    _restore_rng_state,
    _safe_torch_load,
)


ROOT = Path(__file__).resolve().parents[2]


@pytest.fixture
def local_tmp_path():
    # The Codex Windows sandbox can deny access to pytest's chmod(0700)
    # basetemp. A unique directory below the already-writable, gitignored data
    # root keeps this test portable without relying on the process TEMP path.
    path = ROOT / "data" / "test-train-detector" / uuid.uuid4().hex
    path.mkdir(parents=True)
    try:
        yield path
    finally:
        shutil.rmtree(path, ignore_errors=True)


def _manifest(image_path: Path, *, class_id=0, is_negative=False):
    image = {
        "image_id": "0123456789abcdef",
        "path": str(image_path),
        "boxes": [
            {
                "xmin": 0.25,
                "xmax": 0.75,
                "ymin": 0.25,
                "ymax": 0.75,
                "occluded": False,
                "truncated": False,
                "source_mid": "/m/01mqdt",
            }
        ],
    }
    if is_negative:
        image["is_negative"] = True
        image["boxes"] = []
    return {
        "schema_version": 1,
        "dataset": "Open Images",
        "dataset_version": "v7",
        "class": {"id": class_id, "name": "traffic_sign"},
        "splits": {
            "train": {"images": [image]},
            "validation": {"images": [image]},
        },
    }


def test_module_import_does_not_import_pytorch():
    result = subprocess.run(
        [
            sys.executable,
            "-c",
            "import sys; import ml_service.train_detector; "
            "print(int('torch' in sys.modules or 'torchvision' in sys.modules))",
        ],
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
    )

    assert result.stdout.strip() == "0"


def test_letterbox_is_fixed_320_with_runtime_fill_contract():
    geometry = compute_letterbox_geometry(640, 360)

    assert geometry.target_size == IMAGE_SIZE == 320
    assert geometry.resized_width == 320
    assert geometry.resized_height == 180
    assert geometry.pad_left == 0
    assert geometry.pad_top == 70
    assert LETTERBOX_FILL == 114


def test_normalized_box_is_transformed_to_letterboxed_xyxy_pixels():
    geometry = compute_letterbox_geometry(640, 360)

    transformed = transform_normalized_box((0.25, 0.25, 0.75, 0.75), geometry)

    assert transformed == pytest.approx((80.0, 115.0, 240.0, 205.0))


def test_augmentation_plan_is_seeded_and_stays_in_safe_color_range():
    first_rng = random.Random(20260803)
    second_rng = random.Random(20260803)

    first = [sample_augmentation_plan(first_rng) for _ in range(4)]
    second = [sample_augmentation_plan(second_rng) for _ in range(4)]

    assert first == second
    for plan in first:
        assert 0.8 <= plan.brightness <= 1.2
        assert 0.8 <= plan.contrast <= 1.2
        assert 0.8 <= plan.color <= 1.2


def test_horizontal_augmentation_flips_pixels_and_bbox_without_mutating_source():
    image = Image.new("RGB", (2, 1))
    image.putpixel((0, 0), (255, 0, 0))
    image.putpixel((1, 0), (0, 0, 255))
    boxes = ((0.1, 0.2, 0.4, 0.8),)
    plan = AugmentationPlan(
        horizontal_flip=True, brightness=1.0, contrast=1.0, color=1.0
    )

    augmented, augmented_boxes = apply_augmentation(image, boxes, plan)

    assert augmented.getpixel((0, 0)) == (0, 0, 255)
    assert augmented.getpixel((1, 0)) == (255, 0, 0)
    assert augmented_boxes[0] == pytest.approx((0.6, 0.2, 0.9, 0.8))
    assert boxes == ((0.1, 0.2, 0.4, 0.8),)


def test_detection_dataset_defaults_to_validation_safe_no_augmentation():
    record = DetectionRecord(
        image_id="test",
        image_path=Path("not-read.jpg"),
        boxes=((0.1, 0.2, 0.4, 0.8),),
    )

    validation = OpenImagesDetectionDataset([record])
    training = OpenImagesDetectionDataset([record], augment=True)

    assert validation.augment is False
    assert training.augment is True


def test_manifest_is_validated_and_its_exact_sha_is_recorded(local_tmp_path):
    image_path = local_tmp_path / "sign.jpg"
    image_path.write_bytes(b"test-placeholder")
    manifest_path = local_tmp_path / "manifest.json"
    payload = json.dumps(_manifest(image_path), indent=2).encode("utf-8")
    manifest_path.write_bytes(payload)

    manifest = load_dataset_manifest(manifest_path)

    assert manifest.sha256 == sha256_bytes(payload)
    assert len(manifest.records["train"]) == 1
    assert manifest.records["train"][0].image_path == image_path
    assert manifest.records["train"][0].boxes == ((0.25, 0.25, 0.75, 0.75),)


def test_manifest_rejects_an_unexpected_class_contract(local_tmp_path):
    manifest_path = local_tmp_path / "manifest.json"
    manifest_path.write_text(
        json.dumps(_manifest(local_tmp_path / "sign.jpg", class_id=1)), encoding="utf-8"
    )

    with pytest.raises(ValueError, match="traffic_sign class id 0"):
        load_dataset_manifest(manifest_path)


def test_manifest_allows_explicit_hard_negative_but_not_empty_positive(
    local_tmp_path,
):
    negative_path = local_tmp_path / "negative.json"
    negative_path.write_text(
        json.dumps(_manifest(local_tmp_path / "road.jpg", is_negative=True)),
        encoding="utf-8",
    )

    manifest = load_dataset_manifest(negative_path)

    assert manifest.records["train"][0].is_negative is True
    assert manifest.records["train"][0].boxes == ()

    invalid = _manifest(local_tmp_path / "road.jpg")
    invalid["splits"]["train"]["images"][0]["boxes"] = []
    invalid_path = local_tmp_path / "invalid-positive.json"
    invalid_path.write_text(json.dumps(invalid), encoding="utf-8")
    with pytest.raises(ValueError, match="image has no boxes"):
        load_dataset_manifest(invalid_path)


def test_hard_negative_target_preserves_torchvision_empty_tensor_shapes():
    class FakeTorch:
        float32 = np.float32
        int64 = np.int64

        @staticmethod
        def tensor(values, dtype):
            return np.asarray(values, dtype=dtype)

        @staticmethod
        def empty(shape, dtype):
            return np.empty(shape, dtype=dtype)

        @staticmethod
        def full(shape, value, dtype):
            return np.full(shape, value, dtype=dtype)

        @staticmethod
        def zeros(shape, dtype):
            return np.zeros(shape, dtype=dtype)

    target = build_detection_target([], 3, FakeTorch)

    assert target["boxes"].shape == (0, 4)
    assert target["boxes"].dtype == np.float32
    assert target["labels"].shape == (0,)
    assert target["area"].shape == (0,)
    assert target["iscrowd"].shape == (0,)


def test_ap50_precision_and_recall_use_one_to_one_iou_matching():
    ground_truths = {
        "first": [(0.0, 0.0, 10.0, 10.0)],
        "second": [(20.0, 20.0, 30.0, 30.0)],
    }
    predictions = {
        "first": [
            (0.99, (0.0, 0.0, 10.0, 10.0)),
            (0.90, (0.0, 0.0, 10.0, 10.0)),  # duplicate is a false positive
        ],
        "second": [(0.80, (20.0, 20.0, 30.0, 30.0))],
    }

    metrics = calculate_detection_metrics(
        ground_truths, predictions, score_threshold=0.85, iou_threshold=0.5
    )

    assert metrics["true_positives"] == 1
    assert metrics["false_positives"] == 1
    assert metrics["precision"] == pytest.approx(0.5)
    assert metrics["recall"] == pytest.approx(0.5)
    assert metrics["ap50"] == pytest.approx(5 / 6, abs=0.01)


def test_checkpoint_rejects_a_different_dataset_manifest_sha():
    checkpoint = {
        "schema_version": 1,
        "contract": checkpoint_contract("A" * 64),
    }

    validate_checkpoint_contract(checkpoint, "A" * 64)
    with pytest.raises(ValueError, match="manifest SHA"):
        validate_checkpoint_contract(checkpoint, "B" * 64)


class _FakeRngTensor:
    def detach(self):
        return self

    def cpu(self):
        return self


class _FakeCuda:
    @staticmethod
    def is_available():
        return False


class _FakeRngTorch:
    cuda = _FakeCuda()
    restored_state = None

    @staticmethod
    def get_rng_state():
        return _FakeRngTensor()

    @classmethod
    def set_rng_state(cls, state):
        cls.restored_state = state


def test_checkpoint_rng_state_is_weights_only_safe_and_reproducible():
    original_python_state = random.getstate()
    original_numpy_state = np.random.get_state()
    stack = {"torch": _FakeRngTorch, "np": np}
    try:
        random.seed(20260804)
        np.random.seed(20260804)
        state = _capture_rng_state(stack)
        expected = (random.random(), float(np.random.random()))

        assert isinstance(state["numpy"], dict)
        assert isinstance(state["numpy"]["keys"], list)
        assert all(isinstance(value, int) for value in state["numpy"]["keys"])

        random.seed(1)
        np.random.seed(1)
        _restore_rng_state(state, stack)

        assert (random.random(), float(np.random.random())) == pytest.approx(expected)
        assert _FakeRngTorch.restored_state is state["torch"]
    finally:
        random.setstate(original_python_state)
        np.random.set_state(original_numpy_state)


def test_checkpoint_loader_always_requests_restricted_weights_only_mode():
    class FakeTorch:
        calls = []

        @classmethod
        def load(cls, path, **kwargs):
            cls.calls.append((path, kwargs))
            return {"schema_version": 1}

    checkpoint = _safe_torch_load(Path("checkpoint.pt"), FakeTorch, "cpu")

    assert checkpoint == {"schema_version": 1}
    assert FakeTorch.calls == [
        (
            Path("checkpoint.pt"),
            {"map_location": "cpu", "weights_only": True},
        )
    ]


def test_checkpoint_loader_never_falls_back_to_unrestricted_pickle():
    class LegacyTorch:
        calls = 0

        @classmethod
        def load(cls, path, **kwargs):
            cls.calls += 1
            raise TypeError("unexpected keyword argument 'weights_only'")

    with pytest.raises(RuntimeError, match="weights_only"):
        _safe_torch_load(Path("checkpoint.pt"), LegacyTorch, "cpu")

    assert LegacyTorch.calls == 1


def test_checkpoint_version_metadata_round_trips_with_weights_only_loader():
    torch = pytest.importorskip("torch")
    payload = {
        "versions": {
            "python": str(sys.version_info.major),
            "torch": str(torch.__version__),
            "torchvision": "test-version",
        },
        "rng_state": _capture_rng_state({"torch": torch, "np": np}),
    }
    checkpoint = io.BytesIO()
    torch.save(payload, checkpoint)
    checkpoint.seek(0)

    restored = _safe_torch_load(checkpoint, torch, "cpu")

    assert restored["versions"] == payload["versions"]
    assert isinstance(restored["versions"]["torch"], str)


def test_cli_defaults_to_amp_and_does_not_expose_unsafe_worker_processes():
    parser = build_parser()
    args = parser.parse_args([])

    assert args.amp is True
    assert args.pretrained_backbone is True
    assert args.export_onnx is True
    assert not hasattr(args, "workers")
    assert not hasattr(args, "num_workers")
