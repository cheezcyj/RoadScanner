from __future__ import annotations

import ipaddress
from io import BytesIO
import operator
import time
from typing import Iterable
from urllib.parse import urlsplit

import requests
from PIL import Image, UnidentifiedImageError


class ImageFetchError(ValueError):
    pass


class ImageFetchUnavailable(ImageFetchError):
    """A transient network or upstream failure that callers may retry."""


class ImageFetcher:
    def __init__(
        self,
        allowed_hosts: Iterable[str],
        max_bytes: int = 5 * 1024 * 1024,
        max_pixels: int = 25_000_000,
        max_dimension: int = 10_000,
        timeout_seconds: float = 5.0,
        allowed_ports: Iterable[int | str] = (80, 443),
        session: requests.Session | None = None,
    ) -> None:
        self.allowed_hosts = {host.strip().lower() for host in allowed_hosts if host.strip()}
        if not self.allowed_hosts:
            raise ValueError("At least one image host must be explicitly allowed")
        self.allowed_ports = self._validated_ports(allowed_ports)
        if max_bytes <= 0 or timeout_seconds <= 0:
            raise ValueError("Image size and timeout limits must be positive")
        if isinstance(max_pixels, bool) or isinstance(max_dimension, bool):
            raise ValueError("Image pixel and dimension limits must be positive")
        try:
            max_pixels = operator.index(max_pixels)
            max_dimension = operator.index(max_dimension)
        except TypeError as error:
            raise ValueError(
                "Image pixel and dimension limits must be positive integers"
            ) from error
        if max_pixels <= 0 or max_dimension <= 0:
            raise ValueError("Image pixel and dimension limits must be positive")
        self.max_bytes = max_bytes
        self.max_pixels = max_pixels
        self.max_dimension = max_dimension
        self.timeout_seconds = timeout_seconds
        self.session = session or requests.Session()
        self.session.trust_env = False

    def fetch(self, image_url: str) -> Image.Image:
        parsed = urlsplit(image_url)
        if parsed.scheme not in {"http", "https"} or not parsed.hostname:
            raise ImageFetchError("image_url must be an absolute HTTP(S) URL")
        if parsed.username or parsed.password or parsed.fragment:
            raise ImageFetchError("image_url contains forbidden URL components")

        host = parsed.hostname.lower()
        if host not in self.allowed_hosts:
            raise ImageFetchError("image_url host is not allowed")
        self._reject_ambiguous_loopback(host)
        try:
            port = parsed.port
        except ValueError as error:
            raise ImageFetchError("image_url port is invalid") from error
        effective_port = port or (443 if parsed.scheme == "https" else 80)
        if effective_port not in self.allowed_ports:
            raise ImageFetchError("image_url port is not allowed")

        deadline = time.monotonic() + self.timeout_seconds
        try:
            response = self.session.get(
                image_url,
                stream=True,
                allow_redirects=False,
                timeout=(min(2.0, self.timeout_seconds), self.timeout_seconds),
                headers={"Accept": "image/png,image/jpeg,image/bmp"},
            )
        except requests.RequestException as error:
            raise ImageFetchUnavailable("image download failed") from error

        try:
            if response.status_code != 200:
                if response.status_code in {408, 425, 429} or 500 <= response.status_code < 600:
                    raise ImageFetchUnavailable("image server is temporarily unavailable")
                raise ImageFetchError("image server returned a non-success status")
            content_type = response.headers.get("Content-Type", "").split(";", 1)[0].lower()
            if not content_type.startswith("image/"):
                raise ImageFetchError("image server did not return an image")
            declared_size = response.headers.get("Content-Length")
            if declared_size and int(declared_size) > self.max_bytes:
                raise ImageFetchError("image exceeds the maximum size")

            content = bytearray()
            for chunk in response.iter_content(64 * 1024):
                if time.monotonic() > deadline:
                    raise ImageFetchUnavailable("image download timed out")
                if not chunk:
                    continue
                content.extend(chunk)
                if len(content) > self.max_bytes:
                    raise ImageFetchError("image exceeds the maximum size")
        finally:
            response.close()

        image = None
        try:
            image = Image.open(BytesIO(content))
            width, height = image.size
            if width <= 0 or height <= 0:
                raise ImageFetchError("image dimensions must be positive")
            if width > self.max_dimension or height > self.max_dimension:
                raise ImageFetchError("image dimensions exceed the maximum")
            if width * height > self.max_pixels:
                raise ImageFetchError("image pixel count exceeds the maximum")
            image.load()
            return image
        except ImageFetchError:
            if image is not None:
                image.close()
            raise
        except Image.DecompressionBombError as error:
            if image is not None:
                image.close()
            raise ImageFetchError("image exceeds safe decompression limits") from error
        except (UnidentifiedImageError, OSError) as error:
            if image is not None:
                image.close()
            raise ImageFetchError("downloaded content is not a supported image") from error

    @staticmethod
    def _validated_ports(values: Iterable[int | str]) -> set[int]:
        ports: set[int] = set()
        for value in values:
            if isinstance(value, bool):
                raise ValueError("Allowed image ports must be integers from 1 to 65535")
            try:
                port = int(value)
            except (TypeError, ValueError) as error:
                raise ValueError(
                    "Allowed image ports must be integers from 1 to 65535"
                ) from error
            if str(port) != str(value).strip() or not 1 <= port <= 65535:
                raise ValueError("Allowed image ports must be integers from 1 to 65535")
            ports.add(port)
        if not ports:
            raise ValueError("At least one image port must be explicitly allowed")
        return ports

    def _reject_ambiguous_loopback(self, host: str) -> None:
        """Allow explicit loopback literals, but reject alternate numeric spellings."""
        if host == "localhost":
            return
        try:
            address = ipaddress.ip_address(host)
        except ValueError:
            return
        if address.is_loopback and host not in {"127.0.0.1", "::1"}:
            raise ImageFetchError("non-canonical loopback address is not allowed")
