package com.example.bluemessage;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * constructor that handles the state changes of the messages
 */
public class ChatUtils {
    private Context context;
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private final UUID APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private final String APP_NAME = "BlueMessage";
    private ConnectThread connectionThread;
    private AcceptThread acceptThread;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;

    public ChatUtils(Context context, Handler handler){
        this.context = context;
        this.handler = handler;

        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * returns state of connection between selected device.
     */
    public int getState(){
        return state;
    }

    /**
     * sets the state value and reflects the state back to the Message activity
     * @param state
     */
    public synchronized void setState(int state){
        this.state = state;
        handler.obtainMessage(DiscoverDevice.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    private synchronized void start(){
        if(connectionThread != null){
            connectionThread.cancel();
            connectionThread = null;
        }
        if(acceptThread == null){
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        setState(STATE_LISTEN);
    }

    public synchronized void stop(){
        if(connectionThread != null){
            connectionThread.cancel();
            connectionThread = null;
        }
        if(acceptThread != null){
            acceptThread.cancel();
            acceptThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * checks if the given device is currently connecting if so it will cancel the thread and start a new one
     * The method will also set the State of the connection to CONNECTING
     * @param device
     */
    public void connect(BluetoothDevice device){
        if(state == STATE_CONNECTING){
            connectionThread.cancel();
            connectionThread = null;
        }

        connectionThread = new ConnectThread(device);
        connectionThread.start();

        setState(STATE_CONNECTING);
    }

    /**
     * Creates accepting Thread that creates a Bluetooth Server Socket to allow the device to connect to the client
     */
    private class AcceptThread extends Thread{

        private BluetoothServerSocket serverSocket;

        public AcceptThread(){
            BluetoothServerSocket temp = null;
            try{
                temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                Log.e("Accept to Constructor", e.toString());
            }

            serverSocket = temp;
        }

        /**
         * tries to create a connection with the server socket. if it fails or cancels then the socket is close.
         * On success the method is ***** run
         */
        public void run() {
            BluetoothSocket socket = null;
            try{
                socket = serverSocket.accept();
            }catch (IOException e){
                Log.e("Accept to Run", e.toString());
                try {
                    serverSocket.close();
                } catch (IOException e1){
                    Log.e("Accept to Close", e1.toString());
                }
            }
            if (socket!= null){
                switch (state){
                    case STATE_LISTEN:
                        break;
                    case STATE_CONNECTING:
                        connect(socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                        break;
                    case STATE_CONNECTED:
                        try {
                            socket.close();
                        } catch (IOException e){
                            Log.e("Accept to CloseSocket", e.toString());
                        }
                        break;
                }
            }
        }
        /**
         * Cancel the thread and closes the socket
         */
        public void cancel() {
            try {
                serverSocket.close();
            }catch (IOException e){
                Log.e("Accept to CancelServer", e.toString());
            }
        }
    }

    /**
     * Creates Connecting Thread that creates a Bluetooth client Socket to allow the device to connect to the another device
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;


        public ConnectThread(BluetoothDevice device){
            this.device = device;

            BluetoothSocket temp = null;
            try{
                temp = device.createRfcommSocketToServiceRecord(APP_UUID);
            }catch (IOException e){
                Log.e("Connect to Constructor", e.toString());
            }

            socket = temp;
        }

        /**
         * tries to create connection between devices and closes the socket if it fails or runs the method connected(device)
         */
        public void run(){
            try {
                socket.connect();
            }catch (IOException e){
                Log.e("Connect to Run", e.toString());
                try {
                    socket.close();
                }catch (IOException e1){
                    Log.e("Connect to CloseSocket", e.toString());
                }
                connectionFailed();
            }

            synchronized (ChatUtils.this){
                connectionThread = null;
            }

            connected(device);

        }

        /**
         * Cancel the thread and closes the socket
         */
        public void cancel(){
            try {
                socket.close();
            } catch (IOException e){
                Log.e("Connect to Cancel", e.toString());
            }
        }

        /**
         * notify the handler that the connection failed
         */
        private synchronized void connectionFailed(){
            Message msg = handler.obtainMessage(DiscoverDevice.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(DiscoverDevice.TOAST, "Unable to Connect to device");
            msg.setData(bundle);
            handler.sendMessage(msg);

            //restart ChatUtil to listen again
            ChatUtils.this.start();
        }

        /**
         * Use the thread to connect to a given device first checks if a device is already connected and if so disconnects from it
         * then it send a message back to handler that the device is connected and change the state to DEVICE_CONNECTED
         * @param device
         */
        private synchronized void connected(BluetoothDevice device){
            if(connectionThread != null){
                connectionThread.cancel();
                connectionThread = null;
            }

            Message msg = handler.obtainMessage(DiscoverDevice.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(DiscoverDevice.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            handler.sendMessage(msg);

            setState(STATE_CONNECTED);
        }
    }
}
