package com.github.mikelambert.killswitch.model;

import android.bluetooth.BluetoothDevice;
import android.hardware.usb.UsbDevice;

import com.github.mikelambert.killswitch.common.HardwareCircuit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HardwareToken {
    private HardwareCircuit circuit;
    private BluetoothDevice bluetoothDevice;
    private UsbDevice usbDevice;
}
