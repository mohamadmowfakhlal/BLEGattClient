package com.example.android.bluetoothlegatt;

import org.json.JSONException;
import org.json.JSONObject;

public class Nonces {
public byte[] CNonce;
public byte[] SNonce;
public String MAC;
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
public String getMAC() {
	return MAC;
}
public void setMAC(String mAC) {
	MAC = mAC;
}

	public JSONObject toJSON() throws JSONException {

		JSONObject jo = new JSONObject();
		jo.put("SNonce", SNonce);
		jo.put("MAC", MAC);
		jo.put("CNonce",CNonce);

		return jo;
	}
}