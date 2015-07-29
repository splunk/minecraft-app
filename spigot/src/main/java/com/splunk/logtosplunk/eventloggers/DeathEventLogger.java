package com.splunk.logtosplunk.eventloggers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import com.splunk.logtosplunk.LogToSplunkPlugin;
import com.splunk.logtosplunk.event_loggers.AbstractEventLogger;

/**
 * Handles the logging of death events.
 */
public class DeathEventLogger extends AbstractEventLogger implements Listener {

    /**
     * Whether to turn off logging non-player related monster deaths. Monsters causing their own death generates a lot
     * of spammy events. E.g. Bats flying into lava...
     */
    public static final boolean IGNORE_MONSTER_ACCIDENTS = true;


    public DeathEventLogger() {
        super(LogToSplunkPlugin.properties, LogToSplunkPlugin.messagePreparer);
    }

    /**
     * Captures DeathEvents and sends them to the message preparer.
     *
     * @param event The captured BreakEvent.
     */
    @EventHandler
    public void captureDeathEvent(EntityDeathEvent event) {
                event.getEventName();
        final Boolean playerDied = event.getEntity() instanceof Player;
        String killer = null;
        logger.info("death event: " + event.getEventName() + "  " + event.getEntity().getKiller());
//        logAndSend(
//                new LoggableDeathEvent(deathAction, gameTime, worldName, position).setKiller(killer).setVicitim(victim)
//                        .setDamageSource(damageSource));
    }
}
