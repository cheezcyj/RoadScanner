from __future__ import annotations

import hashlib
import json
import threading
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Dict, List, Mapping, Optional

import numpy as np
from PIL import Image

from .image_processing import normalize_image_rgb


RECOVERED_MODEL_SHA256 = (
    "7F9FD2D60F907FC346185B4CE62C4C50C2879DF4469906BBD10FBD26A4ECB0CE"
)
RECOVERED_OOD_REFERENCE_SHA256 = (
    "B291EB750E33EFD59E27E09E5EA2236DE05E38624E2355EC0B42E653A3E43F4D"
)
RECOVERED_CLASS_MAP_SHA256 = (
    "A5001A6CFAC4CF1C1135ABA53312226A59617FA635DF09B1C9B7B9F037A01F8E"
)
RECOVERED_DESIGN_PROTOTYPE_SHA256 = (
    "5AA43019CF6A15C84A04AF58C962736DCDDB4C5D0BC6FE557EEEB25197538953"
)
RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256 = (
    "8610646A04986AD84D7DAA9FB985660D507BA216EC53EF4F00186C0CB48A0F06"
)
OOD_FEATURE_LAYER = "batch_normalization_2"
DEFAULT_OOD_THRESHOLD_OVERRIDES = {12: 0.77}
DEFAULT_DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES = {33: 0.50}
DEFAULT_DESIGN_PROTOTYPE_RAW_MATCH_CLASSES = frozenset({33})
DESIGN_PROTOTYPE_COUNT = 43
DESIGN_PROTOTYPE_DIMENSION = 512
PREDICTION_SOURCE_MODEL = "recovered_cnn"
PREDICTION_SOURCE_PROTOTYPE = "canonical_design_prototype"
PREDICTION_SOURCE_REJECTED = "rejected"


@dataclass(frozen=True)
class ClassLabel:
    class_id: int
    result_id: int
    key: str
    name_en: str
    name_ko: str
    de_sign_code: str


@dataclass(frozen=True)
class Prediction:
    class_id: int
    result_id: int
    key: str
    name_en: str
    name_ko: str
    confidence: float
    margin: float
    ood_similarity: Optional[float]
    ood_threshold: Optional[float]
    accepted: bool
    reason: str
    top3: List[Dict[str, Any]]
    prediction_source: str = PREDICTION_SOURCE_MODEL
    raw_class_id: Optional[int] = None
    base_reason: str = "ok"
    prototype_class_id: Optional[int] = None
    prototype_similarity: Optional[float] = None
    prototype_margin: Optional[float] = None
    prototype_threshold: Optional[float] = None

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class ClassMap:
    def __init__(self, path: Path) -> None:
        payload = json.loads(path.read_text(encoding="utf-8"))
        class_count = int(payload["class_count"])
        rows = payload["classes"]
        if class_count != 43 or len(rows) != class_count:
            raise ValueError("The existing RoadScanner model requires exactly 43 classes")

        labels = [ClassLabel(**row) for row in rows]
        expected_ids = list(range(class_count))
        if [label.class_id for label in labels] != expected_ids:
            raise ValueError("Class IDs must be complete and ordered from 0 through 42")
        result_ids = [label.result_id for label in labels]
        if len(set(result_ids)) != class_count or min(result_ids) <= 0:
            raise ValueError("Result IDs must be unique positive integers")

        self.dataset = str(payload["dataset"])
        self.class_count = class_count
        self.unknown_result_id = int(payload["unknown_result_id"])
        if self.unknown_result_id in result_ids or self.unknown_result_id <= 0:
            raise ValueError("The unknown result ID must be positive and outside the class map")
        unknown = payload["unknown"]
        if int(unknown["result_id"]) != self.unknown_result_id:
            raise ValueError("Unknown result metadata must match unknown_result_id")
        self.unknown = unknown
        self.labels = labels

    def __getitem__(self, class_id: int) -> ClassLabel:
        return self.labels[class_id]


