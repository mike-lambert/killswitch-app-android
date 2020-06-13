package com.github.mikelambert.killswitch.ui.devices;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.companion.AssociationRequest;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.github.mikelambert.killswitch.KillswitchApplication;
import com.github.mikelambert.killswitch.R;
import com.github.mikelambert.killswitch.common.HardwareCircuit;
import com.github.mikelambert.killswitch.common.KillswitchDeviceAdministrator;
import com.github.mikelambert.killswitch.event.KillswitchBluetoothGracefulDisconnect;
import com.github.mikelambert.killswitch.io.ble.KillswitchBluetoothCircuit;

import org.greenrobot.eventbus.Subscribe;

import static android.app.Activity.RESULT_OK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.github.mikelambert.killswitch.io.ble.KillswitchBluetoothCircuit.UUID_KILLSWITCH_BLE_SERVICE_PING;

public class DevicesFragment extends Fragment {
    public static int REQUEST_PAIR_DEVICE = 0x0000BBBB;
    public static int REQUEST_BLUETOOTH_PERMISSIONS = 0x000000F1;

    private DevicesViewModel devicesViewModel;
    private Button scanButton;
    private TextView bleDevice;
    private HardwareCircuit last;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        devicesViewModel = ViewModelProviders.of(this).get(DevicesViewModel.class);
        View root = inflater.inflate(R.layout.fragment_devices, container, false);
        scanButton = root.findViewById(R.id.button_ble_scan);
        bleDevice = root.findViewById(R.id.text_ble_token);
        scanButton.setEnabled(false);

        devicesViewModel.getData().observe(getViewLifecycleOwner(), data -> {
            last = data;
            KillswitchDeviceAdministrator killswitch = KillswitchApplication.getInstance(getActivity()).getKillswitch();
            scanButton.setEnabled(!killswitch.isArmed() || killswitch.getBoundCircuit() == null);
            if (last != null) {
                bleDevice.setText(last.getName());
                killswitch.bindCircuit(last);
                scanButton.setText(R.string.label_ble_unbind);
            } else {
                bleDevice.setText("");
            }
        });

        scanButton.setOnClickListener(view -> {
            KillswitchDeviceAdministrator killswitch = KillswitchApplication.getInstance(getActivity()).getKillswitch();
            HardwareCircuit circuit = killswitch.getBoundCircuit();
            if (circuit != null) {
                if (!killswitch.isArmed() && killswitch.getBoundCircuit() != null) {
                    killswitch.getBoundCircuit().disconnect();
                    killswitch.unbindCircuit();
                    devicesViewModel.post(null);
                }
                scanButton.setText(R.string.label_ble_scan);
            } else {
                scanButton.setText(R.string.label_ble_unbind);
                discoverCompanions();
            }
        });

