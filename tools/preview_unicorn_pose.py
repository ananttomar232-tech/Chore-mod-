#!/usr/bin/env python3
"""
Mirror of client/render/entity/UnicornPose.java - poses the rigged unicorn .hmesh and writes OBJ files you can open
in Blockbench (or any viewer) to tune the animation without starting Minecraft.

    python3 tools/preview_unicorn_pose.py out_dir lean gallop phase fly t
"""
import gzip
import math
import os
import struct
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import meshlib as ml  # noqa: E402

LEAN_DEG = 46.0
LEAN_LIFT = 0.30


def read_hmesh(path):
    data = gzip.open(path).read()
    o = 8
    nb = struct.unpack_from(">i", data, o)[0]; o += 4
    bones = []
    for _ in range(nb):
        ln = struct.unpack_from(">H", data, o)[0]; o += 2
        name = data[o:o + ln].decode(); o += ln
        parent, px, py, pz = struct.unpack_from(">i3f", data, o); o += 16
        bones.append((name, parent, np.array([px, py, pz])))
    ng = struct.unpack_from(">i", data, o)[0]; o += 4
    groups = []
    dt = np.dtype([("x", ">f4"), ("y", ">f4"), ("z", ">f4"), ("u", ">f4"), ("v", ">f4"),
                   ("nx", "i1"), ("ny", "i1"), ("nz", "i1"), ("b", "u1"), ("w", "u1")])
    for _ in range(ng):
        ln = struct.unpack_from(">H", data, o)[0]; o += 2
        tex = data[o:o + ln].decode(); o += ln
        flags, r, g, b, a = struct.unpack_from(">i4f", data, o); o += 20
        n = struct.unpack_from(">i", data, o)[0]; o += 4
        rec = np.frombuffer(data, dtype=dt, count=n, offset=o); o += n * dt.itemsize
        groups.append((tex, flags, (r, g, b, a), rec))
    return bones, groups


def T(x, y, z):
    m = np.eye(4); m[:3, 3] = (x, y, z); return m


def RX(a):
    m = np.eye(4); c, s = math.cos(a), math.sin(a); m[1:3, 1:3] = [[c, -s], [s, c]]; return m


def RY(a):
    m = np.eye(4); c, s = math.cos(a), math.sin(a); m[0, 0], m[0, 2], m[2, 0], m[2, 2] = c, s, -s, c; return m


def RZ(a):
    m = np.eye(4); c, s = math.cos(a), math.sin(a); m[0:2, 0:2] = [[c, -s], [s, c]]; return m


def pose(bones, lean, gallop, phase, fly, t, head_yaw=0.0, head_pitch=0.0):
    names = [b[0] for b in bones]
    hl, hr = bones[names.index("hind_left")][2], bones[names.index("hind_right")][2]
    hy, hz = (hl[1] + hr[1]) / 2, (hl[2] + hr[2]) / 2
    lr = -math.radians(lean * LEAN_DEG)
    root = T(0, lean * LEAN_LIFT + math.sin(t * 0.06) * 0.02, 0) @ T(0, hy, hz) @ RX(lr) @ T(0, -hy, -hz)
    out = [root]
    sw = math.sin(phase * 0.6662) * 0.85 * gallop
    sw2 = math.sin(phase * 0.6662 + math.pi) * 0.85 * gallop
    for name, _, p in bones[1:]:
        m = root @ T(*p)
        if name == "hind_left":
            m = m @ RX(-lr * 0.92 + sw * 0.6)
        elif name == "hind_right":
            m = m @ RX(-lr * 0.92 + sw2 * 0.6)
        elif name == "front_left":
            m = m @ RX(lean * 0.35 + sw2 * 0.7 + (1 - lean) * math.sin(t * 0.08) * 0.12)
        elif name == "front_right":
            m = m @ RX(lean * 0.35 + sw * 0.7 + (1 - lean) * math.sin(t * 0.08 + 1.7) * 0.12)
        elif name in ("wing_left", "wing_right"):
            side = 1 if name == "wing_left" else -1
            idle = math.sin(t * 0.07) * 0.06
            flap = math.sin(t * 0.45) * 0.62 * fly
            fold = (1 - fly) * lean * 0.35
            m = m @ RY(side * fold) @ RZ(side * (idle + flap - fold * 0.4))
        elif name == "tail":
            m = m @ RX(math.sin(t * 0.11) * 0.12 + gallop * 0.25 - lean * 0.3) @ RY(math.sin(t * 0.07) * 0.18)
        elif name == "head":
            m = m @ RX(lean * 0.55 - math.radians(head_pitch) * 0.6) @ RY(math.radians(max(-40, min(40, head_yaw))) * 0.7)
        out.append(m @ T(*(-p)))
    return out


def apply(groups, mats):
    res = []
    for tex, flags, col, rec in groups:
        P = np.c_[rec["x"], rec["y"], rec["z"], np.ones(len(rec))]
        b = rec["b"].astype(int)
        w = rec["w"] / 255.0
        p0 = P @ mats[0].T
        pb = np.einsum("nij,nj->ni", np.stack(mats)[b], P)
        p = p0 + (pb - p0) * w[:, None]
        N = np.c_[rec["nx"], rec["ny"], rec["nz"]] / 127.0
        uv = np.c_[rec["u"], rec["v"]]
        res.append(ml.Group(tex, flags, col, p[:, :3], uv, N))
    return res


if __name__ == "__main__":
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out = sys.argv[1]
    lean, gallop, phase, fly, t = (float(x) for x in sys.argv[2:7])
    bones, groups = read_hmesh(os.path.join(root, "src/main/resources/assets/limitlessheist/meshes/unicorn.hmesh"))
    gs = apply(groups, pose(bones, lean, gallop, phase, fly, t))
    allp = np.concatenate([g.pos for g in gs])
    print("min y %.3f  max y %.3f" % (allp[:, 1].min(), allp[:, 1].max()))
    texdir = os.path.join(root, "src/main/resources/assets/limitlessheist/textures/mesh/unicorn")
    os.makedirs(out, exist_ok=True)
    tex = []
    for g in gs:
        fn = g.texture.split("/")[-1]
        src = os.path.join(texdir, fn)
        if os.path.exists(src):
            import shutil
            shutil.copy(src, os.path.join(out, fn))
            tex.append(fn)
        else:
            tex.append(None)
    ml.write_obj(os.path.join(out, "pose.obj"), gs, tex)
