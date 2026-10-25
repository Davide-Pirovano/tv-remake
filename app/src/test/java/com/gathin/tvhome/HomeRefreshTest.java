package com.gathin.tvhome;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HomeRefreshTest {
    private long delay(String instant, String zone) {
        return HomeRefresh.nextDelayMillis(false, Instant.parse(instant).toEpochMilli(), ZoneId.of(zone));
    }

    @Test public void staticHomeUpdatesAtGreetingBoundaryInsteadOfAnHourAfterOpening() {
        assertEquals(500L, delay("2026-10-25T16:59:59.500Z", "UTC"));
        assertEquals(3_600_000L, delay("2026-10-25T17:00:00Z", "UTC"));
    }

    @Test public void localHourInQuarterHourTimezoneIsRespected() {
        assertEquals(300_000L, delay("2026-10-25T12:10:00Z", "Asia/Kathmandu"));
    }

    @Test public void daylightSavingTransitionsDoNotDelayTheNextUpdate() {
        assertEquals(30_000L, delay("2026-10-25T00:59:30Z", "Europe/Rome"));
        assertEquals(30_000L, delay("2026-03-29T00:59:30Z", "Europe/Rome"));
    }

    @Test public void liveFeaturesRetainMinuteUpdates() {
        assertEquals(60_000L, HomeRefresh.nextDelayMillis(true, 0L, ZoneId.of("UTC")));
    }
}
