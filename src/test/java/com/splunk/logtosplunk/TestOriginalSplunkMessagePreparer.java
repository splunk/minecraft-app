package com.splunk.logtosplunk;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.splunk.logtosplunk.actions.BlockEventAction;
import com.splunk.logtosplunk.actions.DeathEventAction;
import com.splunk.logtosplunk.actions.PlayerEventAction;
import com.splunk.logtosplunk.loggable_events.LoggableBlockEvent;
import com.splunk.logtosplunk.loggable_events.LoggableDeathEvent;
import com.splunk.logtosplunk.loggable_events.LoggablePlayerEvent;

import net.minecraft.util.Vec3;


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
        writeEvent(PlayerEventAction.PLAYER_CONNECT,new Vec3(10, 10, 10), null);
        final String expected = "action=player_connect player=Bro! world=woName x=10.0 y=10.0 z=10.0 game_time=1000";
        assertEquals(expected, spy.getMessage());
    }

    @Test
    public void testPlayerLogoutEvent() {
        writeEvent(PlayerEventAction.PLAYER_DISCONNECT, new Vec3(10, 10, 10), null);
        final String expected = "action=player_disconnect player=Bro! world=woName x=10.0 y=10.0 z=10.0 game_time=1000";
        assertEquals(expected, spy.getMessage());
    }

    @Test
    public void testPlayerChatEvent() {
        writeEvent(PlayerEventAction.CHAT, new Vec3(10, 10, 10), "stuffit");
        final String expected = "action=chat player=Bro! world=woName x=10.0 y=10.0 z=10.0 game_time=1000 message=\"stuffit\"";
        assertEquals(expected, spy.getMessage());
    }

    @Test
    public void testPlayerMoveLogging(){
        writeEvent(PlayerEventAction.LOCATION, new Vec3(10,10,10),null);
        assertEquals(null,spy.getMessage());

        writeEvent(PlayerEventAction.LOCATION, new Vec3(10,10,10),null);

        final String expected = "action=move player=Bro! world=woName from_x=10.0 from_y=10.0 from_z=10.0 to_x=10.0 to_y=10.0 to_z=10.0 game_time=1000";
        assertEquals(expected,spy.getMessage());

        writeEvent(PlayerEventAction.LOCATION, new Vec3(11,11,11),null);
        final String expected2 = "action=move player=Bro! world=woName from_x=10.0 from_y=10.0 from_z=10.0 to_x=11.0 to_y=11.0 to_z=11.0 game_time=1000";
        assertEquals(expected2 ,spy.getMessage());

        writeEvent(PlayerEventAction.LOCATION, new Vec3(12,13,14),null);
        final String expected3 = "action=move player=Bro! world=woName from_x=11.0 from_y=11.0 from_z=11.0 to_x=12.0 to_y=13.0 to_z=14.0 game_time=1000";
        assertEquals(expected3 ,spy.getMessage());
    }

    @Test
    public void testBlockEventLogging(){
        writeEvent(BlockEventAction.BREAK);
        assertEquals("action=block_broken player=Bro! world=woName x=10.0 y=10.0 z=10.0 game_time=1000 block_type=block base_type=stone", spy.getMessage());

        writeEvent(BlockEventAction.PLACE);
        assertEquals("action=block_placed player=Bro! world=woName x=10.0 y=10.0 z=10.0 game_time=1000 block_type=block base_type=stone", spy.getMessage());
    }

    @Test
    public void testDeathEventLogging(){
        writeEvent(DeathEventAction.PLAYER_DIED);
        assertEquals("action=player_died victim=veectim killer=keeler damage_source=deemage_source world=woName x=10.0 y=10.0 z=10.0 game_time=1000", spy.getMessage());

    }

    private void writeEvent(DeathEventAction action) {
        LoggableDeathEvent event =
                new LoggableDeathEvent(action, 1000, "woName", new Vec3(10,10,10)).setVicitim("veectim").setKiller("keeler").setDamageSource("deemage_source");

        messagePreparer.writeMessage(event);
    }

    private void writeEvent(BlockEventAction action){
        LoggableBlockEvent event =
                new LoggableBlockEvent(action, 1000, "woName", new Vec3(10,10,10)).setPlayerName("Bro!").setBlockName("block").setBaseType("stone");

        messagePreparer.writeMessage(event);
    }
    private void writeEvent(PlayerEventAction action, Vec3 coords, String message) {
        LoggablePlayerEvent event =
                new LoggablePlayerEvent(action, 1000, "woName", coords).setPlayerName("Bro!");
        if (message != null) {
            event.setMessage(message);
        }
        messagePreparer.writeMessage(event);
    }
}
