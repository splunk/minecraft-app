package com.splunk.logtosplunk.event_loggers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mojang.authlib.GameProfile;
import com.splunk.logtosplunk.SplunkMessagePreparerSpy;
import com.splunk.logtosplunk.actions.PlayerEventAction;
import com.splunk.logtosplunk.loggable_events.LoggablePlayerEvent;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class TestAllPlayerEventLoggers {

    private PlayerEventLogger logger;
    private PlayerMovementEventLogger moveLogger;

    private SplunkMessagePreparerSpy spy;

    /**
     * For login logout event.
     */
    @Mock
    EntityPlayer player = mock(EntityPlayer.class);

    /**
     * For chat event.
     */
    EntityPlayerMP mPlayer = mock(EntityPlayerMP.class);

    @InjectMocks
    private PlayerEvent.PlayerLoggedInEvent loggedInEvent = mock(PlayerEvent.PlayerLoggedInEvent.class);

    @InjectMocks
    private PlayerEvent.PlayerLoggedOutEvent loggedOutEvent = mock(PlayerEvent.PlayerLoggedOutEvent.class);

    /**
     * For movement.
     */
    @InjectMocks
    private LivingEvent.LivingUpdateEvent updateEvent = mock(LivingEvent.LivingUpdateEvent.class);

    @Before
    public void setUp() {
        spy = new SplunkMessagePreparerSpy();
        logger = new PlayerEventLogger(spy);
        moveLogger = new PlayerMovementEventLogger(spy);

        MockitoAnnotations.initMocks(this);
        when(player.getName()).thenReturn("Bro!");
        when(player.getPositionVector()).thenReturn(new Vec3(10, 10, 10));

        when(mPlayer.getName()).thenReturn("Bro!");
        when(mPlayer.getPositionVector()).thenReturn(new Vec3(10, 10, 10));

        GameProfile gameProfile = mock(GameProfile.class);
        when(mPlayer.getGameProfile()).thenReturn(gameProfile);

        final World world = mock(World.class);
        when(player.getEntityWorld()).thenReturn(world);
        when(mPlayer.getEntityWorld()).thenReturn(world);

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

    @Test
    public void testOnPlayerChat() {
        LoggablePlayerEvent expected =
                getExpectedLoggablePlayerEvent((PlayerEventAction.CHAT)).setMessage("message").setPlayerName("Bro!");

        ServerChatEvent chatEvent = new ServerChatEvent(mPlayer, "message", null);
        logger.onPlayerChat(chatEvent);
        assertEquals(expected, spy.getLoggable());
    }

    @Test
    public void testOnPlayerStatusReported_move() {
        LoggablePlayerEvent expected =
                getExpectedLoggablePlayerEvent((PlayerEventAction.LOCATION)).setPlayerName("Bro!");

        moveLogger.onPlayerStatusReported(updateEvent);
        assertEquals(expected, spy.getLoggable());
    }

    @Test
    public void testOnPlayerStatusReported_moveALittle() {
        LoggablePlayerEvent expected =
                getExpectedLoggablePlayerEvent((PlayerEventAction.LOCATION)).setPlayerName("Bro!");

        moveLogger.onPlayerStatusReported(updateEvent);
        when(player.getPositionVector()).thenReturn(new Vec3(10.1, 10.1, 10.1));
        moveLogger.onPlayerStatusReported(updateEvent);

        assertEquals(expected, spy.getLoggable());
    }

    @Test
    public void testOnPlayerStatusReported_moveALittleMore() {
        LoggablePlayerEvent expected =
                getExpectedLoggablePlayerEvent((PlayerEventAction.LOCATION), new Vec3(11, 11, 11))
                        .setPlayerName("Bro!");

        moveLogger.onPlayerStatusReported(updateEvent);
        when(player.getPositionVector()).thenReturn(new Vec3(11, 11, 11));
        moveLogger.onPlayerStatusReported(updateEvent);

        assertEquals(expected, spy.getLoggable());
    }

    private LoggablePlayerEvent getExpectedLoggablePlayerEvent(PlayerEventAction action) {
        return new LoggablePlayerEvent(action, 1000, "WoName", new Vec3(10, 10, 10)).setPlayerName("Bro!");
    }

    private LoggablePlayerEvent getExpectedLoggablePlayerEvent(PlayerEventAction action, Vec3 coords) {
        return new LoggablePlayerEvent(action, 1000, "WoName", new Vec3(11, 11, 11)).setPlayerName("Bro!");
    }
}
