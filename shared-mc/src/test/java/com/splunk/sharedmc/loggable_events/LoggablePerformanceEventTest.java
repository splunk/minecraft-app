package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LoggablePerformanceEventTest {
    @Test
    public void sample_serializesMetrics() {
        LoggablePerformanceEvent e = new LoggablePerformanceEvent(0L);
        e.setTps(19.8).setMspt(8.4).setOnlinePlayers(12).setLoadedChunks(1500);
        String json = e.toJson();
        assertTrue(json.contains("tps"));
        assertTrue(json.contains("19.8"));
        assertTrue(json.contains("mspt"));
        assertTrue(json.contains("online_players"));
    }
}