        bleDevice.setOnClickListener(view -> {
            KillswitchDeviceAdministrator killswitch = KillswitchApplication.getInstance(getActivity()).getKillswitch();
            if (!killswitch.isArmed() && killswitch.getBoundCircuit() != null) {
                if (killswitch.getBoundCircuit() != null) {
                    killswitch.getBoundCircuit().disconnect();
                    killswitch.unbindCircuit();
                }
                devicesViewModel.post(null);
            }
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        KillswitchApplication.getEventBus(getActivity()).register(this);
        checkPermissions();
        KillswitchDeviceAdministrator killswitch = KillswitchApplication.getInstance(getActivity()).getKillswitch();
        scanButton.setEnabled(!killswitch.isArmed() || killswitch.getBoundCircuit() == null);
        HardwareCircuit circuit = killswitch.getBoundCircuit();
        if (circuit != null) {
            bleDevice.setText(circuit.getName());
            scanButton.setText(R.string.label_ble_unbind);
        } else {
            scanButton.setText(R.string.label_ble_scan);
        }
    }

    @Override
    public void onPause() {
        KillswitchApplication.getEventBus(getActivity()).unregister(this);
        super.onPause();
    }

    private void checkPermissions() {
        boolean btGranted = isGranted(Manifest.permission.BLUETOOTH);
        boolean btAdminGranted = isGranted(Manifest.permission.BLUETOOTH_ADMIN);
        boolean fineLocation = isGranted(Manifest.permission.ACCESS_FINE_LOCATION);
        if (btGranted && btAdminGranted && fineLocation) {
            Log.v("Devices", "Permissions granted, discover");
            scanButton.setEnabled(true);
        } else {
            // TODO: filter revoked to add
            Log.v("Devices", "Permissions missed, requesting");
            String[] permissions = new String[3];
            permissions[0] = Manifest.permission.BLUETOOTH;
            permissions[1] = Manifest.permission.BLUETOOTH_ADMIN;
            permissions[2] = Manifest.permission.ACCESS_FINE_LOCATION;
            ActivityCompat.requestPermissions(getActivity(), permissions, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    private boolean isGranted(String permission) {
        return ContextCompat.checkSelfPermission(getActivity(), permission) == PERMISSION_GRANTED;
    }

    private void discoverCompanions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CompanionDeviceManager deviceManager = getActivity().getSystemService(CompanionDeviceManager.class);
            Log.v("Devices", "Building filter");
            BluetoothLeDeviceFilter leFilter = new BluetoothLeDeviceFilter.Builder()
                    .setScanFilter(
                            new ScanFilter.Builder()
                                    .setServiceData(new ParcelUuid(UUID_KILLSWITCH_BLE_SERVICE_PING), null)
                                    .build()
                    )
                    .build();
            AssociationRequest pairingRequest = new AssociationRequest.Builder()
                    .addDeviceFilter(leFilter)
                    .setSingleDevice(false)
                    .build();

            Log.v("Devices", "Requesting association");
            deviceManager.associate(pairingRequest,
                    new CompanionDeviceManager.Callback() {
                        @Override
                        public void onDeviceFound(IntentSender chooserLauncher) {
                            try {
                                startIntentSenderForResult(chooserLauncher, REQUEST_PAIR_DEVICE, null, 0, 0, 0, null);
                            } catch (IntentSender.SendIntentException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(CharSequence error) {
                            Log.v("Devices", error.toString());
                        }
                    },
                    null);
        } else {
            // TODO: implement scanning classically
            Log.v("Devices", "CLASSIC SCAN NOT IMPLEMENTED");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PAIR_DEVICE && resultCode == RESULT_OK) {
            // User has chosen to pair with the Bluetooth device.
            Object deviceToPair = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
            Log.v("Devices", "Pending access to BLE device " + deviceToPair);
            handleCompanionPairing(deviceToPair);
        }
    }

    private void handleCompanionPairing(Object deviceToPair) {
        BluetoothDevice device = null;
        if (deviceToPair instanceof BluetoothDevice) {
            Log.v("Devices", "4.4 : " + deviceToPair);
            device = (BluetoothDevice) deviceToPair;
        }

        if (deviceToPair instanceof ScanResult) {
            Log.v("Devices", "5.0+ : " + deviceToPair);
            ScanResult result = (ScanResult) deviceToPair;
            device = result.getDevice();
        }

        if (device == null) {
            Log.v("Devices", "No BLE device acquired");
            return;
        }
        Log.v("Devices", "Bonding with " + device.getName() + "; MAC " + device.getAddress());
        device.createBond();
        KillswitchBluetoothCircuit circuit = new KillswitchBluetoothCircuit(getActivity(), device);
        circuit.setupConnection();
        devicesViewModel.post(circuit);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            Log.v("Devices", "Requested permissions received");
            if (grantResults.length == 3 && grantResults[0] == PERMISSION_GRANTED || grantResults[1] == PERMISSION_GRANTED || grantResults[2] == PERMISSION_GRANTED) {
                scanButton.setEnabled(true);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Subscribe
    public void onBleTokenDisconnect(KillswitchBluetoothGracefulDisconnect event) {
        KillswitchDeviceAdministrator killswitch = KillswitchApplication.getInstance(getActivity()).getKillswitch();
        killswitch.unbindCircuit();
        getActivity().runOnUiThread(() -> {
            scanButton.setEnabled(!killswitch.isArmed() || killswitch.getBoundCircuit() == null);
            scanButton.setText(R.string.label_ble_scan);
            bleDevice.setText("");
        });
    }
}
