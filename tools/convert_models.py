#!/usr/bin/env python3
"""
convert_models.py - turns the Sketchfab glTF files in source_models/ into runtime meshes for the mod.

    python3 tools/gen_textures.py        # (first) paints the replacement / original textures
    python3 tools/convert_models.py      # converts every model
    python3 tools/convert_models.py unicorn briefcase   # only some

Output
    src/main/resources/assets/limitlessheist/meshes/<id>.hmesh            (gzip, read by HeistMesh.java)
    src/main/resources/assets/limitlessheist/textures/mesh/<id>/*.png
    blockbench/<id>/<id>.obj (+ .mtl + textures)  -> open in Blockbench (File > Import > OBJ) to edit.

Round trip with Blockbench:
    1. Import blockbench/<id>/<id>.obj in Blockbench (Generic Model), edit the mesh.
    2. File > Export > Export OBJ Model, save it as blockbench/<id>/<id>_edited.obj
    3. Re-run this script: if <id>_edited.obj exists it is used as the source instead of the glTF
       (positions are already in final Minecraft units, so no rescale is applied).
"""
import colorsys
import math
import os
import shutil
import sys

import numpy as np
from PIL import Image, ImageEnhance, ImageOps

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import meshlib as ml  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "source_models")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/limitlessheist")
GEN = os.path.join(ROOT, "tools/generated")  # textures painted by gen_textures.py
BB = os.path.join(ROOT, "blockbench")
MODID = "limitlessheist"


# ------------------------------------------------------------------------------------------------
# helpers
# ------------------------------------------------------------------------------------------------
def fit(prims, rot_y=0.0, height=None, width=None, length=None, scale=None, anchor="bottom", recenter_x=True):
    """Rotate around Y, scale to the requested size (Minecraft blocks) and move the origin.
    anchor: 'bottom' -> origin at bottom centre, 'center' -> bbox centre, 'back' -> bottom centre of the back (+Z) face
    Model convention: FRONT FACES -Z (north)."""
    r = ml.rot_y(rot_y)
    for p in prims:
        p.pos = p.pos @ r.T
        p.nrm = p.nrm @ r.T
    mn, mx = ml.bounds(prims)
    size = mx - mn
    if scale is None:
        if height:
            scale = height / size[1]
        elif width:
            scale = width / size[0]
        elif length:
            scale = length / size[2]
        else:
            scale = 1.0
    c = (mn + mx) / 2
    if anchor == "bottom":
        off = np.array([c[0] if recenter_x else 0, mn[1], c[2]])
    elif anchor == "center":
        off = c
    elif anchor == "back":
        off = np.array([c[0], mn[1], mx[2]])
    else:
        off = np.zeros(3)
    for p in prims:
        p.pos = (p.pos - off) * scale
    return scale


def tex_rel(model_id, name):
    return "%s:textures/mesh/%s/%s.png" % (MODID, model_id, name)


def save_tex(model_id, name, img, max_size=1024):
    img = img.convert("RGBA")
    if max(img.size) > max_size:
        f = max_size / max(img.size)
        img = img.resize((max(1, int(img.width * f)), max(1, int(img.height * f))), Image.LANCZOS)
    # Minecraft is happiest with power-of-two sizes
    def p2(v):
        return 1 << max(0, (v - 1).bit_length())
    w, h = p2(img.width), p2(img.height)
    if (w, h) != img.size:
        img = img.resize((w, h), Image.LANCZOS)
    d = os.path.join(ASSETS, "textures/mesh", model_id)
    os.makedirs(d, exist_ok=True)
    img.save(os.path.join(d, name + ".png"), optimize=True)
    return tex_rel(model_id, name)


WHITE = None


def white_tex(model_id):
    return save_tex(model_id, "white", Image.new("RGBA", (4, 4), (255, 255, 255, 255)))


def expand(p, mask=None):
    """Triangle soup arrays (pos, uv, nrm) for a Prim (optionally only triangles in mask)."""
    idx = p.idx if mask is None else p.idx[mask]
    flat = idx.reshape(-1)
    return p.pos[flat], p.uv[flat], p.nrm[flat]


def flip_v(uv):
    # glTF UV origin is top-left already (same as Minecraft) -> nothing to do. Kept for clarity.
    return uv


