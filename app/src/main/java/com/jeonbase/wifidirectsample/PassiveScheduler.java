package com.jeonbase.wifidirectsample;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Hohyun on 4/17/2015.
 */
public class PassiveScheduler extends IntentService implements WifiP2pManager.ChannelListener, DeviceListFragment.DeviceActionListener, WifiP2pManager.ConnectionInfoListener {

    public PassiveScheduler(){
        super("PassiveScheduler");
    }

    public static final String TAG = "Micronet Demo";
    public static final int NOTIFICATION_ID = 1;
    public static final String NOTE_HEAD = "Micronet Passive Mode";
    public static final String DEFAULT_DIR = "content://com.android.providers.media.documents/document/image%3A15716";
    public static final int SERVER_PORT = 8988;

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

    /*private WifiP2pManager.ConnectionInfoListener connectionListener = new WifiP2pManager.ConnectionInfoListener(){
        @Override
        public void onConnectionInfoAvailable(final WifiP2pInfo info) {
            PassiveScheduler.this.info = info;


            // InetAddress from WifiP2pInfo struct.
            try{
                Log.d(TAG, "Group Owner IP - " + PassiveScheduler.this.info.groupOwnerAddress.getHostAddress());

                // After the group negotiation, we assign the group owner as the file
                // server. The file server is single threaded, single connection server
                // socket.
                if (PassiveScheduler.this.info.groupFormed && PassiveScheduler.this.info.isGroupOwner) {
                    //Listen
                    new FileServerAsyncTask(PassiveScheduler.this).execute();
                    Log.d(WiFiDirectActivity.TAG, "Listening for File");
                } else if (PassiveScheduler.this.info.groupFormed) {
                    // The device acts as the client. Send File
                    //send data
                    sendfile();

                    //disconnect
                    disconnect();
                }
            }catch(NullPointerException e){
                Log.e(TAG, e.getMessage());
            }

        }
    };*/

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

        //register();

