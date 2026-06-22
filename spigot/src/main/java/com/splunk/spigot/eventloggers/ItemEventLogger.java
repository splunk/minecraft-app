package com.splunk.spigot.eventloggers;

import static com.splunk.spigot.LogToSplunkPlugin.locationAsPoint;

import java.util.Properties;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableItemEvent;
import com.splunk.sharedmc.loggable_events.LoggableItemEvent.ItemAction;
import com.splunk.sharedmc.util.EventThrottle;

/**
 * Logs item pickup and drop. Pickups are throttled per-player to avoid floods.
 */
public class ItemEventLogger extends AbstractEventLogger implements Listener {

    private final EventThrottle throttle = new EventThrottle(1000L);

    public ItemEventLogger(Properties props) {
        super(props);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (!throttle.allow("pickup:" + player.getName())) {
            return;
        }
        LoggableItemEvent loggable = new LoggableItemEvent(
                ItemAction.PICKUP, player.getWorld().getTime(), player.getWorld().getName(),
                locationAsPoint(player.getLocation()));
        loggable.setPlayerName(player.getName())
                .setItem(event.getItem().getItemStack().getType().toString())
                .setQuantity(event.getItem().getItemStack().getAmount());
        logAndSend(loggable);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        LoggableItemEvent loggable = new LoggableItemEvent(
                ItemAction.DROP, player.getWorld().getTime(), player.getWorld().getName(),
                locationAsPoint(player.getLocation()));
        loggable.setPlayerName(player.getName())
                .setItem(event.getItemDrop().getItemStack().getType().toString())
                .setQuantity(event.getItemDrop().getItemStack().getAmount());
        logAndSend(loggable);
    }
}
