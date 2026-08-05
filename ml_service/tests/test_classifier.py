import hashlib
import json
import shutil
import uuid
from pathlib import Path

import numpy as np
import pytest
from PIL import Image

from ml_service.classifier import (
    PREDICTION_SOURCE_MODEL,
    PREDICTION_SOURCE_PROTOTYPE,
    PREDICTION_SOURCE_REJECTED,
    ClassMap,
    TrafficSignClassifier,
)


ROOT = Path(__file__).resolve().parents[2]
CLASS_MAP = ROOT / "ml_service" / "class_map.json"
OOD_REFERENCE = ROOT / "ml_service" / "ood_reference.npz"


@pytest.fixture
def prototype_reference():
    directory = ROOT / "data" / "test-design-prototypes" / uuid.uuid4().hex
    directory.mkdir(parents=True)
    artifact = directory / "design_prototypes.npz"
    metadata = directory / "design_prototypes.json"
    prototypes = np.zeros((43, 512), dtype=np.float32)
    prototypes[np.arange(43), np.arange(43)] = 1.0
    np.savez(
        artifact,
        prototypes=prototypes,
        class_ids=np.arange(43, dtype=np.int64),
    )
    artifact_sha256 = hashlib.sha256(artifact.read_bytes()).hexdigest().upper()
    metadata.write_text(
        json.dumps(
            {
                "schema_version": 1,
                "artifact": artifact.name,
                "artifact_sha256": artifact_sha256,
                "shape": [43, 512],
                "dtype": "float32",
                "row_l2_normalized": True,
                "feature_layer": "batch_normalization_2",
                "model_sha256": "test-double",
                "class_map_sha256": TrafficSignClassifier._sha256(CLASS_MAP),
                "source_lock_sha256": "A" * 64,
                "source_variant": "upload-ready-transparent",
                "classes": [{"class_id": class_id} for class_id in range(43)],
            }
        ),
        encoding="utf-8",
    )
    try:
        yield artifact, metadata, prototypes
    finally:
        shutil.rmtree(directory, ignore_errors=True)


class ArrayResult:
    def __init__(self, values):
        self.values = values

    def numpy(self):
        return self.values


class FakeModel:
    input_shape = (None, 30, 30, 3)
    output_shape = (None, 43)

    def __init__(self, probabilities):
        self.probabilities = np.asarray([probabilities], dtype=np.float32)
        self.last_batch = None

    def __call__(self, batch, training=False):
        assert training is False
        self.last_batch = np.asarray(batch)
        return ArrayResult(self.probabilities)


class FakeFeatureModel:
    def __init__(self, values):
        self.values = np.asarray([values], dtype=np.float32)
        self.output_shape = (None, self.values.shape[1])

    def __call__(self, batch, training=False):
        assert training is False
        return ArrayResult(np.repeat(self.values, len(batch), axis=0))


def probabilities(winner=33, confidence=0.95, runner_up=14, runner_confidence=0.04):
    values = np.full(43, (1.0 - confidence - runner_confidence) / 41, dtype=np.float32)
    values[winner] = confidence
    values[runner_up] = runner_confidence
    return values


def classifier(model, **kwargs):
    kwargs.setdefault("min_visual_std", 0.0)
    return TrafficSignClassifier(
        model_path=ROOT / "missing-test-model.h5",
        class_map_path=CLASS_MAP,
        model=model,
        **kwargs,
    )


def ood_feature_at_similarity(class_id, target_similarity):
    with np.load(OOD_REFERENCE, allow_pickle=False) as payload:
        centroid = np.asarray(payload["centroids"][class_id], dtype=np.float32)
    centroid /= np.linalg.norm(centroid)
    orthogonal = np.zeros_like(centroid)
    orthogonal[int(np.argmin(np.abs(centroid)))] = 1.0
    orthogonal -= float(orthogonal @ centroid) * centroid
    orthogonal /= np.linalg.norm(orthogonal)
    return (
        target_similarity * centroid
        + np.sqrt(1.0 - target_similarity**2) * orthogonal
    )


def test_class_map_is_complete_and_explicit():
    mapping = ClassMap(CLASS_MAP)

    assert mapping.class_count == 43
    assert mapping[0].result_id == 1
    assert mapping[33].key == "turn_right_ahead"
    assert mapping[33].result_id == 34
    assert mapping[42].result_id == 43
    assert mapping.unknown_result_id == 44


