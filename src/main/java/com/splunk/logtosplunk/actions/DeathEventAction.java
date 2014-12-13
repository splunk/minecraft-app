package com.splunk.logtosplunk.actions;

/**
 * Different types of actions that can occur as part of a DeathEvent.
 */
public enum DeathEventAction {
    MOB_DIED("mob_died"),
    PLAYER_DIED("player_died");

    /**
     * The name of the action.
     */
    private String action;

    DeathEventAction(String action) {
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