class TrafficSignClassifier:
    """Loads the recovered CNN and applies its empirically verified preprocessing."""

    INPUT_SIZE = (30, 30)
    EXPECTED_INPUT_SHAPE = (None, 30, 30, 3)
    EXPECTED_OUTPUT_SHAPE = (None, 43)

    def __init__(
        self,
        model_path: Path,
        class_map_path: Path,
        min_confidence: float = 0.85,
        min_margin: float = 0.15,
        max_crop_aspect: float = 1.45,
        min_visual_std: float = 0.01,
        min_neighbor_correlation: float = 0.4,
        ood_safety_margin: float = 0.07,
        ood_threshold_overrides: Optional[Mapping[int, float]] = None,
        expected_model_sha256: Optional[str] = RECOVERED_MODEL_SHA256,
        expected_class_map_sha256: Optional[str] = RECOVERED_CLASS_MAP_SHA256,
        ood_reference_path: Optional[Path] = None,
        expected_ood_reference_sha256: Optional[str] = None,
        design_prototype_path: Optional[Path] = None,
        expected_design_prototype_sha256: Optional[str] = None,
        design_prototype_metadata_path: Optional[Path] = None,
        expected_design_prototype_metadata_sha256: Optional[str] = None,
        design_prototype_min_similarity: float = 0.80,
        design_prototype_min_margin: float = 0.15,
        design_prototype_threshold_overrides: Optional[
            Mapping[int, float]
        ] = None,
        design_prototype_raw_match_classes: Optional[set[int] | frozenset[int]] = None,
        model: Optional[Any] = None,
        feature_model: Optional[Any] = None,
    ) -> None:
        if not 0.0 <= min_confidence <= 1.0:
            raise ValueError("min_confidence must be between 0 and 1")
        if not 0.0 <= min_margin <= 1.0:
            raise ValueError("min_margin must be between 0 and 1")
        if max_crop_aspect < 1.0:
            raise ValueError("max_crop_aspect must be at least 1")
        if not 0.0 <= min_visual_std <= 1.0:
            raise ValueError("min_visual_std must be between 0 and 1")
        if not -1.0 <= min_neighbor_correlation <= 1.0:
            raise ValueError("min_neighbor_correlation must be between -1 and 1")
        if not 0.0 <= ood_safety_margin <= 1.0:
            raise ValueError("ood_safety_margin must be between 0 and 1")
        if not 0.0 <= design_prototype_min_similarity <= 1.0:
            raise ValueError("design_prototype_min_similarity must be between 0 and 1")
        if not 0.0 <= design_prototype_min_margin <= 1.0:
            raise ValueError("design_prototype_min_margin must be between 0 and 1")

        expected_model_sha256 = self._validated_expected_hash(
            expected_model_sha256, "model"
        )
        expected_class_map_sha256 = self._validated_expected_hash(
            expected_class_map_sha256, "class map"
        )
        expected_ood_reference_sha256 = self._validated_expected_hash(
            expected_ood_reference_sha256, "OOD reference"
        )
        expected_design_prototype_sha256 = self._validated_expected_hash(
            expected_design_prototype_sha256, "design prototype"
        )
        expected_design_prototype_metadata_sha256 = self._validated_expected_hash(
            expected_design_prototype_metadata_sha256,
            "design prototype metadata",
        )
        self.model_path = model_path.resolve()
        self.class_map_path = class_map_path.resolve()
        self.class_map_sha256 = self._sha256(self.class_map_path)
        if (
            expected_class_map_sha256
            and self.class_map_sha256 != expected_class_map_sha256.upper()
        ):
            raise ValueError(f"Class-map SHA-256 mismatch: {self.class_map_sha256}")
        self.class_map_hash_verified = expected_class_map_sha256 is not None
        self.class_map = ClassMap(self.class_map_path)
        self.ood_threshold_overrides = self._validated_ood_threshold_overrides(
            DEFAULT_OOD_THRESHOLD_OVERRIDES
            if ood_threshold_overrides is None
            else ood_threshold_overrides,
            self.class_map.class_count,
        )
        self.design_prototype_threshold_overrides = (
            self._validated_design_prototype_threshold_overrides(
                DEFAULT_DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES
                if design_prototype_threshold_overrides is None
                else design_prototype_threshold_overrides,
                self.class_map.class_count,
            )
        )
        self.design_prototype_raw_match_classes = self._validated_class_ids(
            DEFAULT_DESIGN_PROTOTYPE_RAW_MATCH_CLASSES
            if design_prototype_raw_match_classes is None
            else design_prototype_raw_match_classes,
            self.class_map.class_count,
            "design prototype raw-match classes",
        )
        self.min_confidence = min_confidence
        self.min_margin = min_margin
        self.max_crop_aspect = max_crop_aspect
        self.min_visual_std = min_visual_std
        self.min_neighbor_correlation = min_neighbor_correlation
        self.ood_safety_margin = ood_safety_margin
        self.design_prototype_min_similarity = design_prototype_min_similarity
        self.design_prototype_min_margin = design_prototype_min_margin
        self._lock = threading.Lock()
        self.model_sha256 = (
            self._sha256(self.model_path) if self.model_path.is_file() else "test-double"
        )
        if (
            expected_model_sha256
            and self.model_sha256 != "test-double"
            and self.model_sha256 != expected_model_sha256.upper()
        ):
            raise ValueError(
                "Model SHA-256 does not match the recovered RoadScanner artifact: "
                f"{self.model_sha256}"
            )
        self.model_hash_verified = (
            expected_model_sha256 is not None and self.model_sha256 != "test-double"
        )
        self._model = model if model is not None else self._load_model()
        self._validate_model_shape()
        self.ood_reference_sha256: Optional[str] = None
        self.ood_reference_hash_verified = False
        self._ood_centroids: Optional[np.ndarray] = None
        self._ood_thresholds: Optional[np.ndarray] = None
        self._feature_model: Optional[Any] = None
        self._feature_dimension: Optional[int] = None
        self.design_prototype_sha256: Optional[str] = None
        self.design_prototype_hash_verified = False
        self.design_prototype_metadata_sha256: Optional[str] = None
        self.design_prototype_metadata_hash_verified = False
        self.design_prototype_source_lock_sha256: Optional[str] = None
        self.design_prototype_source_variant: Optional[str] = None
        self._design_prototypes: Optional[np.ndarray] = None
        if ood_reference_path is not None:
            self._load_ood_reference(
                ood_reference_path,
                expected_ood_reference_sha256,
                feature_model,
            )
        if design_prototype_path is not None:
            if self._feature_model is None:
                self._feature_model = self._validated_feature_model(
                    feature_model,
                    DESIGN_PROTOTYPE_DIMENSION,
                    "design prototype",
                )
                self._feature_dimension = DESIGN_PROTOTYPE_DIMENSION
            self._load_design_prototypes(
                design_prototype_path,
                expected_design_prototype_sha256,
                design_prototype_metadata_path,
                expected_design_prototype_metadata_sha256,
            )
        elif design_prototype_metadata_path is not None:
            raise ValueError(
                "design_prototype_metadata_path requires design_prototype_path"
            )

    def _load_model(self) -> Any:
        if not self.model_path.is_file():
            raise FileNotFoundError(f"Model file was not found: {self.model_path}")
        import tensorflow as tf

        return tf.keras.models.load_model(str(self.model_path), compile=False)

    def _validate_model_shape(self) -> None:
        input_shape = tuple(self._model.input_shape)
        output_shape = tuple(self._model.output_shape)
        if input_shape != self.EXPECTED_INPUT_SHAPE:
            raise ValueError(f"Unexpected model input shape: {input_shape}")
        if output_shape != self.EXPECTED_OUTPUT_SHAPE:
            raise ValueError(f"Unexpected model output shape: {output_shape}")

    def _load_ood_reference(
        self,
        path: Path,
        expected_sha256: Optional[str],
        feature_model: Optional[Any],
    ) -> None:
        resolved = path.resolve()
        if not resolved.is_file():
            raise FileNotFoundError(f"OOD reference was not found: {resolved}")
        actual_sha256 = self._sha256(resolved)
        if expected_sha256 and actual_sha256 != expected_sha256.upper():
            raise ValueError(f"OOD reference SHA-256 mismatch: {actual_sha256}")
        with np.load(resolved, allow_pickle=False) as payload:
            centroids = np.asarray(payload["centroids"], dtype=np.float32)
            thresholds = np.asarray(payload["thresholds"], dtype=np.float32)
        if centroids.shape[0] != self.class_map.class_count or thresholds.shape != (
            self.class_map.class_count,
        ):
            raise ValueError("OOD reference does not cover all 43 classes")
        if not np.all(np.isfinite(centroids)) or not np.all(np.isfinite(thresholds)):
            raise ValueError("OOD reference contains non-finite values")
        for class_id, threshold in self.ood_threshold_overrides.items():
            calibrated_threshold = float(thresholds[class_id])
            if threshold < calibrated_threshold:
                raise ValueError(
                    f"OOD threshold override for class {class_id} must not be below "
                    f"its calibrated threshold {calibrated_threshold:.8f}"
                )

        feature_model = self._validated_feature_model(
            feature_model, centroids.shape[1], "OOD"
        )
        self.ood_reference_sha256 = actual_sha256
        self.ood_reference_hash_verified = expected_sha256 is not None
        self._ood_centroids = centroids
        self._ood_thresholds = thresholds
        self._feature_model = feature_model
        self._feature_dimension = int(centroids.shape[1])

    def _validated_feature_model(
        self,
        feature_model: Optional[Any],
        expected_dimension: int,
        label: str,
    ) -> Any:
        if feature_model is None:
            import tensorflow as tf

            try:
                feature_layer = self._model.get_layer(OOD_FEATURE_LAYER)
            except (AttributeError, ValueError) as error:
                raise ValueError(
                    f"Model is missing the {label} feature layer: {OOD_FEATURE_LAYER}"
                ) from error
            feature_model = tf.keras.Model(self._model.inputs[0], feature_layer.output)
        feature_shape = tuple(feature_model.output_shape)
        if len(feature_shape) != 2 or feature_shape[-1] != expected_dimension:
            raise ValueError(
                f"{label} feature shape {feature_shape} does not end in "
                f"{expected_dimension}"
            )
        return feature_model

    def _load_design_prototypes(
        self,
        path: Path,
        expected_sha256: Optional[str],
        metadata_path: Optional[Path],
        expected_metadata_sha256: Optional[str],
    ) -> None:
        resolved = path.resolve()
        if not resolved.is_file():
            raise FileNotFoundError(f"Design prototype artifact was not found: {resolved}")
        actual_sha256 = self._sha256(resolved)
        if expected_sha256 and actual_sha256 != expected_sha256:
            raise ValueError(f"Design prototype SHA-256 mismatch: {actual_sha256}")
        with np.load(resolved, allow_pickle=False) as payload:
            if set(payload.files) != {"prototypes", "class_ids"}:
                raise ValueError(
                    "Design prototype artifact must contain prototypes and class_ids"
                )
            stored_prototypes = payload["prototypes"]
            stored_class_ids = payload["class_ids"]
            if stored_prototypes.dtype != np.dtype(np.float32):
                raise ValueError("Design prototype dtype must be exactly float32")
            if stored_class_ids.dtype != np.dtype(np.int64):
                raise ValueError("Design prototype class_ids dtype must be exactly int64")
            prototypes = np.asarray(stored_prototypes, dtype=np.float32)
            class_ids = np.asarray(stored_class_ids, dtype=np.int64)
        expected_shape = (
            self.class_map.class_count,
            DESIGN_PROTOTYPE_DIMENSION,
        )
        if prototypes.shape != expected_shape:
            raise ValueError(
                f"Design prototype shape {prototypes.shape} does not match {expected_shape}"
            )
        if class_ids.shape != (self.class_map.class_count,) or not np.array_equal(
            class_ids, np.arange(self.class_map.class_count, dtype=np.int64)
        ):
            raise ValueError("Design prototype class IDs must be ordered from 0 to 42")
        if not np.all(np.isfinite(prototypes)):
            raise ValueError("Design prototypes contain non-finite values")
        norms = np.linalg.norm(prototypes, axis=1)
        if not np.allclose(norms, 1.0, rtol=1e-5, atol=1e-6):
            raise ValueError("Design prototype rows must be L2-normalized")
        if self._feature_dimension != prototypes.shape[1]:
            raise ValueError(
                "Design prototype dimension does not match the model feature layer"
            )

        resolved_metadata = (
            metadata_path.resolve()
            if metadata_path is not None
            else resolved.with_suffix(".json")
        )
        if not resolved_metadata.is_file():
            raise FileNotFoundError(
                f"Design prototype metadata was not found: {resolved_metadata}"
            )
        metadata_sha256 = self._sha256(resolved_metadata)
        if (
            expected_metadata_sha256
            and metadata_sha256 != expected_metadata_sha256
        ):
            raise ValueError(
                "Design prototype metadata SHA-256 mismatch: "
                f"{metadata_sha256}"
            )
        metadata = json.loads(resolved_metadata.read_text(encoding="utf-8"))
        self._validate_design_prototype_metadata(
            metadata, resolved.name, actual_sha256, prototypes.shape
        )

        self.design_prototype_sha256 = actual_sha256
        self.design_prototype_hash_verified = expected_sha256 is not None
        self.design_prototype_metadata_sha256 = metadata_sha256
        self.design_prototype_metadata_hash_verified = (
            expected_metadata_sha256 is not None
        )
        self.design_prototype_source_lock_sha256 = str(
            metadata["source_lock_sha256"]
        )
        self.design_prototype_source_variant = str(metadata["source_variant"])
        self._design_prototypes = prototypes

    def _validate_design_prototype_metadata(
        self,
        metadata: Any,
        artifact_name: str,
        artifact_sha256: str,
        artifact_shape: tuple[int, ...],
    ) -> None:
        if not isinstance(metadata, Mapping) or metadata.get("schema_version") != 1:
            raise ValueError("Design prototype metadata must use schema_version 1")
        expected_values = {
            "artifact": artifact_name,
            "artifact_sha256": artifact_sha256,
            "feature_layer": OOD_FEATURE_LAYER,
            "model_sha256": self.model_sha256,
            "class_map_sha256": self.class_map_sha256,
            "shape": list(artifact_shape),
            "dtype": "float32",
            "row_l2_normalized": True,
        }
        for key, expected in expected_values.items():
            if metadata.get(key) != expected:
                raise ValueError(
                    f"Design prototype metadata {key} does not match the loaded artifact"
                )
        source_lock_sha256 = str(metadata.get("source_lock_sha256", ""))
        if len(source_lock_sha256) != 64 or any(
            character not in "0123456789ABCDEF"
            for character in source_lock_sha256
        ):
            raise ValueError(
                "Design prototype metadata must contain an uppercase source-lock SHA-256"
            )
        source_variant = metadata.get("source_variant")
        if not isinstance(source_variant, str) or not source_variant:
            raise ValueError("Design prototype metadata must name its source variant")
        classes = metadata.get("classes")
        if not isinstance(classes, list) or len(classes) != self.class_map.class_count:
            raise ValueError("Design prototype metadata must cover all 43 classes")
        if [int(item.get("class_id", -1)) for item in classes] != list(
            range(self.class_map.class_count)
        ):
            raise ValueError(
                "Design prototype metadata class IDs must be ordered from 0 to 42"
            )

    @staticmethod
    def _sha256(path: Path) -> str:
        digest = hashlib.sha256()
        with path.open("rb") as source:
            for chunk in iter(lambda: source.read(1024 * 1024), b""):
                digest.update(chunk)
        return digest.hexdigest().upper()

    @staticmethod
    def _validated_expected_hash(value: Optional[str], label: str) -> Optional[str]:
        if value is None:
            return None
        normalized = value.strip().upper()
        if len(normalized) != 64 or any(
            character not in "0123456789ABCDEF" for character in normalized
        ):
            raise ValueError(f"Expected {label} SHA-256 must contain 64 hex characters")
        return normalized

    @staticmethod
    def _validated_ood_threshold_overrides(
        overrides: Mapping[int, float], class_count: int
    ) -> Dict[int, float]:
        if not isinstance(overrides, Mapping):
            raise ValueError("OOD threshold overrides must be a mapping")
        validated: Dict[int, float] = {}
        for class_id, threshold in overrides.items():
            if isinstance(class_id, bool) or not isinstance(class_id, int):
                raise ValueError("OOD threshold override class IDs must be integers")
            if not 0 <= class_id < class_count:
                raise ValueError(
                    f"OOD threshold override class ID must be between 0 and {class_count - 1}"
                )
            if isinstance(threshold, bool):
                raise ValueError("OOD threshold override values must be numeric")
            try:
                normalized_threshold = float(threshold)
            except (TypeError, ValueError) as error:
                raise ValueError("OOD threshold override values must be numeric") from error
            if not np.isfinite(normalized_threshold) or not 0.0 <= normalized_threshold <= 1.0:
                raise ValueError("OOD threshold override values must be between 0 and 1")
            validated[class_id] = normalized_threshold
        return validated

    @staticmethod
    def _validated_design_prototype_threshold_overrides(
        overrides: Mapping[int, float], class_count: int
    ) -> Dict[int, float]:
        if not isinstance(overrides, Mapping):
            raise ValueError("Design prototype threshold overrides must be a mapping")
        validated: Dict[int, float] = {}
        for class_id, threshold in overrides.items():
            if isinstance(class_id, bool) or not isinstance(class_id, int):
                raise ValueError(
                    "Design prototype threshold override class IDs must be integers"
                )
            if not 0 <= class_id < class_count:
                raise ValueError(
                    "Design prototype threshold override class ID must be between "
                    f"0 and {class_count - 1}"
                )
            if isinstance(threshold, bool):
                raise ValueError(
                    "Design prototype threshold override values must be numeric"
                )
            try:
                normalized_threshold = float(threshold)
            except (TypeError, ValueError) as error:
                raise ValueError(
                    "Design prototype threshold override values must be numeric"
                ) from error
            if (
                not np.isfinite(normalized_threshold)
                or not 0.0 <= normalized_threshold <= 1.0
            ):
                raise ValueError(
                    "Design prototype threshold override values must be between 0 and 1"
                )
            validated[class_id] = normalized_threshold
        return validated

    @staticmethod
    def _validated_class_ids(
        class_ids: Any, class_count: int, label: str
    ) -> frozenset[int]:
        if isinstance(class_ids, (str, bytes, Mapping)):
            raise ValueError(f"{label} must be a collection of integer class IDs")
        try:
            values = list(class_ids)
        except TypeError as error:
            raise ValueError(
                f"{label} must be a collection of integer class IDs"
            ) from error
        validated: set[int] = set()
        for class_id in values:
            if isinstance(class_id, bool) or not isinstance(class_id, int):
                raise ValueError(f"{label} must contain only integer class IDs")
            if not 0 <= class_id < class_count:
                raise ValueError(
                    f"{label} class IDs must be between 0 and {class_count - 1}"
                )
            if class_id in validated:
                raise ValueError(f"{label} must not repeat a class ID")
            validated.add(class_id)
        return frozenset(validated)

    @classmethod
    def preprocess(cls, image: Image.Image) -> np.ndarray:
        image = normalize_image_rgb(image)
        return cls._preprocess_normalized(image)

    @classmethod
    def _preprocess_normalized(cls, image: Image.Image) -> np.ndarray:
        resized = image.resize(cls.INPUT_SIZE, Image.Resampling.BICUBIC)
        rgb = np.asarray(resized, dtype=np.float32)
        bgr = rgb[..., ::-1].copy()
        return np.expand_dims(bgr / 255.0, axis=0)

    def _crop_reason(self, image: Image.Image) -> str:
        if image.width <= 0 or image.height <= 0:
            raise ValueError("Image dimensions must be positive")
        landscape_aspect = image.width / image.height
        if min(image.width, image.height) < 12:
            return "image_too_small"
        if landscape_aspect > self.max_crop_aspect:
            return "sign_crop_required"
        return "ok"

    @staticmethod
    def _neighbor_correlation(pixels: np.ndarray) -> float:
        correlations = []
        for channel in range(pixels.shape[2]):
            values = pixels[:, :, channel]
            for left, right in ((values[:, :-1], values[:, 1:]), (values[:-1], values[1:])):
                centered_left = left - float(left.mean())
                centered_right = right - float(right.mean())
                denominator = float(
                    np.sqrt(
                        np.sum(centered_left * centered_left)
                        * np.sum(centered_right * centered_right)
                    )
                )
                correlations.append(
                    1.0
                    if denominator <= 1e-12
                    else float(np.sum(centered_left * centered_right) / denominator)
                )
        return float(np.mean(correlations))

    def predict(self, image: Image.Image) -> Prediction:
        return self.predict_many([image])[0]

    def predict_many(self, images: List[Image.Image]) -> List[Prediction]:
        if not images:
            return []
        normalized_images = [normalize_image_rgb(image) for image in images]
        prepared = [
            self._preprocess_normalized(image) for image in normalized_images
        ]
        crop_reasons = []
        for image, tensor in zip(normalized_images, prepared):
            reason = self._crop_reason(image)
            spatial_std = float(np.std(tensor[0], axis=(0, 1)).mean())
            if reason == "ok" and spatial_std < self.min_visual_std:
                reason = "low_visual_information"
            if (
                reason == "ok"
                and self._neighbor_correlation(tensor[0])
                < self.min_neighbor_correlation
            ):
                reason = "high_frequency_noise"
            crop_reasons.append(reason)
        batch = np.concatenate(prepared, axis=0)

        with self._lock:
            raw = self._model(batch, training=False)
            raw_features = (
                self._feature_model(batch, training=False)
                if self._feature_model is not None
                else None
            )
        probabilities_batch = np.asarray(raw.numpy() if hasattr(raw, "numpy") else raw)
        expected_shape = (len(images), self.class_map.class_count)
        if probabilities_batch.shape != expected_shape:
            raise ValueError(f"Unexpected prediction shape: {probabilities_batch.shape}")
        if not np.all(np.isfinite(probabilities_batch)):
            raise ValueError("Model returned non-finite probabilities")

        feature_batch = None
        if raw_features is not None:
            feature_batch = np.asarray(
                raw_features.numpy() if hasattr(raw_features, "numpy") else raw_features,
                dtype=np.float32,
            )
            if feature_batch.shape != (len(images), self._feature_dimension):
                raise ValueError(
                    f"Unexpected classifier feature shape: {feature_batch.shape}"
                )
            if not np.all(np.isfinite(feature_batch)):
                raise ValueError("Model returned non-finite classifier features")

        return [
            self._build_prediction(probabilities, crop_reason, features)
            for probabilities, crop_reason, features in zip(
                probabilities_batch,
                crop_reasons,
                feature_batch if feature_batch is not None else [None] * len(images),
            )
        ]

    def _build_prediction(
        self,
        probabilities: np.ndarray,
        crop_reason: str,
        features: Optional[np.ndarray],
    ) -> Prediction:
        order = np.argsort(probabilities)[::-1]
        raw_class_id = int(order[0])
        confidence = float(probabilities[raw_class_id])
        margin = confidence - float(probabilities[int(order[1])])

        ood_similarity = None
        ood_threshold = None
        normalized_features = None
        if features is not None:
            norm = max(float(np.linalg.norm(features)), 1e-12)
            normalized_features = features / norm
            if self._ood_centroids is not None and self._ood_thresholds is not None:
                ood_similarity = float(
                    normalized_features @ self._ood_centroids[raw_class_id]
                )
                ood_threshold = self.ood_threshold_overrides.get(raw_class_id)
                if ood_threshold is None:
                    ood_threshold = min(
                        1.0,
                        float(self._ood_thresholds[raw_class_id])
                        + self.ood_safety_margin,
                    )

        base_reason = crop_reason
        if base_reason == "ok" and confidence < self.min_confidence:
            base_reason = "low_confidence"
        if base_reason == "ok" and margin < self.min_margin:
            base_reason = "low_margin"
        if (
            base_reason == "ok"
            and ood_similarity is not None
            and ood_similarity < ood_threshold
        ):
            base_reason = "out_of_distribution"

        final_class_id = raw_class_id
        reason = base_reason
        accepted = base_reason == "ok"
        prediction_source = (
            PREDICTION_SOURCE_MODEL if accepted else PREDICTION_SOURCE_REJECTED
        )
        prototype_class_id = None
        prototype_similarity = None
        prototype_margin = None
        prototype_threshold = None
        if (
            not accepted
            and crop_reason == "ok"
            and normalized_features is not None
            and self._design_prototypes is not None
        ):
            similarities = normalized_features @ self._design_prototypes.T
            prototype_order = np.argsort(similarities)[::-1]
            prototype_class_id = int(prototype_order[0])
            prototype_similarity = float(similarities[prototype_class_id])
            prototype_margin = prototype_similarity - float(
                similarities[int(prototype_order[1])]
            )
            prototype_threshold = self.design_prototype_threshold_overrides.get(
                prototype_class_id,
                self.design_prototype_min_similarity,
            )
            raw_match_required = (
                prototype_class_id in self.design_prototype_raw_match_classes
            )
            if (
                prototype_similarity >= prototype_threshold
                and prototype_margin >= self.design_prototype_min_margin
                and (not raw_match_required or raw_class_id == prototype_class_id)
            ):
                final_class_id = prototype_class_id
                reason = "ok"
                accepted = True
                prediction_source = PREDICTION_SOURCE_PROTOTYPE

        label = self.class_map[final_class_id]

        top3 = []
        for index in order[:3]:
            candidate = self.class_map[int(index)]
            top3.append(
                {
                    "class_id": candidate.class_id,
                    "key": candidate.key,
                    "confidence": round(float(probabilities[int(index)]), 8),
                }
            )

        return Prediction(
            class_id=final_class_id,
            result_id=(label.result_id if accepted else self.class_map.unknown_result_id),
            key=label.key,
            name_en=label.name_en,
            name_ko=label.name_ko,
            confidence=confidence,
            margin=margin,
            ood_similarity=ood_similarity,
            ood_threshold=ood_threshold,
            accepted=accepted,
            reason=reason,
            top3=top3,
            prediction_source=prediction_source,
            raw_class_id=raw_class_id,
            base_reason=base_reason,
            prototype_class_id=prototype_class_id,
            prototype_similarity=prototype_similarity,
            prototype_margin=prototype_margin,
            prototype_threshold=prototype_threshold,
        )

    def metadata(self) -> Dict[str, Any]:
        return {
            "dataset": self.class_map.dataset,
            "class_count": self.class_map.class_count,
            "input_shape": list(self.EXPECTED_INPUT_SHAPE),
            "output_shape": list(self.EXPECTED_OUTPUT_SHAPE),
            "model_sha256": self.model_sha256,
            "class_map_sha256": self.class_map_sha256,
            "model_hash_verified": self.model_hash_verified,
            "class_map_hash_verified": self.class_map_hash_verified,
            "ood_enabled": self._ood_centroids is not None,
            "ood_reference_sha256": self.ood_reference_sha256,
            "ood_reference_hash_verified": self.ood_reference_hash_verified,
            "ood_feature_layer": OOD_FEATURE_LAYER if self._ood_centroids is not None else None,
            "ood_safety_margin": self.ood_safety_margin,
            "ood_threshold_overrides": {
                str(class_id): threshold
                for class_id, threshold in sorted(self.ood_threshold_overrides.items())
            },
            "design_prototype_enabled": self._design_prototypes is not None,
            "design_prototype_sha256": self.design_prototype_sha256,
            "design_prototype_hash_verified": self.design_prototype_hash_verified,
            "design_prototype_metadata_sha256": (
                self.design_prototype_metadata_sha256
            ),
            "design_prototype_metadata_hash_verified": (
                self.design_prototype_metadata_hash_verified
            ),
            "design_prototype_source_lock_sha256": (
                self.design_prototype_source_lock_sha256
            ),
            "design_prototype_source_variant": self.design_prototype_source_variant,
            "design_prototype_shape": (
                list(self._design_prototypes.shape)
                if self._design_prototypes is not None
                else None
            ),
            "design_prototype_feature_layer": (
                OOD_FEATURE_LAYER if self._design_prototypes is not None else None
            ),
            "design_prototype_policy": "rejected_predictions_only",
            "design_prototype_min_similarity": self.design_prototype_min_similarity,
            "design_prototype_min_margin": self.design_prototype_min_margin,
            "design_prototype_threshold_overrides": {
                str(class_id): threshold
                for class_id, threshold in sorted(
                    self.design_prototype_threshold_overrides.items()
                )
            },
            "design_prototype_raw_match_classes": sorted(
                self.design_prototype_raw_match_classes
            ),
            "preprocessing": (
                "EXIF transpose -> alpha composite on opaque white -> RGB -> BGR "
                "-> bicubic 30x30 -> float32 / 255"
            ),
            "min_confidence": self.min_confidence,
            "min_margin": self.min_margin,
            "max_landscape_crop_aspect": self.max_crop_aspect,
            "min_visual_std": self.min_visual_std,
            "min_neighbor_correlation": self.min_neighbor_correlation,
        }
