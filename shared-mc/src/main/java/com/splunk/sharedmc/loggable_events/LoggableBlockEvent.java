package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;

/**
 * Almost pojo with fields for information that might be associated with a block event.
 */
public class LoggableBlockEvent extends AbstractLoggableEvent {
    public static final String BASE_TYPE = "base_type";
    public static final String BLOCK_NAME = "block_type";

    private String playerName;
    private String cause;
    private final BlockEventAction action;
    private String blockName;
    private String baseType;

    /**
     * Constructor.
     *
     * @param action The type of block action this represents, e.g. 'break'.
     */
    public LoggableBlockEvent(BlockEventAction action, long gameTime, String worldName, Point3dLong location) {
        super(LoggableEventType.BLOCK, gameTime, worldName, location);
        this.action = action;
        this.addField(ACTION, action.asString());
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
        this.addField(CAUSE, cause);
        return this;
    }

    public String getPlayerName() {
        return playerName;
    }

    public LoggableBlockEvent setPlayerName(String playerName) {
        this.playerName = playerName;
        this.addField(PLAYER_NAME, playerName);
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
        this.addField(BLOCK_NAME, blockName);
        return this;
    }

    public String getBaseType() {
        return baseType;
    }

    public LoggableBlockEvent setBaseType(String baseType) {
        this.baseType = baseType;
        this.addField(BASE_TYPE, baseType);
        return this;
    }

    //    @Override
    //    public String toString() {
    //        final StringBuilder b = new StringBuilder();
    //        b.append(getType().getEventName()).append(": ").append(getAction().asString());
    //        if (playerName != null) {
    //            b.append(", By: ").append(playerName);
    //        }
    //        if (blockName != null) {
    //            b.append(", Block: ").append(blockName);
    //        }
    //        if (cause != null) {
    //            b.append(", Cause: ").append(cause);
    //        }
    //        if (baseType != null) {
    //            b.append(", Material: ").append(baseType);
    //        }
    //        b.append(' ').append(getLocation());
    //
    //        return b.toString();
    //    }

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

        if (getCoordinates() != null ? !getCoordinates().equals(that.getCoordinates()) :
                that.getCoordinates() != null) {
            return false;
        }

        if (playerName != null ? !playerName.equals(that.playerName) : that.playerName != null) {
            return false;
        }
        if (blockName != null ? !blockName.equals(that.blockName) : that.blockName != null) {
            return false;
        }

        if (baseType != null ? !baseType.equals(that.baseType) : that.baseType != null) {
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
        result = 31 * result + (baseType != null ? baseType.hashCode() : 0);

        return result;
    }

    /**
     * Different types of actions that can occur as part of a BlockEvent.
     */
    public enum BlockEventAction {
        BREAK("block_broken"),
        PLACE("block_placed");

        /**
         * The name of the action.
         */
        private final String action;

        BlockEventAction(String action) {
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
