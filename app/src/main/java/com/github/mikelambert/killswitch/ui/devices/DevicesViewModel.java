package com.github.mikelambert.killswitch.ui.devices;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.mikelambert.killswitch.common.HardwareCircuit;

public class DevicesViewModel extends ViewModel {

    private MutableLiveData<HardwareCircuit> data;

    public DevicesViewModel() {
        data = new MutableLiveData<>();
    }

    public LiveData<HardwareCircuit> getData() {
        return data;
    }

    public void post(HardwareCircuit token){
        data.postValue(token);
    }
}