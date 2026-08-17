package com.splunk.sharedmc.loggable_events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.splunk.logging.SplunkCimLogEvent;
import com.splunk.sharedmc.Point3dLong;

/**
 * Classes extending this benefit from a convenient way to get a Json representation, time of creation and event type,
 * world name, coordinates and location.
 */
public class AbstractLoggableEvent extends SplunkCimLogEvent implements LoggableEvent {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final String PLAYER_NAME = "player";
    public static final String CAUSE = "cause";
    public static final String ACTION = "action";

    /**
     * Constructor. Enforces that subclasses must have a loggable event type. Null-checks
     * {@code coordinates} before dereferencing it (fixes a prior NPE risk when an event has
     * no location, e.g. server lifecycle events) and delegates to the 3-arg constructor.
     *
     * @param type The type of event that this is.
     */
    public AbstractLoggableEvent(LoggableEventType type, long worldTime, String worldName, Point3dLong coordinates) {
        this(type, worldTime, worldName);
        if (coordinates != null) {
            this.addField("xCoord", coordinates.xCoord);
            this.addField("yCoord", coordinates.yCoord);
            this.addField("zCoord", coordinates.zCoord);
        }
    }

    /**
     * Constructor for events with no world location (e.g. server lifecycle, performance).
     */
    public AbstractLoggableEvent(LoggableEventType type, long worldTime, String worldName) {
        super(type.getEventName(), "");
        this.addField("time", System.currentTimeMillis());
        this.addField("game_time", worldTime);
        if (worldName != null) {
            this.addField("world", worldName);
        }
    }

    @Override
    public String toJson() {
        return gson.toJson(this);
    }

    @Override
    public void addField(String key, Object value){
        if(value == null){
            return;
        }
        super.addField(key, value);
    }
}
