#!/usr/bin/env python3
"""
Original textures for the models designed from scratch (called by gen_textures.py):

  entity/vip_guard.png            128x128  black suit painted on the vanilla iron golem UV layout
  entity/vip_accessories.png       32x32   sunglasses, lens glint, earpiece, tie
  models/armor/lava_suit.png      128x64   Lava Proof "Shark" Suit (LavaSuitModel UV layout)
  entity/freeze_bolt.png, entity/core_halo.png, block/neon/question.png
  block/mystery_box_*.png, block/laser_emitter*.png, block/neon_block_*.png, block/neon_strip_*.png
  item/*.png                       icons (crazy apple, smoke bomb, hacker usb, lava suit pieces, neon sign, grappling gun)
  icon.png                         mod icon
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, "src/main/resources/assets/limitlessheist")


def save(img, rel):
    p = os.path.join(A, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p, optimize=True)


def hexrgb(h, a=255):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def noisy(img, box, base, amount=10, seed=0):
    """Fill box with base colour + per-pixel noise (gives the hand-painted Minecraft look)."""
    rnd = random.Random(seed)
    x0, y0, x1, y1 = [int(round(v)) for v in box]
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < img.width and 0 <= y < img.height:
                d = rnd.randint(-amount, amount)
                px[x, y] = tuple(max(0, min(255, c + d)) for c in base[:3]) + (base[3] if len(base) > 3 else 255,)


def box_faces(u, v, w, h, d):
    """Minecraft box-UV rectangles (x0,y0,x1,y1) for a cuboid of size w,h,d with texture offset u,v."""
    return {
        "down": (u + d, v, u + d + w, v + d),
        "up": (u + d + w, v, u + d + 2 * w, v + d),
        "west": (u, v + d, u + d, v + d + h),
        "north": (u + d, v + d, u + d + w, v + d + h),  # front
        "east": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def paint_box(img, u, v, w, h, d, color, amount=8, seed=0, faces=None):
    for name, r in box_faces(u, v, w, h, d).items():
        if faces and name not in faces:
            continue
        noisy(img, (math.floor(r[0]), math.floor(r[1]), math.ceil(r[2]), math.ceil(r[3])), color, amount, seed + hash(name) % 97)


# ================================================================================================= VIP GUARD (golem UV)
SUIT = hexrgb("#141418")
SUIT_HI = hexrgb("#24242c")
SHIRT = hexrgb("#ecebe6")
IRON = hexrgb("#b9b4ab")
IRON_DK = hexrgb("#8d877d")
GOLD = hexrgb("#e2b23a")


def vip_guard():
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # ---- head (0,0) 8x10x8: iron face, slick black hair
    paint_box(img, 0, 0, 8, 10, 8, IRON, 9, 1)
    f = box_faces(0, 0, 8, 10, 8)
    noisy(img, f["up"], hexrgb("#0d0d10"), 6, 2)                     # hair on top
    for k in ("west", "east", "south"):
        x0, y0, x1, y1 = f[k]
        noisy(img, (x0, y0, x1, y0 + 3), hexrgb("#0d0d10"), 6, 3)    # hair band around
    x0, y0, x1, y1 = f["north"]                                     # face 8x10 at (8,8)
    noisy(img, (x0, y0, x1, y0 + 2), hexrgb("#0d0d10"), 6, 4)       # hairline
    d.rectangle((x0 + 1, y0 + 3, x0 + 3, y0 + 3), fill=hexrgb("#3a3631"))  # brows (frowning)
    d.rectangle((x0 + 5, y0 + 3, x0 + 6, y0 + 3), fill=hexrgb("#3a3631"))
    d.rectangle((x0 + 1, y0 + 4, x0 + 2, y0 + 4), fill=hexrgb("#4a0d0d"))  # eyes (under the shades)
    d.rectangle((x0 + 5, y0 + 4, x0 + 6, y0 + 4), fill=hexrgb("#4a0d0d"))
    d.rectangle((x0 + 2, y0 + 8, x0 + 5, y0 + 8), fill=hexrgb("#5d5850"))  # serious mouth
    noisy(img, (x0, y0 + 9, x1, y1), IRON_DK, 6, 5)                 # jaw shadow
    # nose (24,0) 2x4x2
    paint_box(img, 24, 0, 2, 4, 2, IRON_DK, 6, 6)
    # ---- body (0,40) 18x12x11: black jacket, white shirt V, lapels, gold pocket square
    paint_box(img, 0, 40, 18, 12, 11, SUIT, 5, 7)
    f = box_faces(0, 40, 18, 12, 11)
    x0, y0, x1, y1 = f["north"]                                     # 18x12 at (11,51)
    cx = x0 + 9
    for row in range(0, 9):                                          # shirt V narrowing downwards
        half = max(1, 4 - row // 2)
        d.line((cx - half, y0 + row, cx + half - 1, y0 + row), fill=SHIRT)
    for row in range(0, 9):                                          # lapels
        half = max(1, 4 - row // 2)
        d.point((cx - half - 1, y0 + row), fill=SUIT_HI)
        d.point((cx + half, y0 + row), fill=SUIT_HI)
    for yy in (y0 + 9, y0 + 11):                                     # buttons
        d.point((cx - 1, yy), fill=hexrgb("#3c3c44"))
    d.rectangle((x0 + 3, y0 + 2, x0 + 4, y0 + 3), fill=GOLD)         # pocket square
    noisy(img, (f["up"][0] + 7, f["up"][1] + 4, f["up"][0] + 11, f["up"][1] + 7), SHIRT, 4, 8)  # collar seen from above
    xs0, ys0, xs1, ys1 = f["south"]
    d.line((xs0 + 9, ys0, xs0 + 9, ys1), fill=SUIT_HI)               # back seam
    # ---- waist (0,70) 9x5x6: belt + buckle
    paint_box(img, 0, 70, 9, 5, 6, SUIT, 5, 9)
    f = box_faces(0, 70, 9, 5, 6)
    for k in ("north", "south", "west", "east"):
        x0, y0, x1, y1 = f[k]
        noisy(img, (x0, y0, x1, y0 + 2), hexrgb("#2a1a12"), 4, 10)
    x0, y0, x1, y1 = f["north"]
    d.rectangle((x0 + 3, y0, x0 + 5, y0 + 1), fill=GOLD)
    # ---- arms: sleeves, white cuffs, iron hands. right (60,21) left (60,58) 4x30x6
    for (u, v) in ((60, 21), (60, 58)):
        paint_box(img, u, v, 4, 30, 6, SUIT, 5, u + v)
        for name, (x0, y0, x1, y1) in box_faces(u, v, 4, 30, 6).items():
            if name in ("up",):
                continue
            if name == "down":
                noisy(img, (x0, y0, x1, y1), IRON, 8, 11)
                continue
            noisy(img, (x0, y0 + 21, x1, y0 + 23), SHIRT, 4, 12)      # cuff
            noisy(img, (x0, y0 + 23, x1, y1), IRON, 8, 13)           # hand
            d.line((x0, y0 + 26, x1 - 1, y0 + 26), fill=IRON_DK)     # knuckles
    # ---- legs (37,0) and (60,0) 6x16x5: trousers + shoes
    for (u, v) in ((37, 0), (60, 0)):
        paint_box(img, u, v, 6, 16, 5, SUIT, 5, u)
        for name, (x0, y0, x1, y1) in box_faces(u, v, 6, 16, 5).items():
            if name == "down":
                noisy(img, (x0, y0, x1, y1), hexrgb("#050505"), 3, 14)
            elif name != "up":
                noisy(img, (x0, y1 - 3, x1, y1), hexrgb("#08080a"), 3, 15)   # shoes
                d.line((x0, y1 - 3, x1 - 1, y1 - 3), fill=hexrgb("#2c2c34"))  # shine
                if name == "north":
                    d.line(((x0 + x1) // 2, y0, (x0 + x1) // 2, y1 - 4), fill=SUIT_HI)  # crease
    save(img, "textures/entity/vip_guard.png")


def vip_accessories():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    noisy(img, (0, 0, 20, 16), hexrgb("#0b0b0e"), 3, 20)             # lens + frame region (glossy black)
    d.line((1, 1, 4, 1), fill=hexrgb("#3a4a66"))                    # cold reflection on lenses
    d.line((1, 5, 4, 5), fill=hexrgb("#3a4a66"))
    noisy(img, (0, 8, 20, 10), hexrgb("#1c1c22"), 3, 21)             # top bar
    # lens glint (drawn additively): diagonal white streak fading out
    g = Image.new("L", (12, 6), 0)
    gd = ImageDraw.Draw(g)
    gd.line((2, 5, 6, 0), fill=255, width=2)
    g = g.filter(ImageFilter.GaussianBlur(0.6))
    img.paste(Image.merge("RGBA", (g, g, g, Image.new("L", g.size, 255))), (20, 0))
    noisy(img, (12, 10, 20, 16), hexrgb("#2a2a30"), 4, 22)           # earpiece
    d.point((13, 11), fill=hexrgb("#cfd8e0"))
    # tie: deep red silk with fine diagonal stripes
    for y in range(16, 32):
        for x in range(0, 16):
            stripe = (x + y) % 4 == 0
            img.putpixel((x, y), hexrgb("#5c0b12") if stripe else hexrgb("#9e1220"))
    noisy(img, (16, 16, 24, 20), GOLD, 6, 23)                        # collar bar
    save(img, "textures/entity/vip_accessories.png")


# ================================================================================================= LAVA SUIT
ORANGE = hexrgb("#ff6a12")
ORANGE_DK = hexrgb("#c2410a")
RED = hexrgb("#b01212")
BLACKP = hexrgb("#1a1414")
STEEL = hexrgb("#8f949c")

# (u, v, w, h, d, role) - must match LavaSuitModel.getTexturedModelData()
LAVA_SUIT_BOXES = [
    (0, 0, 11, 2, 11, "crown"), (44, 0, 11, 7, 2.5, "helmet_back"), (0, 13, 1.2, 4, 4, "ear"), (10, 13, 0.8, 4, 0.8, "antenna"),
    (64, 0, 10, 7.4, 8.8, "visor"), (0, 22, 12, 2, 12, "collar"),
    (16, 36, 10, 12.5, 6.4, "torso"), (48, 36, 6, 5, 1, "console"), (62, 36, 10.4, 2, 6.8, "belt"),
    (84, 0, 8, 10, 3, "pack"), (84, 14, 3, 11, 3, "tank"), (98, 14, 1, 6, 4, "fin"),
    (0, 36, 5.4, 12.5, 5.4, "sleeve"), (0, 54, 6.4, 4, 6.4, "shoulder"),
    (26, 54, 5.4, 8.5, 5.4, "pants"), (48, 50, 6, 4.8, 6.6, "boot"),
]


def lava_suit():
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for i, (u, v, w, h, dd, role) in enumerate(LAVA_SUIT_BOXES):
        faces = box_faces(u, v, w, h, dd)
        rect = lambda r: (math.floor(r[0]), math.floor(r[1]), math.ceil(r[2]) - 1, math.ceil(r[3]) - 1)  # noqa: E731
        if role == "visor":  # red glass: translucent, brighter rim, one highlight streak
            for name, r in faces.items():
                x0, y0, x1, y1 = rect(r)
                for y in range(y0, y1 + 1):
                    for x in range(x0, x1 + 1):
                        img.putpixel((x, y), (255, 60, 40, 150))
                d.rectangle((x0, y0, x1, y1), outline=(255, 120, 90, 210))
            x0, y0, x1, y1 = rect(faces["north"])
            d.line((x0 + 2, y0 + 1, x0 + 5, y0 + 4), fill=(255, 220, 200, 230))
            continue
        base = {"crown": ORANGE, "helmet_back": ORANGE, "ear": BLACKP, "antenna": STEEL, "collar": BLACKP,
                "torso": ORANGE, "console": BLACKP, "belt": BLACKP, "pack": STEEL, "tank": hexrgb("#d9d9d9"),
                "fin": ORANGE, "sleeve": ORANGE, "shoulder": ORANGE_DK, "pants": ORANGE, "boot": BLACKP}[role]
        for name, r in faces.items():
            noisy(img, (math.floor(r[0]), math.floor(r[1]), math.ceil(r[2]), math.ceil(r[3])), base, 9, i * 7 + len(name))
        if role in ("torso", "sleeve", "pants"):  # panel lines + red trims + hazard stripes
            for name in ("north", "south", "west", "east"):
                x0, y0, x1, y1 = rect(faces[name])
                d.line((x0, y0, x1, y0), fill=RED)
                d.line((x0, y1, x1, y1), fill=RED)
                if role == "sleeve":
                    for x in range(x0, x1 + 1):  # hazard band
                        img.putpixel((x, y0 + 7), hexrgb("#ffd21f") if (x // 2) % 2 == 0 else BLACKP)
                if role == "pants":
                    d.line((x0, y0 + 4, x1, y0 + 4), fill=ORANGE_DK)
        if role == "torso":
            x0, y0, x1, y1 = rect(faces["north"])
            for k in range(3):  # shark gills
                d.line((x0 + 1, y0 + 4 + k * 2, x0 + 3, y0 + 3 + k * 2), fill=BLACKP)
                d.line((x1 - 3, y0 + 3 + k * 2, x1 - 1, y0 + 4 + k * 2), fill=BLACKP)
            d.rectangle((x0 + 3, y1 - 4, x1 - 3, y1 - 3), fill=RED)
        if role == "console":
            x0, y0, x1, y1 = rect(faces["north"])
            d.point((x0 + 1, y0 + 1), fill=hexrgb("#ff2a2a"))
            d.point((x0 + 3, y0 + 1), fill=hexrgb("#39ff6a"))
            d.line((x0 + 1, y0 + 3, x1 - 1, y0 + 3), fill=hexrgb("#ffb02a"))
        if role == "belt":
            x0, y0, x1, y1 = rect(faces["north"])
            d.rectangle((x0 + 4, y0, x0 + 6, y1), fill=hexrgb("#ffd21f"))
        if role == "tank":
            for name in ("north", "south", "west", "east"):
                x0, y0, x1, y1 = rect(faces[name])
                d.line((x0, y0 + 2, x1, y0 + 2), fill=RED)
                d.line((x0, y0 + 3, x1, y0 + 3), fill=RED)
        if role == "crown":  # shark teeth along the brow
            x0, y0, x1, y1 = rect(faces["north"])
            for x in range(x0, x1 + 1, 2):
                img.putpixel((x, y1), (245, 245, 240, 255))
        if role == "boot":
            for name in ("north", "south", "west", "east"):
                x0, y0, x1, y1 = rect(faces[name])
                d.line((x0, y1, x1, y1), fill=ORANGE)
    save(img, "textures/models/armor/lava_suit.png")


# ================================================================================================= small FX / blocks / items
def radial(size, color, power=2.0, core=0.0):
    yy, xx = np.mgrid[0:size, 0:size]
    r = np.sqrt((xx - size / 2 + 0.5) ** 2 + (yy - size / 2 + 0.5) ** 2) / (size / 2)
    a = np.clip(1 - r, 0, 1) ** power
    a = np.maximum(a, (r < core) * 1.0)
    rgb = np.array(color[:3], float)[None, None] / 255 * a[..., None]
    rgb = np.clip(rgb + (np.clip(1 - r / 0.25, 0, 1) ** 2)[..., None] * 0.9, 0, 1)
    return Image.fromarray((np.dstack([rgb, a]) * 255).astype(np.uint8), "RGBA")


def fx():
    # freeze bolt: snowflake orb
    s = 64
    img = radial(s, (120, 220, 255), 1.6)
    d = ImageDraw.Draw(img)
    for k in range(6):
        a = k * math.pi / 3
        d.line((32, 32, 32 + math.cos(a) * 26, 32 + math.sin(a) * 26), fill=(230, 250, 255, 255), width=2)
        for t in (0.55, 0.8):
            px, py = 32 + math.cos(a) * 26 * t, 32 + math.sin(a) * 26 * t
            for sgn in (-1, 1):
                b = a + sgn * 0.7
                d.line((px, py, px + math.cos(b) * 6, py + math.sin(b) * 6), fill=(230, 250, 255, 255), width=1)
    save(img, "textures/entity/freeze_bolt.png")
    save(radial(128, (190, 80, 255), 2.2), "textures/entity/core_halo.png")
    # neon question mark (hologram)
    import gen_textures as gt
    q = gt.neon_lines((256, 256), ["?"], (255, 255, 255), font_path=gt.FONT_BLACK, pad=0.08)
    save(q, "textures/block/neon/question.png")


DYES = {"magenta": "#ff2bd6", "pink": "#ff6fb5", "purple": "#a64dff", "light_blue": "#5ac8ff", "cyan": "#2ff3ff",
        "lime": "#5dff3a", "yellow": "#ffe23a", "orange": "#ff8a1f", "red": "#ff2a2a", "white": "#f4f4ff"}


def neon_blocks():
    for name, col in DYES.items():
        c = np.array(hexrgb(col)[:3], float)
        img = np.zeros((16, 16, 4))
        yy, xx = np.mgrid[0:16, 0:16]
        edge = np.minimum(np.minimum(xx, 15 - xx), np.minimum(yy, 15 - yy))
        glow = 0.55 + 0.45 * np.clip(edge / 5, 0, 1)
        img[..., :3] = np.clip(c[None, None] * glow[..., None] + 255 * np.clip(edge - 4, 0, 3)[..., None] / 3 * 0.35, 0, 255)
        img[..., 3] = 255
        frame = edge == 0
        img[frame, :3] = c * 0.45
        save(Image.fromarray(img.astype(np.uint8), "RGBA"), "textures/block/neon_block_%s.png" % name)
        # strip: white-hot centre line, coloured tube
        st = np.zeros((16, 16, 4))
        st[..., :3] = c
        st[..., 3] = 235
        st[6:10, :, :3] = np.clip(c * 0.4 + 160, 0, 255)
        save(Image.fromarray(st.astype(np.uint8), "RGBA"), "textures/block/neon_strip_%s.png" % name)


def crates():
    # mystery box: dark metal crate with a neon "?" panel, 32x32
    side = Image.new("RGBA", (32, 32), hexrgb("#141218"))
    noisy(side, (0, 0, 32, 32), hexrgb("#17141d"), 6, 30)
    d = ImageDraw.Draw(side)
    d.rectangle((0, 0, 31, 31), outline=hexrgb("#d4a52c"))
    d.rectangle((1, 1, 30, 30), outline=hexrgb("#6b5216"))
    for p in ((2, 2), (29, 2), (2, 29), (29, 29)):
        d.point(p, fill=hexrgb("#ffe38a"))
    import gen_textures as gt
    q = gt.neon_lines((64, 64), ["?"], (255, 43, 214), font_path=gt.FONT_BLACK, pad=0.12).resize((24, 24), Image.LANCZOS)
    side.alpha_composite(q, (4, 4))
    save(side, "textures/block/mystery_box_side.png")
    top = Image.new("RGBA", (32, 32), hexrgb("#17141d"))
    noisy(top, (0, 0, 32, 32), hexrgb("#17141d"), 6, 31)
    d = ImageDraw.Draw(top)
    d.rectangle((0, 0, 31, 31), outline=hexrgb("#d4a52c"))
    d.rectangle((6, 6, 25, 25), outline=hexrgb("#2ff3ff"))
    d.line((6, 6, 25, 25), fill=hexrgb("#145e66"))
    d.line((25, 6, 6, 25), fill=hexrgb("#145e66"))
    save(top, "textures/block/mystery_box_top.png")
    bottom = Image.new("RGBA", (32, 32), hexrgb("#0e0c12"))
    noisy(bottom, (0, 0, 32, 32), hexrgb("#0e0c12"), 4, 32)
    save(bottom, "textures/block/mystery_box_bottom.png")
    # laser emitter 16x16
    le = Image.new("RGBA", (16, 16), hexrgb("#2b2b31"))
    noisy(le, (0, 0, 16, 16), hexrgb("#2b2b31"), 7, 33)
    d = ImageDraw.Draw(le)
    d.rectangle((0, 0, 15, 15), outline=hexrgb("#121214"))
    d.rectangle((5, 5, 10, 10), fill=hexrgb("#550000"))
    d.rectangle((6, 6, 9, 9), fill=hexrgb("#ff2a2a"))
    d.point((7, 7), fill=hexrgb("#ffd0d0"))
    for x in (2, 13):
        for y in (2, 13):
            d.point((x, y), fill=hexrgb("#8f949c"))
    save(le, "textures/block/laser_emitter.png")


def icon_apple():
    # 16x16 Crazy Apple: golden apple silhouette, dark purple-black skin, glowing magenta veins
    shape = ["................", ".......##.......", "......#.#.......", "....####.##.....", "...##########...",
             "..############..", "..############..", ".##############.", ".##############.", ".##############.",
             ".##############.", "..############..", "..############..", "...##########...", "....###..###....",
             "................"]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rnd = random.Random(4)
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch == "#":
                shade = 0.6 + 0.4 * (1 - y / 15) - 0.25 * (x / 15)
                img.putpixel((x, y), (int(45 * shade), int(10 * shade), int(70 * shade), 255))
    for (x, y) in [(5, 6), (6, 7), (7, 8), (7, 9), (8, 10), (9, 9), (10, 8), (11, 7), (4, 9), (5, 10), (10, 11), (9, 12)]:
        img.putpixel((x, y), (255, 60, 230, 255))
    for (x, y) in [(4, 5), (5, 5), (4, 6)]:
        img.putpixel((x, y), (190, 140, 255, 255))   # shine
    for (x, y) in [(7, 1), (7, 2), (8, 2)]:
        img.putpixel((x, y), (60, 30, 20, 255))      # stem
    for (x, y) in [(9, 2), (10, 2), (10, 3)]:
        img.putpixel((x, y), (120, 20, 160, 255))    # dark leaf
    save(img, "textures/item/crazy_apple.png")


def icon_pixels(name, rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                img.putpixel((x, y), palette[ch])
    save(img, "textures/item/%s.png" % name)


def icons():
    icon_apple()
    icon_pixels("smoke_bomb", [
        "................", "........ff......", ".......f..f.....", "......k.........", ".....kkkk.......", "....kggggk......",
        "...kgggggg k....", "..kgggggggggk...", "..kgwggggggggk..", "..kggggggggggk..", "..kgggggggggk...",
        "...kggggggggk...", "....kkggggkk....", "......kkkk......", "................", "................"],
        {"k": hexrgb("#101014"), "g": hexrgb("#3a3a44"), "w": hexrgb("#9a9aa8"), "f": hexrgb("#ff8a1f")})
    icon_pixels("hacker_usb", [
        "................", "......ssss......", "......s..s......", "......ssss......", ".....kkkkkk.....",
        ".....kggggk.....", ".....kgGGgk.....", ".....kgggg k....", ".....kgGGgk.....", ".....kggggk.....",
        ".....kgGGgk.....", ".....kggggk.....", ".....kkkkkk.....", "......kkkk......", "................", "................"],
        {"s": hexrgb("#c9ccd2"), "k": hexrgb("#0b0b0e"), "g": hexrgb("#1c3a22"), "G": hexrgb("#39ff6a")})
    o, r, k, g, t = hexrgb("#ff6a12"), hexrgb("#ff3a2a", 200), hexrgb("#1a1414"), hexrgb("#ffd21f"), hexrgb("#c2410a")
    pal = {"o": o, "r": r, "k": k, "g": g, "t": t, "w": hexrgb("#ffe0d0")}
    icon_pixels("lava_suit_helmet", ["................", "....oooooooo....", "...oooooooooo...", "..oorrrrrrrroo..",
                                     "..orrwrrrrrrro..", "..orwrrrrrrrro..", "..orrrrrrrrrro..", "..orrrrrrrrrro..",
                                     "..oorrrrrrrroo..", "...oooooooooo...", "..kkkkkkkkkkkk..", "..kkkkkkkkkkkk..",
                                     "................", "................", "................", "................"], pal)
    icon_pixels("lava_suit_chestplate", ["................", "..ttt......ttt..", ".ooooo....ooooo.", ".oooooooooooooo.",
                                         ".ookoooooooooko.", ".ooko kkkk oko..", ".oo.okgkkgk.oo..", ".oo.oooooooo.oo.",
                                         ".oo.ogogogog.oo.", "....oooooooo....", "....okkkkkko....", "....oooooooo....",
                                         "....oooooooo....", "................", "................", "................"], pal)
    icon_pixels("lava_suit_leggings", ["................", "...kkkkkkkkkk...", "...okkkgkkkko...", "...oooooooooo...",
                                       "...oooo..oooo...", "...ooto..otoo...", "...oooo..oooo...", "...oooo..oooo...",
                                       "...ooto..otoo...", "...oooo..oooo...", "...oooo..oooo...", "...oooo..oooo...",
                                       "................", "................", "................", "................"], pal)
    icon_pixels("lava_suit_boots", ["................", "................", "................", "................",
                                    "................", "...oooo..oooo...", "...oooo..oooo...", "...ooto..otoo...",
                                    "...oooo..oooo...", "..kkkkk..kkkkk..", "..kkkkko.kkkkko.", "..oooooo.oooooo.",
                                    "................", "................", "................", "................"], pal)
    # neon sign item: mini neon "N" in a frame
    import gen_textures as gt
    ns = gt.neon_lines((64, 64), ["N"], (255, 43, 214), font_path=gt.FONT_BLACK, pad=0.1).resize((16, 16), Image.LANCZOS)
    save(ns, "textures/item/neon_sign.png")
    # grappling hook gun texture (used by the 3D JSON model: 32x32 regions)
    gun = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    noisy(gun, (0, 0, 16, 8), hexrgb("#2b2b31"), 6, 40)    # body / rail (gunmetal)
    noisy(gun, (16, 0, 32, 8), hexrgb("#5a3a22"), 8, 41)   # stock / grip (dark wood)
    noisy(gun, (0, 8, 16, 16), hexrgb("#c9ccd2"), 6, 42)   # steel (limbs / hook)
    noisy(gun, (16, 8, 32, 16), hexrgb("#d4a52c"), 6, 43)  # brass spool
    noisy(gun, (0, 16, 16, 24), hexrgb("#1a1a1e"), 3, 44)  # rope wound (dark)
    for y in range(16, 24, 2):
        ImageDraw.Draw(gun).line((0, y, 15, y), fill=hexrgb("#3a3a40"))
    noisy(gun, (16, 16, 24, 24), hexrgb("#ff2a2a"), 4, 45) # red laser sight
    noisy(gun, (24, 16, 32, 24), hexrgb("#101012"), 3, 46) # black rubber
    save(gun, "textures/item/grappling_hook_gun.png")


def mod_icon():
    import gen_textures as gt
    img = Image.new("RGBA", (128, 128), (8, 6, 14, 255))
    q = gt.neon_lines((128, 128), ["?"], (255, 43, 214), font_path=gt.FONT_BLACK, pad=0.1)
    img.alpha_composite(q)
    save(img, "icon.png")


def main():
    vip_guard()
    vip_accessories()
    lava_suit()
    fx()
    neon_blocks()
    crates()
    icons()
    mod_icon()


if __name__ == "__main__":
    import sys
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    main()
    print("entity/item/block textures written")
