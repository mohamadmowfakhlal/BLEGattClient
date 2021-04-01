package com.example.android.bluetoothlegatt;

import java.util.List;

public class DeviceList {
    private List<Device> customResponseList;
    public List<Device> getCustomResponseList() {
        return customResponseList;
    }
    public void setCustomResponseList(List<Device> customResponseList) {
        this.customResponseList = customResponseList;
    }
}