def emit(model_id, bones, groups, preview_tex):
    path = os.path.join(ASSETS, "meshes", model_id + ".hmesh")
    ml.write_hmesh(path, bones, groups)
    # OBJ for Blockbench / preview
    d = os.path.join(BB, model_id)
    os.makedirs(d, exist_ok=True)
    tex_files = []
    for gr, src in zip(groups, preview_tex):
        if src:
            fn = os.path.basename(src)
            shutil.copy(src, os.path.join(d, fn))
            tex_files.append(fn)
        else:
            tex_files.append(None)
    ml.write_obj(os.path.join(d, model_id + ".obj"), groups, tex_files)
    tris = sum(len(g.pos) for g in groups) // 3
    kb = os.path.getsize(path) // 1024
    print("  -> %s  %d groups, %d triangles, %d KB" % (os.path.relpath(path, ROOT), len(groups), tris, kb))


def tex_file(model_id, rid):
    return os.path.join(ASSETS, "textures/mesh", model_id, rid.split("/")[-1])


def simple_model(model_id, prims, flags_for=None, tex_override=None, max_tex=1024, budget=None, color_override=None):
    """Generic: one group per material, textures copied (optionally replaced by generated art)."""
    flags_for = flags_for or {}
    tex_override = tex_override or {}
    color_override = color_override or {}
    by_mat = {}
    for p in prims:
        by_mat.setdefault(p.material["name"], []).append(p)
    total = sum(p.tri_count() for p in prims)
    groups, previews = [], []
    for mname, ps in by_mat.items():
        mat = ps[0].material
        if budget and total > budget:
            ps = [ml.simplify(p, max(8, int(p.tri_count() * budget / total))) if p.tri_count() > 200 else p for p in ps]
        pos = np.concatenate([expand(p)[0] for p in ps])
        uv = np.concatenate([expand(p)[1] for p in ps])
        nrm = np.concatenate([expand(p)[2] for p in ps])
        src = tex_override.get(mname) or mat["texture"] or mat.get("emissive_texture")
        color = list(color_override.get(mname, mat["color"]))
        if src:
            img = Image.open(src) if isinstance(src, str) else src
            name = mname.replace(".", "_").replace(" ", "_").lower()
            rid = save_tex(model_id, name, img, max_tex)
        else:
            rid = white_tex(model_id)
        flags = flags_for.get(mname, 0)
        if mat.get("double_sided"):
            flags |= ml.FLAG_NOCULL
        if mat.get("alpha_mode") == "BLEND" or color[3] < 0.99:
            flags |= ml.FLAG_TRANSLUCENT
        groups.append(ml.Group(rid, flags, color, pos, uv, nrm))
        previews.append(tex_file(model_id, rid))
    return groups, previews


def edited_or_gltf(model_id, gltf):
    e = os.path.join(BB, model_id, model_id + "_edited.obj")
    if os.path.exists(e):
        print("  using Blockbench edit", os.path.relpath(e, ROOT))
        return ml.load_obj(e), True
    return ml.load_gltf(gltf), False


# ------------------------------------------------------------------------------------------------
# individual models
# ------------------------------------------------------------------------------------------------
def conv_briefcase():
    mid = "briefcase"
    prims, edited = edited_or_gltf(mid, os.path.join(SRC, "briefcase/scene.gltf"))
    if not edited:
        fit(prims, rot_y=90, width=0.9)
    # Black-market look: dark carbon leather, gold locks
    leather = Image.open(os.path.join(SRC, "briefcase/textures/Team3_Sanchez_BriefcaseLeather_diffuse.jpeg")).convert("RGB")
    leather = ImageOps.colorize(ImageOps.grayscale(leather), black=(8, 8, 10), white=(95, 92, 100))
    groups, prev = simple_model(mid, prims,
                                tex_override={"Team3_Sanchez_BriefcaseLeather": leather},
                                color_override={"Team3_Sanchez_BriefcaseLocks": [0.95, 0.75, 0.25, 1.0]},
                                budget=5000)
    emit(mid, [("root", -1, (0, 0, 0))], groups, prev)


