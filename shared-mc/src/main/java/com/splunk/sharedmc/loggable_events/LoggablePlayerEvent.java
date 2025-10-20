package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;

/**
 * Almost pojo with fields for information that might be associated with a player event.
 */
public class LoggablePlayerEvent extends AbstractLoggableEvent {
    public static final String MESSAGE = "message";
    public static final String REASON = "reason";

    /**
     * Constructor.
     *
     * @param action The type of player action this represents, e.g. 'player_disconnected'.
     */
    public LoggablePlayerEvent(PlayerEventAction action, long gameTime, String worldName, Point3dLong location) {
        super(LoggableEventType.PLAYER, gameTime, worldName, location);
        this.addField(ACTION, action.asString());
    }

    public LoggablePlayerEvent setPlayerName(String playerName) {
        this.addField(PLAYER_NAME, playerName);
        return this;
    }

    public LoggablePlayerEvent setMessage(String message) {
        this.addField(MESSAGE, message);
        return this;
    }

    public LoggablePlayerEvent setReason(String reason) {
        this.addField(REASON, reason);
        return this;
    }

    public LoggablePlayerEvent setTo(Point3dLong to) {
        if(to == null){
            return this;
        }
        this.addField("to_x", to.xCoord);
        this.addField("to_y", to.yCoord);
        this.addField("to_z", to.zCoord);
        return this;
    }

    public LoggablePlayerEvent setFrom(Point3dLong from) {
        if(from == null){
            return this;
        }
        this.addField("from_x", from.xCoord);
        this.addField("from_y", from.yCoord);
        this.addField("from_z", from.zCoord);
        
        return this;
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
