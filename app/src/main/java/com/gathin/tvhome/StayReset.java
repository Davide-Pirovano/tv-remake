package com.gathin.tvhome;

/** Fixed reset scope: never clear the launcher, tuner, Play Services or device settings. */
final class StayReset {
    static final String[] PACKAGES = {"com.amazon.amazonvideo.livingroom", "com.spotify.tv.android",
            "com.netflix.ninja", "com.google.android.youtube.tv"};
    static final String[] LABELS = {"Prime Video", "Spotify", "Netflix", "YouTube", "Account Google del TV"};
    static final int STEPS = 5;
    static boolean complete(String[] results) {
        if (results.length != STEPS) return false;
        for (int i=0;i<PACKAGES.length;i++)
            if (!"automatico".equals(results[i]) && !"manuale".equals(results[i]) && !"da verificare".equals(results[i])) return false;
        return "manuale".equals(results[4]) || "automatico".equals(results[4]);
    }
    private StayReset() {}
}
