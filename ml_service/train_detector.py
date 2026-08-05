from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import platform
import random
import sys
import time
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Dict, Iterable, Mapping, Sequence

from .metadata_paths import public_metadata_path


IMAGE_SIZE = 320
LETTERBOX_FILL = 114
MODEL_NAME = "ssdlite320_mobilenet_v3_large"
MODEL_CLASS_COUNT = 2  # torchvision background=0, traffic_sign=1
TARGET_LABEL = 1
EXPORTED_LABEL = 1
ONNX_INPUT_NAME = "images"
ONNX_OUTPUT_NAMES = ("boxes", "scores", "labels")
CHECKPOINT_SCHEMA_VERSION = 1
RUN_SCHEMA_VERSION = 1


@dataclass(frozen=True)
class LetterboxGeometry:
    source_width: int
    source_height: int
    target_size: int
    resized_width: int
    resized_height: int
    pad_left: int
    pad_top: int

    @property
    def scale_x(self) -> float:
        return self.resized_width / self.source_width

    @property
    def scale_y(self) -> float:
        return self.resized_height / self.source_height


@dataclass(frozen=True)
class DetectionRecord:
    image_id: str
    image_path: Path
    boxes: tuple[tuple[float, float, float, float], ...]
    is_negative: bool = False


@dataclass(frozen=True)
class AugmentationPlan:
    horizontal_flip: bool
    brightness: float
    contrast: float
    color: float


@dataclass(frozen=True)
class DatasetManifest:
    path: Path
    sha256: str
    dataset: str
    dataset_version: str
    records: Mapping[str, tuple[DetectionRecord, ...]]


@dataclass(frozen=True)
class TrainingConfig:
    manifest: Path
    output_dir: Path
    epochs: int
    batch_size: int
    learning_rate: float
    momentum: float
    weight_decay: float
    seed: int
    device: str
    amp: bool
    pretrained_backbone: bool
    score_threshold: float
    iou_threshold: float
    max_detections: int
    resume: Path | None
    export_onnx: bool
    onnx_path: Path | None


def sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest().upper()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def compute_letterbox_geometry(
    width: int, height: int, target_size: int = IMAGE_SIZE
) -> LetterboxGeometry:
    if width <= 0 or height <= 0:
        raise ValueError(f"image dimensions must be positive, got {width}x{height}")
    if target_size <= 0:
        raise ValueError("target size must be positive")
    scale = min(target_size / width, target_size / height)
    resized_width = min(target_size, max(1, int(round(width * scale))))
    resized_height = min(target_size, max(1, int(round(height * scale))))
    return LetterboxGeometry(
        source_width=width,
        source_height=height,
        target_size=target_size,
        resized_width=resized_width,
        resized_height=resized_height,
        pad_left=(target_size - resized_width) // 2,
        pad_top=(target_size - resized_height) // 2,
    )


def transform_normalized_box(
    box: Sequence[float], geometry: LetterboxGeometry
) -> tuple[float, float, float, float]:
    if len(box) != 4:
        raise ValueError("bounding box must contain xmin, ymin, xmax, ymax")
    xmin, ymin, xmax, ymax = (float(value) for value in box)
    if not (0.0 <= xmin < xmax <= 1.0 and 0.0 <= ymin < ymax <= 1.0):
        raise ValueError(f"invalid normalized bounding box: {tuple(box)}")
    left = geometry.pad_left + xmin * geometry.source_width * geometry.scale_x
    top = geometry.pad_top + ymin * geometry.source_height * geometry.scale_y
    right = geometry.pad_left + xmax * geometry.source_width * geometry.scale_x
    bottom = geometry.pad_top + ymax * geometry.source_height * geometry.scale_y
    size = float(geometry.target_size)
    return (
        min(size, max(0.0, left)),
        min(size, max(0.0, top)),
        min(size, max(0.0, right)),
        min(size, max(0.0, bottom)),
    )


def sample_augmentation_plan(rng: Any = random) -> AugmentationPlan:
    return AugmentationPlan(
        horizontal_flip=rng.random() < 0.5,
        brightness=rng.uniform(0.8, 1.2),
        contrast=rng.uniform(0.8, 1.2),
        color=rng.uniform(0.8, 1.2),
    )


def _flip_normalized_box(
    box: Sequence[float],
) -> tuple[float, float, float, float]:
    xmin, ymin, xmax, ymax = (float(value) for value in box)
    return 1.0 - xmax, ymin, 1.0 - xmin, ymax


def apply_augmentation(
    image: Any,
    boxes: Sequence[Sequence[float]],
    plan: AugmentationPlan,
) -> tuple[Any, tuple[tuple[float, float, float, float], ...]]:
    from PIL import Image, ImageEnhance

    augmented_boxes = tuple(tuple(float(value) for value in box) for box in boxes)
    if plan.horizontal_flip:
        image = image.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
        augmented_boxes = tuple(_flip_normalized_box(box) for box in augmented_boxes)
    image = ImageEnhance.Brightness(image).enhance(plan.brightness)
    image = ImageEnhance.Contrast(image).enhance(plan.contrast)
    image = ImageEnhance.Color(image).enhance(plan.color)
    return image, augmented_boxes


