package com.splunk.sharedmc.loggable_events;

/**
 * Server lifecycle and world-state events (start, stop, weather).
 */
public class LoggableServerEvent extends AbstractLoggableEvent {

    public LoggableServerEvent(ServerAction action, long gameTime, String worldName) {
        super(LoggableEventType.SERVER, gameTime, worldName);
        this.addField(ACTION, action.asString());
    }

    public LoggableServerEvent setWeather(String weather) {
        this.addField("weather", weather);
        return this;
    }

    public LoggableServerEvent setMotd(String motd) {
        this.addField("motd", motd);
        return this;
    }

    public enum ServerAction {
        SERVER_START("server_start"),
        SERVER_STOP("server_stop"),
        WEATHER_CHANGE("weather_change");

        private final String action;
        ServerAction(String action) { this.action = action; }
        public String asString() { return action; }
    }
}
