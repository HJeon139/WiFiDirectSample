package com.jeonbase.wifidirectsample;

import android.net.wifi.p2p.WifiP2pInfo;

/** Interface for callback invocation when connection info is available */
public interface ConnectionInfoListener {
    /**
     * The requested connection info is available
     * @param info Wi-Fi p2p connection info
     */
    public void onConnectionInfoAvailable(WifiP2pInfo info);
}