def _resolve_image_path(
    manifest_path: Path, split: str, image_id: str, raw_path: object
) -> Path:
    if not isinstance(raw_path, str) or not raw_path.strip():
        raise ValueError(f"{split}/{image_id}: image path is missing")
    candidate = Path(raw_path)
    if candidate.is_absolute():
        return candidate
    candidates = (
        Path.cwd() / candidate,
        manifest_path.parent / candidate,
        manifest_path.parent / "images" / split / f"{image_id}.jpg",
    )
    for resolved in candidates:
        if resolved.is_file():
            return resolved.resolve()
    # Keep the canonical portable layout in the error emitted by __getitem__.
    return candidates[-1].resolve()


def _parse_manifest_box(
    raw_box: object, split: str, image_id: str
) -> tuple[float, float, float, float]:
    if not isinstance(raw_box, Mapping):
        raise ValueError(f"{split}/{image_id}: box must be an object")
    try:
        xmin = float(raw_box["xmin"])
        xmax = float(raw_box["xmax"])
        ymin = float(raw_box["ymin"])
        ymax = float(raw_box["ymax"])
    except (KeyError, TypeError, ValueError) as error:
        raise ValueError(f"{split}/{image_id}: malformed bounding box") from error
    if not all(math.isfinite(value) for value in (xmin, ymin, xmax, ymax)):
        raise ValueError(f"{split}/{image_id}: non-finite bounding box")
    if not (0.0 <= xmin < xmax <= 1.0 and 0.0 <= ymin < ymax <= 1.0):
        raise ValueError(f"{split}/{image_id}: invalid normalized bounding box")
    return xmin, ymin, xmax, ymax


def load_dataset_manifest(path: Path) -> DatasetManifest:
    path = path.resolve()
    try:
        payload = path.read_bytes()
    except OSError as error:
        raise ValueError(f"cannot read dataset manifest: {path}") from error
    try:
        raw = json.loads(payload)
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise ValueError(f"invalid dataset manifest JSON: {path}") from error
    if not isinstance(raw, Mapping):
        raise ValueError("dataset manifest root must be an object")
    if raw.get("schema_version") != 1:
        raise ValueError("unsupported dataset manifest schema_version")
    class_spec = raw.get("class")
    if not isinstance(class_spec, Mapping) or (
        class_spec.get("id") != 0 or class_spec.get("name") != "traffic_sign"
    ):
        raise ValueError("manifest must describe generic traffic_sign class id 0")
    raw_splits = raw.get("splits")
    if not isinstance(raw_splits, Mapping):
        raise ValueError("dataset manifest splits must be an object")

    records_by_split: Dict[str, tuple[DetectionRecord, ...]] = {}
    for split in ("train", "validation"):
        split_spec = raw_splits.get(split)
        if not isinstance(split_spec, Mapping):
            raise ValueError(f"dataset manifest is missing {split} split")
        raw_images = split_spec.get("images")
        if not isinstance(raw_images, list) or not raw_images:
            raise ValueError(f"dataset manifest {split} split has no images")
        records = []
        seen_ids: set[str] = set()
        for index, raw_image in enumerate(raw_images):
            if not isinstance(raw_image, Mapping):
                raise ValueError(f"{split} image {index} must be an object")
            image_id = raw_image.get("image_id")
            if not isinstance(image_id, str) or not image_id:
                raise ValueError(f"{split} image {index} has no image_id")
            if image_id in seen_ids:
                raise ValueError(f"duplicate image_id in {split}: {image_id}")
            seen_ids.add(image_id)
            raw_boxes = raw_image.get("boxes")
            is_negative = raw_image.get("is_negative", False)
            if not isinstance(is_negative, bool):
                raise ValueError(f"{split}/{image_id}: is_negative must be boolean")
            if not isinstance(raw_boxes, list):
                raise ValueError(f"{split}/{image_id}: boxes must be a list")
            if is_negative and raw_boxes:
                raise ValueError(
                    f"{split}/{image_id}: hard-negative image must have no boxes"
                )
            if not is_negative and not raw_boxes:
                raise ValueError(f"{split}/{image_id}: image has no boxes")
            boxes = tuple(
                _parse_manifest_box(raw_box, split, image_id)
                for raw_box in raw_boxes
            )
            records.append(
                DetectionRecord(
                    image_id=image_id,
                    image_path=_resolve_image_path(
                        path, split, image_id, raw_image.get("path")
                    ),
                    boxes=boxes,
                    is_negative=is_negative,
                )
            )
        records_by_split[split] = tuple(records)

    return DatasetManifest(
        path=path,
        sha256=sha256_bytes(payload),
        dataset=str(raw.get("dataset", "")),
        dataset_version=str(raw.get("dataset_version", "")),
        records=records_by_split,
    )


