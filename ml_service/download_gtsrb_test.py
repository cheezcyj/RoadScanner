from __future__ import annotations

import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path

import requests


ARCHIVE_BASE = (
    "https://sid.erda.dk/public/archives/"
    "daaeac0d7ce1152aea9b61d9f1e19370"
)
FILES = {
    "GTSRB_Final_Test_Images.zip": (
        88_978_620,
        "48BA6FAB7E877EB64EAF8DE99035B0AAECFBC279BEE23E35DECA4AC1D0A837FA",
    ),
    "GTSRB_Final_Test_GT.zip": (
        99_620,
        "F94E5A7614D75845C74C04DDB26B8796B9E483F43541DD95DD5B726504E16D6D",
    ),
}
TRAINING_FILES = {
    "GTSRB_Final_Training_Images.zip": (
        276_294_756,
        "D32AC4B5FA9A1CBD1994768413902E8193599D9434CF0A8EB9CFD00A6D3A290C",
    ),
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def download(
    session: requests.Session,
    name: str,
    expected_size: int,
    expected_sha256: str,
    output_dir: Path,
) -> dict:
    destination = output_dir / name
    if destination.is_file() and destination.stat().st_size == expected_size:
        actual_sha256 = sha256(destination)
        if actual_sha256 == expected_sha256:
            return {
                "name": name,
                "url": f"{ARCHIVE_BASE}/{name}",
                "bytes": expected_size,
                "sha256": actual_sha256,
                "status": "existing",
            }

    temporary = destination.with_suffix(destination.suffix + ".part")
    url = f"{ARCHIVE_BASE}/{name}"
    with session.get(url, stream=True, timeout=(10, 120)) as response:
        response.raise_for_status()
        with temporary.open("wb") as target:
            for chunk in response.iter_content(1024 * 1024):
                if chunk:
                    target.write(chunk)
    if temporary.stat().st_size != expected_size:
        raise RuntimeError(
            f"Unexpected size for {name}: {temporary.stat().st_size} != {expected_size}"
        )
    actual_sha256 = sha256(temporary)
    if actual_sha256 != expected_sha256:
        raise RuntimeError(
            f"Unexpected SHA-256 for {name}: {actual_sha256} != {expected_sha256}"
        )
    temporary.replace(destination)
    return {
        "name": name,
        "url": url,
        "bytes": expected_size,
        "sha256": actual_sha256,
        "status": "downloaded",
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Download the official GTSRB archives")
    parser.add_argument("--output-dir", type=Path, default=Path("data/gtsrb"))
    parser.add_argument(
        "--include-training",
        action="store_true",
        help="also download the training archive used to calibrate the OOD reference",
    )
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)

    session = requests.Session()
    session.headers["User-Agent"] = "RoadScanner model recovery validation"
    files = dict(FILES)
    training_exists = any((args.output_dir / name).is_file() for name in TRAINING_FILES)
    if args.include_training or training_exists:
        files.update(TRAINING_FILES)
    records = [
        download(session, name, size, expected_hash, args.output_dir)
        for name, (size, expected_hash) in files.items()
    ]
    manifest = {
        "source": "Official GTSRB ERDA public archive",
        "archive_page": f"{ARCHIVE_BASE}/published-archive.html",
        "retrieved_at": datetime.now(timezone.utc).isoformat(),
        "files": records,
    }
    manifest_path = args.output_dir / "download-manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(json.dumps(manifest, indent=2))


if __name__ == "__main__":
    main()
