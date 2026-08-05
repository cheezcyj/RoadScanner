from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
import re
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, Iterator, List, Mapping, Sequence

import requests
from PIL import Image


OPEN_IMAGES_VERSION = "v7"
TRAFFIC_SIGN_MID = "/m/01mqdt"
TRAFFIC_SIGN_NAME = "Traffic sign"
STOP_SIGN_MID = "/m/02pv19"
SOURCE_CLASSES = {
    TRAFFIC_SIGN_MID: TRAFFIC_SIGN_NAME,
    STOP_SIGN_MID: "Stop sign",
}
ANNOTATION_LICENSE = "https://creativecommons.org/licenses/by/4.0/"
SOURCE_PAGE = "https://storage.googleapis.com/openimages/web/download_v7.html"
CLASS_DESCRIPTIONS_URL = (
    "https://storage.googleapis.com/openimages/v7/"
    "oidv7-class-descriptions-boxable.csv"
)
CLASS_DESCRIPTIONS_BYTES = 12_064
SPLIT_SOURCES = {
    "train": {
        "annotations_url": (
            "https://storage.googleapis.com/openimages/v6/"
            "oidv6-train-annotations-bbox.csv"
        ),
        "annotations_bytes": 2_258_447_590,
        "metadata_url": (
            "https://storage.googleapis.com/openimages/2018_04/train/"
            "train-images-boxable-with-rotation.csv"
        ),
        "metadata_bytes": 638_407_721,
    },
    "validation": {
        "annotations_url": (
            "https://storage.googleapis.com/openimages/v5/"
            "validation-annotations-bbox.csv"
        ),
        "annotations_bytes": 25_105_048,
        "metadata_url": (
            "https://storage.googleapis.com/openimages/2018_04/validation/"
            "validation-images-with-rotation.csv"
        ),
        "metadata_bytes": 15_245_485,
    },
    "test": {
        "annotations_url": (
            "https://storage.googleapis.com/openimages/v5/"
            "test-annotations-bbox.csv"
        ),
        "annotations_bytes": 77_484_237,
        "metadata_url": (
            "https://storage.googleapis.com/openimages/2018_04/test/"
            "test-images-with-rotation.csv"
        ),
        "metadata_bytes": 45_227_339,
    },
}
HUMAN_IMAGE_LABEL_SOURCES = {
    "train": {
        "url": (
            "https://storage.googleapis.com/openimages/v5/"
            "train-annotations-human-imagelabels-boxable.csv"
        ),
        "bytes": 376_764_810,
    },
    "validation": {
        "url": (
            "https://storage.googleapis.com/openimages/v5/"
            "validation-annotations-human-imagelabels-boxable.csv"
        ),
        "bytes": 10_649_275,
    },
    "test": {
        "url": (
            "https://storage.googleapis.com/openimages/v5/"
            "test-annotations-human-imagelabels-boxable.csv"
        ),
        "bytes": 32_055_857,
    },
}
IMAGE_ID_PATTERN = re.compile(r"^[0-9a-f]{16}$")
BBOX_FIELDS = (
    "ImageID",
    "Source",
    "LabelName",
    "Confidence",
    "XMin",
    "XMax",
    "YMin",
    "YMax",
    "IsOccluded",
    "IsTruncated",
    "IsGroupOf",
    "IsDepiction",
    "IsInside",
)
METADATA_FIELDS = (
    "ImageID",
    "Subset",
    "OriginalURL",
    "OriginalLandingURL",
    "License",
    "AuthorProfileURL",
    "Author",
    "Title",
    "OriginalSize",
    "OriginalMD5",
    "Thumbnail300KURL",
    "Rotation",
)
HUMAN_IMAGE_LABEL_FIELDS = (
    "ImageID",
    "Source",
    "LabelName",
    "Confidence",
)


@dataclass(frozen=True)
class BoundingBox:
    xmin: float
    xmax: float
    ymin: float
    ymax: float
    occluded: bool
    truncated: bool
    source_mid: str

    @property
    def width(self) -> float:
        return self.xmax - self.xmin

    @property
    def height(self) -> float:
        return self.ymax - self.ymin

    def to_yolo(self) -> str:
        center_x = (self.xmin + self.xmax) / 2.0
        center_y = (self.ymin + self.ymax) / 2.0
        return (
            f"0 {center_x:.8f} {center_y:.8f} "
            f"{self.width:.8f} {self.height:.8f}"
        )