class OpenImagesDetectionDataset:
    """Torch-compatible dataset without importing torch at module import time."""

    def __init__(
        self,
        records: Sequence[DetectionRecord],
        image_size: int = IMAGE_SIZE,
        *,
        augment: bool = False,
        rng: Any | None = None,
    ):
        self.records = tuple(records)
        self.image_size = image_size
        self.augment = augment
        self.rng = rng if rng is not None else random

    def __len__(self) -> int:
        return len(self.records)

    def __getitem__(self, index: int) -> tuple[Any, Dict[str, Any]]:
        # These are deliberately lazy: production inference and ordinary pytest do
        # not need a 2+ GB detector-training environment.
        import numpy as np
        import torch
        from PIL import Image

        record = self.records[index]
        try:
            with Image.open(record.image_path) as source:
                image = source.convert("RGB")
        except OSError as error:
            raise RuntimeError(f"cannot load training image: {record.image_path}") from error
        normalized_boxes = record.boxes
        augmentation_plan = (
            sample_augmentation_plan(self.rng) if self.augment else None
        )
        geometry = compute_letterbox_geometry(*image.size, self.image_size)
        resized = image.resize(
            (geometry.resized_width, geometry.resized_height),
            resample=Image.Resampling.BILINEAR,
        )
        letterboxed = Image.new(
            "RGB",
            (self.image_size, self.image_size),
            color=(LETTERBOX_FILL, LETTERBOX_FILL, LETTERBOX_FILL),
        )
        letterboxed.paste(resized, (geometry.pad_left, geometry.pad_top))
        # Apply photometric work only after reducing the source to the fixed
        # detector input. Open Images originals can be multi-megapixel, and
        # enhancing them before resize needlessly starves the GPU data path.
        if augmentation_plan is not None:
            letterboxed, normalized_boxes = apply_augmentation(
                letterboxed, normalized_boxes, augmentation_plan
            )
        pixels = np.asarray(letterboxed, dtype=np.float32).copy()
        image_tensor = torch.from_numpy(pixels).permute(2, 0, 1).div_(255.0)

        boxes = [transform_normalized_box(box, geometry) for box in normalized_boxes]
        target = build_detection_target(boxes, index, torch)
        return image_tensor, target


def build_detection_target(
    boxes: Sequence[Sequence[float]], index: int, torch: Any
) -> Dict[str, Any]:
    if boxes:
        boxes_tensor = torch.tensor(boxes, dtype=torch.float32).reshape(-1, 4)
        areas = (boxes_tensor[:, 2] - boxes_tensor[:, 0]) * (
            boxes_tensor[:, 3] - boxes_tensor[:, 1]
        )
    else:
        # torchvision detection models require an empty target to retain the
        # two-dimensional [N, 4] box contract for hard-negative images.
        boxes_tensor = torch.empty((0, 4), dtype=torch.float32)
        areas = torch.empty((0,), dtype=torch.float32)
    return {
        "boxes": boxes_tensor,
        "labels": torch.full((len(boxes),), TARGET_LABEL, dtype=torch.int64),
        "image_id": torch.tensor([index], dtype=torch.int64),
        "area": areas,
        "iscrowd": torch.zeros((len(boxes),), dtype=torch.int64),
    }


def detection_collate(batch: Sequence[tuple[Any, Mapping[str, Any]]]) -> tuple:
    return tuple(zip(*batch))


def box_iou(
    first: Sequence[float], second: Sequence[float]
) -> float:
    left = max(float(first[0]), float(second[0]))
    top = max(float(first[1]), float(second[1]))
    right = min(float(first[2]), float(second[2]))
    bottom = min(float(first[3]), float(second[3]))
    intersection = max(0.0, right - left) * max(0.0, bottom - top)
    first_area = max(0.0, float(first[2]) - float(first[0])) * max(
        0.0, float(first[3]) - float(first[1])
    )
    second_area = max(0.0, float(second[2]) - float(second[0])) * max(
        0.0, float(second[3]) - float(second[1])
    )
    union = first_area + second_area - intersection
    return intersection / union if union > 0.0 else 0.0


