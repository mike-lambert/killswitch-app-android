package com.github.mikelambert.killswitch.ui.devices;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.mikelambert.killswitch.model.HardwareToken;

public class DevicesViewModel extends ViewModel {

    private MutableLiveData<HardwareToken> data;

    public DevicesViewModel() {
        data = new MutableLiveData<>();
    }

    public LiveData<HardwareToken> getData() {
        return data;
    }
}