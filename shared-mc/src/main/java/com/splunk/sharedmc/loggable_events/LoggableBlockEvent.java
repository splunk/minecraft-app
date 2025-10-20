package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;

/**
 * Almost pojo with fields for information that might be associated with a block event.
 */
public class LoggableBlockEvent extends AbstractLoggableEvent {
    public static final String BASE_TYPE = "base_type";
    public static final String BLOCK_NAME = "block_type";

    /**
     * Constructor.
     *
     * @param action The type of block action this represents, e.g. 'break'.
     */
    public LoggableBlockEvent(BlockEventAction action, long gameTime, String worldName, Point3dLong location, String blockName, String playerName) {
        super(LoggableEventType.BLOCK, gameTime, worldName, location);
        setBlockName(blockName);
        setPlayerName(playerName);
        this.addField(ACTION, action.asString().toUpperCase());
    }

    public void setPlayerName(String playerName) {
        this.addField(PLAYER_NAME, playerName);
    }

    public void setBlockName(String blockName) {
        this.addField(BLOCK_NAME, blockName);
    }

    public void setBaseType(String baseType) {
        this.addField(BASE_TYPE, baseType);
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
