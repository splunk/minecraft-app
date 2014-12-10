package com.splunk.logtosplunk.actions;

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
    private String action;

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