def conv_mona_lisa():
    mid = "mona_lisa"
    prims, edited = edited_or_gltf(mid, os.path.join(SRC, "mona_lisa/scene.gltf"))
    if not edited:
        mn, mx = ml.bounds(prims)
        sz = mx - mn
        # painting faces the camera along the thinnest axis; make it face -Z (out of the wall)
        thin = int(np.argmin(sz))
        if thin == 0:
            fit(prims, rot_y=-90, height=2.6, anchor="back")
        else:
            fit(prims, rot_y=180, height=2.6, anchor="back")
    groups, prev = simple_model(mid, prims, max_tex=1024)
    emit(mid, [("root", -1, (0, 0, 0))], groups, prev)


def conv_freeze_ray():
    mid = "freeze_ray"
    prims, edited = edited_or_gltf(mid, os.path.join(SRC, "freeze_ray_gun/scene.gltf"))
    if not edited:
        mn, mx = ml.bounds(prims)
        sz = mx - mn
        # barrel along -Z (forward)
        fit(prims, rot_y=0 if sz[2] >= sz[0] else 90, length=1.0, anchor="center")
    groups, prev = simple_model(mid, prims, flags_for={"material_0": 0})
    # pixel-art 64x64 texture: keep it crisp (nearest) -> upscale with NEAREST so it's not blurred
    emit(mid, [("root", -1, (0, 0, 0))], groups, prev)


def conv_rifle():
    mid = "assault_rifle"
    prims, edited = edited_or_gltf(mid, os.path.join(SRC, "sci_fi_assault_rifle/scene.gltf"))
    if not edited:
        fit(prims, rot_y=0, length=1.25, anchor="center")
    groups, prev = simple_model(mid, prims, max_tex=1024, budget=6000)
    emit(mid, [("root", -1, (0, 0, 0))], groups, prev)


def conv_weapon_rack():
    mid = "weapon_rack"
    prims, edited = edited_or_gltf(mid, os.path.join(SRC, "weapon_rack/scene.gltf"))
    if not edited:
        # panel is thin along X, face points -X in the source; rotate so it faces -Z, back against the wall at +Z
        fit(prims, rot_y=90, width=3.0, anchor="back")
        for p in prims:  # move origin to bottom-centre of the back face (wall)
            pass
    base = Image.open(os.path.join(GEN, "weapon_rack_base.png"))  # emblem replaced (see gen_textures.py)
    groups, prev = simple_model(mid, prims, tex_override={"02___Default": base}, max_tex=2048)
    # second, additive emissive pass for the glowing strips of the rack
    em = Image.open(os.path.join(SRC, "weapon_rack/textures/02___Default_emissive.jpeg")).convert("RGB")
    em_rgba = Image.merge("RGBA", (*Image.new("RGB", em.size, (255, 40, 60)).split(), ImageOps.grayscale(em)))
    rid = save_tex(mid, "glow", em_rgba, 1024)
    g0 = groups[0]
    groups.append(ml.Group(rid, ml.FLAG_EMISSIVE | ml.FLAG_GLOW | ml.FLAG_TRANSLUCENT, (1, 1, 1, 1), g0.pos, g0.uv, g0.nrm))
    prev.append(tex_file(mid, rid))
    emit(mid, [("root", -1, (0, 0, 0))], groups, prev)


HACKER_REPLACE = {  # material -> generated replacement texture (original art, no third-party logos)
    "Material.034": "hk_poster_blackmarket.png",
    "Material.032": "hk_poster_zeroday.png",
    "Material.017": "hk_poster_wanted_unicorn.png",
    "Material.020": "hk_poster_missions.png",
    "Material.024": "hk_screen_boot.png",
    "Material.001": "hk_screen_terminal.png",
}
HACKER_EMISSIVE = {"Material.001", "Material.002", "Screen_nc.001", "Material.024"}
HACKER_RECOLOR = {  # retro beige -> black gunmetal / luxury
    "beige_retro": [0.10, 0.10, 0.12, 1.0],
    "desk": [0.05, 0.05, 0.06, 1.0],
    "gray": [0.16, 0.16, 0.19, 1.0],
    "material_0": [0.30, 0.30, 0.34, 1.0],
    "material_21": [0.85, 0.65, 0.20, 1.0],
}


