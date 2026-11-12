package com.limitlessanant.heist.world;

import com.limitlessanant.heist.registry.ModEffects;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * <pre>
 * /heist build black_market     colossal cavern ~58 blocks below you + secret shaft up to where you stand
 * /heist build museum           Mission 1 (north of you)
 * /heist build stable           Mission 2 (north of you)
 * /heist build dragon_vault     Mission 3 (north of you)
 * /heist build bunker_gold | bunker_diamond | bunker_netherite
 * /heist wanted clear           remove the WANTED status from yourself
 * </pre>
 */
public final class HeistCommands {
    private static final List<String> STRUCTURES = List.of("black_market", "museum", "stable", "dragon_vault",
            "bunker_gold", "bunker_diamond", "bunker_netherite");

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(
                CommandManager.literal("heist").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.literal("build")
                                .then(CommandManager.argument("structure", StringArgumentType.word())
                                        .suggests((ctx, b) -> CommandSource.suggestMatching(STRUCTURES, b))
                                        .executes(HeistCommands::build)))
                        .then(CommandManager.literal("wanted").then(CommandManager.literal("clear").executes(ctx -> {
                            ctx.getSource().getPlayerOrThrow().removeStatusEffect(ModEffects.WANTED);
                            ctx.getSource().sendFeedback(() -> Text.literal("You are no longer wanted.").formatted(Formatting.GREEN), false);
                            return 1;
                        })))));
    }

    private static int build(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String what = StringArgumentType.getString(ctx, "structure");
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity p = src.getPlayerOrThrow();
        ServerWorld w = src.getWorld();
        BlockPos here = p.getBlockPos();
        Canvas c;
        switch (what) {
            case "black_market" -> {
                int floorY = Math.max(w.getBottomY() + 8, here.getY() - 58);
                // cavern centre 48 blocks north of you: the shaft at your feet lands at the south edge, facing the tower door
                BlockPos origin = new BlockPos(here.getX(), floorY, here.getZ() - 48);
                c = BlackMarketGenerator.build(w, origin, here.getY(), 0, 48);
            }
            case "museum" -> c = MissionGenerators.museum(w, here.north(3));
            case "stable" -> c = MissionGenerators.stable(w, here.north(3));
            case "dragon_vault" -> c = MissionGenerators.dragonVault(w, here.north(2));
            case "bunker_gold" -> c = MissionGenerators.bunker(w, here.north(2), MissionGenerators.Tier.GOLD);
            case "bunker_diamond" -> c = MissionGenerators.bunker(w, here.north(2), MissionGenerators.Tier.DIAMOND);
            case "bunker_netherite" -> c = MissionGenerators.bunker(w, here.north(2), MissionGenerators.Tier.NETHERITE);
            default -> {
                src.sendError(Text.literal("Unknown structure. Try: " + String.join(", ", STRUCTURES)));
                return 0;
            }
        }
        c.submit(src, what);
        return 1;
    }

    private HeistCommands() {
    }
}
