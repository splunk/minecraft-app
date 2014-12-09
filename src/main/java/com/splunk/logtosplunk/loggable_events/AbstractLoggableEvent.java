package com.splunk.logtosplunk.loggable_events;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.util.Vec3;

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
    private long time = System.currentTimeMillis();

    /**
     * The in-game time of this event.
     */
    private final long worldTime;

    /**
     * World the event occurred in.
     */
    private String worldName;

    /**
     * Coordinates where the event occurred.
     */
    private Vec3 coordinates;

    /**
     * Constructor. Enforces that subclasses must have a loggable event type.
     *
     * @param type The type of event that this is.
     */
    public AbstractLoggableEvent(LoggableEventType type, long worldTime, String worldName, Vec3 coordinates) {
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
    public Vec3 getCoordinates() {
        return coordinates;
    }

    @Override
    public String getLocation() {
        StringBuilder b = new StringBuilder();
        b.append("@ World: " + worldName);
        if (getCoordinates() != null) {
            b.append(
                    ", " + "x:" + coordinates.xCoord +
                            " " + "y:" + coordinates.yCoord +
                            " " + "z:" + coordinates.zCoord);
        }
        b.append(", WorldTime:" + worldTime);
        return b.toString();
    }
}