def test_preprocessing_matches_recovered_bgr_unit_range_contract():
    model = FakeModel(probabilities())
    service = classifier(model)
    image = Image.new("RGB", (40, 40), color=(10, 20, 30))

    prediction = service.predict(image)

    assert prediction.class_id == 33
    assert prediction.result_id == 34
    assert prediction.accepted is True
    assert model.last_batch.shape == (1, 30, 30, 3)
    np.testing.assert_allclose(
        model.last_batch[0, 0, 0], np.asarray([30, 20, 10]) / 255.0, rtol=1e-6
    )


def test_low_confidence_uses_unknown_result_instead_of_forcing_a_match():
    model = FakeModel(probabilities(confidence=0.40, runner_confidence=0.30))
    service = classifier(model, min_confidence=0.85, min_margin=0.15)

    prediction = service.predict(Image.new("RGB", (40, 40), color="red"))

    assert prediction.accepted is False
    assert prediction.reason == "low_confidence"
    assert prediction.result_id == 44


def test_wide_road_scene_requires_a_sign_crop_even_with_high_confidence():
    model = FakeModel(probabilities())
    service = classifier(model, max_crop_aspect=1.5)

    prediction = service.predict(Image.new("RGB", (160, 90), color="white"))

    assert prediction.accepted is False
    assert prediction.reason == "sign_crop_required"
    assert prediction.result_id == 44


def test_tall_official_style_sign_crop_is_not_rejected_by_aspect_ratio():
    model = FakeModel(probabilities())
    service = classifier(model, max_crop_aspect=1.5)

    prediction = service.predict(Image.new("RGB", (29, 55), color="white"))

    assert prediction.accepted is True
    assert prediction.reason == "ok"
    assert prediction.result_id == 34


def test_out_of_distribution_features_use_unknown_result():
    service = classifier(
        FakeModel(probabilities()),
        ood_reference_path=OOD_REFERENCE,
        feature_model=FakeFeatureModel(np.zeros(512, dtype=np.float32)),
    )

    prediction = service.predict(Image.new("RGB", (40, 40), color="white"))

    assert prediction.accepted is False
    assert prediction.reason == "out_of_distribution"
    assert prediction.result_id == 44
    assert prediction.ood_similarity == 0.0


def test_ood_safety_margin_rejects_a_near_boundary_feature():
    with np.load(OOD_REFERENCE, allow_pickle=False) as payload:
        centroid = np.asarray(payload["centroids"][33], dtype=np.float32)
        base_threshold = float(payload["thresholds"][33])
    centroid /= np.linalg.norm(centroid)
    orthogonal = np.zeros_like(centroid)
    orthogonal[int(np.argmin(np.abs(centroid)))] = 1.0
    orthogonal -= float(orthogonal @ centroid) * centroid
    orthogonal /= np.linalg.norm(orthogonal)
    target_similarity = base_threshold + 0.035
    feature = (
        target_similarity * centroid
        + np.sqrt(1.0 - target_similarity**2) * orthogonal
    )
    image = Image.new("RGB", (40, 40), color="white")

    permissive = classifier(
        FakeModel(probabilities()),
        ood_reference_path=OOD_REFERENCE,
        feature_model=FakeFeatureModel(feature),
        ood_safety_margin=0.0,
    ).predict(image)
    guarded = classifier(
        FakeModel(probabilities()),
        ood_reference_path=OOD_REFERENCE,
        feature_model=FakeFeatureModel(feature),
        ood_safety_margin=0.07,
    ).predict(image)

    assert permissive.accepted is True
    assert guarded.accepted is False
    assert guarded.reason == "out_of_distribution"
    assert guarded.ood_threshold == pytest.approx(base_threshold + 0.07)


def test_priority_road_uses_its_absolute_ood_threshold_override():
    feature = ood_feature_at_similarity(12, 0.78)
    service = classifier(
        FakeModel(probabilities(winner=12)),
        ood_reference_path=OOD_REFERENCE,
        feature_model=FakeFeatureModel(feature),
        ood_safety_margin=0.07,
    )

    prediction = service.predict(Image.new("RGB", (40, 40), color="white"))

    assert prediction.class_id == 12
    assert prediction.accepted is True
    assert prediction.reason == "ok"
    assert prediction.ood_threshold == pytest.approx(0.77)
    assert service.metadata()["ood_threshold_overrides"] == {"12": 0.77}


