from __future__ import annotations

import argparse
import hashlib
import json
import re
import zipfile
from collections import defaultdict
from pathlib import Path

import numpy as np
from PIL import Image

from .classifier import TrafficSignClassifier


MEMBER_PATTERN = re.compile(r"/([0-9]{5})/([0-9]{5})_([0-9]{5})[.]ppm$")
FEATURE_LAYER = "batch_normalization_2"
DEFAULT_TRAINING_SHA256 = (
    "D32AC4B5FA9A1CBD1994768413902E8193599D9434CF0A8EB9CFD00A6D3A290C"
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def normalized(values: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(values, axis=1, keepdims=True)
    return values / np.maximum(norms, 1e-12)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Build an in-distribution feature reference from GTSRB training tracks"
    )
    parser.add_argument("--model", type=Path, default=Path("road_scanner.h5"))
    parser.add_argument(
        "--training",
        type=Path,
        default=Path("data/gtsrb/GTSRB_Final_Training_Images.zip"),
    )
    parser.add_argument("--output", type=Path, default=Path("ml_service/ood_reference.npz"))
    parser.add_argument("--batch-size", type=int, default=256)
    parser.add_argument("--calibration-fold", type=int, default=0)
    parser.add_argument("--fold-count", type=int, default=5)
    parser.add_argument("--reject-quantile", type=float, default=0.005)
    args = parser.parse_args()

    if args.batch_size < 1:
        raise ValueError("batch size must be positive")
    if args.fold_count < 2 or not 0 <= args.calibration_fold < args.fold_count:
        raise ValueError("calibration fold must be within a fold count of at least two")
    if not 0.0 < args.reject_quantile < 0.5:
        raise ValueError("reject quantile must be between zero and 0.5")
    actual_training_hash = sha256(args.training)
    if actual_training_hash != DEFAULT_TRAINING_SHA256:
        raise RuntimeError(
            f"Unexpected training archive SHA-256: {actual_training_hash}"
        )

    import tensorflow as tf

    model = tf.keras.models.load_model(str(args.model), compile=False)
    feature_model = tf.keras.Model(model.inputs[0], model.get_layer(FEATURE_LAYER).output)

    reference_by_class = defaultdict(list)
    calibration_features = []
    calibration_predictions = []
    calibration_labels = []
    with zipfile.ZipFile(args.training) as archive:
        members = []
        for name in archive.namelist():
            match = MEMBER_PATTERN.search(name)
            if match:
                class_id, track_id, _ = (int(value) for value in match.groups())
                members.append((name, class_id, track_id))

        for offset in range(0, len(members), args.batch_size):
            batch_members = members[offset : offset + args.batch_size]
            tensors = []
            for name, _, _ in batch_members:
                with archive.open(name) as source:
                    tensors.append(TrafficSignClassifier.preprocess(Image.open(source))[0])
            batch = np.asarray(tensors)
            batch_features = feature_model(batch, training=False).numpy()
            batch_probabilities = model(batch, training=False).numpy()
            for row, feature, probabilities in zip(
                batch_members, batch_features, batch_probabilities
            ):
                _, class_id, track_id = row
                if track_id % args.fold_count == args.calibration_fold:
                    calibration_features.append(feature)
                    calibration_predictions.append(int(np.argmax(probabilities)))
                    calibration_labels.append(class_id)
                else:
                    reference_by_class[class_id].append(feature)

    expected_classes = set(range(43))
    if set(reference_by_class) != expected_classes:
        raise RuntimeError("Training reference is missing one or more GTSRB classes")

    centroids = []
    for class_id in range(43):
        class_features = normalized(np.asarray(reference_by_class[class_id], dtype=np.float32))
        centroid = class_features.mean(axis=0)
        centroid /= max(float(np.linalg.norm(centroid)), 1e-12)
        centroids.append(centroid)
    centroids_array = np.asarray(centroids, dtype=np.float32)

    calibration_array = normalized(np.asarray(calibration_features, dtype=np.float32))
    predicted_array = np.asarray(calibration_predictions, dtype=np.int64)
    label_array = np.asarray(calibration_labels, dtype=np.int64)
    similarities = np.sum(calibration_array * centroids_array[predicted_array], axis=1)
    thresholds = np.empty(43, dtype=np.float32)
    for class_id in range(43):
        class_scores = similarities[predicted_array == class_id]
        if len(class_scores) < 30:
            raise RuntimeError(f"Too few calibration predictions for class {class_id}")
        thresholds[class_id] = np.quantile(class_scores, args.reject_quantile)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(
        args.output,
        centroids=centroids_array,
        thresholds=thresholds,
    )
    artifact_hash = sha256(args.output)
    metadata = {
        "schema_version": 1,
        "feature_layer": FEATURE_LAYER,
        "model_sha256": sha256(args.model),
        "training_archive_sha256": actual_training_hash,
        "split": {
            "unit": "GTSRB track",
            "fold_count": args.fold_count,
            "calibration_fold": args.calibration_fold,
            "reference_tracks": "track_id modulo fold_count != calibration_fold",
        },
        "reject_quantile": args.reject_quantile,
        "reference_samples": sum(len(values) for values in reference_by_class.values()),
        "calibration_samples": len(calibration_array),
        "calibration_classifier_accuracy": float(
            np.mean(predicted_array == label_array)
        ),
        "calibration_ood_acceptance": float(
            np.mean(similarities >= thresholds[predicted_array])
        ),
        "artifact_sha256": artifact_hash,
        "threshold_min": float(thresholds.min()),
        "threshold_max": float(thresholds.max()),
    }
    metadata_path = args.output.with_suffix(".json")
    metadata_path.write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
