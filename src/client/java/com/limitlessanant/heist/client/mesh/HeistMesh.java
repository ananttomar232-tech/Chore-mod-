package com.limitlessanant.heist.client.mesh;

import net.minecraft.util.Identifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * A converted 3D model (.hmesh, written by tools/convert_models.py). Triangle soup grouped by material, optional bones.
 * Units are blocks, origin as the converter chose (usually bottom-centre), the model's front faces -Z.
 */
public final class HeistMesh {
    public static final int FLAG_EMISSIVE = 1, FLAG_TRANSLUCENT = 2, FLAG_RAINBOW = 4, FLAG_GLOW = 8, FLAG_NOCULL = 16;

    public record Bone(String name, int parent, float px, float py, float pz) {
    }

    public static final class Group {
        public Identifier texture;
        public int flags;
        public float r, g, b, a;
        public int count;
        public float[] pos, uv, nrm, weight;
        public byte[] bone;

        public boolean has(int flag) {
            return (flags & flag) != 0;
        }
    }

    public final String name;
    public final Bone[] bones;
    public final Group[] groups;
    public final float minX, minY, minZ, maxX, maxY, maxZ;

    private HeistMesh(String name, Bone[] bones, Group[] groups) {
        this.name = name;
        this.bones = bones;
        this.groups = groups;
        float[] mn = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE}, mx = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (Group g : groups) {
            for (int i = 0; i < g.count; i++) {
                for (int k = 0; k < 3; k++) {
                    mn[k] = Math.min(mn[k], g.pos[i * 3 + k]);
                    mx[k] = Math.max(mx[k], g.pos[i * 3 + k]);
                }
            }
        }
        minX = mn[0];
        minY = mn[1];
        minZ = mn[2];
        maxX = mx[0];
        maxY = mx[1];
        maxZ = mx[2];
    }

    public int boneIndex(String n) {
        for (int i = 0; i < bones.length; i++) if (bones[i].name.equals(n)) return i;
        return -1;
    }

    public static HeistMesh read(String name, InputStream raw) throws IOException {
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new GZIPInputStream(raw), 1 << 16))) {
            byte[] magic = new byte[4];
            in.readFully(magic);
            if (magic[0] != 'H' || magic[1] != 'M' || magic[2] != 'S' || magic[3] != 'H') throw new IOException("not a .hmesh");
            int version = in.readInt();
            if (version != 1) throw new IOException("unsupported .hmesh version " + version);
            Bone[] bones = new Bone[in.readInt()];
            for (int i = 0; i < bones.length; i++) {
                bones[i] = new Bone(in.readUTF(), in.readInt(), in.readFloat(), in.readFloat(), in.readFloat());
            }
            Group[] groups = new Group[in.readInt()];
            for (int gi = 0; gi < groups.length; gi++) {
                Group g = new Group();
                g.texture = new Identifier(in.readUTF());
                g.flags = in.readInt();
                g.r = in.readFloat();
                g.g = in.readFloat();
                g.b = in.readFloat();
                g.a = in.readFloat();
                int n = in.readInt();
                g.count = n;
                g.pos = new float[n * 3];
                g.uv = new float[n * 2];
                g.nrm = new float[n * 3];
                g.bone = new byte[n];
                g.weight = new float[n];
                for (int i = 0; i < n; i++) {
                    g.pos[i * 3] = in.readFloat();
                    g.pos[i * 3 + 1] = in.readFloat();
                    g.pos[i * 3 + 2] = in.readFloat();
                    g.uv[i * 2] = in.readFloat();
                    g.uv[i * 2 + 1] = in.readFloat();
                    g.nrm[i * 3] = in.readByte() / 127f;
                    g.nrm[i * 3 + 1] = in.readByte() / 127f;
                    g.nrm[i * 3 + 2] = in.readByte() / 127f;
                    g.bone[i] = in.readByte();
                    g.weight[i] = (in.readByte() & 0xff) / 255f;
                }
                groups[gi] = g;
            }
            return new HeistMesh(name, bones, groups);
        }
    }
}
