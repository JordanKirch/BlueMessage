package com.example.bluemessage;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private ListView listPairedDevices, listAvailableDevices;
    private ProgressBar progressScan;
    private ArrayAdapter<String> adapterPairedDevices, adapterAvailableDevices;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        context = this;
        //setup paired and available device list
        listPairedDevices = findViewById(R.id.list_paired_devices);
        listAvailableDevices = findViewById(R.id.list_available_devices);
        progressScan = findViewById(R.id.progress_scan);

        adapterPairedDevices = new ArrayAdapter<String>(context, R.layout.device_list_item);
        adapterAvailableDevices = new ArrayAdapter<String>(context, R.layout.device_list_item);

        listPairedDevices.setAdapter(adapterPairedDevices);
        listAvailableDevices.setAdapter(adapterAvailableDevices);

        //set onClickListeners for connectable devices
        listAvailableDevices.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            //Send device info to the message page
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                Intent intent = new Intent();
                intent.putExtra("deviceAddress", address);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        //scan for paired and add to paired list
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if(pairedDevices != null && pairedDevices.size()>0){
            for(BluetoothDevice b : pairedDevices){
                adapterPairedDevices.add(b.getName() + "\n" + b.getAddress());
            }
        }

        //set up intent filters
        IntentFilter filterFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothDeviceListener, filterFound);
        IntentFilter filterEndScan = new IntentFilter(BluetoothAdapter. ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothDeviceListener, filterEndScan);
    }

    private BroadcastReceiver bluetoothDeviceListener = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    //if not a paired device add to availableDevices
                    adapterAvailableDevices.add(device.getName() + "\n" + device.getAddress());
                }
                //else time expires on the scan
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    progressScan.setVisibility(View.GONE);
                    if(adapterAvailableDevices.getCount() == 0){
                        Toast.makeText(context, "No device found", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(context, "click device to chat", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()) {
            case R.id.menu_scan:
                scanDevices();
                return true;
                default:
                    return super.onOptionsItemSelected(item);

        }

    }

    private void scanDevices(){
        progressScan.setVisibility(View.VISIBLE);
        adapterAvailableDevices.clear();
        Toast.makeText(context,"Scan started", Toast.LENGTH_SHORT).show();
        //if currently discovering cancel process
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        //start discovery
        bluetoothAdapter.startDiscovery();
    }
}