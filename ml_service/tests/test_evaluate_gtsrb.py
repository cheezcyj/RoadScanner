import subprocess
import sys
from pathlib import Path

import pytest

from ml_service.evaluate_gtsrb import (
    build_parser,
    build_prediction_source_report,
)


ROOT = Path(__file__).resolve().parents[2]


def test_cli_enables_design_prototype_by_default_and_supports_explicit_disable():
    parser = build_parser()

    assert parser.parse_args([]).no_design_prototype is False
    assert parser.parse_args(["--no-design-prototype"]).no_design_prototype is True


def test_prediction_source_report_counts_accepted_and_correct_results():
    report = build_prediction_source_report(
        source_total={
            "canonical_design_prototype": 6,
            "recovered_cnn": 10,
            "rejected": 2,
        },
        source_correct={
            "canonical_design_prototype": 6,
            "recovered_cnn": 9,
            "rejected": 1,
        },
        source_accepted={
            "canonical_design_prototype": 6,
            "recovered_cnn": 10,
            "rejected": 0,
        },
        source_accepted_correct={
            "canonical_design_prototype": 6,
            "recovered_cnn": 9,
            "rejected": 0,
        },
    )

    assert report["canonical_design_prototype"] == {
        "samples": 6,
        "correct": 6,
        "accuracy": 1.0,
        "accepted_samples": 6,
        "accepted_correct": 6,
        "accepted_accuracy": 1.0,
    }
    assert report["recovered_cnn"]["accepted_accuracy"] == pytest.approx(0.9)
    assert report["rejected"]["accepted_accuracy"] is None


def test_import_does_not_eagerly_load_tensorflow():
    result = subprocess.run(
        [
            sys.executable,
            "-c",
            "import sys; import ml_service.evaluate_gtsrb; "
            "print(int('tensorflow' in sys.modules))",
        ],
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
    )

    assert result.stdout.strip() == "0"
