"""The Doomsday app's icon options, drawn in code (original art: no film character, mask or logo).

Each option N writes icons/option-N/: banner.png (320x180, the TV's apps row), icon.png (512,
the square icon), and fg.png + bg.png (432, Android's adaptive icon layers). The chosen one is
copied into the app with use_icon.sh N. Run: python3 make_icons.py [out_dir] (Pillow + numpy).
"""

from __future__ import annotations

import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = Path(__file__).resolve().parent
FONTS = HERE.parent.parent / "doomcore" / "src" / "main" / "assets" / "fonts"
S = 4  # drawn at 4x, then scaled down for clean edges

GREEN = (96, 236, 150)
DEEP = (20, 120, 70)
GOLD = (240, 196, 110)
DARK = (5, 9, 7)


def font(weight: int, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONTS / f"manrope_{weight}.ttf"), size)


def canvas(w: int, h: int, colour=DARK) -> Image.Image:
    return Image.new("RGBA", (w * S, h * S), colour + (255,))


def glow(img: Image.Image, layer: Image.Image, radius: float, strength: float = 1.0) -> Image.Image:
    """Adds [layer] and a blurred copy of it (light), on top of [img]."""
    blur = layer.filter(ImageFilter.GaussianBlur(radius * S))
    strength *= 1.6
    a = np.asarray(img).astype(np.float32)
    for extra, k in ((blur, strength), (layer, 1.0)):
        b = np.asarray(extra).astype(np.float32)
        alpha = b[..., 3:4] / 255.0 * k
        a[..., :3] = np.clip(a[..., :3] + b[..., :3] * alpha, 0, 255)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def radial(w: int, h: int, cx: float, cy: float, r: float, colour, peak: float) -> Image.Image:
    yy, xx = np.mgrid[0 : h * S, 0 : w * S].astype(np.float32)
    d = np.sqrt((xx - cx * S) ** 2 + (yy - cy * S) ** 2) / (r * S)
    a = np.clip(1 - d, 0, 1) ** 2 * peak
    arr = np.zeros((h * S, w * S, 4), np.uint8)
    arr[..., :3] = colour
    arr[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(arr, "RGBA")


def layer(w: int, h: int) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    im = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)


def vignette(img: Image.Image) -> Image.Image:
    w, h = img.size
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    d = np.sqrt(((xx - w / 2) / (w / 2)) ** 2 + ((yy - h / 2) / (h / 2)) ** 2)
    k = np.clip(1.15 - d * 0.55, 0.35, 1)[..., None]
    a = np.asarray(img).astype(np.float32)
    a[..., :3] *= k
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def wordmark(img: Image.Image, w: int, h: int, y: float, size: int = 15) -> Image.Image:
    lay, d = layer(w, h)
    f = font(700, size * S)
    text = "D O O M S D A Y"
    tw = d.textlength(text, font=f)
    d.text(((w * S - tw) / 2, y * S), text, font=f, fill=GOLD + (255,))
    return glow(img, lay, 2, 0.35)


def gold_frame(img: Image.Image, w: int, h: int, inset: float, radius: float, width: float = 1.2) -> Image.Image:
    lay, d = layer(w, h)
    d.rounded_rectangle([inset * S, inset * S, (w - inset) * S, (h - inset) * S], radius=radius * S, outline=GOLD + (170,), width=int(width * S))
    return glow(img, lay, 1.5, 0.3)


def done(img: Image.Image, w: int, h: int) -> Image.Image:
    return vignette(img).resize((w, h), Image.LANCZOS)


# ---- the scenes: each draws into a w x h box (centre-weighted), for banner and icon alike.


