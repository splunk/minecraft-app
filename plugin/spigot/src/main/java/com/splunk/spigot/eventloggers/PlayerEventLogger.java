package com.splunk.spigot.eventloggers;

import static com.splunk.spigot.LogToSplunkPlugin.locationAsPoint;

import java.util.Properties;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggablePlayerEvent;
import com.splunk.sharedmc.loggable_events.LoggablePlayerEvent.PlayerEventAction;

/**
 * Handles the logging of player events.
 */
public class PlayerEventLogger extends AbstractEventLogger implements Listener {
    public static final double GRANULARITY = 1.5;
    public static final int MAX_PLAYERS = 128;

    /**
     * Keeps track players last positions, in a guava cache for it's eviction policy.
     */
    private final Cache<String, Location> lastKnownCoordinates =
            CacheBuilder.newBuilder().maximumSize(MAX_PLAYERS).build(
                    new CacheLoader<String, Location>() {
                        @Override
                        public Location load(String key) throws Exception {
                            return lastKnownCoordinates.getIfPresent(key);
                        }
                    });

    /**
     * Constructs a new PlayerEventLogger.
     *
     * @param props Properties to configure this EventLogger with.
     */
    public PlayerEventLogger(Properties props) {
        super(props);
    }

    /**
     * Logs to Splunk when a player logs in.
     *
     * @param event The captured event.
     */
    @EventHandler
    public void onPlayerConnect(PlayerLoginEvent event) {
        logAndSend(
                generateLoggablePlayerEvent(event, PlayerEventAction.PLAYER_CONNECT, null, event.getKickMessage()));
    }

    /**
     * Logs to Splunk when a player logs out.
     *
     * @param event The captured event.
     */
    @EventHandler
    public void onPlayerDisconnect(PlayerKickEvent event) {
        logAndSend(
                generateLoggablePlayerEvent(
                        event, PlayerEventAction.PLAYER_DISCONNECT, event.getReason(), event.getLeaveMessage()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        logAndSend(
                generateLoggablePlayerEvent(
                        event, PlayerEventAction.PLAYER_DISCONNECT, null, event.getQuitMessage()));
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location previous = lastKnownCoordinates.getIfPresent(event.getPlayer().getDisplayName());

        String cause = event.getCause().name();

        logAndSend(
                generateLoggablePlayerEvent(
                        event, PlayerEventAction.TELEPORT, cause, null));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location previous = lastKnownCoordinates.getIfPresent(event.getPlayer().getDisplayName());

        if (previous != null && event.getFrom().getWorld().getName() == previous.getWorld().getName() && previous.distance(event.getTo()) < GRANULARITY) {
            return;
        }

        logAndSend(
                generateLoggablePlayerEvent(
                        event, PlayerEventAction.MOVE,null, null));
    }

    private LoggablePlayerEvent generateLoggablePlayerEvent(
            PlayerEvent event, PlayerEventAction actionType, String reason, String message) {
        final World world = event.getPlayer().getWorld();
        final long worldTime = world.getTime();
        final String worldName = world.getName();
        final LoggablePlayerEvent loggable = new LoggablePlayerEvent(
                actionType, worldTime, worldName, locationAsPoint(event.getPlayer().getLocation()));

        loggable.setPlayerName(event.getPlayer().getDisplayName());
        loggable.setReason(reason);
        loggable.setMessage(message);

        if (event.getClass().equals(PlayerMoveEvent.class) || event.getClass().equals(PlayerTeleportEvent.class)) {

            loggable.setFrom(
                    locationAsPoint(lastKnownCoordinates.getIfPresent(event.getPlayer().getDisplayName())));

            if (loggable.getFrom() == null) {
                loggable.setFrom(locationAsPoint(((PlayerMoveEvent) event).getFrom()));
            }
            loggable.setTo(locationAsPoint(((PlayerMoveEvent) event).getTo()));
            lastKnownCoordinates.put(event.getPlayer().getDisplayName(), ((PlayerMoveEvent) event).getTo());
        }

        return loggable;
    }
}
