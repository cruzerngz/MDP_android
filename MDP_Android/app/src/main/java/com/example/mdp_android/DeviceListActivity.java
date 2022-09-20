package com.example.mdp_android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    //    private RecyclerView.Adapter adapter;
    private DeviceListRecyclerAdapter adapter;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        setTitle("Devices Found");
        BTManager.instance.setDeviceActivityContext(this);



        BTManager.instance.discoverDevices((device) -> {
//            DeviceListRecyclerAdapter.BTDevices.add(device);
                    if (device != null) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            if (device.getName() != "" && device.getName() != null && !adapter.contains(device)) {  // sus
                                adapter.addDevice(device);
                                adapter.notifyDataSetChanged();
                            }
                            return;
                        }
                    }
                }
        );

        recyclerView = findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DeviceListRecyclerAdapter();
        recyclerView.setAdapter(adapter);


        // add to the top of list
        Set<BluetoothDevice> previouslyBondedSet = BTManager.instance.bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice btDevice : previouslyBondedSet){
            adapter.addDevice(btDevice);
            adapter.notifyDataSetChanged();
        }
    }
}