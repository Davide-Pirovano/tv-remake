package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/** Only acts after an owner starts a reset, and only on known Android TV Settings screens. */
public final class GuestCleanupService extends AccessibilityService {
    private static GuestCleanupService connected;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int index, phase;
    private long deadline;
    private String session, label;
    private boolean running;
    private android.view.View overlay;
    private android.widget.TextView message;
    static boolean available() { return connected != null; }
    static boolean active() { return connected != null && connected.running; }
    static void begin() { if (connected != null) connected.startCleanup(); }
    @Override protected void onServiceConnected() { connected=this; }
    @Override public boolean onUnbind(Intent intent) { stop(false); if(connected==this)connected=null; return super.onUnbind(intent); }
    @Override public void onDestroy() { stop(false); if(connected==this)connected=null; super.onDestroy(); }
    @Override public void onInterrupt() { stop(true); }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(running && event.getPackageName()!=null && event.getPackageName().toString().equals("com.android.tv.settings"))
            schedule(400);
    }
    private void startCleanup() {
        if(running || !GuestActivity.prefs(this).getBoolean("reset_pending",false))return;
        session=GuestActivity.prefs(this).getString("reset_session","");
        if(session.isEmpty())return;
        index=0;running=true;
        try { showOverlay(); } catch(RuntimeException e) {stop(true);return;}
        next();
    }
    private void next() {
        if(!running)return;
        if(index==StayReset.PACKAGES.length) {
            if(!GuestActivity.prefs(this).edit().remove("reset_step_4").commit()) {stop(true);return;}
            message.setText("Controllo account Google");
            phase=3;deadline=android.os.SystemClock.elapsedRealtime()+15000;
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK));
                schedule(600);
            } catch(RuntimeException e) {stop(true);}
            return;
        }
        if(!session.equals(GuestActivity.prefs(this).getString("reset_session",""))) {stop(false);return;}
        try {
            message.setText("Pulizia app in corso\n"+StayReset.LABELS[index]+" · "+(index+1)+" / 4");
            label=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(StayReset.PACKAGES[index],0)).toString();
            if(!GuestActivity.prefs(this).edit().remove("reset_step_"+index).commit()) {stop(true);return;}
            phase=0;deadline=android.os.SystemClock.elapsedRealtime()+15000;
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:"+StayReset.PACKAGES[index])).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK));
            schedule(600);
        }catch(PackageManager.NameNotFoundException | SecurityException | android.content.ActivityNotFoundException e){stop(true);}
    }
    private final Runnable inspect=this::inspect;
    private void schedule(long delay){handler.removeCallbacks(inspect);handler.postDelayed(inspect,delay);}
    private void inspect() {
        if(!running)return;
        if(android.os.SystemClock.elapsedRealtime()>deadline) {android.util.Log.w("GuestCleanup","Timeout step="+index+" phase="+phase);stop(true);return;}
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null || !"com.android.tv.settings".contentEquals(root.getPackageName())) {schedule(500);return;}
        try {
            if(phase==3) {
                if(noAccounts(root)) {
                    if(!session.equals(GuestActivity.prefs(this).getString("reset_session",""))
                            || !GuestActivity.prefs(this).edit().putString("reset_step_4","automatico").commit()) {stop(true);return;}
                    android.util.Log.i("GuestCleanup","Android reports no accounts");stop(true);return;
                }
                if(has(root,"android:id/title","Account e accesso")) {
                    android.util.Log.i("GuestCleanup","Accounts require manual removal");stop(true);return;
                }
            } else if(phase==0 && has(root,"android:id/summary",StayReset.PACKAGES[index])
                    && click(root,"android:id/title","Cancellare dati")) {
                android.util.Log.i("GuestCleanup","Opened confirmation step="+index);phase=1;deadline=android.os.SystemClock.elapsedRealtime()+15000;
            } else if(phase==1 && has(root,"com.android.tv.settings:id/guidance_breadcrumb",label)
                    && has(root,"com.android.tv.settings:id/guidance_title","Cancellare dati")
                    && click(root,"com.android.tv.settings:id/guidedactions_item_title","OK")) {
                android.util.Log.i("GuestCleanup","Requested data clear step="+index);phase=2;deadline=android.os.SystemClock.elapsedRealtime()+15000;
            } else if(phase==2 && has(root,"android:id/summary",StayReset.PACKAGES[index])
                    && has(root,"android:id/title","Cancellare dati")) {
                // Settings returned from its confirmation. This is not an Android package-manager success callback.
                if(!session.equals(GuestActivity.prefs(this).getString("reset_session",""))
                        || !GuestActivity.prefs(this).edit().putString("reset_step_"+index,"da verificare").commit()) {stop(true);return;}
                android.util.Log.i("GuestCleanup","Returned to app info step="+index);index++;next();return;
            }
        }finally {root.recycle();}
        schedule(500);
    }
    /** Only accept Android's explicit empty-account summary in the Accounts row. */
    private boolean noAccounts(AccessibilityNodeInfo root) {
        for(AccessibilityNodeInfo title:root.findAccessibilityNodeInfosByViewId("android:id/title")) {
            if(!"Account e accesso".contentEquals(title.getText()==null?"":title.getText())) {title.recycle();continue;}
            AccessibilityNodeInfo row=title.getParent();
            while(row!=null) {
                if(has(row,"android:id/summary","Nessun account")) {row.recycle();title.recycle();return true;}
                if(row.isClickable()) {row.recycle();break;}
                AccessibilityNodeInfo parent=row.getParent();row.recycle();row=parent;
            }
            title.recycle();
        }
        return false;
    }
    private boolean has(AccessibilityNodeInfo root,String id,String text) {
        for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId(id)) {
            boolean match=text.contentEquals(n.getText()==null?"":n.getText());n.recycle();if(match)return true;
        }
        return false;
    }
    private boolean click(AccessibilityNodeInfo root,String id,String text) {
        for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByViewId(id)) {
            if(!text.contentEquals(n.getText()==null?"":n.getText())) {n.recycle();continue;}
            AccessibilityNodeInfo current=n;
            while(current!=null) {
                if(current.isClickable() && current.isEnabled()) {
                    boolean result=current.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    if(current!=n)current.recycle();n.recycle();return result;
                }
                AccessibilityNodeInfo parent=current.getParent();if(current!=n)current.recycle();current=parent;
            }
            n.recycle();
        }
        return false;
    }
    private void showOverlay() {
        android.widget.LinearLayout box=new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setGravity(android.view.Gravity.CENTER);
        box.setBackgroundColor(GuestStyle.CANVAS);
        android.widget.ProgressBar progress=new android.widget.ProgressBar(this);
        box.addView(progress);
        message=new android.widget.TextView(this);
        message.setTextColor(GuestStyle.MIST);message.setTextSize(25);
        message.setGravity(android.view.Gravity.CENTER);message.setPadding(0,32,0,0);
        box.addView(message);
        android.view.WindowManager.LayoutParams lp=new android.view.WindowManager.LayoutParams(
                -1,-1,android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.OPAQUE);
        getSystemService(android.view.WindowManager.class).addView(box,lp);overlay=box;
    }
    private void stop(boolean show) {
        boolean wasRunning=running;running=false;handler.removeCallbacks(inspect);
        if(overlay!=null) {
            try {getSystemService(android.view.WindowManager.class).removeView(overlay);}catch(RuntimeException ignored){}
            overlay=null;message=null;
        }
        if(!show)GuestActivity.cancelCleanupReturn();
        if(show && wasRunning) {
            try {startActivity(new Intent(this,GuestActivity.class).putExtra("owner",true).putExtra("cleanup_return",true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));}
            catch(RuntimeException ignored){GuestActivity.cancelCleanupReturn();}
        }
    }
}
