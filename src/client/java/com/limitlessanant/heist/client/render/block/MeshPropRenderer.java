package com.limitlessanant.heist.client.render.block;

import com.limitlessanant.heist.block.MeshPropBlock;
import com.limitlessanant.heist.block.Shapes;
import com.limitlessanant.heist.blockentity.HackerTerminalBlockEntity;
import com.limitlessanant.heist.client.mesh.MeshManager;
import com.limitlessanant.heist.client.mesh.MeshRenderer;
import com.limitlessanant.heist.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Draws the converted 3D model of a mesh prop block (briefcase, Mona Lisa, hacker terminal). */
public class MeshPropRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private final TextRenderer text;

    public MeshPropRenderer(BlockEntityRendererFactory.Context ctx) {
        this.text = ctx.getTextRenderer();
    }

    public static String meshFor(BlockState s) {
        if (s.isOf(ModBlocks.BRIEFCASE)) return "briefcase";
        if (s.isOf(ModBlocks.MONA_LISA)) return "mona_lisa";
        if (s.isOf(ModBlocks.HACKER_TERMINAL)) return "hacker_terminal";
        if (s.isOf(ModBlocks.WEAPON_RACK)) return "weapon_rack";
        return null;
    }

    @Override
    public void render(T be, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay) {
        BlockState s = be.getCachedState();
        if (!(s.getBlock() instanceof MeshPropBlock prop)) return;
        ms.push();
        ms.translate(0.5, 0, 0.5);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(Shapes.yRotation(s.get(MeshPropBlock.FACING))));
        if (prop.isWallMounted()) ms.translate(0, 0, 0.5); // back of the model against the wall
        MeshRenderer.Options o = new MeshRenderer.Options();
        o.time = be.getWorld() == null ? 0 : be.getWorld().getTime() + tickDelta;
        int hack = be instanceof HackerTerminalBlockEntity h ? h.getHackTicks() : -1;
        if (hack >= 0) o.emissiveFlicker = (((int) o.time) % 4 < 2) ? 1.3f : 0.6f; // screens glitch while hacking
        MeshRenderer.render(MeshManager.get(meshFor(s)), ms, vcp, light, overlay, o);
        afterMesh(be, ms, vcp, light, overlay, tickDelta);
        if (hack >= 0) { // floating "HACKING" hologram
            ms.push();
            ms.translate(0, 2.7, 0);
            ms.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());
            ms.scale(-0.03f, -0.03f, 0.03f);
            String msg = "HACKING " + (hack * 100 / HackerTerminalBlockEntity.HACK_TIME) + "%";
            text.draw(msg, -text.getWidth(msg) / 2f, 0, 0x39ff6a, false, ms.peek().getPositionMatrix(), vcp,
                    TextRenderer.TextLayerType.SEE_THROUGH, 0x66000000, MeshRenderer.FULL_BRIGHT);
            ms.pop();
        }
        ms.pop();
    }

    /** Hook for subclasses (weapon rack items). Matrix is in the model frame (front = -Z). */
    protected void afterMesh(T be, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay, float tickDelta) {
    }

    @Override
    public boolean rendersOutsideBoundingBox(T be) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 128;
    }
}
