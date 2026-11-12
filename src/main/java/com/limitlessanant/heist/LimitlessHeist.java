package com.limitlessanant.heist;

import com.limitlessanant.heist.heist.HeistEvents;
import com.limitlessanant.heist.registry.ModBlockEntities;
import com.limitlessanant.heist.registry.ModBlocks;
import com.limitlessanant.heist.registry.ModEffects;
import com.limitlessanant.heist.registry.ModEntities;
import com.limitlessanant.heist.registry.ModItemGroup;
import com.limitlessanant.heist.registry.ModItems;
import com.limitlessanant.heist.world.HeistCommands;
import com.limitlessanant.heist.world.StructureBuilder;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LimitlessAnant's Black Market Heist - main (common) entrypoint.
 * <p>
 * Everything is registered from the static registry classes in {@code registry/}. Order matters:
 * effects -> blocks -> block entities -> items -> entities -> item group.
 */
public class LimitlessHeist implements ModInitializer {
    public static final String MOD_ID = "limitlessheist";
    public static final Logger LOG = LoggerFactory.getLogger("LimitlessHeist");

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModEffects.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModItems.init();
        ModEntities.init();
        ModItemGroup.init();
        HeistCommands.init();
        StructureBuilder.init();
        HeistEvents.init();
        LOG.info("Black Market is open. Welcome, Limitless.");
    }
}
