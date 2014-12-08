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
        writeEvent(PlayerEventAction.PLAYER_CONNECT);
        final String expected = "action=player_connect player=Bro! world=woName x=10.0 y=10.0 z=10.0 game_time=1000";
        assertEquals(expected, spy.getMessage());
    }

    @Test
    public void testPlayerLogoutEvent() {
        writeEvent(PlayerEventAction.PLAYER_DISCONNECT);
        final String expected = "action=player_disconnect player=Bro! world=woName x=10.0 y=10.0 z=10.0 game_time=1000";
        assertEquals(expected, spy.getMessage());
    }

    private void writeEvent(PlayerEventAction action) {
        LoggablePlayerEvent event =
                new LoggablePlayerEvent(action, 1000, "woName", new Vec3(10, 10, 10))
                        .setPlayerName("Bro!");
        messagePreparer.writeMessage(event);
    }
}
