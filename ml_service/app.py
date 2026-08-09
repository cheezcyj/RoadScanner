from __future__ import annotations

import logging
import math
import os
from pathlib import Path
from typing import Any, Dict, Tuple

from flask import Flask, Response, jsonify, request

from .classifier import (
    DEFAULT_DESIGN_PROTOTYPE_RAW_MATCH_CLASSES,
    DEFAULT_DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES,
    DEFAULT_OOD_THRESHOLD_OVERRIDES,
    RECOVERED_CLASS_MAP_SHA256,
    RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256,
    RECOVERED_DESIGN_PROTOTYPE_SHA256,
    RECOVERED_MODEL_SHA256,
    RECOVERED_OOD_REFERENCE_SHA256,
    TrafficSignClassifier,
)
from .detector import TRAINED_DETECTOR_MODEL_SHA256, TrafficSignDetector
from .image_fetcher import ImageFetchError, ImageFetcher, ImageFetchUnavailable
from .pipeline import PIPELINE_MODE_DETECT, TrafficSignPipeline


REPOSITORY_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OOD_THRESHOLD_OVERRIDES_ENV = ",".join(
    f"{class_id}:{threshold:g}"
    for class_id, threshold in sorted(DEFAULT_OOD_THRESHOLD_OVERRIDES.items())
)
DEFAULT_DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES_ENV = ",".join(
    f"{class_id}:{threshold:g}"
    for class_id, threshold in sorted(
        DEFAULT_DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES.items()
    )
)
DEFAULT_DESIGN_PROTOTYPE_RAW_MATCH_CLASSES_ENV = ",".join(
    str(class_id) for class_id in sorted(DEFAULT_DESIGN_PROTOTYPE_RAW_MATCH_CLASSES)
)


def _float_env(name: str, default: str) -> float:
    try:
        return float(os.environ.get(name, default))
    except ValueError as error:
        raise ValueError(f"{name} must be numeric") from error


def _int_env(name: str, default: str) -> int:
    try:
        return int(os.environ.get(name, default))
    except ValueError as error:
        raise ValueError(f"{name} must be an integer") from error


def _bool_env(name: str, default: str) -> bool:
    value = os.environ.get(name, default).strip().lower()
    if value in {"1", "true", "yes", "on"}:
        return True
    if value in {"0", "false", "no", "off"}:
        return False
    raise ValueError(f"{name} must be a boolean")


def _parse_ood_threshold_overrides(value: str) -> Dict[int, float]:
    if not isinstance(value, str):
        raise ValueError("ROADSCANNER_OOD_THRESHOLD_OVERRIDES must be text")
    if not value.strip():
        return {}

    overrides: Dict[int, float] = {}
    for entry in value.split(","):
        parts = entry.strip().split(":")
        if len(parts) != 2 or not all(part.strip() for part in parts):
            raise ValueError(
                "ROADSCANNER_OOD_THRESHOLD_OVERRIDES must use class_id:threshold entries"
            )
        try:
            class_id = int(parts[0].strip())
        except ValueError as error:
            raise ValueError(
                "ROADSCANNER_OOD_THRESHOLD_OVERRIDES class IDs must be integers"
            ) from error
        try:
            threshold = float(parts[1].strip())
        except ValueError as error:
            raise ValueError(
                "ROADSCANNER_OOD_THRESHOLD_OVERRIDES thresholds must be numeric"
            ) from error
        if not 0 <= class_id < 43:
            raise ValueError(
                "ROADSCANNER_OOD_THRESHOLD_OVERRIDES class IDs must be between 0 and 42"
            )
        if not math.isfinite(threshold) or not 0.0 <= threshold <= 1.0:
            raise ValueError(
                "ROADSCANNER_OOD_THRESHOLD_OVERRIDES thresholds must be between 0 and 1"
            )
        if class_id in overrides:
            raise ValueError(
                "ROADSCANNER_OOD_THRESHOLD_OVERRIDES must not repeat a class ID"
            )
        overrides[class_id] = threshold
    return overrides


