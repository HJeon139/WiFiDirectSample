package com.jeonbase.wifidirectsample;

import java.io.File;

/**
 * Class socket contains methods the application layer can use to communicate with other devices
 * in the network.
 *
 * Created by Hohyun Jeon on 2/7/2015.
 *
 *
 * Depreciated on 2/9/2015, All socket methods will be implemented in DeviceListFragment.java
 */
public class Socket {
    private String status = null;

    public Socket(){
        //default constructor
    }

    //Method to ping and return an array of neighbors
    public Node[] findNeighbors(){
        //default return value
        Node[] retval = new Node[0];

        //Return Statement
        return retval;
    }

    //Connects with specific Node and returns array of share-able files
    public String[] listNeighborFiles(){
        //default return value
        String[] retval = null;

        return retval;
    }

    //Connects with a specific Node and returns file location after downloading to location upon success
    public File downloadFromNode(Node node){
        //default return value
        File retval = null;


        //return statement
        return retval;
    }

    //Connects with specified Node and tries to download specified file upon success.
    public File downloadFromNode(Node node, String file){
        //default return value
        File retval = null;


        //return statement
        return retval;
    }


}
