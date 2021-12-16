package com.example.bluemessage;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    public EditText editText;
    public String userName;

    /**
     * initialize bluetooth adapter and textview
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        editText = findViewById(R.id.editTextName);

    }

    /**
     * Checks if the device has bluetooth on device
     * if so then it will request to turn on bluetooth if it is not currently on.
     * Lastly once the bluetooth is on then when the user clicks on the connect button
     * it will grab the users name update the name of the bluetooth device and send the user the discover device activity.
     * @param v
     */
    public void connect(View v){


        //Checks if Bluetooth is available on device
        if(bluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "Bluetooth is not supported on this phone", Toast.LENGTH_SHORT).show();
        }

        else{
            //create intent to turn on BlueTooth
            if(!bluetoothAdapter.isEnabled()){
                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(i, 1);

            }
            else{
                //Moves to discover device page
                Intent intent = new Intent(MainActivity.this, DiscoverDevice.class);
                //puts username in a string
                userName = editText.getText().toString();
                intent.putExtra("userName", userName);
                startActivity(intent);
            }

            if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
                Intent discover = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discover.putExtra(bluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discover);
            }

        }
    }

    /**
     * lets the user know that bluetooth is enabled via toast
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == 1){
            if(resultCode == RESULT_OK){
                Toast.makeText(getApplicationContext(), "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }
}