def arch_window(img, w, h, cx, cy, scale):
    """1: a stained-glass arch window, green and gold, light pouring out."""
    img = Image.alpha_composite(img, radial(w, h, cx, cy, 90 * scale, DEEP, 0.9))
    lay, d = layer(w, h)
    aw, ah = 52 * scale, 110 * scale
    x0, x1, top, bottom = cx - aw / 2, cx + aw / 2, cy - ah / 2, cy + ah / 2
    pts = [(x0, bottom), (x0, top + aw * 0.55)]
    for i in range(21):
        t = math.pi * i / 20
        pts.append((cx - math.cos(t) * aw / 2, top + aw * 0.55 - math.sin(t) * aw * 0.55 * 1.1))
    pts += [(x1, top + aw * 0.55), (x1, bottom)]
    d.polygon([(x * S, y * S) for x, y in pts], fill=(150, 230, 120, 255))
    # leaded panes: vertical bars and diamonds, alternating green and gold glass
    for i, col in enumerate([(70, 210, 120), (230, 200, 90), (120, 240, 150), (60, 180, 100)]):
        yy = top + aw * 0.55 + i * (ah - aw * 0.55) / 4
        d.rectangle([x0 * S, yy * S, x1 * S, (yy + (ah - aw * 0.55) / 4) * S], fill=col + (120,))
    for k in range(1, 4):
        x = x0 + aw * k / 4
        d.line([(x * S, (top + aw * 0.2) * S), (x * S, bottom * S)], fill=(10, 20, 14, 255), width=int(2.2 * scale * S))
    for k in range(1, 5):
        y = top + aw * 0.55 + k * (ah - aw * 0.55) / 5
        d.line([(x0 * S, y * S), (x1 * S, y * S)], fill=(10, 20, 14, 255), width=int(1.6 * scale * S))
    d.line([(cx * S, top * S), (cx * S, bottom * S)], fill=(10, 20, 14, 255), width=int(2.6 * scale * S))
    d.line([(x * S, y * S) for x, y in pts], fill=GOLD + (255,), width=int(2.4 * scale * S), joint="curve")
    img = glow(img, lay, 14 * scale, 0.9)
    # light falling on the floor
    img = Image.alpha_composite(img, radial(w, h, cx, bottom + 6 * scale, 70 * scale, (120, 230, 140), 0.35))
    return img


def countdown_ring(img, w, h, cx, cy, scale):
    """2: a glowing green countdown ring with a thin gold frame and the days left."""
    img = Image.alpha_composite(img, radial(w, h, cx, cy, 80 * scale, DEEP, 0.7))
    lay, d = layer(w, h)
    r = 52 * scale
    d.ellipse([(cx - r - 8 * scale) * S, (cy - r - 8 * scale) * S, (cx + r + 8 * scale) * S, (cy + r + 8 * scale) * S], outline=GOLD + (200,), width=int(1.4 * scale * S))
    d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], outline=(40, 90, 60, 255), width=int(7 * scale * S))
    d.arc([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], -90, 210, fill=GREEN + (255,), width=int(7 * scale * S))
    for k in range(60):
        a = math.tau * k / 60
        r0, r1 = r - 14 * scale, r - (19 if k % 5 == 0 else 16) * scale
        d.line([((cx + math.cos(a) * r0) * S, (cy + math.sin(a) * r0) * S), ((cx + math.cos(a) * r1) * S, (cy + math.sin(a) * r1) * S)], fill=(160, 220, 180, 160), width=int(1 * scale * S))
    img = glow(img, lay, 8 * scale, 0.8)
    lay, d = layer(w, h)
    f = font(300, int(46 * scale * S))
    t = "35"
    tw = d.textlength(t, font=f)
    d.text(((cx * S - tw / 2), (cy - 31 * scale) * S), t, font=f, fill=(240, 255, 245, 255))
    f2 = font(700, int(8 * scale * S))
    t2 = "D A Y S"
    d.text(((cx * S - d.textlength(t2, font=f2) / 2), (cy + 22 * scale) * S), t2, font=f2, fill=GOLD + (230,))
    return glow(img, lay, 4 * scale, 0.4)


