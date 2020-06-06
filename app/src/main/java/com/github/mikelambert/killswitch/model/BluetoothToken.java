package com.github.mikelambert.killswitch.model;

import android.bluetooth.le.ScanResult;

import lombok.Data;

@Data
public class BluetoothToken {
    private ScanResult descriptor;
}
