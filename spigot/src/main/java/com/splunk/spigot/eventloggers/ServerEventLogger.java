package com.splunk.spigot.eventloggers;

import java.util.Properties;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import com.splunk.sharedmc.event_loggers.AbstractEventLogger;
import com.splunk.sharedmc.loggable_events.LoggableServerEvent;
import com.splunk.sharedmc.loggable_events.LoggableServerEvent.ServerAction;

/**
 * Logs server lifecycle and world-state events.
 */
public class ServerEventLogger extends AbstractEventLogger implements Listener {

    public ServerEventLogger(Properties props) {
        super(props);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        LoggableServerEvent e = new LoggableServerEvent(ServerAction.SERVER_START, 0L, null);
        e.setMotd(event.getType().toString());
        logAndSend(e);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        LoggableServerEvent e = new LoggableServerEvent(
                ServerAction.WEATHER_CHANGE, event.getWorld().getTime(), event.getWorld().getName());
        e.setWeather(event.toWeatherState() ? "storm" : "clear");
        logAndSend(e);
    }
}
