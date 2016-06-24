package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;

/**
 * Almost pojo with fields for information that might be associated with a player event.
 */
public class LoggablePlayerEvent extends AbstractLoggableEvent {

    public static final String MESSAGE = "message";
    public static final String REASON = "reason";
    public static final String ITEM = "item";

    private String playerName;
    private final PlayerEventAction action;
    private String message;
    private String reason;
    private String item;

    private Point3dLong from;
    private Point3dLong to;

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
        if (message != null)
            this.addField(MESSAGE, "'" + message + "'");
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

    public String getItem() { return item;}

    public LoggablePlayerEvent setItem( String item){
        this.item = item;
        this.addField(ITEM, item);
        return this;
    }

    public Point3dLong getTo() {
        return to;
    }

    public LoggablePlayerEvent setTo(Point3dLong to) {
        if (to == null) {
            return this;
        }
        this.to = to;
        this.addField("to_x", to.xCoord);
        this.addField("to_y", to.yCoord);
        this.addField("to_z", to.zCoord);
        return this;
    }

    public Point3dLong getFrom() {
        return from;
    }

    public LoggablePlayerEvent setFrom(Point3dLong from) {
        if (from == null) {
            return this;
        }
        this.from = from;
        this.addField("from_x", from.xCoord);
        this.addField("from_y", from.yCoord);
        this.addField("from_z", from.zCoord);

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoggablePlayerEvent)) {
            return false;
        }

        final LoggablePlayerEvent that = (LoggablePlayerEvent) o;

        if (action != that.action) {
            return false;
        }
        if (from != null ? !from.equals(that.from) : that.from != null) {
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
        if (to != null ? !to.equals(that.to) : that.to != null) {
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
        result = 31 * result + (from != null ? from.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
    }

    /**
     * Different types of actions that can occur as part of a PlayerEvent.
     */
    public enum PlayerEventAction {
        PLAYER_CONNECT("player_connect"),
        PLAYER_DISCONNECT("player_disconnect"),
        CHAT("player_chat"),
        MOVE("player_move"),
        TELEPORT("player_teleport"),
        EMPTY("bucket_empty");

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
