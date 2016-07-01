package com.splunk.spigot.eventloggers;

import static com.splunk.spigot.LogToSplunkPlugin.locationAsPoint;

import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.EntityType;
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

        String killer = null;
        long gameTime = event.getEntity().getWorld().getFullTime();
        String world = event.getEntity().getWorld().getName();
        Point3dLong location = locationAsPoint(event.getEntity().getLocation());


        String victim = "";


        LoggableDeathEvent deathEvent;
        if (event instanceof PlayerDeathEvent) {
            // Player died
            deathEvent = new LoggableDeathEvent(LoggableDeathEvent.DeathEventAction.PLAYER_DIED, gameTime, world, location);
            victim = event.getEntity().getName();


        } else {

            //mob died
            deathEvent = new LoggableDeathEvent(LoggableDeathEvent.DeathEventAction.MOB_DIED, gameTime, world, location);

            victim = event.getEntityType().name();
            if (event.getEntityType() == EntityType.SKELETON) {
                org.bukkit.entity.Skeleton skeleton = (org.bukkit.entity.Skeleton) event.getEntity();
                victim = skeleton.getSkeletonType().name() + "_SKELETON";
            }
        }

        if (event.getEntity().getKiller() != null) {
            // Player did the killing
            killer = event.getEntity().getKiller().getDisplayName();
            victim = event.getEntity().getCustomName();

            final String instrument = event.getEntity().getKiller().getInventory().getItemInMainHand().getData().toString();
            deathEvent.setInstrument(instrument.replaceAll("\\(\\S*\\)", ""));

        } else {
            // Mob did the killing
            if (event instanceof PlayerDeathEvent) {
                // Only works if a player dies.

                Pattern regex = Pattern.compile("\\S* (was slain by|was shot by a|was blown up by) (?<killer>\\S*)");
                Matcher matcher = regex.matcher(((PlayerDeathEvent) event).getDeathMessage());

                if (matcher.matches()) {
                    killer = matcher.group("killer");
                }


            }
        }

        deathEvent.setKiller(killer);
        deathEvent.setVictim(victim);
        deathEvent.setDamageSource(event.getEntity().getLastDamageCause().getCause().name());

        logAndSend(deathEvent);
    }
}
