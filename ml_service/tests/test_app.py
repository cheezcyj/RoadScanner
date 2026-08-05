from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from ml_service.app import (
    _parse_design_prototype_raw_match_classes,
    _parse_design_prototype_threshold_overrides,
    _parse_ood_threshold_overrides,
    create_app,
)
from ml_service.classifier import TrafficSignClassifier
from ml_service.detector import Detection
from ml_service.image_fetcher import ImageFetchError, ImageFetchUnavailable


ROOT = Path(__file__).resolve().parents[2]


class ArrayResult:
    def __init__(self, values):
        self.values = values

    def numpy(self):
        return self.values


class FakeModel:
    input_shape = (None, 30, 30, 3)
    output_shape = (None, 43)

    def __call__(self, batch, training=False):
        probabilities = np.zeros((1, 43), dtype=np.float32)
        probabilities[0, 33] = 0.99
        probabilities[0, 14] = 0.01
        return ArrayResult(probabilities)


class FakeFetcher:
    def fetch(self, image_url):
        if image_url == "https://allowed.test/sign.png":
            pixels = np.zeros((60, 60, 3), dtype=np.uint8)
            pixels[:, :30] = (20, 80, 220)
            pixels[:, 30:] = (240, 240, 240)
            return Image.fromarray(pixels)
        raise ImageFetchError("image_url host is not allowed")


class FakeDetector:
    def detect(self, image):
        return [Detection(5, 5, 55, 55, 0.97)]

    def metadata(self):
        return {
            "model_sha256": "test-double",
            "model_hash_verified": False,
            "min_score": 0.35,
        }


def test_java_compatible_predict_endpoint_returns_plain_result_id():
    classifier = TrafficSignClassifier(
        model_path=ROOT / "missing-test-model.h5",
        class_map_path=ROOT / "ml_service" / "class_map.json",
        model=FakeModel(),
    )
    app = create_app(
        {"TESTING": True, "PIPELINE_MODE": "crop"}, classifier, FakeFetcher()
    )

    response = app.test_client().post(
        "/predict", json={"image_url": "https://allowed.test/sign.png"}
    )

    assert response.status_code == 200
    assert response.content_type.startswith("text/plain")
    assert response.get_data(as_text=True) == "34"
    assert (
        response.headers["X-RoadScanner-Catalog-SHA256"]
        == classifier.class_map_sha256
    )


def test_debug_endpoint_exposes_confidence_and_model_contract():
    classifier = TrafficSignClassifier(
        model_path=ROOT / "missing-test-model.h5",
        class_map_path=ROOT / "ml_service" / "class_map.json",
        model=FakeModel(),
    )
    app = create_app(
        {"TESTING": True, "PIPELINE_MODE": "crop"}, classifier, FakeFetcher()
    )

    response = app.test_client().post(
        "/predict/debug", json={"image_url": "https://allowed.test/sign.png"}
    )
    body = response.get_json()

    assert response.status_code == 200
    assert body["class_id"] == 33
    assert body["result_id"] == 34
    assert body["key"] == "turn_right_ahead"
    assert body["accepted"] is True
    assert body["prediction_source"] == "recovered_cnn"
    assert body["raw_class_id"] == 33
    assert body["base_reason"] == "ok"
    assert body["prototype_similarity"] is None
    assert body["prototype_margin"] is None
    assert "alpha composite on opaque white" in body["preprocessing"]
    assert body["pipeline_mode"] == "crop"
    assert body["detector_enabled"] is False


def test_predict_rejects_missing_or_disallowed_image_url():
    classifier = TrafficSignClassifier(
        model_path=ROOT / "missing-test-model.h5",
        class_map_path=ROOT / "ml_service" / "class_map.json",
        model=FakeModel(),
    )
    app = create_app(
        {"TESTING": True, "PIPELINE_MODE": "crop"}, classifier, FakeFetcher()
    )
    client = app.test_client()

    assert client.post("/predict", json={}).status_code == 400
    assert (
        client.post("/predict", json={"image_url": "https://blocked.test/a.png"}).status_code
        == 422
    )


def test_predict_reports_transient_image_fetch_failure_as_503():
    class UnavailableFetcher:
        def fetch(self, image_url):
            raise ImageFetchUnavailable("image download timed out")

    classifier = TrafficSignClassifier(
        model_path=ROOT / "missing-test-model.h5",
        class_map_path=ROOT / "ml_service" / "class_map.json",
        model=FakeModel(),
    )
    app = create_app(
        {"TESTING": True, "PIPELINE_MODE": "crop"},
        classifier,
        UnavailableFetcher(),
    )

    response = app.test_client().post(
        "/predict", json={"image_url": "https://allowed.test/sign.png"}
    )

    assert response.status_code == 503
    assert response.get_json() == {"error": "image download timed out"}


def test_detect_pipeline_keeps_plain_contract_and_exposes_debug_metadata():
    classifier = TrafficSignClassifier(
        model_path=ROOT / "missing-test-model.h5",
        class_map_path=ROOT / "ml_service" / "class_map.json",
        model=FakeModel(),
    )
    app = create_app(
        {"TESTING": True, "PIPELINE_MODE": "detect"},
        classifier,
        FakeFetcher(),
        FakeDetector(),
    )
    client = app.test_client()

    response = client.post(
        "/predict", json={"image_url": "https://allowed.test/sign.png"}
    )
    debug = client.post(
        "/predict/debug", json={"image_url": "https://allowed.test/sign.png"}
    ).get_json()
    health = client.get("/health").get_json()

    assert response.status_code == 200
    assert response.get_data(as_text=True) == "34"
    assert response.headers["X-RoadScanner-Catalog-SHA256"] == (
        classifier.class_map_sha256
    )
    assert debug["pipeline_mode"] == "detect"
    assert debug["detection_count"] == 1
    assert debug["candidates"][0]["detection"]["score"] == 0.97
    assert health["status"] == "ready"
    assert health["detector_enabled"] is True
    assert health["detector"]["model_sha256"] == "test-double"


