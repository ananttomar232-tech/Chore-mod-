package com.limitlessanant.heist.client.render.entity;

import com.limitlessanant.heist.client.mesh.HeistMesh;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

/**
 * Procedural animation for the static unicorn mesh. The converter painted per-vertex weights for 8 bones; here we
 * build one matrix per bone.
 * <ul>
 *   <li>lean 0 = the majestic rearing pose of the original model (idle)</li>
 *   <li>lean 1 = body tilted forward ~45 deg around the hips: gallop / flight pose</li>
 * </ul>
 * Mirrored 1:1 in tools/preview_unicorn_pose.py for tuning.
 */
public final class UnicornPose {
    public static final float LEAN_DEG = 46f;
    public static final float LEAN_LIFT = 0.30f;   // raise the body a bit in the gallop pose so front hooves clear the ground

    /**
     * @param lean      0..1
     * @param gallop    0..1 how much the legs swing
     * @param phase     limb animation position
     * @param fly       0..1 wing flap strength
     * @param t         age in ticks (+tickDelta)
     * @param headYaw   head yaw relative to body, degrees
     * @param headPitch degrees
     */
    public static Matrix4f[] compute(HeistMesh mesh, float lean, float gallop, float phase, float fly, float t, float headYaw, float headPitch) {
        int n = mesh.bones.length;
        Matrix4f[] out = new Matrix4f[n];
        HeistMesh.Bone hipL = bone(mesh, "hind_left"), hipR = bone(mesh, "hind_right");
        float hx = 0, hy = (hipL.py() + hipR.py()) / 2, hz = (hipL.pz() + hipR.pz()) / 2;
        float leanRad = -lean * LEAN_DEG * MathHelper.RADIANS_PER_DEGREE;
        Matrix4f root = new Matrix4f()
                .translate(0, lean * LEAN_LIFT + breath(t, lean) , 0)
                .translate(hx, hy, hz).rotateX(leanRad).translate(-hx, -hy, -hz);
        out[0] = root;
        float sw = MathHelper.sin(phase * 0.6662f) * 0.85f * gallop;
        float sw2 = MathHelper.sin(phase * 0.6662f + MathHelper.PI) * 0.85f * gallop;
        for (int i = 1; i < n; i++) {
            HeistMesh.Bone b = mesh.bones[i];
            Matrix4f m = new Matrix4f(root).translate(b.px(), b.py(), b.pz());
            switch (b.name()) {
                case "hind_left" -> m.rotateX(-leanRad * 0.92f + sw * 0.6f);
                case "hind_right" -> m.rotateX(-leanRad * 0.92f + sw2 * 0.6f);
                case "front_left" -> m.rotateX(lean * 0.35f + sw2 * 0.7f + (1 - lean) * MathHelper.sin(t * 0.08f) * 0.12f);
                case "front_right" -> m.rotateX(lean * 0.35f + sw * 0.7f + (1 - lean) * MathHelper.sin(t * 0.08f + 1.7f) * 0.12f);
                case "wing_left", "wing_right" -> {
                    float side = b.name().equals("wing_left") ? 1 : -1;
                    float idle = MathHelper.sin(t * 0.07f) * 0.06f;
                    float flap = MathHelper.sin(t * 0.45f) * 0.62f * fly;
                    float fold = (1 - fly) * lean * 0.35f;  // tuck the wings back while galloping
                    m.rotateY(side * fold).rotateZ(side * (idle + flap - fold * 0.4f));
                }
                case "tail" -> m.rotateX(MathHelper.sin(t * 0.11f) * 0.12f + gallop * 0.25f - lean * 0.3f)
                        .rotateY(MathHelper.sin(t * 0.07f) * 0.18f);
                case "head" -> m.rotateX((lean * 0.55f) - headPitch * MathHelper.RADIANS_PER_DEGREE * 0.6f)
                        .rotateY(MathHelper.clamp(headYaw, -40, 40) * MathHelper.RADIANS_PER_DEGREE * 0.7f);
                default -> {
                }
            }
            out[i] = m.translate(-b.px(), -b.py(), -b.pz());
        }
        return out;
    }

    private static float breath(float t, float lean) {
        return MathHelper.sin(t * 0.06f) * 0.02f;
    }

    private static HeistMesh.Bone bone(HeistMesh mesh, String name) {
        int i = mesh.boneIndex(name);
        return mesh.bones[Math.max(i, 0)];
    }

    private UnicornPose() {
    }
}