def gauntlet_orb(img, w, h, cx, cy, scale):
    """3: a dark armoured hand, palm up, holding a green energy orb."""
    img = Image.alpha_composite(img, radial(w, h, cx, cy - 10 * scale, 90 * scale, DEEP, 0.8))
    lay, d = layer(w, h)
    orb = 22 * scale
    oy = cy - 18 * scale
    d.ellipse([(cx - orb) * S, (oy - orb) * S, (cx + orb) * S, (oy + orb) * S], fill=(170, 255, 200, 255))
    img = glow(img, lay, 16 * scale, 1.2)
    lay, d = layer(w, h)
    for k in range(6):
        a = k * math.tau / 6 + 0.4
        pts = [(cx + math.cos(a) * orb * 0.4, oy + math.sin(a) * orb * 0.4)]
        for j in range(1, 5):
            rr = orb * (0.4 + j * 0.28)
            pts.append((cx + math.cos(a + (0.25 if j % 2 else -0.2)) * rr, oy + math.sin(a + (0.25 if j % 2 else -0.2)) * rr))
        d.line([(x * S, y * S) for x, y in pts], fill=(220, 255, 230, 230), width=int(1.4 * scale * S))
    img = glow(img, lay, 3 * scale, 0.8)
    # the hand: a cupped palm of dark plates with gold edges, fingers curling up round the orb
    lay, d = layer(w, h)
    palm = [(cx - 42 * scale, cy + 4 * scale), (cx + 42 * scale, cy + 4 * scale), (cx + 30 * scale, cy + 30 * scale), (cx + 14 * scale, cy + 58 * scale), (cx - 14 * scale, cy + 58 * scale), (cx - 30 * scale, cy + 30 * scale)]
    d.polygon([(x * S, y * S) for x, y in palm], fill=(16, 22, 20, 255), outline=GOLD + (200,))
    for k, dx in enumerate([-36, -20, -4, 12]):
        x = cx + dx * scale
        top = cy - (8 + (k % 2) * 6) * scale
        d.rounded_rectangle([x * S, top * S, (x + 13 * scale) * S, (cy + 10 * scale) * S], radius=6 * scale * S, fill=(22, 30, 26, 255), outline=GOLD + (160,), width=int(1 * scale * S))
        for j in range(1, 3):
            yy = top + j * (cy + 10 * scale - top) / 3
            d.line([(x * S, yy * S), ((x + 13 * scale) * S, yy * S)], fill=(60, 70, 64, 255), width=int(1 * scale * S))
    d.rounded_rectangle([(cx + 30 * scale) * S, (cy - 4 * scale) * S, (cx + 44 * scale) * S, (cy + 22 * scale) * S], radius=6 * scale * S, fill=(22, 30, 26, 255), outline=GOLD + (160,), width=int(1 * scale * S))
    rim = radial(w, h, cx, oy, 60 * scale, (90, 255, 150), 0.25)
    img = Image.alpha_composite(img, lay)
    return Image.alpha_composite(img, rim)


def summon_circle(img, w, h, cx, cy, scale):
    """4: the summoning floor circle seen from above: rune rings, one igniting."""
    img = Image.alpha_composite(img, radial(w, h, cx, cy, 95 * scale, DEEP, 0.6))
    lay, d = layer(w, h)
    ry = 0.62
    for k, rr in enumerate([66, 52, 38, 24, 11]):
        r = rr * scale
        bright = k == 1
        col = (190, 255, 210, 255) if bright else (70, 200, 120, 210)
        d.ellipse([(cx - r) * S, (cy - r * ry) * S, (cx + r) * S, (cy + r * ry) * S], outline=col, width=int((2.6 if bright else 1.4) * scale * S))
        if k < 3:
            n = 24 - k * 6
            for j in range(n):
                a = math.tau * j / n
                x, y = cx + math.cos(a) * (r - 6 * scale), cy + math.sin(a) * (r - 6 * scale) * ry
                d.line([((x - 1.5 * scale) * S, (y - 2 * scale) * S), ((x + 1.5 * scale) * S, (y + 2 * scale) * S)], fill=col, width=int(1 * scale * S))
    for j in range(8):
        a = math.tau * j / 8
        d.line([(cx * S, cy * S), ((cx + math.cos(a) * 66 * scale) * S, (cy + math.sin(a) * 66 * scale * ry) * S)], fill=(60, 170, 100, 150), width=int(1 * scale * S))
    img = glow(img, lay, 9 * scale, 1.0)
    # a spark rising from the bright ring
    return Image.alpha_composite(img, radial(w, h, cx + 30 * scale, cy - 20 * scale, 16 * scale, (200, 255, 220), 0.7))


