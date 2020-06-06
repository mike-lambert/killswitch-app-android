package com.github.mikelambert.killswitch.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.mikelambert.killswitch.persistence.PersistentState;

public class SettingsViewModel extends ViewModel {
    private final MutableLiveData<PersistentState> data;

    public SettingsViewModel(){
        data = new MutableLiveData<>();
    }

    public LiveData<PersistentState> getData() {
        return data;
    }

    public void post(PersistentState data){
        this.data.postValue(data);
    }
}
