package com.splunk.sharedmc.loggable_events;

/**
 * A sampled snapshot of server performance metrics.
 */
public class LoggablePerformanceEvent extends AbstractLoggableEvent {

    public LoggablePerformanceEvent(long gameTime) {
        super(LoggableEventType.PERFORMANCE, gameTime, null);
        this.addField(ACTION, "performance_sample");
    }

    public LoggablePerformanceEvent setTps(double tps) {
        this.addField("tps", tps);
        return this;
    }

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
