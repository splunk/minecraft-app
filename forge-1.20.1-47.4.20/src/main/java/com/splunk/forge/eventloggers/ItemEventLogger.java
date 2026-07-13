package com.splunk.forge.eventloggers;

import java.util.Properties;

import com.splunk.sharedmc.Point3dLong;
import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableItemEvent;
import com.splunk.sharedmc.loggable_events.LoggableItemEvent.ItemAction;
import com.splunk.sharedmc.util.EventThrottle;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Logs item pickup and drop events. Pickup is throttled (it can fire rapidly, e.g. when
 * picking up XP orbs or arrows); drop is not throttled since it is a deliberate, low
 * frequency player action. Mirrors the spigot module's {@code ItemEventLogger}.
 */
public class ItemEventLogger extends AbstractEventLogger {

    private final EventThrottle throttle = new EventThrottle(1000L);

    public ItemEventLogger(Properties props) {
        super(props);
    }

    @SubscribeEvent
    public void onPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (!throttle.allow("pickup:" + player.getName().getString())) {
            return;
        }
        ItemStack stack = event.getItem().getItem();
        Level level = player.level();
        Point3dLong loc = new Point3dLong(player.getX(), player.getY(), player.getZ());
        LoggableItemEvent loggable = new LoggableItemEvent(
                ItemAction.PICKUP, level.getGameTime(), level.dimension().location().toString(), loc);
        loggable.setPlayerName(player.getName().getString())
                .setItem(stack.getItem().toString())
                .setQuantity(stack.getCount());
        logAndSend(loggable);
    }

    @SubscribeEvent
    public void onDrop(ItemTossEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getEntity().getItem();
        Level level = player.level();
        Point3dLong loc = new Point3dLong(player.getX(), player.getY(), player.getZ());
        LoggableItemEvent loggable = new LoggableItemEvent(
                ItemAction.DROP, level.getGameTime(), level.dimension().location().toString(), loc);
        loggable.setPlayerName(player.getName().getString())
                .setItem(stack.getItem().toString())
                .setQuantity(stack.getCount());
        logAndSend(loggable);
    }
}
