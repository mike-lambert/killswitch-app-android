package com.github.mikelambert.killswitch.io.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.github.mikelambert.killswitch.common.CircuitState;
import com.github.mikelambert.killswitch.common.HardwareCircuit;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

public class KillswitchUsbCircuit implements HardwareCircuit {
    private final Context context;
    private final String descriptor;
    private UsbDevice device;

    public KillswitchUsbCircuit(Context context, String descriptor) {
        this.context = context;
        this.descriptor = descriptor;
    }

    @Override
    public boolean isTarget() {
        return false;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void connect() {

    }

    @Override
    public void lockOn(boolean fireOnDisconnect) {

    }

    @Override
    public void unlock() {

    }

    @Override
    public boolean ping() {
        return false;
    }

    @Override
    public CircuitState state() {
        return null;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescriptor() {
        return null;
    }

    private void refresh() {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbSerialProber usbDefaultProber = UsbSerialProber.getDefaultProber();
        for(UsbDevice device : usbManager.getDeviceList().values()) {
            UsbSerialDriver driver = usbDefaultProber.probeDevice(device);
            if(driver != null) {
                for(int port = 0; port < driver.getPorts().size(); port++){
                    UsbSerialPort p = driver.getPorts().get(port);
                    //TODO: match
                    //p.getDevice().
                }

            } else {

            }
        }
    }
}
