import builtins
import hashlib
from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from ml_service.detector import TrafficSignDetector


ROOT = Path(__file__).resolve().parents[2]


class FakeValueInfo:
    def __init__(self, name, shape=None):
        self.name = name
        self.shape = shape


class FakeSession:
    def __init__(self, boxes=None, scores=None, labels=None, input_shape=None):
        self.boxes = np.asarray(
            boxes if boxes is not None else [], dtype=np.float32
        ).reshape(-1, 4)
        self.scores = np.asarray(
            scores if scores is not None else [], dtype=np.float32
        )
        self.labels = np.asarray(
            labels if labels is not None else [], dtype=np.int64
        )
        self.input_shape = input_shape or [1, 3, 320, 320]
        self.requested_outputs = None
        self.last_feed = None

    def get_inputs(self):
        return [FakeValueInfo("images", self.input_shape)]

    def get_outputs(self):
        return [
            FakeValueInfo("boxes"),
            FakeValueInfo("scores"),
            FakeValueInfo("labels"),
        ]

    def run(self, output_names, feed):
        self.requested_outputs = output_names
        self.last_feed = feed
        return [self.boxes, self.scores, self.labels]


def detector(session, **kwargs):
    return TrafficSignDetector(
        model_path=ROOT / "missing-test-detector.onnx",
        session=session,
        **kwargs,
    )


def test_injected_session_uses_fixed_rgb_letterbox_and_restores_boxes():
    session = FakeSession(
        boxes=[[80.0, 100.0, 240.0, 220.0]],
        scores=[0.9],
        labels=[1],
    )
    service = detector(session)

    detections = service.detect(
        Image.new("RGB", (160, 80), color=(10, 20, 30))
    )

    assert session.requested_outputs == ["boxes", "scores", "labels"]
    tensor = session.last_feed["images"]
    assert tensor.shape == (1, 3, 320, 320)
    assert tensor.dtype == np.float32
    np.testing.assert_allclose(
        tensor[0, :, 100, 100], np.asarray([10, 20, 30]) / 255.0
    )
    np.testing.assert_allclose(
        tensor[0, :, 0, 0], np.asarray([114, 114, 114]) / 255.0
    )
    assert len(detections) == 1
    np.testing.assert_allclose(
        [
            detections[0].x_min,
            detections[0].y_min,
            detections[0].x_max,
            detections[0].y_max,
        ],
        [40.0, 10.0, 120.0, 70.0],
    )
    assert detections[0].score == pytest.approx(0.9)
    assert detections[0].label == 1


def test_injected_session_does_not_import_onnxruntime(monkeypatch):
    original_import = builtins.__import__

    def guarded_import(name, *args, **kwargs):
        if name == "onnxruntime":
            raise AssertionError("onnxruntime must stay lazy for injected sessions")
        return original_import(name, *args, **kwargs)

    monkeypatch.setattr(builtins, "__import__", guarded_import)

    service = detector(FakeSession())

    assert service.detect(Image.new("RGB", (32, 32), color="white")) == []


def test_artifact_hash_is_verified_before_using_injected_session():
    model_path = ROOT / "ml_service" / "ood_reference.npz"
    expected = hashlib.sha256(model_path.read_bytes()).hexdigest()

    service = TrafficSignDetector(
        model_path=model_path,
        expected_model_sha256=expected.lower(),
        session=FakeSession(),
    )

    assert service.model_sha256 == expected.upper()
    assert service.metadata()["model_hash_verified"] is True
    with pytest.raises(ValueError, match="Detector model SHA-256 mismatch"):
        TrafficSignDetector(
            model_path=model_path,
            expected_model_sha256="0" * 64,
            session=FakeSession(),
        )


def test_score_threshold_numpy_nms_and_candidate_limit_are_applied():
    session = FakeSession(
        boxes=[
            [10.0, 10.0, 100.0, 100.0],
            [12.0, 12.0, 98.0, 98.0],
            [200.0, 200.0, 240.0, 240.0],
            [260.0, 260.0, 300.0, 300.0],
            [120.0, 120.0, 150.0, 150.0],
        ],
        scores=[0.95, 0.90, 0.80, 0.70, 0.20],
        labels=[1, 1, 1, 1, 1],
    )
    service = detector(
        session,
        min_score=0.5,
        nms_iou_threshold=0.5,
        max_candidates=2,
    )

    detections = service.detect(Image.new("RGB", (320, 320), color="white"))

    assert [d.score for d in detections] == pytest.approx([0.95, 0.80])
    assert [d.x_min for d in detections] == pytest.approx([10.0, 200.0])


