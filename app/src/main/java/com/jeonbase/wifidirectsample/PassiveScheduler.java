package com.jeonbase.wifidirectsample;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hohyun on 4/17/2015.
 */
public class PassiveScheduler extends IntentService implements WifiP2pManager.ChannelListener, DeviceListFragment.DeviceActionListener, WifiP2pManager.ConnectionInfoListener {

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

    private WifiP2pInfo info;

    private List<WifiP2pDevice> p_peers = new ArrayList<WifiP2pDevice>();

    private WifiP2pManager.ConnectionInfoListener connectionListener = new WifiP2pManager.ConnectionInfoListener(){
        @Override
        public void onConnectionInfoAvailable(final WifiP2pInfo info) {
            PassiveScheduler.this.info = info;


            // InetAddress from WifiP2pInfo struct.
            Log.d(TAG, "Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

            // After the group negotiation, we assign the group owner as the file
            // server. The file server is single threaded, single connection server
            // socket.
            if (info.groupFormed && info.isGroupOwner) {
                //Listen
                new FileServerAsyncTask(PassiveScheduler.this).execute();
                Log.d(WiFiDirectActivity.TAG, "Listening for File");
            } else if (info.groupFormed) {
                // The device acts as the client. Send File
                //send data
                sendfile();

                //disconnect
                disconnect();
            }
        }
    };

    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            // Out with the old, in with the new.
            p_peers.clear();
            p_peers.addAll(peerList.getDeviceList());

            if (p_peers.size() == 0) {
                Log.d(WiFiDirectActivity.TAG, "No devices found");
                return;
            }
        }
    };

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

        //broadcastIntent.setAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        Intent broadcastIntent = new Intent(this, WakeReceiver.class);
        sendBroadcast(broadcastIntent);
        //stopDiscovery();

        Log.d(WiFiDirectActivity.TAG, "peer count: " + Integer.toString(p_peers.size()));
        if (p_peers.size()>0){
            stopDiscovery();
            for(int i=0; i<p_peers.size(); i++){
                connect(i);
            }
        }else{
            disconnect();
        }

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

    public void stopDiscovery(){
        manager.stopPeerDiscovery(channel,new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                String reason;
                switch(reasonCode){
                    case WifiP2pManager.P2P_UNSUPPORTED:
                        reason = "WiFi P2P is not supported on this device.";
                        break;
                    case WifiP2pManager.BUSY:
                        reason = "Framework is busy and unable to service this request.";
                        break;
                    case WifiP2pManager.ERROR:
                        reason = "Internal Error.";
                        break;
                    default:
                        reason = "Undefined failure error code: "+reasonCode;
                }
                Log.d(TAG, "Discovery Stop Failed:" + reason);

            }

            @Override
            public void onSuccess() {
                Log.d(TAG, "Discovery Stopped");

            }

        });
    }

    @Override
    public void connect(WifiP2pConfig config) {

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                transfer();
            }

            @Override
            public void onFailure(int reason) {
                sendNotification("Micronet Status Update:", "Connection Failed... Retry?");
            }
        });
    }

    public void connect(int peerNumber) {
        WifiP2pDevice device = p_peers.get(peerNumber);

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(WiFiDirectActivity.TAG, "Connected" );
                manager.requestConnectionInfo(channel, connectionListener);
                transfer();
            }

            @Override
            public void onFailure(int reason) {
                sendNotification("Micronet Status Update:", "Connection Failed... Retry?");
                Log.d(WiFiDirectActivity.TAG, "Connection Failed");
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        this.info = info;


        // InetAddress from WifiP2pInfo struct.
        Log.d(TAG, "Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            //Listen
            new FileServerAsyncTask(this).execute();
            Log.d(WiFiDirectActivity.TAG, "Listening for File");
        } else if (info.groupFormed) {
            // The device acts as the client. Send File
            //send data
            sendfile();

            //disconnect
            disconnect();
        }
    }

    public void transfer(){
        if (info.groupFormed && info.isGroupOwner) {
            //Listen
            new FileServerAsyncTask(this).execute();
            Log.d(WiFiDirectActivity.TAG, "Listening for File");
        } else if (info.groupFormed) {
            // The device acts as the client. Send File
            //send data
            sendfile();

            //disconnect
            disconnect();
        }
    }

    public void sendfile(){
        // FileTransferService.
        Log.d(WiFiDirectActivity.TAG, "Sending File");
        //Uri uri = data.getData();
        //TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        //statusText.setText("Sending: " + uri);

        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + "content://com.android.providers.media.documents/document/image%3A15716");
        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, "content://com.android.providers.media.documents/document/image%3A15716");
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        this.startService(serviceIntent);
    }

    @Override
    public void disconnect() {

        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                String reason;
                switch(reasonCode){
                    case WifiP2pManager.P2P_UNSUPPORTED:
                        reason = "WiFi P2P is not supported on this device.";
                        break;
                    case WifiP2pManager.BUSY:
                        reason = "Framework is busy and unable to service this request.";
                        break;
                    case WifiP2pManager.ERROR:
                        reason = "Internal Error.";
                        break;
                    default:
                        reason = "Undefined failure error code: "+reasonCode;
                }
                Log.d(TAG, "Disconnect failed. Reason :" + reason);

            }

            @Override
            public void onSuccess() {
                /*fragment.getView().setVisibility(View.GONE);*/
                Log.d(WiFiDirectActivity.TAG, "Disconnected");
            }

        });
    }

    public void resetData() {
        if (!isWifiP2pEnabled) {
            sendNotification("Micronet Status Update:", "WiFi Direct is Disabled");

        }else {

            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    sendNotification("Micronet Status Update:", "Scanning for peers");
                    Log.d(WiFiDirectActivity.TAG, "Discovery Initiated");
                    if (manager != null) {
                        manager.requestPeers(channel, peerListListener);
                        Log.d(WiFiDirectActivity.TAG, "P2P peers changed");
                    }

                }

                @Override
                public void onFailure(int reasonCode) {
                    String reason;
                    switch(reasonCode){
                        case WifiP2pManager.P2P_UNSUPPORTED:
                            reason = "WiFi P2P is not supported on this device.";
                            break;
                        case WifiP2pManager.BUSY:
                            reason = "Framework is busy and unable to service this request.";
                            break;
                        case WifiP2pManager.ERROR:
                            reason = "Internal Error.";
                            break;
                        default:
                            reason = "Undefined failure error code: "+reasonCode;
                    }
                    sendNotification("Micronet Status Update:", "Scanning Failed: "+reason);
                    Log.d(WiFiDirectActivity.TAG, "Discovery Failed: "+reason);
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
