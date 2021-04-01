package com.example.android.bluetoothlegatt;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AES {

	public  byte[] encrypt(byte[] input, byte[] key) {
		byte[] crypted = null;
		try {
		
			SecretKeySpec skey = new SecretKeySpec(key, "AES");
			
			Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, skey);
			crypted = cipher.doFinal(input);
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		
		return crypted;
	}

	public static byte[] decrypt(byte[] input, byte[] key) {
		byte[] output = null;
		try {
			SecretKeySpec skey = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, skey);
			output = cipher.doFinal(input);
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return  output;
	}



}