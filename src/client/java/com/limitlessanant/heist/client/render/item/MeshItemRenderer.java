package com.limitlessanant.heist.client.render.item;

import com.limitlessanant.heist.client.mesh.HeistMesh;
import com.limitlessanant.heist.client.mesh.MeshManager;
import com.limitlessanant.heist.client.mesh.MeshRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders an item as its 3D mesh. The mesh is centred in the 1x1x1 item box and scaled so its largest side = {@code size};
 * models/item/<id>.json ("parent": "builtin/entity") holds the per-context display transforms.
 * {@code faceViewer}: props whose front (-Z) should face the camera in the inventory are turned 180 degrees;
 * guns keep -Z (barrel) pointing away from the camera = forward in first person.
 */
public class MeshItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    private final String mesh;
    private final float size;
    private final boolean faceViewer;

    public MeshItemRenderer(String mesh, float size, boolean faceViewer) {
        this.mesh = mesh;
        this.size = size;
        this.faceViewer = faceViewer;
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay) {
        HeistMesh m = MeshManager.get(mesh);
        if (m == null) return;
        float sx = m.maxX - m.minX, sy = m.maxY - m.minY, sz = m.maxZ - m.minZ;
        float s = size / Math.max(sx, Math.max(sy, sz));
        ms.push();
        ms.translate(0.5, 0.5, 0.5);
        if (faceViewer) ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        ms.scale(s, s, s);
        ms.translate(-(m.minX + m.maxX) / 2, -(m.minY + m.maxY) / 2, -(m.minZ + m.maxZ) / 2);
        MeshRenderer.Options o = new MeshRenderer.Options();
        o.time = MinecraftClient.getInstance().world == null ? 0 : MinecraftClient.getInstance().world.getTime();
        MeshRenderer.render(m, ms, vcp, light, overlay, o);
        ms.pop();
    }
}
