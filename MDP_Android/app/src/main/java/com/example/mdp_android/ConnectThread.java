package com.example.mdp_android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

public class ConnectThread extends Thread {
    private BluetoothSocket mmSocket = null;
    private final BluetoothDevice mmDevice;

    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final String TAG = "ConnectThread";

    Consumer<BluetoothSocket> onConnectSuccessConsumer = null;
    public ConnectThread(BluetoothDevice device, Consumer<BluetoothSocket> c) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.

        mmDevice = device;

        // Store consumer
        onConnectSuccessConsumer = c ;
        createSocket(mmDevice);
    }

    // to recursive try to create socket
    private void createSocket(BluetoothDevice device){
        BluetoothSocket tmp = null;
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            if (ActivityCompat.checkSelfPermission(BTManager.instance.appCompatActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            }
            tmp = device.createRfcommSocketToServiceRecord(uuid);

        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    @SuppressLint("MissingPermission")
    private void tryConnectToSocket(){
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            Log.e(TAG,"trying to connect to socket...");
            BTManager.instance.passMessageToMessageInterface("System","trying to connect to socket...",true);
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
//            try {
//                Log.e(TAG,"cannot connect, closing socket..");
//                mmSocket.close();
//            } catch (IOException closeException) {
//                Log.e(TAG, "Could not close the client socket", closeException);
//            }

            Log.e(TAG, "failed to connect to socket!");
            BTManager.instance.passMessageToMessageInterface("System","failed to connect to socket!",true);
            tryConnectToSocket();
            return;
        }
    }

    public void run() {
        // recursive create socket
        while (mmSocket == null){
            createSocket(mmDevice);
        }

        Log.e(TAG, "socket established!");
        BTManager.instance.passMessageToMessageInterface("System","socket established!");

        BTManager.instance.stopDiscovery();
        // recursively try to connect to socket
        tryConnectToSocket();

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        BTManager.instance.passMessageToMessageInterface("System","connected to socket!");
        Log.e(TAG, "connected to socket!");
        manageMyConnectedSocket(mmSocket);
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    // successful connection
    @SuppressLint("MissingPermission")
    // confirm get socket
    public void manageMyConnectedSocket(BluetoothSocket btSocket){
        BTManager.instance.currentDeviceName = mmDevice.getName();
        BTManager.instance.setCurrentDevice(mmDevice);
        if (onConnectSuccessConsumer != null){
            onConnectSuccessConsumer.accept(btSocket);

            //BTManager.instance.appendMessages("Device " + mmDevice.getName() + " is connected!");
            //Log.e(TAG, "Device " + mmDevice.getName() + " is connected!");
        }
    }
}