def calculate_detection_metrics(
    ground_truths: Mapping[str, Sequence[Sequence[float]]],
    predictions: Mapping[str, Sequence[tuple[float, Sequence[float]]]],
    *,
    score_threshold: float = 0.5,
    iou_threshold: float = 0.5,
    max_detections: int = 100,
) -> Dict[str, float | int]:
    """Calculate one-class COCO-style 101-point AP50 and threshold metrics."""

    if not 0.0 <= score_threshold <= 1.0:
        raise ValueError("score_threshold must be in [0, 1]")
    if not 0.0 < iou_threshold <= 1.0:
        raise ValueError("iou_threshold must be in (0, 1]")
    if max_detections < 1:
        raise ValueError("max_detections must be positive")

    total_ground_truths = sum(len(boxes) for boxes in ground_truths.values())
    ranked: list[tuple[float, str, int, Sequence[float]]] = []
    for image_id, image_predictions in predictions.items():
        ordered = sorted(
            enumerate(image_predictions), key=lambda item: (-float(item[1][0]), item[0])
        )[:max_detections]
        for original_index, (score, box) in ordered:
            ranked.append((float(score), str(image_id), original_index, box))
    ranked.sort(key=lambda item: (-item[0], item[1], item[2]))

    matched: Dict[str, set[int]] = {}
    true_positive_flags: list[int] = []
    false_positive_flags: list[int] = []
    for _, image_id, _, predicted_box in ranked:
        image_ground_truths = ground_truths.get(image_id, ())
        already_matched = matched.setdefault(image_id, set())
        best_index = -1
        best_iou = -1.0
        for index, actual_box in enumerate(image_ground_truths):
            if index in already_matched:
                continue
            overlap = box_iou(predicted_box, actual_box)
            if overlap > best_iou:
                best_iou = overlap
                best_index = index
        is_true_positive = best_index >= 0 and best_iou >= iou_threshold
        if is_true_positive:
            already_matched.add(best_index)
        true_positive_flags.append(int(is_true_positive))
        false_positive_flags.append(int(not is_true_positive))

    cumulative_true: list[int] = []
    cumulative_false: list[int] = []
    running_true = 0
    running_false = 0
    for true_flag, false_flag in zip(true_positive_flags, false_positive_flags):
        running_true += true_flag
        running_false += false_flag
        cumulative_true.append(running_true)
        cumulative_false.append(running_false)

    recalls = [
        value / total_ground_truths if total_ground_truths else 0.0
        for value in cumulative_true
    ]
    precisions = [
        true_count / (true_count + false_count)
        for true_count, false_count in zip(cumulative_true, cumulative_false)
    ]
    if total_ground_truths:
        interpolated = []
        for step in range(101):
            recall_level = step / 100.0
            candidates = [
                precision
                for recall, precision in zip(recalls, precisions)
                if recall >= recall_level
            ]
            interpolated.append(max(candidates, default=0.0))
        ap50 = sum(interpolated) / 101.0
    else:
        ap50 = 0.0

    threshold_count = sum(1 for score, *_ in ranked if score >= score_threshold)
    threshold_true = sum(true_positive_flags[:threshold_count])
    threshold_false = sum(false_positive_flags[:threshold_count])
    precision = (
        threshold_true / (threshold_true + threshold_false)
        if threshold_true + threshold_false
        else 0.0
    )
    recall = threshold_true / total_ground_truths if total_ground_truths else 0.0
    return {
        "ap50": ap50,
        "precision": precision,
        "recall": recall,
        "true_positives": threshold_true,
        "false_positives": threshold_false,
        "ground_truths": total_ground_truths,
        "predictions_at_threshold": threshold_count,
    }


def _import_training_stack() -> Dict[str, Any]:
    try:
        import numpy as np
        import onnx
        import onnxruntime as ort
        import torch
        import torchvision
    except ImportError as error:
        raise RuntimeError(
            "detector training dependencies are missing; install the PyTorch wheel "
            "and ml_service/requirements-detector-training.txt"
        ) from error
    return {
        "np": np,
        "onnx": onnx,
        "ort": ort,
        "torch": torch,
        "torchvision": torchvision,
    }


def _set_reproducible_seed(seed: int, stack: Mapping[str, Any]) -> None:
    np = stack["np"]
    torch = stack["torch"]
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)
    torch.backends.cudnn.benchmark = False
    torch.backends.cudnn.deterministic = True
    torch.use_deterministic_algorithms(True, warn_only=True)


def _select_device(requested: str, torch: Any) -> Any:
    if requested == "auto":
        requested = "cuda" if torch.cuda.is_available() else "cpu"
    if requested == "cuda" and not torch.cuda.is_available():
        raise RuntimeError("CUDA was requested but torch.cuda.is_available() is false")
    return torch.device(requested)


def _build_model(stack: Mapping[str, Any], pretrained_backbone: bool) -> Any:
    torchvision = stack["torchvision"]
    detector_module = torchvision.models.detection
    backbone_weights = None
    if pretrained_backbone:
        backbone_weights = (
            torchvision.models.MobileNet_V3_Large_Weights.IMAGENET1K_V1
        )
    return detector_module.ssdlite320_mobilenet_v3_large(
        weights=None,
        weights_backbone=backbone_weights,
        num_classes=MODEL_CLASS_COUNT,
    )


def _make_grad_scaler(torch: Any, enabled: bool) -> Any:
    try:
        return torch.amp.GradScaler("cuda", enabled=enabled)
    except (AttributeError, TypeError):  # torch < 2.3 compatibility
        return torch.cuda.amp.GradScaler(enabled=enabled)


def _autocast(torch: Any, enabled: bool) -> Any:
    try:
        return torch.amp.autocast("cuda", enabled=enabled)
    except AttributeError:  # torch < 2.0 compatibility
        return torch.cuda.amp.autocast(enabled=enabled)


def _create_loader(
    dataset: OpenImagesDetectionDataset,
    *,
    batch_size: int,
    shuffle: bool,
    seed: int,
    device: Any,
    torch: Any,
) -> Any:
    generator = torch.Generator()
    generator.manual_seed(seed)
    return torch.utils.data.DataLoader(
        dataset,
        batch_size=batch_size,
        shuffle=shuffle,
        # SSDLite contains a 1x1 BatchNorm activation and therefore cannot train
        # on a final singleton batch. Keep all larger remainders.
        drop_last=bool(shuffle and len(dataset) % batch_size == 1),
        num_workers=0,  # Required for deterministic Windows spawn-free training.
        pin_memory=device.type == "cuda",
        persistent_workers=False,
        collate_fn=detection_collate,
        generator=generator,
    )


