"""
meshlib.py - tiny glTF / OBJ loader + writer used by the Limitless Heist model pipeline.

Pipeline:  Sketchfab glTF  --(convert_models.py)-->  assets/limitlessheist/meshes/<id>.hmesh
                                                 +   assets/limitlessheist/textures/mesh/<id>/*.png
                                                 +   blockbench/<id>/<id>.obj   (editable in Blockbench)

Everything is plain numpy so it runs anywhere (python3 -m pip install numpy pillow fast-simplification).
"""
import base64
import gzip
import json
import math
import os
import struct

import numpy as np

COMP = {5120: np.int8, 5121: np.uint8, 5122: np.int16, 5123: np.uint16, 5125: np.uint32, 5126: np.float32}
NCOMP = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}


class Prim:
    """One triangle list with a single material. Arrays are numpy, positions already in world space."""

    def __init__(self, name, material, pos, nrm, uv, idx):
        self.name = name
        self.material = material  # dict: name, texture(abs path or None), color(rgba floats), emissive(bool), double_sided
        self.pos = pos.astype(np.float64)
        self.nrm = nrm.astype(np.float64)
        self.uv = uv.astype(np.float64)
        self.idx = idx.astype(np.int64).reshape(-1, 3)

    def tri_count(self):
        return len(self.idx)


