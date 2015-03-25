package com.jeonbase.wifidirectsample;

/**
 * Created by Hohyun Jeon on 2/9/2015.
 */
/** Interface for callback invocation when peer list is available */
public interface PeerListListener {
    /**
     * The requested peer list is available
     * @param peers List of available peers
     */
    public void onPeersAvailable(WifiP2pDeviceList peers);
}
