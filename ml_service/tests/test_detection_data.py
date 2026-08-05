import shutil
import sys
import uuid
from pathlib import Path

import pytest

from ml_service import detection_data
from ml_service.detection_data import (
    BoundingBox,
    HUMAN_IMAGE_LABEL_SOURCES,
    STOP_SIGN_MID,
    SourceObservation,
    TRAFFIC_SIGN_MID,
    collect_split_negative_images,
    image_download_url,
    parse_traffic_sign_box,
    stable_image_sample,
)


ROOT = Path(__file__).resolve().parents[2]


@pytest.fixture
def local_tmp_path():
    path = ROOT / "data" / "test-detection-data" / uuid.uuid4().hex
    path.mkdir(parents=True)
    try:
        yield path
    finally:
        shutil.rmtree(path, ignore_errors=True)


def row(**overrides):
    values = {
        "ImageID": "0123456789abcdef",
        "LabelName": TRAFFIC_SIGN_MID,
        "XMin": "0.1",
        "XMax": "0.3",
        "YMin": "0.2",
        "YMax": "0.6",
        "IsOccluded": "0",
        "IsTruncated": "1",
        "IsGroupOf": "0",
        "IsDepiction": "0",
        "IsInside": "0",
    }
    values.update(overrides)
    return values


def test_open_images_box_converts_to_single_class_yolo_coordinates():
    box = parse_traffic_sign_box(row())

    assert box is not None
    assert box.to_yolo() == "0 0.20000000 0.40000000 0.20000000 0.40000000"
    assert box.occluded is False
    assert box.truncated is True
    assert box.source_mid == TRAFFIC_SIGN_MID


def test_stop_sign_child_class_is_folded_into_generic_detector_class():
    box = parse_traffic_sign_box(row(LabelName=STOP_SIGN_MID))

    assert box is not None
    assert box.source_mid == STOP_SIGN_MID
    assert box.to_yolo().startswith("0 ")


@pytest.mark.parametrize("flag", ["IsGroupOf", "IsDepiction", "IsInside"])
def test_unsuitable_open_images_annotations_are_excluded(flag):
    assert parse_traffic_sign_box(row(**{flag: "1"})) is None


def test_invalid_normalized_box_is_rejected():
    with pytest.raises(ValueError, match="Invalid bounding box"):
        parse_traffic_sign_box(row(XMin="0.8", XMax="0.3"))


def test_sampling_is_stable_and_independent_of_input_order():
    image_ids = [f"{number:016x}" for number in range(20)]

    first = stable_image_sample(image_ids, 5, "fixed-seed")
    second = stable_image_sample(reversed(image_ids), 5, "fixed-seed")

    assert first == second
    assert len(first) == 5


def test_image_url_rejects_path_injection():
    assert image_download_url("train", "0123456789abcdef").endswith(
        "/train/0123456789abcdef.jpg"
    )
    with pytest.raises(ValueError, match="Invalid Open Images image ID"):
        image_download_url("train", "../escape")


class CsvResponse:
    def __init__(self, text):
        self._text = text
        self.headers = {
            "Content-Length": str(len(text.encode("utf-8"))),
            "ETag": '"fixture-etag"',
        }
        self.encoding = None
        self.closed = False

    def raise_for_status(self):
        return None

    def iter_lines(self, **_kwargs):
        return iter(self._text.splitlines())

    def close(self):
        self.closed = True


class CsvSession:
    def __init__(self, response):
        self.response = response

    def get(self, url, **_kwargs):
        assert url == "https://example.test/human-labels.csv"
        return self.response


def test_human_verified_negatives_exclude_every_bbox_positive(monkeypatch):
    negative_id = "0000000000000001"
    overlap_id = "0000000000000002"
    positive_label_id = "0000000000000003"
    stop_label_id = "0000000000000004"
    csv_text = "\n".join(
        (
            "ImageID,Source,LabelName,Confidence",
            f"{negative_id},verification,{TRAFFIC_SIGN_MID},0",
            f"{overlap_id},verification,{TRAFFIC_SIGN_MID},0.0",
            f"{positive_label_id},verification,{TRAFFIC_SIGN_MID},1",
            f"{stop_label_id},verification,{STOP_SIGN_MID},0",
        )
    )
    response = CsvResponse(csv_text)
    monkeypatch.setitem(
        HUMAN_IMAGE_LABEL_SOURCES,
        "validation",
        {
            "url": "https://example.test/human-labels.csv",
            "bytes": len(csv_text.encode("utf-8")),
        },
    )

    image_ids, observation, matched_rows, overlap_count = (
        collect_split_negative_images(
            CsvSession(response), "validation", {overlap_id}
        )
    )

    assert image_ids == [negative_id]
    assert matched_rows == 2
    assert overlap_count == 1
    assert observation.observed_bytes == len(csv_text.encode("utf-8"))
    assert observation.etag == '"fixture-etag"'
    assert response.closed is True


