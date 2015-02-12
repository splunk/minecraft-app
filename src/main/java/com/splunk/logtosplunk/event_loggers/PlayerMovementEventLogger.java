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
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PlayerMovementEventLogger {
    private static final String LOG_NAME_MODIFIER = " - MOVE";
    private static final Logger logger = LogManager.getLogger(LogToSplunkMod.LOGGER_NAME + LOG_NAME_MODIFIER);

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
     * Logs via Log4j and forwards the message to the message preparer.
     *
     * @param loggable The message to log.
     */
    private void logAndSend(LoggablePlayerEvent loggable) {
        logger.debug(loggable);
        messagePreparer.writeMessage(loggable);
    }
    /**
     * Processes and sends messages to Splunk.
     */
    private final SplunkMessagePreparer messagePreparer;


}
