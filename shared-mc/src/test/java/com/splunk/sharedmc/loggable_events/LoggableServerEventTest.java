package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LoggableServerEventTest {
    @Test
    public void serverStart_serializesAction() {
        LoggableServerEvent e = new LoggableServerEvent(
                LoggableServerEvent.ServerAction.SERVER_START, 0L, null);
        String json = e.toJson();
        assertTrue(json.contains("server_start"));
    }

    @Test
    public void weatherChange_carriesState() {
        LoggableServerEvent e = new LoggableServerEvent(
                LoggableServerEvent.ServerAction.WEATHER_CHANGE, 1000L, "world");
        e.setWeather("storm");
        String json = e.toJson();
        assertTrue(json.contains("weather"));
        assertTrue(json.contains("storm"));
    }
}
