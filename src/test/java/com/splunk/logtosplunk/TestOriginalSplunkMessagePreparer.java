package com.splunk.logtosplunk;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.splunk.logtosplunk.actions.PlayerEventAction;
import com.splunk.logtosplunk.loggable_events.LoggablePlayerEvent;

import net.minecraft.util.Vec3;

public class TestOriginalSplunkMessagePreparer {

    private OriginalSplunkMessagePreparer messagePreparer;
    private SplunkConnectorSpy spy;

    @Before
    public void setUp() {
        spy = new SplunkConnectorSpy();
        messagePreparer = new OriginalSplunkMessagePreparer(spy);
    }

    @Test
    public void testPlayerLoginEvent() {
        writeEvent(PlayerEventAction.PLAYER_CONNECT, null);
        final String expected = "action=player_connect player=Bro! world=woName x=10.0 y=10.0 z=10.0 game_time=1000";
        assertEquals(expected, spy.getMessage());
    }

    @Test
    public void testPlayerLogoutEvent() {
        writeEvent(PlayerEventAction.PLAYER_DISCONNECT, null);
        final String expected = "action=player_disconnect player=Bro! world=woName x=10.0 y=10.0 z=10.0 game_time=1000";
        assertEquals(expected, spy.getMessage());
    }

    @Test
    public void testPlayerChatEvent() {
        writeEvent(PlayerEventAction.CHAT, "stuffit");
        final String expected = "action=chat player=Bro! world=woName x=10.0 y=10.0 z=10.0 game_time=1000 message=\"stuffit\"";
        assertEquals(expected, spy.getMessage());
    }

    private void writeEvent(PlayerEventAction action, String message) {
        LoggablePlayerEvent event =
                new LoggablePlayerEvent(action, 1000, "woName", new Vec3(10, 10, 10)).setPlayerName("Bro!");
        if (message != null) {
            event.setMessage(message);
        }
        messagePreparer.writeMessage(event);
    }
}
