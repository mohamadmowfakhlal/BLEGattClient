package com.example.android.bluetoothlegatt;

import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Parcel;
import android.os.Parcelable;


import java.util.UUID;

public class Nonc implements Parcelable {
	private UUID id;

	public byte[] CNonce;
	public byte[] SNonce;
	public String MAC;



	public Nonc() {
    }

    public Nonc( byte[] CNonce,  byte[] SNonce) {
    	id = UUID.randomUUID();
        this.CNonce = CNonce;
        this.SNonce = SNonce;
    }

	protected Nonc(Parcel in) {
		CNonce = in.createByteArray();
		SNonce = in.createByteArray();
		MAC = in.readString();
	}

	public static final Creator<Nonc> CREATOR = new Creator<Nonc>() {
		@Override
		public Nonc createFromParcel(Parcel in) {
			return new Nonc(in);
		}

		@Override
		public Nonc[] newArray(int size) {
			return new Nonc[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByteArray(CNonce);
		dest.writeByteArray(SNonce);
		dest.writeString(MAC);
	}

	@Override
	public String toString() {
		return "Device [DeviceID=" + CNonce + ", MAC=" + MAC + "]";
	}

	public JSONObject toJSON() throws JSONException {

		JSONObject jo = new JSONObject();
		jo.put("byte[]", CNonce);
		jo.put("string", MAC);

		return jo;
	}
}