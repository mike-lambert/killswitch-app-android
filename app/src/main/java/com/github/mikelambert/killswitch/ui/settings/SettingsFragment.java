package com.github.mikelambert.killswitch.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.github.mikelambert.killswitch.KillswitchApplication;
import com.github.mikelambert.killswitch.R;
import com.github.mikelambert.killswitch.persistence.PersistentState;

import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;

public class SettingsFragment extends Fragment {

    private SettingsViewModel settingsViewModel;
    private EditText clicksCount;
    private CheckBox wipeSd;
    private CheckBox multiclick;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        settingsViewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        multiclick = root.findViewById(R.id.checkbox_multiclick);
        wipeSd = root.findViewById(R.id.checkbox_wipe_sd);
        clicksCount = root.findViewById(R.id.edit_clicks);
        settingsViewModel.getData().observe(getViewLifecycleOwner(), data -> {
            wipeSd.setChecked(data.isWipeSdCard());
            multiclick.setChecked(data.isActivateByMulticlick());
            clicksCount.setText(Integer.toString(data.getClicksCount()));
        });
        bindActions();
        settingsViewModel.post(KillswitchApplication.getInstance(getActivity()).getKillswitch().currentState());
        return root;
    }

    private void bindActions(){
        wipeSd.setOnClickListener(view -> {
            syncModel();
        });

        multiclick.setOnClickListener(view -> {
            syncModel();
        });

        clicksCount.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == IME_ACTION_DONE) {
                syncModel();
                return true;
            }
            return false;
        });
    }

    private void syncModel() {
        PersistentState current = KillswitchApplication.getInstance(getActivity()).getKillswitch().currentState();
        current.setWipeSdCard(wipeSd.isChecked());
        current.setActivateByMulticlick(multiclick.isChecked());
        current.setClicksCount(Integer.parseInt(clicksCount.getText().toString()));
        KillswitchApplication.getInstance(getActivity()).getKillswitch().onSettingsUpdated(current);
        settingsViewModel.post(current);
    }
}