def _node_matrix(n):
    if "matrix" in n:
        return np.array(n["matrix"], dtype=np.float64).reshape(4, 4).T
    t = n.get("translation", [0, 0, 0])
    x, y, z, w = n.get("rotation", [0, 0, 0, 1])
    s = n.get("scale", [1, 1, 1])
    r = np.array([
        [1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
        [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
        [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)],
    ])
    m = np.eye(4)
    m[:3, :3] = r * np.array(s)
    m[:3, 3] = t
    return m


def load_gltf(path):
    """Returns a list of Prim in world space (scene graph transforms applied)."""
    base = os.path.dirname(path)
    g = json.load(open(path))
    buffers = []
    for b in g["buffers"]:
        uri = b["uri"]
        if uri.startswith("data:"):
            buffers.append(base64.b64decode(uri.split(",", 1)[1]))
        else:
            buffers.append(open(os.path.join(base, uri), "rb").read())

    def accessor(i):
        a = g["accessors"][i]
        bv = g["bufferViews"][a["bufferView"]]
        dt = np.dtype(COMP[a["componentType"]])
        nc = NCOMP[a["type"]]
        off = bv.get("byteOffset", 0) + a.get("byteOffset", 0)
        stride = bv.get("byteStride", 0) or dt.itemsize * nc
        raw = buffers[bv["buffer"]]
        if stride == dt.itemsize * nc:
            arr = np.frombuffer(raw, dtype=dt, count=a["count"] * nc, offset=off).reshape(a["count"], nc)
        else:
            arr = np.lib.stride_tricks.as_strided(
                np.frombuffer(raw, dtype=np.uint8, offset=off),
                shape=(a["count"], nc * dt.itemsize), strides=(stride, 1)).copy().view(dt).reshape(a["count"], nc)
        arr = arr.astype(np.float64)
        if a.get("normalized"):
            arr /= float(np.iinfo(dt).max)
        return arr

    def tex_path(info):
        if not info:
            return None
        tex = g["textures"][info["index"]]
        src = tex.get("source")
        if src is None:
            return None
        return os.path.join(base, g["images"][src]["uri"])

    materials = []
    for m in g.get("materials", []):
        pbr = m.get("pbrMetallicRoughness", {})
        sg = m.get("extensions", {}).get("KHR_materials_pbrSpecularGlossiness", {})
        tex = tex_path(pbr.get("baseColorTexture")) or tex_path(sg.get("diffuseTexture"))
        color = pbr.get("baseColorFactor") or sg.get("diffuseFactor") or [1, 1, 1, 1]
        em_tex = tex_path(m.get("emissiveTexture"))
        em_f = m.get("emissiveFactor", [0, 0, 0])
        materials.append({
            "name": m.get("name", "mat"),
            "texture": tex,
            "emissive_texture": em_tex,
            "emissive_factor": em_f,
            "color": list(color),
            "alpha_mode": m.get("alphaMode", "OPAQUE"),
            "double_sided": m.get("doubleSided", False),
        })
    default_mat = {"name": "default", "texture": None, "emissive_texture": None, "emissive_factor": [0, 0, 0],
                   "color": [1, 1, 1, 1], "alpha_mode": "OPAQUE", "double_sided": False}

    prims = []

    def walk(ni, parent, path):
        n = g["nodes"][ni]
        m = parent @ _node_matrix(n)
        name = n.get("name", "node%d" % ni)
        if "mesh" in n:
            for p in g["meshes"][n["mesh"]]["primitives"]:
                if p.get("mode", 4) != 4:
                    continue
                pos = accessor(p["attributes"]["POSITION"])
                nrm = accessor(p["attributes"]["NORMAL"]) if "NORMAL" in p["attributes"] else np.zeros_like(pos)
                uv = accessor(p["attributes"]["TEXCOORD_0"]) if "TEXCOORD_0" in p["attributes"] else np.zeros((len(pos), 2))
                idx = accessor(p["indices"]).astype(np.int64) if "indices" in p else np.arange(len(pos))
                pw = (np.c_[pos, np.ones(len(pos))] @ m.T)[:, :3]
                nm = np.linalg.inv(m[:3, :3]).T
                nw = nrm @ nm.T
                ln = np.linalg.norm(nw, axis=1, keepdims=True)
                nw = np.where(ln > 1e-9, nw / np.maximum(ln, 1e-9), 0)
                mat = materials[p["material"]] if "material" in p else default_mat
                # a negative-determinant transform flips winding
                idx = idx.reshape(-1, 3)
                if np.linalg.det(m[:3, :3]) < 0:
                    idx = idx[:, ::-1]
                prims.append(Prim("/".join(path + [name]), dict(mat), pw, nw, uv, idx))
        for c in n.get("children", []):
            walk(c, m, path + [name])

    scene = g["scenes"][g.get("scene", 0)]
    for r in scene["nodes"]:
        walk(r, np.eye(4), [])
    return prims


def load_obj(path):
    """Minimal Wavefront OBJ (+MTL map_Kd) loader, so models edited in Blockbench can be fed back in."""
    base = os.path.dirname(path)
    vs, vts, vns = [], [], []
    groups = {}  # material -> list of (v,vt,vn) triples per corner
    mats = {}
    cur = "default"
    obj_name = "obj"
    for line in open(path, encoding="utf-8", errors="replace"):
        parts = line.strip().split()
        if not parts:
            continue
        k = parts[0]
        if k == "v":
            vs.append([float(x) for x in parts[1:4]])
        elif k == "vt":
            vts.append([float(x) for x in parts[1:3]])
        elif k == "vn":
            vns.append([float(x) for x in parts[1:4]])
        elif k in ("o", "g"):
            obj_name = parts[1] if len(parts) > 1 else obj_name
        elif k == "usemtl":
            cur = parts[1]
        elif k == "mtllib":
            mp = os.path.join(base, " ".join(parts[1:]))
            if os.path.exists(mp):
                m = None
                for ml in open(mp, encoding="utf-8", errors="replace"):
                    mp_ = ml.strip().split()
                    if not mp_:
                        continue
                    if mp_[0] == "newmtl":
                        m = mp_[1]
                        mats[m] = {"name": m, "texture": None, "color": [1, 1, 1, 1], "emissive_texture": None,
                                   "emissive_factor": [0, 0, 0], "alpha_mode": "OPAQUE", "double_sided": True}
                    elif mp_[0] == "map_Kd" and m:
                        mats[m]["texture"] = os.path.join(base, " ".join(mp_[1:]))
                    elif mp_[0] == "Kd" and m:
                        mats[m]["color"] = [float(x) for x in mp_[1:4]] + [1]
        elif k == "f":
            corners = []
            for c in parts[1:]:
                s = c.split("/")
                vi = int(s[0])
                ti = int(s[1]) if len(s) > 1 and s[1] else 0
                ni = int(s[2]) if len(s) > 2 and s[2] else 0
                corners.append((vi - 1 if vi > 0 else len(vs) + vi,
                                (ti - 1 if ti > 0 else len(vts) + ti) if ti else -1,
                                (ni - 1 if ni > 0 else len(vns) + ni) if ni else -1))
            for i in range(1, len(corners) - 1):
                groups.setdefault(cur, []).extend([corners[0], corners[i], corners[i + 1]])
    vs = np.array(vs or [[0, 0, 0]])
    vts = np.array(vts or [[0, 0]])
    vns = np.array(vns or [[0, 1, 0]])
    prims = []
    for mname, cs in groups.items():
        cs = np.array(cs)
        pos = vs[cs[:, 0]]
        uv = np.where(cs[:, 1:2] >= 0, vts[np.maximum(cs[:, 1], 0)], 0)
        nrm = np.where(cs[:, 2:3] >= 0, vns[np.maximum(cs[:, 2], 0)], 0)
        idx = np.arange(len(cs)).reshape(-1, 3)
        mat = mats.get(mname, {"name": mname, "texture": None, "color": [1, 1, 1, 1], "emissive_texture": None,
                               "emissive_factor": [0, 0, 0], "alpha_mode": "OPAQUE", "double_sided": True})
        prims.append(Prim(obj_name + "/" + mname, dict(mat), pos, nrm, uv, idx))
    return prims


def face_normals(pos, idx):
    a, b, c = pos[idx[:, 0]], pos[idx[:, 1]], pos[idx[:, 2]]
    n = np.cross(b - a, c - a)
    ln = np.linalg.norm(n, axis=1, keepdims=True)
    return n / np.maximum(ln, 1e-12)


def bounds(prims):
    allp = np.concatenate([p.pos for p in prims])
    return allp.min(0), allp.max(0)


def simplify(prim, target_tris):
    """Quadric decimation keeping per-vertex UV/normal of the surviving vertices."""
    if prim.tri_count() <= target_tris:
        return prim
    import fast_simplification
    reduction = 1.0 - target_tris / prim.tri_count()
    pts = prim.pos.astype(np.float32)
    faces = prim.idx.astype(np.int64)
    _, _, collapses = fast_simplification.simplify(pts, faces, target_reduction=reduction, return_collapses=True)
    new_pts, new_faces, mapping = fast_simplification.replay_simplification(pts, faces, collapses)
    # mapping: old vertex index -> new vertex index. Pick, for every new vertex, the attributes of one old vertex.
    rep = np.full(len(new_pts), -1, dtype=np.int64)
    order = np.arange(len(mapping))
    rep[mapping] = order  # last writer wins, good enough
    rep = np.where(rep < 0, 0, rep)
    return Prim(prim.name, prim.material, new_pts.astype(np.float64), prim.nrm[rep], prim.uv[rep], new_faces)


# --------------------------------------------------------------------------------------------
#  .hmesh writer  (read by com.limitlessanant.heist.client.mesh.HeistMesh)
# --------------------------------------------------------------------------------------------
FLAG_EMISSIVE = 1     # rendered full-bright
FLAG_TRANSLUCENT = 2  # alpha blended
FLAG_RAINBOW = 4      # vertex colour animated with a dark rainbow at runtime
FLAG_GLOW = 8         # extra additive "bloom" pass on top
FLAG_NOCULL = 16      # double sided


class Group:
    def __init__(self, texture, flags, color, pos, uv, nrm, bone=None, weight=None):
        self.texture = texture  # resource id, e.g. "limitlessheist:textures/mesh/unicorn/body.png"
        self.flags = flags
        self.color = color  # (r,g,b,a) 0..1
        self.pos = pos  # (T*3, 3) expanded triangle corners
        self.uv = uv
        self.nrm = nrm
        n = len(pos)
        self.bone = bone if bone is not None else np.zeros(n, dtype=np.int64)
        self.weight = weight if weight is not None else np.ones(n)


def write_hmesh(path, bones, groups):
    """
    Big-endian, gzip compressed (Java DataInputStream friendly).
      int   magic 'HMSH'   int version (1)
      int   boneCount { UTF name, int parent, float px,py,pz }
      int   groupCount {
              UTF texture, int flags, float r,g,b,a, int vertexCount,
              vertexCount * { float x,y,z, float u,v, byte nx,ny,nz, byte bone, byte weight(0..255) }
            }
    """
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out = bytearray()
    out += b"HMSH" + struct.pack(">i", 1)
    out += struct.pack(">i", len(bones))
    for name, parent, pivot in bones:
        nb = name.encode("utf-8")
        out += struct.pack(">H", len(nb)) + nb + struct.pack(">i3f", parent, *pivot)
    out += struct.pack(">i", len(groups))
    for gr in groups:
        tb = gr.texture.encode("utf-8")
        out += struct.pack(">H", len(tb)) + tb
        out += struct.pack(">i4f", gr.flags, *gr.color)
        n = len(gr.pos)
        out += struct.pack(">i", n)
        rec = np.zeros(n, dtype=[("x", ">f4"), ("y", ">f4"), ("z", ">f4"), ("u", ">f4"), ("v", ">f4"),
                                 ("nx", "i1"), ("ny", "i1"), ("nz", "i1"), ("b", "u1"), ("w", "u1")])
        rec["x"], rec["y"], rec["z"] = gr.pos[:, 0], gr.pos[:, 1], gr.pos[:, 2]
        rec["u"], rec["v"] = gr.uv[:, 0], gr.uv[:, 1]
        nn = np.clip(np.round(gr.nrm * 127), -127, 127).astype(np.int8)
        rec["nx"], rec["ny"], rec["nz"] = nn[:, 0], nn[:, 1], nn[:, 2]
        rec["b"] = gr.bone.astype(np.uint8)
        rec["w"] = np.clip(np.round(gr.weight * 255), 0, 255).astype(np.uint8)
        out += rec.tobytes()
    with gzip.open(path, "wb", compresslevel=9) as f:
        f.write(bytes(out))


def write_obj(path, groups, tex_files):
    """Writes an OBJ + MTL (for Blockbench / previews). tex_files: list of png paths relative to the obj, per group."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    mtl_path = path[:-4] + ".mtl"
    name = os.path.basename(path)[:-4]
    with open(mtl_path, "w") as m:
        for i, gr in enumerate(groups):
            m.write("newmtl m%d\nKd %.4f %.4f %.4f\nd %.3f\n" % (i, gr.color[0], gr.color[1], gr.color[2], gr.color[3]))
            if tex_files[i]:
                m.write("map_Kd %s\n" % tex_files[i])
            m.write("\n")
    with open(path, "w") as f:
        f.write("# Limitless Heist mesh export - edit in Blockbench (File > Import > OBJ) and re-export as OBJ\n")
        f.write("mtllib %s.mtl\no %s\n" % (name, name))
        vi = 1
        for i, gr in enumerate(groups):
            f.write("g part%d\nusemtl m%d\n" % (i, i))
            for p in gr.pos:
                f.write("v %.5f %.5f %.5f\n" % tuple(p))
            for t in gr.uv:
                f.write("vt %.5f %.5f\n" % (t[0], 1.0 - t[1]))
            for nn in gr.nrm:
                f.write("vn %.4f %.4f %.4f\n" % tuple(nn))
            for t in range(len(gr.pos) // 3):
                a = vi + t * 3
                f.write("f %d/%d/%d %d/%d/%d %d/%d/%d\n" % (a, a, a, a + 1, a + 1, a + 1, a + 2, a + 2, a + 2))
            vi += len(gr.pos)


def rot_y(deg):
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rot_x(deg):
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def rot_z(deg):
    r = math.radians(deg)
    c, s = math.cos(r), math.sin(r)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def weld(pos, decimals=4):
    """Index of a welded (deduplicated by position) vertex for every vertex."""
    q = np.round(pos * 10 ** decimals).astype(np.int64)
    _, inv = np.unique(q, axis=0, return_inverse=True)
    return inv.reshape(-1)


def connected_components(idx, nverts):
    """Label every triangle with the id of its connected mesh island (vectorised union-find)."""
    parent = np.arange(nverts)

    def find(x):
        while True:
            px = parent[x]
            if np.array_equal(px, x):
                return x
            parent[x] = parent[px]
            x = parent[x]

    edges = np.r_[idx[:, [0, 1]], idx[:, [1, 2]]]
    for _ in range(64):
        ra = find(edges[:, 0].copy())
        rb = find(edges[:, 1].copy())
        diff = ra != rb
        if not diff.any():
            break
        lo = np.minimum(ra[diff], rb[diff])
        hi = np.maximum(ra[diff], rb[diff])
        np.minimum.at(parent, hi, lo)
    roots = find(np.arange(nverts))
    return roots[idx[:, 0]]