def test_classes_without_an_override_keep_the_global_ood_safety_margin():
    with np.load(OOD_REFERENCE, allow_pickle=False) as payload:
        base_threshold = float(payload["thresholds"][33])
    feature = ood_feature_at_similarity(33, base_threshold + 0.05)
    service = classifier(
        FakeModel(probabilities(winner=33)),
        ood_reference_path=OOD_REFERENCE,
        feature_model=FakeFeatureModel(feature),
        ood_safety_margin=0.07,
    )

    prediction = service.predict(Image.new("RGB", (40, 40), color="white"))

    assert prediction.accepted is False
    assert prediction.reason == "out_of_distribution"
    assert prediction.ood_threshold == pytest.approx(base_threshold + 0.07)


def test_design_prototype_does_not_override_an_accepted_model_prediction(
    prototype_reference,
):
    artifact, metadata, prototypes = prototype_reference
    service = classifier(
        FakeModel(probabilities(winner=9)),
        feature_model=FakeFeatureModel(prototypes[15]),
        design_prototype_path=artifact,
        design_prototype_metadata_path=metadata,
    )

    prediction = service.predict(Image.new("RGB", (40, 40), color="white"))

    assert prediction.accepted is True
    assert prediction.class_id == 9
    assert prediction.raw_class_id == 9
    assert prediction.prediction_source == PREDICTION_SOURCE_MODEL
    assert prediction.prototype_similarity is None


def test_rejected_prediction_can_use_a_high_margin_design_prototype(
    prototype_reference,
):
    artifact, metadata, prototypes = prototype_reference
    service = classifier(
        FakeModel(probabilities(winner=9, confidence=0.40, runner_confidence=0.30)),
        feature_model=FakeFeatureModel(prototypes[15]),
        design_prototype_path=artifact,
        design_prototype_metadata_path=metadata,
    )

    prediction = service.predict(Image.new("RGB", (40, 40), color="white"))

    assert prediction.accepted is True
    assert prediction.result_id == 16
    assert prediction.class_id == 15
    assert prediction.raw_class_id == 9
    assert prediction.base_reason == "low_confidence"
    assert prediction.reason == "ok"
    assert prediction.prediction_source == PREDICTION_SOURCE_PROTOTYPE
    assert prediction.prototype_class_id == 15
    assert prediction.prototype_similarity == pytest.approx(1.0)
    assert prediction.prototype_margin == pytest.approx(1.0)
    assert prediction.prototype_threshold == pytest.approx(0.80)


def test_turn_right_prototype_requires_the_raw_model_to_agree(
    prototype_reference,
):
    artifact, metadata, prototypes = prototype_reference
    common = {
        "feature_model": FakeFeatureModel(prototypes[33]),
        "design_prototype_path": artifact,
        "design_prototype_metadata_path": metadata,
    }
    mismatch = classifier(
        FakeModel(
            probabilities(winner=32, confidence=0.40, runner_up=33, runner_confidence=0.30)
        ),
        **common,
    ).predict(Image.new("RGB", (40, 40), color="white"))
    agreement = classifier(
        FakeModel(
            probabilities(winner=33, confidence=0.40, runner_up=32, runner_confidence=0.30)
        ),
        **common,
    ).predict(Image.new("RGB", (40, 40), color="white"))

    assert mismatch.accepted is False
    assert mismatch.prediction_source == PREDICTION_SOURCE_REJECTED
    assert mismatch.prototype_class_id == 33
    assert mismatch.prototype_similarity == pytest.approx(1.0)
    assert mismatch.prototype_threshold == pytest.approx(0.50)
    assert agreement.accepted is True
    assert agreement.class_id == 33
    assert agreement.prediction_source == PREDICTION_SOURCE_PROTOTYPE


