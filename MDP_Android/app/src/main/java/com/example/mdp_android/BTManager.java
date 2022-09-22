package com.example.mdp_android;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.function.Consumer;

// What this class does :
// toggleBluetooth: toggle bluetooth on when its off
// toggleDiscoverable: Make device discoverable
// discoverDevices: discover available devices to pair
// startPairing: start pairing the devices
// continueConnectToDevice: close the current page and go back to main page
// checkPairedBefore
// Broadcast Receivers: for debugging purposes

public class BTManager {

    public static final String TAG = "BTManager";
    public static BTManager instance;
    AppCompatActivity appCompatActivity;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    public MyBluetoothService myBluetoothService;
    private AppCompatActivity deviceActivityCtx;
    public IAppendMessages messageInterface;
    public String currentDeviceName = "";
    public BluetoothDevice currentDevice = null;

    public BTManager(AppCompatActivity appCompatActivity, IAppendMessages messageInterface) {
        instance = this;
        this.appCompatActivity = appCompatActivity;
        bluetoothManager = appCompatActivity.getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        this.messageInterface = messageInterface;
    }

    public void setDeviceActivityContext(AppCompatActivity ctx) {
        deviceActivityCtx = ctx;
    }

    // enable Bluetooth if its off
    public void toggleBluetooth() {
        if (ActivityCompat.checkSelfPermission(appCompatActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            //Case 1: Device has no BT capability
            if (bluetoothAdapter == null) {
                Log.e(TAG, "device does not support bluetooth");
                Toast.makeText(appCompatActivity, "device does not support bluetooth", Toast.LENGTH_SHORT).show();
            }

            // Case 1: Bluetooth is off then on it using intent
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                appCompatActivity.startActivity(enableBT);
                IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                appCompatActivity.registerReceiver(bluetoothAdapterBR, BTIntent);
                Log.e(TAG, "enabling BT... waiting for permission");
            } else if (bluetoothAdapter.isEnabled()) {
                toggleDiscoverable();
            }
        }
    }

