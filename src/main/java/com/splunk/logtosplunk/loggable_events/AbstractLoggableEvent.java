package com.splunk.logtosplunk.loggable_events;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.splunk.logtosplunk.Point3dLong;

/**
 * Classes extending this benefit from a convenient way to get a Json representation, time of creation and event type,
 * world name, coordinates and location.
 */
public class AbstractLoggableEvent implements LoggableEvent {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * General event type. Can be used to categorize events.
     */
    private final LoggableEventType type;

    /**
     * Time of this objects initialization.
     */
    private final long time = System.currentTimeMillis();

    /**
     * The in-game time of this event.
     */
    private final long worldTime;

    /**
     * World the event occurred in.
     */
    private final String worldName;

    /**
     * Coordinates where the event occurred.
     */
    private final Point3dLong coordinates;

    /**
     * Constructor. Enforces that subclasses must have a loggable event type.
     *
     * @param type The type of event that this is.
     */
    public AbstractLoggableEvent(LoggableEventType type, long worldTime, String worldName, Point3dLong coordinates) {
        this.type = type;
        this.worldTime = worldTime;
        this.worldName = worldName;
        this.coordinates = coordinates;
    }

    @Override
    public String toJson() {
        return gson.toJson(this);
    }

    @Override
    public LoggableEventType getType() {
        return type;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public long getWorldTime() {
        return worldTime;
    }

    @Override
    public String getWorldName() {
        return worldName;
    }

    @Nullable
    @Override
    public Point3dLong getCoordinates() {
        return coordinates;
    }

    @Override
    public String getLocation() {
        final StringBuilder b = new StringBuilder();
        b.append("@ World: ").append(worldName);
        if (getCoordinates() != null) {
            b.append(", " + "x:").append(coordinates.xCoord).append(' ').append("y:").append(coordinates.yCoord)
                    .append(' ').append("z:").append(coordinates.zCoord);
        }
        b.append(", WorldTime:").append(worldTime);
        return b.toString();
    }
}
