#!/usr/bin/env python3
"""
gen_textures.py - paints every ORIGINAL texture of the mod (no third-party art):

  * neon signs: the 18 Japanese signs of the provided set translated to English + 16 new Black Market designs
  * hacker den posters / screens (replace the 80s room posters that showed other companies' logos)
  * weapon rack: provided texture with the military emblem removed and a "Weapon Cartel" crest painted in
  * entity / item / block textures for the from-scratch models (golem suit, lava suit, crates, gadgets ...)

    python3 tools/gen_textures.py
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont, ImageOps

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, "src/main/resources/assets/limitlessheist")
GEN = os.path.join(ROOT, "tools/generated")
SRC = os.path.join(ROOT, "source_models")
os.makedirs(GEN, exist_ok=True)

FONT_BOLD = "/usr/share/fonts/opentype/inter/Inter-Bold.otf"
FONT_BLACK = "/usr/share/fonts/opentype/inter/Inter-Black.otf"
FONT_MONO = "/usr/share/fonts/truetype/jetbrains-mono/JetBrainsMono-Bold.ttf"
if not os.path.exists(FONT_MONO):
    import glob
    c = glob.glob("/usr/share/fonts/truetype/jetbrains-mono/*Bold*.ttf") + glob.glob("/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf")
    FONT_MONO = c[0]


def font(path, size):
    return ImageFont.truetype(path, size)


def save(img, rel):
    p = os.path.join(A, "textures", rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p, optimize=True)
    return p


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


# =================================================================================================
#  NEON
# =================================================================================================
def neon_from_mask(tube, color, glow_radius, core_shrink=3, glow_strength=1.0):
    """tube: L mask of the neon tube shape. Returns RGBA with white-hot core, coloured tube and soft halo."""
    w, h = tube.size
    col = np.array(color, dtype=np.float64) / 255
    t = np.asarray(tube).astype(np.float64) / 255
    core = np.asarray(tube.filter(ImageFilter.MinFilter(core_shrink * 2 + 1)).filter(ImageFilter.GaussianBlur(1))).astype(np.float64) / 255
    g1 = np.asarray(tube.filter(ImageFilter.GaussianBlur(glow_radius))).astype(np.float64) / 255
    g2 = np.asarray(tube.filter(ImageFilter.GaussianBlur(glow_radius * 0.35))).astype(np.float64) / 255
    glow = np.clip(g1 * 2.4 * glow_strength + g2 * 1.1, 0, 1)
    rgb = np.zeros((h, w, 3))
    a = np.clip(np.maximum(t, glow * 0.9), 0, 1)
    tube_col = col * 0.75 + 0.25  # tube itself is a bright tint
    rgb += glow[..., None] * col
    rgb = rgb * (1 - t[..., None]) + tube_col * t[..., None]
    rgb = rgb * (1 - core[..., None] * 0.85) + core[..., None] * 0.85 * np.array([1.0, 1.0, 1.0])
    # premultiplied -> straight alpha
    out = np.dstack([np.clip(rgb, 0, 1), a])
    return Image.fromarray((out * 255).astype(np.uint8), "RGBA")


def outline_text_mask(size, draw_fn):
    """draw_fn(draw, fill, stroke) draws the glyphs. Returns hollow tube mask (outline of the glyph)."""
    outer = Image.new("L", size, 0)
    draw_fn(ImageDraw.Draw(outer), 255, True)
    inner = Image.new("L", size, 0)
    draw_fn(ImageDraw.Draw(inner), 255, False)
    return ImageChops.subtract(outer, inner)


def fit_font(path, text, max_w, max_h, start=400):
    s = start
    while s > 8:
        f = font(path, s)
        b = f.getbbox(text, stroke_width=max(3, s // 12))
        if b[2] - b[0] <= max_w and b[3] - b[1] <= max_h:
            return f
        s -= 4
    return font(path, 8)


def neon_lines(size, lines, color, mode="lines", font_path=FONT_BOLD, pad=0.14, stroke=None, icon=None):
    """mode: 'lines' (horizontal lines), 'stack' (one letter per row), 'rot' (rotated 90deg, reads bottom->top)."""
    W, H = size
    if mode == "rot":
        img = neon_lines((H, W), lines, color, "lines", font_path, pad, stroke, icon)
        return img.rotate(90, expand=True)
    rows = list(lines) if mode == "lines" else [c for c in lines[0] if c != " "]
    if all(not r.strip() for r in rows):
        rows = []
    px, py = W * pad, H * pad
    pw, ph = W - 2 * px, H - 2 * py
    tx, ty, tw_, th_ = px, py, pw, ph  # text box
    icon_box = None
    if icon and not rows:
        icon_box = (W / 2, H / 2, min(pw, ph) * 0.48)
    elif icon and W >= H * 3:  # very wide sign: icon on the left, text on the right
        s_ = ph
        icon_box = (px + s_ * 0.5, H / 2, s_ * 0.42)
        tx, tw_ = px + s_ * 1.05, pw - s_ * 1.05
    elif icon:  # tall / square: icon on top
        s_ = ph * 0.5
        icon_box = (W / 2, py + s_ * 0.5, s_ * 0.42)
        ty, th_ = py + s_, ph - s_
    rh = th_ / max(1, len(rows))
    fonts = [fit_font(font_path, r, tw_, rh * 0.84) for r in rows]
    fs = min([f.size for f in fonts] or [int(min(W, H) * 0.3)])
    f = font(font_path, fs)
    st = stroke or max(3, fs // 12)
    if not rows:
        st = stroke or max(4, int(min(W, H) * 0.03))

    def draw_fn(d, fill, with_stroke):
        for i, r in enumerate(rows):
            b = f.getbbox(r)
            tw, th = b[2] - b[0], b[3] - b[1]
            x = tx + (tw_ - tw) / 2 - b[0]
            y = ty + i * rh + (rh - th) / 2 - b[1]
            if with_stroke:
                d.text((x, y), r, font=f, fill=fill, stroke_width=st, stroke_fill=fill)
            else:
                d.text((x, y), r, font=f, fill=fill)
        if icon_box:
            icon(d, fill, with_stroke, icon_box[:2], icon_box[2], st)

    tube = outline_text_mask((W, H), draw_fn)
    return neon_from_mask(tube, color, glow_radius=max(5, min(W, H) * 0.06), core_shrink=max(1, st // 3))


def stroke_shape(fn):
    """Icon helper: icons are drawn as thick lines (tube), with_stroke=False draws nothing (no hollow)."""
    def icon(d, fill, with_stroke, c, r, st):
        if with_stroke:
            fn(d, fill, c, r, max(3, st))
    return icon


def ic_skull(d, fill, c, r, w):
    cx, cy = c
    d.ellipse((cx - r * 0.8, cy - r, cx + r * 0.8, cy + r * 0.45), outline=fill, width=w)
    d.ellipse((cx - r * 0.5, cy - r * 0.45, cx - r * 0.12, cy - r * 0.05), outline=fill, width=w)
    d.ellipse((cx + r * 0.12, cy - r * 0.45, cx + r * 0.5, cy - r * 0.05), outline=fill, width=w)
    d.line((cx - r * 0.45, cy + r * 0.45, cx - r * 0.45, cy + r * 0.85, cx + r * 0.45, cy + r * 0.85, cx + r * 0.45, cy + r * 0.45), fill=fill, width=w)
    for k in (-0.15, 0.15):
        d.line((cx + r * k, cy + r * 0.5, cx + r * k, cy + r * 0.85), fill=fill, width=w)


def ic_diamond(d, fill, c, r, w):
    cx, cy = c
    pts = [(cx - r, cy - r * 0.3), (cx - r * 0.5, cy - r * 0.8), (cx + r * 0.5, cy - r * 0.8), (cx + r, cy - r * 0.3), (cx, cy + r), (cx - r, cy - r * 0.3)]
    d.line(pts, fill=fill, width=w, joint="curve")
    d.line((cx - r, cy - r * 0.3, cx + r, cy - r * 0.3), fill=fill, width=w)
    d.line((cx - r * 0.5, cy - r * 0.8, cx - r * 0.2, cy - r * 0.3, cx, cy + r, cx + r * 0.2, cy - r * 0.3, cx + r * 0.5, cy - r * 0.8), fill=fill, width=max(2, w // 2))


def ic_crosshair(d, fill, c, r, w):
    cx, cy = c
    d.ellipse((cx - r * 0.75, cy - r * 0.75, cx + r * 0.75, cy + r * 0.75), outline=fill, width=w)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        d.line((cx + dx * r * 0.35, cy + dy * r * 0.35, cx + dx * r, cy + dy * r), fill=fill, width=w)


def ic_eye(d, fill, c, r, w):
    cx, cy = c
    d.arc((cx - r, cy - r * 1.1, cx + r, cy + r * 0.9), 20, 160, fill=fill, width=w)
    d.arc((cx - r, cy - r * 0.9, cx + r, cy + r * 1.1), 200, 340, fill=fill, width=w)
    d.ellipse((cx - r * 0.3, cy - r * 0.3, cx + r * 0.3, cy + r * 0.3), outline=fill, width=w)


def ic_noentry(d, fill, c, r, w):
    cx, cy = c
    d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=fill, width=w)
    d.line((cx - r * 0.7, cy - r * 0.7, cx + r * 0.7, cy + r * 0.7), fill=fill, width=w)


def ic_arrow(d, fill, c, r, w):
    cx, cy = c
    d.line((cx - r * 1.4, cy, cx + r * 1.2, cy), fill=fill, width=w)
    d.line((cx + r * 0.5, cy - r * 0.7, cx + r * 1.25, cy, cx + r * 0.5, cy + r * 0.7), fill=fill, width=w, joint="curve")


def ic_code(d, fill, c, r, w):
    cx, cy = c
    d.line((cx - r * 0.6, cy - r * 0.7, cx - r * 1.3, cy, cx - r * 0.6, cy + r * 0.7), fill=fill, width=w, joint="curve")
    d.line((cx + r * 0.6, cy - r * 0.7, cx + r * 1.3, cy, cx + r * 0.6, cy + r * 0.7), fill=fill, width=w, joint="curve")
    d.line((cx + r * 0.3, cy - r * 0.9, cx - r * 0.3, cy + r * 0.9), fill=fill, width=w)


def ic_crown(d, fill, c, r, w):
    cx, cy = c
    d.line((cx - r, cy + r * 0.6, cx - r, cy - r * 0.6, cx - r * 0.5, cy, cx, cy - r * 0.9, cx + r * 0.5, cy, cx + r, cy - r * 0.6, cx + r, cy + r * 0.6, cx - r, cy + r * 0.6), fill=fill, width=w, joint="curve")


def ic_cup(d, fill, c, r, w):  # cocktail glass for the lounge
    cx, cy = c
    d.line((cx - r, cy - r * 0.8, cx + r, cy - r * 0.8, cx, cy + r * 0.2, cx - r, cy - r * 0.8), fill=fill, width=w, joint="curve")
    d.line((cx, cy + r * 0.2, cx, cy + r * 0.9), fill=fill, width=w)
    d.line((cx - r * 0.5, cy + r * 0.9, cx + r * 0.5, cy + r * 0.9), fill=fill, width=w)


# (id, english text, mode, size, color, japanese original)  -- translated from the provided Japanese sign set
TRANSLATED = [
    ("drugstore", ["DRUG", "STORE"], "lines", (512, 512), "#ffc79a", "薬 (medicine)"),
    ("vacant", ["VACANT"], "lines", (512, 512), "#fff1c9", "空 (vacant / empty)"),
    ("tokyo", ["TOKYO"], "stack", (256, 512), "#fff2a8", "東京 (Tokyo)"),
    ("sushi", ["SUSHI"], "stack", (256, 512), "#f4f59b", "寿司 (sushi)"),
    ("tea_shop", ["TEA  SHOP"], "rot", (256, 512), "#e3f79a", "茶店 (tea shop)"),
    ("dumplings", ["DUMPLINGS"], "rot", (256, 512), "#c9f59a", "餃子 (gyoza dumplings)"),
    ("bbq", ["BBQ"], "stack", (256, 512), "#a8f5a8", "焼肉 (grilled meat / BBQ)"),
    ("no_smoking", ["NO SMOKING"], "rot", (256, 512), "#9cf5c0", "禁煙 (no smoking)"),
    ("mahjong", ["MAHJONG"], "rot", (256, 512), "#9cf5dc", "麻雀 (mahjong)"),
    ("tavern", ["TAVERN"], "stack", (256, 1024), "#8ff0f0", "居酒屋 (izakaya / tavern)"),
    ("beer", ["BEER"], "stack", (256, 1024), "#8fe6ff", "ビール (beer)"),
    ("open", ["OPEN"], "stack", (256, 1024), "#9ad7ff", "営業中 (open for business)"),
    ("hotel", ["HOTEL"], "stack", (256, 1024), "#b3d4ff", "ホテル (hotel)"),
    ("made_in_japan", ["MADE IN JAPAN"], "rot", (256, 1024), "#c3c3ff", "日本製 (made in Japan)"),
    ("parking", ["PARKING"], "stack", (256, 1024), "#c9b3ff", "駐車場 (parking)"),
    ("pachinko", ["PACHINKO"], "stack", (256, 1024), "#ddb0ff", "パチンコ (pachinko)"),
    ("top_cash_paid", ["TOP CASH PAID"], "rot", (256, 1024), "#f2b0ff", "高価買取 (we buy at high prices)"),
    ("karaoke", ["KARAOKE"], "stack", (256, 1024), "#ffb0e6", "カラオケ (karaoke)"),
]

# original Black Market set (designed for the mod)
ORIGINAL = [
    ("black_market", ["BLACK MARKET"], "lines", (1024, 256), "#ff2bd6", None),
    ("weapon_cartel", ["WEAPON CARTEL"], "lines", (1024, 256), "#ff2a2a", ic_crosshair),
    ("hackers_den", ["HACKER'S DEN"], "lines", (1024, 256), "#39ff6a", ic_code),
    ("dealers_lounge", ["DEALER'S LOUNGE"], "lines", (1024, 256), "#ffc23a", ic_cup),
    ("limitless", ["LIMITLESS"], "lines", (1024, 256), "#a64dff", None),
    ("vip_only", ["VIP", "ONLY"], "lines", (512, 256), "#ffd24a", ic_crown),
    ("no_cops", ["NO COPS"], "lines", (512, 256), "#ff3b3b", ic_noentry),
    ("cash_only", ["CASH ONLY"], "lines", (512, 256), "#42ff7b", None),
    ("open_24_7", ["OPEN 24/7"], "lines", (512, 256), "#38e8ff", None),
    ("heist", ["HEIST"], "lines", (512, 256), "#ff304f", None),
    ("this_way", ["THIS WAY"], "lines", (512, 256), "#ff5fd2", ic_arrow),
    ("skull", [""], "lines", (512, 512), "#7dff3a", ic_skull),
    ("diamond", [""], "lines", (512, 512), "#3af0ff", ic_diamond),
    ("watching", ["WATCHING"], "lines", (512, 512), "#ff2244", ic_eye),
    ("dollar", ["$"], "lines", (512, 512), "#3dff85", None),
    ("underground", ["UNDERGROUND"], "rot", (256, 1024), "#ff2bd6", None),
]


def gen_neon():
    designs = []
    for nid, lines, mode, size, color, jp in TRANSLATED + ORIGINAL:
        icon = None
        if callable(jp):
            icon, jp = stroke_shape(jp), None
        if lines == [""]:
            lines = [" "]
        img = neon_lines(size, lines, hexrgb(color), mode, icon=icon)
        save(img, "block/neon/%s.png" % nid)
        designs.append({"id": nid, "width": size[0] / 256.0, "height": size[1] / 256.0, "color": color,
                        "translated_from": jp})
    # design table read by the mod (order = right-click cycle order)
    p = os.path.join(A, "neon_designs.json")
    json.dump({"designs": designs}, open(p, "w"), indent=1, ensure_ascii=False)
    # same table as Java (common code needs the count / sizes for cycling and server-side logic)
    jl = []
    for d in designs:
        tf = "null" if not d["translated_from"] else '"%s"' % d["translated_from"].replace('"', '\\"')
        jl.append('            new Design("%s", %.3ff, %.3ff, 0x%s, %s)' % (d["id"], d["width"], d["height"], d["color"].lstrip("#"), tf))
    java = '''package com.limitlessanant.heist.block;

/**
 * GENERATED by tools/gen_textures.py - do not edit by hand.
 * Neon sign designs: texture = limitlessheist:textures/block/neon/<id>.png, size in blocks at scale 1.
 * translatedFrom = the Japanese sign of the provided "Neon cyberpunk signs" set this design translates.
 */
public final class NeonDesigns {
    public record Design(String id, float width, float height, int color, String translatedFrom) {
    }

    public static final Design[] ALL = {
%s
    };

    public static int count() {
        return ALL.length;
    }

    public static Design get(int i) {
        return ALL[Math.floorMod(i, ALL.length)];
    }

    public static int indexOf(String id) {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].id().equals(id)) return i;
        return 0;
    }

    private NeonDesigns() {
    }
}
''' % ",\n".join(jl)
    jp = os.path.join(ROOT, "src/main/java/com/limitlessanant/heist/block/NeonDesigns.java")
    open(jp, "w", encoding="utf-8").write(java)
    # contact sheet for the README
    sheet_w = 2048
    x = y = 0
    rowh = 0
    tiles = []
    for d in designs:
        im = Image.open(os.path.join(A, "textures/block/neon/%s.png" % d["id"]))
        im = im.resize((int(im.width * 0.5), int(im.height * 0.5)))
        if x + im.width > sheet_w:
            x, y, rowh = 0, y + rowh + 12, 0
        tiles.append((im, x, y))
        x += im.width + 12
        rowh = max(rowh, im.height)
    sheet = Image.new("RGBA", (sheet_w, y + rowh), (8, 6, 14, 255))
    for im, x, y in tiles:
        sheet.alpha_composite(im, (x, y))
    os.makedirs(os.path.join(ROOT, "docs"), exist_ok=True)
    sheet.convert("RGB").save(os.path.join(ROOT, "docs/neon_signs.png"))


# =================================================================================================
#  HACKER DEN posters / screens
# =================================================================================================
def gradient(size, top, bottom):
    w, h = size
    t = np.linspace(0, 1, h)[:, None, None]
    a = np.array(top, float)[None, None] * (1 - t) + np.array(bottom, float)[None, None] * t
    return Image.fromarray(np.repeat(a, w, 1).astype(np.uint8), "RGB")


def paste_neon(base, text_img, xy):
    base.alpha_composite(text_img, xy)


def poster_blackmarket():
    W, H = 512, 1024
    im = gradient((W, H), (10, 4, 22), (40, 0, 30)).convert("RGBA")
    d = ImageDraw.Draw(im)
    for i in range(0, H, 6):  # scanlines
        d.line((0, i, W, i), fill=(0, 0, 0, 40))
    # city silhouette
    rnd = random.Random(7)
    x = 0
    while x < W:
        bw = rnd.randint(30, 70)
        bh = rnd.randint(200, 520)
        d.rectangle((x, H - bh, x + bw, H), fill=(14, 10, 26, 255))
        for wy in range(H - bh + 10, H - 10, 18):
            for wx in range(x + 6, x + bw - 6, 12):
                if rnd.random() < 0.35:
                    d.rectangle((wx, wy, wx + 4, wy + 7), fill=rnd.choice([(255, 43, 214), (56, 232, 255), (255, 194, 58)]))
        x += bw + 4
    im.alpha_composite(neon_lines((W, 240), ["BLACK", "MARKET"], hexrgb("#ff2bd6"), pad=0.08), (0, 60))
    im.alpha_composite(neon_lines((W, 90), ["VIP ACCESS ONLY"], hexrgb("#38e8ff"), pad=0.08), (0, 320))
    f = font(FONT_MONO, 22)
    d.text((W / 2, 440), "LEVEL -7  //  NO COPS  //  CASH ONLY", font=f, fill=(255, 194, 58), anchor="mm")
    return im.convert("RGB")


def poster_zeroday():
    W = H = 512
    im = Image.new("RGBA", (W, H), (4, 10, 6, 255))
    d = ImageDraw.Draw(im)
    rnd = random.Random(3)
    f = font(FONT_MONO, 14)
    for y in range(0, H, 16):
        d.text((4, y), "".join(rnd.choice("01ABCDEF") for _ in range(60)), font=f, fill=(0, 70, 20))
    sk = Image.new("L", (W, H), 0)
    ic_skull(ImageDraw.Draw(sk), 255, (W / 2, H * 0.42), 150, 14)
    im.alpha_composite(neon_from_mask(sk, (60, 255, 110), 14), (0, 0))
    im.alpha_composite(neon_lines((W, 110), ["ZERO DAY"], (60, 255, 110), pad=0.06), (0, 390))
    return im.convert("RGB")


def poster_wanted_unicorn():
    W, H = 256, 512
    im = Image.new("RGB", (W, H), (206, 186, 140))
    a = np.asarray(im).astype(float)
    rnd = np.random.default_rng(5)
    a *= 0.85 + 0.15 * rnd.random((H, W, 1))
    im = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    d.rectangle((6, 6, W - 7, H - 7), outline=(80, 40, 20), width=4)
    d.text((W / 2, 52), "WANTED", font=font(FONT_BLACK, 52), fill=(90, 20, 20), anchor="mm")
    d.text((W / 2, 98), "LEGENDARY UNICORN", font=font(FONT_BOLD, 18), fill=(60, 30, 20), anchor="mm")
    # unicorn head silhouette
    d.rectangle((40, 125, W - 40, 325), fill=(40, 30, 40))
    pts = [(95, 300), (110, 220), (150, 175), (172, 130), (168, 185), (200, 205), (214, 238), (196, 250), (168, 238), (150, 300)]
    d.polygon(pts, fill=(15, 10, 20))
    d.line((172, 130, 160, 175), fill=(255, 90, 230), width=4)
    d.ellipse((176, 205, 184, 213), fill=(255, 30, 60))
    d.text((W / 2, 350), "OWNER: GOTIYA", font=font(FONT_BOLD, 20), fill=(60, 30, 20), anchor="mm")
    d.text((W / 2, 376), "(he is always sleeping)", font=font(FONT_BOLD, 13), fill=(90, 60, 40), anchor="mm")
    d.text((W / 2, 410), "TAME WITH: CRAZY APPLE", font=font(FONT_BOLD, 15), fill=(90, 20, 20), anchor="mm")
    d.text((W / 2, 455), "REWARD", font=font(FONT_BLACK, 24), fill=(60, 30, 20), anchor="mm")
    d.text((W / 2, 484), "1,000,000 $", font=font(FONT_BLACK, 26), fill=(30, 90, 30), anchor="mm")
    return im


def poster_missions():
    W, H = 256, 512
    im = Image.new("RGBA", (W, H), (12, 12, 18, 255))
    d = ImageDraw.Draw(im)
    for x in range(0, W, 16):
        d.line((x, 0, x, H), fill=(22, 22, 34))
    for y in range(0, H, 16):
        d.line((0, y, W, y), fill=(22, 22, 34))
    im.alpha_composite(neon_lines((W, 70), ["HEIST PLAN"], hexrgb("#ff304f"), pad=0.06), (0, 8))
    f1, f2 = font(FONT_MONO, 17), font(FONT_MONO, 13)
    rows = [("M1", "MONA LISA", "8K MUSEUM", "#ffc23a"), ("M2", "UNICORN", "GOTIYA'S STABLE", "#ff5fd2"), ("M3", "DRAGON CORE", "FREEZE RAY", "#38e8ff")]
    y = 100
    for tag, a, b, c in rows:
        d.rectangle((14, y, W - 14, y + 104), outline=hexrgb(c), width=3)
        d.text((24, y + 10), tag, font=font(FONT_BLACK, 34), fill=hexrgb(c))
        d.text((90, y + 18), a, font=f1, fill=(240, 240, 240))
        d.text((90, y + 44), b, font=f2, fill=(170, 170, 190))
        d.text((24, y + 74), "STATUS: PENDING", font=f2, fill=(255, 80, 80))
        y += 124
    d.text((W / 2, 488), "BUNKER: GOLD > DIAMOND > NETHERITE", font=font(FONT_MONO, 10), fill=(255, 194, 58), anchor="mm")
    return im.convert("RGB")


def screen_boot():
    W, H = 512, 256
    im = Image.new("RGBA", (W, H), (2, 2, 6, 255))
    im.alpha_composite(neon_lines((W, 120), ["LIMITLESS OS"], hexrgb("#a64dff"), pad=0.08), (0, 40))
    d = ImageDraw.Draw(im)
    d.rectangle((156, 180, 356, 192), outline=(166, 77, 255), width=2)
    d.rectangle((159, 183, 290, 189), fill=(56, 232, 255))
    d.text((W / 2, 215), "loading black market uplink...", font=font(FONT_MONO, 14), fill=(150, 150, 170), anchor="mm")
    return im.convert("RGB")


def screen_terminal():
    W, H = 512, 256
    im = Image.new("RGB", (W, H), (0, 6, 2))
    d = ImageDraw.Draw(im)
    f = font(FONT_MONO, 13)
    lines = [
        "root@blackmarket:~# ./breach --target museum_8k",
        "[+] connecting to vault grid ........ OK",
        "[+] bypassing firewall .............. OK",
        "[+] laser grid sector A ............. OFFLINE",
        "[+] laser grid sector B ............. OFFLINE",
        "[!] VIP golem patrol detected (x4)",
        "[+] camera loop injected ............ OK",
        "[+] mona_lisa.obj located: HALL 3",
        "",
        "   ACCESS GRANTED - WELCOME, LIMITLESS",
        "",
        "root@blackmarket:~# _",
    ]
    for i, l in enumerate(lines):
        col = (60, 255, 110)
        if "OFFLINE" in l or "[!]" in l:
            col = (255, 80, 80)
        if "ACCESS" in l:
            col = (255, 194, 58)
        d.text((10, 10 + i * 19), l, font=f, fill=col)
    d.rectangle((0, H - 18, W, H), fill=(60, 255, 110))
    d.text((8, H - 16), " 0:breach*  1:uplink  2:cams        LIMITLESS-NET", font=font(FONT_MONO, 12), fill=(0, 20, 6))
    return im


def gen_hacker():
    poster_blackmarket().save(os.path.join(GEN, "hk_poster_blackmarket.png"))
    poster_zeroday().save(os.path.join(GEN, "hk_poster_zeroday.png"))
    poster_wanted_unicorn().save(os.path.join(GEN, "hk_poster_wanted_unicorn.png"))
    poster_missions().save(os.path.join(GEN, "hk_poster_missions.png"))
    screen_boot().save(os.path.join(GEN, "hk_screen_boot.png"))
    screen_terminal().save(os.path.join(GEN, "hk_screen_terminal.png"))


# =================================================================================================
#  WEAPON RACK: remove the military emblem / lettering, paint our own crest
# =================================================================================================
def gen_weapon_rack():
    src = Image.open(os.path.join(SRC, "weapon_rack/textures/02___Default_baseColor.jpeg")).convert("RGB")
    im = src.copy()
    # 1) emblem on the hex mesh: patch with hex mesh copied from directly above (same x -> grid stays aligned)
    x0, y0, x1, y1 = 480, 1250, 1700, 2050
    dy = 0
    # find a vertical shift that keeps the hex pattern aligned (search for best match)
    ref = np.asarray(src.crop((x0, 200, x1, 600)).convert("L")).astype(float)
    best, bdy = 1e18, 880
    for s in range(840, 960, 2):
        cand = np.asarray(src.crop((x0, 200 + s, x1, 600 + s)).convert("L")).astype(float)
        e = ((cand - ref) ** 2).mean()
        if e < best:
            best, bdy = e, s
    patch = src.crop((x0, y0 - 2 * bdy if y0 - 2 * bdy > 0 else y0 - bdy, x1, (y0 - 2 * bdy if y0 - 2 * bdy > 0 else y0 - bdy) + (y1 - y0)))
    im.paste(patch, (x0, y0))
    # 2) side lettering + small serial text: fill with surrounding panel colour
    d = ImageDraw.Draw(im)
    for box in ((2370, 1400, 2545, 1980), (805, 3430, 895, 3620), (95, 3870, 175, 4055)):
        ring = np.asarray(src.crop((box[0] - 30, box[1], box[0] - 5, box[3])))
        col = tuple(int(c) for c in np.median(ring.reshape(-1, 3), 0))
        d.rectangle(box, fill=col)
    # 3) our crest: crossed rifles + skull + "WEAPON CARTEL", same light-grey ink as the original print
    ink = (150, 150, 155)
    crest = Image.new("RGBA", (1200, 800), (0, 0, 0, 0))
    cd = ImageDraw.Draw(crest)
    cx, cy = 600, 330
    for sgn in (1, -1):  # crossed rifles
        cd.line((cx - 420 * sgn, cy + 250, cx + 420 * sgn, cy - 250), fill=ink, width=34)
        cd.rectangle((cx + 300 * sgn - 40, cy - 190 - 20, cx + 300 * sgn + 40, cy - 190 + 20), fill=ink)
    ic_skull(cd, ink, (cx, cy), 190, 26)
    cd.text((cx, 690), "WEAPON CARTEL", font=font(FONT_BLACK, 110), fill=ink, anchor="mm")
    # original print reads rotated/mirrored on the panel -> rotate to match UV orientation
    crest = crest.rotate(90, expand=True)
    crest = crest.resize((int(crest.width * 0.85), int(crest.height * 0.85)))
    im.paste(crest, (x0 + (x1 - x0 - crest.width) // 2, y0 + (y1 - y0 - crest.height) // 2), crest)
    # side lettering replacement
    side = Image.new("RGBA", (580, 175), (0, 0, 0, 0))
    ImageDraw.Draw(side).text((290, 88), "CARTEL", font=font(FONT_BLACK, 120), fill=(150, 150, 155), anchor="mm")
    side = side.rotate(90, expand=True)
    im.paste(side, (2370, 1400), side)
    im.resize((2048, 2048), Image.LANCZOS).save(os.path.join(GEN, "weapon_rack_base.png"))


if __name__ == "__main__":
    gen_neon()
    gen_hacker()
    gen_weapon_rack()
    try:
        import gen_textures_entities  # noqa: F401  (entity/item/block textures, separate file)
        gen_textures_entities.main()
    except ImportError:
        pass
    print("textures written")
