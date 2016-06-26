package com.splunk.spigot.eventloggers;

import static com.splunk.spigot.LogToSplunkPlugin.locationAsPoint;

import java.util.List;
import java.util.Properties;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.google.common.collect.Lists;

import com.splunk.sharedmc.Point3dLong;
import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableDeathEvent;

/**
 * Handles the logging of death events.
 */
public class DeathEventLogger extends AbstractEventLogger implements Listener {

    /**
     * Whether to turn off logging non-player related monster deaths. Monsters causing their own
     * death generates a lot of spammy events. E.g. Bats flying into lava...
     */
    public static final boolean IGNORE_MONSTER_ACCIDENTS = true;

    // ouch:
    public static final List<String> monsterNames = Lists.newArrayList(
            "Wolf", "Creeper", "Skeleton", "Blaze", "Cave Spider", "Spider", "Zombie Pigman", "Zombie", "Endermite",
            "Enderman", "Magma Cube", "Witch", "Wither", "Guardian", "Ghast", "Slime", "Silverfish");


    public DeathEventLogger(Properties properties) {
        super(properties);
    }

    /**
     * Captures DeathEvents.
     *
     * @param event The captured BreakEvent.
     */
    @EventHandler
    public void captureDeathEvent(EntityDeathEvent event) {
        String victim = event.getEntity().getName();
        String killer = null;
        long gameTime = event.getEntity().getWorld().getFullTime();
        String world = event.getEntity().getWorld().getName();
        Point3dLong location = locationAsPoint(event.getEntity().getLocation());

        if (event instanceof PlayerDeathEvent) {
            event.getEntity().getLastDamageCause();
            if (event.getEntity().getKiller() != null) {
                killer = event.getEntity().getKiller().getDisplayName();
            } else {
                for (String mob : monsterNames) {
                    if (((PlayerDeathEvent) event).getDeathMessage().contains(mob)) {
                        killer = mob;
                        break;
                    }
                }
            }

            if (killer == null) {
                killer = event.getEntity().getLastDamageCause().getCause().name();
            }
            LoggableDeathEvent deathEvent = new LoggableDeathEvent(LoggableDeathEvent.DeathEventAction.PLAYER_DIED, gameTime, world, location);
            deathEvent.setKiller(killer);
            deathEvent.setVictim(victim);
            deathEvent.setDamageSource(event.getEntity().getLastDamageCause().getCause().name());
            logAndSend(deathEvent);
        } else {
            if (event.getEntity().getKiller() != null && event.getEntity().getKiller().getDisplayName() != "ENTITY_ATTACK" && event.getEntity().getKiller().getDisplayName() != "FIRE_TICK") {
                killer = event.getEntity().getKiller().getDisplayName();
            } else {
                // killer = event.getEntity().getLastDamageCause().getCause().name();
            }
            LoggableDeathEvent deathEvent = new LoggableDeathEvent(LoggableDeathEvent.DeathEventAction.MOB_DIED, gameTime, world, location);
            deathEvent.setKiller(killer);
            deathEvent.setVictim(victim);
            deathEvent.setDamageSource(event.getEntity().getLastDamageCause().getCause().name());
            logAndSend(deathEvent);

        }
    }
}
