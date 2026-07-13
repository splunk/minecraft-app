package com.splunk.forge.eventloggers;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.splunk.sharedmc.Point3dLong;
import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableCombatEvent;
import com.splunk.sharedmc.loggable_events.LoggableCombatEvent.CombatAction;
import com.splunk.sharedmc.util.EventThrottle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Logs combat and survival events. High-frequency events (damage, hunger) are throttled
 * per-player. Mirrors the spigot module's {@code CombatEventLogger}.
 *
 * <p>Forge has no hunger-change event (unlike Bukkit's {@code FoodLevelChangeEvent}), so hunger
 * is polled every {@link #FOOD_SAMPLE_INTERVAL_TICKS} ticks and reported only when a player's
 * food level actually changed since the last sample.
 *
 * <p>Note: there is intentionally no death/kill handler here, matching the spigot module —
 * kill events are handled by {@code DeathEventLogger} to avoid double-logging.
 */
public class CombatEventLogger extends AbstractEventLogger {

    private static final long THROTTLE_MS = 1000L;
    private static final int FOOD_SAMPLE_INTERVAL_TICKS = 20;

    private final EventThrottle throttle = new EventThrottle(THROTTLE_MS);
    private final Map<UUID, Integer> lastKnownFoodLevel = new HashMap<>();
    private int tickCounter = 0;

    public CombatEventLogger(Properties props) {
        super(props);
    }

    /** Throttled per-victim: damage events can fire many times per second. */
    @SubscribeEvent
    public void onDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer victim = (ServerPlayer) entity;
        if (!throttle.allow("dmg:" + victim.getName().getString())) {
            return;
        }
        Entity damager = event.getSource().getEntity();
        LoggableCombatEvent loggable = base(CombatAction.DAMAGE, victim);
        loggable.setVictim(victim.getName().getString())
                .setSource(damager != null ? damager.getType().toString() : event.getSource().getMsgId())
                .setAmount(event.getAmount())
                .setCause(event.getSource().getMsgId())
                .setHealthRemaining(victim.getHealth());
        logAndSend(loggable);
    }

    /** Throttled per-player: regen-based healing (e.g. saturation) can fire frequently. */
    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer) entity;
        if (!throttle.allow("heal:" + player.getName().getString())) {
            return;
        }
        LoggableCombatEvent loggable = base(CombatAction.HEAL, player);
        loggable.setVictim(player.getName().getString())
                .setAmount(event.getAmount())
                .setHealthRemaining(player.getHealth());
        logAndSend(loggable);
    }

    /** Not throttled: respawn is a rare, deliberate-trigger event for a given player. */
    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer) event.getEntity();
        lastKnownFoodLevel.remove(player.getUUID());
        LoggableCombatEvent loggable = base(CombatAction.RESPAWN, player);
        loggable.setVictim(player.getName().getString());
        logAndSend(loggable);
    }

    /** Polls food level; Forge has no hunger-change event to subscribe to directly. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++tickCounter % FOOD_SAMPLE_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            int foodLevel = player.getFoodData().getFoodLevel();
            Integer previous = lastKnownFoodLevel.put(player.getUUID(), foodLevel);
            if (previous != null && previous == foodLevel) {
                continue;
            }
            if (!throttle.allow("food:" + player.getName().getString())) {
                continue;
            }
            LoggableCombatEvent loggable = base(CombatAction.HUNGER, player);
            loggable.setVictim(player.getName().getString()).setFoodLevel(foodLevel);
            logAndSend(loggable);
        }
    }

    private LoggableCombatEvent base(CombatAction action, ServerPlayer player) {
        Level level = player.level();
        Point3dLong loc = new Point3dLong(player.getX(), player.getY(), player.getZ());
        return new LoggableCombatEvent(action, level.getGameTime(), level.dimension().location().toString(), loc);
    }
}
