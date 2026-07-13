package com.splunk.spigot.scheduling;

import java.util.Properties;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;

/**
 * Base for loggers driven by the Bukkit scheduler rather than the event bus.
 * Subclasses implement {@link #sample()}; {@link #start(Plugin, long)} schedules it.
 */
public abstract class ScheduledMetricLogger extends AbstractEventLogger {

    public ScheduledMetricLogger(Properties props) {
        super(props);
    }

    /** Called on each scheduled tick. Build and send the metric event(s) here. */
    protected abstract void sample();

    /** Schedules {@link #sample()} every {@code intervalTicks} ticks on the main server thread. */
    public void start(Plugin plugin, long intervalTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    sample();
                } catch (Exception e) {
                    logger.warn("Scheduled metric sample failed", e);
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    /**
     * Like {@link #start(Plugin, long)} but runs {@link #sample()} off the main server thread.
     * Use this when the sample does blocking I/O (file reads, HTTP) so it never stalls a tick.
     * Subclasses scheduled this way MUST NOT touch the Bukkit world/entity API from
     * {@link #sample()} (those calls are only safe on the main thread).
     */
    public void startAsync(Plugin plugin, long intervalTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    sample();
                } catch (Exception e) {
                    logger.warn("Scheduled metric sample failed", e);
                }
            }
        }.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
    }
}
