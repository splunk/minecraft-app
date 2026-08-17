package com.splunk.spigot.eventloggers;

import static com.splunk.spigot.LogToSplunkPlugin.locationAsPoint;

import java.util.Properties;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableCombatEvent;
import com.splunk.sharedmc.loggable_events.LoggableCombatEvent.CombatAction;
import com.splunk.sharedmc.util.EventThrottle;

/**
 * Logs combat and survival events. High-frequency events (damage, hunger) are throttled
 * per-player.
 *
 * <p>Note: there is intentionally no {@code onDeath}/{@code EntityDeathEvent} handler here.
 * Kill events are still handled by the pre-existing {@code DeathEventLogger} to avoid
 * double-logging the same kill as both a CombatEvent and a DeathEvent.
 */
public class CombatEventLogger extends AbstractEventLogger implements Listener {

    private static final long THROTTLE_MS = 1000L;
    private final EventThrottle throttle = new EventThrottle(THROTTLE_MS);

    public CombatEventLogger(Properties props) {
        super(props);
    }

    /** Throttled per-victim: damage events can fire many times per second. */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        if (!throttle.allow("dmg:" + victim.getName())) {
            return;
        }
        LoggableCombatEvent loggable = new LoggableCombatEvent(
                CombatAction.DAMAGE, victim.getWorld().getTime(), victim.getWorld().getName(),
                locationAsPoint(victim.getLocation()));
        loggable.setVictim(victim.getName())
                .setSource(event.getDamager().getType().toString())
                .setAmount(event.getFinalDamage())
                .setCause(event.getCause().toString())
                .setHealthRemaining(victim.getHealth());
        logAndSend(loggable);
    }

    /** Throttled per-player: regen-based healing (e.g. saturation) can fire frequently. */
    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (!throttle.allow("heal:" + player.getName())) {
            return;
        }
        LoggableCombatEvent loggable = new LoggableCombatEvent(
                CombatAction.HEAL, player.getWorld().getTime(), player.getWorld().getName(),
                locationAsPoint(player.getLocation()));
        loggable.setVictim(player.getName())
                .setAmount(event.getAmount())
                .setCause(event.getRegainReason().toString())
                .setHealthRemaining(player.getHealth());
        logAndSend(loggable);
    }

    /** Throttled per-player: food level changes frequently while eating, sprinting, etc. */
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (!throttle.allow("food:" + player.getName())) {
            return;
        }
        LoggableCombatEvent loggable = new LoggableCombatEvent(
                CombatAction.HUNGER, player.getWorld().getTime(), player.getWorld().getName(),
                locationAsPoint(player.getLocation()));
        loggable.setVictim(player.getName()).setFoodLevel(event.getFoodLevel());
        logAndSend(loggable);
    }

    /** Not throttled: respawn is a rare, deliberate-trigger event for a given player. */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        LoggableCombatEvent loggable = new LoggableCombatEvent(
                CombatAction.RESPAWN, event.getPlayer().getWorld().getTime(),
                event.getPlayer().getWorld().getName(), locationAsPoint(event.getRespawnLocation()));
        loggable.setVictim(event.getPlayer().getName());
        logAndSend(loggable);
    }
}
