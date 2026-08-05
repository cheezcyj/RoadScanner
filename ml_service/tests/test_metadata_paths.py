import json
from pathlib import Path, PurePosixPath, PureWindowsPath

from ml_service.metadata_paths import public_metadata_path


ROOT = Path(__file__).resolve().parents[2]


def test_public_metadata_path_preserves_repository_relative_path():
    path = ROOT / "data" / "detector" / "manifest.json"

    assert public_metadata_path(path) == "data/detector/manifest.json"


def test_public_metadata_path_reduces_external_path_to_filename():
    path = ROOT.parent / "private-training-data" / "manifest.json"

    assert public_metadata_path(path) == "manifest.json"


def test_public_metadata_path_handles_foreign_windows_path_without_leaking_it():
    path = chr(92).join(("X:", "private-training-data", "manifest.json"))

    result = public_metadata_path(path)

    assert result == "manifest.json"
    assert not PureWindowsPath(result).is_absolute()
    assert not PurePosixPath(result).is_absolute()
    assert ".." not in PurePosixPath(result).parts


def test_public_metadata_path_accepts_optional_paths():
    assert public_metadata_path(None) is None


def test_committed_detector_metadata_uses_portable_manifest_path():
    metadata = json.loads(
        (ROOT / "traffic_sign_detector.onnx.json").read_text(encoding="utf-8")
    )
    manifest = metadata["dataset_manifest"]

    assert manifest == "data/detector/open-images-v7/manifest.json"
    assert not Path(manifest).is_absolute()
    assert not PureWindowsPath(manifest).is_absolute()
    assert not PurePosixPath(manifest).is_absolute()
