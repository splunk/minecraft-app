package com.splunk.spigot.eventloggers;

import com.splunk.sharedmc.Point3dLong;
import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableEntityEvent;

import java.util.Properties;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;


/**
 * Created by giy4 on 6/24/16.
 */
public class EntityEventLogger extends AbstractEventLogger implements Listener {

    public EntityEventLogger(Properties properties) {
        super(properties);
    }

    @EventHandler
    public void captureSpawnEvent(CreatureSpawnEvent event){

        final String reason = event.getSpawnReason().name();
        logAndSend(getLoggableEntityDefaultEvent(LoggableEntityEvent.EntityEventAction.ENTITY_SPAWN, event,reason));
    }


    private LoggableEntityEvent getLoggableEntityDefaultEvent(LoggableEntityEvent.EntityEventAction action, EntityEvent event, String reason){


        final Location location = event.getEntity().getLocation();

        final World world = event.getEntity().getWorld();
        final Point3dLong coords = new Point3dLong(location.getX(), location.getY(), location.getZ());



        final String entityName = event.getEntity().getName();


        return new LoggableEntityEvent(action, world.getFullTime(), world.getName(), coords).setEntityName(entityName).setReason(reason);
    }
}