def eye_slits(img, w, h, cx, cy, scale):
    """5: two glowing green eye slits in darkness, a faint haze."""
    img = Image.alpha_composite(img, radial(w, h, cx, cy, 80 * scale, (10, 60, 35), 0.8))
    lay, d = layer(w, h)
    for side in (-1, 1):
        x = cx + side * 26 * scale
        pts = [(x - side * 20 * scale, cy - 4 * scale), (x + side * 20 * scale, cy - 10 * scale), (x + side * 17 * scale, cy + 3 * scale), (x - side * 17 * scale, cy + 6 * scale)]
        d.polygon([(px * S, py * S) for px, py in pts], fill=(200, 255, 215, 255))
    return glow(img, lay, 10 * scale, 1.6)


def torch_arch(img, w, h, cx, cy, scale):
    """6: a dark stone arch, green light spilling out, twin torches either side."""
    img = Image.alpha_composite(img, radial(w, h, cx, cy + 10 * scale, 70 * scale, (40, 200, 110), 0.55))
    lay, d = layer(w, h)
    aw, ah = 46 * scale, 74 * scale
    top, bottom = cy - ah / 2, cy + ah / 2
    d.rectangle([(cx - aw / 2 - 12 * scale) * S, (top - 12 * scale) * S, (cx + aw / 2 + 12 * scale) * S, (bottom + 4 * scale) * S], fill=(26, 30, 28, 255))
    # stone blocks round the arch
    for k in range(7):
        yy = top - 12 * scale + k * (ah + 16 * scale) / 7
        d.line([((cx - aw / 2 - 12 * scale) * S, yy * S), ((cx - aw / 2) * S, yy * S)], fill=(50, 56, 52, 255), width=int(1 * scale * S))
        d.line([((cx + aw / 2) * S, yy * S), ((cx + aw / 2 + 12 * scale) * S, yy * S)], fill=(50, 56, 52, 255), width=int(1 * scale * S))
    img = Image.alpha_composite(img, lay)
    lay, d = layer(w, h)
    for k in range(24):  # the doorway: deep green at the top, brighter toward the floor
        t = k / 23
        col = (int(30 + 110 * t), int(150 + 100 * t), int(80 + 90 * t), 255)
        y0 = top + aw / 2 + t * (ah - aw / 2) - 0.01
        d.rectangle([(cx - aw / 2) * S, y0 * S, (cx + aw / 2) * S, (y0 + (ah - aw / 2) / 23 + 0.5) * S], fill=col)
    d.pieslice([(cx - aw / 2) * S, top * S, (cx + aw / 2) * S, (top + aw) * S], 180, 360, fill=(30, 150, 80, 255))
    d.arc([(cx - aw / 2 - 3 * scale) * S, (top - 3 * scale) * S, (cx + aw / 2 + 3 * scale) * S, (top + aw + 3 * scale) * S], 180, 360, fill=GOLD + (200,), width=int(1.6 * scale * S))
    img = glow(img, lay, 12 * scale, 0.9)
    # its light spilling across the floor
    img = Image.alpha_composite(img, radial(w, h, cx, bottom + 4 * scale, 60 * scale, (120, 255, 170), 0.45))
    for side in (-1, 1):
        tx = cx + side * (aw / 2 + 30 * scale)
        lay, d = layer(w, h)
        d.rectangle([(tx - 2 * scale) * S, (cy - 4 * scale) * S, (tx + 2 * scale) * S, (bottom + 4 * scale) * S], fill=(60, 50, 40, 255))
        d.polygon([((tx - 7 * scale) * S, (cy - 4 * scale) * S), ((tx + 7 * scale) * S, (cy - 4 * scale) * S), (tx * S, (cy + 4 * scale) * S)], fill=(90, 70, 40, 255))
        img = Image.alpha_composite(img, lay)
        lay, d = layer(w, h)
        d.ellipse([(tx - 6 * scale) * S, (cy - 24 * scale) * S, (tx + 6 * scale) * S, (cy - 4 * scale) * S], fill=(255, 190, 90, 255))
        d.ellipse([(tx - 3 * scale) * S, (cy - 15 * scale) * S, (tx + 3 * scale) * S, (cy - 5 * scale) * S], fill=(255, 240, 200, 255))
        img = glow(img, lay, 8 * scale, 1.0)
    return img


