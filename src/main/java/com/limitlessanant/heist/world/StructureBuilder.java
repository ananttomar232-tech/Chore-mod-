package com.limitlessanant.heist.world;

import com.limitlessanant.heist.LimitlessHeist;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Runs {@link Canvas} build jobs a slice per tick (BLOCKS_PER_TICK) so even the colossal Black Market cavern
 * (~1M blocks) builds without freezing the server. Progress is reported to whoever ran the command.
 */
public final class StructureBuilder {
    public static final int BLOCKS_PER_TICK = 60_000;
    private static final ArrayDeque<Job> JOBS = new ArrayDeque<>();

    /** One unit of work. Returns true when finished; must subtract the work it did from budget[0]. */
    public interface Op {
        boolean work(ServerWorld world, int[] budget);

        default long size() {
            return 1;
        }
    }

    private static final class Job {
        final ServerWorld world;
        final List<Op> ops;
        final ServerCommandSource source;
        final String name;
        final long total;
        int index;
        long done;
        int lastPct = -1;

        Job(ServerWorld world, List<Op> ops, ServerCommandSource source, String name) {
            this.world = world;
            this.ops = ops;
            this.source = source;
            this.name = name;
            long t = 0;
            for (Op o : ops) t += o.size();
            this.total = Math.max(1, t);
        }
    }

    public static void enqueue(ServerWorld world, List<Op> ops, ServerCommandSource source, String name) {
        JOBS.add(new Job(world, ops, source, name));
        tell(source, Text.literal("Building " + name + " (" + ops.size() + " steps)...").formatted(Formatting.LIGHT_PURPLE));
    }

    /** Messages the player directly (not affected by the sendCommandFeedback gamerule). */
    private static void tell(ServerCommandSource source, Text msg) {
        if (source == null) return;
        if (source.getPlayer() != null) source.getPlayer().sendMessage(msg, false);
        else source.sendFeedback(() -> msg, false);
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Job job = JOBS.peek();
            if (job == null) return;
            int[] budget = {BLOCKS_PER_TICK};
            try {
                while (budget[0] > 0 && job.index < job.ops.size()) {
                    Op op = job.ops.get(job.index);
                    int before = budget[0];
                    if (op.work(job.world, budget)) {
                        job.index++;
                        budget[0] -= 1;
                    }
                    job.done += Math.max(0, before - budget[0]);
                }
            } catch (Exception e) {
                LimitlessHeist.LOG.error("Structure build failed", e);
                if (job.source != null) job.source.sendError(Text.literal("Build failed: " + e));
                JOBS.poll();
                return;
            }
            int pct = (int) Math.min(100, job.done * 100 / job.total);
            if (job.source != null && pct / 10 != job.lastPct / 10 && job.index < job.ops.size()) {
                job.lastPct = pct;
                tell(job.source, Text.literal(job.name + ": " + pct + "%").formatted(Formatting.DARK_PURPLE));
            }
            if (job.index >= job.ops.size()) {
                JOBS.poll();
                tell(job.source, Text.literal(job.name + " is ready!").formatted(Formatting.GOLD, Formatting.BOLD));
            }
        });
    }

    private StructureBuilder() {
    }
}
