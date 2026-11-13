package com.limitlessanant.heist.client.dev;

import com.limitlessanant.heist.LimitlessHeist;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;

import java.io.File;
import java.util.ArrayDeque;
import java.util.function.BooleanSupplier;

/**
 * DEV TOOL (inactive unless started with -Dlimitlessheist.showcase=true): creates a creative world, builds every
 * structure, sets up a prop gallery and saves screenshots to run/screenshots/. Used to check the mod renders correctly
 * without a human at the keyboard - and handy as a starting point for planning video shots.
 */
public final class ShowcaseDirector {
    private static final ArrayDeque<Runnable> STEPS = new ArrayDeque<>();
    private static BooleanSupplier waitFor = () -> true;
    private static int waitTicks;
    private static boolean started;
    private static volatile String lastReady = "";
    private static BlockPos base;

    public static void init() {
        if (!Boolean.getBoolean("limitlessheist.showcase")) return;
        LimitlessHeist.LOG.info("ShowcaseDirector active");
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            String s = msg.getString();
            if (s.endsWith("is ready!")) lastReady = s;
            if (!overlay) LimitlessHeist.LOG.info("[chat] {}", s);
        });
        ClientTickEvents.END_CLIENT_TICK.register(ShowcaseDirector::tick);
        script();
    }

    private static void tick(MinecraftClient mc) {
        if (!started) {
            if (mc.currentScreen instanceof TitleScreen) {
                started = true;
                String name = "showcase_" + System.currentTimeMillis();
                GameRules rules = new GameRules();
                rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, null);
                rules.get(GameRules.DO_MOB_SPAWNING).set(false, null);
                rules.get(GameRules.DO_WEATHER_CYCLE).set(false, null);
                LevelInfo info = new LevelInfo(name, GameMode.CREATIVE, false, Difficulty.PEACEFUL, true, rules, DataConfiguration.SAFE_MODE);
                mc.createIntegratedServerLoader().createAndStart(name, info, new GeneratorOptions(20251112L, false, false),
                        WorldPresets::createDemoOptions);
            }
            return;
        }
        if (mc.player == null || mc.world == null) return;
        mc.player.getAbilities().flying = true; // stay where we teleport the camera
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (!waitFor.getAsBoolean()) return;
        waitFor = () -> true;
        Runnable r = STEPS.poll();
        if (r != null) r.run();
    }

    // ------------------------------------------------------------------ script helpers
    private static void step(Runnable r) {
        STEPS.add(r);
    }

    private static void wait(int ticks) {
        STEPS.add(() -> waitTicks = ticks);
    }

    private static void cmd(String c) {
        STEPS.add(() -> MinecraftClient.getInstance().player.networkHandler.sendChatCommand(c));
    }

    /** cmd with coordinates relative to the recorded base position: use {x},{y},{z} placeholders with offsets: {x+10}. */
    private static void rcmd(String c) {
        STEPS.add(() -> MinecraftClient.getInstance().player.networkHandler.sendChatCommand(expand(c)));
    }

    private static final java.util.regex.Pattern PLACEHOLDER = java.util.regex.Pattern.compile("\\{([xyz])([+-]\\d+)?}");

    private static String expand(String c) {
        java.util.regex.Matcher m = PLACEHOLDER.matcher(c);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            char axis = m.group(1).charAt(0);
            int v = axis == 'x' ? base.getX() : axis == 'y' ? base.getY() : base.getZ();
            if (m.group(2) != null) v += Integer.parseInt(m.group(2).replace("+", ""));
            m.appendReplacement(out, Integer.toString(v));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Sends /heist build and waits for its "is ready!" message (listener armed before the command). */
    private static void build(String structure) {
        STEPS.add(() -> {
            lastReady = "";
            MinecraftClient.getInstance().player.networkHandler.sendChatCommand("heist build " + structure);
            waitFor = () -> lastReady.startsWith(structure);
        });
    }

    private static void hud(boolean visible) {
        STEPS.add(() -> MinecraftClient.getInstance().options.hudHidden = !visible);
    }

    private static void shot(String name) {
        wait(60);
        STEPS.add(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            ScreenshotRecorder.saveScreenshot(new File(mc.runDirectory, ""), name + ".png", mc.getFramebuffer(),
                    t -> LimitlessHeist.LOG.info("[showcase] {}", t.getString()));
        });
        wait(5);
    }

    /** Teleport dx blocks east onto the real ground (height read from the client world once the chunk is loaded). */
    private static void groundTp(int dx) {
        STEPS.add(() -> MinecraftClient.getInstance().player.networkHandler.sendChatCommand("tp @s ~" + dx + " 300 ~"));
        wait(80);
        STEPS.add(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            BlockPos p = mc.player.getBlockPos();
            int y = mc.world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ());
            mc.player.networkHandler.sendChatCommand("tp @s " + p.getX() + " " + y + " " + p.getZ());
        });
        wait(20);
    }

    private static void markBase() {
        STEPS.add(() -> base = MinecraftClient.getInstance().player.getBlockPos());
    }

    // ------------------------------------------------------------------ the shot list
    private static void script() {
        wait(100);
        cmd("time set 6000");
        cmd("weather clear");
        cmd("gamerule sendCommandFeedback false");
        hud(false);
        markBase();

        boolean missionsOnly = "missions".equals(System.getProperty("limitlessheist.showcase.only"));
        if (!missionsOnly) {
        // ---------------- gallery in the sky (props, entities, items)
        cmd("tp @s ~ 200 ~");
        wait(20);
        markBase();
        rcmd("fill {x-12} {y-1} {z-12} {x+12} {y-1} {z+12} minecraft:polished_blackstone");
        rcmd("fill {x-12} {y} {z-12} {x+12} {y} {z-12} minecraft:black_concrete");
        rcmd("fill {x-12} {y+1} {z-12} {x+12} {y+6} {z-12} minecraft:black_concrete");
        rcmd("setblock {x-6} {y+2} {z-11} limitlessheist:neon_sign[facing=south]{Design:\"black_market\",Scale:1}");
        rcmd("setblock {x+5} {y+2} {z-11} limitlessheist:weapon_rack[facing=south]{Items:[{Slot:0b,id:\"limitlessheist:sci_fi_assault_rifle\",Count:1b},{Slot:1b,id:\"limitlessheist:freeze_ray_gun\",Count:1b},{Slot:2b,id:\"limitlessheist:grappling_hook_gun\",Count:1b}]}");
        rcmd("setblock {x-6} {y+5} {z-11} limitlessheist:neon_sign[facing=south]{Design:\"limitless\",Scale:1}");
        rcmd("setblock {x+8} {y} {z-8} limitlessheist:infinity_dragon_core");
        rcmd("setblock {x+3} {y} {z-6} limitlessheist:mystery_box[facing=south]");
        rcmd("setblock {x} {y} {z-6} minecraft:polished_blackstone_bricks");
        rcmd("setblock {x} {y+1} {z-6} limitlessheist:briefcase[facing=south]");
        rcmd("setblock {x-3} {y} {z-6} limitlessheist:laser_emitter[facing=east]");
        rcmd("setblock {x-1} {y} {z-11} limitlessheist:mona_lisa[facing=south]");
        rcmd("summon limitlessheist:vip_guard {x-8} {y} {z-4} {Rotation:[0f,0f],NoAI:1b}");
        rcmd("summon minecraft:armor_stand {x+6} {y} {z-3} {Rotation:[0f,0f],ShowArms:1b,ArmorItems:[{id:\"limitlessheist:lava_suit_boots\",Count:1b},{id:\"limitlessheist:lava_suit_leggings\",Count:1b},{id:\"limitlessheist:lava_suit_chestplate\",Count:1b},{id:\"limitlessheist:lava_suit_helmet\",Count:1b}],HandItems:[{id:\"limitlessheist:freeze_ray_gun\",Count:1b},{}]}");
        rcmd("summon minecraft:villager {x-3} {y} {z-2} {NoAI:1b,Rotation:[0f,0f]}");
        cmd("effect give @e[type=minecraft:villager] limitlessheist:frozen 100000 0");
        rcmd("tp @s {x} {y+3} {z+9} facing {x} {y+1} {z-8}");
        wait(80);
        shot("01_gallery_overview");
        rcmd("tp @s {x-8} {y+3} {z} facing {x-8} {y+2} {z-4}");
        shot("02_vip_guard");
        rcmd("tp @s {x+6} {y+2} {z} facing {x+6} {y+1} {z-3}");
        shot("03_lava_suit");
        rcmd("tp @s {x+5} {y+3} {z-6} facing {x+5} {y+3} {z-11}");
        shot("04_weapon_rack");
        rcmd("tp @s {x+5} {y+2} {z-3} facing {x+8} {y+1} {z-8}");
        shot("05_dragon_core");
        rcmd("tp @s {x+1} {y+2} {z-3} facing {x+1} {y+1} {z-6}");
        shot("06_briefcase_box_laser");
        rcmd("tp @s {x-1} {y+3} {z-5} facing {x-1} {y+3} {z-11}");
        shot("07_mona_lisa");
        rcmd("tp @s {x-3} {y+2} {z+2} facing {x-3} {y+1} {z-2}");
        shot("08_frozen_villager");
        rcmd("summon limitlessheist:unicorn {x} {y} {z+4} {Rotation:[90f,0f],NoAI:1b}");
        rcmd("tp @s {x+6} {y+2} {z+6} facing {x} {y+2} {z+4}");
        shot("09_unicorn_idle");
        hud(true);
        cmd("item replace entity @s weapon.mainhand with limitlessheist:freeze_ray_gun");
        rcmd("tp @s {x} {y+1} {z} facing {x} {y+1} {z-10}");
        shot("10_first_person_freeze_ray");
        cmd("item replace entity @s weapon.mainhand with limitlessheist:grappling_hook_gun");
        shot("11_first_person_grapple");
        cmd("item replace entity @s weapon.mainhand with limitlessheist:sci_fi_assault_rifle");
        shot("12_first_person_rifle");
        cmd("item replace entity @s weapon.mainhand with minecraft:air");
        String[] bar = {"freeze_ray_gun", "grappling_hook_gun", "sci_fi_assault_rifle", "crazy_apple", "mystery_box",
                "infinity_dragon_core", "lava_suit_helmet", "briefcase", "mona_lisa"};
        for (int i = 0; i < bar.length; i++) cmd("item replace entity @s hotbar." + i + " with limitlessheist:" + bar[i]);
        cmd("item replace entity @s hotbar.0 with limitlessheist:freeze_ray_gun");
        hud(true);
        rcmd("tp @s {x} {y+2} {z+12} facing {x} {y+1} {z-8}");
        shot("13_hotbar_icons");
        cmd("clear @s");
        hud(false);

        // ---------------- the black market
        groundTp(250);
        wait(40);
        markBase();
        build("black_market");
        wait(100);
        // centre = base + (0, floor - y, -48); floorY = y - 58 (clamped by the generator to bottom + 8)
        step(() -> base = new BlockPos(base.getX(), Math.max(-56, base.getY() - 58), base.getZ() - 48));
        rcmd("tp @s {x+34} {y+26} {z+42} facing {x} {y+14} {z}");
        wait(200);
        shot("20_market_overview");
        rcmd("tp @s {x} {y+3} {z+30} facing {x} {y+6} {z+15}");
        wait(80);
        shot("21_tower_entrance");
        rcmd("tp @s {x-4} {y+2} {z+4} facing {x} {y+1} {z-6}");
        shot("22_dealers_lounge");
        rcmd("tp @s {x} {y+11} {z-4} facing {x} {y+12} {z-14}");
        shot("23_weapon_cartel");
        rcmd("tp @s {x+2} {y+20} {z-3} facing {x+12} {y+19} {z-12}");
        shot("24_hackers_den");
        rcmd("tp @s {x-12} {y+5} {z-22} facing {x-22} {y+5} {z-38}");
        wait(60);
        shot("25_shops");
        rcmd("tp @s {x+20} {y+40} {z+30} facing {x} {y+36} {z}");
        shot("26_roof_signs");
        rcmd("tp @s {x} {y+2} {z+44} facing {x} {y+6} {z}");
        shot("27_from_the_shaft");

        }
        // ---------------- missions
        groundTp(400);
        wait(60);
        markBase();
        build("museum");
        wait(60);
        rcmd("tp @s {x} {y+3} {z+14} facing {x} {y+3} {z-3}");
        wait(80);
        shot("30_museum_outside");
        rcmd("tp @s {x} {y+3} {z-12} facing {x} {y+3} {z-36}");
        shot("31_museum_lasers_mona_lisa");
        rcmd("tp @s {x+14} {y+2} {z-6} facing {x+16} {y+1} {z-8}");
        shot("32_museum_security_room");

        groundTp(400);
        wait(60);
        markBase();
        build("stable");
        wait(60);
        rcmd("tp @s {x+10} {y+5} {z+6} facing {x} {y+2} {z-10}");
        wait(80);
        shot("40_stable_unicorn");
        rcmd("tp @s {x-3} {y+3} {z-25} facing {x-6} {y} {z-29}");
        shot("41_gotiya_sleeping");

        groundTp(400);
        wait(60);
        markBase();
        build("dragon_vault");
        wait(60);
        rcmd("tp @s {x+6} {y+5} {z-6} facing {x} {y+2} {z-17}");
        wait(80);
        shot("50_dragon_vault");

        groundTp(400);
        wait(60);
        markBase();
        build("bunker_netherite");
        rcmd("tp @s {x} {y+3} {z-2} facing {x} {y+1} {z-14}");
        wait(80);
        shot("60_bunker_netherite");
        step(() -> MinecraftClient.getInstance().scheduleStop());
    }

    private ShowcaseDirector() {
    }
}
