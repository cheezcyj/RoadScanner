import re
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PRIVATE_PATH_PATTERNS = (
    re.compile(r"(?i)(?<![A-Za-z0-9])[A-Z]:[\\/]+"),
    re.compile(r"/(?:home|Users)/[^/\s]+/"),
    re.compile(r"(?i)file:(?:/{2,3}|\\\\)"),
)


def _publishable_paths() -> list[Path]:
    result = subprocess.run(
        ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
        cwd=ROOT,
        check=True,
        capture_output=True,
    )
    return [ROOT / path.decode("utf-8") for path in result.stdout.split(b"\0") if path]


def _text(path: Path) -> str | None:
    payload = path.read_bytes()
    if payload.startswith((b"\xff\xfe", b"\xfe\xff")):
        return payload.decode("utf-16")
    if b"\0" in payload:
        return None
    try:
        return payload.decode("utf-8")
    except UnicodeDecodeError:
        return None


def test_publishable_files_do_not_contain_absolute_local_paths():
    findings: list[str] = []
    for path in _publishable_paths():
        text = _text(path)
        if text is None:
            continue
        for line_number, line in enumerate(text.splitlines(), start=1):
            if any(pattern.search(line) for pattern in PRIVATE_PATH_PATTERNS):
                findings.append(f"{path.relative_to(ROOT).as_posix()}:{line_number}")

    assert not findings, "absolute local paths found in tracked files: " + ", ".join(
        findings
    )