def conv_hacker_terminal():
    mid = "hacker_terminal"
    prims, edited = edited_or_gltf(mid, os.path.join(SRC, "hacker_terminal/scene.gltf"))
    if not edited:
        keep = []
        for p in prims:
            mn, mx = p.pos.min(0), p.pos.max(0)
            c = (mn + mx) / 2
            sz = mx - mn
            name = p.material["name"]
            if name == "desk" and sz.max() > 5:  # the two long desk tops - keep
                keep.append(p)
                continue
            if sz.max() > 5.5:  # room shell, floor, rugs, stray planes
                continue
            if c[2] > 1.7 or c[1] < -0.3 or mx[1] > 5.4:  # sofas / arcade / coffee table / ceiling
                continue
            if c[0] > 2.4 and c[2] > -1.2:  # magazines pile on the right
                continue
            keep.append(p)
        prims = keep
        # face -Z: the desk corner is at (-x,-z) in the source, user stands at +x+z -> rotate 135 so the setup faces the player
        fit(prims, rot_y=180, width=3.6, anchor="bottom")
    over = {k: os.path.join(GEN, v) for k, v in HACKER_REPLACE.items()}
    case = ImageEnhance.Brightness(ImageOps.grayscale(Image.open(os.path.join(SRC, "hacker_terminal/textures/Case_Texture_baseColor.png")).convert("RGB")).convert("RGB")).enhance(0.35)
    over["Case_Texture"] = case
    groups, prev = simple_model(mid, prims,
                                flags_for={m: ml.FLAG_EMISSIVE for m in HACKER_EMISSIVE},
                                tex_override=over, color_override=HACKER_RECOLOR, max_tex=512, budget=22000)
    emit(mid, [("root", -1, (0, 0, 0))], groups, prev)


# ------------------------------------------------------------------------------------------------
# The legendary unicorn: rig + "dark & dangerous" restyle
# ------------------------------------------------------------------------------------------------
UNICORN_BONES = [
    # name, parent, pivot (final model units = blocks, after scale). Filled in conv_unicorn.
    "root", "wing_left", "wing_right", "hind_left", "hind_right", "front_left", "front_right", "tail", "head",
]


def _welded(P, wv):
    """positions indexed by welded vertex id"""
    out = np.zeros((wv.max() + 1, 3))
    out[wv] = P
    return out


def _pca_axis(v):
    c = v.mean(0)
    u, s, vt = np.linalg.svd(v - c, full_matrices=False)
    return c, vt[0]


def _sharpen_islands(P, F, tri_mask, labels, root_point, stretch, taper_from=0.45, min_tris=30):
    """Turns soft curly locks / round feathers into long blade-like spikes.
    Every island is stretched along its main axis away from its root and its tip is tapered to a point."""
    out = P.copy()
    for lab in np.unique(labels[tri_mask]):
        m = tri_mask & (labels == lab)
        if m.sum() < min_tris:
            continue
        vi = np.unique(F[m].reshape(-1))
        v = P[vi]
        c, ax = _pca_axis(v)
        t = (v - c) @ ax
        # root = end nearest root_point
        if np.linalg.norm(c + ax * t.min() - root_point) > np.linalg.norm(c + ax * t.max() - root_point):
            ax, t = -ax, -t
        t0, t1 = t.min(), t.max()
        L = max(t1 - t0, 1e-6)
        s = (t - t0) / L  # 0 at root .. 1 at tip
        radial = (v - c) - np.outer(t, ax)
        k = np.clip((s - taper_from) / (1 - taper_from), 0, 1)
        shrink = 1.0 - 0.85 * k ** 1.3  # tip -> sharp point
        along = t0 + (t - t0) * (1 + (stretch - 1) * s)  # stretch increases towards tip
        out[vi] = c + np.outer(along, ax) + radial * shrink[:, None]
    return out


