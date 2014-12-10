package com.splunk.logtosplunk.event_loggers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.splunk.logtosplunk.LogToSplunkMod;
import com.splunk.logtosplunk.SplunkMessagePreparer;
import com.splunk.logtosplunk.actions.PlayerEventAction;
import com.splunk.logtosplunk.loggable_events.LoggablePlayerEvent;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PlayerMovementEventLogger {
    private static final String LOG_NAME_MODIFIER = " - MOVE";
    private static final Logger logger = LogManager.getLogger(LogToSplunkMod.LOGGER_NAME + LOG_NAME_MODIFIER);
    public static final double GRANULARITY = 1.5;
    public static final int MAX_PLAYERS = 128;

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
     * Processes and sends messages to Splunk.
     */
    private final SplunkMessagePreparer messagePreparer;

    /**
     * Constructor.
     *
     * @param messagePreparer Message preparer to forward packaged events to.
     */
    public PlayerMovementEventLogger(
            SplunkMessagePreparer messagePreparer) {
        this.messagePreparer = messagePreparer;
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
                    new LoggablePlayerEvent(PlayerEventAction.LOCATION, worldTime, worldName, coordinates)
                            .setPlayerName(playerName));
        }
    }

    /**
     * Logs via Log4j and forwards the message to the message preparer.
     *
     * @param loggable The message to log.
     */
    private void logAndSend(LoggablePlayerEvent loggable) {
        logger.debug(loggable);
        messagePreparer.writeMessage(loggable);
    }
}
