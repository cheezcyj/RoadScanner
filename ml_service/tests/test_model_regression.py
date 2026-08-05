import json
from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from ml_service.app import create_app
from ml_service.classifier import (
    PREDICTION_SOURCE_PROTOTYPE,
    RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256,
    RECOVERED_DESIGN_PROTOTYPE_SHA256,
    TrafficSignClassifier,
)


ROOT = Path(__file__).resolve().parents[2]
MODEL = ROOT / "road_scanner.h5"
DEMO = ROOT / "docs" / "demo" / "upload-recognition.gif"
OOD_REFERENCE = ROOT / "ml_service" / "ood_reference.npz"
DETECTOR = ROOT / "traffic_sign_detector.onnx"
PRIORITY_ROAD = ROOT / "data" / "manual-validation" / "positive" / "15.jpg"
DESIGN_PROTOTYPE = ROOT / "ml_service" / "design_prototypes.npz"
DESIGN_PROTOTYPE_METADATA = ROOT / "ml_service" / "design_prototypes.json"
CANONICAL_DESIGNS = (
    ROOT
    / "data"
    / "manual-validation"
    / "positive"
    / "gtsrb-design"
    / "upload-ready-transparent"
)
MANUAL_POSITIVE = ROOT / "data" / "manual-validation" / "positive"
MANUAL_UNKNOWN = ROOT / "data" / "manual-validation" / "unknown"
OPEN_IMAGES_MANIFEST = ROOT / "data" / "detector" / "open-images-v7" / "manifest.json"


@pytest.mark.skipif(
    not MODEL.is_file() or not DEMO.is_file() or not OOD_REFERENCE.is_file(),
    reason="local recovery artifacts absent",
)
def test_recovered_model_matches_the_original_turn_right_demo():
    demo = Image.open(DEMO)
    demo.seek(30)
    sign_crop = demo.convert("RGB").crop((94, 153, 304, 363))
    classifier = TrafficSignClassifier(
        model_path=MODEL,
        class_map_path=ROOT / "ml_service" / "class_map.json",
        ood_reference_path=OOD_REFERENCE,
        max_crop_aspect=1.45,
        min_visual_std=0.01,
    )

    prediction = classifier.predict(sign_crop)

    assert prediction.class_id == 33
    assert prediction.result_id == 34
    assert prediction.key == "turn_right_ahead"
    assert prediction.accepted is True
    assert prediction.confidence > 0.99


@pytest.mark.skipif(
    not all(
        path.is_file()
        for path in (
            MODEL,
            OOD_REFERENCE,
            DETECTOR,
            PRIORITY_ROAD,
            DESIGN_PROTOTYPE,
            DESIGN_PROTOTYPE_METADATA,
        )
    ),
    reason="local detector or priority-road regression artifact absent",
)
def test_detect_pipeline_accepts_the_canonical_priority_road_icon():
    app = create_app({"TESTING": True})

    result = app.extensions["roadscanner_pipeline"].analyze(Image.open(PRIORITY_ROAD))
    payload = result.to_dict()

    assert payload["accepted"] is True
    assert payload["result_id"] == 13
    assert payload["class_id"] == 12
    assert payload["key"] == "priority_road"
    assert payload["ood_threshold"] == pytest.approx(0.77)
    assert payload["ood_similarity"] >= payload["ood_threshold"]
    assert payload["detection_count"] == 1


@pytest.mark.skipif(
    not MODEL.is_file() or not OOD_REFERENCE.is_file(),
    reason="local recovery artifacts absent",
)
def test_recovered_model_rejects_repository_non_sign_images():
    classifier = TrafficSignClassifier(
        model_path=MODEL,
        class_map_path=ROOT / "ml_service" / "class_map.json",
        ood_reference_path=OOD_REFERENCE,
    )
    paths = [
        ROOT / "src/main/webapp/resources/picture/bg01.jpg",
        ROOT / "src/main/webapp/resources/picture/start.jpg",
        ROOT / "src/main/webapp/resources/img/thumbsdown.jpg",
    ]

    predictions = classifier.predict_many([Image.open(path) for path in paths])

    assert all(prediction.accepted is False for prediction in predictions)
    assert all(prediction.result_id == 44 for prediction in predictions)
    assert [prediction.reason for prediction in predictions] == [
        "sign_crop_required",
        "sign_crop_required",
        "out_of_distribution",
    ]