@dataclass(frozen=True)
class SourceObservation:
    url: str
    expected_bytes: int
    observed_bytes: int
    etag: str | None


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def _validated_float(row: Mapping[str, str], key: str) -> float:
    value = float(row[key])
    if not 0.0 <= value <= 1.0:
        raise ValueError(f"{key} must be normalized to [0, 1], got {value}")
    return value


def parse_traffic_sign_box(row: Mapping[str, str]) -> BoundingBox | None:
    """Convert one Open Images row, excluding unsuitable group/depiction boxes."""

    source_mid = row.get("LabelName", "")
    if source_mid not in SOURCE_CLASSES:
        return None
    if any(row.get(flag) == "1" for flag in ("IsGroupOf", "IsDepiction", "IsInside")):
        return None
    xmin = _validated_float(row, "XMin")
    xmax = _validated_float(row, "XMax")
    ymin = _validated_float(row, "YMin")
    ymax = _validated_float(row, "YMax")
    if xmin >= xmax or ymin >= ymax:
        raise ValueError(f"Invalid bounding box for {row.get('ImageID', 'unknown')}")
    return BoundingBox(
        xmin=xmin,
        xmax=xmax,
        ymin=ymin,
        ymax=ymax,
        occluded=row.get("IsOccluded") == "1",
        truncated=row.get("IsTruncated") == "1",
        source_mid=source_mid,
    )


def stable_image_sample(
    image_ids: Iterable[str], limit: int | None, seed: str
) -> List[str]:
    unique_ids = sorted(set(image_ids))
    if limit is None or limit == 0 or limit >= len(unique_ids):
        return unique_ids
    if limit < 0:
        raise ValueError("image limit cannot be negative")
    return sorted(
        unique_ids,
        key=lambda image_id: hashlib.sha256(
            f"{seed}:{image_id}".encode("ascii")
        ).digest(),
    )[:limit]


def _open_csv_response(
    session: requests.Session, url: str, expected_bytes: int
) -> tuple[requests.Response, Iterator[str], SourceObservation]:
    response = session.get(url, stream=True, timeout=(10, 180))
    response.raise_for_status()
    observed_bytes = int(response.headers.get("Content-Length", "0"))
    if observed_bytes != expected_bytes:
        response.close()
        raise RuntimeError(
            f"Source size changed for {url}: {observed_bytes} != {expected_bytes}"
        )
    response.encoding = "utf-8-sig"
    lines = (
        line
        for line in response.iter_lines(chunk_size=1024 * 1024, decode_unicode=True)
        if line
    )
    observation = SourceObservation(
        url=url,
        expected_bytes=expected_bytes,
        observed_bytes=observed_bytes,
        etag=response.headers.get("ETag"),
    )
    return response, lines, observation


def collect_split_annotations(
    session: requests.Session,
    split: str,
) -> tuple[Dict[str, List[BoundingBox]], SourceObservation, int]:
    source = SPLIT_SOURCES[split]
    response, lines, observation = _open_csv_response(
        session,
        str(source["annotations_url"]),
        int(source["annotations_bytes"]),
    )
    boxes_by_image: Dict[str, List[BoundingBox]] = defaultdict(list)
    matched_rows = 0
    try:
        reader = csv.DictReader(lines)
        missing = set(BBOX_FIELDS) - set(reader.fieldnames or ())
        if missing:
            raise RuntimeError(f"Open Images bbox schema changed: {sorted(missing)}")
        for row in reader:
            if row["LabelName"] not in SOURCE_CLASSES:
                continue
            matched_rows += 1
            image_id = row["ImageID"].lower()
            if not IMAGE_ID_PATTERN.fullmatch(image_id):
                raise RuntimeError(f"Invalid Open Images image ID: {image_id}")
            box = parse_traffic_sign_box(row)
            if box is not None:
                boxes_by_image[image_id].append(box)
    finally:
        response.close()
    return dict(boxes_by_image), observation, matched_rows


