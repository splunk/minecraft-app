package com.splunk.sharedmc.util;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Per-key time-window throttle, backed by a guava {@link Cache}. Used to prevent flooding
 * Splunk with high-frequency events (e.g. damage, food level changes, item pickups).
 * {@link #allow(String)} returns true at most once per window per key. Backed by a
 * size-bounded guava cache so it is safe for many players.
 */
public class EventThrottle {

    private static final int MAX_KEYS = 1024;

    private final long windowMillis;
    private final LongSupplier clock;
    private final Cache<String, Long> lastAllowed;

    /**
     * Constructor using the real system clock.
     *
     * @param windowMillis Minimum time, in milliseconds, between two allowed calls for the
     *                      same key.
     */
    public EventThrottle(long windowMillis) {
        this(windowMillis, System::currentTimeMillis);
    }

    /**
     * Testable constructor with an injectable clock. The {@code clock} param exists so the
     * throttle can be unit-tested without depending on real wall-clock time.
     *
     * @param windowMillis Minimum time, in milliseconds, between two allowed calls for the
     *                      same key.
     * @param clock Supplies the current time; injected so tests can control elapsed time.
     */
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
