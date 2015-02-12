package com.splunk.logtosplunk.event_loggers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.splunk.logtosplunk.SplunkMessagePreparer;
import com.splunk.logtosplunk.loggable_events.LoggablePlayerEvent;
import com.splunk.logtosplunk.loggable_events.LoggablePlayerEvent.PlayerEventAction;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Handles the logging of player events.
 */
public class PlayerEventLogger extends AbstractEventLogger {
    public static final double GRANULARITY = 1.5;
    public static final int MAX_PLAYERS = 128;

    /**
     * Constructs a new PlayerEventLogger with the given SplunkMessagePreparer.
     *
     * @param splunkMessagePreparer Message preparer to send packaged raw messages to.
     */
    public PlayerEventLogger(SplunkMessagePreparer splunkMessagePreparer) {
        super(splunkMessagePreparer);
    }

    /**
     * Logs to Splunk when a player logs in.
     *
     * @param event The captured event.
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void onPlayerConnect(PlayerLoggedInEvent event) {
        logAndSend(
                generateLoggablePlayerEvent(event, PlayerEventAction.PLAYER_CONNECT, null, null));
    }

    /**
     * Logs to Splunk when a player logs out.
     *
     * @param event The captured event.
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void onPlayerDisconnect(PlayerLoggedOutEvent event) {
        logAndSend(
                generateLoggablePlayerEvent(
                        event, PlayerEventAction.PLAYER_DISCONNECT, null, null));
    }

    /**
     * Logs to Splunk when a player chats and what they chat.
     *
     * @param chatEvent The captured chat event.
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void onPlayerChat(ServerChatEvent chatEvent) {
        logAndSend(
                generateLoggablePlayerEvent(chatEvent, PlayerEventAction.CHAT, chatEvent.message));
    }

    private LoggablePlayerEvent generateLoggablePlayerEvent(
            PlayerEvent event, PlayerEventAction actionType, String reason, String message) {
        World world = event.player.getEntityWorld();
        final long worldTime = world.getWorldTime();
        final String worldName = world.getWorldInfo().getWorldName();
        final Vec3 coordinates = event.player.getPositionVector();
        final LoggablePlayerEvent loggable = new LoggablePlayerEvent(actionType, worldTime, worldName, coordinates);
        loggable.setPlayerName(event.player.getName());
        loggable.setReason(reason);
        loggable.setMessage(message);

        return loggable;
    }

    private LoggablePlayerEvent generateLoggablePlayerEvent(
            ServerChatEvent event, PlayerEventAction actionType, String message) {
        World world = event.player.getEntityWorld();
        final long worldTime = world.getWorldTime();
        final String worldName = world.getWorldInfo().getWorldName();
        final Vec3 coordinates = event.player.getPositionVector();
        final LoggablePlayerEvent loggable = new LoggablePlayerEvent(actionType, worldTime, worldName, coordinates);
        loggable.setPlayerName(event.player.getName());
        loggable.setMessage(message);

        return loggable;
    }

    /**
     * Living update seems to get called about 10x/sec. We check if the update belongs to a player and if so we check if
     * the players position has changed significantly based on {@code GRANULARITY}.
     *
     * @param playerMove The captured event.
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void onPlayerStatusReported(LivingUpdateEvent playerMove) {
        if (playerMove.entity instanceof EntityPlayer) {
            final String playerName = playerMove.entity.getName();
            final Vec3 coordinates = playerMove.entity.getPositionVector();

            //Don't log if position hasn't changed significantly.
            Vec3 lastCoords = lastKnownCoordinates.getIfPresent(playerName);
            if (lastCoords != null && coordinates.distanceTo(lastCoords) < GRANULARITY) {
                return;
            }

            lastKnownCoordinates.put(playerName, coordinates);
            World world = playerMove.entity.getEntityWorld();
            final long worldTime = world.getWorldTime();
            final String worldName = world.getWorldInfo().getWorldName();
            logAndSend(
                    new LoggablePlayerEvent(
                            PlayerEventAction.LOCATION, worldTime, worldName, coordinates)
                            .setPlayerName(playerName));
        }
    }

    /**
     * Keeps track players last positions, in a guava cache for it's eviction policy.
     */
    private final Cache<String, Vec3> lastKnownCoordinates = CacheBuilder.newBuilder().maximumSize(MAX_PLAYERS).build(
            new CacheLoader<String, Vec3>() {
                @Override
                public Vec3 load(String key) throws Exception {
                    return lastKnownCoordinates.getIfPresent(key);
                }
            });
}