def collect_split_negative_images(
    session: requests.Session,
    split: str,
    excluded_image_ids: Iterable[str],
) -> tuple[List[str], SourceObservation, int, int]:
    """Collect human-verified Traffic sign absences outside the positive set.

    Open Images human image-level labels use ``Confidence=0`` for a verified
    absence. Every image that has an eligible traffic/stop-sign bbox is excluded
    before sampling, even if that positive image is not selected for this subset.
    """

    source = HUMAN_IMAGE_LABEL_SOURCES[split]
    response, lines, observation = _open_csv_response(
        session,
        str(source["url"]),
        int(source["bytes"]),
    )
    excluded = set(excluded_image_ids)
    negative_ids: set[str] = set()
    matched_rows = 0
    overlap_ids: set[str] = set()
    try:
        reader = csv.DictReader(lines)
        missing = set(HUMAN_IMAGE_LABEL_FIELDS) - set(reader.fieldnames or ())
        if missing:
            raise RuntimeError(
                f"Open Images human image-label schema changed: {sorted(missing)}"
            )
        for row in reader:
            if row["LabelName"] != TRAFFIC_SIGN_MID:
                continue
            try:
                confidence = float(row["Confidence"])
            except (TypeError, ValueError) as error:
                raise RuntimeError(
                    "Invalid Traffic sign confidence in Open Images human labels"
                ) from error
            if confidence != 0.0:
                continue
            matched_rows += 1
            image_id = row["ImageID"].lower()
            if not IMAGE_ID_PATTERN.fullmatch(image_id):
                raise RuntimeError(f"Invalid Open Images image ID: {image_id}")
            if image_id in excluded:
                overlap_ids.add(image_id)
                continue
            negative_ids.add(image_id)
    finally:
        response.close()
    return sorted(negative_ids), observation, matched_rows, len(overlap_ids)


def collect_image_metadata(
    session: requests.Session,
    split: str,
    selected_ids: Sequence[str],
) -> tuple[Dict[str, Dict[str, str]], SourceObservation]:
    source = SPLIT_SOURCES[split]
    response, lines, observation = _open_csv_response(
        session,
        str(source["metadata_url"]),
        int(source["metadata_bytes"]),
    )
    wanted = set(selected_ids)
    metadata: Dict[str, Dict[str, str]] = {}
    try:
        reader = csv.DictReader(lines)
        missing = set(METADATA_FIELDS) - set(reader.fieldnames or ())
        if missing:
            raise RuntimeError(f"Open Images metadata schema changed: {sorted(missing)}")
        for row in reader:
            image_id = row["ImageID"].lower()
            if image_id in wanted:
                metadata[image_id] = {field: row.get(field, "") for field in METADATA_FIELDS}
                if len(metadata) == len(wanted):
                    break
    finally:
        response.close()
    missing_ids = wanted - set(metadata)
    if missing_ids:
        preview = ", ".join(sorted(missing_ids)[:5])
        raise RuntimeError(f"Metadata missing for {len(missing_ids)} images: {preview}")
    return metadata, observation


def image_download_url(split: str, image_id: str) -> str:
    if split not in SPLIT_SOURCES:
        raise ValueError(f"Unsupported split: {split}")
    if not IMAGE_ID_PATTERN.fullmatch(image_id):
        raise ValueError(f"Invalid Open Images image ID: {image_id}")
    return f"https://open-images-dataset.s3.amazonaws.com/{split}/{image_id}.jpg"


def _download_image(
    split: str,
    image_id: str,
    destination: Path,
    max_image_bytes: int,
) -> Dict[str, object]:
    url = image_download_url(split, image_id)
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".part")
    if destination.is_file():
        with Image.open(destination) as image:
            width, height = image.size
            image.verify()
        return {
            "image_id": image_id,
            "url": url,
            "path": destination.as_posix(),
            "bytes": destination.stat().st_size,
            "sha256": sha256(destination),
            "width": width,
            "height": height,
            "status": "existing",
        }

    received = 0
    try:
        with requests.get(url, stream=True, timeout=(10, 120)) as response:
            response.raise_for_status()
            declared = int(response.headers.get("Content-Length", "0"))
            if declared <= 0 or declared > max_image_bytes:
                raise RuntimeError(f"Unsafe image size for {image_id}: {declared}")
            with temporary.open("wb") as target:
                for chunk in response.iter_content(1024 * 1024):
                    if not chunk:
                        continue
                    received += len(chunk)
                    if received > max_image_bytes:
                        raise RuntimeError(f"Image exceeded byte limit: {image_id}")
                    target.write(chunk)
        if received != declared:
            raise RuntimeError(f"Truncated image {image_id}: {received} != {declared}")
        with Image.open(temporary) as image:
            width, height = image.size
            image.verify()
        if width <= 0 or height <= 0 or width * height > 50_000_000:
            raise RuntimeError(f"Unsafe image dimensions for {image_id}: {width}x{height}")
        temporary.replace(destination)
        return {
            "image_id": image_id,
            "url": url,
            "path": destination.as_posix(),
            "bytes": received,
            "sha256": sha256(destination),
            "width": width,
            "height": height,
            "status": "downloaded",
        }
    finally:
        if temporary.exists():
            temporary.unlink()