def conv_unicorn():
    mid = "unicorn"
    src = os.path.join(SRC, "unicorn_horse_3d/scene.gltf")
    p = ml.load_gltf(src)[0]
    # --- orientation / scale: head to -Z, 3.4 blocks tall when rearing
    P = p.pos @ ml.rot_y(180).T
    N = p.nrm @ ml.rot_y(180).T
    P[:, 0] -= 0.23  # body is off-centre in the source
    P[:, 1] -= P[:, 1].min()
    zc = 0.0
    S = 3.4 / P[:, 1].max()

    wv = ml.weld(P)
    F = wv[p.idx]  # welded faces for topology
    labels = ml.connected_components(F, wv.max() + 1)
    cent = P[p.idx].mean(1)

    img = Image.open(p.material["texture"]).convert("RGB")
    arr = np.asarray(img).astype(np.float64) / 255
    H, W = arr.shape[:2]
    cu = p.uv[p.idx].mean(1)
    col = arr[np.clip((cu[:, 1] % 1) * H, 0, H - 1).astype(int), np.clip((cu[:, 0] % 1) * W, 0, W - 1).astype(int)]

    ids, cnt = np.unique(labels, return_counts=True)
    body_lab = ids[np.argmax(cnt)]
    top_vertex_tri = np.argmax(P[p.idx][:, :, 1].max(1))
    horn_lab = labels[top_vertex_tri]
    island_col = {}
    for i in ids:
        m = labels == i
        island_col[i] = colorsys.rgb_to_hsv(*col[m].mean(0))
    is_horn = labels == horn_lab
    is_hair = np.array([(island_col[l][1] > 0.12 and 0.05 < island_col[l][0] < 0.11) for l in labels]) & ~is_horn
    island_y = {i: cent[labels == i][:, 1].mean() for i in ids}
    is_eye = np.array([(island_col[l][2] < 0.65 and l != body_lab and island_y[l] > 3.8) for l in labels]) & ~is_horn & ~is_hair
    is_feather = (labels != body_lab) & ~is_horn & ~is_hair & ~is_eye & (np.abs(cent[:, 0]) > 0.35)
    is_membrane = (labels == body_lab) & (np.abs(cent[:, 0]) > 0.45) & (cent[:, 1] > 2.4) & (cent[:, 2] > -0.3)
    is_wing = is_feather | is_membrane
    print("  unicorn parts: horn %d, hair %d, eyes %d, feathers %d, wing membrane %d" % (
        is_horn.sum(), is_hair.sum(), is_eye.sum(), is_feather.sum(), is_membrane.sum()))

    # --- CRUCIAL EDIT 1: sharp, wild, dangerous wings & hair (geometry)
    P2 = P.copy()
    # wings: each feather becomes a long blade
    for side in (1, -1):
        root = np.array([0.45 * side, 2.8, 0.3])
        msk = is_feather & (np.sign(cent[:, 0]) == side)
        P2w = _sharpen_islands(_welded(P2, wv), F, msk, labels, root, stretch=1.45, taper_from=0.35)
        # write back only vertices of those islands
        vsel = np.isin(wv, np.unique(F[msk].reshape(-1)))
        P2[vsel] = P2w[wv[vsel]]
    # mane & tail: curls -> spiky locks
    neck_root = np.array([0, 3.6, 0.2])
    tail_root = np.array([0, 2.3, 1.35])
    for region, root in ((cent[:, 2] < 1.15, neck_root), (cent[:, 2] >= 1.15, tail_root)):
        msk = is_hair & region
        P2h = _sharpen_islands(_welded(P2, wv), F, msk, labels, root, stretch=1.3, taper_from=0.4)
        vsel = np.isin(wv, np.unique(F[msk].reshape(-1)))
        P2[vsel] = P2h[wv[vsel]]
    # wing membrane: wider, swept back, jagged trailing edge
    vm = np.isin(np.arange(len(P)), p.idx[is_membrane].reshape(-1))
    for side in (1, -1):
        sel = vm & (np.sign(P[:, 0]) == side)
        d = np.abs(P2[sel, 0]) - 0.45
        P2[sel, 0] += side * d * 0.18
        P2[sel, 2] += d * 0.12  # sweep back
        # jagged: saw-tooth along the span pushes the trailing edge out
        tooth = (np.mod(d * 9.0, 1.0) - 0.5) * 0.10 * np.clip(d, 0, 1)
        P2[sel, 1] -= np.maximum(tooth, 0) * (P2[sel, 2] > 1.2)

    P = P2 * S
    pivots = {
        "root": (0, 0, 0),
        "wing_left": (0.45 * S, 2.8 * S, 0.3 * S),
        "wing_right": (-0.45 * S, 2.8 * S, 0.3 * S),
        "hind_left": (0.25 * S, 2.0 * S, 1.3 * S),
        "hind_right": (-0.25 * S, 2.0 * S, 1.3 * S),
        "front_left": (0.25 * S, 2.95 * S, -0.45 * S),
        "front_right": (-0.25 * S, 2.95 * S, -0.45 * S),
        "tail": (0, 2.3 * S, 1.35 * S),
        "head": (0, 3.65 * S, -0.35 * S),
    }
    bones = [(n, -1 if n == "root" else 0, pivots[n]) for n in UNICORN_BONES]
    bidx = {n: i for i, n in enumerate(UNICORN_BONES)}

    # --- skin weights (one bone + blend to root per vertex)
    V = P / S  # rule thresholds in source units
    nv = len(P)
    bone = np.zeros(nv, dtype=np.int64)
    weight = np.zeros(nv)

    def assign(mask, b, w):
        better = mask & (w > weight)
        bone[better] = bidx[b]
        weight[better] = w[better]

    x, y, z = V[:, 0], V[:, 1], V[:, 2]
    vert_of = lambda tm: np.isin(np.arange(nv), p.idx[tm].reshape(-1))  # noqa: E731
    wing_v = vert_of(is_wing)
    for side, b in ((1, "wing_left"), (-1, "wing_right")):
        w = np.clip((x * side - 0.33) / 0.25, 0, 1)
        assign(wing_v & (x * side > 0), b, w)
    hair_v = vert_of(is_hair)
    tail_v = hair_v & (z > 1.15) & (y < 2.7)
    assign(tail_v, "tail", np.clip((z - 1.3) / 0.35, 0.2, 1))
    legs_ok = ~wing_v & ~hair_v
    for side, b in ((1, "hind_left"), (-1, "hind_right")):
        w = np.clip((2.05 - y) / 0.45, 0, 1)
        assign(legs_ok & (z > 0.75) & (x * side > -0.02), b, w)
    for side, b in ((1, "front_left"), (-1, "front_right")):
        w = np.clip((-0.5 - z) / 0.35, 0, 1) * np.clip((3.25 - y) / 0.25, 0, 1)
        assign(legs_ok & (x * side > -0.02), b, w)
    head_v = vert_of(~is_wing) & (y > 3.35) & (z < 0.35)
    assign(head_v, "head", np.clip((y - 3.35) / 0.35, 0, 1))
    hmask = vert_of(is_horn | is_eye)
    bone[hmask] = bidx["head"]
    weight[hmask] = 1.0

    # --- CRUCIAL EDIT 2: dark re-colour (texture) -> obsidian coat, black/violet hair, black blade feathers
    hsv = np.asarray(img.convert("HSV")).astype(np.float64) / 255
    lum = arr.mean(2)
    dark = np.zeros_like(arr)
    # coat: obsidian with a cold blue sheen, detail from original luminance
    dark[..., 0] = 0.07 + 0.10 * lum
    dark[..., 1] = 0.07 + 0.10 * lum
    dark[..., 2] = 0.10 + 0.17 * lum
    blondish = (hsv[..., 1] > 0.12) & (hsv[..., 0] > 0.04) & (hsv[..., 0] < 0.13)
    # hair: black roots -> deep crimson/violet highlights
    hair = np.stack([0.05 + 0.55 * lum ** 2.2, 0.0 + 0.04 * lum, 0.06 + 0.35 * lum ** 2.2], -1)
    dark[blondish] = hair[blondish]
    tex_body = Image.fromarray((np.clip(dark, 0, 1) * 255).astype(np.uint8))
    # feathers: separate texture -> black blades with violet edge light
    feather = np.stack([0.04 + 0.30 * lum ** 3, 0.03 + 0.05 * lum ** 3, 0.07 + 0.45 * lum ** 3], -1)
    tex_feather = Image.fromarray((np.clip(feather, 0, 1) * 255).astype(np.uint8))
    # horn: neutral bright spiral detail, the dark rainbow is applied as animated vertex colour at runtime
    tex_horn = ImageOps.autocontrast(ImageOps.grayscale(img)).convert("RGB")
    tex_horn = ImageEnhance.Brightness(tex_horn).enhance(1.15)
    # eyes: solid blood red, emissive
    rid_body = save_tex(mid, "body", tex_body, 1024)
    rid_feather = save_tex(mid, "feathers", tex_feather, 1024)
    rid_horn = save_tex(mid, "horn", tex_horn, 512)
    rid_white = white_tex(mid)

    def group(mask, rid, flags, color=(1, 1, 1, 1), shrink=None):
        flat = p.idx[mask].reshape(-1)
        return ml.Group(rid, flags, color, P[flat], p.uv[flat], N[flat], bone[flat], weight[flat])

    # decimate the body (29k tris) for runtime performance: done on welded topology
    groups = []
    body_mask = ~is_horn & ~is_eye & ~is_feather
    groups.append(_decimated_group(p, P, N, bone, weight, body_mask, 11000, rid_body, ml.FLAG_NOCULL))
    groups.append(_decimated_group(p, P, N, bone, weight, is_feather, 4500, rid_feather, ml.FLAG_NOCULL))
    groups.append(group(is_eye, rid_white, ml.FLAG_EMISSIVE | ml.FLAG_GLOW, (1.0, 0.05, 0.12, 1)))
    groups.append(_decimated_group(p, P, N, bone, weight, is_horn, 500, rid_horn,
                                   ml.FLAG_EMISSIVE | ml.FLAG_RAINBOW | ml.FLAG_GLOW))
    prev = [tex_file(mid, rid_body), tex_file(mid, rid_feather), None, tex_file(mid, rid_horn)]
    emit(mid, bones, groups, prev)


