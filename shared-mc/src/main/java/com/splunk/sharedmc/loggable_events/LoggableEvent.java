package com.splunk.sharedmc.loggable_events;

import com.splunk.sharedmc.Point3dLong;

public interface LoggableEvent {

    /**
     * Gets a JSON String of this object.
     *
     * @return JSON representing this object.
     */
    String toJson();
}
