package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;

/**
 * Item economy events: pickup and drop.
 */
public class LoggableItemEvent extends AbstractLoggableEvent {

    /**
     * Constructor.
     *
     * @param action The type of item action this represents, e.g. 'pickup' or 'drop'.
     */
    public LoggableItemEvent(ItemAction action, long gameTime, String worldName, Point3dLong location) {
        super(LoggableEventType.ITEM, gameTime, worldName, location);
        this.addField(ACTION, action.asString());
    }

    public LoggableItemEvent setPlayerName(String playerName) {
        this.addField(PLAYER_NAME, playerName);
        return this;
    }

    public LoggableItemEvent setItem(String item) {
        this.addField("item", item);
        return this;
    }

    public LoggableItemEvent setQuantity(int quantity) {
        this.addField("quantity", quantity);
        return this;
    }

    public enum ItemAction {
        PICKUP("pickup"),
        DROP("drop");

        private final String action;
        ItemAction(String action) { this.action = action; }
        public String asString() { return action; }
    }
}