@pytest.mark.skipif(
    not MODEL.is_file() or not OOD_REFERENCE.is_file(),
    reason="local recovery artifacts absent",
)
def test_recovered_model_rejects_seeded_solid_and_noise_stress_inputs():
    classifier = TrafficSignClassifier(
        model_path=MODEL,
        class_map_path=ROOT / "ml_service" / "class_map.json",
        ood_reference_path=OOD_REFERENCE,
    )
    random = np.random.default_rng(20260802)
    colors = random.integers(0, 256, (256, 3), dtype=np.uint8)
    images = [
        Image.new("RGB", (64, 64), tuple(int(channel) for channel in color))
        for color in colors
    ]
    images.extend(
        Image.fromarray(random.integers(0, 256, (64, 64, 3), dtype=np.uint8))
        for _ in range(256)
    )

    predictions = classifier.predict_many(images)

    assert all(prediction.accepted is False for prediction in predictions)
    assert all(prediction.result_id == 44 for prediction in predictions)


def _prediction_sources(result) -> list[str]:
    sources = []
    if result.prediction is not None:
        sources.append(result.prediction.prediction_source)
    sources.extend(
        candidate.prediction.prediction_source for candidate in result.candidates
    )
    return sources


@pytest.mark.skipif(
    not all(
        path.exists()
        for path in (
            MODEL,
            OOD_REFERENCE,
            DETECTOR,
            DESIGN_PROTOTYPE,
            DESIGN_PROTOTYPE_METADATA,
            CANONICAL_DESIGNS,
            MANUAL_POSITIVE,
            MANUAL_UNKNOWN,
            OPEN_IMAGES_MANIFEST,
        )
    ),
    reason="local canonical-design regression corpus absent",
)
def test_rejected_only_design_prototype_regression_corpus():
    app = create_app({"TESTING": True})
    pipeline = app.extensions["roadscanner_pipeline"]
    classifier = app.extensions["roadscanner_classifier"]
    health = app.test_client().get("/health").get_json()

    assert health["design_prototype_enabled"] is True
    assert health["design_prototype_hash_verified"] is True
    assert health["design_prototype_metadata_hash_verified"] is True
    assert health["design_prototype_sha256"] == RECOVERED_DESIGN_PROTOTYPE_SHA256
    assert health["design_prototype_metadata_sha256"] == (
        RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256
    )
    assert health["design_prototype_source_variant"] == "upload-ready-transparent"
    assert health["design_prototype_policy"] == "rejected_predictions_only"
    assert health["design_prototype_min_similarity"] == pytest.approx(0.80)
    assert health["design_prototype_min_margin"] == pytest.approx(0.15)
    assert health["design_prototype_threshold_overrides"] == {"33": 0.50}
    assert health["design_prototype_raw_match_classes"] == [33]

    canonical_paths = sorted(CANONICAL_DESIGNS.glob("gtsrb_*.png"))
    assert len(canonical_paths) == 43
    canonical_prototype_accepts = 0
    for path in canonical_paths:
        expected = int(path.name.split("_")[1])
        with Image.open(path) as image:
            result = pipeline.analyze(image)
        assert result.accepted is True, path.name
        assert result.prediction is not None
        assert result.prediction.class_id == expected, path.name
        assert result.result_id == expected + 1, path.name
        canonical_prototype_accepts += int(
            PREDICTION_SOURCE_PROTOTYPE in _prediction_sources(result)
        )
    assert canonical_prototype_accepts > 0

    expected_by_name = {
        "10.jpg": 32,
        "12.jpg": 13,
        "15.jpg": 12,
        "21.jpg": 15,
        "22.jpg": 9,
        "7-1.jpg": 40,
    }
    manual_paths = sorted(MANUAL_POSITIVE.glob("*.jpg"))
    assert len(manual_paths) == 7
    for path in manual_paths:
        expected = expected_by_name.get(path.name, 33)
        with Image.open(path) as image:
            result = pipeline.analyze(image)
        assert result.accepted is True, path.name
        assert result.prediction is not None
        assert result.prediction.class_id == expected, path.name
        assert result.result_id == expected + 1, path.name

    unknown_paths = sorted(MANUAL_UNKNOWN.glob("*.jpg"))
    assert len(unknown_paths) == 2
    for path in unknown_paths:
        with Image.open(path) as image:
            result = pipeline.analyze(image)
        assert result.accepted is False, path.name
        assert result.result_id == classifier.class_map.unknown_result_id
        assert PREDICTION_SOURCE_PROTOTYPE not in _prediction_sources(result)

    manifest = json.loads(OPEN_IMAGES_MANIFEST.read_text(encoding="utf-8"))
    negative_paths = []
    for split in manifest["splits"].values():
        for item in split["images"]:
            if item.get("status") == "downloaded" and item.get("is_negative"):
                negative_paths.append(ROOT / item["path"])
    assert len(negative_paths) == 286
    prototype_negative_accepts = []
    for path in negative_paths:
        with Image.open(path) as image:
            result = pipeline.analyze(image)
        if PREDICTION_SOURCE_PROTOTYPE in _prediction_sources(result):
            prototype_negative_accepts.append(path.name)
    assert prototype_negative_accepts == []
