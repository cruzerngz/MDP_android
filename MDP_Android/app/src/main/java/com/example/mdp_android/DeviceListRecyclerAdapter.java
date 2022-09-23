package com.example.mdp_android;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

import java.util.ArrayList;

public class DeviceListRecyclerAdapter extends RecyclerView.Adapter<DeviceListRecyclerAdapter.ViewHolder> {

    private static ArrayList<BluetoothDevice> devicesArrList = new ArrayList<>();

    public void addDevice(BluetoothDevice btDevice) {
        if (!devicesArrList.contains(btDevice)) {
            devicesArrList.add(btDevice);
        }
    }

    public boolean contains(BluetoothDevice btDevice){
        return devicesArrList.contains(btDevice);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceTitle;

        ViewHolder(View itemView) {
            super(itemView);
            deviceTitle = itemView.findViewById(R.id.deviceTitle);

            itemView.setOnClickListener(v -> {
//                Log.e("DeviceListRecyclerAdapter", deviceTitle.getText() + "");
//                Log.e("DeviceListRecyclerAdapter",BTManager.instance.currentDeviceName);
                if ( deviceTitle.getText().equals(BTManager.instance.currentDeviceName) ){
                    Log.e("DeviceListRecyclerAdapter",BTManager.instance.currentDeviceName + " already connected");
                    Toast.makeText(BTManager.instance.appCompatActivity, BTManager.instance.currentDeviceName + " already connected", Toast.LENGTH_SHORT).show();
                    return;
                }
                // show a toast that it is connecting
                Toast.makeText(BTManager.instance.appCompatActivity, "connecting...", Toast.LENGTH_SHORT).show();
                BTManager.instance.stopDiscovery();
                int position = getAdapterPosition();
                BTManager.instance.startPairing();
                final String TAG = "DeviceListRecyclerAdapter";
                Log.e(TAG, "onItemClick: You Clicked on a device.");
                if (ActivityCompat.checkSelfPermission(BTManager.instance.appCompatActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    String deviceName = devicesArrList.get(position).getName();
                    String deviceAddress = devicesArrList.get(position).getAddress();
                    Log.e(TAG, "onItemClick: deviceName = " + deviceName);
                    Log.e(TAG, "onItemClick: deviceAddress = " + deviceAddress);
                    // Pair devices
                    if (!BTManager.instance.checkPairedBefore(devicesArrList.get(position))){
                        devicesArrList.get(position).createBond();
                    }
                    else{
                        BTManager.instance.continueConnectToDevice(devicesArrList.get(position));
                    }
                    return;
                }

            });

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.card_layout, viewGroup, false);
        return new ViewHolder(v);
    }

    public void onBindViewHolder(ViewHolder viewHolder, int i) {

        if (ActivityCompat.checkSelfPermission(MainActivity.ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            viewHolder.deviceTitle.setText(devicesArrList.get(i).getName() + "\n");
            viewHolder.deviceTitle.append(devicesArrList.get(i).getAddress());
            return;
        }
    }

    @Override
    public int getItemCount() {
        return devicesArrList.size();
    }
}
