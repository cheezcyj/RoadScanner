from __future__ import annotations

import argparse
import io
import json
import os
import re
from pathlib import Path

import requests
from PIL import Image


def meta(html: str, name: str) -> str:
    match = re.search(
        rf'<meta\s+name=["\']{re.escape(name)}["\']\s+content=["\']([^"\']+)',
        html,
        flags=re.IGNORECASE,
    )
    if not match:
        raise RuntimeError(f"Missing {name} meta tag")
    return match.group(1)


def demo_sign_bytes(repository_root: Path) -> bytes:
    demo = Image.open(repository_root / "docs" / "demo" / "upload-recognition.gif")
    demo.seek(30)
    sign = demo.convert("RGB").crop((94, 153, 304, 363))
    output = io.BytesIO()
    sign.save(output, format="PNG")
    return output.getvalue()


def image_bytes(repository_root: Path, image_path: Path | None) -> bytes:
    if image_path is None:
        return demo_sign_bytes(repository_root)
    resolved = image_path if image_path.is_absolute() else repository_root / image_path
    with Image.open(resolved) as source:
        normalized = source.convert("RGB")
        output = io.BytesIO()
        normalized.save(output, format="PNG")
        return output.getvalue()


def expected_label(repository_root: Path, result_id: int) -> dict:
    payload = json.loads(
        (repository_root / "ml_service" / "class_map.json").read_text(
            encoding="utf-8"
        )
    )
    matches = [
        row for row in payload["classes"] if int(row["result_id"]) == result_id
    ]
    unknown = payload.get("unknown")
    if (
        isinstance(unknown, dict)
        and int(payload.get("unknown_result_id", -1)) == result_id
        and int(unknown.get("result_id", -1)) == result_id
    ):
        matches.append(unknown)
    if len(matches) != 1:
        raise ValueError(f"Unknown or duplicate expected result ID: {result_id}")
    return matches[0]


def success_message(upload_id: str, expected: dict) -> str:
    fields = ["E2E_OK", f"upload_id={upload_id}"]
    if "class_id" in expected:
        fields.append(f"class_id={expected['class_id']}")
    fields.extend(
        [f"result_id={expected['result_id']}", f"key={expected['key']}"]
    )
    return " ".join(fields)


def main() -> None:
    parser = argparse.ArgumentParser(description="Exercise login, upload, Java, and ML inference")
    parser.add_argument("--base-url", default="http://127.0.0.1:18082")
    parser.add_argument("--user", default="localuser")
    parser.add_argument(
        "--image",
        type=Path,
        help="whole-scene or cropped image; defaults to the historical demo crop",
    )
    parser.add_argument("--expected-result-id", type=int, default=34)
    args = parser.parse_args()
    password = os.environ.get("ROADSCANNER_LOCAL_PASSWORD")
    if not password:
        raise RuntimeError("ROADSCANNER_LOCAL_PASSWORD is required")

    root = Path(__file__).resolve().parent.parent
    expected = expected_label(root, args.expected_result_id)
    session = requests.Session()
    login_page = session.get(f"{args.base_url}/login", timeout=10)
    login_page.raise_for_status()
    login_page.encoding = "utf-8"
    token = meta(login_page.text, "csrf-token")
    header_name = meta(login_page.text, "csrf-header")
    headers = {header_name: token}

    login = session.post(
        f"{args.base_url}/login",
        data={"id": args.user, "password": password},
        headers=headers,
        timeout=10,
    )
    login.raise_for_status()
    if str(login.json().get("msgId")) != "30":
        raise RuntimeError(f"Local login failed: {login.text}")

    upload_page = session.get(f"{args.base_url}/main/preUpload", timeout=10)
    upload_page.raise_for_status()
    upload_page.encoding = "utf-8"
    token = meta(upload_page.text, "csrf-token")
    header_name = meta(upload_page.text, "csrf-header")
    upload = session.post(
        f"{args.base_url}/main/fileUploaded",
        files={"fileUpload": ("e2e-input.png", image_bytes(root, args.image), "image/png")},
        headers={header_name: token},
        timeout=20,
    )
    upload.raise_for_status()
    upload_id = upload.text.strip()
    if not upload_id.isdigit():
        raise RuntimeError(f"Unexpected upload response: {upload.text}")

    result = session.get(f"{args.base_url}/main/upload?idx={upload_id}", timeout=30)
    result.raise_for_status()
    result.encoding = "utf-8"
    if expected["name_ko"] not in result.text or expected["name_en"] not in result.text:
        raise RuntimeError(
            "End-to-end result did not contain the expected mapping: "
            f"result_id={expected['result_id']} key={expected['key']}"
        )
    print(success_message(upload_id, expected))


if __name__ == "__main__":
    main()
