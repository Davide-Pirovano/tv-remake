package com.gathin.tvhome;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Optional live features need minute updates; the static home only changes greetings each hour. */
final class HomeRefresh {
    private HomeRefresh() { }

    static long nextDelayMillis(boolean minuteUpdates, long now, ZoneId zone) {
        if (minuteUpdates) return 60_000L;
        long nextHour = Instant.ofEpochMilli(now).atZone(zone).plusHours(1)
                .truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli();
        return Math.max(1L, nextHour - now);
    }
}