def _parse_design_prototype_threshold_overrides(value: str) -> Dict[int, float]:
    name = "ROADSCANNER_DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES"
    if not isinstance(value, str):
        raise ValueError(f"{name} must be text")
    if not value.strip():
        return {}

    overrides: Dict[int, float] = {}
    for entry in value.split(","):
        parts = entry.strip().split(":")
        if len(parts) != 2 or not all(part.strip() for part in parts):
            raise ValueError(f"{name} must use class_id:threshold entries")
        try:
            class_id = int(parts[0].strip())
        except ValueError as error:
            raise ValueError(f"{name} class IDs must be integers") from error
        try:
            threshold = float(parts[1].strip())
        except ValueError as error:
            raise ValueError(f"{name} thresholds must be numeric") from error
        if not 0 <= class_id < 43:
            raise ValueError(f"{name} class IDs must be between 0 and 42")
        if not math.isfinite(threshold) or not 0.0 <= threshold <= 1.0:
            raise ValueError(f"{name} thresholds must be between 0 and 1")
        if class_id in overrides:
            raise ValueError(f"{name} must not repeat a class ID")
        overrides[class_id] = threshold
    return overrides


def _parse_design_prototype_raw_match_classes(value: str) -> frozenset[int]:
    name = "ROADSCANNER_DESIGN_PROTOTYPE_RAW_MATCH_CLASSES"
    if not isinstance(value, str):
        raise ValueError(f"{name} must be text")
    if not value.strip():
        return frozenset()

    class_ids: set[int] = set()
    for entry in value.split(","):
        normalized = entry.strip()
        if not normalized:
            raise ValueError(f"{name} must use comma-separated class IDs")
        try:
            class_id = int(normalized)
        except ValueError as error:
            raise ValueError(f"{name} class IDs must be integers") from error
        if not 0 <= class_id < 43:
            raise ValueError(f"{name} class IDs must be between 0 and 42")
        if class_id in class_ids:
            raise ValueError(f"{name} must not repeat a class ID")
        class_ids.add(class_id)
    return frozenset(class_ids)


