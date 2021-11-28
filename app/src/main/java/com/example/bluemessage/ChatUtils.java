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
import java.io.InputStream;
import java.io.OutputStream;
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
    private MessageThread messageThread;

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

    /**
     * Starts all threads for communication and will cancel the thread if a thread is currently active
     */
    private synchronized void start(){
        if(connectionThread != null){
            connectionThread.cancel();
            connectionThread = null;
        }
        if(acceptThread == null){
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        if(messageThread != null){
            messageThread.cancel();
            messageThread = null;
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
        if(messageThread != null){
            messageThread.cancel();
            messageThread = null;
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
        if(messageThread != null){
            messageThread.cancel();
            messageThread = null;
        }

        connectionThread = new ConnectThread(device);
        connectionThread.start();

        setState(STATE_CONNECTING);
    }

    public void write(byte[] buffer){
        MessageThread mThread;
        synchronized(this){
            if(state != STATE_CONNECTED){
                return;
            }
            mThread = messageThread;
        }

        mThread.write(buffer);
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

    private class MessageThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        /**
         * constructor to set up input and output streams
         * @param socket
         */
        public MessageThread(BluetoothSocket socket){
            this.socket = socket;
            InputStream in = null;
            OutputStream out = null;

            try{
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }catch (IOException e){
                Log.e("Connect to in/outstream", e.toString());
            }
            inputStream = in;
            outputStream = out;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            try{
                bytes = inputStream.read(buffer);
                handler.obtainMessage(DiscoverDevice.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
            }catch (IOException e){
                connectionLost();
                Log.e("Input to Run", e.toString());
            }
        }

        public void write(byte[] buffer){
            try{
                outputStream.write(buffer);
                handler.obtainMessage(DiscoverDevice.MESSAGE_Write, -1, -1, buffer).sendToTarget();
            }catch(IOException e){

            }
        }

        public void cancel(){
            try{
                socket.close();
            }catch(IOException e){

            }
        }

        /**
         * method for when connection is lost between send and receiving messages.
         * Will send a message to the handler saying the the connection is lost then it will restart ChatUtils for next request
         */
        private void connectionLost(){
            Message message = handler.obtainMessage(DiscoverDevice.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(DiscoverDevice.TOAST, "Connection Lost");
            message.setData(bundle);
            handler.sendMessage(message);

            ChatUtils.this.start();
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

            connected(device, socket);

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
        private synchronized void connected(BluetoothDevice device, BluetoothSocket socket){
            if(connectionThread != null){
                connectionThread.cancel();
                connectionThread = null;
            }

            if(messageThread != null){
                messageThread.cancel();
                messageThread = null;
            }

            messageThread = new MessageThread(socket);
            messageThread.start();

            Message msg = handler.obtainMessage(DiscoverDevice.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(DiscoverDevice.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            handler.sendMessage(msg);

            setState(STATE_CONNECTED);
        }
    }
}
