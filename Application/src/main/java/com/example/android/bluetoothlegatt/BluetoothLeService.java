/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    public  byte[] data = null;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private int nrTries;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    String serverURL = SampleGattAttributes.getServerURL();
    RequestQueue queue;
    private byte[] orginalCnonce;
    Nonces nonce = new Nonces();
    AES aes = new AES();
    public byte[] deviceIDValue;

    public void setUsername(String username) {
        this.username = username;
    }

    String username;
    boolean verifyserver = false;
    Toast myToast;

    private byte[] sessionKey;
    private final Queue<Runnable> commandQueue = new ConcurrentLinkedQueue<>();
    private boolean commandQueueBusy;
    private boolean isRetrying;
    Handler bleHandler = new Handler(Looper.getMainLooper());


    public static BluetoothDevice getDevice() {
        return device;
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }

    private static  BluetoothDevice device;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        private byte[] serverNonce= null;
        private boolean clientNonce= false;

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == GATT_SUCCESS) {
               broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {

            if (status == GATT_SUCCESS) {
            System.out.println("before reading");
           if(characteristic.getUuid().equals(SampleGattAttributes.getUUIDForName("GattServerNonce"))){
             //   readCustomCharacteristic(SampleGattAttributes.getUUIDForName("GattServerNonce"));
                System.out.println("after "+characteristic.getValue());

            }else if(characteristic.getUuid().equals(SampleGattAttributes.getUUIDForName("deviceID"))){
             //   readCustomCharacteristic(SampleGattAttributes.getUUIDForName("GattServerNonce"));
                System.out.println(" reading"+characteristic.getValue());

            }else if(characteristic.getUuid().equals(SampleGattAttributes.getUUIDForName("realData"))){
                //   readCustomCharacteristic(SampleGattAttributes.getUUIDForName("GattServerNonce"));
                System.out.println(" writing"+characteristic.getValue());

            }
            }else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                // This is where the tricky part comes
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    // Bonding required.
                    // The broadcast receiver should be called.
                } else {
                    // ?
                }
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {

            }else if (status == 0x89) {
                System.out.println("authentication failed");
            }
            // Perform some checks on the status field
            if (status != GATT_SUCCESS) {
                Log.e(TAG, String.format(Locale.ENGLISH,"ERROR: write failed for characteristic: %s, status %d", characteristic.getUuid(), status));
                completedCommand();
                return;
            }

            // Characteristic has been read so processes it

            // We done, complete the command
            completedCommand();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == GATT_SUCCESS) {
                 if (characteristic.getUuid().equals(SampleGattAttributes.getUUIDForName("GattServerNonce"))) {
                    nonce.setSNonce(characteristic.getValue());
                     connect(nonce);
                 }
                if (characteristic.getUuid().equals(SampleGattAttributes.getUUIDForName("clientNonce"))) {
                    System.out.println("Recieved Cnonce" + characteristic.getValue().toString());
                    nonce.setCNonce(characteristic.getValue());
                    clientNonce = true;
                }else  if (characteristic.getUuid().equals(SampleGattAttributes.getUUIDForName("restServerNonce"))) {
                    if(Arrays.equals(serverNonce, aes.decrypt(characteristic.getValue(),sessionKey)))
                        System.out.println("correct session and protection against reply attack");
                } else if(characteristic.getUuid().equals(SampleGattAttributes.getUUIDForName("deviceID"))){
                    nonce.setDeviceID(characteristic.getValue());
                    deviceIDValue = characteristic.getValue();
                }
                  broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                // This is where the tricky part comes
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    // Bonding required.
                    // The broadcast receiver should be called.
                } else {
                    // ?
                }
            } else if (status == 0x89) {
                System.out.println("authentication failed");
            }

            // Perform some checks on the status field
            if (status != GATT_SUCCESS) {
                Log.e(TAG, String.format(Locale.ENGLISH,"ERROR: Read failed for characteristic: %s, status %d", characteristic.getUuid(), status));
                completedCommand();
                return;
            }

            // Characteristic has been read so processes it

            // We done, complete the command
            completedCommand();
        }


        private void connect(Nonces nonce) {
            String token = serverURL + "/token";
            // Optional Parameters to pass as POST request
            JSONObject js = new JSONObject();
            try {
                js.put("SNonce",new String(nonce.SNonce,java.nio.charset.StandardCharsets.ISO_8859_1) );
                js.put("CNonce", new String(nonce.CNonce,java.nio.charset.StandardCharsets.ISO_8859_1));
                js.put("deviceID", new String(nonce.deviceID,java.nio.charset.StandardCharsets.ISO_8859_1));
                js.put("username", username);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Make request for JSONObject
            JsonObjectRequest jsonObjReq = new JsonObjectRequest(
                    Request.Method.POST, token, js,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if(response!=null){
                                //client nonce
                                String clientDecryptedNonce = (String) response.get("CNonce");
                                byte[] clientDecryptedNonceBytes = clientDecryptedNonce.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                                //server nonce
                                String serverDecryptedCNonce = (String) response.get("SNonce");
                                byte[] serverDecryptedNonceBytes = serverDecryptedCNonce.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                                //session key
                                String ReceivedSessionKey = (String) response.get("sessionKey");
                                sessionKey = ReceivedSessionKey.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                                //encrypted session Key that will be sent to gatt server.
                                String encryptedSessionKey = (String) response.get("encryptedSessionKey");
                                byte[] encryptedSessionKeyBytes = encryptedSessionKey.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

                                //server nonce
                                String ReceivedServerNonce = (String) response.get("serverNonce");
                                serverNonce = ReceivedServerNonce.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                                //encrypted server nonce that will be sent to gatt server.
                                String encryptedServerNonce = (String) response.get("encryptedServerNonce");
                                byte[] encryptedServerNonceBytes = encryptedServerNonce.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

                                if (Arrays.equals(orginalCnonce, clientDecryptedNonceBytes)) {
                                    System.out.println("client are sure about the server is real one");
                                    verifyserver = true;                                    //mBluetoothGatt.disconnect();
                                    //byte[] concatenatednonces = new byte[serverDecryptedNonceBytes.length + encryptedSessionKeyBytes.length];
                                    //System.arraycopy(serverDecryptedNonceBytes, 0, concatenatednonces, 0, serverDecryptedNonceBytes.length);
                                    //System.arraycopy( encryptedSessionKeyBytes, 0, concatenatednonces, serverDecryptedNonceBytes.length,  encryptedSessionKeyBytes.length);
                                    writeCustomCharacteristic(serverDecryptedNonceBytes, SampleGattAttributes.getUUIDForName("GattServerNonce"));
                                    writeCustomCharacteristic(encryptedSessionKeyBytes, SampleGattAttributes.getUUIDForName("sessionKey"));
                                    writeCustomCharacteristic(encryptedServerNonceBytes, SampleGattAttributes.getUUIDForName("restServerNonce"));
                                    readCustomCharacteristicForService(SampleGattAttributes.getUUIDForName("restServerNonce"));
                                } else {
                                    mBluetoothGatt.disconnect();
                                }

                                Log.d(TAG, response.get("CNonce") + " i am queen");
                                }else {
                                    System.out.println("you are not authenticated");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();

                        }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                      VolleyLog.d(TAG, "Error: " + error.getMessage());
                }
            }) {

                /**
                 * Passing some request headers
                 */
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "application/json; charset=utf-8");
                    return headers;
                }

            };

            // Adding request to request queue
            queue = Volley.newRequestQueue(getApplicationContext());

            queue.add(jsonObjReq);
        }

    };



    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
            data = characteristic.getValue();


        if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }

        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        mBluetoothGatt.close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                boolean MTU = mBluetoothGatt.requestMtu(512);
                return true;
            } else {
                return false;
            }
        }

        device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        //int bond = device.getBondState();
        //there is an already bond 10
       // if(bond==10)
        //device.createBond();
        //boolean MTU = mBluetoothGatt.requestMtu(512);

        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
    public void readCustomCharacteristicForService(UUID uuid) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(SampleGattAttributes.getUUIDForName("SecurityService"));
        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
        }
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(uuid);
        if(readCharacteristic(mReadCharacteristic) == false){
            Log.w(TAG, "Failed to read characteristic");
        }

    }
    public void readCustomCharacteristicForService(UUID uuid, String serviceName) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(SampleGattAttributes.getUUIDForName(serviceName));
        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
        }
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(uuid);
        if(readCharacteristic(mReadCharacteristic) == false){
            Log.w(TAG, "Failed to read characteristic");
        }

    }
    public boolean readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        if(mBluetoothGatt == null) {
            Log.e(TAG, "ERROR: Gatt is 'null', ignoring read request");
            return false;
        }

        // Check if characteristic is valid
        if(characteristic == null) {
            Log.e(TAG, "ERROR: Characteristic is 'null', ignoring read request");
            return false;
        }

        // Check if this characteristic actually has READ property
        if((characteristic.getProperties() & PROPERTY_READ) == 0 ) {
            Log.e(TAG, "ERROR: Characteristic cannot be read");
            return false;
        }

        // Enqueue the read command now that all checks have been passed
        boolean result = commandQueue.add(new Runnable() {

            @Override
            public void run() {
                if(!mBluetoothGatt.readCharacteristic(characteristic)) {
                    Log.e(TAG, String.format("ERROR: readCharacteristic failed for characteristic: %s", characteristic.getUuid()));
                    completedCommand();
                } else {
                    Log.d(TAG, String.format("reading characteristic <%s>", characteristic.getUuid()));
                    nrTries++;
                }
            }
        });

        if(result) {
            nextCommand();
        } else {
            Log.e(TAG, "ERROR: Could not enqueue read characteristic command");
        }
        return result;
    }

    private void nextCommand() {
        // If there is still a command being executed then bail out
        if(commandQueueBusy) {
            return;
        }

        // Check if we still have a valid gatt object
        if (mBluetoothGatt == null) {
            Log.e(TAG, String.format("ERROR: GATT is 'null' for peripheral '%s', clearing command queue"));
            commandQueue.clear();
            commandQueueBusy = false;
            return;
        }

        // Execute the next command in the queue
        if (commandQueue.size() > 0) {
            final Runnable bluetoothCommand = commandQueue.peek();
            commandQueueBusy = true;
            nrTries = 0;

                bleHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        bluetoothCommand.run();
                    } catch (Exception ex) {
                        Log.e(TAG, String.format("ERROR: Command exception for device '%s'"), ex);
                    }
                }
            });
        }
    }
    private void completedCommand() {
        commandQueueBusy = false;
        isRetrying = false;
        commandQueue.poll();
        nextCommand();
    }

    public void writeCustomCharacteristic(byte[] value,UUID uuid) {
       // System.out.println("Client nonces........ttt........");

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        //System.out.println("Client nonces........ela........");

        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(SampleGattAttributes.getUUIDForName("SecurityService"));
        //System.out.println("Client nonces........ali........");

        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return;
        }

        /*get the read characteristic from the service*/
        final  BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(uuid);
        if(sessionKey!=null && uuid.equals(SampleGattAttributes.getUUIDForName("realData"))){
            value = aes.encryptwihpadding(value,sessionKey);
        }
        mWriteCharacteristic.setValue(value);
        //mWriteCharacteristic.setWriteType(2);
        if(uuid.equals(SampleGattAttributes.getUUIDForName("clientNonce"))) {
            orginalCnonce = value;
            //mWriteCharacteristic.setWriteType(1);
          //  System.out.println("Cnonce" + orginalCnonce.toString());
        }

        //System.out.println("Client nonces........................kkkkkkkkk......................................");
        //if(mBluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false){
          // Log.w(TAG, "Failed to write characteristic");
        //}
        // Enqueue the write command now that all checks have been passed
        boolean result = commandQueue.add(new Runnable() {
            @Override
            public void run() {
                if(!mBluetoothGatt.writeCharacteristic(mWriteCharacteristic)) {
                    Log.e(TAG, String.format("ERROR: readCharacteristic failed for characteristic: %s", mWriteCharacteristic.getUuid()));
                    completedCommand();
                } else {
                    Log.d(TAG, String.format("reading characteristic <%s>", mWriteCharacteristic.getUuid()));
                    nrTries++;
                }
            }
        });

        if(result) {
            nextCommand();
        } else {
            Log.e(TAG, "ERROR: Could not enqueue read characteristic command");
        }
    }

    public void writeCustomCharacteristic(byte[] value,UUID uuid,String serviceName) {
       // System.out.println("Client nonces........ttt........");

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        //System.out.println("Client nonces........ela........");

        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(SampleGattAttributes.getUUIDForName(serviceName));
        //System.out.println("Client nonces........ali........");

        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return;
        }
        /*get the read characteristic from the service*/
        final  BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(uuid);

        if(sessionKey!=null && uuid.equals(SampleGattAttributes.getUUIDForName("realData"))){
            value = aes.encryptwihpadding(value,sessionKey);
        }

        mWriteCharacteristic.setValue(value);
        //mWriteCharacteristic.setWriteType(2);
        if(uuid.equals(SampleGattAttributes.getUUIDForName("clientNonce"))) {
            orginalCnonce = value;
            //mWriteCharacteristic.setWriteType(1);
          //  System.out.println("Cnonce" + orginalCnonce.toString());
        }
        //System.out.println("Client nonces........................kkkkkkkkk......................................");
        //if(mBluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false){
          // Log.w(TAG, "Failed to write characteristic");
        //}
        // Enqueue the write command now that all checks have been passed
        boolean result = commandQueue.add(new Runnable() {

            @Override
            public void run() {
                if(!mBluetoothGatt.writeCharacteristic(mWriteCharacteristic)) {
                    Log.e(TAG, String.format("ERROR: readCharacteristic failed for characteristic: %s", mWriteCharacteristic.getUuid()));
                    completedCommand();
                } else {
                    Log.d(TAG, String.format("reading characteristic <%s>", mWriteCharacteristic.getUuid()));
                    nrTries++;
                }
            }
        });

        if(result) {
            nextCommand();
        } else {
            Log.e(TAG, "ERROR: Could not enqueue read characteristic command");
        }
    }
}
