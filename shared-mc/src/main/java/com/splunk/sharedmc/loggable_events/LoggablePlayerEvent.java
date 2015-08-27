package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;

/**
 * Almost pojo with fields for information that might be associated with a player event.
 */
public class LoggablePlayerEvent extends AbstractLoggableEvent {
    public static final String MESSAGE = "message";
    public static final String REASON = "reason";
    private String playerName;
    private final PlayerEventAction action;
    private String message;
    private String reason;

    /**
     * Constructor.
     *
     * @param action The type of player action this represents, e.g. 'player_disconnected'.
     */
    public LoggablePlayerEvent(PlayerEventAction action, long gameTime, String worldName, Point3dLong location) {
        super(LoggableEventType.PLAYER, gameTime, worldName, location);
        this.addField(ACTION, action.asString());
        this.action = action;
    }

    public String getPlayerName() {
        return playerName;
    }

    public LoggablePlayerEvent setPlayerName(String playerName) {
        this.playerName = playerName;
        this.addField(PLAYER_NAME, playerName);
        return this;
    }

    public PlayerEventAction getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }

    public LoggablePlayerEvent setMessage(String message) {
        this.message = message;
        this.addField(MESSAGE, message);
        return this;
    }

    public String getReason() {
        return reason;
    }

    public LoggablePlayerEvent setReason(String reason) {
        this.reason = reason;
        this.addField(REASON, reason);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LoggablePlayerEvent that = (LoggablePlayerEvent) o;

        if (action != that.action) {
            return false;
        }

        if (getCoordinates() != null ? !getCoordinates().equals(that.getCoordinates()) :
                that.getCoordinates() != null) {
            return false;
        }
        if (message != null ? !message.equals(that.message) : that.message != null) {
            return false;
        }
        if (playerName != null ? !playerName.equals(that.playerName) : that.playerName != null) {
            return false;
        }
        if (reason != null ? !reason.equals(that.reason) : that.reason != null) {
            return false;
        }
        if (this.getWorldTime() != that.getWorldTime()) {
            return false;
        }
        if (getWorldName() != null ? !getWorldName().equals(that.getWorldName()) : that.getWorldName() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = playerName != null ? playerName.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        return result;
    }

    /**
     * Different types of actions that can occur as part of a PlayerEvent.
     */
    public enum PlayerEventAction {
        PLAYER_CONNECT("player_connect"),
        PLAYER_DISCONNECT("player_disconnect"),
        CHAT("chat"),
        LOCATION("move");

        /**
         * The name of the action.
         */
        private final String action;

        PlayerEventAction(String action) {
            this.action = action;
        }

        /**
         * String representation of the action.
         *
         * @return The action in friendly String format.
         */
        public String asString() {
            return action;
        }
    }
}
