#!/usr/bin/env python3
"""TimeDroid launcher icon: a retro CRT TV with a classic droid on screen."""
from PIL import Image, ImageDraw

TEAL_DEEP = (7, 26, 32)
TEAL_TOP = (18, 72, 86)
BODY = (16, 58, 72)
BODY_EDGE = (32, 96, 114)
SCREEN = (255, 246, 222)
GREEN = (61, 220, 132)
AMBER = (255, 184, 77)
WHITE = (255, 255, 255)


def bg(size):
    im = Image.new("RGB", (size, size), TEAL_DEEP)
    d = ImageDraw.Draw(im)
    for y in range(size):
        t = y / (size - 1)
        c = tuple(int(TEAL_TOP[i] + (TEAL_DEEP[i] - TEAL_TOP[i]) * t) for i in range(3))
        d.line([(0, y), (size, y)], fill=c)
    return im


def draw_tv(d, cx, cy, s, mono=False):
    """Draw the TV centred at (cx, cy); s = overall scale (1.0 -> body width 248 at 432 canvas)."""
    W, H = 248 * s, 176 * s
    x0, y0 = cx - W / 2, cy - H / 2
    body = WHITE if mono else BODY
    edge = WHITE if mono else BODY_EDGE
    # antennae
    aw = max(1, int(10 * s))
    d.line([(cx - W * 0.28, y0 + 6 * s), (cx - W * 0.42, y0 - 62 * s)], fill=edge if not mono else WHITE, width=aw)
    d.line([(cx + W * 0.28, y0 + 6 * s), (cx + W * 0.42, y0 - 62 * s)], fill=edge if not mono else WHITE, width=aw)
    for sx in (-0.42, 0.42):
        r = 9 * s
        d.ellipse([cx + W * sx - r, y0 - 62 * s - r, cx + W * sx + r, y0 - 62 * s + r],
                  fill=(255, 255, 255) if mono else AMBER)
    # body
    d.rounded_rectangle([x0, y0, x0 + W, y0 + H], radius=30 * s, fill=body)
    # screen
    sw, sh = 178 * s, 118 * s
    scx, scy = cx - 22 * s, cy + 8 * s
    sx0, sy0 = scx - sw / 2, scy - sh / 2
    if mono:
        d.rounded_rectangle([sx0, sy0, sx0 + sw, sy0 + sh], radius=14 * s, fill=(0, 0, 0, 0))
        droid(d, scx, scy + 4 * s, 0.62 * s, mono=True)
    else:
        d.rounded_rectangle([sx0, sy0, sx0 + sw, sy0 + sh], radius=14 * s, fill=SCREEN)
        droid(d, scx, scy + 4 * s, 0.62 * s)
        # scanlines
        line_c = (214, 205, 186)
        y = sy0 + 10 * s
        while y < sy0 + sh - 6 * s:
            d.line([(sx0 + 8 * s, y), (sx0 + sw - 8 * s, y)], fill=line_c, width=max(1, int(1.5 * s)))
            y += 12 * s
    # knobs
    kx = x0 + W - 32 * s
    for ky, col in ((cy - 22 * s, AMBER), (cy + 24 * s, SCREEN if not mono else WHITE)):
        r = 10 * s
        d.ellipse([kx - r, ky - r, kx + r, ky + r], fill=col, outline=edge, width=max(1, int(2.5 * s)))


def droid(d, cx, cy, s, mono=False):
    """Classic droid head on the screen."""
    col = WHITE if mono else GREEN
    r = 46 * s
    # head dome
    d.pieslice([cx - r, cy - r * 0.9, cx + r, cy + r * 1.1], 180, 360, fill=col)
    d.rectangle([cx - r, cy - r * 0.02, cx + r, cy + r * 0.42], fill=col)
    # head antennae
    aw = max(1, int(7 * s))
    d.line([(cx - r * 0.55, cy - r * 0.8), (cx - r * 0.82, cy - r * 1.35)], fill=col, width=aw)
    d.line([(cx + r * 0.55, cy - r * 0.8), (cx + r * 0.82, cy - r * 1.35)], fill=col, width=aw)
    # eyes
    if not mono:
        er = 7.5 * s
        for ex in (-r * 0.42, r * 0.42):
            d.ellipse([cx + ex - er, cy - r * 0.52 - er, cx + ex + er, cy - r * 0.52 + er], fill=SCREEN)
    # mouth line
    d.line([(cx - r * 0.5, cy + r * 0.2), (cx + r * 0.5, cy + r * 0.2)],
           fill=(0, 0, 0, 0) if mono else SCREEN, width=max(1, int(5 * s)))


def fg(size):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    s = size / 432.0
    draw_tv(d, size / 2, size * 0.52, s)
    return im


def mono(size):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    s = size / 432.0
    draw_tv(d, size / 2, size * 0.52, s, mono=True)
    return im


def full(size):
    im = bg(size).convert("RGBA")
    d = ImageDraw.Draw(im)
    s = size / 432.0 * 0.92
    draw_tv(d, size / 2, size * 0.52, s)
    return im


if __name__ == "__main__":
    import sys
    out = sys.argv[1]
    for dpi, px in (("mdpi", 108), ("hdpi", 162), ("xhdpi", 216), ("xxhdpi", 324), ("xxxhdpi", 432)):
        fg(px).save(f"{out}/mipmap-{dpi}/ic_launcher_fg.png")
        mono(px).save(f"{out}/mipmap-{dpi}/ic_launcher_mono.png")
    full(512).save(f"{out}/store-icon-512.png")
    print("icons written to", out)
