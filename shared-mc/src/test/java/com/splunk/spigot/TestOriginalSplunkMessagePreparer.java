package com.splunk.spigot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.splunk.sharedmc.BasicSplunkMessagePreparer;
import com.splunk.sharedmc.Point3dLong;
import com.splunk.sharedmc.loggable_events.LoggableBlockEvent;
import com.splunk.sharedmc.loggable_events.LoggableBlockEvent.BlockEventAction;
import com.splunk.sharedmc.loggable_events.LoggableDeathEvent;
import com.splunk.sharedmc.loggable_events.LoggableDeathEvent.DeathEventAction;
import com.splunk.sharedmc.loggable_events.LoggablePlayerEvent;
import com.splunk.sharedmc.loggable_events.LoggablePlayerEvent.PlayerEventAction;


public class TestOriginalSplunkMessagePreparer {

    private BasicSplunkMessagePreparer messagePreparer;
    private SplunkConnectionSpy spy;

    @Before
    public void setUp() {
        spy = new SplunkConnectionSpy();
        messagePreparer = new BasicSplunkMessagePreparer(spy);
    }

    @Test
    public void testPlayerLoginEvent() {
        writeEvent(PlayerEventAction.PLAYER_CONNECT,new Point3dLong(10, 10, 10), null);
        final String expected = "action=player_connect player=Bro! world=woName x=10 y=10 z=10 game_time=1000";
        assertEquals(expected, spy.getMessage());
    }

    @Test
    public void testPlayerLogoutEvent() {
        writeEvent(PlayerEventAction.PLAYER_DISCONNECT, new Point3dLong(10, 10, 10), null);
        final String expected = "action=player_disconnect player=Bro! world=woName x=10 y=10 z=10 game_time=1000";
        assertEquals(expected, spy.getMessage());
    }

    @Test
    public void testPlayerChatEvent() {
        writeEvent(PlayerEventAction.CHAT, new Point3dLong(10, 10, 10), "stuffit");
        final String expected = "action=chat player=Bro! world=woName x=10 y=10 z=10 game_time=1000 message=\"stuffit\"";
        assertEquals(expected, spy.getMessage());
    }

    @Test
    public void testPlayerMoveLogging(){
        writeEvent(PlayerEventAction.LOCATION, new Point3dLong(10,10,10),null);
        assertNull(spy.getMessage());

        writeEvent(PlayerEventAction.LOCATION, new Point3dLong(10,10,10),null);

        final String expected = "action=move player=Bro! world=woName from_x=10 from_y=10 from_z=10 to_x=10 to_y=10 to_z=10 game_time=1000";
        assertEquals(expected,spy.getMessage());

        writeEvent(PlayerEventAction.LOCATION, new Point3dLong(11,11,11),null);
        final String expected2 = "action=move player=Bro! world=woName from_x=10 from_y=10 from_z=10 to_x=11 to_y=11 to_z=11 game_time=1000";
        assertEquals(expected2 ,spy.getMessage());

        writeEvent(PlayerEventAction.LOCATION, new Point3dLong(12,13,14),null);
        final String expected3 = "action=move player=Bro! world=woName from_x=11 from_y=11 from_z=11 to_x=12 to_y=13 to_z=14 game_time=1000";
        assertEquals(expected3 ,spy.getMessage());
    }

    @Test
    public void testBlockEventLogging(){
        writeEvent(BlockEventAction.BREAK);
        assertEquals("action=block_broken player=Bro! world=woName x=10 y=10 z=10 game_time=1000 block_type=block base_type=stone", spy.getMessage());

        writeEvent(BlockEventAction.PLACE);
        assertEquals("action=block_placed player=Bro! world=woName x=10 y=10 z=10 game_time=1000 block_type=block base_type=stone", spy.getMessage());
    }

    @Test
    public void testDeathEventLogging(){
        writeEvent(DeathEventAction.PLAYER_DIED);
        assertEquals("action=player_died victim=veectim killer=keeler damage_source=deemage_source world=woName x=10 y=10 z=10 game_time=1000", spy.getMessage());

    }

    private void writeEvent(DeathEventAction action) {
        LoggableDeathEvent event =
                new LoggableDeathEvent(action, 1000, "woName", new Point3dLong(10,10,10)).setVictim("veectim").setKiller("keeler").setDamageSource("deemage_source");

        messagePreparer.writeMessage(event);
    }

    private void writeEvent(BlockEventAction action){
        LoggableBlockEvent event =
                new LoggableBlockEvent(action, 1000, "woName", new Point3dLong(10,10,10)).setPlayerName("Bro!").setBlockName("block").setBaseType("stone");

        messagePreparer.writeMessage(event);
    }
    private void writeEvent(PlayerEventAction action, Point3dLong coords, String message) {
        LoggablePlayerEvent event =
                new LoggablePlayerEvent(action, 1000, "woName", coords).setPlayerName("Bro!");
        if (message != null) {
            event.setMessage(message);
        }
        messagePreparer.writeMessage(event);
    }
}
