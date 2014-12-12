package com.splunk.logtosplunk.actions;

/**
 * Different types of actions that can occur as part of a BlockEvent.
 */
public enum BlockEventAction {
    BREAK("block_broken"),
    PLACE("block_placed");

    /**
     * The name of the action.
     */
    private String action;

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
