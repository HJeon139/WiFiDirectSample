package com.jeonbase.wifidirectsample;

/**
 * Duplicated by Hohyun Jeon on 1/19/2015.
 */

// Copyright 2011 Google Inc. All Rights Reserved.

        import android.app.IntentService;
        import android.app.NotificationManager;
        import android.content.ContentResolver;
        import android.content.Context;
        import android.content.Intent;
        import android.net.Uri;
        import android.os.Bundle;
        import android.support.v4.app.NotificationCompat;
        import android.util.Log;

        import java.io.FileNotFoundException;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.OutputStream;
        import java.net.InetSocketAddress;
        import java.net.Socket;
        import java.security.InvalidKeyException;
        import java.security.NoSuchAlgorithmException;

        import javax.crypto.Cipher;
        import javax.crypto.CipherOutputStream;
        import javax.crypto.NoSuchPaddingException;
        import javax.crypto.spec.SecretKeySpec;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String ACTION_PASSIVE = "com.example.android.wifidirect.PASSIVE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket to: "+host);
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(WiFiDirectActivity.TAG, "Client socket: " + Boolean.toString(socket.isConnected()));
                OutputStream stream = socket.getOutputStream();

                // CIPHER THE OUTPUT STREAM
                // Length is 16 byte
                // Careful when taking user input!!! http://stackoverflow.com/a/3452620/1188357
                SecretKeySpec sks = new SecretKeySpec("MyDifficultPassw".getBytes(), "AES");
                // Create cipher
                Cipher cipher = null;
                try {
                    cipher = Cipher.getInstance("AES");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                }
                try {
                    cipher.init(Cipher.ENCRYPT_MODE, sks);
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }
                // Wrap the output stream
                CipherOutputStream cos = new CipherOutputStream(stream, cipher);


                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d(WiFiDirectActivity.TAG, e.toString());
                }
                //DeviceDetailFragment.copyFile(is, stream);
                // USE CipherOutputStream
                DeviceDetailFragment.copyFile(is, cos);
                Log.d(WiFiDirectActivity.TAG, "Client: Data written");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }
}