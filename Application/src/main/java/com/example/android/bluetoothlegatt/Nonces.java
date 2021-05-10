package com.example.android.bluetoothlegatt;

import org.json.JSONException;
import org.json.JSONObject;

public class Nonces {
public byte[] CNonce;
public byte[] SNonce;
public byte [] deviceID;
public byte[] getCNonce() {
	return CNonce;
}
public void setCNonce(byte[] cNonce) {
	CNonce = cNonce;
}
public byte[] getSNonce() {
	return SNonce;
}
public void setSNonce(byte[] sNonce) {
	SNonce = sNonce;
}
public byte[] getDeviceID() {
	return deviceID;
}
public void setDeviceID(byte[] deviceid) {
	deviceID = deviceid;
}

	public JSONObject toJSON() throws JSONException {

		JSONObject jo = new JSONObject();
		jo.put("SNonce", SNonce);
		jo.put("deviceID", deviceID);
		jo.put("CNonce",CNonce);

		return jo;
	}
}