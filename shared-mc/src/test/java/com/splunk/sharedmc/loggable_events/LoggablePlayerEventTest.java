package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import com.splunk.sharedmc.Point3dLong;
import org.junit.Test;

public class LoggablePlayerEventTest {
    @Test
    public void connect_carriesSessionDetail() {
        LoggablePlayerEvent e = new LoggablePlayerEvent(
                LoggablePlayerEvent.PlayerEventAction.PLAYER_CONNECT, 0L, "world", new Point3dLong(0, 64, 0));
        e.setPlayerName("Steve")
         .setPlayerUuid("11111111-2222-3333-4444-555555555555")
         .setPlayerIp("203.0.113.7")
         .setProtocolVersion(767);
        String json = e.toJson();
        assertTrue(json.contains("uuid"));
        assertTrue(json.contains("203.0.113.7"));
        assertTrue(json.contains("protocol_version"));
    }

    @Test
    public void gamemodeChange_serializesAction() {
        LoggablePlayerEvent e = new LoggablePlayerEvent(
                LoggablePlayerEvent.PlayerEventAction.GAMEMODE_CHANGE, 0L, "world", new Point3dLong(0, 64, 0));
        e.setGamemode("CREATIVE");
        String json = e.toJson();
        assertTrue(json.contains("gamemode_change"));
        assertTrue(json.contains("CREATIVE"));
    }
}