def test_design_prototype_margin_and_structural_guards_fail_closed(
    prototype_reference,
):
    artifact, metadata, prototypes = prototype_reference
    ambiguous_feature = (prototypes[9] + prototypes[15]) / np.sqrt(2.0)
    common = {
        "feature_model": FakeFeatureModel(ambiguous_feature),
        "design_prototype_path": artifact,
        "design_prototype_metadata_path": metadata,
    }
    ambiguous = classifier(
        FakeModel(probabilities(winner=9, confidence=0.40, runner_confidence=0.30)),
        **common,
    ).predict(Image.new("RGB", (40, 40), color="white"))
    wide = classifier(
        FakeModel(probabilities(winner=9, confidence=0.40, runner_confidence=0.30)),
        max_crop_aspect=1.5,
        **common,
    ).predict(Image.new("RGB", (160, 90), color="white"))

    assert ambiguous.accepted is False
    assert ambiguous.prototype_similarity == pytest.approx(1.0 / np.sqrt(2.0))
    assert ambiguous.prototype_margin == pytest.approx(0.0)
    assert wide.accepted is False
    assert wide.reason == "sign_crop_required"
    assert wide.prototype_similarity is None


@pytest.mark.parametrize(
    ("prototype_dtype", "class_id_dtype", "message"),
    [
        (np.float64, np.int64, "dtype must be exactly float32"),
        (np.float32, np.int32, "class_ids dtype must be exactly int64"),
    ],
)
def test_design_prototype_artifact_rejects_implicit_dtype_coercion(
    prototype_reference, prototype_dtype, class_id_dtype, message
):
    artifact, metadata, prototypes = prototype_reference
    np.savez(
        artifact,
        prototypes=prototypes.astype(prototype_dtype),
        class_ids=np.arange(43, dtype=class_id_dtype),
    )
    metadata_payload = json.loads(metadata.read_text(encoding="utf-8"))
    metadata_payload["artifact_sha256"] = hashlib.sha256(
        artifact.read_bytes()
    ).hexdigest().upper()
    metadata.write_text(json.dumps(metadata_payload), encoding="utf-8")

    with pytest.raises(ValueError, match=message):
        classifier(
            FakeModel(probabilities()),
            feature_model=FakeFeatureModel(prototypes[33]),
            design_prototype_path=artifact,
            design_prototype_metadata_path=metadata,
        )


@pytest.mark.parametrize(
    "overrides, message",
    [
        ({43: 0.77}, "class ID"),
        ({True: 0.77}, "class IDs must be integers"),
        ({12: float("nan")}, "between 0 and 1"),
        ({12: 1.01}, "between 0 and 1"),
        ({"12": 0.77}, "class IDs must be integers"),
    ],
)
def test_invalid_ood_threshold_overrides_fail_closed(overrides, message):
    with pytest.raises(ValueError, match=message):
        classifier(
            FakeModel(probabilities(winner=12)),
            ood_threshold_overrides=overrides,
        )


def test_ood_threshold_override_cannot_undercut_calibrated_threshold():
    with pytest.raises(ValueError, match="must not be below"):
        classifier(
            FakeModel(probabilities(winner=12)),
            ood_reference_path=OOD_REFERENCE,
            feature_model=FakeFeatureModel(np.zeros(512, dtype=np.float32)),
            ood_threshold_overrides={12: 0.70},
        )


def test_model_hash_mismatch_fails_closed_before_inference():
    with np.testing.assert_raises_regex(ValueError, "Model SHA-256"):
        TrafficSignClassifier(
            model_path=OOD_REFERENCE,
            class_map_path=CLASS_MAP,
            model=FakeModel(probabilities()),
        )


def test_low_information_image_uses_unknown_result():
    service = classifier(FakeModel(probabilities()), min_visual_std=0.01)

    prediction = service.predict(Image.new("RGB", (40, 40), color=(220, 20, 80)))

    assert prediction.accepted is False
    assert prediction.reason == "low_visual_information"
    assert prediction.result_id == 44


def test_preprocess_ignores_rgb_hidden_behind_fully_transparent_pixels():
    first = Image.new("RGBA", (40, 40), color=(255, 0, 0, 0))
    second = Image.new("RGBA", (40, 40), color=(0, 0, 255, 0))
    for image in (first, second):
        for x in range(10, 30):
            for y in range(10, 30):
                image.putpixel((x, y), (20, 180, 60, 255))

    first_tensor = TrafficSignClassifier.preprocess(first)
    second_tensor = TrafficSignClassifier.preprocess(second)

    np.testing.assert_array_equal(first_tensor, second_tensor)
    # Classifier tensors are BGR, so an opaque white background is [1, 1, 1].
    np.testing.assert_array_equal(first_tensor[0, 0, 0], [1.0, 1.0, 1.0])
