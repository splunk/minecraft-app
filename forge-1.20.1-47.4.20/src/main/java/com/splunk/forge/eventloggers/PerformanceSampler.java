package com.splunk.forge.eventloggers;

import java.util.Properties;

import com.splunk.forge.scheduling.ScheduledMetricLogger;
import com.splunk.sharedmc.loggable_events.LoggablePerformanceEvent;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Periodically samples server performance (online players, loaded chunks). Mirrors the
 * spigot module's {@code PerformanceSampler}; TPS/MSPT are omitted (see that class's javadoc
 * for why they're Bukkit/Paper-only there — Forge has no equivalent public API either, without
 * reaching into internal tick-time tracking not exposed by {@code MinecraftServer}).
 */
public class PerformanceSampler extends ScheduledMetricLogger {

    public PerformanceSampler(Properties props) {
        super(props);
    }

    @Override
    protected void sample() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        LoggablePerformanceEvent e = new LoggablePerformanceEvent(0L);
        e.setOnlinePlayers(server.getPlayerCount());
        int loaded = 0;
        for (ServerLevel level : server.getAllLevels()) {
            loaded += level.getChunkSource().getLoadedChunksCount();
        }
        e.setLoadedChunks(loaded);
        logAndSend(e);
    }
}
