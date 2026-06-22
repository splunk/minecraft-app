package com.splunk.spigot.eventloggers;

import java.util.Properties;

import org.bukkit.Bukkit;

import com.splunk.sharedmc.loggable_events.LoggablePerformanceEvent;
import com.splunk.spigot.scheduling.ScheduledMetricLogger;

/**
 * Periodically samples server performance (online players, loaded chunks).
 *
 * <p>NOTE: {@code Bukkit.getTPS()} and {@code Bukkit.getAverageTickTime()} are
 * Paper-only APIs and are not present on vanilla spigot-api (verified via
 * {@code javap} against spigot-api 1.21.10 — no {@code getTPS}/{@code AverageTickTime}
 * symbols found on {@code org.bukkit.Bukkit}). TPS/MSPT sampling is therefore omitted
 * here; only metrics available on the Bukkit API are sampled.
 */
public class PerformanceSampler extends ScheduledMetricLogger {

    public PerformanceSampler(Properties props) {
        super(props);
    }

    @Override
    protected void sample() {
        LoggablePerformanceEvent e = new LoggablePerformanceEvent(0L);
        e.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
        int loaded = 0;
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            loaded += w.getLoadedChunks().length;
        }
        e.setLoadedChunks(loaded);
        logAndSend(e);
    }
}
