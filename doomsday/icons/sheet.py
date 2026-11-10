"""A comparison sheet of the icon options: each as it'd sit in Google TV's apps row (real size
and 2x), plus the square icon. Run: python3 sheet.py OUT.png"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
HERE = Path(__file__).resolve().parent
F = HERE.parent.parent / "doomcore" / "src" / "main" / "assets" / "fonts"
opts = sorted(HERE.glob("option-*"), key=lambda p: int(p.name.split("-")[1]))
current = HERE.parent / "src" / "main" / "res" / "drawable-nodpi"
rows = [("Now (current)", current / "banner.png", current / "icon.png")] + [(f"{p.name.split('-')[1]} · {(p/'name.txt').read_text().strip()}", p / "banner.png", p / "icon.png") for p in opts]
RH = 420
W = 2000
sheet = Image.new("RGB", (W, 90 + RH * len(rows)), (16, 18, 22))
d = ImageDraw.Draw(sheet)
big = ImageFont.truetype(str(F / "manrope_700.ttf"), 34)
small = ImageFont.truetype(str(F / "manrope_500.ttf"), 20)
d.text((40, 26), "Doomsday app icon options (original art, no film images). Left: Google TV row at real size; middle: 2x; right: square icon", font=small, fill=(220, 225, 230))
def tile(colour):
    t = Image.new("RGB", (320, 180), colour)
    return t
for i, (name, b, ic) in enumerate(rows):
    y = 90 + i * RH
    d.text((40, y + 6), name, font=big, fill=(240, 240, 240))
    # a Google TV-like row: neighbours greyed, ours focused (scaled 1.1 with a white ring)
    rx, ry = 40, y + 80
    for k in range(3):
        x = rx + k * 340
        if k == 1:
            ban = Image.open(b).convert("RGB").resize((320, 180))
            fb = ban.resize((352, 198))
            sheet.paste(fb, (x - 16, ry - 9))
            d.rounded_rectangle([x - 20, ry - 13, x + 336, ry + 193], 14, outline=(255, 255, 255), width=4)
        else:
            d.rounded_rectangle([x, ry, x + 320, ry + 180], 12, fill=(40, 44, 52) if k == 0 else (54, 58, 66))
    ban2 = Image.open(b).convert("RGB").resize((640, 360), Image.LANCZOS) if Image.open(b).size[0] >= 640 else Image.open(b).convert("RGB").resize((640, 360), Image.NEAREST)
    # 2x: show the banner doubled (upscaled from 320x180) to judge detail
    ban2 = Image.open(b).convert("RGB").resize((560, 315), Image.LANCZOS)
    sheet.paste(ban2, (1080, y + 60))
    ico = Image.open(ic).convert("RGB").resize((300, 300), Image.LANCZOS)
    mask = Image.new("L", (300, 300), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, 299, 299], 60, fill=255)
    sheet.paste(ico, (1670, y + 60), mask)
sheet.save(sys.argv[1])
print(sheet.size)
