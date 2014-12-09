package com.splunk.logtosplunk.loggable_events;

/**
 * Categories of loggable events.
 */
public enum LoggableEventType {
    PLAYER("PlayerEvent");

    private final String eventName;

    LoggableEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
