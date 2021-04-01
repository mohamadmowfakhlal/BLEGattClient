package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.content.Intent;

import android.os.Build;
import android.os.Bundle;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FirstFragment extends Fragment {
    EditText username;
    EditText password;
    Toast myToast;
    String serverURL = "http://ec2-18-185-6-210.eu-central-1.compute.amazonaws.com";
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState

    ) {
        // Inflate the layout for this fragment
        View fragmentFirstLayout = inflater.inflate(R.layout.fragment_first, container, false);
        // Get the count text view
        username = fragmentFirstLayout.findViewById(R.id.username);
        password = fragmentFirstLayout.findViewById(R.id.password);
        //login = fragmentFirstLayout.findViewById(R.id.textview_first);
        return fragmentFirstLayout;

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.count_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user_name = username.getText().toString();
                String pass_word = password.getText().toString();
                // Instantiate the RequestQueue.
                RequestQueue queue =  Volley.newRequestQueue(getActivity().getApplicationContext());
                    // Add the request to the RequestQueue.
                    //queue.add(stringRequest);*/
                String authorizationURL =serverURL+"/checkauthorizatation/"+ user_name +"?password="+pass_word+ "";
                GsonRequest<Device[]> myReq = new GsonRequest<Device[]>(Request.Method.GET,null,
                        authorizationURL,
                        Device[].class,null,
                        createMyReqSuccessListener(),
                        createMyReqErrorListener());
                queue.add(myReq);

            }
        });

    }




    private Response.Listener<Device[]> createMyReqSuccessListener() {

        return new Response.Listener<Device[]>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Device[] response) {
                List<Device> Devices = Arrays.asList(response);
                ((MainActivity) getActivity()).setObjectList( Devices);
                boolean empty = Devices.isEmpty();
                if(!empty){
                    Intent intent = new Intent(getActivity(), DeviceScanActivity.class);
                    ArrayList<Device> names = new ArrayList<>(Devices);
                    intent.putExtra("mylist", names);
                    startActivity(intent);
                      //NavHostFragment.findNavController(FirstFragment.this).navigate( FirstFragmentDirections.actionFirstFragmentToScanFragment());
                }else{
                    myToast = Toast.makeText(getActivity(), "failed to login !", Toast.LENGTH_SHORT);
                    myToast.show();
                }

            }
        };
    }
    private Response.ErrorListener createMyReqErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Error ");
            }
        };
    }
}

