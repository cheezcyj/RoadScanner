from types import SimpleNamespace

import numpy as np
from PIL import Image

from ml_service.classifier import Prediction
from ml_service.detector import Detection
from ml_service.pipeline import TrafficSignPipeline


UNKNOWN = {
    "result_id": 44,
    "key": "unknown",
    "name_en": "Unknown",
    "name_ko": "알 수 없음",
}


def prediction(class_id=33, result_id=34, accepted=True, reason="ok"):
    return Prediction(
        class_id=class_id,
        result_id=result_id if accepted else 44,
        key=f"class_{class_id}",
        name_en=f"Class {class_id}",
        name_ko=f"분류 {class_id}",
        confidence=0.99 if accepted else 0.5,
        margin=0.98 if accepted else 0.01,
        ood_similarity=None,
        ood_threshold=None,
        accepted=accepted,
        reason=reason,
        top3=[],
    )


class FakeClassifier:
    def __init__(self, predictions):
        self.predictions = list(predictions)
        self.class_map = SimpleNamespace(unknown_result_id=44, unknown=UNKNOWN)
        self.predict_calls = []
        self.predict_many_calls = []

    def predict(self, image):
        self.predict_calls.append(image)
        return self.predictions[0]

    def predict_many(self, images):
        self.predict_many_calls.append(images)
        return self.predictions[: len(images)]


class FakeDetector:
    def __init__(self, detections):
        self.detections = list(detections)
        self.calls = []

    def detect(self, image):
        self.calls.append(image)
        return self.detections

    def metadata(self):
        return {"model_sha256": "test-double", "min_score": 0.35}


def test_crop_mode_delegates_to_recovered_classifier_without_detector():
    expected = prediction()
    classifier = FakeClassifier([expected])
    pipeline = TrafficSignPipeline(classifier, mode="crop")
    source = Image.new("RGB", (80, 60))

    result = pipeline.analyze(source)

    assert result.result_id == 34
    assert result.prediction is expected
    assert len(classifier.predict_calls) == 1
    assert classifier.predict_calls[0].mode == "RGB"
    assert classifier.predict_calls[0].size == source.size
    assert classifier.predict_many_calls == []
    assert result.to_dict()["pipeline_mode"] == "crop"


def test_detect_mode_requires_a_detector():
    classifier = FakeClassifier([prediction()])

    try:
        TrafficSignPipeline(classifier, mode="detect")
    except ValueError as error:
        assert "requires" in str(error)
    else:
        raise AssertionError("detect mode accepted a missing detector")


def test_no_detection_returns_unknown_without_running_classifier():
    classifier = FakeClassifier([prediction()])
    pipeline = TrafficSignPipeline(classifier, "detect", FakeDetector([]))

    result = pipeline.analyze(Image.new("RGB", (200, 120)))

    assert result.result_id == 44
    assert result.accepted is False
    assert result.reason == "no_sign_detected"
    assert classifier.predict_calls == []
    assert classifier.predict_many_calls == []
    assert result.to_dict()["class_id"] is None


def test_pipeline_normalization_ignores_hidden_rgb_before_detector():
    detector = FakeDetector([])
    pipeline = TrafficSignPipeline(
        FakeClassifier([prediction()]),
        "detect",
        detector,
        allow_crop_fallback=False,
    )
    first = Image.new("RGBA", (80, 60), color=(255, 0, 0, 0))
    second = Image.new("RGBA", (80, 60), color=(0, 0, 255, 0))
    first.putpixel((20, 20), (10, 200, 30, 255))
    second.putpixel((20, 20), (10, 200, 30, 255))

    pipeline.analyze(first)
    pipeline.analyze(second)

    assert all(image.mode == "RGB" for image in detector.calls)
    np.testing.assert_array_equal(
        np.asarray(detector.calls[0]), np.asarray(detector.calls[1])
    )


