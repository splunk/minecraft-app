package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AbstractLoggableEventTest {

    @Test
    public void locationLessConstructor_doesNotNpe_andOmitsCoords() {
        AbstractLoggableEvent e =
                new AbstractLoggableEvent(LoggableEventType.SERVER, 0L, "world");
        String json = e.toJson();
        assertTrue(json.contains("SERVER".toLowerCase()) || json.contains("ServerEvent"));
        assertFalse("location-less event must not emit xCoord", json.contains("xCoord"));
    }
}
