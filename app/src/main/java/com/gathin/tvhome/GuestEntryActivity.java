package com.gathin.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

/** Opens the guest interface even when the stock launcher is the default home. */
public final class GuestEntryActivity extends Activity {
 @Override protected void onCreate(Bundle state) {
  super.onCreate(state);
  getPackageManager().setComponentEnabledSetting(new ComponentName(this,MainActivity.class),
    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
  startActivity(new Intent(this,MainActivity.class)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));
  finish();
 }
}
