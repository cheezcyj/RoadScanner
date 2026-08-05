from __future__ import annotations

from pathlib import Path, PurePosixPath, PureWindowsPath


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]


def public_metadata_path(
    path: str | Path | None,
    *,
    repository_root: Path = REPOSITORY_ROOT,
) -> str | None:
    """Return a portable path for metadata that may be published."""
    if path is None:
        return None

    raw = str(path)
    candidate = Path(path)
    windows_path = PureWindowsPath(raw)
    if windows_path.is_absolute() and not candidate.is_absolute():
        return windows_path.name or "external"

    try:
        relative = candidate.resolve().relative_to(repository_root.resolve())
    except (OSError, RuntimeError, ValueError):
        return windows_path.name or candidate.name or "external"

    portable = relative.as_posix()
    if (
        ".." in relative.parts
        or PureWindowsPath(portable).is_absolute()
        or PurePosixPath(portable).is_absolute()
    ):
        return windows_path.name or candidate.name or "external"
    return portable