def bolt_d(img, w, h, cx, cy, scale):
    """7: green energy crackling into a "D" on a dark disc in a gold ring."""
    lay, d = layer(w, h)
    r = 62 * scale
    d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=(12, 20, 16, 255))
    img = Image.alpha_composite(img, lay)
    img = gold_ring(img, w, h, cx, cy, r, scale)
    lay, d = layer(w, h)
    rnd = np.random.default_rng(7)
    pts = []
    x0 = cx - 18 * scale
    for t in np.linspace(-1, 1, 14):  # the spine
        pts.append((x0 + rnd.uniform(-2, 2) * scale, cy + t * 34 * scale))
    for t in np.linspace(-math.pi / 2, math.pi / 2, 22):  # the bowl
        pts.append((x0 + math.cos(t) * 38 * scale + rnd.uniform(-2.5, 2.5) * scale, cy + math.sin(t) * 34 * scale * -1 + rnd.uniform(-2, 2) * scale))
    d.polygon([(x * S, y * S) for x, y in pts], fill=(40, 140, 80, 110))
    d.line([(x * S, y * S) for x, y in pts] + [(pts[0][0] * S, pts[0][1] * S)], fill=(150, 255, 190, 255), width=int(6 * scale * S), joint="curve")
    d.line([(x * S, y * S) for x, y in pts] + [(pts[0][0] * S, pts[0][1] * S)], fill=(235, 255, 240, 255), width=int(2.4 * scale * S), joint="curve")
    for _ in range(5):
        i = rnd.integers(0, len(pts))
        x, y = pts[i]
        d.line([(x * S, y * S), ((x + rnd.uniform(-12, 12) * scale) * S, (y + rnd.uniform(-12, 12) * scale) * S)], fill=(160, 255, 190, 200), width=int(1.2 * scale * S))
    return glow(img, lay, 7 * scale, 1.3)


def gold_ring(img, w, h, cx, cy, r, scale):
    lay, d = layer(w, h)
    d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], outline=GOLD + (230,), width=int(2 * scale * S))
    return glow(img, lay, 3 * scale, 0.5)


OPTIONS = [
    ("Stained-glass arch", arch_window),
    ("Countdown ring", countdown_ring),
    ("Gauntlet and orb", gauntlet_orb),
    ("Summon circle", summon_circle),
    ("Glowing eyes", eye_slits),
    ("Torches and arch", torch_arch),
    ("Energy D", bolt_d),
]


def banner(draw, n: int) -> Image.Image:
    w, h = 320, 180
    img = canvas(w, h)
    img = Image.alpha_composite(img, radial(w, h, 160, 70, 190, (14, 70, 40), 0.9))
    img = draw(img, w, h, 160, 70, 0.92)
    img = wordmark(img, w, h, 145, 16)
    img = gold_frame(img, w, h, 5, 14, 1)
    return done(img, w, h)


def icon(draw, size: int = 512) -> Image.Image:
    img = canvas(size, size)
    img = draw(img, size, size, size / 2, size / 2, size / 160)
    return done(img, size, size)


def adaptive(draw, size: int = 432) -> tuple[Image.Image, Image.Image]:
    """The adaptive icon's layers: the scene in the middle 66% (the safe zone), dark behind."""
    fg = canvas(size, size, (0, 0, 0))
    fg = draw(fg, size, size, size / 2, size / 2, size / 160 * 0.66)
    arr = np.asarray(done(fg, size, size)).copy()
    lum = arr[..., :3].max(axis=2)
    arr[..., 3] = np.clip(lum * 3, 0, 255).astype(np.uint8)  # dark becomes see-through
    bg = Image.new("RGBA", (size, size), DARK + (255,))
    return Image.fromarray(arr, "RGBA"), bg


def main(out: Path) -> None:
    for n, (name, draw) in enumerate(OPTIONS, 1):
        d = out / f"option-{n}"
        d.mkdir(parents=True, exist_ok=True)
        banner(draw, n).convert("RGB").save(d / "banner.png")
        icon(draw).convert("RGB").save(d / "icon.png")
        fg, bg = adaptive(draw)
        fg.save(d / "fg.png")
        bg.convert("RGB").save(d / "bg.png")
        (d / "name.txt").write_text(name + "\n")
        print(n, name)


if __name__ == "__main__":
    main(Path(sys.argv[1]) if len(sys.argv) > 1 else HERE)