def test_negative_example_writes_zero_byte_yolo_label_and_manifest_flag(
    monkeypatch, local_tmp_path
):
    tmp_path = local_tmp_path
    positive_id = "0000000000000010"
    negative_id = "0000000000000011"
    box = BoundingBox(0.1, 0.3, 0.2, 0.6, False, False, TRAFFIC_SIGN_MID)

    def fake_download(split, image_id, destination, max_image_bytes):
        assert split == "train"
        assert max_image_bytes == 1024
        return {
            "image_id": image_id,
            "url": f"https://example.test/{image_id}.jpg",
            "path": destination.as_posix(),
            "bytes": 10,
            "sha256": "A" * 64,
            "width": 100,
            "height": 100,
            "status": "downloaded",
        }

    monkeypatch.setattr(detection_data, "_download_image", fake_download)
    records = detection_data._write_split_files(
        tmp_path,
        "train",
        {positive_id: [box], negative_id: []},
        {positive_id: {}, negative_id: {}},
        workers=1,
        max_image_bytes=1024,
    )
    by_id = {record["image_id"]: record for record in records}

    negative_label = tmp_path / "labels" / "train" / f"{negative_id}.txt"
    positive_label = tmp_path / "labels" / "train" / f"{positive_id}.txt"
    assert negative_label.read_bytes() == b""
    assert positive_label.read_text(encoding="ascii").endswith("\n")
    assert by_id[negative_id]["is_negative"] is True
    assert by_id[negative_id]["boxes"] == []
    assert by_id[positive_id]["is_negative"] is False


def test_manifest_preserves_negative_source_and_selection_counts(
    monkeypatch, local_tmp_path
):
    tmp_path = local_tmp_path
    positive_id = "0000000000000020"
    negative_ids = ["0000000000000021", "0000000000000022"]
    class_text = f"{TRAFFIC_SIGN_MID},Traffic sign\n{STOP_SIGN_MID},Stop sign\n"

    class ClassResponse:
        content = class_text.encode("utf-8")
        text = class_text

        @staticmethod
        def raise_for_status():
            return None

    class Session:
        def __init__(self):
            self.headers = {}

        @staticmethod
        def get(_url, **_kwargs):
            return ClassResponse()

    bbox_source = SourceObservation("https://example.test/bbox", 20, 20, "bbox")
    negative_source = SourceObservation(
        "https://example.test/human", 30, 30, "human"
    )
    metadata_source = SourceObservation(
        "https://example.test/metadata", 40, 40, "metadata"
    )
    box = BoundingBox(0.1, 0.3, 0.2, 0.6, False, False, TRAFFIC_SIGN_MID)

    monkeypatch.setattr(detection_data.requests, "Session", Session)
    monkeypatch.setattr(
        detection_data, "CLASS_DESCRIPTIONS_BYTES", len(class_text.encode("utf-8"))
    )
    monkeypatch.setattr(
        detection_data,
        "collect_split_annotations",
        lambda _session, split: ({positive_id: [box]}, bbox_source, 1),
    )

    def fake_negatives(_session, split, excluded_image_ids):
        assert set(excluded_image_ids) == {positive_id}
        return negative_ids, negative_source, 3, 1

    monkeypatch.setattr(
        detection_data, "collect_split_negative_images", fake_negatives
    )

    def fake_metadata(_session, split, selected_ids):
        return ({image_id: {} for image_id in selected_ids}, metadata_source)

    monkeypatch.setattr(detection_data, "collect_image_metadata", fake_metadata)

    def fake_write(_output, _split, boxes, _metadata, _workers, _max_bytes):
        return [
            {
                "image_id": image_id,
                "boxes": [box.to_yolo() for box in image_boxes],
                "is_negative": not bool(image_boxes),
            }
            for image_id, image_boxes in sorted(boxes.items())
        ]

    monkeypatch.setattr(detection_data, "_write_split_files", fake_write)
    manifest = detection_data.download_open_images_subset(
        tmp_path,
        limits={"train": 1},
        workers=1,
        max_image_bytes=1024,
        seed="fixed",
        negative_limits={"train": 1},
    )
    split = manifest["splits"]["train"]

    assert split["negative_annotation_source"] == {
        "url": "https://example.test/human",
        "expected_bytes": 30,
        "observed_bytes": 30,
        "etag": "human",
    }
    assert split["traffic_sign_negative_rows"] == 3
    assert split["positive_overlap_images_excluded_from_negatives"] == 1
    assert split["eligible_negative_images"] == 2
    assert split["selected_positive_images"] == 1
    assert split["selected_negative_images"] == 1
    assert split["selected_images"] == 2
    assert sum(record["is_negative"] for record in split["images"]) == 1


def test_cli_enables_train_and_all_validation_negatives_by_default(
    monkeypatch, capsys
):
    captured = {}

    def fake_download(**kwargs):
        captured.update(kwargs)
        split = {
            "eligible_images": 1,
            "selected_images": 1,
            "selected_positive_images": 1,
            "selected_negative_images": 0,
            "selected_boxes": 1,
        }
        return {"splits": {"train": split, "validation": split}}

    monkeypatch.setattr(detection_data, "download_open_images_subset", fake_download)
    monkeypatch.setattr(sys, "argv", ["detection_data"])

    detection_data.main()

    assert captured["negative_limits"] == {"train": 250, "validation": 0}
    assert "selected_negative_images" in capsys.readouterr().out