        sendNotification(NOTE_HEAD, "Passive mode active");
        //passive stuff
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        resetData();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Log.e(WiFiDirectActivity.TAG, e.getMessage());
        }

        Log.d(WiFiDirectActivity.TAG, "peer count: " + Integer.toString(p_peers.size()));
        if (p_peers.size()>0){
            cancelNotification();
            for(int i=0; i<p_peers.size(); i++){
                //connect(i);
                Log.d(WiFiDirectActivity.TAG, "Loop("+Integer.toString(i)+"): \t"+Long.toString(SystemClock.elapsedRealtime()));

                WifiP2pDevice device = p_peers.get(i);

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.groupOwnerIntent = 0;
                config.wps.setup = WpsInfo.PBC;

                sendNotification(NOTE_HEAD, "Attempting to connect to: "+ device.deviceName);
                Log.d(TAG, "Attempting to connect to: "+ device.deviceName);
                manager.connect(channel, config, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(WiFiDirectActivity.TAG, "Connected" );

                        //stopDiscovery();
                        //Intent bRIntent = new Intent(PassiveScheduler.this, WiFiDirectBroadcastReceiver.class);
                        //bRIntent.setAction("PASSIVE_MODE_AUTO_SEND");
                        //sendBroadcast(bRIntent);

                        //sendNotification(NOTE_HEAD, "Connected to peer: "+ p_peers.get(i).deviceName);


                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            Log.e(WiFiDirectActivity.TAG, e.getMessage());
                        }
                        //manager.createGroup(channel, new WifiP2pManager.ActionListener(){

                            //@Override
                            //public void onSuccess() {
                                Log.d(WiFiDirectActivity.TAG, "****:\t"+ Long.toString(SystemClock.elapsedRealtime()));
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    Log.e(WiFiDirectActivity.TAG, e.getMessage());
                                }
                                manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener(){
                                    @Override
                                    public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                        InetAddress ownerAddress=info.groupOwnerAddress;

                                        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                                        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

                                        if (networkInfo.isConnected()) {

                                            if(info.groupFormed){
                                                if(info.isGroupOwner){
                                                    //listen
                                                    FileServerAsyncTask fileServerAsyncTask = new FileServerAsyncTask(getApplicationContext());
                                                    fileServerAsyncTask.execute();
                                                    try{
                                                        try {
                                                            try{
                                                                fileServerAsyncTask.get(15000, TimeUnit.MILLISECONDS);
                                                            }catch(TimeoutException te){
                                                                Log.e(TAG, " "+te.getMessage());
                                                            }
                                                        }catch(ExecutionException ee){
                                                            Log.e(TAG, " "+ee.getMessage());
                                                        }
                                                    }catch (InterruptedException ie ){
                                                        Log.e(TAG, " "+ie.getMessage());
                                                    }
                                                }else{
                                                    //send file
                                                    Log.d(TAG,"info: \t" + info.toString());

                                                    // FileTransferService.
                                                    Log.d(WiFiDirectActivity.TAG, "Sending File: "+ Long.toString(SystemClock.elapsedRealtime()));
                                                    //Uri uri = data.getData();
                                                    //TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
                                                    //statusText.setText("Sending: " + uri);


                                                    Log.d(WiFiDirectActivity.TAG, "File Directory: " + DEFAULT_DIR);
                                                    Intent serviceIntent = new Intent(PassiveScheduler.this, FileTransferService.class);
                                                    serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
                                                    serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, DEFAULT_DIR);
                                                    serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
                                                    try {
                                                        Log.d(TAG,"ownerAddress: \t" + ownerAddress.toString().substring(1));
                                                        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                                                ownerAddress.toString().substring(1));
                                                        startService(serviceIntent);
                                                    }catch(NullPointerException e){
                                                        Log.e(WiFiDirectActivity.TAG, e.getMessage());
                                                        sendNotification(NOTE_HEAD, "Error: Null Peer Address");
                                                        disconnect();
                                                    }
                                                    sendNotification(NOTE_HEAD, "File Transfer Started");


                                                    try {
                                                        Thread.sleep(5000);
                                                    } catch (InterruptedException e) {
                                                        Log.e(WiFiDirectActivity.TAG, e.getMessage());
                                                    }

                                                    disconnect();
                                                }
                                            }else{
                                                Log.d(WiFiDirectActivity.TAG, "No longer connected:\t"+ Long.toString(SystemClock.elapsedRealtime()));
                                                disconnect();
                                            }


                                        } else {
                                            Log.d(WiFiDirectActivity.TAG, "Info Pass Failed:\t"+ Long.toString(SystemClock.elapsedRealtime()));
                                            disconnect();
                                        }
                                    }
                                });
                            //}

                            /*@Override
                            public void onFailure(int reason) {
                                Log.d(WiFiDirectActivity.TAG, "Grouping Failed");
                                disconnect();
                            }
                        });*/
                        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
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
                                Log.d(TAG, "Connect Stop Failed:" + reason);

                            }

                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Connection Stopped");

                            }
                        });

                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        String reason;
                        switch(reasonCode) {
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
                                reason = "Undefined failure error code: " + reasonCode;
                        }
                        sendNotification("Micronet Status Update:", "Connection Failed: "+reason);
                        Log.d(WiFiDirectActivity.TAG, "Connection Failed: "+reason);
                    }
                });

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Log.e(WiFiDirectActivity.TAG, e.getMessage());
                }


            }
        }else{
            //
            //disconnect();
            sendNotification(NOTE_HEAD, "No Peers Found");
        }
        //stopDiscovery();

        Log.d(WiFiDirectActivity.TAG, "Passive: End...");

        Intent broadcastIntent = new Intent(this, WakeReceiver.class);
        sendBroadcast(broadcastIntent);
        sendNotification(NOTE_HEAD, "Sleeping...");
        WakefulReceiver.completeWakefulIntent(intent);

    }

    private void register(){
        Map record = new HashMap();

        record.put("listenport", Integer.toString(SERVER_PORT));
        record.put("peer_name", "main_server");
        record.put("available", "visible");


        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
                Log.d(TAG, "Add service success: "+ Long.toString(SystemClock.elapsedRealtime()));
            }

            @Override
            public void onFailure(int reasonCode) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
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
                Log.d(TAG, "Add service failed:" + reason);
            }
        });
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

    private void cancelNotification(){
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
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
                //manager.requestConnectionInfo(channel, connectionListener);

                //transfer();
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
                Intent bRIntent = new Intent(PassiveScheduler.this, WiFiDirectBroadcastReceiver.class);
                bRIntent.setAction("PASSIVE_MODE_AUTO_SEND");
                sendBroadcast(bRIntent);

            }

            @Override
            public void onFailure(int reason) {
                sendNotification(NOTE_HEAD, "Connection Failed... Retry?");
                Log.d(WiFiDirectActivity.TAG, "Connection Failed");
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        this.info = info;
        Log.d(WiFiDirectActivity.TAG, "onConnection info transfer");

        // InetAddress from WifiP2pInfo struct.


        try{
            Log.d(TAG, "Group Owner IP - " + this.info.groupOwnerAddress.getHostAddress());


            // After the group negotiation, we assign the group owner as the file
            // server. The file server is single threaded, single connection server
            // socket.
            if (this.info.groupFormed && this.info.isGroupOwner) {
                //Listen
                sendNotification(NOTE_HEAD, "Acting as server... Listening...");
                new FileServerAsyncTask(PassiveScheduler.this).execute();
                Log.d(WiFiDirectActivity.TAG, "Listening for File");
            } else if (this.info.groupFormed) {
                // The device acts as the client. Send File
                //send data
                sendNotification(NOTE_HEAD, "Sending to: " + this.info.groupOwnerAddress.getHostAddress());
                sendfile();

                //disconnect
                disconnect();
            }
        }catch(NullPointerException e){
            Log.e(TAG, e.getMessage());
        }

    }

    public void transfer(){
        if(info != null){
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
        }else{
            Log.e(WiFiDirectActivity.TAG, "info file is null");
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
                this.info.groupOwnerAddress.getHostAddress());
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
            sendNotification(NOTE_HEAD, "WiFi Direct is Disabled");

        }else {

            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    sendNotification(NOTE_HEAD, "Scanning for peers");
                    Log.d(WiFiDirectActivity.TAG, "Discovery Initiated");
                    if (manager != null) {
                        manager.requestPeers(channel, peerListListener);
                        Log.d(WiFiDirectActivity.TAG, "P2P peers updated");
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
                            disconnect();
                            break;
                        case WifiP2pManager.ERROR:
                            reason = "Internal Error.";
                            break;
                        default:
                            reason = "Undefined failure error code: "+reasonCode;
                    }
                    sendNotification(NOTE_HEAD, "Scanning Failed: "+reason);
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
            sendNotification(NOTE_HEAD, "Channel lost. Trying again");
            //Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();

            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            /*Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();*/
            sendNotification(NOTE_HEAD, "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.");
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
