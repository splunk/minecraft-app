package com.splunk.sharedmc.loggable_events;

/**
 * Categories of loggable events. Includes the extended logging categories SERVER,
 * PERFORMANCE, COMBAT, ITEM, and PROGRESSION added alongside the original PLAYER, BLOCK,
 * and DEATH categories.
 */
public enum LoggableEventType {
    PLAYER("PlayerEvent"),
    BLOCK("BlockEvent"),
    DEATH("DeathEvent"),
    SERVER("ServerEvent"),
    PERFORMANCE("PerformanceEvent"),
    COMBAT("CombatEvent"),
    ITEM("ItemEvent"),
    PROGRESSION("ProgressionEvent");

    private final String eventName;

    LoggableEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
