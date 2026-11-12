#!/usr/bin/env python3
"""
gen_assets.py - writes every JSON resource of the mod (blockstates, block/item models, lang, loot tables) and the
Blockbench project files (blockbench/*.bbmodel) for the models designed from scratch.

The block/item JSON models with "elements" ARE Blockbench "Java Block/Item" models: open them directly in Blockbench
(File > Open Model) to edit, then File > Export > Java Block/Item over the same file.
"""
import json
import os
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, "src/main/resources/assets/limitlessheist")
D = os.path.join(ROOT, "src/main/resources/data/limitlessheist")
M = "limitlessheist"
COLORS = ["magenta", "pink", "purple", "light_blue", "cyan", "lime", "yellow", "orange", "red", "white"]


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def bs(name, obj):
    w(os.path.join(A, "blockstates", name + ".json"), obj)


def bm(name, obj):
    w(os.path.join(A, "models/block", name + ".json"), obj)


def im(name, obj):
    w(os.path.join(A, "models/item", name + ".json"), obj)


HORIZ = {"north": 0, "east": 90, "south": 180, "west": 270}

# ------------------------------------------------------------------------------------------------ display transforms
BLOCKISH = {  # mesh props as items (front faces the viewer after the renderer's 180 turn)
    "gui": {"rotation": [25, -30, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}
GUN = {  # mesh guns: barrel along -Z in the model = forward in first person
    "gui": {"rotation": [15, -90, 0], "translation": [0, 0, 0], "scale": [1.1, 1.1, 1.1]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
    "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3.5, -2], "scale": [0.8, 0.8, 0.8]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3.5, -2], "scale": [0.8, 0.8, 0.8]},
    "firstperson_righthand": {"rotation": [0, 0, 0], "translation": [1, 3, -4], "scale": [0.9, 0.9, 0.9]},
    "firstperson_lefthand": {"rotation": [0, 0, 0], "translation": [1, 3, -4], "scale": [0.9, 0.9, 0.9]},
}
CORE = {
    "gui": {"rotation": [20, 30, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 2], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 2], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
}


def builtin(name, display):
    im(name, {"parent": "builtin/entity", "gui_light": "front", "display": display})


def generated(name, tex=None):
    im(name, {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/%s" % (M, tex or name)}})


# ------------------------------------------------------------------------------------------------ Blockbench-style models
def face(tex, uv, cull=None):
    f = {"uv": uv, "texture": "#" + tex}
    if cull:
        f["cullface"] = cull
    return f


def cube(frm, to, tex, uv_full=True, rotation=None, name=None, uvs=None):
    faces = {}
    for d in ("north", "east", "south", "west", "up", "down"):
        faces[d] = {"uv": (uvs or {}).get(d, [0, 0, 16, 16]) if uv_full else [0, 0, 16, 16], "texture": "#" + (tex[d] if isinstance(tex, dict) else tex)}
    e = {"from": frm, "to": to, "faces": faces}
    if name:
        e["name"] = name
    if rotation:
        e["rotation"] = rotation
    return e


def mystery_box_model():
    tex = {"north": "side", "south": "side", "east": "side", "west": "side", "up": "top", "down": "bottom"}
    els = [cube([1, 0, 1], [15, 13, 15], tex, name="crate")]
    for x in (0.5, 13.5):  # gold corner posts
        for z in (0.5, 13.5):
            els.append(cube([x, 0, z], [x + 2, 14, z + 2], "gold", name="post"))
    els.append(cube([0.5, 12, 0.5], [15.5, 14, 15.5], {"north": "gold", "south": "gold", "east": "gold", "west": "gold", "up": "top", "down": "gold"}, name="lid band"))
    bm("mystery_box", {"credit": "LimitlessAnant Heist - made with Blockbench conventions", "parent": "minecraft:block/block",
                       "textures": {"side": M + ":block/mystery_box_side", "top": M + ":block/mystery_box_top",
                                    "bottom": M + ":block/mystery_box_bottom", "gold": "minecraft:block/gold_block",
                                    "particle": M + ":block/mystery_box_side"}, "elements": els})
    bs("mystery_box", {"variants": {"facing=%s" % f: {"model": M + ":block/mystery_box", "y": r} for f, r in HORIZ.items()}})
    im("mystery_box", {"parent": M + ":block/mystery_box"})


def laser_emitter_model():
    # modelled pointing UP; blockstate rotates it for the 6 facings
    els = [
        cube([4, 0, 4], [12, 2, 12], "body", name="base plate"),
        cube([5.5, 2, 5.5], [10.5, 4, 10.5], "body", name="housing"),
        cube([6.5, 4, 6.5], [9.5, 4.6, 9.5], {"north": "lens", "south": "lens", "east": "lens", "west": "lens", "up": "lens", "down": "body"}, name="lens"),
    ]
    bm("laser_emitter", {"parent": "minecraft:block/block", "textures": {"body": M + ":block/laser_emitter", "lens": "minecraft:block/redstone_block",
                                                                         "particle": M + ":block/laser_emitter"}, "elements": els})
    rot = {"up": (0, 0), "down": (180, 0), "north": (90, 0), "south": (90, 180), "west": (90, 270), "east": (90, 90)}
    bs("laser_emitter", {"variants": {"facing=%s" % f: dict({"model": M + ":block/laser_emitter"}, **({"x": x} if x else {}), **({"y": y} if y else {}))
                                      for f, (x, y) in rot.items()}})
    im("laser_emitter", {"parent": M + ":block/laser_emitter"})


def grappling_gun_model():
    """A crossbow rebuilt as a grappling hook launcher (Blockbench Java Item model, 32x32 texture)."""
    T = {"textures": {"0": M + ":item/grappling_hook_gun", "particle": M + ":item/grappling_hook_gun"}}

    def el(name, frm, to, uv, rot=None):
        # uv = (u,v) top-left of a 4x4 cell on the 32x32 sheet (in 16-unit model UV space: /2)
        u, v = uv
        fa = {d: {"uv": [u / 2, v / 2, u / 2 + 2, v / 2 + 2], "texture": "#0"} for d in ("north", "east", "south", "west", "up", "down")}
        e = {"name": name, "from": frm, "to": to, "faces": fa}
        if rot:
            e["rotation"] = rot
        return e

    gunmetal, wood, steel, brass, rope, red, rubber = (0, 0), (16, 0), (0, 8), (16, 8), (0, 16), (16, 16), (24, 16)
    body = [
        el("stock", [7, 5, 10], [9, 8, 16], wood),
        el("grip", [7.25, 1.5, 10.5], [8.75, 6, 12.5], rubber, {"angle": -22.5, "axis": "x", "origin": [8, 4, 11.5]}),
        el("rail", [6.5, 7, 2], [9.5, 9.5, 11], gunmetal),
        el("limb_left", [1, 8.2, 4.5], [8, 9.2, 6], steel, {"angle": -22.5, "axis": "y", "origin": [8, 8.5, 5]}),
        el("limb_right", [8, 8.2, 4.5], [15, 9.2, 6], steel, {"angle": 22.5, "axis": "y", "origin": [8, 8.5, 5]}),
        el("cable", [2.5, 8.45, 7.5], [13.5, 8.95, 8], rope),
        el("spool", [6, 4, 4.5], [10, 7, 8], brass),
        el("spool_rope", [5.8, 4.6, 5], [10.2, 6.4, 7.5], rope),
        el("laser_sight", [9.5, 9.5, 2.5], [10.5, 10.5, 6], red),
        el("trigger", [7.6, 5.2, 9.2], [8.4, 6.6, 9.8], steel),
    ]
    hook = [
        el("hook_shaft", [7.5, 7.9, -3], [8.5, 8.9, 2.5], steel),
        el("hook_prong_up", [7.5, 8.9, -3.5], [8.5, 11, -2.5], steel, {"angle": 22.5, "axis": "x", "origin": [8, 8.9, -3]}),
        el("hook_prong_left", [5.5, 7.9, -3.5], [7.5, 8.9, -2.5], steel, {"angle": -22.5, "axis": "y", "origin": [7.5, 8.4, -3]}),
        el("hook_prong_right", [8.5, 7.9, -3.5], [10.5, 8.9, -2.5], steel, {"angle": 22.5, "axis": "y", "origin": [8.5, 8.4, -3]}),
        el("hook_tip", [7.25, 7.65, -4.5], [8.75, 9.15, -3], brass),
    ]
    disp = {
        "thirdperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 3, 1], "scale": [0.85, 0.85, 0.85]},
        "thirdperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 3, 1], "scale": [0.85, 0.85, 0.85]},
        "firstperson_righthand": {"rotation": [0, 180, 0], "translation": [2, 2, -1], "scale": [0.9, 0.9, 0.9]},
        "firstperson_lefthand": {"rotation": [0, 180, 0], "translation": [2, 2, -1], "scale": [0.9, 0.9, 0.9]},
        "gui": {"rotation": [20, 115, 0], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    }
    im("grappling_hook_gun_fired", dict(T, **{"credit": "LimitlessAnant Heist", "elements": body, "display": disp}))
    im("grappling_hook_head", dict(T, **{"elements": hook, "display": {"ground": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}}}))
    im("grappling_hook_gun", dict(T, **{"credit": "LimitlessAnant Heist - crossbow turned grappling gun", "elements": body + hook, "display": disp,
                                        "overrides": [{"predicate": {M + ":state": 0.5}, "model": M + ":item/grappling_hook_gun_fired"},
                                                      {"predicate": {M + ":state": 1.0}, "model": M + ":item/grappling_hook_head"}]}))
    return body + hook


def neon_models():
    for c in COLORS:
        bm("neon_block_" + c, {"parent": "minecraft:block/cube_all", "textures": {"all": "%s:block/neon_block_%s" % (M, c)}})
        bs("neon_block_" + c, {"variants": {"": {"model": "%s:block/neon_block_%s" % (M, c)}}})
        im("neon_block_" + c, {"parent": "%s:block/neon_block_%s" % (M, c)})
        tex = "%s:block/neon_strip_%s" % (M, c)
        bm("neon_strip_" + c, {"parent": "minecraft:block/block", "ambientocclusion": False,
                               "textures": {"t": tex, "particle": tex},
                               "elements": [cube([6.5, 0, 6.5], [9.5, 16, 9.5], "t")]})
        bs("neon_strip_" + c, {"variants": {"axis=y": {"model": "%s:block/neon_strip_%s" % (M, c)},
                                            "axis=x": {"model": "%s:block/neon_strip_%s" % (M, c), "x": 90, "y": 90},
                                            "axis=z": {"model": "%s:block/neon_strip_%s" % (M, c), "x": 90}}})
        im("neon_strip_" + c, {"parent": "%s:block/neon_strip_%s" % (M, c)})


def mesh_blocks():
    particles = {"hacker_terminal": "minecraft:block/black_concrete", "briefcase": "minecraft:block/black_wool",
                 "mona_lisa": "minecraft:block/spruce_planks", "weapon_rack": "minecraft:block/netherite_block",
                 "neon_sign": M + ":block/neon_block_magenta", "infinity_dragon_core": "minecraft:block/amethyst_block"}
    for b, p in particles.items():
        bm(b, {"textures": {"particle": p}})
    for b in ("hacker_terminal", "briefcase", "mona_lisa", "weapon_rack"):
        bs(b, {"variants": {"facing=%s" % f: {"model": "%s:block/%s" % (M, b)} for f in HORIZ}})
    bs("neon_sign", {"variants": {"": {"model": M + ":block/neon_sign"}}})
    bs("infinity_dragon_core", {"variants": {"": {"model": M + ":block/infinity_dragon_core"}}})
    for b in ("hacker_terminal", "briefcase", "mona_lisa", "weapon_rack"):
        builtin(b, BLOCKISH)
    builtin("infinity_dragon_core", CORE)
    builtin("freeze_ray_gun", GUN)
    builtin("sci_fi_assault_rifle", GUN)
    generated("neon_sign")


def items():
    for i in ("crazy_apple", "smoke_bomb", "hacker_usb", "lava_suit_helmet", "lava_suit_chestplate", "lava_suit_leggings", "lava_suit_boots"):
        generated(i)
    for e in ("vip_guard", "unicorn", "gotiya"):
        im(e + "_spawn_egg", {"parent": "minecraft:item/template_spawn_egg"})


# ------------------------------------------------------------------------------------------------ data
def loot():
    def self_drop(b):
        w(os.path.join(D, "loot_tables/blocks", b + ".json"), {"type": "minecraft:block", "pools": [{
            "rolls": 1, "entries": [{"type": "minecraft:item", "name": "%s:%s" % (M, b)}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    for b in ["hacker_terminal", "briefcase", "mona_lisa", "weapon_rack", "neon_sign", "infinity_dragon_core", "laser_emitter"] \
            + ["neon_block_" + c for c in COLORS] + ["neon_strip_" + c for c in COLORS]:
        self_drop(b)

    def it(name, weight, lo=1, hi=1):
        e = {"type": "minecraft:item", "name": name, "weight": weight}
        if hi > 1:
            e["functions"] = [{"function": "minecraft:set_count", "count": {"min": lo, "max": hi}}]
        return e

    # Thief Mystery Box: one random heist gadget (edit the weights to tune the drops)
    w(os.path.join(D, "loot_tables/blocks/mystery_box.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [
            it(M + ":grappling_hook_gun", 12), it(M + ":freeze_ray_gun", 8), it(M + ":crazy_apple", 10),
            it(M + ":hacker_usb", 14, 1, 3), it(M + ":smoke_bomb", 14, 2, 4),
            it(M + ":lava_suit_helmet", 3), it(M + ":lava_suit_chestplate", 3), it(M + ":lava_suit_leggings", 3), it(M + ":lava_suit_boots", 3),
            it(M + ":sci_fi_assault_rifle", 4), it("minecraft:ender_pearl", 8, 2, 4), it("minecraft:diamond", 6, 1, 3),
            it("minecraft:enchanted_golden_apple", 2)]}]})


def lang():
    L = {
        "itemGroup.limitlessheist.heist": "LimitlessAnant's Black Market Heist",
        "block.limitlessheist.hacker_terminal": "Hacker Computer Terminal Setup",
        "block.limitlessheist.briefcase": "Dealer's Briefcase",
        "block.limitlessheist.mona_lisa": "Mona Lisa",
        "block.limitlessheist.weapon_rack": "Weapon Rack",
        "block.limitlessheist.neon_sign": "Neon Cyberpunk Sign",
        "block.limitlessheist.mystery_box": "Thief Mystery Box",
        "block.limitlessheist.infinity_dragon_core": "The Infinity Dragon Core",
        "block.limitlessheist.laser_emitter": "Laser Grid Emitter",
        "item.limitlessheist.sci_fi_assault_rifle": "Sci-Fi Assault Rifle",
        "item.limitlessheist.freeze_ray_gun": "Freeze Ray Gun",
        "item.limitlessheist.grappling_hook_gun": "Grappling Hook Gun",
        "item.limitlessheist.hacker_usb": "Hacker USB (EMP)",
        "item.limitlessheist.smoke_bomb": "Smoke Bomb",
        "item.limitlessheist.crazy_apple": "Crazy Apple",
        "item.limitlessheist.lava_suit_helmet": "Lava Proof Suit Helmet",
        "item.limitlessheist.lava_suit_chestplate": "Lava Proof Suit Chestplate",
        "item.limitlessheist.lava_suit_leggings": "Lava Proof Suit Leggings",
        "item.limitlessheist.lava_suit_boots": "Lava Proof Suit Boots",
        "item.limitlessheist.vip_guard_spawn_egg": "VIP Bodyguard Spawn Egg",
        "item.limitlessheist.unicorn_spawn_egg": "Legendary Unicorn Spawn Egg",
        "item.limitlessheist.gotiya_spawn_egg": "Gotiya Spawn Egg",
        "entity.limitlessheist.vip_guard": "VIP Bodyguard",
        "entity.limitlessheist.unicorn": "Legendary Unicorn",
        "entity.limitlessheist.gotiya": "Gotiya",
        "entity.limitlessheist.freeze_bolt": "Freeze Bolt",
        "entity.limitlessheist.grapple_hook": "Grappling Hook",
        "effect.limitlessheist.frozen": "Frozen",
        "effect.limitlessheist.wanted": "WANTED",
    }
    for c in COLORS:
        nice = c.replace("_", " ").title()
        L["block.limitlessheist.neon_block_" + c] = "%s Neon Block" % nice
        L["block.limitlessheist.neon_strip_" + c] = "%s Neon Tube" % nice
    w(os.path.join(A, "lang/en_us.json"), L)


# ------------------------------------------------------------------------------------------------ .bbmodel (Blockbench projects)
def bb_cube(name, frm, to, uv=(0, 0), origin=(0, 0, 0), rotation=(0, 0, 0), inflate=0.0, mirror=False):
    return {"name": name, "box_uv": True, "rescale": False, "locked": False, "from": list(frm), "to": list(to), "autouv": 0,
            "color": 0, "origin": list(origin), "rotation": list(rotation), "uv_offset": list(uv), "inflate": inflate,
            "mirror_uv": mirror, "faces": {d: {"uv": [0, 0, 0, 0], "texture": 0} for d in ("north", "east", "south", "west", "up", "down")},
            "type": "cube", "uuid": str(uuid.uuid4())}


def bbmodel(path, name, cubes_by_group, tex_path, tw, th, fmt="modded_entity"):
    """Writes a Blockbench project. Entity cubes are given in model pixels (Blockbench's modded entity space: y up,
    so ModelPart y (down) is negated)."""
    elements, outliner = [], []
    for gname, (pivot, cubes) in cubes_by_group.items():
        uuids = []
        for c in cubes:
            elements.append(c)
            uuids.append(c["uuid"])
        outliner.append({"name": gname, "origin": list(pivot), "rotation": [0, 0, 0], "uuid": str(uuid.uuid4()),
                         "export": True, "isOpen": True, "visibility": True, "children": uuids})
    tex = {"path": tex_path, "name": os.path.basename(tex_path), "folder": "", "namespace": M, "id": "0",
           "particle": False, "render_mode": "default", "visible": True, "mode": "link", "saved": True,
           "uuid": str(uuid.uuid4()), "relative_path": os.path.relpath(tex_path, os.path.dirname(path))}
    proj = {"meta": {"format_version": "4.5", "model_format": fmt, "box_uv": True}, "name": name,
            "model_identifier": name, "visible_box": [1, 1, 0], "resolution": {"width": tw, "height": th},
            "elements": elements, "outliner": outliner, "textures": [tex]}
    w(path, proj)


def mc_to_bb(x, y, z, sx, sy, sz, pivot):
    """ModelPart cuboid (relative to part pivot, y down) -> Blockbench absolute from/to (y up, 24 - y)."""
    px, py, pz = pivot
    x0, y0, z0 = px + x, py + y, pz + z
    return (x0, 24 - (y0 + sy), z0), (x0 + sx, 24 - y0, z0 + sz)


def blockbench_projects():
    bb = os.path.join(ROOT, "blockbench")
    # VIP accessories (attached to the iron golem head pivot (0,-7,-2) and body pivot (0,-7,0))
    glasses = [((-4.5, -9.0, -6.4, 4.0, 2.5, 0.8), (0, 0)), ((0.5, -9.0, -6.4, 4.0, 2.5, 0.8), (0, 4)), ((-0.5, -8.8, -6.3, 1.0, 0.8, 0.6), (10, 0)),
               ((-4.8, -9.2, -6.5, 9.6, 0.6, 1.0), (0, 8)), ((-4.7, -8.9, -5.9, 0.5, 0.6, 5.0), (0, 10)), ((4.2, -8.9, -5.9, 0.5, 0.6, 5.0), (0, 10))]
    tie = [((-1.0, -2.0, -6.8, 2.0, 1.6, 0.9), (0, 16)), ((-1.25, -0.4, -6.6, 2.5, 5.0, 0.6), (6, 16)), ((-0.85, 4.6, -6.6, 1.7, 2.4, 0.6), (12, 16)),
           ((-1.4, -2.2, -6.75, 2.8, 0.4, 0.3), (16, 16))]
    ear = [((-4.6, -7.5, -2.5, 0.8, 1.0, 1.0), (12, 10)), ((-4.4, -6.5, -2.2, 0.4, 5.0, 0.4), (16, 10))]
    head_pivot, body_pivot = (0, -7, -2), (0, -7, 0)
    groups = {}
    for gname, cubes, pv in (("glasses", glasses, head_pivot), ("earpiece", ear, head_pivot), ("tie", tie, body_pivot)):
        cs = []
        for (x, y, z, sx, sy, sz), uv in cubes:
            f, t = mc_to_bb(x, y, z, sx, sy, sz, pv)
            cs.append(bb_cube(gname, f, t, uv))
        groups[gname] = ((pv[0], 24 - pv[1], pv[2]), cs)
    bbmodel(os.path.join(bb, "vip_accessories.bbmodel"), "vip_accessories", groups,
            os.path.join(A, "textures/entity/vip_accessories.png"), 32, 32)
    # Lava suit (biped pivots)
    import gen_textures_entities as ge
    piv = {"head": (0, 0, 0), "body": (0, 0, 0), "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0), "right_leg": (-1.9, 12, 0), "left_leg": (1.9, 12, 0)}
    parts = {
        "head": [(-5.5, -9.5, -5.5, 11, 2, 11, 0), (-5.5, -7.5, 3.0, 11, 7, 2.5, 1), (-6.2, -6.0, -2.0, 1.2, 4, 4, 2), (5.0, -6.0, -2.0, 1.2, 4, 4, 2),
                 (4.6, -13.0, 1.0, 0.8, 4, 0.8, 3), (-5.0, -7.6, -5.6, 10, 7.4, 8.8, 4), (-6.0, -0.6, -6.0, 12, 2, 12, 5)],
        "body": [(-5.0, 0.0, -3.2, 10, 12.5, 6.4, 6), (-3.0, 2.0, -4.0, 6, 5, 1, 7), (-5.2, 10.0, -3.4, 10.4, 2, 6.8, 8), (-4.0, 0.5, 3.2, 8, 10, 3, 9),
                 (-3.6, -1.0, 6.2, 3, 11, 3, 10), (0.6, -1.0, 6.2, 3, 11, 3, 10), (-0.5, -3.5, 5.5, 1, 6, 4, 11)],
        "right_arm": [(-4.0, -2.6, -2.7, 5.4, 12.5, 5.4, 12), (-4.6, -3.4, -3.2, 6.4, 4, 6.4, 13)],
        "left_arm": [(-1.4, -2.6, -2.7, 5.4, 12.5, 5.4, 12), (-1.8, -3.4, -3.2, 6.4, 4, 6.4, 13)],
        "right_leg": [(-2.7, -0.2, -2.7, 5.4, 8.5, 5.4, 14), (-3.0, 7.5, -3.6, 6, 4.8, 6.6, 15)],
        "left_leg": [(-2.7, -0.2, -2.7, 5.4, 8.5, 5.4, 14), (-3.0, 7.5, -3.6, 6, 4.8, 6.6, 15)],
    }
    groups = {}
    for gname, cubes in parts.items():
        pv = piv[gname]
        cs = []
        for (x, y, z, sx, sy, sz, bi) in cubes:
            u, v = ge.LAVA_SUIT_BOXES[bi][0], ge.LAVA_SUIT_BOXES[bi][1]
            f, t = mc_to_bb(x, y, z, sx, sy, sz, pv)
            cs.append(bb_cube(ge.LAVA_SUIT_BOXES[bi][5], f, t, (u, v), mirror=gname.startswith("left")))
        groups[gname] = ((pv[0], 24 - pv[1], pv[2]), cs)
    bbmodel(os.path.join(bb, "lava_suit.bbmodel"), "lava_suit", groups, os.path.join(A, "textures/models/armor/lava_suit.png"), 128, 64)


def main():
    mystery_box_model()
    laser_emitter_model()
    grappling_gun_model()
    neon_models()
    mesh_blocks()
    items()
    loot()
    lang()
    blockbench_projects()


if __name__ == "__main__":
    import sys
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    main()
    print("JSON assets + Blockbench projects written")
