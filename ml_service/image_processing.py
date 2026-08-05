from __future__ import annotations

from typing import Tuple

from PIL import Image, ImageOps


OPAQUE_WHITE_RGB: Tuple[int, int, int] = (255, 255, 255)


def normalize_image_rgb(
    image: Image.Image,
    *,
    background: Tuple[int, int, int] = OPAQUE_WHITE_RGB,
) -> Image.Image:
    """Apply EXIF orientation and return an opaque RGB image.

    Direct ``RGBA -> RGB`` conversion preserves the RGB values stored behind
    fully transparent pixels. Those invisible values must not influence model
    inference, so alpha-bearing images are explicitly composited over the
    documented opaque background first.
    """

    oriented = ImageOps.exif_transpose(image)
    has_alpha = "A" in oriented.getbands() or (
        oriented.mode == "P" and "transparency" in oriented.info
    )
    if not has_alpha:
        return oriented.convert("RGB")

    foreground = oriented.convert("RGBA")
    opaque_background = Image.new(
        "RGBA", foreground.size, color=(*background, 255)
    )
    return Image.alpha_composite(opaque_background, foreground).convert("RGB")
