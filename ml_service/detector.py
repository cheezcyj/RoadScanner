from __future__ import annotations

import hashlib
import threading
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Tuple

import numpy as np
from PIL import Image

from .image_processing import normalize_image_rgb


FOREGROUND_LABEL = 1
TRAINED_DETECTOR_MODEL_SHA256 = (
    "D1EC740200141D1BB6D96935ACC947216CFA28911A6D8F4F56F2832E7B30CF03"
)


@dataclass(frozen=True)
class Detection:
    """A traffic-sign box in coordinates of the original image."""

    x_min: float
    y_min: float
    x_max: float
    y_max: float
    score: float
    label: int = FOREGROUND_LABEL

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass(frozen=True)
class _LetterboxTransform:
    scale_x: float
    scale_y: float
    pad_x: int
    pad_y: int
    original_width: int
    original_height: int


class TrafficSignDetector:
    """Runs the single-class RoadScanner ONNX traffic-sign detector.

    The ONNX artifact has a deliberately small, fixed contract. It accepts one
    normalized RGB image in NCHW layout and returns unfiltered ``xyxy`` boxes in
    the 320-by-320 letterboxed image, confidence scores, and class labels.
    Runtime validation and NMS live here so malformed or incompatible artifacts
    fail closed rather than silently producing a wrong crop.
    """

    INPUT_SIZE = 320
    EXPECTED_INPUT_SHAPE = (1, 3, INPUT_SIZE, INPUT_SIZE)
    OUTPUT_NAMES = ("boxes", "scores", "labels")
    CLASS_COUNT = 1
    FOREGROUND_LABEL = FOREGROUND_LABEL

    def __init__(
        self,
        model_path: Path,
        min_score: float = 0.7,
        nms_iou_threshold: float = 0.45,
        max_candidates: int = 10,
        expected_model_sha256: Optional[str] = None,
        session: Optional[Any] = None,
        providers: Optional[Sequence[str]] = None,
    ) -> None:
        if not 0.0 <= min_score <= 1.0:
            raise ValueError("min_score must be between 0 and 1")
        if not 0.0 <= nms_iou_threshold <= 1.0:
            raise ValueError("nms_iou_threshold must be between 0 and 1")
        if isinstance(max_candidates, bool):
            raise ValueError("max_candidates must be a positive integer")
        try:
            normalized_max_candidates = int(max_candidates)
        except (TypeError, ValueError) as error:
            raise ValueError("max_candidates must be a positive integer") from error
        if normalized_max_candidates <= 0 or normalized_max_candidates != max_candidates:
            raise ValueError("max_candidates must be a positive integer")

        expected_model_sha256 = self._validated_expected_hash(
            expected_model_sha256
        )
        self.model_path = Path(model_path).resolve()
        self.min_score = float(min_score)
        self.nms_iou_threshold = float(nms_iou_threshold)
        self.max_candidates = normalized_max_candidates
        self._lock = threading.Lock()

        if self.model_path.is_file():
            self.model_sha256 = self._sha256(self.model_path)
        elif session is not None:
            self.model_sha256 = "test-double"
        else:
            raise FileNotFoundError(f"Detector model was not found: {self.model_path}")

        if (
            expected_model_sha256 is not None
            and self.model_sha256 != "test-double"
            and self.model_sha256 != expected_model_sha256
        ):
            raise ValueError(
                f"Detector model SHA-256 mismatch: {self.model_sha256}"
            )
        self.model_hash_verified = (
            expected_model_sha256 is not None and self.model_sha256 != "test-double"
        )

        self._session = (
            session
            if session is not None
            else self._load_session(providers=providers)
        )
        self._input_name = self._validate_session_contract()

    def _load_session(self, providers: Optional[Sequence[str]]) -> Any:
        # Keep ONNX Runtime optional for unit tests and dataset-only workflows.
        import onnxruntime as ort

        options: Dict[str, Any] = {}
        if providers is not None:
            options["providers"] = list(providers)
        return ort.InferenceSession(str(self.model_path), **options)

    def _validate_session_contract(self) -> str:
        try:
            inputs = list(self._session.get_inputs())
            outputs = list(self._session.get_outputs())
        except (AttributeError, TypeError) as error:
            raise ValueError("Detector session does not expose ONNX input/output metadata") from error

        if len(inputs) != 1:
            raise ValueError(f"Detector must expose exactly one input, got {len(inputs)}")
        try:
            input_shape = tuple(inputs[0].shape)
            input_name = str(inputs[0].name)
        except (AttributeError, TypeError) as error:
            raise ValueError("Detector input metadata is malformed") from error
        if input_shape != self.EXPECTED_INPUT_SHAPE:
            raise ValueError(f"Unexpected detector input shape: {input_shape}")
        if not input_name:
            raise ValueError("Detector input name must not be empty")

        try:
            output_names = tuple(str(output.name) for output in outputs)
        except AttributeError as error:
            raise ValueError("Detector output metadata is malformed") from error
        if len(output_names) != len(set(output_names)):
            raise ValueError("Detector output names must be unique")
        missing = [name for name in self.OUTPUT_NAMES if name not in output_names]
        if missing:
            raise ValueError(
                "Detector is missing required outputs: " + ", ".join(missing)
            )
        return input_name

    @staticmethod
    def _sha256(path: Path) -> str:
        digest = hashlib.sha256()
        with path.open("rb") as source:
            for chunk in iter(lambda: source.read(1024 * 1024), b""):
                digest.update(chunk)
        return digest.hexdigest().upper()

    @staticmethod
    def _validated_expected_hash(value: Optional[str]) -> Optional[str]:
        if value is None:
            return None
        normalized = value.strip().upper()
        if len(normalized) != 64 or any(
            character not in "0123456789ABCDEF" for character in normalized
        ):
            raise ValueError(
                "Expected detector model SHA-256 must contain 64 hex characters"
            )
        return normalized

    @classmethod
    def preprocess(
        cls, image: Image.Image
    ) -> Tuple[np.ndarray, _LetterboxTransform]:
        image = normalize_image_rgb(image)
        width, height = image.size
        if width <= 0 or height <= 0:
            raise ValueError("Image dimensions must be positive")

        scale = min(cls.INPUT_SIZE / width, cls.INPUT_SIZE / height)
        resized_width = max(1, min(cls.INPUT_SIZE, round(width * scale)))
        resized_height = max(1, min(cls.INPUT_SIZE, round(height * scale)))
        pad_x = (cls.INPUT_SIZE - resized_width) // 2
        pad_y = (cls.INPUT_SIZE - resized_height) // 2

        resized = image.resize(
            (resized_width, resized_height), Image.Resampling.BILINEAR
        )
        letterboxed = Image.new(
            "RGB", (cls.INPUT_SIZE, cls.INPUT_SIZE), color=(114, 114, 114)
        )
        letterboxed.paste(resized, (pad_x, pad_y))

        pixels = np.asarray(letterboxed, dtype=np.float32) / 255.0
        tensor = np.transpose(pixels, (2, 0, 1))[None, ...]
        tensor = np.ascontiguousarray(tensor, dtype=np.float32)
        transform = _LetterboxTransform(
            scale_x=resized_width / width,
            scale_y=resized_height / height,
            pad_x=pad_x,
            pad_y=pad_y,
            original_width=width,
            original_height=height,
        )
        return tensor, transform

    def detect(self, image: Image.Image) -> List[Detection]:
        tensor, transform = self.preprocess(image)
        with self._lock:
            raw_outputs = self._session.run(
                list(self.OUTPUT_NAMES), {self._input_name: tensor}
            )
        if not isinstance(raw_outputs, (list, tuple)) or len(raw_outputs) != 3:
            raise ValueError("Detector must return boxes, scores, and labels")

        boxes = np.asarray(raw_outputs[0])
        scores = np.asarray(raw_outputs[1])
        labels = np.asarray(raw_outputs[2])
        boxes, scores, labels = self._validate_outputs(boxes, scores, labels)

        eligible = np.flatnonzero(scores >= self.min_score)
        if eligible.size == 0:
            return []
        order = eligible[np.argsort(-scores[eligible], kind="stable")]
        mapped_boxes = self._restore_boxes(boxes[order], transform)
        mapped_scores = scores[order]
        mapped_labels = labels[order]

        widths = mapped_boxes[:, 2] - mapped_boxes[:, 0]
        heights = mapped_boxes[:, 3] - mapped_boxes[:, 1]
        valid = (widths > 0.0) & (heights > 0.0)
        mapped_boxes = mapped_boxes[valid]
        mapped_scores = mapped_scores[valid]
        mapped_labels = mapped_labels[valid]
        if mapped_boxes.shape[0] == 0:
            return []

        keep = self._nms(
            mapped_boxes, mapped_scores, self.nms_iou_threshold
        )
        return [
            Detection(
                x_min=float(mapped_boxes[index, 0]),
                y_min=float(mapped_boxes[index, 1]),
                x_max=float(mapped_boxes[index, 2]),
                y_max=float(mapped_boxes[index, 3]),
                score=float(mapped_scores[index]),
                label=int(mapped_labels[index]),
            )
            for index in keep[: self.max_candidates]
        ]

    @classmethod
    def _validate_outputs(
        cls,
        boxes: np.ndarray,
        scores: np.ndarray,
        labels: np.ndarray,
    ) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
        if boxes.ndim != 2 or boxes.shape[1:] != (4,):
            raise ValueError(f"Unexpected detector boxes shape: {boxes.shape}")
        expected_vector_shape = (boxes.shape[0],)
        if scores.shape != expected_vector_shape:
            raise ValueError(f"Unexpected detector scores shape: {scores.shape}")
        if labels.shape != expected_vector_shape:
            raise ValueError(f"Unexpected detector labels shape: {labels.shape}")

        arrays = (boxes, scores, labels)
        if any(
            not np.issubdtype(array.dtype, np.number)
            or np.issubdtype(array.dtype, np.complexfloating)
            for array in arrays
        ):
            raise ValueError("Detector outputs must be real numeric arrays")
        try:
            boxes = boxes.astype(np.float32, copy=False)
            scores = scores.astype(np.float32, copy=False)
            numeric_labels = labels.astype(np.float64, copy=False)
        except (TypeError, ValueError) as error:
            raise ValueError("Detector outputs must be numeric") from error

        if not np.all(np.isfinite(boxes)):
            raise ValueError("Detector boxes contain non-finite values")
        if not np.all(np.isfinite(scores)):
            raise ValueError("Detector scores contain non-finite values")
        if not np.all(np.isfinite(numeric_labels)):
            raise ValueError("Detector labels contain non-finite values")
        if np.any(boxes < 0.0) or np.any(boxes > float(cls.INPUT_SIZE)):
            raise ValueError("Detector boxes must stay within the letterboxed image")
        if np.any(boxes[:, 2] < boxes[:, 0]) or np.any(
            boxes[:, 3] < boxes[:, 1]
        ):
            raise ValueError("Detector boxes must use valid xyxy coordinates")
        if np.any(scores < 0.0) or np.any(scores > 1.0):
            raise ValueError("Detector scores must be between 0 and 1")
        if np.any(numeric_labels != np.floor(numeric_labels)):
            raise ValueError("Detector labels must be integers")
        if np.any(numeric_labels != float(cls.FOREGROUND_LABEL)):
            raise ValueError(
                f"Traffic-sign detector labels must all be {cls.FOREGROUND_LABEL}"
            )

        return boxes, scores, numeric_labels.astype(np.int64, copy=False)

    @staticmethod
    def _restore_boxes(
        boxes: np.ndarray, transform: _LetterboxTransform
    ) -> np.ndarray:
        restored = boxes.astype(np.float32, copy=True)
        restored[:, [0, 2]] = (
            restored[:, [0, 2]] - float(transform.pad_x)
        ) / transform.scale_x
        restored[:, [1, 3]] = (
            restored[:, [1, 3]] - float(transform.pad_y)
        ) / transform.scale_y
        restored[:, [0, 2]] = np.clip(
            restored[:, [0, 2]], 0.0, float(transform.original_width)
        )
        restored[:, [1, 3]] = np.clip(
            restored[:, [1, 3]], 0.0, float(transform.original_height)
        )
        return restored

    @staticmethod
    def _nms(
        boxes: np.ndarray, scores: np.ndarray, iou_threshold: float
    ) -> List[int]:
        if boxes.shape[0] == 0:
            return []
        order = np.argsort(-scores, kind="stable")
        areas = (boxes[:, 2] - boxes[:, 0]) * (
            boxes[:, 3] - boxes[:, 1]
        )
        kept: List[int] = []

        while order.size:
            current = int(order[0])
            kept.append(current)
            if order.size == 1:
                break

            remaining = order[1:]
            x_min = np.maximum(boxes[current, 0], boxes[remaining, 0])
            y_min = np.maximum(boxes[current, 1], boxes[remaining, 1])
            x_max = np.minimum(boxes[current, 2], boxes[remaining, 2])
            y_max = np.minimum(boxes[current, 3], boxes[remaining, 3])
            intersection = np.maximum(0.0, x_max - x_min) * np.maximum(
                0.0, y_max - y_min
            )
            union = areas[current] + areas[remaining] - intersection
            iou = np.divide(
                intersection,
                union,
                out=np.zeros_like(intersection),
                where=union > 0.0,
            )
            order = remaining[iou <= iou_threshold]
        return kept

    def metadata(self) -> Dict[str, Any]:
        return {
            "class_count": self.CLASS_COUNT,
            "foreground_label": self.FOREGROUND_LABEL,
            "input_shape": list(self.EXPECTED_INPUT_SHAPE),
            "output_names": list(self.OUTPUT_NAMES),
            "model_sha256": self.model_sha256,
            "model_hash_verified": self.model_hash_verified,
            "preprocessing": (
                "EXIF transpose -> alpha composite on opaque white -> RGB "
                "-> 320x320 letterbox (fill 114) "
                "-> float32 / 255 -> NCHW"
            ),
            "box_format": "xyxy in original-image pixel coordinates",
            "min_score": self.min_score,
            "nms_iou_threshold": self.nms_iou_threshold,
            "max_candidates": self.max_candidates,
        }
