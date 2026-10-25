package com.gathin.tvhome;

/** Fixed reset scope: never clear the launcher, tuner, Play Services or device settings. */
final class StayReset {
    static final String[] PACKAGES = {"com.amazon.amazonvideo.livingroom", "com.spotify.tv.android",
            "com.netflix.ninja", "com.google.android.youtube.tv"};
    static final String[] LABELS = {"Prime Video", "Spotify", "Netflix", "YouTube", "Account Google del TV"};
    static final int STEPS = 5;
    static boolean complete(String[] results) {
        if (results.length != STEPS) return false;
        for (String result : results)
            if (!"automatico".equals(result) && !"manuale".equals(result)) return false;
        // Google accounts always require the owner's explicit check in system settings.
        return "manuale".equals(results[4]);
    }
    private StayReset() {}
}
