package com.example.mdp_android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class MyBluetoothService {
    private static final String TAG = "MyBluetoothService";
    private BluetoothSocket mmSocket;
    private Handler handler; // handler that gets info from Bluetooth service
    ConnectedThread connectedThread;
    private Activity uiThreadRef;
    private boolean isThreadRunning = true;

    public MyBluetoothService(BluetoothSocket socket){
        mmSocket = socket;
        connectedThread = new ConnectedThread(mmSocket);
        connectedThread.start();
    }

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }


    public void sendMessage(String message){
        connectedThread.write(message.getBytes(StandardCharsets.UTF_8));
    }

    public void setActivityCtx(Activity a){
         this.uiThreadRef = a;
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private InputStream mmInStream = null;
        private OutputStream mmOutStream = null;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            // establish input and output streams
            establishStream(mmSocket);
        }

        @SuppressLint("MissingPermission")
        public void run() {

            // if setup fails, recursively call establishStream
            while (mmInStream == null || mmOutStream == null){
                Log.e(TAG,"retrying to establish input and output stream...");
                establishStream(mmSocket);
            }

            // successfully connected to device
            BTManager.instance.toggleLockMessageInterface();
            BTManager.instance.passMessageToMessageInterface("System","I/O stream established!");
            Log.e(TAG, "I/O stream established!");

            BTManager.instance.passMessageToMessageInterface("System","Device " + BTManager.instance.currentDevice.getName() + " is connected!");
            Log.e(TAG, "Device " + BTManager.instance.currentDevice.getName() + " is connected!");

            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (isThreadRunning) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    String message = new String(mmBuffer,0,numBytes);
                    BTManager.instance.passMessageToMessageInterface(BTManager.instance.currentDeviceName, message);
                    Log.e(TAG,message);
                    // Send the obtained bytes to the UI activity.
//                    Message readMsg = handler.obtainMessage(
//                            MessageConstants.MESSAGE_READ, numBytes, -1,
//                            mmBuffer);
//                    readMsg.sendToTarget();

                } catch (IOException e) {
                    Log.e(TAG, "Input stream was disconnected", e);
                    BTManager.instance.passMessageToMessageInterface("System","Input stream was disconnected");

                    // if input stream disconnected means device lost connection
                    BTManager.instance.passMessageToMessageInterface("System","Device " + BTManager.instance.currentDevice.getName() + " disconnected!");
                    Log.e(TAG, "Device " + BTManager.instance.currentDevice.getName() + " disconnected!");
                    BTManager.instance.toggleLockMessageInterface();

                    BTManager.instance.reconnect();
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                uiThreadRef.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            mmOutStream.write(bytes);
                        } catch (IOException e) {
                            Log.e(TAG, "Error occurred when sending data", e);
                        }
                    }
                });

                // Share the sent message with the UI activity.
//                Message writtenMsg = handler.obtainMessage(
//                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
//                writtenMsg.sendToTarget();
            } catch (Exception e) {
                Log.e(TAG, "IDK Whats going on", e);

                // Send a failure message back to the activity.
//                Message writeErrorMsg =
//                        handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
//                Bundle bundle = new Bundle();
//                bundle.putString("toast",
//                        "Couldn't send data to the other device");
//                writeErrorMsg.setData(bundle);
//                handler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }

        // to recursively call establish stream if it fails
        private void establishStream(BluetoothSocket socket){
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

    }

    // outside connected thread class
    private void turnThreadOff(){
        isThreadRunning = false;
    }

    public void destroyService(){
        connectedThread.cancel();
        turnThreadOff();
    }
}