def _decimated_group(p, P, N, bone, weight, tri_mask, budget, rid, flags):
    sub = ml.Prim("u", {}, P, N, p.uv, p.idx[tri_mask])
    # compact vertex set
    used = np.unique(sub.idx)
    remap = np.full(len(P), -1)
    remap[used] = np.arange(len(used))
    sub = ml.Prim("u", {}, P[used], N[used], p.uv[used], remap[sub.idx])
    b, w = bone[used], weight[used]
    if sub.tri_count() > budget:
        import fast_simplification
        reduction = 1 - budget / sub.tri_count()
        pts = sub.pos.astype(np.float32)
        _, _, coll = fast_simplification.simplify(pts, sub.idx, target_reduction=reduction, return_collapses=True)
        new_pts, new_faces, mapping = fast_simplification.replay_simplification(pts, sub.idx, coll)
        rep = np.zeros(len(new_pts), dtype=np.int64)
        rep[mapping] = np.arange(len(mapping))
        sub = ml.Prim("u", {}, new_pts.astype(np.float64), sub.nrm[rep], sub.uv[rep], new_faces)
        b, w = b[rep], w[rep]
    flat = sub.idx.reshape(-1)
    return ml.Group(rid, flags, (1, 1, 1, 1), sub.pos[flat], sub.uv[flat], sub.nrm[flat], b[flat], w[flat])


