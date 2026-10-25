package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restores the guest screen once when Android finishes booting. */
public final class GuestBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            android.util.Log.i("GuestTV","Boot completed: opening guest home");
            PlayKeyBlocker.restoreGuestHome();
        }
    }
}
