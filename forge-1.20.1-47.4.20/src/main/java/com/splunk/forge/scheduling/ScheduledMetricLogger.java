package com.splunk.forge.scheduling;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Base for loggers driven by a timer rather than a game event. Mirrors the spigot module's
 * {@code ScheduledMetricLogger}, adapted to Forge: {@link #start(long)} samples on the main
 * server thread via the Forge tick event bus (subclass MUST register {@code this} on
 * {@code MinecraftForge.EVENT_BUS} for the tick handler to fire); {@link #startAsync(long)}
 * samples off-thread on a dedicated scheduled executor, for subclasses that do blocking I/O.
 */
public abstract class ScheduledMetricLogger extends AbstractEventLogger {

    /** Assumes a steady 20 ticks/second, matching vanilla server tick rate. */
    private static final long MS_PER_TICK = 50L;

    private long intervalTicks;
    private int tickCounter = 0;

    public ScheduledMetricLogger(Properties props) {
        super(props);
    }

    /** Called on each scheduled interval. Build and send the metric event(s) here. */
    protected abstract void sample();

    /**
     * Arms the tick-driven sampler. The caller must also register {@code this} instance on
     * {@code MinecraftForge.EVENT_BUS} so {@link #onServerTick(TickEvent.ServerTickEvent)} fires.
     */
    public void start(long intervalTicks) {
        this.intervalTicks = intervalTicks;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (intervalTicks <= 0 || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++tickCounter % intervalTicks != 0) {
            return;
        }
        runSampleSafely();
    }

    /**
     * Like {@link #start(long)} but runs {@link #sample()} on a dedicated background thread, so
     * blocking I/O (file reads, HTTP) never stalls a server tick. Subclasses scheduled this way
     * MUST NOT touch Minecraft world/entity state from {@link #sample()} (only safe on the main
     * thread).
     */
    public void startAsync(long intervalTicks) {
        long periodMs = Math.max(MS_PER_TICK, intervalTicks * MS_PER_TICK);
        ThreadFactory daemonFactory = r -> {
            Thread t = new Thread(r, "logtosplunk-scheduled-metric");
            t.setDaemon(true);
            return t;
        };
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(daemonFactory);
        executor.scheduleAtFixedRate(this::runSampleSafely, periodMs, periodMs, TimeUnit.MILLISECONDS);
    }

    private void runSampleSafely() {
        try {
            sample();
        } catch (Exception e) {
            logger.warn("Scheduled metric sample failed", e);
        }
    }
}
