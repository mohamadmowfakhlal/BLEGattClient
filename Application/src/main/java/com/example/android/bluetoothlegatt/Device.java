package com.example.android.bluetoothlegatt;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class Device implements Parcelable {
	private UUID id;

    public String deviceID;
    public String MAC;
    public byte[] CNonce;
    public byte[] SNonce;
    public Device() {
    }

    public Device(String deviceID, String MAC) {
    	id = UUID.randomUUID();
        this.deviceID = deviceID;
        this.MAC = MAC;
    }

    protected Device(Parcel in) {
        deviceID = in.readString();
        MAC = in.readString();
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    @Override
    public String toString() {
        return "Device [DeviceID=" + deviceID + ", MAC=" + MAC + "]";
    }

    public JSONObject toJSON() throws JSONException {

        JSONObject jo = new JSONObject();
        jo.put("integer", deviceID);
        jo.put("string", MAC);

        return jo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceID);
        dest.writeString(MAC);
    }
}
