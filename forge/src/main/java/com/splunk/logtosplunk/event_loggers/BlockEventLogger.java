package com.splunk.logtosplunk.event_loggers;

import java.util.Properties;

import com.splunk.logtosplunk.Point3dLong;
import com.splunk.logtosplunk.SplunkMessagePreparer;
import com.splunk.logtosplunk.loggable_events.LoggableBlockEvent;
import com.splunk.logtosplunk.loggable_events.LoggableBlockEvent.BlockEventAction;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Handles the logging of block events.
 */
public class BlockEventLogger extends AbstractEventLogger {

    /**
     * Constructor.
     *
     * @param messagePreparer Used to process this classes captured data.
     * @param props Properties to configure this EventLogger with.
     */
    public BlockEventLogger(Properties props, SplunkMessagePreparer messagePreparer) {
        super(props, messagePreparer);
    }

    /**
     * Captures Block BreakEvents and sends them to the message preparer.
     *
     * @param event The captured BreakEvent.
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void captureBreakEvent(BreakEvent event) {
        logAndSend(getLoggableBlockBreakPlaceEvent(BlockEventAction.BREAK, event));
    }

    /**
     * Captures Block PlaceEvents and sends them to the message preparer.
     *
     * @param event The captured PlaceEvent.
     */
    @SubscribeEvent
    @SideOnly(Side.SERVER)
    public void capturePlaceEvent(PlaceEvent event) {
        logAndSend(getLoggableBlockBreakPlaceEvent(BlockEventAction.PLACE, event));
    }

    private LoggableBlockEvent getLoggableBlockBreakPlaceEvent(BlockEventAction action, BlockEvent event) {

        final Block block = event.state.getBlock();
        final String base_type = block.getUnlocalizedName();
        final World w = event.world;
        final Item item = Item.getItemFromBlock(block);

        String blockName = null;
        if (item != null) {
            final int damVal = block.getDamageValue(event.world, event.pos);
            final ItemStack stack = new ItemStack(block, 1, damVal);
            blockName = item.getItemStackDisplayName(stack).replace(' ', '_');
        } else {

            blockName = base_type;
            if (event instanceof PlaceEvent) {
                blockName = ((PlaceEvent) event).itemInHand.getDisplayName();
            }
        }
        final Point3dLong coords = new Point3dLong(event.pos.getX(), event.pos.getY(), event.pos.getZ());
        String playerName = null;
        if (event instanceof BreakEvent) {
            playerName = ((BreakEvent) event).getPlayer().getDisplayNameString();
        } else if (event instanceof PlaceEvent) {
            playerName = ((PlaceEvent) event).player.getDisplayNameString();
        }

        return new LoggableBlockEvent(action, w.getWorldTime(), w.getWorldInfo().getWorldName(), coords)
                .setBlockName(blockName).setPlayerName(playerName).setBaseType(base_type);
    }
}