def _write_split_files(
    output_dir: Path,
    split: str,
    boxes_by_image: Mapping[str, Sequence[BoundingBox]],
    metadata: Mapping[str, Mapping[str, str]],
    workers: int,
    max_image_bytes: int,
) -> List[Dict[str, object]]:
    image_dir = output_dir / "images" / split
    label_dir = output_dir / "labels" / split
    label_dir.mkdir(parents=True, exist_ok=True)
    futures = {}
    with ThreadPoolExecutor(max_workers=workers) as executor:
        for image_id in sorted(boxes_by_image):
            destination = image_dir / f"{image_id}.jpg"
            future = executor.submit(
                _download_image,
                split,
                image_id,
                destination,
                max_image_bytes,
            )
            futures[future] = image_id
        downloaded = {
            futures[future]: future.result() for future in as_completed(futures)
        }

    records: List[Dict[str, object]] = []
    for image_id in sorted(boxes_by_image):
        label_path = label_dir / f"{image_id}.txt"
        lines = [box.to_yolo() for box in boxes_by_image[image_id]]
        label_text = "\n".join(lines)
        if label_text:
            label_text += "\n"
        label_path.write_text(label_text, encoding="ascii")
        record = dict(downloaded[image_id])
        record.update(
            {
                "label_path": label_path.as_posix(),
                "label_sha256": sha256(label_path),
                "boxes": [asdict(box) for box in boxes_by_image[image_id]],
                "is_negative": not bool(boxes_by_image[image_id]),
                "source_metadata": dict(metadata[image_id]),
            }
        )
        records.append(record)
    return records


def _write_dataset_yaml(output_dir: Path) -> Path:
    dataset_yaml = output_dir / "dataset.yaml"
    root = output_dir.resolve().as_posix()
    dataset_yaml.write_text(
        "\n".join(
            (
                f"path: {json.dumps(root)}",
                "train: images/train",
                "val: images/validation",
                "test: images/test",
                "names:",
                "  0: traffic_sign",
                "",
            )
        ),
        encoding="utf-8",
    )
    return dataset_yaml


