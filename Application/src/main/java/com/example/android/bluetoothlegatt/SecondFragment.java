package com.example.android.bluetoothlegatt;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.os.Bundle;
//import android.support.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

import java.security.SecureRandom;
import java.util.List;

//import androidx.navigation.fragment.navArgs;

public class SecondFragment extends Fragment {
    ListView listView;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false);

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<Device> DeviceList = ((MainActivity) getActivity()).getObjectList();
        listView= view.getRootView().findViewById(R.id.list_view);
        //String[] listItem = getResources().getStringArray(R.array.array_technology);

        String[] values = new String[] { "Android List View",
                "Adapter implementation",
                "Simple List View In Android",
                "Create List View Android",
                "Android Example",
                "List View Source Code",
                "List View Array Adapter",
                "Android Example List View"
        };

        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, values);
        listView.setAdapter(itemsAdapter);

        //Integer count = SecondFragmentArgs.fromBu ndle(getArguments()).getMyArg();
        //String countText = getString(R.string.random_heading, count);
        //TextView headerView = view.getRootView().findViewById(R.id.textview_header);
       // headerView.setText(countText);
        //Random random = new java.util.Random();
        //Integer randomNumber = 0;
        //if (count > 0) {
     //       randomNumber = random.nextInt(count + 1);
       // }

        //TextView randomView = view.getRootView().findViewById(R.id.textview_random);
        //randomView.setText(randomNumber.toString());
        view.findViewById(R.id.button_second).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });

    // Create the adapter to convert the array to views
        DevicesAdpater adapter = new DevicesAdpater(getActivity(), DeviceList);
    // Attach the adapter to a ListView
        ListView listView =  view.getRootView().findViewById(R.id.list_view);
        listView.setAdapter(adapter);
    }

    public class DevicesAdpater extends ArrayAdapter<Device> {
        public DevicesAdpater(Context context, List<Device> users) {
            super(context, 0, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Device device = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user, parent, false);
            }
            // Lookup view for data population
            TextView deviceID = (TextView) convertView.findViewById(R.id.deviceID);
            TextView MAC = (TextView) convertView.findViewById(R.id.MAC);
            // Populate the data into the template view using the data object
            deviceID.setText(device.deviceID);
            MAC.setText(device.MAC);
            // Return the completed view to render on screen
            Button btButton = (Button) convertView.findViewById(R.id.btButton);
            // Cache row position inside the button using `setTag`
            btButton.setTag(position);
            // Attach the click event handler
            btButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = (Integer) view.getTag();
                    // Access the row position here to get the correct data item
                    Device device = getItem(position);


                    byte[] Cnonce = new byte[16];
                    new SecureRandom().nextBytes(Cnonce);
                    String HexCnonce = convertBytesToHex(Cnonce);

                    Nonces nonce = new Nonces();
                    //nonce.setCNonce("4c6bYGRIWzZOsxVQtL7YxQ==");
                    //nonce.setSNonce("SkWRnLgmdqC9HwtFzWGuJA==");
                    nonce.setMAC(device.MAC);
                    // Do what you want here...
                    // Instantiate the RequestQueue.
                    RequestQueue queue =  Volley.newRequestQueue(getActivity().getApplicationContext());
/*                String url ="ec2-35-158-119-174.eu-central-1.compute.amazonaws.com/check/"+ user_name +"?password="+pass_word+ "";

                   // Add the request to the RequestQueue.
                    //queue.add(stringRequest);*/
                    String url ="http://ec2-3-127-81-25.eu-central-1.compute.amazonaws.com/token/";
                    GsonRequest<Nonces> myReq = null;
                    try {
                        myReq = new GsonRequest<Nonces>( Request.Method.POST,nonce.toJSON(),
                                url,
                                Nonces.class,null,
                                createMyReqSuccessListener(),
                                createMyReqErrorListener());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    queue.add(myReq);
                }
            });

            return convertView;
        }

    }

    private Response.Listener<Nonces> createMyReqSuccessListener() {

        return new Response.Listener<Nonces>() {
            @Override
            public void onResponse(Nonces response) {
                System.out.println("Hello world" +response.getCNonce() + response.getSNonce());
            }
        };
    }

    private Response.ErrorListener createMyReqErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Error ");

                // Do whatever you want to do with error.getMessage();
            }
        };
    }
    // util to print bytes in hex
    private static String convertBytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte temp : bytes) {
            result.append(String.format("%02x", temp));
        }
        return result.toString();
    }
}