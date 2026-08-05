from __future__ import annotations

import argparse
import csv
import io
import json
import zipfile
from collections import defaultdict
from pathlib import Path

from PIL import Image

from .classifier import (
    PREDICTION_SOURCE_PROTOTYPE,
    RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256,
    RECOVERED_DESIGN_PROTOTYPE_SHA256,
    RECOVERED_MODEL_SHA256,
    RECOVERED_OOD_REFERENCE_SHA256,
    TrafficSignClassifier,
)
from .metadata_paths import public_metadata_path


def find_member(archive: zipfile.ZipFile, suffix: str) -> str:
    matches = [name for name in archive.namelist() if name.endswith(suffix)]
    if len(matches) != 1:
        raise RuntimeError(f"Expected one {suffix} in the archive, found {len(matches)}")
    return matches[0]


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Evaluate the recovered H5 on official GTSRB test data")
    parser.add_argument("--model", type=Path, default=Path("road_scanner.h5"))
    parser.add_argument(
        "--images", type=Path, default=Path("data/gtsrb/GTSRB_Final_Test_Images.zip")
    )
    parser.add_argument(
        "--ground-truth", type=Path, default=Path("data/gtsrb/GTSRB_Final_Test_GT.zip")
    )
    parser.add_argument("--batch-size", type=int, default=128)
    parser.add_argument("--limit", type=int)
    parser.add_argument("--use-roi", action="store_true")
    parser.add_argument("--min-confidence", type=float, default=0.85)
    parser.add_argument("--min-margin", type=float, default=0.15)
    parser.add_argument("--max-crop-aspect", type=float, default=1.45)
    parser.add_argument("--min-visual-std", type=float, default=0.01)
    parser.add_argument("--min-neighbor-correlation", type=float, default=0.4)
    parser.add_argument("--ood-safety-margin", type=float, default=0.07)
    parser.add_argument(
        "--ood-reference", type=Path, default=Path("ml_service/ood_reference.npz")
    )
    parser.add_argument(
        "--design-prototype",
        type=Path,
        default=Path("ml_service/design_prototypes.npz"),
    )
    parser.add_argument(
        "--design-prototype-metadata",
        type=Path,
        default=Path("ml_service/design_prototypes.json"),
    )
    parser.add_argument(
        "--no-design-prototype",
        action="store_true",
        help="Disable the rejected-only canonical design prototype fallback",
    )
    return parser


def build_prediction_source_report(
    source_total: dict[str, int],
    source_correct: dict[str, int],
    source_accepted: dict[str, int],
    source_accepted_correct: dict[str, int],
) -> dict[str, dict[str, int | float | None]]:
    report: dict[str, dict[str, int | float | None]] = {}
    for source in sorted(source_total):
        total = source_total[source]
        correct = source_correct[source]
        accepted = source_accepted[source]
        accepted_correct = source_accepted_correct[source]
        report[source] = {
            "samples": total,
            "correct": correct,
            "accuracy": correct / total if total else None,
            "accepted_samples": accepted,
            "accepted_correct": accepted_correct,
            "accepted_accuracy": accepted_correct / accepted if accepted else None,
        }
    return report