CONVERTERS = {
    "briefcase": conv_briefcase,
    "mona_lisa": conv_mona_lisa,
    "freeze_ray": conv_freeze_ray,
    "assault_rifle": conv_rifle,
    "weapon_rack": conv_weapon_rack,
    "hacker_terminal": conv_hacker_terminal,
    "unicorn": conv_unicorn,
}



# ------------------------------------------------------------------------------------------------
# The Infinity Dragon Core - designed from scratch (procedural, no source file)
# ------------------------------------------------------------------------------------------------
def _bipyramid(n, r, h_top, h_bot, twist=0.0):
    """Faceted crystal: n-gon ring with a tip above and below. Returns triangle soup (pos, uv, nrm)."""
    tris = []
    ring = [(math.cos(2 * math.pi * k / n + twist) * r, 0.0, math.sin(2 * math.pi * k / n + twist) * r) for k in range(n)]
    top, bot = (0.0, h_top, 0.0), (0.0, -h_bot, 0.0)
    for k in range(n):
        a, b = ring[k], ring[(k + 1) % n]
        tris.append((top, b, a))
        tris.append((bot, a, b))
    return tris


def _torus(R, r, seg=48, sides=8, tilt=0.0):
    tris = []
    for i in range(seg):
        for j in range(sides):
            def p(ii, jj):
                u, v = 2 * math.pi * ii / seg, 2 * math.pi * jj / sides
                return ((R + r * math.cos(v)) * math.cos(u), r * math.sin(v), (R + r * math.cos(v)) * math.sin(u))
            a, b, c, d = p(i, j), p(i + 1, j), p(i + 1, j + 1), p(i, j + 1)
            tris.append((a, b, c))
            tris.append((a, c, d))
    return tris


