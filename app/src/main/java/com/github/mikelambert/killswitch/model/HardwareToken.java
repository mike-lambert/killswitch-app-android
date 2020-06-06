package com.github.mikelambert.killswitch.model;

import android.bluetooth.le.ScanResult;
import android.hardware.usb.UsbDevice;

import lombok.Data;

@Data
public class HardwareToken {
    private ScanResult bluetoothDevice;
    private UsbDevice usbDevice;
}
