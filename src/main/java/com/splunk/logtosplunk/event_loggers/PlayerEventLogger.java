package com.splunk.logtosplunk.event_loggers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splunk.logtosplunk.LogToSplunkMod;
import com.splunk.logtosplunk.SplunkMessagePreparer;
import com.splunk.logtosplunk.actions.PlayerEventAction;
import com.splunk.logtosplunk.loggable_events.LoggablePlayerEvent;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;
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
        World world = event.player.getEntityWorld();
        final long worldTime = world.getWorldTime();
        final String worldName = world.getWorldInfo().getWorldName();
        final Vec3 coordinates = event.player.getPositionVector();
        final LoggablePlayerEvent loggable =
                new LoggablePlayerEvent(PlayerEventAction.PLAYER_CONNECT, worldTime, worldName, coordinates);
        loggable.setPlayerName(event.player.getName());
        logAndSend(loggable);
    }

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
