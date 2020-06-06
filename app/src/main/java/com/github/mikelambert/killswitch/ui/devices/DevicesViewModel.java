package com.github.mikelambert.killswitch.ui.devices;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.mikelambert.killswitch.model.BluetoothToken;

public class DevicesViewModel extends ViewModel {

    private MutableLiveData<BluetoothToken> data;

    public DevicesViewModel() {
        data = new MutableLiveData<>();
    }

    public LiveData<BluetoothToken> getData() {
        return data;
    }
}