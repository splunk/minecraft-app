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
        LoggablePlayerEvent event =
                new LoggablePlayerEvent(PlayerEventAction.PLAYER_CONNECT, 1000, "woName", new Vec3(10, 10, 10))
                        .setPlayerName("Bro!");
        messagePreparer.writeMessage(event);

        //Format of the original splunk message system is never going to change.
        final String expected = "action=player_connect player=Bro! world=woName x=10.0 y=10.0 z=10.0 game_time=1000";
        assertEquals(expected, spy.getMessage());
    }
}