def download_open_images_subset(
    output_dir: Path,
    limits: Mapping[str, int | None],
    workers: int,
    max_image_bytes: int,
    seed: str,
    negative_limits: Mapping[str, int | None] | None = None,
) -> Dict[str, object]:
    if workers < 1 or workers > 16:
        raise ValueError("workers must be between 1 and 16")
    output_dir.mkdir(parents=True, exist_ok=True)
    session = requests.Session()
    session.headers["User-Agent"] = "RoadScanner detector dataset bootstrap/1.0"

    class_path = output_dir / "source" / "oidv7-class-descriptions-boxable.csv"
    class_path.parent.mkdir(parents=True, exist_ok=True)
    class_response = session.get(CLASS_DESCRIPTIONS_URL, timeout=(10, 30))
    class_response.raise_for_status()
    if len(class_response.content) != CLASS_DESCRIPTIONS_BYTES:
        raise RuntimeError("Open Images class description size changed")
    missing_classes = [
        f"{mid},{name}"
        for mid, name in SOURCE_CLASSES.items()
        if f"{mid},{name}" not in class_response.text
    ]
    if missing_classes:
        raise RuntimeError(
            f"Source classes are absent from the official class map: {missing_classes}"
        )
    class_path.write_bytes(class_response.content)

    split_manifests: Dict[str, object] = {}
    for split, limit in limits.items():
        all_boxes, annotation_source, matched_rows = collect_split_annotations(
            session, split
        )
        selected_positive_ids = stable_image_sample(
            # Preserve the original positive-subset selection when hard negatives
            # are added in a later run.
            all_boxes, limit, seed=f"{seed}:{split}"
        )
        selected_boxes: Dict[str, Sequence[BoundingBox]] = {
            image_id: all_boxes[image_id] for image_id in selected_positive_ids
        }

        negative_source: SourceObservation | None = None
        matched_negative_rows = 0
        positive_overlap_images = 0
        eligible_negative_ids: List[str] = []
        selected_negative_ids: List[str] = []
        if negative_limits is not None and split in negative_limits:
            (
                eligible_negative_ids,
                negative_source,
                matched_negative_rows,
                positive_overlap_images,
            ) = collect_split_negative_images(session, split, all_boxes)
            selected_negative_ids = stable_image_sample(
                eligible_negative_ids,
                negative_limits[split],
                seed=f"{seed}:{split}:negative",
            )
            selected_boxes.update(
                {image_id: () for image_id in selected_negative_ids}
            )

        selected_ids = sorted(selected_boxes)
        metadata, metadata_source = collect_image_metadata(session, split, selected_ids)
        image_records = _write_split_files(
            output_dir,
            split,
            selected_boxes,
            metadata,
            workers,
            max_image_bytes,
        )
        split_manifests[split] = {
            "annotation_source": asdict(annotation_source),
            "negative_annotation_source": (
                asdict(negative_source) if negative_source is not None else None
            ),
            "metadata_source": asdict(metadata_source),
            "traffic_sign_rows_before_exclusions": matched_rows,
            "traffic_sign_negative_rows": matched_negative_rows,
            "positive_overlap_images_excluded_from_negatives": (
                positive_overlap_images
            ),
            "eligible_images": len(all_boxes),
            "eligible_positive_images": len(all_boxes),
            "eligible_negative_images": len(eligible_negative_ids),
            "selected_positive_images": len(selected_positive_ids),
            "selected_negative_images": len(selected_negative_ids),
            "selected_images": len(selected_boxes),
            "selected_boxes": sum(len(boxes) for boxes in selected_boxes.values()),
            "images": image_records,
        }

    dataset_yaml = _write_dataset_yaml(output_dir)
    manifest: Dict[str, object] = {
        "schema_version": 1,
        "dataset": "Open Images",
        "dataset_version": OPEN_IMAGES_VERSION,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "source_page": SOURCE_PAGE,
        "annotation_license": ANNOTATION_LICENSE,
        "image_license_policy": (
            "Per-image license and attribution are preserved in source_metadata; "
            "verify them before redistributing pixels or derived artifacts."
        ),
        "class": {
            "id": 0,
            "name": "traffic_sign",
            "source_classes": [
                {"mid": mid, "name": name} for mid, name in SOURCE_CLASSES.items()
            ],
        },
        "class_descriptions": {
            "url": CLASS_DESCRIPTIONS_URL,
            "bytes": class_path.stat().st_size,
            "sha256": sha256(class_path),
            "path": class_path.as_posix(),
        },
        "selection": {
            "seed": seed,
            "excluded_flags": ["IsGroupOf", "IsDepiction", "IsInside"],
        },
        "splits": split_manifests,
        "dataset_yaml": {
            "path": dataset_yaml.as_posix(),
            "sha256": sha256(dataset_yaml),
        },
    }
    manifest_path = output_dir / "manifest.json"
    temporary = manifest_path.with_suffix(".json.part")
    temporary.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    os.replace(temporary, manifest_path)
    return manifest


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Download a reproducible Open Images traffic-sign detector subset"
    )
    parser.add_argument(
        "--output-dir", type=Path, default=Path("data/detector/open-images-v7")
    )
    parser.add_argument("--train-images", type=int, default=500)
    parser.add_argument(
        "--train-negative-images",
        type=int,
        default=250,
        help="0 downloads every eligible human-verified negative train image",
    )
    parser.add_argument(
        "--validation-images",
        type=int,
        default=0,
        help="0 downloads every eligible validation image",
    )
    parser.add_argument(
        "--test-images",
        type=int,
        help="omit to skip the test split; 0 downloads every eligible image",
    )
    parser.add_argument(
        "--validation-negative-images",
        type=int,
        default=0,
        help="0 downloads every eligible human-verified negative validation image",
    )
    parser.add_argument(
        "--test-negative-images",
        type=int,
        help="omit to skip test negatives; 0 downloads every eligible image",
    )
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--max-image-bytes", type=int, default=20 * 1024 * 1024)
    parser.add_argument("--seed", default="roadscanner-open-images-v7-traffic-sign-v1")
    args = parser.parse_args()

    limits: Dict[str, int | None] = {
        "train": args.train_images,
        "validation": args.validation_images,
    }
    if args.test_images is not None:
        limits["test"] = args.test_images
    negative_limits: Dict[str, int | None] = {
        "train": args.train_negative_images,
        "validation": args.validation_negative_images,
    }
    if args.test_images is not None and args.test_negative_images is not None:
        negative_limits["test"] = args.test_negative_images
    manifest = download_open_images_subset(
        output_dir=args.output_dir,
        limits=limits,
        workers=args.workers,
        max_image_bytes=args.max_image_bytes,
        seed=args.seed,
        negative_limits=negative_limits,
    )
    summary = {
        split: {
            "eligible_images": details["eligible_images"],
            "selected_images": details["selected_images"],
            "selected_positive_images": details["selected_positive_images"],
            "selected_negative_images": details["selected_negative_images"],
            "selected_boxes": details["selected_boxes"],
        }
        for split, details in manifest["splits"].items()
    }
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
