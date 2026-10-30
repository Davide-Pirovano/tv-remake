package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Routes Home to the guest launcher and blocks one IR key. No screen access or polling. */
public final class PlayKeyBlocker extends AccessibilityService {
    private static PlayKeyBlocker connected;
    static boolean isStoreKey(int scanCode,String deviceName) {
        return scanCode==501 && "MStar Smart TV IR Receiver".equals(deviceName);
    }
    static boolean isConnected(){return connected!=null;}
    static void restoreGuestHome(){if(connected!=null)connected.guestHome();}
    private android.content.BroadcastReceiver unlockReceiver;
    private void guestHome() {
        try {
            startActivity(new android.content.Intent(this,MainActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK|android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }catch(RuntimeException e){android.util.Log.e("GuestTV","Cannot open guest home",e);}
    }
    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        stopWaitingForUnlock();
        connected=this;
        android.os.UserManager users=getSystemService(android.os.UserManager.class);
        if(users.isUserUnlocked()){guestHome();return;}
        unlockReceiver=new android.content.BroadcastReceiver(){
            @Override public void onReceive(android.content.Context context,android.content.Intent intent){
                stopWaitingForUnlock();guestHome();
            }
        };
        registerReceiver(unlockReceiver,new android.content.IntentFilter(android.content.Intent.ACTION_USER_UNLOCKED));
        if(users.isUserUnlocked()){stopWaitingForUnlock();guestHome();}
    }
    private void stopWaitingForUnlock(){
        if(unlockReceiver!=null){unregisterReceiver(unlockReceiver);unlockReceiver=null;}
    }
    @Override public boolean onUnbind(android.content.Intent intent){
        stopWaitingForUnlock();if(connected==this)connected=null;
        return super.onUnbind(intent);
    }
    @Override public void onDestroy(){stopWaitingForUnlock();if(connected==this)connected=null;super.onDestroy();}
    @Override protected boolean onKeyEvent(KeyEvent event) {
        if(event.getKeyCode()==KeyEvent.KEYCODE_HOME){
            if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled()){
                GuestActivity.cancelCleanupReturn();
                android.util.Log.i("GuestTV","Home key routed to guest home");guestHome();
            }
            return true;
        }
        if(event.getScanCode()!=501)return false;
        android.view.InputDevice device=event.getDevice();
        if(device==null||!isStoreKey(event.getScanCode(),device.getName()))return false;
        if(event.getAction()==KeyEvent.ACTION_UP)android.util.Log.i("GuestTV","Physical Google Play key blocked");
        return true;
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){}
}
