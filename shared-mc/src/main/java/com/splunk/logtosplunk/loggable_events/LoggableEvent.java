package com.splunk.logtosplunk.loggable_events;

import com.splunk.logtosplunk.Point3dLong;

public interface LoggableEvent {

    /**
     * Gets a JSON String of this object.
     *
     * @return JSON representing this object.
     */
    String toJson();

    /**
     * Get the type of event that occurred, used to categorize events.
     *
     * @return The type of event that occurred.
     */
    LoggableEventType getType();

    /**
     * System time in milliseconds that this event was initialized.
     *
     * @return System time in milliseconds that this event was initialized.
     */
    long getTime();

    /**
     * In-game time.
     *
     * @return A long representing in-game time.
     */
    long getWorldTime();

    /**
     * Gets the name of the current world.
     *
     * @return Name of the current world.
     */
    String getWorldName();

    /**
     * String representation of where the event occurred.
     *
     * @return If the event occurred at a location, the location it occurred at.
     */
    Point3dLong getCoordinates();

    /**
     * Gets a String representation of where this event occurred.
     *
     * @return A String ofwhere this event occurred.
     */
    String getLocation();
}
