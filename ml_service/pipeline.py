from __future__ import annotations

import math
from dataclasses import dataclass
from typing import TYPE_CHECKING, Any, Dict, Optional, Sequence, Tuple

from PIL import Image

from .classifier import (
    PREDICTION_SOURCE_REJECTED,
    Prediction,
    TrafficSignClassifier,
)
from .image_processing import normalize_image_rgb

if TYPE_CHECKING:
    from .detector import Detection, TrafficSignDetector


PIPELINE_MODE_CROP = "crop"
PIPELINE_MODE_DETECT = "detect"
SUPPORTED_PIPELINE_MODES = frozenset({PIPELINE_MODE_CROP, PIPELINE_MODE_DETECT})


@dataclass(frozen=True)
class CandidateAnalysis:
    detection: "Detection"
    crop_box: Tuple[int, int, int, int]
    prediction: Prediction

    def to_dict(self) -> Dict[str, Any]:
        to_dict = getattr(self.detection, "to_dict", None)
        if callable(to_dict):
            detection = to_dict()
        else:
            detection = {
                "x_min": float(self.detection.x_min),
                "y_min": float(self.detection.y_min),
                "x_max": float(self.detection.x_max),
                "y_max": float(self.detection.y_max),
                "score": float(self.detection.score),
                "label": int(self.detection.label),
            }
        return {
            "detection": detection,
            "crop_box": list(self.crop_box),
            "prediction": self.prediction.to_dict(),
        }


@dataclass(frozen=True)
class AnalysisResult:
    result_id: int
    accepted: bool
    reason: str
    pipeline_mode: str
    unknown: Dict[str, Any]
    prediction: Optional[Prediction] = None
    candidates: Tuple[CandidateAnalysis, ...] = ()
    crop_fallback_attempted: bool = False
    crop_fallback_used: bool = False

    def to_dict(self) -> Dict[str, Any]:
        if self.prediction is not None:
            payload = self.prediction.to_dict()
        else:
            payload = {
                "class_id": None,
                "result_id": self.result_id,
                "key": str(self.unknown["key"]),
                "name_en": str(self.unknown["name_en"]),
                "name_ko": str(self.unknown["name_ko"]),
                "confidence": 0.0,
                "margin": 0.0,
                "ood_similarity": None,
                "ood_threshold": None,
                "accepted": False,
                "reason": self.reason,
                "top3": [],
                "prediction_source": PREDICTION_SOURCE_REJECTED,
                "raw_class_id": None,
                "base_reason": self.reason,
                "prototype_class_id": None,
                "prototype_similarity": None,
                "prototype_margin": None,
                "prototype_threshold": None,
            }

        # The pipeline decision is authoritative. In detect mode an otherwise valid
        # class prediction can still be rejected because the candidates disagree.
        payload.update(
            {
                "result_id": self.result_id,
                "accepted": self.accepted,
                "reason": self.reason,
                "pipeline_mode": self.pipeline_mode,
                "detection_count": len(self.candidates),
                "candidates": [candidate.to_dict() for candidate in self.candidates],
                "crop_fallback_attempted": self.crop_fallback_attempted,
                "crop_fallback_used": self.crop_fallback_used,
            }
        )
        return payload


