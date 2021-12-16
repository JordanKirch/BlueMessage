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
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class DiscoverDevice extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;

    private ChatUtils chatUtils;
    private EditText editText;
    private Button clearButton;
    private Button sendButton;
    private ListView listMainChat;
    private ArrayAdapter<String> adapterChat;
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
    /**
     * Handler is used to track what state the device is in during the connection process
     * The STATE_CHANGE case updates the message that is displayed on the main chat page
     * Write uploads the users message and displays on screen
     * Read gets the message from the users device and displays it
     * Device_name gets connected devices name
     * Toast tells the user which if the user is connected or if an error occurs
     */
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
                            setState("Listening");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState("Connected " + connectedDevice);
                            break;
                    }
                    break;
                case MESSAGE_Write:
                    byte[] bufferW = (byte[])message.obj;
                    String outputBuffer = new String(bufferW);
                    adapterChat.add("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t"+ userName+": " + outputBuffer);
                    break;
                case MESSAGE_READ:
                    byte[] bufferR = (byte[]) message.obj;
                    String inputBuffer = new String(bufferR, 0, message.arg1);
                    adapterChat.add(connectedDevice + ": " + inputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, connectedDevice, Toast.LENGTH_LONG).show();
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
        //get username from first activity
        userName = getIntent().getExtras().getString("userName");

        initMessage();

        initBluetooth();
    }

    /**
     * initializes the chat list, popupmenu and the buttons on the main page
     */
    private void initMessage(){
        //main list
        listMainChat = findViewById(R.id.list_conversation);
        listMainChat.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final int selected_item = i;
                adapterChat.remove(adapterChat.getItem(selected_item));
                adapterChat.notifyDataSetChanged();
            }
        });

        //popup menu
        registerForContextMenu(listMainChat);
        //text view
        editText = findViewById(R.id.message_body);
        //set up array adapter
        adapterChat = new ArrayAdapter<String>(context, R.layout.message_layout);
        listMainChat.setAdapter(adapterChat);
        //clear button
        clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener(){
            /**
             * Clear edit text
             * @param v
             */
            @Override
            public void onClick(View v){
                editText.setText("");
            }
        });
        //send button
        sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String message = editText.getText().toString();
                if(!message.isEmpty()){
                    chatUtils.write(message.getBytes());
                    editText.setText("");
                }
            }
        });
    }

    /**
     * displays React and Delete menu when the user clicks on a message
     * @param menu
     * @param v
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.menu_delete_and_react, menu);
    }

    /**
     * displays react options and once the user select on a message will be sent with that reaction.
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.lol:
                chatUtils.write("LOL".getBytes());
                return true;
            case R.id.thumbsup:
                chatUtils.write("Thumbs Up".getBytes());
                return true;
            case R.id.thumbsdown:
                chatUtils.write("Thumbs Down".getBytes());
                return true;
                default:
                    return super.onContextItemSelected(item);
        }

    }

    /**
     * Returns the created menu for the messaging page and allows the suers to see device connecting window
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_message_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Method used when the user clicks on the menu button to allow the user
     * to go to the Device list activity and select a device to connect to.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.menu_search_device:
                checkPermissions();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /**
     * initialise Bluetooth
     */
    private void initBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.setName(userName);
        if(bluetoothAdapter == null){
            Toast.makeText(context, "No Bluetooth on Device", Toast.LENGTH_SHORT).show();
        }
    }

    public void onButtonClick(View view){
        checkPermissions();
    }

    /**
     * allows the device to be discoverable for a given amount of time
     */
    public void checkPermissions(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(DiscoverDevice.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        }else{
            Intent intent = new Intent(context, DeviceListActivity.class);
            startActivityForResult(intent, SELECT_DEVICE);
        }
    }

    /**
     * Toast the connection log of the device
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == SELECT_DEVICE && resultCode == RESULT_OK){
            String address = data.getStringExtra("deviceAddress");
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * function runs when the end user clicks the plus button in the main chat window.
     * It request the users location to find blue tooth devices and opens the DevicelistActivity
     * where the user can scan and connect to devices.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
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

    /**
     * Disconnects device on Destroy
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(chatUtils != null){
            chatUtils.stop();
        }
    }
}