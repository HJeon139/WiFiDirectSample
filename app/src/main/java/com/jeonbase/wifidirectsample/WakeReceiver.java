package com.jeonbase.wifidirectsample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created by Hohyun on 4/17/2015.
 * Handles sleep->wake cycle
 */

public class WakeReceiver extends BroadcastReceiver {
    public static final String ACTION_PASSIVE = "com.example.android.wifidirect.PASSIVE";
    WakefulReceiver alarm = new WakefulReceiver();



    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(WiFiDirectActivity.TAG, "WakeReceive:\t"+ Long.toString(SystemClock.elapsedRealtime()));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Log.e(WiFiDirectActivity.TAG, e.getMessage());
        }

        context.stopService(new Intent(context, PassiveScheduler.class));
        alarm.setAlarm(context);

    }
}
