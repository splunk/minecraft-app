package com.splunk.logtosplunk.loggable_events;

import com.splunk.logtosplunk.actions.PlayerEventAction;

import net.minecraft.util.Vec3;

/**
 * Almost pojo with fields for information that might be associated with a player event.
 */
public class LoggablePlayerEvent extends AbstractLoggableEvent {
    private String playerName;
    private PlayerEventAction action;
    private String message;
    private String reason;

    /**
     * Constructor.
     *
     * @param action The type of player action this represents, e.g. 'player_disconnected'.
     */
    public LoggablePlayerEvent(PlayerEventAction action, long gameTime, String worldName, Vec3 location) {
        super(LoggableEventType.PLAYER,gameTime,worldName,location);
        this.action = action;
    }

    public String getPlayerName() {
        return playerName;
    }

    public LoggablePlayerEvent setPlayerName(String playerName) {
        this.playerName = playerName;
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
        return this;
    }

    public String getReason() {
        return reason;
    }

    public LoggablePlayerEvent setReason(String reason) {
        this.reason = reason;
        return this;
    }

    @Override
    public String toString(){
        StringBuilder b = new StringBuilder();
        b.append(getType().getEventName()).append(": ").append(getAction().asString());
        if(playerName != null){
            b.append(", By: ").append(playerName);
        }
        b.append(" " + getLocation());

        if(reason != null){
            b.append(", Reason: ").append(reason);
        }
        if(message != null){
            b.append(", Message: ").append(message);
        }
        return b.toString();
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
        //Seems like vec3's equals is borked.
        if (getCoordinates() != null ? !getCoordinates().toString().equals(that.getCoordinates().toString()) : that.getCoordinates() != null) {
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
        if ( getWorldName() != null ? !getWorldName().equals(that.getWorldName()) : that.getWorldName() != null) {
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
}