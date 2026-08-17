package com.splunk.sharedmc.loggable_events;

import static org.junit.Assert.assertTrue;
import com.splunk.sharedmc.Point3dLong;
import org.junit.Test;

public class LoggableItemEventTest {
    @Test
    public void pickup_carriesItemAndQuantity() {
        LoggableItemEvent e = new LoggableItemEvent(
                LoggableItemEvent.ItemAction.PICKUP, 0L, "world", new Point3dLong(0, 64, 0));
        e.setPlayerName("Steve").setItem("DIAMOND").setQuantity(3);
        String json = e.toJson();
        assertTrue(json.contains("pickup"));
        assertTrue(json.contains("DIAMOND"));
        assertTrue(json.contains("quantity"));
    }
}