def _train_one_epoch(
    model: Any,
    loader: Iterable,
    optimizer: Any,
    scaler: Any,
    device: Any,
    torch: Any,
    amp_enabled: bool,
) -> Dict[str, float]:
    model.train()
    totals: Dict[str, float] = {}
    batches = 0
    for images, targets in loader:
        images = [image.to(device, non_blocking=True) for image in images]
        targets = [
            {key: value.to(device, non_blocking=True) for key, value in target.items()}
            for target in targets
        ]
        optimizer.zero_grad(set_to_none=True)
        with _autocast(torch, amp_enabled):
            loss_map = model(images, targets)
            loss = sum(loss_map.values())
        if not bool(torch.isfinite(loss).item()):
            raise RuntimeError(f"non-finite training loss: {float(loss.detach().cpu())}")
        scaler.scale(loss).backward()
        scaler.step(optimizer)
        scaler.update()
        batches += 1
        totals["loss"] = totals.get("loss", 0.0) + float(loss.detach().cpu())
        for name, value in loss_map.items():
            totals[name] = totals.get(name, 0.0) + float(value.detach().cpu())
    if batches == 0:
        raise RuntimeError("training data loader produced no batches")
    return {name: value / batches for name, value in totals.items()}


def _evaluate(
    model: Any,
    loader: Iterable,
    device: Any,
    torch: Any,
    *,
    score_threshold: float,
    iou_threshold: float,
    max_detections: int,
) -> Dict[str, float | int]:
    model.eval()
    ground_truths: Dict[str, list[list[float]]] = {}
    predictions: Dict[str, list[tuple[float, list[float]]]] = {}
    sequence = 0
    with torch.inference_mode():
        for images, targets in loader:
            device_images = [image.to(device, non_blocking=True) for image in images]
            outputs = model(device_images)
            for target, output in zip(targets, outputs):
                image_id = str(sequence)
                sequence += 1
                ground_truths[image_id] = target["boxes"].tolist()
                labels = output["labels"].detach().cpu().tolist()
                scores = output["scores"].detach().cpu().tolist()
                boxes = output["boxes"].detach().cpu().tolist()
                predictions[image_id] = [
                    (float(score), box)
                    for label, score, box in zip(labels, scores, boxes)
                    if int(label) == TARGET_LABEL
                ]
    return calculate_detection_metrics(
        ground_truths,
        predictions,
        score_threshold=score_threshold,
        iou_threshold=iou_threshold,
        max_detections=max_detections,
    )


def checkpoint_contract(manifest_sha256: str) -> Dict[str, Any]:
    return {
        "model": MODEL_NAME,
        "image_size": IMAGE_SIZE,
        "letterbox_fill_rgb": [LETTERBOX_FILL] * 3,
        "torchvision_num_classes": MODEL_CLASS_COUNT,
        "torchvision_foreground_label": TARGET_LABEL,
        "exported_foreground_label": EXPORTED_LABEL,
        "dataset_manifest_sha256": manifest_sha256,
    }


def validate_checkpoint_contract(
    checkpoint: Mapping[str, Any], manifest_sha256: str
) -> None:
    if checkpoint.get("schema_version") != CHECKPOINT_SCHEMA_VERSION:
        raise ValueError("unsupported detector checkpoint schema")
    expected = checkpoint_contract(manifest_sha256)
    actual = checkpoint.get("contract")
    if actual != expected:
        raise ValueError("checkpoint contract or dataset manifest SHA does not match")


def _capture_rng_state(stack: Mapping[str, Any]) -> Dict[str, Any]:
    torch = stack["torch"]
    numpy_state = stack["np"].random.get_state()
    state = {
        "python": random.getstate(),
        # NumPy normally stores its MT19937 keys in an ndarray. Encoding the
        # array as primitive integers keeps checkpoints compatible with
        # PyTorch's restricted weights-only unpickler.
        "numpy": {
            "bit_generator": str(numpy_state[0]),
            "keys": [int(value) for value in numpy_state[1]],
            "position": int(numpy_state[2]),
            "has_gauss": int(numpy_state[3]),
            "cached_gaussian": float(numpy_state[4]),
        },
        "torch": torch.get_rng_state(),
    }
    if torch.cuda.is_available():
        state["cuda"] = torch.cuda.get_rng_state_all()
    return state


def _restore_rng_state(state: Mapping[str, Any], stack: Mapping[str, Any]) -> None:
    torch = stack["torch"]
    random.setstate(state["python"])
    numpy_state = state["numpy"]
    if not isinstance(numpy_state, Mapping):
        raise ValueError("checkpoint NumPy RNG state must use the safe mapping format")
    required_numpy_keys = {
        "bit_generator",
        "keys",
        "position",
        "has_gauss",
        "cached_gaussian",
    }
    if set(numpy_state) != required_numpy_keys:
        raise ValueError("checkpoint NumPy RNG state is malformed")
    stack["np"].random.set_state(
        (
            str(numpy_state["bit_generator"]),
            stack["np"].asarray(numpy_state["keys"], dtype=stack["np"].uint32),
            int(numpy_state["position"]),
            int(numpy_state["has_gauss"]),
            float(numpy_state["cached_gaussian"]),
        )
    )
    # A checkpoint loaded with map_location="cuda" also moves these serialized
    # RNG byte tensors. PyTorch's RNG restoration APIs require CPU ByteTensors.
    torch.set_rng_state(state["torch"].detach().cpu())
    if "cuda" in state and torch.cuda.is_available():
        torch.cuda.set_rng_state_all(
            [rng_state.detach().cpu() for rng_state in state["cuda"]]
        )


