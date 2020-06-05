package com.github.mikelambert.killswitch.ui.status;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.mikelambert.killswitch.model.KillswitchStatus;

public class StatusViewModel extends ViewModel {

    private MutableLiveData<KillswitchStatus> statusData;

    public StatusViewModel() {
        statusData = new MutableLiveData<>();
        statusData.setValue(new KillswitchStatus());
    }

    public LiveData<KillswitchStatus> getProducer() {
        return statusData;
    }

    public void post(KillswitchStatus data){
        statusData.postValue(data);
    }
}