package com.example.bluemessage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DiscoverDevice extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;

    private ChatUtils chatUtils;
    private TextView textView;
    private String userName;
    private Context context;
    private final int LOCATION_REQUEST = 101;
    private final int SELECT_DEVICE = 102;

    public static final int MESSAGE_STATE_CHANGE = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_Write = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";
    private String connectedDevice;

    private Handler handler = new Handler(new Handler.Callback(){

        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what){
                case MESSAGE_STATE_CHANGE:
                    switch (message.arg1){
                        case ChatUtils.STATE_NONE:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_LISTEN:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState("Connected " + connectedDevice);
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    /**
     * Reflect ChatUtils states in Message_State_Change Handler given subTitle
     * @param subTitle
     */
    private void setState(CharSequence subTitle){
        getSupportActionBar().setSubtitle(subTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_device);
        context = this;
        chatUtils = new ChatUtils(context, handler);
        textView = findViewById(R.id.name);

        userName = getIntent().getExtras().getString("userName");
        textView.setText(userName);

        initBluetooth();
    }

    /**
     * initialise Bluetooth
     */
    private void initBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(context, "No Bluetooth on Device", Toast.LENGTH_SHORT).show();
        }
    }

    public void onButtonClick(View view){
        checkPermissions();
    }

    public void checkPermissions(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(DiscoverDevice.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        }else{
            Intent intent = new Intent(context, DeviceListActivity.class);
            startActivityForResult(intent, SELECT_DEVICE);
        }
    }
    //Post the connection log of the device
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == SELECT_DEVICE && resultCode == RESULT_OK){
            String address = data.getStringExtra("deviceAddress");
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(requestCode == LOCATION_REQUEST){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(context, DeviceListActivity.class);
                startActivityForResult(intent, SELECT_DEVICE);
            }else{
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Location permission required.\n try again")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener(){

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                checkPermissions();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener(){

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                DiscoverDevice.this.finish();
                            }
                        })
                        .show();
            }
        }else {
            super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(chatUtils != null){
            chatUtils.stop();
        }
    }
}