def _atomic_torch_save(payload: Mapping[str, Any], path: Path, torch: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".part")
    torch.save(dict(payload), temporary)
    os.replace(temporary, path)


def _atomic_json_save(payload: Mapping[str, Any], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".part")
    temporary.write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    os.replace(temporary, path)


def _safe_torch_load(path: Path, torch: Any, map_location: Any) -> Any:
    """Load tensor/primitive checkpoints without executing pickle globals."""

    try:
        return torch.load(path, map_location=map_location, weights_only=True)
    except TypeError as error:
        # Falling back to the legacy unrestricted loader would execute pickle
        # globals before this module can validate the checkpoint contract.
        raise RuntimeError(
            "safe checkpoint loading requires PyTorch with weights_only support"
        ) from error


def _load_checkpoint(
    path: Path,
    model: Any,
    optimizer: Any,
    scheduler: Any,
    scaler: Any,
    manifest_sha256: str,
    stack: Mapping[str, Any],
    device: Any,
) -> tuple[int, float, list[Mapping[str, Any]]]:
    torch = stack["torch"]
    checkpoint = _safe_torch_load(path, torch, device)
    if not isinstance(checkpoint, Mapping):
        raise ValueError("detector checkpoint root must be a mapping")
    validate_checkpoint_contract(checkpoint, manifest_sha256)
    model.load_state_dict(checkpoint["model_state"])
    optimizer.load_state_dict(checkpoint["optimizer_state"])
    scheduler.load_state_dict(checkpoint["scheduler_state"])
    if checkpoint.get("scaler_state"):
        scaler.load_state_dict(checkpoint["scaler_state"])
    if checkpoint.get("rng_state"):
        _restore_rng_state(checkpoint["rng_state"], stack)
    return (
        int(checkpoint["epoch"]) + 1,
        float(checkpoint.get("best_ap50", 0.0)),
        list(checkpoint.get("history", [])),
    )


def _export_onnx(
    model: Any,
    sample_image: Any,
    path: Path,
    manifest: DatasetManifest,
    stack: Mapping[str, Any],
) -> Dict[str, Any]:
    np = stack["np"]
    onnx = stack["onnx"]
    ort = stack["ort"]
    torch = stack["torch"]

    class ExportWrapper(torch.nn.Module):
        def __init__(self, detector: Any):
            super().__init__()
            self.detector = detector

        def forward(self, images: Any) -> tuple[Any, Any, Any]:
            output = self.detector([images[0]])[0]
            return (
                output["boxes"],
                output["scores"],
                output["labels"].to(torch.int64),
            )

    model = model.to("cpu").eval()
    wrapper = ExportWrapper(model).eval()
    sample = sample_image.detach().to("cpu").unsqueeze(0)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".part")
    with torch.inference_mode():
        expected = tuple(value.detach().cpu().numpy() for value in wrapper(sample))
        torch.onnx.export(
            wrapper,
            sample,
            temporary,
            input_names=[ONNX_INPUT_NAME],
            output_names=list(ONNX_OUTPUT_NAMES),
            opset_version=18,
            do_constant_folding=True,
            dynamic_axes={name: {0: "num_detections"} for name in ONNX_OUTPUT_NAMES},
            dynamo=False,
        )
    checked_model = onnx.load(temporary)
    onnx.checker.check_model(checked_model)
    session = ort.InferenceSession(
        str(temporary), providers=["CPUExecutionProvider"]
    )
    observed = session.run(list(ONNX_OUTPUT_NAMES), {ONNX_INPUT_NAME: sample.numpy()})
    for name, torch_value, ort_value in zip(ONNX_OUTPUT_NAMES, expected, observed):
        if torch_value.shape != ort_value.shape:
            raise RuntimeError(
                f"ONNX parity failed for {name}: {torch_value.shape} != {ort_value.shape}"
            )
        if name == "labels":
            if not np.array_equal(torch_value, ort_value):
                raise RuntimeError("ONNX parity failed for labels")
        elif not np.allclose(torch_value, ort_value, rtol=1e-3, atol=1e-3):
            difference = float(np.max(np.abs(torch_value - ort_value)))
            raise RuntimeError(f"ONNX parity failed for {name}: max diff {difference}")
    os.replace(temporary, path)

    parity = {}
    for name, torch_value, ort_value in zip(ONNX_OUTPUT_NAMES, expected, observed):
        if name == "labels" or torch_value.size == 0:
            parity[f"{name}_max_abs_difference"] = 0.0
        else:
            parity[f"{name}_max_abs_difference"] = float(
                np.max(np.abs(torch_value - ort_value))
            )
    metadata = {
        "schema_version": 1,
        "artifact": path.name,
        "artifact_sha256": sha256_file(path),
        "dataset_manifest": public_metadata_path(manifest.path),
        "dataset_manifest_sha256": manifest.sha256,
        "model": MODEL_NAME,
        "input": {
            "name": ONNX_INPUT_NAME,
            "shape": [1, 3, IMAGE_SIZE, IMAGE_SIZE],
            "dtype": "float32",
            "color": "RGB",
            "range": [0.0, 1.0],
            "resize": "aspect-preserving letterbox",
            "letterbox_fill_rgb": [LETTERBOX_FILL] * 3,
        },
        "outputs": {
            "boxes": {"shape": ["N", 4], "format": "xyxy_letterboxed_pixels"},
            "scores": {"shape": ["N"], "range": [0.0, 1.0]},
            "labels": {"shape": ["N"], "traffic_sign": EXPORTED_LABEL},
        },
        "validation": {"onnx_checker": "pass", "onnxruntime_parity": parity},
        "versions": {
            "torch": torch.__version__,
            "torchvision": stack["torchvision"].__version__,
            "onnx": onnx.__version__,
            "onnxruntime": ort.__version__,
        },
    }
    _atomic_json_save(metadata, path.with_suffix(path.suffix + ".json"))
    return metadata