def test_square_single_sign_can_use_guarded_crop_fallback():
    expected = prediction()
    classifier = FakeClassifier([expected])
    pipeline = TrafficSignPipeline(classifier, "detect", FakeDetector([]))

    result = pipeline.analyze(Image.new("RGB", (100, 90)))

    assert result.result_id == 34
    assert result.accepted is True
    assert result.prediction is expected
    assert result.crop_fallback_attempted is True
    assert result.crop_fallback_used is True
    assert result.to_dict()["detection_count"] == 0


def test_rejected_crop_fallback_stays_unknown_and_is_visible_in_debug():
    classifier = FakeClassifier(
        [prediction(accepted=False, reason="out_of_distribution")]
    )
    pipeline = TrafficSignPipeline(classifier, "detect", FakeDetector([]))

    result = pipeline.analyze(Image.new("RGB", (100, 100)))

    assert result.result_id == 44
    assert result.reason == "no_sign_detected"
    assert result.crop_fallback_attempted is True
    assert result.crop_fallback_used is False


def test_detect_mode_uses_square_crop_with_ten_percent_padding():
    classifier = FakeClassifier([prediction()])
    detector = FakeDetector([Detection(20, 30, 60, 50, 0.9)])
    pipeline = TrafficSignPipeline(classifier, "detect", detector)

    result = pipeline.analyze(Image.new("RGB", (200, 120)))

    assert result.result_id == 34
    assert result.accepted is True
    crop = classifier.predict_many_calls[0][0]
    assert crop.size == (48, 48)
    assert result.candidates[0].crop_box == (16, 16, 64, 64)


def test_multiple_candidates_are_accepted_only_when_all_agree():
    detections = [
        Detection(10, 10, 30, 30, 0.95),
        Detection(60, 20, 85, 45, 0.90),
    ]
    classifier = FakeClassifier([prediction(), prediction()])
    pipeline = TrafficSignPipeline(classifier, "detect", FakeDetector(detections))

    result = pipeline.analyze(Image.new("RGB", (120, 80)))

    assert result.result_id == 34
    assert result.accepted is True
    assert result.reason == "ok"
    assert result.to_dict()["detection_count"] == 2


def test_conflicting_accepted_candidates_return_ambiguous_unknown():
    detections = [
        Detection(10, 10, 30, 30, 0.95),
        Detection(60, 20, 85, 45, 0.90),
    ]
    classifier = FakeClassifier(
        [prediction(class_id=33, result_id=34), prediction(class_id=14, result_id=15)]
    )
    pipeline = TrafficSignPipeline(classifier, "detect", FakeDetector(detections))

    result = pipeline.analyze(Image.new("RGB", (120, 80)))

    assert result.result_id == 44
    assert result.accepted is False
    assert result.reason == "ambiguous"
    assert result.to_dict()["class_id"] is None


def test_mixed_acceptance_is_ambiguous_and_all_rejected_is_unknown():
    detections = [
        Detection(10, 10, 30, 30, 0.95),
        Detection(60, 20, 85, 45, 0.90),
    ]
    image = Image.new("RGB", (120, 80))

    mixed = TrafficSignPipeline(
        FakeClassifier(
            [prediction(), prediction(accepted=False, reason="low_confidence")]
        ),
        "detect",
        FakeDetector(detections),
    ).analyze(image)
    rejected = TrafficSignPipeline(
        FakeClassifier(
            [
                prediction(accepted=False, reason="low_confidence"),
                prediction(accepted=False, reason="out_of_distribution"),
            ]
        ),
        "detect",
        FakeDetector(detections),
    ).analyze(image)

    assert (mixed.result_id, mixed.reason) == (44, "ambiguous")
    assert (rejected.result_id, rejected.reason) == (
        44,
        "all_candidates_rejected",
    )


def test_detect_metadata_nests_detector_contract():
    pipeline = TrafficSignPipeline(
        FakeClassifier([prediction()]), "detect", FakeDetector([])
    )

    metadata = pipeline.metadata()

    assert metadata["pipeline_mode"] == "detect"
    assert metadata["detector_enabled"] is True
    assert metadata["detector"]["model_sha256"] == "test-double"
    assert metadata["selection_policy"] == "strict_candidate_consensus"
    assert metadata["crop_fallback_enabled"] is True
    assert metadata["crop_fallback_max_aspect"] == 1.25
