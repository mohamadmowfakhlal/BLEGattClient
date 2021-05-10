package com.example.android.bluetoothlegatt;

import android.content.Intent;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

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
import java.util.HashMap;
import java.util.Map;

public class FirstFragment extends Fragment {
    EditText username;
    EditText password;
    Toast myToast;
    String serverURL = SampleGattAttributes.getServerURL();
    String user_name;
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
        view.findViewById(R.id.test_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                user_name = username.getText().toString();
                String pass_word = password.getText().toString();
                // Instantiate the RequestQueue.
                RequestQueue queue =  Volley.newRequestQueue(getActivity().getApplicationContext());
                    // Add the request to the RequestQueue.
                    //queue.add(stringRequest);*/
                JSONObject js = new JSONObject();
                try {

                    js.put("password", pass_word);
                    js.put("username", user_name);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // Make request for JSONObject
                JsonObjectRequest jsonObjReq = new JsonObjectRequest(
                        Request.Method.POST, serverURL+"/checkauthentication/", js,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                if(response!=null) {
                                    try {
                                        if(response.get("session") != null){
                                            Intent intent = new Intent(getActivity(), DeviceScanActivity.class);
                                            intent.putExtra("username", user_name);
                                            startActivity(intent);
                                        }else{
                                            myToast = Toast.makeText(getActivity(), "failed to login !", Toast.LENGTH_SHORT);
                                            myToast.show();
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //VolleyLog.d(TAG, "Error: " + error.getMessage());
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
                queue.add(jsonObjReq);
            }
        });
    }


}

