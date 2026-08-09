from pathlib import Path
import shutil
import uuid

from PIL import Image
import pytest

from ml_service.e2e_smoke import expected_label, image_bytes, success_message


ROOT = Path(__file__).resolve().parents[2]


def test_expected_result_id_is_resolved_from_the_shared_class_map():
    label = expected_label(ROOT, 15)

    assert label["class_id"] == 14
    assert label["key"] == "stop"
    assert label["name_en"] == "Stop"


def test_unknown_result_id_is_resolved_without_a_class_id():
    label = expected_label(ROOT, 44)

    assert "class_id" not in label
    assert label["result_id"] == 44
    assert label["key"] == "unknown"
    assert label["name_en"] == "Unknown / low-confidence result"
    assert success_message("123", label) == (
        "E2E_OK upload_id=123 result_id=44 key=unknown"
    )


@pytest.mark.parametrize("result_id", [0, 45])
def test_invalid_expected_result_id_is_rejected(result_id):
    with pytest.raises(
        ValueError, match=f"Unknown or duplicate expected result ID: {result_id}"
    ):
        expected_label(ROOT, result_id)


def test_explicit_e2e_image_is_normalized_to_png():
    directory = ROOT / "data" / "test-e2e-smoke" / uuid.uuid4().hex
    directory.mkdir(parents=True)
    try:
        source = directory / "scene.jpg"
        Image.new("RGB", (31, 19), color=(12, 34, 56)).save(source)

        payload = image_bytes(ROOT, source)

        assert payload.startswith(b"\x89PNG\r\n\x1a\n")
    finally:
        shutil.rmtree(directory, ignore_errors=True)
