package com.jeonbase.wifidirectsample;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * Created by Hohyun on 4/17/2015.
 */
public class PassiveScheduler extends IntentService implements WifiP2pManager.ChannelListener, DeviceListFragment.DeviceActionListener {

    public PassiveScheduler(){
        super("PassiveScheduler");
    }

    public static final String TAG = "Micronet Demo";
    public static final int NOTIFICATION_ID = 1;

    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = true;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;



    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(WiFiDirectActivity.TAG, "Passive: Starting...");
        sendNotification("Micronet Status Updated:", "Passive mode active");
        //passive stuff
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);


        resetData();

        Log.d(WiFiDirectActivity.TAG, "Passive: End...");
        WakefulReceiver.completeWakefulIntent(intent);

    }

    private void sendNotification(String title, String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, WiFiDirectActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    /*public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }*/

    @Override
    public void showDetails(WifiP2pDevice device) {
       /* DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);*/

    }

    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                sendNotification("Micronet Status Update:", "Connection Failed... Retry?");
            }
        });
    }

    @Override
    public void disconnect() {
        /*final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                *//*fragment.getView().setVisibility(View.GONE);*//*
            }

        });*/
    }

    public void resetData() {
        if (!isWifiP2pEnabled) {
            sendNotification("Micronet Status Update:", "WiFi Direct is Disabled");

        }else {

            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    sendNotification("Micronet Status Update:", "Discovery Initiated");
                    Log.d(WiFiDirectActivity.TAG, "Discovery Initiated");
                }

                @Override
                public void onFailure(int reasonCode) {
                    sendNotification("Micronet Status Update:", "Discovery Failed: "+reasonCode);
                    Log.d(WiFiDirectActivity.TAG, "Discovery Failed: "+reasonCode);
                }
            });
            Log.d(WiFiDirectActivity.TAG, "Passive: Discovery: End");
        }
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            sendNotification("Micronet Status Update:", "Channel lost. Trying again");
            //Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();

            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            /*Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();*/
            sendNotification("Micronet Status Update:", "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.");
        }
    }

    @Override
    public void cancelDisconnect() {

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        /*if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        sendNotification("Micronet Status Update:", "Channel lost. Trying again");
                        Toast.makeText(PassiveScheduler.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(PassiveScheduler.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
*/
    }
}
