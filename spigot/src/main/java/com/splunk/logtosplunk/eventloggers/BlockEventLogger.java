package com.splunk.logtosplunk.eventloggers;

import java.util.Properties;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.splunk.logtosplunk.Point3dLong;
import com.splunk.logtosplunk.SplunkMessagePreparer;
import com.splunk.logtosplunk.event_loggers.AbstractEventLogger;
import com.splunk.logtosplunk.loggable_events.LoggableBlockEvent;
import com.splunk.logtosplunk.loggable_events.LoggableBlockEvent.BlockEventAction;


/**
 * Handles the logging of block events.
 */
public class BlockEventLogger extends AbstractEventLogger implements Listener {


    public BlockEventLogger(Properties properties, SplunkMessagePreparer messagePreparer) {
        super(properties, messagePreparer);
    }

    /**
     * Captures Block BreakEvents and sends them to the message preparer.
     *
     * @param event The captured BreakEvent.
     */
    @EventHandler
    public void captureBreakEvent(BlockBreakEvent event) {
        logger.info(event.getBlock().getType().name());
        logAndSend(getLoggableBlockBreakPlaceEvent(BlockEventAction.BREAK, event));
    }

    /**
     * Captures Block PlaceEvents and sends them to the message preparer.
     *
     * @param event The captured PlaceEvent.
     */
    @EventHandler
    public void capturePlaceEvent(BlockPlaceEvent event) {
        logAndSend(getLoggableBlockBreakPlaceEvent(BlockEventAction.PLACE, event));
    }

    private LoggableBlockEvent getLoggableBlockBreakPlaceEvent(BlockEventAction action, BlockEvent event) {

        final Block block = event.getBlock();
        final Location location = event.getBlock().getLocation();
        final String name = block.getType().name();
        final World w = block.getWorld();


        final Point3dLong coords = new Point3dLong(location.getX(), location.getY(), location.getZ());
        String playerName = null;
        if (event instanceof BlockBreakEvent) {
            playerName = ((BlockBreakEvent) event).getPlayer().getName();
        } else if (event instanceof BlockPlaceEvent) {
            playerName = ((BlockPlaceEvent) event).getPlayer().getName();
        }
        logger.info("LOGGING A BLOCK EVENT");
        return new LoggableBlockEvent(action, w.getFullTime(), w.getWorldType().getName(), coords)
                .setBlockName(name).setPlayerName(playerName).setBaseType(name);
    }
}