def run_training(config: TrainingConfig) -> Dict[str, Any]:
    _validate_config(config)
    manifest = load_dataset_manifest(config.manifest)
    stack = _import_training_stack()
    torch = stack["torch"]
    _set_reproducible_seed(config.seed, stack)
    device = _select_device(config.device, torch)
    amp_enabled = bool(config.amp and device.type == "cuda")

    train_dataset = OpenImagesDetectionDataset(
        manifest.records["train"], augment=True
    )
    validation_dataset = OpenImagesDetectionDataset(
        manifest.records["validation"], augment=False
    )
    if len(train_dataset) < 2:
        raise ValueError("SSDLite training requires at least two training images")
    validation_loader = _create_loader(
        validation_dataset,
        batch_size=config.batch_size,
        shuffle=False,
        seed=config.seed,
        device=device,
        torch=torch,
    )
    model = _build_model(stack, config.pretrained_backbone).to(device)
    optimizer = torch.optim.SGD(
        [parameter for parameter in model.parameters() if parameter.requires_grad],
        lr=config.learning_rate,
        momentum=config.momentum,
        weight_decay=config.weight_decay,
    )
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(
        optimizer, T_max=max(1, config.epochs)
    )
    scaler = _make_grad_scaler(torch, amp_enabled)

    start_epoch = 0
    best_ap50 = -1.0
    history: list[Mapping[str, Any]] = []
    if config.resume is not None:
        start_epoch, best_ap50, history = _load_checkpoint(
            config.resume,
            model,
            optimizer,
            scheduler,
            scaler,
            manifest.sha256,
            stack,
            device,
        )
    if start_epoch >= config.epochs:
        raise ValueError(
            f"checkpoint already reached epoch {start_epoch}; --epochs must be larger"
        )

    config.output_dir.mkdir(parents=True, exist_ok=True)
    manifest_snapshot = config.output_dir / "dataset-manifest.json"
    manifest_payload = manifest.path.read_bytes()
    if sha256_bytes(manifest_payload) != manifest.sha256:
        raise RuntimeError("dataset manifest changed before training started")
    snapshot_temporary = manifest_snapshot.with_suffix(".json.part")
    snapshot_temporary.write_bytes(manifest_payload)
    os.replace(snapshot_temporary, manifest_snapshot)
    started_at = time.time()
    best_path = config.output_dir / "best.pt"
    last_path = config.output_dir / "last.pt"
    for epoch in range(start_epoch, config.epochs):
        # Per-epoch generator makes resumed sample order identical to an uninterrupted run.
        train_loader = _create_loader(
            train_dataset,
            batch_size=config.batch_size,
            shuffle=True,
            seed=config.seed + epoch,
            device=device,
            torch=torch,
        )
        training_metrics = _train_one_epoch(
            model,
            train_loader,
            optimizer,
            scaler,
            device,
            torch,
            amp_enabled,
        )
        validation_metrics = _evaluate(
            model,
            validation_loader,
            device,
            torch,
            score_threshold=config.score_threshold,
            iou_threshold=config.iou_threshold,
            max_detections=config.max_detections,
        )
        epoch_record = {
            "epoch": epoch,
            "learning_rate": float(optimizer.param_groups[0]["lr"]),
            "train": training_metrics,
            "validation": validation_metrics,
        }
        history.append(epoch_record)
        scheduler.step()
        improved = float(validation_metrics["ap50"]) > best_ap50
        if improved:
            best_ap50 = float(validation_metrics["ap50"])
        checkpoint = {
            "schema_version": CHECKPOINT_SCHEMA_VERSION,
            "epoch": epoch,
            "best_ap50": best_ap50,
            "contract": checkpoint_contract(manifest.sha256),
            "config": {
                **asdict(config),
                "manifest": public_metadata_path(config.manifest),
                "output_dir": public_metadata_path(config.output_dir),
                "resume": public_metadata_path(config.resume),
                "onnx_path": public_metadata_path(config.onnx_path),
            },
            "history": history,
            "model_state": model.state_dict(),
            "optimizer_state": optimizer.state_dict(),
            "scheduler_state": scheduler.state_dict(),
            "scaler_state": scaler.state_dict(),
            "rng_state": _capture_rng_state(stack),
            "versions": {
                "python": platform.python_version(),
                # PyTorch exposes ``torch.__version__`` as a TorchVersion
                # instance in some releases.  That custom class is deliberately
                # rejected by ``weights_only=True``, so persist plain strings in
                # the otherwise tensor/primitive-only checkpoint.
                "torch": str(torch.__version__),
                "torchvision": str(stack["torchvision"].__version__),
            },
        }
        _atomic_torch_save(checkpoint, last_path, torch)
        if improved:
            _atomic_torch_save(checkpoint, best_path, torch)
        print(json.dumps(epoch_record, ensure_ascii=False), flush=True)

    # Refuse to associate an artifact with a manifest changed during training.
    if sha256_file(manifest.path) != manifest.sha256:
        raise RuntimeError("dataset manifest changed while detector was training")

    export_metadata = None
    if config.export_onnx:
        export_source = best_path if best_path.is_file() else last_path
        best_checkpoint = _safe_torch_load(export_source, torch, "cpu")
        validate_checkpoint_contract(best_checkpoint, manifest.sha256)
        model.load_state_dict(best_checkpoint["model_state"])
        sample_image, _ = validation_dataset[0]
        onnx_path = config.onnx_path or config.output_dir / "traffic_sign_detector.onnx"
        export_metadata = _export_onnx(
            model, sample_image, onnx_path, manifest, stack
        )

    summary = {
        "schema_version": RUN_SCHEMA_VERSION,
        "model": MODEL_NAME,
        "device": str(device),
        "amp_requested": config.amp,
        "amp_enabled": amp_enabled,
        "num_workers": 0,
        "seed": config.seed,
        "dataset_manifest": public_metadata_path(manifest.path),
        "dataset_manifest_snapshot": public_metadata_path(manifest_snapshot),
        "dataset_manifest_sha256": manifest.sha256,
        "train_images": len(train_dataset),
        "validation_images": len(validation_dataset),
        "best_ap50": best_ap50,
        "history": history,
        "elapsed_seconds": time.time() - started_at,
        "checkpoints": {
            "last": public_metadata_path(last_path),
            "best": public_metadata_path(best_path),
        },
        "onnx": export_metadata,
    }
    _atomic_json_save(summary, config.output_dir / "run.json")
    return summary


