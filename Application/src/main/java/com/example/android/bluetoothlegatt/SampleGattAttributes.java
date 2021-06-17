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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    public static String getServerURL() {
        return serverURL;
    }

    private static  String serverURL = "http://ec2-3-122-228-173.eu-central-1.compute.amazonaws.com";

    static {
        attributes.put("00001805-0000-1000-8000-00805f9b34fb","TimeService");
        attributes.put("fb440001-8000-0080-0010-00000d180000","ConfigurationService");
        attributes.put("fb340001-8000-0080-0010-00000d180000", "SecurityService");
        attributes.put("fb340002-8000-0080-0010-00000d180000","key");
        attributes.put("fb340003-8000-0080-0010-00000d180000", "clientNonce");
        attributes.put("fb340004-8000-0080-0010-00000d180000", "GattServerNonce");
        attributes.put("fb340005-8000-0080-0010-00000d180000","MAC");
        attributes.put("fb340006-8000-0080-0010-00000d180000","restServerNonce");
        attributes.put("fb340007-8000-0080-0010-00000d180000","sessionNumber");
        attributes.put("fb340008-8000-0080-0010-00000d180000","GattSessionRestServerNonce");
        attributes.put("fb340009-8000-0080-0010-00000d180000","deviceID");
        attributes.put("fb340010-8000-0080-0010-00000d180000","realData");

       // public static final UUID GattSessionRestServerNonce_UUID = UUID.fromString("fb340008-8000-0080-0010-00000d180000");

    }
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static UUID getUUIDForName(String name){
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getValue().equals(name)) {
                System.out.println(entry.getKey());
                return UUID.fromString(entry.getKey());
            }
        }
        return  null;
        }
}
