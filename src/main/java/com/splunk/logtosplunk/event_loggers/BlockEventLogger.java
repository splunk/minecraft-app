package com.splunk.logtosplunk.event_loggers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.splunk.logtosplunk.LogToSplunkMod;
import com.splunk.logtosplunk.SplunkMessagePreparer;
import com.splunk.logtosplunk.actions.BlockEventAction;
import com.splunk.logtosplunk.loggable_events.LoggableBlockEvent;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Handles the logging of block events.
 */
public class BlockEventLogger {
    private static final String LOG_NAME_MODIFIER = " - BLOCK";
    private static final Logger logger = LogManager.getLogger(LogToSplunkMod.LOGGER_NAME + LOG_NAME_MODIFIER);

    /**
     * Prepares and forwards data to be sent to splunk.
     */
    private SplunkMessagePreparer messagePreparer;

    /**
     * Constructor.
     *
     * @param messagePreparer used to process this classes captured data.
     */
    public BlockEventLogger(SplunkMessagePreparer messagePreparer) {
        this.messagePreparer = messagePreparer;
    }

    /**
     * Captures Block BreakEvents and sends them to the message preparer.
     *
     * @param event The captured BreakEvent.
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void captureBreakEvent(BlockEvent.BreakEvent event) {
        logAndSend(getLoggableBlockBreakPlaceEvent(BlockEventAction.BREAK, event));
    }

    /**
     * Captures Block PlaceEvents and sends them to the message preparer.
     *
     * @param event The captured PlaceEvent.
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void capturePlaceEvent(BlockEvent.PlaceEvent event) {
        logAndSend(getLoggableBlockBreakPlaceEvent(BlockEventAction.PLACE, event));
    }

    private LoggableBlockEvent getLoggableBlockBreakPlaceEvent(BlockEventAction action, BlockEvent event) {
        Block block = event.state.getBlock();
        String base_type = block.getUnlocalizedName();
        World w = event.world;
        Item item =  Item.getItemFromBlock(block);
        int damVal = block.getDamageValue(event.world, event.pos);
        ItemStack stack = new ItemStack(block, 1, damVal);
        String blockName = item.getItemStackDisplayName(stack).replace(' ', '_');
        Vec3 coords = new Vec3(event.pos.getX(), event.pos.getY(), event.pos.getZ());
        String playerName = null;
        if (event instanceof BlockEvent.BreakEvent) {
            playerName = ((BlockEvent.BreakEvent) event).getPlayer().getName();
        } else if (event instanceof BlockEvent.PlaceEvent) {
            playerName = ((BlockEvent.PlaceEvent) event).player.getName();
        }
        return new LoggableBlockEvent(action, w.getWorldTime(), w.getWorldInfo().getWorldName(), coords)
                .setBlockName(blockName).setPlayerName(playerName).setBaseType(base_type);
    }

    /**
     * Logs via Log4j and forwards the messgage to the message preparer.
     *
     * @param loggable The message to log.
     */
    private void logAndSend(LoggableBlockEvent loggable) {
        logger.debug(loggable);
        messagePreparer.writeMessage(loggable);
    }
}