def test_default_pipeline_uses_the_pinned_whole_scene_detector_path():
    classifier = TrafficSignClassifier(
        model_path=ROOT / "missing-test-model.h5",
        class_map_path=ROOT / "ml_service" / "class_map.json",
        model=FakeModel(),
    )
    app = create_app(
        {"TESTING": True},
        classifier,
        FakeFetcher(),
        FakeDetector(),
    )

    health = app.test_client().get("/health").get_json()

    assert health["pipeline_mode"] == "detect"
    assert health["detector_enabled"] is True
    assert app.config["DETECTOR_MIN_SCORE"] == 0.7
    assert app.config["DETECTOR_MAX_CANDIDATES"] == 10


def test_default_priority_road_override_is_exposed_by_health(monkeypatch):
    monkeypatch.delenv("ROADSCANNER_OOD_THRESHOLD_OVERRIDES", raising=False)
    classifier = TrafficSignClassifier(
        model_path=ROOT / "missing-test-model.h5",
        class_map_path=ROOT / "ml_service" / "class_map.json",
        model=FakeModel(),
    )
    app = create_app(
        {"TESTING": True, "PIPELINE_MODE": "crop"}, classifier, FakeFetcher()
    )

    health = app.test_client().get("/health").get_json()

    assert app.config["OOD_SAFETY_MARGIN"] == 0.07
    assert app.config["OOD_THRESHOLD_OVERRIDES"] == {12: 0.77}
    assert health["ood_safety_margin"] == 0.07
    assert health["ood_threshold_overrides"] == {"12": 0.77}
    assert app.config["DESIGN_PROTOTYPE_ENABLED"] is True
    assert app.config["DESIGN_PROTOTYPE_MIN_SIMILARITY"] == 0.80
    assert app.config["DESIGN_PROTOTYPE_MIN_MARGIN"] == 0.15
    assert app.config["DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES"] == {33: 0.50}
    assert app.config["DESIGN_PROTOTYPE_RAW_MATCH_CLASSES"] == frozenset({33})


def test_ood_threshold_override_environment_format_is_parsed():
    assert _parse_ood_threshold_overrides("12:0.77, 33:0.72") == {
        12: 0.77,
        33: 0.72,
    }
    assert _parse_ood_threshold_overrides("  ") == {}


@pytest.mark.parametrize(
    "value",
    [
        "12",
        "priority:0.77",
        "12:not-a-number",
        "43:0.77",
        "12:1.1",
        "12:nan",
        "12:0.77,12:0.78",
    ],
)
def test_invalid_ood_threshold_override_environment_fails_closed(value):
    with pytest.raises(ValueError, match="ROADSCANNER_OOD_THRESHOLD_OVERRIDES"):
        _parse_ood_threshold_overrides(value)


def test_design_prototype_environment_formats_are_parsed():
    assert _parse_design_prototype_threshold_overrides("33:0.50, 12:0.80") == {
        33: 0.50,
        12: 0.80,
    }
    assert _parse_design_prototype_threshold_overrides("  ") == {}
    assert _parse_design_prototype_raw_match_classes("33, 12") == frozenset(
        {12, 33}
    )
    assert _parse_design_prototype_raw_match_classes("  ") == frozenset()


@pytest.mark.parametrize(
    "value",
    ["33", "right:0.5", "33:nan", "43:0.5", "33:1.1", "33:0.5,33:0.6"],
)
def test_invalid_design_prototype_threshold_environment_fails_closed(value):
    with pytest.raises(
        ValueError, match="ROADSCANNER_DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES"
    ):
        _parse_design_prototype_threshold_overrides(value)


@pytest.mark.parametrize("value", ["right", "43", "33,,12", "33,33"])
def test_invalid_design_prototype_raw_match_environment_fails_closed(value):
    with pytest.raises(
        ValueError, match="ROADSCANNER_DESIGN_PROTOTYPE_RAW_MATCH_CLASSES"
    ):
        _parse_design_prototype_raw_match_classes(value)


def test_detect_pipeline_requires_a_pinned_detector_artifact():
    classifier = TrafficSignClassifier(
        model_path=ROOT / "missing-test-model.h5",
        class_map_path=ROOT / "ml_service" / "class_map.json",
        model=FakeModel(),
    )

    try:
        create_app(
            {
                "TESTING": True,
                "PIPELINE_MODE": "detect",
                "EXPECTED_DETECTOR_MODEL_SHA256": "",
            },
            classifier,
            FakeFetcher(),
        )
    except ValueError as error:
        assert "EXPECTED_DETECTOR_MODEL_SHA256" in str(error)
    else:
        raise AssertionError("detect mode accepted an unpinned detector artifact")


def test_app_passes_image_decode_limits_to_default_fetcher():
    classifier = TrafficSignClassifier(
        model_path=ROOT / "missing-test-model.h5",
        class_map_path=ROOT / "ml_service" / "class_map.json",
        model=FakeModel(),
    )

    app = create_app(
        {
            "TESTING": True,
            "MAX_IMAGE_PIXELS": 1_234_567,
            "MAX_IMAGE_DIMENSION": 4_321,
            "PIPELINE_MODE": "crop",
        },
        classifier,
    )
    fetcher = app.extensions["roadscanner_image_fetcher"]

    assert fetcher.max_pixels == 1_234_567
    assert fetcher.max_dimension == 4_321
