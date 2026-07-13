package com.splunk.forge.eventloggers;

import java.util.Properties;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableProgressionEvent;
import com.splunk.sharedmc.loggable_events.LoggableProgressionEvent.ProgressionAction;

import com.mojang.brigadier.context.CommandContextBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Logs progression and activity: level, craft, fish, command. Mirrors the spigot module's
 * {@code ProgressionEventLogger}, minus two categories Forge 1.20.1 has no event for:
 *
 * <ul>
 *   <li><b>exp_change</b> — Forge only exposes {@code PlayerXpEvent.LevelChange} (level-ups),
 *       not per-orb XP gain like Bukkit's {@code PlayerExpChangeEvent}. Omitted rather than
 *       approximated.
 *   <li><b>enchant</b> — Forge has no event fired specifically when a player enchants an item
 *       via the enchanting table (verified against the {@code forge-1.20.1-47.4.20} event bus
 *       classes: no {@code EnchantItemEvent} equivalent exists). Omitted rather than
 *       approximated with an unrelated container-close event.
 * </ul>
 */
public class ProgressionEventLogger extends AbstractEventLogger {

    public ProgressionEventLogger(Properties props) {
        super(props);
    }

    private LoggableProgressionEvent base(ProgressionAction action, Player player) {
        LoggableProgressionEvent e = new LoggableProgressionEvent(
                action, player.level().getGameTime(), player.level().dimension().location().toString());
        e.setPlayerName(player.getName().getString());
        return e;
    }

    @SubscribeEvent
    public void onLevelChange(PlayerXpEvent.LevelChange event) {
        Player player = event.getEntity();
        LoggableProgressionEvent e = base(ProgressionAction.LEVEL_CHANGE, player);
        e.setNewLevel(player.experienceLevel + event.getLevels());
        logAndSend(e);
    }

    @SubscribeEvent
    public void onCraft(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        LoggableProgressionEvent e = base(ProgressionAction.CRAFT, player);
        e.setDetail(event.getCrafting().getItem().toString());
        logAndSend(e);
    }

    @SubscribeEvent
    public void onFish(ItemFishedEvent event) {
        Player player = event.getEntity();
        LoggableProgressionEvent e = base(ProgressionAction.FISH, player);
        e.setDetail(event.getHookEntity().getHookedIn() != null
                ? "entity"
                : event.getDrops().size() + " item(s)");
        logAndSend(e);
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        CommandContextBuilder<CommandSourceStack> context = event.getParseResults().getContext();
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) source.getEntity();
        LoggableProgressionEvent e = base(ProgressionAction.COMMAND, player);
        e.setDetail(event.getParseResults().getReader().getString());
        logAndSend(e);
    }
}
