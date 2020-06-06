package com.github.mikelambert.killswitch.ui.devices;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.github.mikelambert.killswitch.R;

public class DevicesFragment extends Fragment {

    private DevicesViewModel devicesViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        devicesViewModel = ViewModelProviders.of(this).get(DevicesViewModel.class);
        View root = inflater.inflate(R.layout.fragment_devices, container, false);
        devicesViewModel.getData().observe(getViewLifecycleOwner(), data -> {

        });
        return root;
    }
}
