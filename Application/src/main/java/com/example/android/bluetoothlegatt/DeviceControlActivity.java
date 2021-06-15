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

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
//import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.things.bluetooth.BluetoothConfigManager;

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

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String USER_NAME = "USER_NAME";

    private String data = null;
    private TextView mDataField;
    private Button writeButton;

    private EditText mkeyField;
    private String mDeviceName;
    private String mDeviceAddress;
    private byte[] devID;
    private String username;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    boolean numericcomparison = false ;
    boolean passkey = false ;
    boolean justworks = false ;
    boolean pin = false;
    boolean plaintext = true ;

    private EditText deviceID;
    private TextView deviceIDLabel;
    private boolean changeDeviceID;
    private Button configureButton;
    private Button listServiceButton;
    private Button changeKeyButton;
    private Button saveButton;
    private String serverURL = SampleGattAttributes.getServerURL();
    RequestQueue queue;
    Toast myToast;
    // Start GATT delay (wait for BLE scan to actually finish)
    private static final long START_GATT_DELAY = 500; // msec
    byte[] encryptednewKey;
    AES aes;
    private final Handler mStartGattHandler = new Handler();
    private final Runnable mStartGattRunnable = new Runnable() {
        @Override
        public void run() {
            Intent gattServiceIntent = new Intent(DeviceControlActivity.this,
                    BluetoothLeService.class);bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mPairingRequestReceiver = new BroadcastReceiver() {
        @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothDevice.ACTION_PAIRING_REQUEST.
                equals(intent.getAction())) { // either            numeric comparison or passkey is used
            int pairingtype = intent.getIntExtra(
                    BluetoothDevice.EXTRA_PAIRING_VARIANT,
                    BluetoothDevice.ERROR);

            if (pairingtype == BluetoothDevice.
                    PAIRING_VARIANT_PASSKEY_CONFIRMATION) {
                numericcomparison = true;
                plaintext = false;
                System.out.println("numeric");
            }
            if (pairingtype == BluetoothDevice.
                    PAIRING_VARIANT_PIN) {
                passkey = true;
                plaintext = false;
                System.out.println("passkey");
               // mBluetoothLeService.getDevice() .setPin("dd".getBytes());

            }else if(pairingtype == 4){
                pin = true;
                plaintext =false;
                System.out.println("authentication using pin ");
            }
        }

        if (BluetoothDevice.ACTION_BOND_STATE_CHANGED
                .equals(intent.getAction())) { // Bonding 11,            bonded 12, or bonding none 10( failure )?
            int bondstate = intent.getIntExtra(
                    BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothDevice.ERROR);

            if (bondstate == BluetoothDevice.BOND_BONDED) {

                if (!numericcomparison && !passkey && !pin) {
                    justworks = true;
                    plaintext = false;
                    System.out.println("just work");
                }
            }else if(bondstate == BluetoothDevice.BOND_BONDING){

            }
        }
    }
    };
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices(),false);
                long startTime = System.currentTimeMillis();
                System.out.println("startTime"+startTime);
                establishSecureConnection();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
        }
        }

    };



    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();

                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                // If there is an active notification on a characteristic, clear
                                // it first so it doesn't update the data field on the user interface.
                                if (mNotifyCharacteristic != null) {
                                    mBluetoothLeService.setCharacteristicNotification(
                                            mNotifyCharacteristic, false);
                                    mNotifyCharacteristic = null;
                                }
                                mBluetoothLeService.readCharacteristic(characteristic);
                            }
                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                mNotifyCharacteristic = characteristic;
                                mBluetoothLeService.setCharacteristicNotification(
                                        characteristic, true);
                            }
                            return true;
                    }
                    return false;
                }
    };


    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);

        mDataField.setText(R.string.no_data);
        deviceID.setVisibility(View.INVISIBLE);
       // newKey.setVisibility(View.INVISIBLE);
        deviceIDLabel.setVisibility(View.INVISIBLE);
        //newKeyLabel.setVisibility(View.INVISIBLE);
        configureButton.setVisibility(View.INVISIBLE);
        listServiceButton.setVisibility(View.INVISIBLE);
        changeKeyButton.setVisibility(View.INVISIBLE);
        saveButton.setVisibility(View.INVISIBLE);
        writeButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.button_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        username = intent.getStringExtra(USER_NAME);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        // Sets up UI references.

        aes = new AES();
        mDataField = (TextView) findViewById(R.id.data_value);
        writeButton = (Button) findViewById(R.id.test_button);
        writeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // your handler code here
                SecretKeySpec macKey = new SecretKeySpec(mBluetoothLeService.getSessionKey(), "HmacMD5");
                aes.initMAC(macKey);
                byte[] data = "changeo2levelchangeo2leve".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                //mBluetoothLeService.writeCustomCharacteristic(mBluetoothLeService.sessionNumber.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),SampleGattAttributes.getUUIDForName("sessionNumber"),"SecurityService");
                mBluetoothLeService.writeCustomCharacteristic(data,SampleGattAttributes.getUUIDForName("realData"),"SecurityService");
                mBluetoothLeService.writeCustomCharacteristic(aes.calculateMAC(data),SampleGattAttributes.getUUIDForName("MAC"),"SecurityService");
            }
        });

        deviceID = (EditText) findViewById(R.id.deviceID);
        deviceIDLabel = (TextView) findViewById(R.id.deviceIDLabel);
        configureButton = (Button) findViewById(R.id.configue_button);
        changeKeyButton = (Button) findViewById(R.id.changekey_button);
        saveButton = (Button) findViewById(R.id.save_button);
        listServiceButton = (Button) findViewById(R.id.listService_button);
        configureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deviceID.setVisibility(View.VISIBLE);
                deviceIDLabel.setVisibility(View.VISIBLE);
                //newKey.setVisibility(View.INVISIBLE);
               // newKeyLabel.setVisibility(View.INVISIBLE);
                saveButton.setVisibility(View.VISIBLE);
                displayGattServices(mBluetoothLeService.getSupportedGattServices(),false);
                changeDeviceID = true;
                mBluetoothLeService.readCustomCharacteristicForService(SampleGattAttributes.getUUIDForName("deviceID"));
            }
        });

        listServiceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deviceID.setVisibility(View.INVISIBLE);
                deviceIDLabel.setVisibility(View.INVISIBLE);
                saveButton.setVisibility(View.INVISIBLE);
                displayGattServices(mBluetoothLeService.getSupportedGattServices(),true);
                //startActivity(intent);
            }
        });
        changeKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceID.setVisibility(View.INVISIBLE);
                deviceIDLabel.setVisibility(View.INVISIBLE);
                displayGattServices(mBluetoothLeService.getSupportedGattServices(),false);
                changeDeviceID = false;
                byte[] key = new byte[16];
                new SecureRandom().nextBytes(key);
                encryptednewKey = aes.encrypt(key,mBluetoothLeService.getSessionKey());
                devID = mBluetoothLeService.deviceIDValue;
                setNewKey(devID,key);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                if(changeDeviceID){
                    devID = mBluetoothLeService.deviceIDValue;
                    setDeviceID(deviceID.getText().toString(),devID);
                }
            }
        });

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        mStartGattHandler.postDelayed(mStartGattRunnable, START_GATT_DELAY);