def main() -> None:
    args = build_parser().parse_args()

    if args.batch_size < 1:
        raise ValueError("batch size must be positive")
    design_prototype_enabled = not args.no_design_prototype
    classifier = TrafficSignClassifier(
        model_path=args.model,
        class_map_path=Path("ml_service/class_map.json"),
        min_confidence=args.min_confidence,
        min_margin=args.min_margin,
        max_crop_aspect=args.max_crop_aspect,
        min_visual_std=args.min_visual_std,
        min_neighbor_correlation=args.min_neighbor_correlation,
        ood_safety_margin=args.ood_safety_margin,
        expected_model_sha256=RECOVERED_MODEL_SHA256,
        ood_reference_path=args.ood_reference,
        expected_ood_reference_sha256=RECOVERED_OOD_REFERENCE_SHA256,
        design_prototype_path=(
            args.design_prototype if design_prototype_enabled else None
        ),
        expected_design_prototype_sha256=(
            RECOVERED_DESIGN_PROTOTYPE_SHA256
            if design_prototype_enabled
            else None
        ),
        design_prototype_metadata_path=(
            args.design_prototype_metadata if design_prototype_enabled else None
        ),
        expected_design_prototype_metadata_sha256=(
            RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256
            if design_prototype_enabled
            else None
        ),
    )

    with zipfile.ZipFile(args.images) as image_zip, zipfile.ZipFile(args.ground_truth) as gt_zip:
        gt_member = find_member(gt_zip, "GT-final_test.csv")
        with gt_zip.open(gt_member) as raw_csv:
            reader = csv.DictReader(io.TextIOWrapper(raw_csv, encoding="utf-8-sig"), delimiter=";")
            rows = list(reader)
        if args.limit:
            rows = rows[: args.limit]

        image_members = {
            Path(name).name: name
            for name in image_zip.namelist()
            if name.lower().endswith(".ppm")
        }
        correct = 0
        total = 0
        accepted = 0
        accepted_correct = 0
        class_correct = defaultdict(int)
        class_total = defaultdict(int)
        source_total = defaultdict(int)
        source_correct = defaultdict(int)
        source_accepted = defaultdict(int)
        source_accepted_correct = defaultdict(int)

        for offset in range(0, len(rows), args.batch_size):
            batch_rows = rows[offset : offset + args.batch_size]
            images = []
            labels = []
            for row in batch_rows:
                member = image_members.get(row["Filename"])
                if member is None:
                    raise RuntimeError(f"Missing test image: {row['Filename']}")
                with image_zip.open(member) as source:
                    image = Image.open(source).convert("RGB")
                    if args.use_roi:
                        image = image.crop(
                            (
                                int(row["Roi.X1"]),
                                int(row["Roi.Y1"]),
                                int(row["Roi.X2"]) + 1,
                                int(row["Roi.Y2"]) + 1,
                            )
                        )
                    images.append(image.copy())
                    labels.append(int(row["ClassId"]))

            predictions = classifier.predict_many(images)
            for expected, prediction in zip(labels, predictions):
                total += 1
                class_total[expected] += 1
                is_correct = expected == prediction.class_id
                source = prediction.prediction_source
                source_total[source] += 1
                if is_correct:
                    source_correct[source] += 1
                if prediction.accepted:
                    accepted += 1
                    source_accepted[source] += 1
                    if is_correct:
                        accepted_correct += 1
                        source_accepted_correct[source] += 1
                if is_correct:
                    correct += 1
                    class_correct[expected] += 1

    per_class = {
        str(class_id): class_correct[class_id] / class_total[class_id]
        for class_id in sorted(class_total)
    }
    prediction_sources = build_prediction_source_report(
        source_total,
        source_correct,
        source_accepted,
        source_accepted_correct,
    )
    prototype_accepted = source_accepted[PREDICTION_SOURCE_PROTOTYPE]
    prototype_correct = source_accepted_correct[PREDICTION_SOURCE_PROTOTYPE]
    result = {
        "model": public_metadata_path(args.model),
        "preprocessing": "BGR, bicubic 30x30, float32 / 255",
        "roi_crop": args.use_roi,
        "samples": total,
        "correct": correct,
        "accuracy": correct / total,
        "macro_accuracy": sum(per_class.values()) / len(per_class),
        "thresholds": {
            "min_confidence": args.min_confidence,
            "min_margin": args.min_margin,
            "max_landscape_crop_aspect": args.max_crop_aspect,
            "min_visual_std": args.min_visual_std,
            "min_neighbor_correlation": args.min_neighbor_correlation,
            "ood_safety_margin": args.ood_safety_margin,
            "ood_threshold_overrides": classifier.metadata()[
                "ood_threshold_overrides"
            ],
            "ood_reference_sha256": classifier.ood_reference_sha256,
            "design_prototype": {
                "enabled": design_prototype_enabled,
                "sha256": classifier.design_prototype_sha256,
                "metadata_sha256": classifier.design_prototype_metadata_sha256,
                "min_similarity": classifier.design_prototype_min_similarity,
                "min_margin": classifier.design_prototype_min_margin,
                "threshold_overrides": classifier.metadata()[
                    "design_prototype_threshold_overrides"
                ],
                "raw_match_classes": classifier.metadata()[
                    "design_prototype_raw_match_classes"
                ],
            },
        },
        "accepted_samples": accepted,
        "coverage": accepted / total,
        "accepted_accuracy": accepted_correct / accepted if accepted else None,
        "rejected_samples": total - accepted,
        "prediction_sources": prediction_sources,
        "prototype_rescue": {
            "accepted_samples": prototype_accepted,
            "correct_samples": prototype_correct,
            "accuracy": (
                prototype_correct / prototype_accepted
                if prototype_accepted
                else None
            ),
        },
        "per_class_accuracy": per_class,
    }
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