def create_app(
    config: Dict[str, Any] | None = None,
    classifier: TrafficSignClassifier | None = None,
    image_fetcher: ImageFetcher | None = None,
    detector: TrafficSignDetector | None = None,
) -> Flask:
    app = Flask(__name__)
    app.config.update(
        MODEL_PATH=os.environ.get(
            "ROADSCANNER_MODEL_PATH", str(REPOSITORY_ROOT / "road_scanner.h5")
        ),
        CLASS_MAP_PATH=os.environ.get(
            "ROADSCANNER_CLASS_MAP_PATH", str(Path(__file__).with_name("class_map.json"))
        ),
        OOD_REFERENCE_PATH=os.environ.get(
            "ROADSCANNER_OOD_REFERENCE_PATH",
            str(Path(__file__).with_name("ood_reference.npz")),
        ),
        EXPECTED_MODEL_SHA256=os.environ.get(
            "ROADSCANNER_EXPECTED_MODEL_SHA256", RECOVERED_MODEL_SHA256
        ),
        EXPECTED_CLASS_MAP_SHA256=os.environ.get(
            "ROADSCANNER_EXPECTED_CLASS_MAP_SHA256", RECOVERED_CLASS_MAP_SHA256
        ),
        EXPECTED_OOD_REFERENCE_SHA256=os.environ.get(
            "ROADSCANNER_EXPECTED_OOD_REFERENCE_SHA256",
            RECOVERED_OOD_REFERENCE_SHA256,
        ),
        ALLOWED_IMAGE_HOSTS=os.environ.get(
            "ROADSCANNER_ALLOWED_IMAGE_HOSTS", "127.0.0.1,localhost,::1"
        ),
        ALLOWED_IMAGE_PORTS=os.environ.get(
            "ROADSCANNER_ALLOWED_IMAGE_PORTS", "80,443,18080"
        ),
        MAX_IMAGE_BYTES=_int_env("ROADSCANNER_MAX_IMAGE_BYTES", str(5 * 1024 * 1024)),
        MAX_IMAGE_PIXELS=_int_env("ROADSCANNER_MAX_IMAGE_PIXELS", "25000000"),
        MAX_IMAGE_DIMENSION=_int_env("ROADSCANNER_MAX_IMAGE_DIMENSION", "10000"),
        IMAGE_TIMEOUT_SECONDS=_float_env("ROADSCANNER_IMAGE_TIMEOUT_SECONDS", "5"),
        MIN_CONFIDENCE=_float_env("ROADSCANNER_MIN_CONFIDENCE", "0.85"),
        MIN_MARGIN=_float_env("ROADSCANNER_MIN_MARGIN", "0.15"),
        MAX_CROP_ASPECT=_float_env("ROADSCANNER_MAX_CROP_ASPECT", "1.45"),
        MIN_VISUAL_STD=_float_env("ROADSCANNER_MIN_VISUAL_STD", "0.01"),
        MIN_NEIGHBOR_CORRELATION=_float_env(
            "ROADSCANNER_MIN_NEIGHBOR_CORRELATION", "0.4"
        ),
        OOD_SAFETY_MARGIN=_float_env("ROADSCANNER_OOD_SAFETY_MARGIN", "0.07"),
        OOD_THRESHOLD_OVERRIDES=_parse_ood_threshold_overrides(
            os.environ.get(
                "ROADSCANNER_OOD_THRESHOLD_OVERRIDES",
                DEFAULT_OOD_THRESHOLD_OVERRIDES_ENV,
            )
        ),
        DESIGN_PROTOTYPE_ENABLED=_bool_env(
            "ROADSCANNER_DESIGN_PROTOTYPE_ENABLED", "true"
        ),
        DESIGN_PROTOTYPE_PATH=os.environ.get(
            "ROADSCANNER_DESIGN_PROTOTYPE_PATH",
            str(Path(__file__).with_name("design_prototypes.npz")),
        ),
        DESIGN_PROTOTYPE_METADATA_PATH=os.environ.get(
            "ROADSCANNER_DESIGN_PROTOTYPE_METADATA_PATH",
            str(Path(__file__).with_name("design_prototypes.json")),
        ),
        EXPECTED_DESIGN_PROTOTYPE_SHA256=os.environ.get(
            "ROADSCANNER_EXPECTED_DESIGN_PROTOTYPE_SHA256",
            RECOVERED_DESIGN_PROTOTYPE_SHA256,
        ),
        EXPECTED_DESIGN_PROTOTYPE_METADATA_SHA256=os.environ.get(
            "ROADSCANNER_EXPECTED_DESIGN_PROTOTYPE_METADATA_SHA256",
            RECOVERED_DESIGN_PROTOTYPE_METADATA_SHA256,
        ),
        DESIGN_PROTOTYPE_MIN_SIMILARITY=_float_env(
            "ROADSCANNER_DESIGN_PROTOTYPE_MIN_SIMILARITY", "0.80"
        ),
        DESIGN_PROTOTYPE_MIN_MARGIN=_float_env(
            "ROADSCANNER_DESIGN_PROTOTYPE_MIN_MARGIN", "0.15"
        ),
        DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES=(
            _parse_design_prototype_threshold_overrides(
                os.environ.get(
                    "ROADSCANNER_DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES",
                    DEFAULT_DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES_ENV,
                )
            )
        ),
        DESIGN_PROTOTYPE_RAW_MATCH_CLASSES=(
            _parse_design_prototype_raw_match_classes(
                os.environ.get(
                    "ROADSCANNER_DESIGN_PROTOTYPE_RAW_MATCH_CLASSES",
                    DEFAULT_DESIGN_PROTOTYPE_RAW_MATCH_CLASSES_ENV,
                )
            )
        ),
        PIPELINE_MODE=os.environ.get("ROADSCANNER_PIPELINE_MODE", "detect"),
        DETECTOR_MODEL_PATH=os.environ.get(
            "ROADSCANNER_DETECTOR_MODEL_PATH",
            str(REPOSITORY_ROOT / "traffic_sign_detector.onnx"),
        ),
        EXPECTED_DETECTOR_MODEL_SHA256=os.environ.get(
            "ROADSCANNER_EXPECTED_DETECTOR_MODEL_SHA256",
            TRAINED_DETECTOR_MODEL_SHA256,
        ),
        DETECTOR_MIN_SCORE=_float_env("ROADSCANNER_DETECTOR_MIN_SCORE", "0.7"),
        DETECTOR_NMS_IOU_THRESHOLD=_float_env(
            "ROADSCANNER_DETECTOR_NMS_IOU_THRESHOLD", "0.45"
        ),
        DETECTOR_MAX_CANDIDATES=_int_env(
            "ROADSCANNER_DETECTOR_MAX_CANDIDATES", "10"
        ),
        ALLOW_CROP_FALLBACK=_bool_env("ROADSCANNER_ALLOW_CROP_FALLBACK", "true"),
        CROP_FALLBACK_MAX_ASPECT=_float_env(
            "ROADSCANNER_CROP_FALLBACK_MAX_ASPECT", "1.25"
        ),
    )
    if config:
        app.config.update(config)

    classifier = classifier or TrafficSignClassifier(
        model_path=Path(app.config["MODEL_PATH"]),
        class_map_path=Path(app.config["CLASS_MAP_PATH"]),
        min_confidence=float(app.config["MIN_CONFIDENCE"]),
        min_margin=float(app.config["MIN_MARGIN"]),
        max_crop_aspect=float(app.config["MAX_CROP_ASPECT"]),
        min_visual_std=float(app.config["MIN_VISUAL_STD"]),
        min_neighbor_correlation=float(app.config["MIN_NEIGHBOR_CORRELATION"]),
        ood_safety_margin=float(app.config["OOD_SAFETY_MARGIN"]),
        ood_threshold_overrides=(
            _parse_ood_threshold_overrides(app.config["OOD_THRESHOLD_OVERRIDES"])
            if isinstance(app.config["OOD_THRESHOLD_OVERRIDES"], str)
            else app.config["OOD_THRESHOLD_OVERRIDES"]
        ),
        expected_model_sha256=str(app.config["EXPECTED_MODEL_SHA256"]),
        expected_class_map_sha256=str(app.config["EXPECTED_CLASS_MAP_SHA256"]),
        ood_reference_path=Path(app.config["OOD_REFERENCE_PATH"]),
        expected_ood_reference_sha256=str(
            app.config["EXPECTED_OOD_REFERENCE_SHA256"]
        ),
        design_prototype_path=(
            Path(app.config["DESIGN_PROTOTYPE_PATH"])
            if bool(app.config["DESIGN_PROTOTYPE_ENABLED"])
            else None
        ),
        expected_design_prototype_sha256=str(
            app.config["EXPECTED_DESIGN_PROTOTYPE_SHA256"]
        ),
        design_prototype_metadata_path=(
            Path(app.config["DESIGN_PROTOTYPE_METADATA_PATH"])
            if bool(app.config["DESIGN_PROTOTYPE_ENABLED"])
            else None
        ),
        expected_design_prototype_metadata_sha256=str(
            app.config["EXPECTED_DESIGN_PROTOTYPE_METADATA_SHA256"]
        ),
        design_prototype_min_similarity=float(
            app.config["DESIGN_PROTOTYPE_MIN_SIMILARITY"]
        ),
        design_prototype_min_margin=float(
            app.config["DESIGN_PROTOTYPE_MIN_MARGIN"]
        ),
        design_prototype_threshold_overrides=(
            _parse_design_prototype_threshold_overrides(
                app.config["DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES"]
            )
            if isinstance(app.config["DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES"], str)
            else app.config["DESIGN_PROTOTYPE_THRESHOLD_OVERRIDES"]
        ),
        design_prototype_raw_match_classes=(
            _parse_design_prototype_raw_match_classes(
                app.config["DESIGN_PROTOTYPE_RAW_MATCH_CLASSES"]
            )
            if isinstance(app.config["DESIGN_PROTOTYPE_RAW_MATCH_CLASSES"], str)
            else app.config["DESIGN_PROTOTYPE_RAW_MATCH_CLASSES"]
        ),
    )
    image_fetcher = image_fetcher or ImageFetcher(
        allowed_hosts=str(app.config["ALLOWED_IMAGE_HOSTS"]).split(","),
        allowed_ports=str(app.config["ALLOWED_IMAGE_PORTS"]).split(","),
        max_bytes=int(app.config["MAX_IMAGE_BYTES"]),
        max_pixels=int(app.config["MAX_IMAGE_PIXELS"]),
        max_dimension=int(app.config["MAX_IMAGE_DIMENSION"]),
        timeout_seconds=float(app.config["IMAGE_TIMEOUT_SECONDS"]),
    )
    pipeline_mode = str(app.config["PIPELINE_MODE"]).strip().lower()
    if pipeline_mode == PIPELINE_MODE_DETECT and detector is None:
        expected_detector_hash = app.config["EXPECTED_DETECTOR_MODEL_SHA256"]
        if (
            not isinstance(expected_detector_hash, str)
            or not expected_detector_hash.strip()
        ):
            raise ValueError(
                "EXPECTED_DETECTOR_MODEL_SHA256 is required in detect pipeline mode"
            )
        detector = TrafficSignDetector(
            model_path=Path(app.config["DETECTOR_MODEL_PATH"]),
            min_score=float(app.config["DETECTOR_MIN_SCORE"]),
            nms_iou_threshold=float(app.config["DETECTOR_NMS_IOU_THRESHOLD"]),
            max_candidates=int(app.config["DETECTOR_MAX_CANDIDATES"]),
            expected_model_sha256=expected_detector_hash.strip(),
        )
    pipeline = TrafficSignPipeline(
        classifier=classifier,
        mode=pipeline_mode,
        detector=detector,
        allow_crop_fallback=bool(app.config["ALLOW_CROP_FALLBACK"]),
        crop_fallback_max_aspect=float(app.config["CROP_FALLBACK_MAX_ASPECT"]),
    )

    app.extensions["roadscanner_classifier"] = classifier
    app.extensions["roadscanner_image_fetcher"] = image_fetcher
    app.extensions["roadscanner_detector"] = detector
    app.extensions["roadscanner_pipeline"] = pipeline

    @app.get("/health")
    def health() -> Tuple[Response, int]:
        return jsonify(
            {"status": "ready", **classifier.metadata(), **pipeline.metadata()}
        ), 200

    def run_prediction():
        payload = request.get_json(silent=True)
        if not isinstance(payload, dict):
            return None, (jsonify({"error": "JSON object required"}), 400)
        image_url = payload.get("image_url")
        if not isinstance(image_url, str) or not image_url.strip():
            return None, (jsonify({"error": "image_url is required"}), 400)
        try:
            image = image_fetcher.fetch(image_url.strip())
            return pipeline.analyze(image), None
        except ImageFetchUnavailable as error:
            app.logger.warning("Image source is temporarily unavailable: %s", error)
            return None, (jsonify({"error": str(error)}), 503)
        except ImageFetchError as error:
            app.logger.info("Rejected image input: %s", error)
            return None, (jsonify({"error": str(error)}), 422)
        except Exception:
            app.logger.exception("Traffic-sign inference failed")
            return None, (jsonify({"error": "inference failed"}), 503)

    @app.post("/predict")
    def predict():
        prediction, error = run_prediction()
        if error:
            return error
        response = Response(str(prediction.result_id), status=200, mimetype="text/plain")
        response.headers["X-RoadScanner-Catalog-SHA256"] = classifier.class_map_sha256
        return response

    @app.post("/predict/debug")
    def predict_debug():
        prediction, error = run_prediction()
        if error:
            return error
        return jsonify(
            {
                **prediction.to_dict(),
                **classifier.metadata(),
                **pipeline.metadata(),
            }
        ), 200

    return app


def main() -> None:
    from waitress import serve

    logging.basicConfig(level=os.environ.get("ROADSCANNER_ML_LOG_LEVEL", "INFO"))
    host = os.environ.get("ROADSCANNER_ML_HOST", "127.0.0.1")
    port = _int_env("ROADSCANNER_ML_PORT", "5000")
    if host not in {"127.0.0.1", "localhost", "::1"}:
        raise ValueError("The recovery service binds to loopback only")
    serve(create_app(), host=host, port=port, threads=4)


if __name__ == "__main__":
    main()