//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        IntentFilter pairingRequestFilter = new
                IntentFilter (BluetoothDevice.ACTION_FOUND) ;
        pairingRequestFilter.addAction (BluetoothDevice.ACTION_BOND_STATE_CHANGED );
        pairingRequestFilter.addAction (
                BluetoothDevice.ACTION_PAIRING_REQUEST );
        registerReceiver (mPairingRequestReceiver ,
                pairingRequestFilter );
        pairingRequestFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY );
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        unregisterReceiver(mPairingRequestReceiver);
        //unpairDevice(mBluetoothLeService.getDevice());
        mBluetoothLeService = null;

    }
    // Function to unpair from passed in device
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) { Log.e(TAG, e.getMessage()); }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
               // mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data1) {
        if (data1 != null) {
            mDataField.setText(data1);
            data = data1;
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices,boolean show) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                //if(gattCharacteristic.getUuid().equals(SampleGattAttributes.getUUIDForName("deviceID")) && gattCharacteristic.getValue() != null)
                    //deviceIDValue.setText(gattCharacteristic.getValue().toString());
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);

        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        if(show)
            mGattServicesList.setAdapter(gattServiceAdapter);
        else
            mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    public void establishSecureConnection() {
        byte[] Cnonce = new byte[16];
        new SecureRandom().nextBytes(Cnonce);
        mBluetoothLeService.writeCustomCharacteristic(Cnonce,SampleGattAttributes.getUUIDForName("clientNonce"));
        mBluetoothLeService.readCustomCharacteristicForService(SampleGattAttributes.getUUIDForName("GattSessionRestServerNonce"));
        //mBluetoothLeService.readCustomCharacteristicForService(SampleGattAttributes.getUUIDForName("clientNonce"));
        //mBluetoothLeService.readCustomCharacteristicForService(SampleGattAttributes.getUUIDForName("deviceID"));
        //mBluetoothLeService.readCustomCharacteristicForService(SampleGattAttributes.getUUIDForName("GattServerNonce"));
        mBluetoothLeService.setUsername(username);
    }

    private void setDeviceID(String  deviceID,byte[] oldDeviceID) {
        String token = serverURL + "/deviceID";
        // Optional Parameters to pass as POST request
        JSONObject js = new JSONObject();
        try {
            js.put("deviceID", deviceID);
            js.put("oldDeviceID", new String(oldDeviceID,java.nio.charset.StandardCharsets.ISO_8859_1));
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
                            if(response.length() !=0){
                                String deviceID = (String) response.get("deviceID");
                                byte[] encryptedDeviceID = aes.encryptwihpadding(deviceID.getBytes(),mBluetoothLeService.getSessionKey());
                                //mBluetoothLeService.writeCustomCharacteristic(mBluetoothLeService.sessionNumber.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),SampleGattAttributes.getUUIDForName("sessionNumber"),"SecurityService");
                                mBluetoothLeService.writeCustomCharacteristic(encryptedDeviceID,SampleGattAttributes.getUUIDForName("deviceID"));
                                mBluetoothLeService.readCustomCharacteristicForService(SampleGattAttributes.getUUIDForName("deviceID"));
                            }else{
                                myToast = Toast.makeText(DeviceControlActivity.this, "session is finished !", Toast.LENGTH_SHORT);
                                myToast.show();
                                final Intent intent = new Intent(DeviceControlActivity.this, MainActivity.class);
                                startActivity(intent);
                                mConnected = false;
                                updateConnectionState(R.string.disconnected);
                                invalidateOptionsMenu();
                                clearUI();
                                onDestroy();
                            }
                        } catch (Exception e) {

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

    private void setNewKey(byte[]  deviceID,byte[] newKey) {
        String token = serverURL + "/key";
        // Optional Parameters to pass as POST request
        JSONObject js = new JSONObject();
        try {
            js.put("deviceID",  new String(deviceID,java.nio.charset.StandardCharsets.ISO_8859_1));
            js.put("key", new String(newKey,java.nio.charset.StandardCharsets.ISO_8859_1));
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
                                //mBluetoothLeService.writeCustomCharacteristic(mBluetoothLeService.sessionNumber.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),SampleGattAttributes.getUUIDForName("sessionNumber"),"SecurityService");
                                mBluetoothLeService.writeCustomCharacteristic(encryptednewKey,SampleGattAttributes.getUUIDForName("key"));
                                //key must not read as it transfer in plaintext next line should be commented
                                mBluetoothLeService.readCustomCharacteristicForService(SampleGattAttributes.getUUIDForName("key"));
                            }else{
                                myToast = Toast.makeText(DeviceControlActivity.this, "session is finished !", Toast.LENGTH_SHORT);
                                myToast.show();
                                //final Intent intent = new Intent(DeviceControlActivity.this, MainActivity.class);
                                //startActivity(intent);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();

                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                myToast = Toast.makeText(DeviceControlActivity.this, "failed to login !", Toast.LENGTH_SHORT);
                myToast.show();
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


}




