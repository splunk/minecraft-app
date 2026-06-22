package com.splunk.sharedmc.util;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Per-key time-window throttle. {@link #allow(String)} returns true at most once per
 * window per key. Backed by a size-bounded guava cache so it is safe for many players.
 */
public class EventThrottle {

    private static final int MAX_KEYS = 1024;

    private final long windowMillis;
    private final LongSupplier clock;
    private final Cache<String, Long> lastAllowed;

    public EventThrottle(long windowMillis) {
        this(windowMillis, System::currentTimeMillis);
    }

    /** Testable constructor with an injectable clock. */
    public EventThrottle(long windowMillis, LongSupplier clock) {
        this.windowMillis = windowMillis;
        this.clock = clock;
        this.lastAllowed = CacheBuilder.newBuilder()
                .maximumSize(MAX_KEYS)
                .expireAfterAccess(windowMillis * 4, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * @return true if an event for {@code key} should be sent now (first call, or the
     *         window since the last allowed call has elapsed); false to drop it.
     */
    public synchronized boolean allow(String key) {
        long now = clock.getAsLong();
        Long last = lastAllowed.getIfPresent(key);
        if (last == null || (now - last) >= windowMillis) {
            lastAllowed.put(key, now);
            return true;
        }
        return false;
    }
}
