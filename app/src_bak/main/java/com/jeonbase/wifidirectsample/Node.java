package com.jeonbase.wifidirectsample;

import android.net.wifi.p2p.WifiP2pDevice;
/**
 * Node is a struc that contains information on it's address, UID, etc.
 * Created by Hohyun Jeon on 2/7/2015.
 *
 *
 * Depreciated on 2/9/2015: We will be modifying WifiP2pDevice Directly to implement namespace addressing.
 */
public class Node extends WifiP2pDevice{
    private byte address;
    private String UID;

    public Node(byte address, String uid){
        this.address = address;
        this.UID = uid;
    }


    //Getters and setters
    public byte getAddress() {
        return address;
    }

    public String getUID() {
        return UID;
    }

    public void setAddress(byte address) {
        this.address = address;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    //ToDo: address resolution methods


}