def test_candidate_limit_is_applied_after_nms_so_separate_sign_survives():
    service = detector(
        FakeSession(
            boxes=[
                [10.0, 10.0, 100.0, 100.0],
                [12.0, 12.0, 98.0, 98.0],
                [200.0, 200.0, 250.0, 250.0],
            ],
            scores=[0.99, 0.98, 0.97],
            labels=[1, 1, 1],
        ),
        max_candidates=2,
        nms_iou_threshold=0.5,
    )

    detections = service.detect(Image.new("RGB", (320, 320), color="white"))

    assert [d.score for d in detections] == pytest.approx([0.99, 0.97])
    assert [d.x_min for d in detections] == pytest.approx([10.0, 200.0])


def test_restore_boxes_uses_real_scale_on_each_letterbox_axis():
    # 101x67 rounds to 320x212. The two realized scales are not identical.
    service = detector(
        FakeSession(
            boxes=[[0.0, 54.0, 320.0, 266.0]],
            scores=[0.9],
            labels=[1],
        )
    )

    detection = service.detect(Image.new("RGB", (101, 67), color="white"))[0]

    assert (detection.x_min, detection.y_min) == pytest.approx((0.0, 0.0))
    assert (detection.x_max, detection.y_max) == pytest.approx((101.0, 67.0))


def test_detector_preprocess_ignores_hidden_rgb_in_transparent_pixels():
    first = Image.new("RGBA", (40, 40), color=(255, 0, 0, 0))
    second = Image.new("RGBA", (40, 40), color=(0, 0, 255, 0))
    first.putpixel((20, 20), (10, 30, 220, 255))
    second.putpixel((20, 20), (10, 30, 220, 255))

    first_tensor, _ = TrafficSignDetector.preprocess(first)
    second_tensor, _ = TrafficSignDetector.preprocess(second)

    np.testing.assert_array_equal(first_tensor, second_tensor)


def test_clipped_zero_area_model_box_is_safely_discarded():
    service = detector(
        FakeSession(
            boxes=[[320.0, 10.0, 320.0, 40.0]],
            scores=[0.99],
            labels=[1],
        )
    )

    assert service.detect(Image.new("RGB", (320, 320), color="white")) == []


@pytest.mark.parametrize(
    ("boxes", "scores", "labels", "message"),
    [
        (
            np.zeros((1, 1, 4), dtype=np.float32),
            np.asarray([0.9], dtype=np.float32),
            np.asarray([1], dtype=np.int64),
            "boxes shape",
        ),
        (
            np.asarray([[0.0, 0.0, np.nan, 10.0]], dtype=np.float32),
            np.asarray([0.9], dtype=np.float32),
            np.asarray([1], dtype=np.int64),
            "non-finite",
        ),
        (
            np.asarray([[0.0, 0.0, 10.0, 10.0]], dtype=np.float32),
            np.asarray([1.1], dtype=np.float32),
            np.asarray([1], dtype=np.int64),
            "between 0 and 1",
        ),
        (
            np.asarray([[0.0, 0.0, 321.0, 10.0]], dtype=np.float32),
            np.asarray([0.9], dtype=np.float32),
            np.asarray([1], dtype=np.int64),
            "letterboxed image",
        ),
        (
            np.asarray([[0.0, 0.0, 10.0, 10.0]], dtype=np.float32),
            np.asarray([0.9], dtype=np.float32),
            np.asarray([0], dtype=np.int64),
            "labels must all be 1",
        ),
    ],
)
def test_invalid_model_outputs_fail_closed(boxes, scores, labels, message):
    session = FakeSession()
    session.boxes = boxes
    session.scores = scores
    session.labels = labels
    service = detector(session)

    with pytest.raises(ValueError, match=message):
        service.detect(Image.new("RGB", (320, 320), color="white"))


def test_session_contract_and_configuration_are_exposed_in_metadata():
    service = detector(
        FakeSession(), min_score=0.4, nms_iou_threshold=0.3, max_candidates=7
    )

    metadata = service.metadata()

    assert metadata["input_shape"] == [1, 3, 320, 320]
    assert metadata["output_names"] == ["boxes", "scores", "labels"]
    assert metadata["class_count"] == 1
    assert metadata["foreground_label"] == 1
    assert metadata["min_score"] == 0.4
    assert metadata["nms_iou_threshold"] == 0.3
    assert metadata["max_candidates"] == 7
    assert "fill 114" in metadata["preprocessing"]


def test_unexpected_session_input_shape_is_rejected():
    with pytest.raises(ValueError, match="Unexpected detector input shape"):
        detector(FakeSession(input_shape=[None, 3, 320, 320]))
