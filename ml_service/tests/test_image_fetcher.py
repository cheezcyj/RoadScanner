from PIL import Image
import pytest
import requests

from ml_service.image_fetcher import (
    ImageFetchError,
    ImageFetcher,
    ImageFetchUnavailable,
)


class NoNetworkSession:
    trust_env = True

    def get(self, *args, **kwargs):
        raise AssertionError("A blocked URL must not reach the network")


class ImageResponse:
    status_code = 200
    headers = {"Content-Type": "image/png"}

    def iter_content(self, chunk_size):
        yield b"image header"

    def close(self):
        pass


class ImageSession:
    trust_env = True

    def get(self, *args, **kwargs):
        return ImageResponse()


class HeaderOnlyImage:
    def __init__(self, size):
        self.size = size
        self.load_called = False
        self.close_called = False

    def load(self):
        self.load_called = True

    def close(self):
        self.close_called = True


def test_image_fetcher_blocks_unlisted_hosts_before_network_access():
    fetcher = ImageFetcher(["127.0.0.1"], session=NoNetworkSession())

    with pytest.raises(ImageFetchError, match="host is not allowed"):
        fetcher.fetch("http://169.254.169.254/latest/meta-data/")


def test_image_fetcher_requires_an_absolute_http_url():
    fetcher = ImageFetcher(["127.0.0.1"], session=NoNetworkSession())

    with pytest.raises(ImageFetchError, match="absolute HTTP"):
        fetcher.fetch("/local-files/sign.png")


def test_image_fetcher_blocks_unlisted_ports_before_network_access():
    fetcher = ImageFetcher(
        ["127.0.0.1"], allowed_ports=[18080], session=NoNetworkSession()
    )

    with pytest.raises(ImageFetchError, match="port is not allowed"):
        fetcher.fetch("http://127.0.0.1:5000/sign.png")


@pytest.mark.parametrize("ports", [[], [0], [65536], [True], ["not-a-port"]])
def test_image_fetcher_requires_an_explicit_valid_port_allowlist(ports):
    with pytest.raises(ValueError, match="image port"):
        ImageFetcher(["127.0.0.1"], allowed_ports=ports)


@pytest.mark.parametrize(
    "limits",
    [
        {"max_pixels": 0},
        {"max_pixels": -1},
        {"max_pixels": 1.5},
        {"max_pixels": True},
        {"max_dimension": 0},
        {"max_dimension": -1},
        {"max_dimension": 1.5},
        {"max_dimension": True},
    ],
)
def test_image_fetcher_requires_positive_decode_limits(limits):
    with pytest.raises(ValueError, match="pixel and dimension limits"):
        ImageFetcher(["127.0.0.1"], session=NoNetworkSession(), **limits)


def test_pixel_limit_is_checked_from_header_before_image_load(monkeypatch):
    header = HeaderOnlyImage((5_001, 5_000))
    monkeypatch.setattr(Image, "open", lambda source: header)
    fetcher = ImageFetcher(["127.0.0.1"], session=ImageSession())

    with pytest.raises(ImageFetchError, match="pixel count"):
        fetcher.fetch("http://127.0.0.1/sign.png")

    assert header.load_called is False
    assert header.close_called is True


def test_dimension_limit_is_checked_from_header_before_image_load(monkeypatch):
    header = HeaderOnlyImage((10_001, 1))
    monkeypatch.setattr(Image, "open", lambda source: header)
    fetcher = ImageFetcher(["127.0.0.1"], session=ImageSession())

    with pytest.raises(ImageFetchError, match="dimensions"):
        fetcher.fetch("http://127.0.0.1/sign.png")

    assert header.load_called is False
    assert header.close_called is True


def test_pillow_decompression_bomb_is_exposed_as_image_fetch_error(monkeypatch):
    def reject_bomb(source):
        raise Image.DecompressionBombError("unsafe image dimensions")

    monkeypatch.setattr(Image, "open", reject_bomb)
    fetcher = ImageFetcher(["127.0.0.1"], session=ImageSession())

    with pytest.raises(ImageFetchError, match="decompression limits"):
        fetcher.fetch("http://127.0.0.1/sign.png")


def test_image_fetcher_enforces_a_total_streaming_deadline(monkeypatch):
    class SlowResponse(ImageResponse):
        def iter_content(self, chunk_size):
            yield b"first"
            yield b"second"

    class SlowSession:
        trust_env = True

        def get(self, *args, **kwargs):
            return SlowResponse()

    clock = iter([0.0, 1.0, 6.0])
    monkeypatch.setattr("ml_service.image_fetcher.time.monotonic", lambda: next(clock))
    fetcher = ImageFetcher(
        ["127.0.0.1"], timeout_seconds=5.0, session=SlowSession()
    )

    with pytest.raises(ImageFetchUnavailable, match="timed out"):
        fetcher.fetch("http://127.0.0.1/sign.png")


def test_network_errors_are_classified_as_transient_unavailability():
    class TimeoutSession:
        trust_env = True

        def get(self, *args, **kwargs):
            raise requests.Timeout("upstream timed out")

    fetcher = ImageFetcher(["127.0.0.1"], session=TimeoutSession())

    with pytest.raises(ImageFetchUnavailable, match="download failed"):
        fetcher.fetch("http://127.0.0.1/sign.png")


def test_server_503_is_classified_as_transient_unavailability():
    class UnavailableResponse(ImageResponse):
        status_code = 503

    class UnavailableSession:
        trust_env = True

        def get(self, *args, **kwargs):
            return UnavailableResponse()

    fetcher = ImageFetcher(["127.0.0.1"], session=UnavailableSession())

    with pytest.raises(ImageFetchUnavailable, match="temporarily unavailable"):
        fetcher.fetch("http://127.0.0.1/sign.png")
