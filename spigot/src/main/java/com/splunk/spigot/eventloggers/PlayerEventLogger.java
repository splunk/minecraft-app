package com.splunk.spigot.eventloggers;

import java.util.Properties;

import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggablePlayerEvent;
import com.splunk.sharedmc.loggable_events.LoggablePlayerEvent.PlayerEventAction;
import com.splunk.spigot.LogToSplunkPlugin;

/**
 * Handles the logging of player events.
 */
public class PlayerEventLogger extends AbstractEventLogger implements Listener {
    public static final double GRANULARITY = 1.5;

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

    public void onPlayerConnect(PlayerLoginEvent event) {
        logAndSend(
                generateLoggablePlayerEvent(event, PlayerEventAction.PLAYER_CONNECT, null, event.getKickMessage()));
    }

    /**
     * Logs to Splunk when a player logs out.
     *
     * @param event The captured event.
     */
    public void onPlayerDisconnect(PlayerKickEvent event) {
        logAndSend(
                generateLoggablePlayerEvent(
                        event, PlayerEventAction.PLAYER_DISCONNECT, event.getReason(), event.getLeaveMessage()));
    }

    public void onPlayerMove(PlayerMoveEvent event) {
        logAndSend(
                generateLoggablePlayerEvent(
                        event, PlayerEventAction.PLAYER_DISCONNECT, null, null));
    }

    private static LoggablePlayerEvent generateLoggablePlayerEvent(
            PlayerEvent event, PlayerEventAction actionType, String reason, String message) {
        final World world = event.getPlayer().getWorld();
        final long worldTime = world.getTime();
        final String worldName = world.getName();
        final LoggablePlayerEvent loggable = new LoggablePlayerEvent(
                actionType, worldTime, worldName, LogToSplunkPlugin.locationAsPoint(event.getPlayer().getLocation()));

        loggable.setPlayerName(event.getPlayer().getDisplayName());
        loggable.setReason(reason);
        loggable.setMessage(message);

        if (event.getClass().equals(PlayerMoveEvent.class)) {
            loggable.setFrom(LogToSplunkPlugin.locationAsPoint(((PlayerMoveEvent) event).getFrom()));
            loggable.setTo(LogToSplunkPlugin.locationAsPoint(((PlayerMoveEvent) event).getTo()));
        }

        return loggable;
    }
}
