package com.splunk.sharedmc.loggable_events;

/**
 * A sampled snapshot of server performance metrics. Unlike the other LoggableEvent types,
 * this is produced periodically by {@code ScheduledMetricLogger} polling rather than in
 * response to a Bukkit event.
 */
public class LoggablePerformanceEvent extends AbstractLoggableEvent {

    /**
     * Constructor.
     *
     * @param gameTime The in-game time at which this sample was taken.
     */
    public LoggablePerformanceEvent(long gameTime) {
        super(LoggableEventType.PERFORMANCE, gameTime, null);
        this.addField(ACTION, "performance_sample");
    }

    /**
     * Unused on vanilla spigot-api — {@code Bukkit.getTPS()} is a Paper-only API; retained
     * for when/if the project switches to paper-api.
     */
    public LoggablePerformanceEvent setTps(double tps) {
        this.addField("tps", tps);
        return this;
    }

    /**
     * Unused on vanilla spigot-api — {@code Bukkit.getAverageTickTime()} is a Paper-only
     * API; retained for when/if the project switches to paper-api.
     */
    public LoggablePerformanceEvent setMspt(double mspt) {
        this.addField("mspt", mspt);
        return this;
    }

    public LoggablePerformanceEvent setOnlinePlayers(int count) {
        this.addField("online_players", count);
        return this;
    }

    public LoggablePerformanceEvent setLoadedChunks(int chunks) {
        this.addField("loaded_chunks", chunks);
        return this;
    }
}