def _soup(tris, offset=(0, 0, 0), uv_mode="cyl"):
    pos = np.array([v for t in tris for v in t], dtype=np.float64) + np.array(offset)
    nrm = np.repeat(ml.face_normals(pos, np.arange(len(pos)).reshape(-1, 3)), 3, axis=0)
    if uv_mode == "cyl":
        ang = (np.arctan2(pos[:, 2] - offset[2], pos[:, 0] - offset[0]) / (2 * math.pi)) % 1
        hh = pos[:, 1] - pos[:, 1].min()
        uv = np.c_[ang, 1 - hh / max(hh.max(), 1e-6)]
    else:
        uv = np.c_[(pos[:, 0] - pos[:, 0].min()) / max(np.ptp(pos[:, 0]), 1e-6), (pos[:, 2] - pos[:, 2].min()) / max(np.ptp(pos[:, 2]), 1e-6)]
    return pos, uv, nrm


def conv_dragon_core():
    mid = "dragon_core"
    # textures
    size = 128
    yy, xx = np.mgrid[0:size, 0:size] / (size - 1)
    crystal = np.zeros((size, size, 3))
    crystal[..., 0] = 0.55 + 0.45 * (1 - yy)
    crystal[..., 1] = 0.10 + 0.25 * (1 - yy) ** 3
    crystal[..., 2] = 0.85 + 0.15 * yy
    veins = (np.abs(np.sin(xx * 31 + np.sin(yy * 9) * 2)) < 0.08) | (np.abs(np.sin(yy * 23 + xx * 5)) < 0.05)
    crystal[veins] = [1.0, 0.85, 1.0]
    rid_crystal = save_tex(mid, "crystal", Image.fromarray((crystal * 255).astype(np.uint8)), 128)
    ring = np.zeros((size, size, 3))
    ring[...] = [0.85, 0.62, 0.18]
    runes = (np.floor(xx * 24) % 3 == 0) & (np.abs(yy - 0.5) < 0.18)
    ring[runes] = [0.35, 0.95, 1.0]
    ring *= (0.75 + 0.25 * np.sin(yy * math.pi))[..., None]
    rid_ring = save_tex(mid, "ring", Image.fromarray((np.clip(ring, 0, 1) * 255).astype(np.uint8)), 128)
    shard = np.zeros((32, 32, 3))
    shard[...] = [1.0, 0.25, 0.85]
    rid_shard = save_tex(mid, "shard", Image.fromarray((shard * 255).astype(np.uint8)), 32)

    bones = [("root", -1, (0, 0, 0)), ("crystal", 0, (0, 0, 0)), ("ring_a", 0, (0, 0, 0)), ("ring_b", 0, (0, 0, 0)), ("shards", 0, (0, 0, 0))]

    def grp(tris, rid, flags, bone, color=(1, 1, 1, 1), offset=(0, 0, 0)):
        p, uv, n = _soup(tris, offset)
        return ml.Group(rid, flags, color, p, uv, n, np.full(len(p), bone), np.ones(len(p)))

    groups = [
        grp(_bipyramid(8, 0.26, 0.48, 0.42), rid_crystal, ml.FLAG_EMISSIVE | ml.FLAG_GLOW, 1),
        grp(_bipyramid(6, 0.13, 0.26, 0.22, 0.3), rid_shard, ml.FLAG_EMISSIVE | ml.FLAG_GLOW, 1, (1, 0.9, 1, 1)),
        grp(_torus(0.55, 0.035), rid_ring, ml.FLAG_EMISSIVE | ml.FLAG_GLOW, 2),
        grp(_torus(0.70, 0.03, 56, 8), rid_ring, ml.FLAG_EMISSIVE | ml.FLAG_GLOW, 3),
    ]
    sh = []
    for k in range(6):
        a = 2 * math.pi * k / 6
        sh += [tuple(tuple(np.array(v) + (math.cos(a) * 0.9, math.sin(a * 2) * 0.15, math.sin(a) * 0.9)) for v in t)
               for t in _bipyramid(4, 0.05, 0.12, 0.12)]
    groups.append(grp(sh, rid_shard, ml.FLAG_EMISSIVE | ml.FLAG_GLOW, 4))
    emit(mid, bones, groups, [tex_file(mid, rid_crystal), tex_file(mid, rid_shard), tex_file(mid, rid_ring), tex_file(mid, rid_ring), tex_file(mid, rid_shard)])


CONVERTERS["dragon_core"] = conv_dragon_core


if __name__ == "__main__":
    todo = sys.argv[1:] or list(CONVERTERS)
    for k in todo:
        print("converting", k)
        CONVERTERS[k]()
