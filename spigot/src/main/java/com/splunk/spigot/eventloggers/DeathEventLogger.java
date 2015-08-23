package com.splunk.spigot.eventloggers;

import java.util.Properties;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.splunk.sharedmc.SplunkMessagePreparer;
import com.splunk.sharedmc.event_loggers.AbstractEventLogger;

/**
 * Handles the logging of death events.
 */
public class DeathEventLogger extends AbstractEventLogger implements Listener {

    /**
     * Whether to turn off logging non-player related monster deaths. Monsters causing their own death generates a lot
     * of spammy events. E.g. Bats flying into lava...
     */
    public static final boolean IGNORE_MONSTER_ACCIDENTS = true;


    public DeathEventLogger(Properties properties, SplunkMessagePreparer messagePreparer) {
        super(properties, messagePreparer);
    }

    /**
     * Captures DeathEvents and sends them to the message preparer.
     *
     * @param event The captured BreakEvent.
     */
    @EventHandler
    public void captureDeathEvent(EntityDeathEvent event) {
                event.getEventName();
        String killer = null;
        logger.info("death event: " + event.getEventName() + "  " + event.getEntity().getKiller());
//        logAndSend(
//                new LoggableDeathEvent(deathAction, gameTime, worldName, position).setKiller(killer).setVicitim(victim)
//                        .setDamageSource(damageSource));
        if(event instanceof PlayerDeathEvent){
            event.getEntity().getLastDamageCause();
            logger.info("DAMAGE CAUSE:" + event.getEntity().getLastDamageCause().getCause().name());
            logger.info("PLAYER DEATH (womp womp): " + ((PlayerDeathEvent)event).getDeathMessage());
        }
    }
}
