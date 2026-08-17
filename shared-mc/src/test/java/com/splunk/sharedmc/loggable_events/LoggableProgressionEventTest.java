package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LoggableProgressionEventTest {
    @Test
    public void levelUp_carriesNewLevel() {
        LoggableProgressionEvent e = new LoggableProgressionEvent(
                LoggableProgressionEvent.ProgressionAction.LEVEL_CHANGE, 0L, "world");
        e.setPlayerName("Alex").setNewLevel(30);
        String json = e.toJson();
        assertTrue(json.contains("level_change"));
        assertTrue(json.contains("new_level"));
        assertTrue(json.contains("30"));
    }

    @Test
    public void command_carriesCommandText() {
        LoggableProgressionEvent e = new LoggableProgressionEvent(
                LoggableProgressionEvent.ProgressionAction.COMMAND, 0L, "world");
        e.setPlayerName("Alex").setDetail("/gamemode creative");
        String json = e.toJson();
        assertTrue(json.contains("command"));
        assertTrue(json.contains("gamemode creative"));
    }
}
