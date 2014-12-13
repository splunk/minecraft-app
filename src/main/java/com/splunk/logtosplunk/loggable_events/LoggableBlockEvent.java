package com.splunk.logtosplunk.loggable_events;

import com.splunk.logtosplunk.actions.BlockEventAction;

import net.minecraft.util.Vec3;

/**
 * Almost pojo with fields for information that might be associated with a block event.
 */
public class LoggableBlockEvent extends AbstractLoggableEvent {
    private String playerName;
    private String cause;
    private BlockEventAction action;
    private String blockName;

    /**
     * Constructor.
     *
     * @param action The type of block action this represents, e.g. 'break'.
     */
    public LoggableBlockEvent(BlockEventAction action, long gameTime, String worldName, Vec3 location) {
        super(LoggableEventType.BLOCK, gameTime, worldName, location);
        this.action = action;
    }

    /**
     * unused
     */
    public String getCause() {
        return cause;
    }

    /**
     * unused
     */
    public LoggableBlockEvent setCause(String cause) {
        this.cause = cause;
        return this;
    }

    public String getPlayerName() {
        return playerName;
    }

    public LoggableBlockEvent setPlayerName(String playerName) {
        this.playerName = playerName;
        return this;
    }

    public BlockEventAction getAction() {
        return action;
    }

    public String getBlockName() {
        return blockName;
    }

    public LoggableBlockEvent setBlockName(String blockName) {
        this.blockName = blockName;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getType().getEventName()).append(": ").append(getAction().asString());
        if (playerName != null) {
            b.append(", By: ").append(playerName);
        }
        if (blockName != null) {
            b.append(", Block: " + blockName);
        }
        if (cause != null) {
            b.append(", Cause: " + cause);
        }
        b.append(" " + getLocation());

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

        final LoggableBlockEvent that = (LoggableBlockEvent) o;

        if (action != that.action) {
            return false;
        }
        //Seems like vec3's equals is borked.
        if (getCoordinates() != null ? !getCoordinates().toString().equals(that.getCoordinates().toString()) :
                that.getCoordinates() != null) {
            return false;
        }

        if (playerName != null ? !playerName.equals(that.playerName) : that.playerName != null) {
            return false;
        }
        if (blockName != null ? !blockName.equals(that.blockName) : that.blockName != null) {
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
        result = 31 * result + (cause != null ? cause.hashCode() : 0);
        result = 31 * result + (blockName != null ? blockName.hashCode() : 0);
        return result;
    }
}
