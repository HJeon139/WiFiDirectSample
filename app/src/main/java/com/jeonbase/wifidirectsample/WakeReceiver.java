package com.jeonbase.wifidirectsample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Hohyun on 4/17/2015.
 * Handles sleep->wake cycle
 */

public class WakeReceiver extends BroadcastReceiver {
    public static final String ACTION_PASSIVE = "com.example.android.wifidirect.PASSIVE";
    WakefulReceiver alarm = new WakefulReceiver();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_PASSIVE))
        {
            alarm.setAlarm(context);
        }
    }
}
