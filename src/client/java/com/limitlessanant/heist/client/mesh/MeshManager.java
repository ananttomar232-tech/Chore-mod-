package com.limitlessanant.heist.client.mesh;

import com.limitlessanant.heist.LimitlessHeist;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Loads every assets/<ns>/meshes/*.hmesh on resource reload (F3+T works, resource packs can override them). */
public final class MeshManager implements SimpleSynchronousResourceReloadListener {
    private static final Map<String, HeistMesh> MESHES = new HashMap<>();

    public static HeistMesh get(String name) {
        return MESHES.get(name);
    }

    @Override
    public Identifier getFabricId() {
        return LimitlessHeist.id("meshes");
    }

    @Override
    public void reload(ResourceManager manager) {
        MESHES.clear();
        for (Map.Entry<Identifier, Resource> e : manager.findResources("meshes", id -> id.getPath().endsWith(".hmesh")).entrySet()) {
            String path = e.getKey().getPath();
            String name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".hmesh".length());
            try (InputStream in = e.getValue().getInputStream()) {
                MESHES.put(name, HeistMesh.read(name, in));
            } catch (Exception ex) {
                LimitlessHeist.LOG.error("Failed to load mesh {}", e.getKey(), ex);
            }
        }
        LimitlessHeist.LOG.info("Loaded {} heist meshes: {}", MESHES.size(), MESHES.keySet());
    }
}
