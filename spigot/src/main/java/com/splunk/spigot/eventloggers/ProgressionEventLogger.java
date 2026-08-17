package com.splunk.spigot.eventloggers;

import java.util.Properties;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.entity.Player;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableProgressionEvent;
import com.splunk.sharedmc.loggable_events.LoggableProgressionEvent.ProgressionAction;

/**
 * Logs progression and activity: XP, level, enchant, craft, fish, command.
 */
public class ProgressionEventLogger extends AbstractEventLogger implements Listener {

    public ProgressionEventLogger(Properties props) {
        super(props);
    }

    private LoggableProgressionEvent base(ProgressionAction action, Player player) {
        LoggableProgressionEvent e = new LoggableProgressionEvent(
                action, player.getWorld().getTime(), player.getWorld().getName());
        e.setPlayerName(player.getName());
        return e;
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        LoggableProgressionEvent e = base(ProgressionAction.EXP_CHANGE, event.getPlayer());
        e.setExpAmount(event.getAmount());
        logAndSend(e);
    }

    @EventHandler
    public void onLevelChange(PlayerLevelChangeEvent event) {
        LoggableProgressionEvent e = base(ProgressionAction.LEVEL_CHANGE, event.getPlayer());
        e.setNewLevel(event.getNewLevel());
        logAndSend(e);
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        LoggableProgressionEvent e = base(ProgressionAction.ENCHANT, event.getEnchanter());
        e.setDetail(event.getItem().getType().toString());
        logAndSend(e);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        LoggableProgressionEvent e = base(ProgressionAction.CRAFT, player);
        e.setDetail(event.getRecipe().getResult().getType().toString());
        logAndSend(e);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        LoggableProgressionEvent e = base(ProgressionAction.FISH, event.getPlayer());
        e.setDetail(event.getState().toString());
        logAndSend(e);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        LoggableProgressionEvent e = base(ProgressionAction.COMMAND, event.getPlayer());
        e.setDetail(event.getMessage());
        logAndSend(e);
    }
}
