package com.splunk.logtosplunk.event_loggers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.splunk.logtosplunk.SplunkMessagePreparerSpy;
import com.splunk.logtosplunk.actions.PlayerEventAction;
import com.splunk.logtosplunk.loggable_events.LoggablePlayerEvent;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class TestPlayerEventLogger {

    private PlayerEventLogger logger;
    private SplunkMessagePreparerSpy spy;

    @Mock
    EntityPlayer player = mock(EntityPlayer.class);

    @InjectMocks
    private PlayerEvent.PlayerLoggedInEvent loggedInEvent = mock(PlayerEvent.PlayerLoggedInEvent.class);

    @InjectMocks
    private PlayerEvent.PlayerLoggedOutEvent loggedOutEvent = mock(PlayerEvent.PlayerLoggedOutEvent.class);

    @Before
    public void setUp() {
        spy = new SplunkMessagePreparerSpy();
        logger = new PlayerEventLogger(spy);
        MockitoAnnotations.initMocks(this);
        when(player.getName()).thenReturn("Bro!");
        when(player.getPositionVector()).thenReturn(new Vec3(10, 10, 10));

        final World world = mock(World.class);
        when(player.getEntityWorld()).thenReturn(world);
        when(world.getWorldTime()).thenReturn(1000L);

        WorldInfo info = mock(WorldInfo.class);
        when(info.getWorldName()).thenReturn("WoName");
        when(world.getWorldInfo()).thenReturn(info);
    }

    @Test
    public void testOnPlayerLogin() {
        LoggablePlayerEvent expected = getExpectedLoggablePlayerEvent(PlayerEventAction.PLAYER_CONNECT);
        logger.onPlayerConnect(loggedInEvent);

        assertEquals(expected, spy.getLoggable());
    }

    @Test
    public void testOnPlayerLogout() {
        LoggablePlayerEvent expected = getExpectedLoggablePlayerEvent(PlayerEventAction.PLAYER_DISCONNECT);
        logger.onPlayerDisconnect(loggedOutEvent);

        assertEquals(expected, spy.getLoggable());
    }

    private LoggablePlayerEvent getExpectedLoggablePlayerEvent(PlayerEventAction action) {
        return new LoggablePlayerEvent(action, 1000,"WoName",new Vec3(10, 10, 10))
                .setPlayerName("Bro!");
    }
}
