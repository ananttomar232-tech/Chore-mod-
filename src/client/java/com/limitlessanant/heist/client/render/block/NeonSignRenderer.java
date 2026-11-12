package com.limitlessanant.heist.client.render.block;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.block.NeonDesigns;
import com.limitlessanant.heist.block.NeonSignBlock;
import com.limitlessanant.heist.block.Shapes;
import com.limitlessanant.heist.blockentity.NeonSignBlockEntity;
import com.limitlessanant.heist.client.render.Fx;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Full-bright neon texture + additive halo pass. Wall: centred on the block, floor: stands on it, ceiling: hangs from it. */
public class NeonSignRenderer implements BlockEntityRenderer<NeonSignBlockEntity> {
    private static final Identifier[] TEX = new Identifier[NeonDesigns.count()];

    public NeonSignRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    public static Identifier texture(int design) {
        int i = Math.floorMod(design, NeonDesigns.count());
        if (TEX[i] == null) TEX[i] = LimitlessHeist.id("textures/block/neon/" + NeonDesigns.get(i).id() + ".png");
        return TEX[i];
    }

    /** 0..1 brightness for flickering signs: mostly on, with short realistic stutters. */
    public static float flicker(long seed, float time) {
        int tick = (int) time;
        long h = (tick / 3 + seed) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        int v = (int) (h & 1023);
        if (v < 40) return 0.12f;
        if (v < 70) return 0.55f;
        return 0.92f + 0.08f * MathHelper.sin(time * 1.7f);
    }

    @Override
    public void render(NeonSignBlockEntity be, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay) {
        BlockState s = be.getCachedState();
        if (!(s.getBlock() instanceof NeonSignBlock)) return;
        NeonDesigns.Design d = NeonDesigns.get(be.getDesign());
        float sc = be.getScale(), w = d.width() * sc, h = d.height() * sc;
        float time = be.getWorld() == null ? 0 : be.getWorld().getTime() + tickDelta;
        float br = be.isFlicker() ? flicker(be.getPos().asLong(), time) : 1f;
        WallMountLocation face = s.get(NeonSignBlock.FACE);
        ms.push();
        ms.translate(0.5, 0.5, 0.5);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(Shapes.yRotation(s.get(NeonSignBlock.FACING))));
        float y0 = switch (face) {
            case FLOOR -> -0.5f + h / 2;
            case CEILING -> 0.5f - h / 2;
            default -> 0f;
        };
        if (face == WallMountLocation.WALL) ms.translate(0, 0, 0.47);
        Identifier tex = texture(be.getDesign());
        Fx.glowQuad(ms, vcp, tex, w, h, 0, y0, br, br, br, Math.min(1, br + 0.1f), 0.85f * br, false);
        if (face != WallMountLocation.WALL) { // free-standing: readable from behind as well
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            ms.translate(0, 0, -0.01);
            Fx.glowQuad(ms, vcp, tex, w, h, 0, y0, br, br, br, Math.min(1, br + 0.1f), 0.85f * br, false);
        }
        ms.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(NeonSignBlockEntity be) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 160;
    }
}
