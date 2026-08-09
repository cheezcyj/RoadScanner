from __future__ import annotations

import argparse
import json
import os
import statistics
import sys
import time
from collections import Counter
from pathlib import Path
from typing import Any, Callable, Dict, Mapping, Sequence

from PIL import Image, ImageOps

from .classifier import (
    RECOVERED_CLASS_MAP_SHA256,
    RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256,
    RECOVERED_DESIGN_PROTOTYPE_SHA256,
    RECOVERED_MODEL_SHA256,
    RECOVERED_OOD_REFERENCE_SHA256,
    TrafficSignClassifier,
)
from .detector import FOREGROUND_LABEL, Detection, TrafficSignDetector
from .metadata_paths import public_metadata_path
from .pipeline import TrafficSignPipeline
from .train_detector import (
    DatasetManifest,
    DetectionRecord,
    calculate_detection_metrics,
    load_dataset_manifest,
)


EVALUATION_SCHEMA_VERSION = 1


def _percentile(values: Sequence[float], percentile: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(float(value) for value in values)
    if len(ordered) == 1:
        return ordered[0]
    position = (len(ordered) - 1) * percentile
    lower = int(position)
    upper = min(lower + 1, len(ordered) - 1)
    fraction = position - lower
    return ordered[lower] + (ordered[upper] - ordered[lower]) * fraction


def summarize_latency(milliseconds: Sequence[float]) -> Dict[str, float | int]:
    values = [float(value) for value in milliseconds]
    if not values:
        return {
            "count": 0,
            "total_ms": 0.0,
            "mean_ms": 0.0,
            "median_ms": 0.0,
            "p50_ms": 0.0,
            "p95_ms": 0.0,
            "p99_ms": 0.0,
            "min_ms": 0.0,
            "max_ms": 0.0,
            "throughput_images_per_second": 0.0,
        }
    total = sum(values)
    return {
        "count": len(values),
        "total_ms": total,
        "mean_ms": statistics.fmean(values),
        "median_ms": statistics.median(values),
        "p50_ms": _percentile(values, 0.50),
        "p95_ms": _percentile(values, 0.95),
        "p99_ms": _percentile(values, 0.99),
        "min_ms": min(values),
        "max_ms": max(values),
        "throughput_images_per_second": (
            len(values) * 1000.0 / total if total > 0.0 else 0.0
        ),
    }


def _load_evaluation_image(record: DetectionRecord) -> Image.Image:
    try:
        with Image.open(record.image_path) as source:
            # Runtime detection uses the same EXIF transpose. Normalizing here
            # makes the ground-truth pixel conversion use the analyzed geometry.
            return ImageOps.exif_transpose(source).convert("RGB")
    except OSError as error:
        raise RuntimeError(
            f"cannot load evaluation image {record.image_id}: {record.image_path}"
        ) from error


def _absolute_ground_truths(
    record: DetectionRecord, image: Image.Image
) -> list[tuple[float, float, float, float]]:
    width, height = image.size
    return [
        (xmin * width, ymin * height, xmax * width, ymax * height)
        for xmin, ymin, xmax, ymax in record.boxes
    ]


def _validated_predictions(
    detections: Sequence[Detection], image_id: str
) -> list[tuple[float, tuple[float, float, float, float]]]:
    predictions = []
    for detection in detections:
        if int(detection.label) != FOREGROUND_LABEL:
            raise ValueError(
                f"{image_id}: detector foreground label must be {FOREGROUND_LABEL}"
            )
        predictions.append(
            (
                float(detection.score),
                (
                    float(detection.x_min),
                    float(detection.y_min),
                    float(detection.x_max),
                    float(detection.y_max),
                ),
            )
        )
    return predictions


class _ReplayDetector:
    """Feeds already-timed detections into the exact production pipeline logic."""

    def __init__(self, metadata: Mapping[str, Any]):
        self._metadata = dict(metadata)
        self.detections: tuple[Detection, ...] = ()

    def detect(self, image: Image.Image) -> list[Detection]:
        del image
        return list(self.detections)

    def metadata(self) -> Dict[str, Any]:
        return dict(self._metadata)


def _ratio(numerator: int, denominator: int) -> float:
    return numerator / denominator if denominator else 0.0


def evaluate_records(
    manifest: DatasetManifest,
    detector: TrafficSignDetector,
    *,
    split: str = "validation",
    score_threshold: float = 0.7,
    match_iou_threshold: float = 0.5,
    max_detections: int = 10,
    limit: int | None = None,
    warmup_runs: int = 3,
    classifier: TrafficSignClassifier | None = None,
    clock_ns: Callable[[], int] = time.perf_counter_ns,
) -> Dict[str, Any]:
    if split not in manifest.records:
        raise ValueError(f"dataset manifest does not contain split: {split}")
    if not 0.0 <= score_threshold <= 1.0:
        raise ValueError("score threshold must be in [0, 1]")
    if not 0.0 < match_iou_threshold <= 1.0:
        raise ValueError("match IoU threshold must be in (0, 1]")
    if max_detections < 1:
        raise ValueError("max detections must be positive")
    if limit is not None and limit < 1:
        raise ValueError("limit must be positive")
    if warmup_runs < 0:
        raise ValueError("warmup runs cannot be negative")

    records = list(manifest.records[split])
    if limit is not None:
        records = records[:limit]
    if not records:
        raise ValueError(f"dataset split has no records: {split}")

    detector_metadata = detector.metadata()
    first_image = _load_evaluation_image(records[0])
    for _ in range(warmup_runs):
        detector.detect(first_image)

    ground_truths: Dict[str, list[tuple[float, float, float, float]]] = {}
    predictions: Dict[
        str, list[tuple[float, tuple[float, float, float, float]]]
    ] = {}
    detector_latency_ms: list[float] = []
    image_detection_counts: list[int] = []
    negative_images = 0
    negative_images_accepted = 0
    positive_images_with_detection = 0

    replay_detector = _ReplayDetector(detector_metadata)
    pipeline = (
        TrafficSignPipeline(classifier, mode="detect", detector=replay_detector)
        if classifier is not None
        else None
    )
    pipeline_post_detection_ms: list[float] = []
    pipeline_end_to_end_ms: list[float] = []
    pipeline_accepted = 0
    pipeline_positive_accepted = 0
    pipeline_negative_accepted = 0
    pipeline_reasons: Counter[str] = Counter()
    pipeline_result_ids: Counter[str] = Counter()

    for index, record in enumerate(records):
        image = first_image if index == 0 else _load_evaluation_image(record)
        ground_truths[record.image_id] = _absolute_ground_truths(record, image)

        started = clock_ns()
        detections = list(detector.detect(image))
        elapsed_ms = (clock_ns() - started) / 1_000_000.0
        if elapsed_ms < 0.0:
            raise RuntimeError("evaluation clock moved backwards")
        detector_latency_ms.append(elapsed_ms)
        predictions[record.image_id] = _validated_predictions(
            detections, record.image_id
        )

        threshold_detections = tuple(
            detection
            for detection in detections[:max_detections]
            if float(detection.score) >= score_threshold
        )
        image_detection_counts.append(len(threshold_detections))
        has_detection = bool(threshold_detections)
        if record.is_negative:
            negative_images += 1
            negative_images_accepted += int(has_detection)
        else:
            positive_images_with_detection += int(has_detection)

        if pipeline is not None:
            replay_detector.detections = threshold_detections
            pipeline_started = clock_ns()
            result = pipeline.analyze(image)
            post_detection_ms = (clock_ns() - pipeline_started) / 1_000_000.0
            if post_detection_ms < 0.0:
                raise RuntimeError("evaluation clock moved backwards")
            pipeline_post_detection_ms.append(post_detection_ms)
            pipeline_end_to_end_ms.append(elapsed_ms + post_detection_ms)
            pipeline_accepted += int(result.accepted)
            pipeline_positive_accepted += int(result.accepted and not record.is_negative)
            pipeline_negative_accepted += int(result.accepted and record.is_negative)
            pipeline_reasons[result.reason] += 1
            pipeline_result_ids[str(result.result_id)] += 1

    metrics = calculate_detection_metrics(
        ground_truths,
        predictions,
        score_threshold=score_threshold,
        iou_threshold=match_iou_threshold,
        max_detections=max_detections,
    )
    positive_images = len(records) - negative_images
    images_with_detection = sum(count > 0 for count in image_detection_counts)
    result: Dict[str, Any] = {
        "schema_version": EVALUATION_SCHEMA_VERSION,
        "dataset": {
            "name": manifest.dataset,
            "version": manifest.dataset_version,
            "manifest": public_metadata_path(manifest.path),
            "manifest_sha256": manifest.sha256,
            "split": split,
            "evaluated_images": len(records),
            "positive_images": positive_images,
            "negative_images": negative_images,
            "limit": limit,
        },
        "contract": {
            "foreground_label": FOREGROUND_LABEL,
            "input": "RGB float32 /255 NCHW, 320x320 letterbox fill 114",
            "boxes": "xyxy original-image pixel coordinates",
            "score_threshold": score_threshold,
            "match_iou_threshold": match_iou_threshold,
            "max_detections_per_image": max_detections,
        },
        "detector": detector_metadata,
        "detector_metrics": {
            **metrics,
            "images_with_detection": images_with_detection,
            "positive_images_with_detection": positive_images_with_detection,
            "positive_image_detection_rate": _ratio(
                positive_images_with_detection, positive_images
            ),
            "negative_images_accepted": negative_images_accepted,
            "negative_image_acceptance_rate": _ratio(
                negative_images_accepted, negative_images
            ),
            "detections_at_threshold": sum(image_detection_counts),
        },
        "latency": {
            "warmup_runs": warmup_runs,
            "detector": summarize_latency(detector_latency_ms),
        },
    }

    if pipeline is not None and classifier is not None:
        classifier_metadata = getattr(classifier, "metadata", None)
        result["full_pipeline"] = {
            "semantic_class_accuracy": None,
            "semantic_class_accuracy_reason": (
                "The generic Open Images detector manifest has no GTSRB class labels."
            ),
            "accepted_images": pipeline_accepted,
            "acceptance_rate": _ratio(pipeline_accepted, len(records)),
            "positive_images_accepted": pipeline_positive_accepted,
            "positive_image_acceptance_rate": _ratio(
                pipeline_positive_accepted, positive_images
            ),
            "negative_images_accepted": pipeline_negative_accepted,
            "negative_image_acceptance_rate": _ratio(
                pipeline_negative_accepted, negative_images
            ),
            "reasons": dict(sorted(pipeline_reasons.items())),
            "result_ids": dict(sorted(pipeline_result_ids.items())),
            "classifier": (
                classifier_metadata() if callable(classifier_metadata) else None
            ),
            "latency": {
                "post_detection": summarize_latency(pipeline_post_detection_ms),
                "end_to_end": summarize_latency(pipeline_end_to_end_ms),
            },
        }
    return result


def _atomic_json_write(payload: Mapping[str, Any], path: Path) -> None:
    path = path.resolve()
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".part")
    temporary.write_text(
        json.dumps(payload, indent=2, ensure_ascii=False, allow_nan=False) + "\n",
        encoding="utf-8",
    )
    os.replace(temporary, path)