class TrafficSignPipeline:
    """Runs crop classification or detector-assisted whole-image analysis."""

    CROP_PADDING_FRACTION = 0.10
    SELECTION_POLICY = "strict_candidate_consensus"

    def __init__(
        self,
        classifier: TrafficSignClassifier,
        mode: str = PIPELINE_MODE_CROP,
        detector: Optional["TrafficSignDetector"] = None,
        allow_crop_fallback: bool = True,
        crop_fallback_max_aspect: float = 1.25,
    ) -> None:
        normalized_mode = str(mode).strip().lower()
        if normalized_mode not in SUPPORTED_PIPELINE_MODES:
            choices = ", ".join(sorted(SUPPORTED_PIPELINE_MODES))
            raise ValueError(f"pipeline mode must be one of: {choices}")
        if normalized_mode == PIPELINE_MODE_DETECT and detector is None:
            raise ValueError("detect pipeline mode requires a traffic-sign detector")
        if not isinstance(allow_crop_fallback, bool):
            raise ValueError("allow_crop_fallback must be a boolean")
        if crop_fallback_max_aspect < 1.0:
            raise ValueError("crop_fallback_max_aspect must be at least 1")

        self.classifier = classifier
        self.mode = normalized_mode
        self.detector = detector
        self.allow_crop_fallback = allow_crop_fallback
        self.crop_fallback_max_aspect = float(crop_fallback_max_aspect)

    @property
    def _unknown_result_id(self) -> int:
        return int(self.classifier.class_map.unknown_result_id)

    @property
    def _unknown(self) -> Dict[str, Any]:
        return dict(self.classifier.class_map.unknown)

    def analyze(self, image: Image.Image) -> AnalysisResult:
        normalized = normalize_image_rgb(image)
        if self.mode == PIPELINE_MODE_CROP:
            prediction = self.classifier.predict(normalized)
            return AnalysisResult(
                result_id=prediction.result_id,
                accepted=prediction.accepted,
                reason=prediction.reason,
                pipeline_mode=self.mode,
                unknown=self._unknown,
                prediction=prediction,
            )

        detector = self.detector
        if detector is None:
            raise RuntimeError("detect pipeline lost its required detector")
        detections = list(detector.detect(normalized))
        if not detections:
            if self.allow_crop_fallback and self._is_crop_fallback_candidate(normalized):
                prediction = self.classifier.predict(normalized)
                if prediction.accepted:
                    return AnalysisResult(
                        result_id=prediction.result_id,
                        accepted=True,
                        reason="ok",
                        pipeline_mode=self.mode,
                        unknown=self._unknown,
                        prediction=prediction,
                        crop_fallback_attempted=True,
                        crop_fallback_used=True,
                    )
                return self._unknown_result(
                    "no_sign_detected", crop_fallback_attempted=True
                )
            return self._unknown_result("no_sign_detected")

        crop_boxes = [
            self._square_crop_box(normalized.size, detection)
            for detection in detections
        ]
        crops = [normalized.crop(box) for box in crop_boxes]
        predictions = self.classifier.predict_many(crops)
        if len(predictions) != len(detections):
            raise ValueError("classifier did not return one prediction per detection")

        candidates = tuple(
            CandidateAnalysis(detection, crop_box, prediction)
            for detection, crop_box, prediction in zip(
                detections, crop_boxes, predictions
            )
        )
        accepted = [prediction for prediction in predictions if prediction.accepted]
        if not accepted:
            return self._unknown_result("all_candidates_rejected", candidates)

        if len(accepted) != len(predictions):
            return self._unknown_result("ambiguous", candidates)

        result_ids = {prediction.result_id for prediction in accepted}
        if len(result_ids) != 1:
            return self._unknown_result("ambiguous", candidates)

        representative = accepted[0]
        return AnalysisResult(
            result_id=representative.result_id,
            accepted=True,
            reason="ok",
            pipeline_mode=self.mode,
            unknown=self._unknown,
            prediction=representative,
            candidates=candidates,
        )

    def _unknown_result(
        self,
        reason: str,
        candidates: Sequence[CandidateAnalysis] = (),
        crop_fallback_attempted: bool = False,
    ) -> AnalysisResult:
        return AnalysisResult(
            result_id=self._unknown_result_id,
            accepted=False,
            reason=reason,
            pipeline_mode=self.mode,
            unknown=self._unknown,
            candidates=tuple(candidates),
            crop_fallback_attempted=crop_fallback_attempted,
        )

    def _is_crop_fallback_candidate(self, image: Image.Image) -> bool:
        width, height = image.size
        aspect = max(width / height, height / width)
        return aspect <= self.crop_fallback_max_aspect

    @classmethod
    def _square_crop_box(
        cls,
        image_size: Tuple[int, int],
        detection: "Detection",
    ) -> Tuple[int, int, int, int]:
        image_width, image_height = image_size
        coordinates = (
            float(detection.x_min),
            float(detection.y_min),
            float(detection.x_max),
            float(detection.y_max),
        )
        if not all(math.isfinite(value) for value in coordinates):
            raise ValueError("detector returned non-finite coordinates")
        x_min, y_min, x_max, y_max = coordinates
        if x_max <= x_min or y_max <= y_min:
            raise ValueError("detector returned an empty bounding box")

        center_x = (x_min + x_max) / 2.0
        center_y = (y_min + y_max) / 2.0
        side = max(x_max - x_min, y_max - y_min)
        side *= 1.0 + 2.0 * cls.CROP_PADDING_FRACTION
        side = min(side, float(image_width), float(image_height))
        if side < 1.0:
            raise ValueError("detector returned a bounding box that is too small")

        left = center_x - side / 2.0
        top = center_y - side / 2.0
        left = min(max(left, 0.0), image_width - side)
        top = min(max(top, 0.0), image_height - side)

        # A common integer side length keeps the crop square after PIL rounding.
        integer_side = max(1, int(math.ceil(side)))
        integer_side = min(integer_side, image_width, image_height)
        integer_left = int(math.floor(left))
        integer_top = int(math.floor(top))
        integer_left = min(max(integer_left, 0), image_width - integer_side)
        integer_top = min(max(integer_top, 0), image_height - integer_side)
        return (
            integer_left,
            integer_top,
            integer_left + integer_side,
            integer_top + integer_side,
        )

    def metadata(self) -> Dict[str, Any]:
        detector_metadata = None
        if self.mode == PIPELINE_MODE_DETECT and self.detector is not None:
            detector_metadata = self.detector.metadata()
        return {
            "pipeline_mode": self.mode,
            "detector_enabled": detector_metadata is not None,
            "detector": detector_metadata,
            "selection_policy": self.SELECTION_POLICY,
            "candidate_crop_padding_fraction": self.CROP_PADDING_FRACTION,
            "crop_fallback_enabled": (
                self.mode == PIPELINE_MODE_DETECT and self.allow_crop_fallback
            ),
            "crop_fallback_max_aspect": self.crop_fallback_max_aspect,
        }