    // Check status of bluetooth adapter - toggling on/off bluetooth
    private final BroadcastReceiver bluetoothAdapterBR = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.e(TAG, "onReceive: STATE OFF");
                        Toast.makeText(appCompatActivity, "Bluetooth is off", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.e(TAG, "bluetoothAdapterBR: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.e(TAG, "bluetoothAdapterBR: STATE ON");
                        Toast.makeText(appCompatActivity, "Bluetooth is on", Toast.LENGTH_LONG).show();
                        toggleDiscoverable();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.e(TAG, "bluetoothAdapterBR: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    //------------------------------------------------------------------------------------------------------------------------
    // enable discoverable
    public void toggleDiscoverable() {
        if (ActivityCompat.checkSelfPermission(appCompatActivity, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Making device discoverable for 5 minutes (300 seconds) .");

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            appCompatActivity.startActivity(discoverableIntent);

            IntentFilter intentFilter = new IntentFilter(bluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            appCompatActivity.registerReceiver(discoverableBR, intentFilter);
        }
    }

    // Check status of bluetooth adapter - enabling discoverable
    private final BroadcastReceiver discoverableBR = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.e(TAG, "discoverableBR: Discoverability Enabled.");
                        Toast.makeText(appCompatActivity, "discoverable on", Toast.LENGTH_SHORT).show();
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.e(TAG, "discoverableBR: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.e(TAG, "discoverableBR: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.e(TAG, "discoverableBR: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.e(TAG, "discoverableBR: Connected.");
                        break;
                }

            }
        }
    };

    //------------------------------------------------------------------------------------------------------------------------

    // discover devices
    //Store a function that takes in bluetooth devices
    Consumer<BluetoothDevice> discoverDeviceCallback = null;

    public void discoverDevices(Consumer<BluetoothDevice> c) {
        Log.e(TAG, "Looking for unpaired devices");
        discoverDeviceCallback = c;
        if (ActivityCompat.checkSelfPermission(appCompatActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                Log.e(TAG, "Canceling discovery");

                bluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                appCompatActivity.registerReceiver(discoverDevicesBR, discoverDevicesIntent);
            } else if (!bluetoothAdapter.isDiscovering()) {

                bluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                appCompatActivity.registerReceiver(discoverDevicesBR, discoverDevicesIntent);
            }
        }
    }


    // List devices that are not yet paired
    private BroadcastReceiver discoverDevicesBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
//            Log.e(TAG, "onReceive: ACTION FOUND.");

            if (ActivityCompat.checkSelfPermission(appCompatActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    DeviceListRecyclerAdapter.BTDevices.add(device);
                    if (discoverDeviceCallback != null) {
                        discoverDeviceCallback.accept(device);
                    }

                    if (device.getName() != null){
                        Log.e(TAG, "onReceive: " + device.getName());
                        Log.e(TAG, "onReceive:" + device.getAddress());
                    }

                }
            }
        }
    };


    //------------------------------------------------------------------------------------------------------------------------

    // toggle pairing
    public void startPairing() {
        //Broadcasts when bond state changes (ie:pairing)
        Log.e(TAG, "started pairing...");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        appCompatActivity.registerReceiver(pairingBR, filter);
    }

    // Detect Pairing status changes
    private final BroadcastReceiver pairingBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "calling pairingBR...");
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(appCompatActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                }
                // case1: Already bonded
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.e(TAG, "pairingBR: BOND_BONDED.");
                    continueConnectToDevice(device);
                }
                // case2: Creating a bond
                if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.e(TAG, "pairingBR: BOND_BONDING.");
                    device.createBond();
                }
                // case3: Breaking a bond
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.e(TAG, "pairingBR: BOND_NONE.");
                }
            }

        }
    };

    public void setCurrentDevice(BluetoothDevice device){
        currentDevice = device;
    }

    public void continueConnectToDevice(BluetoothDevice device) {
        Log.e(TAG, "pairingBR: BOND_BONDED.");
        // Create a thread
        ConnectThread connectThread = new ConnectThread(device,
                (socket) -> { // socket here is a type of Bluetooth socket
            // lines 261 - 269 only runs when consumer accepts the consumer
            myBluetoothService = new MyBluetoothService(socket);
            // to use append function in BTManager
            myBluetoothService.setActivityCtx(appCompatActivity);
            // Close deviceListActivity
            if (deviceActivityCtx != null) {
                deviceActivityCtx.finish();
            }
        }
        );
        connectThread.start();
    }

    //------------------------------------------------------------------------------------------------------------------------

    // check if paired before
    public boolean checkPairedBefore(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(appCompatActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

        }
        return bluetoothAdapter.getBondedDevices().contains(device);
    }


    //------------------------------------------------------------------------------------------------------------------------

    // method to cancelDiscovery once the pairing process begins
    public void stopDiscovery() {
        if (ActivityCompat.checkSelfPermission(appCompatActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

        }
        bluetoothAdapter.cancelDiscovery();
    }

    public void onDestroy() {
        appCompatActivity.unregisterReceiver(bluetoothAdapterBR);
        appCompatActivity.unregisterReceiver(discoverableBR);
        appCompatActivity.unregisterReceiver(discoverDevicesBR);
        appCompatActivity.unregisterReceiver(pairingBR);
        if (ActivityCompat.checkSelfPermission(appCompatActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG,"test");
            return;
        }
        bluetoothAdapter.cancelDiscovery();
        Log.e(TAG, "destroying...");
    }

    //public void appendMessages(String s){
    //    messageInterface.appendMessages(s);
    //}

    public void passMessageToMessageInterface(String sender, String content){
        appCompatActivity.runOnUiThread(new Runnable() {
            public void run() {
                messageInterface.handleMessage(sender,content);
                previousMsg = content;
                previousSender = sender;
            }
        });
    }

    private String previousMsg = "";
    private String previousSender = "";
    public void passMessageToMessageInterface(String sender, String content, boolean counter){

        appCompatActivity.runOnUiThread(new Runnable() {
            public void run() {
                // as long as either not same allow sending else don't send
                if (!content.equals(previousMsg) || !previousSender.equals(sender)){
                    messageInterface.handleMessage(sender,content);
                    previousMsg = content;
                    previousSender = sender;
                }
            }
        });
    }

    public void toggleLockMessageInterface(){
        appCompatActivity.runOnUiThread(new Runnable() {
            public void run() {
                messageInterface.toggleLock();
            }
        });
    }

    // Persistent connection: if somehow disconnect, reconnect the socket
    // delete connected thread in background
    // create a new service
    public void reconnect(){
        myBluetoothService.destroyService();
        myBluetoothService = null;
        continueConnectToDevice(currentDevice);
    }
}
