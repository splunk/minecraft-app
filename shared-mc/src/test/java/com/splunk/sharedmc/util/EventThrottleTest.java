package com.splunk.sharedmc.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class EventThrottleTest {
    @Test
    public void firstEventForKeyPasses_secondWithinWindowBlocked() {
        long[] now = {1_000L};
        EventThrottle throttle = new EventThrottle(1000L, () -> now[0]);

        assertTrue("first event passes", throttle.allow("Steve"));
        now[0] = 1_500L; // 500ms later, inside window
        assertFalse("second event inside window blocked", throttle.allow("Steve"));
        now[0] = 2_100L; // 1100ms after first, outside window
        assertTrue("event after window passes", throttle.allow("Steve"));
    }

    @Test
    public void differentKeysAreIndependent() {
        long[] now = {0L};
        EventThrottle throttle = new EventThrottle(1000L, () -> now[0]);
        assertTrue(throttle.allow("Steve"));
        assertTrue(throttle.allow("Alex"));
    }
}