def _load_classifier(args: argparse.Namespace) -> TrafficSignClassifier | None:
    if args.classifier_model is None:
        if args.ood_reference is not None or args.design_prototype is not None:
            raise ValueError(
                "--ood-reference and --design-prototype require --classifier-model"
            )
        return None
    return TrafficSignClassifier(
        model_path=args.classifier_model,
        class_map_path=args.class_map,
        min_confidence=args.classifier_min_confidence,
        min_margin=args.classifier_min_margin,
        ood_safety_margin=args.classifier_ood_safety_margin,
        expected_model_sha256=(
            args.expected_classifier_sha256 or RECOVERED_MODEL_SHA256
        ),
        expected_class_map_sha256=(
            args.expected_class_map_sha256 or RECOVERED_CLASS_MAP_SHA256
        ),
        ood_reference_path=args.ood_reference,
        expected_ood_reference_sha256=(
            (args.expected_ood_reference_sha256 or RECOVERED_OOD_REFERENCE_SHA256)
            if args.ood_reference is not None
            else None
        ),
        design_prototype_path=args.design_prototype,
        expected_design_prototype_sha256=(
            (
                args.expected_design_prototype_sha256
                or RECOVERED_DESIGN_PROTOTYPE_SHA256
            )
            if args.design_prototype is not None
            else None
        ),
        design_prototype_metadata_path=args.design_prototype_metadata,
        expected_design_prototype_metadata_sha256=(
            (
                args.expected_design_prototype_metadata_sha256
                or RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256
            )
            if args.design_prototype is not None
            else None
        ),
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Evaluate a RoadScanner ONNX detector on a pinned dataset manifest; "
            "optionally evaluate classifier-assisted pipeline acceptance"
        )
    )
    parser.add_argument(
        "--manifest",
        type=Path,
        default=Path("data/detector/open-images-v7/manifest.json"),
    )
    parser.add_argument("--detector", type=Path, required=True)
    parser.add_argument("--expected-detector-sha256")
    parser.add_argument("--split", choices=("train", "validation"), default="validation")
    parser.add_argument("--score-threshold", type=float, default=0.7)
    parser.add_argument("--match-iou-threshold", type=float, default=0.5)
    parser.add_argument("--nms-iou-threshold", type=float, default=0.45)
    parser.add_argument("--max-detections", type=int, default=10)
    parser.add_argument("--limit", type=int)
    parser.add_argument("--warmup-runs", type=int, default=3)
    parser.add_argument(
        "--provider",
        action="append",
        dest="providers",
        help="ONNX Runtime provider; repeat to set provider priority",
    )
    parser.add_argument("--classifier-model", type=Path)
    parser.add_argument(
        "--class-map", type=Path, default=Path("ml_service/class_map.json")
    )
    parser.add_argument("--ood-reference", type=Path)
    parser.add_argument("--design-prototype", type=Path)
    parser.add_argument("--design-prototype-metadata", type=Path)
    parser.add_argument("--expected-classifier-sha256")
    parser.add_argument("--expected-class-map-sha256")
    parser.add_argument("--expected-ood-reference-sha256")
    parser.add_argument("--expected-design-prototype-sha256")
    parser.add_argument("--expected-design-prototype-metadata-sha256")
    parser.add_argument("--classifier-min-confidence", type=float, default=0.85)
    parser.add_argument("--classifier-min-margin", type=float, default=0.15)
    parser.add_argument("--classifier-ood-safety-margin", type=float, default=0.07)
    parser.add_argument("--output", type=Path)
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        manifest = load_dataset_manifest(args.manifest)
        # Keep every model-emitted score for AP ranking; evaluate the operational
        # threshold separately in calculate_detection_metrics.
        detector = TrafficSignDetector(
            model_path=args.detector,
            min_score=0.0,
            nms_iou_threshold=args.nms_iou_threshold,
            max_candidates=args.max_detections,
            expected_model_sha256=args.expected_detector_sha256,
            providers=args.providers,
        )
        classifier = _load_classifier(args)
        report = evaluate_records(
            manifest,
            detector,
            split=args.split,
            score_threshold=args.score_threshold,
            match_iou_threshold=args.match_iou_threshold,
            max_detections=args.max_detections,
            limit=args.limit,
            warmup_runs=args.warmup_runs,
            classifier=classifier,
        )
        if args.output is not None:
            _atomic_json_write(report, args.output)
        print(json.dumps(report, indent=2, ensure_ascii=False, allow_nan=False))
        return 0
    except (OSError, RuntimeError, ValueError) as error:
        print(f"detector evaluation failed: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
