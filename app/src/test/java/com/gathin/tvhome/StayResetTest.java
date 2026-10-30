package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class StayResetTest {
    @Test public void incompleteOrFailedResetCannotReleaseNewStay() {
        String[] results={"automatico","automatico","automatico","automatico","manuale"};
        assertTrue(StayReset.complete(results));
        for(int i=0;i<StayReset.STEPS;i++) {
            String saved=results[i];
            for(String incomplete:new String[]{"",null,"errore","in corso","da verificare"}) {
                results[i]=incomplete;
                assertFalse(StayReset.complete(results));
            }
            results[i]=saved;
        }
    }
    @Test public void googleRequiresExplicitManualCheckEvenAfterAppReset() {
        assertFalse(StayReset.complete(new String[]{"automatico","automatico","automatico","automatico","automatico"}));
        assertTrue(StayReset.complete(new String[]{"manuale","manuale","manuale","manuale","manuale"}));
        assertFalse(StayReset.complete(new String[0]));
        assertFalse(StayReset.complete(new String[6]));
    }
    @Test public void destructiveScopeIsExactlyFourStreamingApps() {
        assertArrayEquals(new String[]{"com.amazon.amazonvideo.livingroom","com.spotify.tv.android",
                "com.netflix.ninja","com.google.android.youtube.tv"},StayReset.PACKAGES);
        assertEquals(StayReset.STEPS,StayReset.LABELS.length);
    }
}
