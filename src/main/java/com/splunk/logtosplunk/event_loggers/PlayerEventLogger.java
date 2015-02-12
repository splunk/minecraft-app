package com.splunk.logtosplunk.event_loggers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.splunk.logtosplunk.LogToSplunkMod;
import com.splunk.logtosplunk.SplunkMessagePreparer;
import com.splunk.logtosplunk.loggable_events.LoggablePlayerEvent;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Handles the logging of player events.
 */
public class PlayerEventLogger {
    private static final String LOG_NAME_MODIFIER = " - PLAYER";
    private static final Logger logger = LogManager.getLogger(LogToSplunkMod.LOGGER_NAME + LOG_NAME_MODIFIER);
    public static final double GRANULARITY = 1.5;
    public static final int MAX_PLAYERS = 128;

    /**
     * Processes and sends messages to Splunk.
     */
    private final SplunkMessagePreparer messagePreparer;

    /**
     * Constructor.
     *
     * @param splunkMessagePreparer Message preparer to send packaged raw messages to.
     */
    public PlayerEventLogger(SplunkMessagePreparer splunkMessagePreparer) {
        this.messagePreparer = splunkMessagePreparer;
    }

    /**
     * Logs to Splunk when a player logs in.
     *
     * @param event The captured event.
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void onPlayerConnect(PlayerEvent.PlayerLoggedInEvent event) {
        logAndSend(
                generateLoggablePlayerEvent(event, LoggablePlayerEvent.PlayerEventAction.PLAYER_CONNECT, null, null));
    }

    /**
     * Logs to Splunk when a player logs out.
     *
     * @param event The captured event.
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        logAndSend(
                generateLoggablePlayerEvent(
                        event, LoggablePlayerEvent.PlayerEventAction.PLAYER_DISCONNECT, null, null));
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
                generateLoggablePlayerEvent(chatEvent, LoggablePlayerEvent.PlayerEventAction.CHAT, chatEvent.message));
    }

    private LoggablePlayerEvent generateLoggablePlayerEvent(
            PlayerEvent event, LoggablePlayerEvent.PlayerEventAction actionType, String reason, String message) {
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
            ServerChatEvent event, LoggablePlayerEvent.PlayerEventAction actionType, String message) {
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
    public void onPlayerStatusReported(LivingEvent.LivingUpdateEvent playerMove) {
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
                            LoggablePlayerEvent.PlayerEventAction.LOCATION, worldTime, worldName, coordinates)
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

    /**
     * Logs via Log4j and forwards the messgage to the message preparer.
     *
     * @param loggable The message to log.
     */
    private void logAndSend(LoggablePlayerEvent loggable) {
        logger.info(loggable);
        messagePreparer.writeMessage(loggable);
    }
}
