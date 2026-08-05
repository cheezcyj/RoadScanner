import json
import shutil
import subprocess
import sys
import uuid
from pathlib import Path
from types import SimpleNamespace

import pytest
from PIL import Image

from ml_service.classifier import Prediction
from ml_service.detector import Detection
from ml_service.evaluate_detector import (
    build_parser,
    evaluate_records,
    summarize_latency,
)
from ml_service.train_detector import DatasetManifest, DetectionRecord


ROOT = Path(__file__).resolve().parents[2]


@pytest.fixture
def evaluation_files():
    path = ROOT / "data" / "test-evaluate-detector" / uuid.uuid4().hex
    path.mkdir(parents=True)
    try:
        positive = path / "positive.png"
        negative = path / "negative.png"
        Image.new("RGB", (100, 100), color=(255, 0, 0)).save(positive)
        Image.new("RGB", (100, 100), color=(0, 0, 255)).save(negative)
        yield path, positive, negative
    finally:
        shutil.rmtree(path, ignore_errors=True)


def manifest(path, positive, negative):
    records = (
        DetectionRecord(
            image_id="positive",
            image_path=positive,
            boxes=((0.1, 0.1, 0.5, 0.5),),
        ),
        DetectionRecord(
            image_id="negative",
            image_path=negative,
            boxes=(),
            is_negative=True,
        ),
    )
    return DatasetManifest(
        path=path / "manifest.json",
        sha256="A" * 64,
        dataset="Open Images",
        dataset_version="v7",
        records={"validation": records},
    )


class FakeDetector:
    def __init__(self, bad_label=False):
        self.bad_label = bad_label
        self.calls = 0

    def detect(self, image):
        self.calls += 1
        if image.getpixel((0, 0))[0] > image.getpixel((0, 0))[2]:
            return [Detection(10, 10, 50, 50, 0.9, 0 if self.bad_label else 1)]
        return [Detection(5, 5, 15, 15, 0.8)]

    def metadata(self):
        return {
            "foreground_label": 1,
            "model_sha256": "B" * 64,
            "input_shape": [1, 3, 320, 320],
        }


class AcceptingClassifier:
    def __init__(self):
        self.class_map = SimpleNamespace(
            unknown_result_id=44,
            unknown={
                "result_id": 44,
                "key": "unknown",
                "name_en": "Unknown",
                "name_ko": "알 수 없음",
            },
        )

    def predict_many(self, images):
        return [
            Prediction(
                class_id=33,
                result_id=34,
                key="turn_right_ahead",
                name_en="Turn right ahead",
                name_ko="우회전",
                confidence=0.99,
                margin=0.98,
                ood_similarity=0.99,
                ood_threshold=0.8,
                accepted=True,
                reason="ok",
                top3=[],
            )
            for _ in images
        ]

    def metadata(self):
        return {"model_sha256": "C" * 64, "class_count": 43}


def test_latency_summary_reports_interpolated_percentiles_and_throughput():
    summary = summarize_latency([1.0, 2.0, 3.0, 4.0])

    assert summary["count"] == 4
    assert summary["total_ms"] == 10.0
    assert summary["mean_ms"] == 2.5
    assert summary["median_ms"] == 2.5
    assert summary["p50_ms"] == 2.5
    assert summary["p95_ms"] == pytest.approx(3.85)
    assert summary["throughput_images_per_second"] == 400.0


def test_detector_report_includes_ap_threshold_metrics_and_negative_acceptance(
    evaluation_files,
):
    path, positive, negative = evaluation_files
    report = evaluate_records(
        manifest(path, positive, negative),
        FakeDetector(),
        score_threshold=0.5,
        warmup_runs=0,
    )

    metrics = report["detector_metrics"]
    assert metrics["ap50"] == pytest.approx(1.0)
    assert metrics["precision"] == pytest.approx(0.5)
    assert metrics["recall"] == pytest.approx(1.0)
    assert metrics["true_positives"] == 1
    assert metrics["false_positives"] == 1
    assert metrics["negative_images_accepted"] == 1
    assert metrics["negative_image_acceptance_rate"] == 1.0
    assert report["contract"]["foreground_label"] == 1
    assert report["dataset"]["manifest"].startswith("data/")
    assert report["dataset"]["manifest_sha256"] == "A" * 64
    assert report["latency"]["detector"]["count"] == 2
    json.dumps(report, allow_nan=False)


def test_optional_classifier_reports_full_pipeline_acceptance_without_fake_accuracy(
    evaluation_files,
):
    path, positive, negative = evaluation_files
    report = evaluate_records(
        manifest(path, positive, negative),
        FakeDetector(),
        score_threshold=0.5,
        warmup_runs=0,
        classifier=AcceptingClassifier(),
    )

    pipeline = report["full_pipeline"]
    assert pipeline["semantic_class_accuracy"] is None
    assert pipeline["accepted_images"] == 2
    assert pipeline["positive_images_accepted"] == 1
    assert pipeline["negative_images_accepted"] == 1
    assert pipeline["reasons"] == {"ok": 2}
    assert pipeline["classifier"]["class_count"] == 43
    assert pipeline["latency"]["end_to_end"]["count"] == 2


def test_unexpected_foreground_label_fails_closed(evaluation_files):
    path, positive, negative = evaluation_files

    with pytest.raises(ValueError, match="foreground label must be 1"):
        evaluate_records(
            manifest(path, positive, negative),
            FakeDetector(bad_label=True),
            warmup_runs=0,
        )


def test_cli_contract_and_import_do_not_require_onnx_or_tensorflow():
    args = build_parser().parse_args(["--detector", "detector.onnx"])

    assert args.split == "validation"
    assert args.score_threshold == 0.7
    assert args.match_iou_threshold == 0.5
    assert args.max_detections == 10
    assert args.classifier_ood_safety_margin == 0.07
    assert args.warmup_runs == 3

    result = subprocess.run(
        [
            sys.executable,
            "-c",
            "import sys; import ml_service.evaluate_detector; "
            "print(int('onnxruntime' in sys.modules or 'tensorflow' in sys.modules))",
        ],
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    assert result.stdout.strip() == "0"