def _validate_config(config: TrainingConfig) -> None:
    if config.epochs < 1:
        raise ValueError("epochs must be positive")
    if config.batch_size < 2:
        raise ValueError("SSDLite batch size must be at least 2 for BatchNorm")
    if config.learning_rate <= 0.0:
        raise ValueError("learning rate must be positive")
    if not 0.0 <= config.momentum < 1.0:
        raise ValueError("momentum must be in [0, 1)")
    if config.weight_decay < 0.0:
        raise ValueError("weight decay cannot be negative")
    if config.seed < 0:
        raise ValueError("seed cannot be negative")
    if config.device not in {"auto", "cpu", "cuda"}:
        raise ValueError("device must be auto, cpu, or cuda")
    if not 0.0 <= config.score_threshold <= 1.0:
        raise ValueError("score threshold must be in [0, 1]")
    if not 0.0 < config.iou_threshold <= 1.0:
        raise ValueError("IoU threshold must be in (0, 1]")
    if config.max_detections < 1:
        raise ValueError("max detections must be positive")
    if config.resume is not None and not config.resume.is_file():
        raise ValueError(f"resume checkpoint does not exist: {config.resume}")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Train a reproducible one-class torchvision SSDLite320 traffic-sign "
            "detector and export its checked ONNX artifact"
        )
    )
    parser.add_argument(
        "--manifest",
        type=Path,
        default=Path("data/detector/open-images-v7/manifest.json"),
    )
    parser.add_argument("--output-dir", type=Path, default=Path("runs/detector"))
    parser.add_argument("--epochs", type=int, default=30)
    parser.add_argument("--batch-size", type=int, default=8)
    parser.add_argument("--learning-rate", type=float, default=0.005)
    parser.add_argument("--momentum", type=float, default=0.9)
    parser.add_argument("--weight-decay", type=float, default=0.0005)
    parser.add_argument("--seed", type=int, default=20260803)
    parser.add_argument("--device", choices=("auto", "cpu", "cuda"), default="auto")
    parser.add_argument(
        "--amp",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="use CUDA automatic mixed precision (automatically disabled on CPU)",
    )
    parser.add_argument(
        "--pretrained-backbone",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="initialize MobileNetV3 from torchvision ImageNet weights",
    )
    parser.add_argument("--score-threshold", type=float, default=0.5)
    parser.add_argument("--iou-threshold", type=float, default=0.5)
    parser.add_argument("--max-detections", type=int, default=100)
    parser.add_argument("--resume", type=Path)
    parser.add_argument(
        "--export-onnx", action=argparse.BooleanOptionalAction, default=True
    )
    parser.add_argument("--onnx-path", type=Path)
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    config = TrainingConfig(
        manifest=args.manifest,
        output_dir=args.output_dir,
        epochs=args.epochs,
        batch_size=args.batch_size,
        learning_rate=args.learning_rate,
        momentum=args.momentum,
        weight_decay=args.weight_decay,
        seed=args.seed,
        device=args.device,
        amp=args.amp,
        pretrained_backbone=args.pretrained_backbone,
        score_threshold=args.score_threshold,
        iou_threshold=args.iou_threshold,
        max_detections=args.max_detections,
        resume=args.resume,
        export_onnx=args.export_onnx,
        onnx_path=args.onnx_path,
    )
    try:
        summary = run_training(config)
    except (RuntimeError, ValueError) as error:
        print(f"detector training failed: {error}", file=sys.stderr)
        return 2
    print(json.dumps(summary, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
