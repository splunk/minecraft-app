package com.splunk.forge.eventloggers;

import java.util.Properties;

import com.splunk.sharedmc.Point3dLong;
import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableDeathEvent;
import com.splunk.sharedmc.loggable_events.LoggableDeathEvent.DeathEventAction;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Logs entity deaths (players and mobs) via Forge's native {@code LivingDeathEvent}.
 */
public class DeathEventLogger extends AbstractEventLogger {

    public DeathEventLogger(Properties props) {
        super(props);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        final Level level = entity.level();
        if (level.isClientSide()) {
            return;
        }
        final long worldTime = level.getGameTime();
        final String worldName = level.dimension().location().toString();
        final Point3dLong loc = new Point3dLong(entity.getX(), entity.getY(), entity.getZ());

        final boolean isPlayer = entity instanceof ServerPlayer;
        final DeathEventAction action = isPlayer ? DeathEventAction.PLAYER_DIED : DeathEventAction.MOB_DIED;

        final String victim = entity.getName().getString();
        final DamageSource source = event.getSource();
        final Entity attacker = source.getEntity();
        final String killer = attacker != null ? attacker.getName().getString() : null;
        final String damageSource = source.getMsgId();

        logAndSend(new LoggableDeathEvent(action, worldTime, worldName, loc, killer, victim, damageSource));
    }
}
