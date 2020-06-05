package com.github.mikelambert.killswitch.ui.status;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.github.mikelambert.killswitch.R;

public class StatusFragment extends Fragment {

    private StatusViewModel statusViewModel;
    private ToggleButton toggleEngage;
    private ToggleButton toggleAdmin;
    private TextView statusAdmin;
    private TextView statusKillswitch;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        statusViewModel = ViewModelProviders.of(this).get(StatusViewModel.class);
        View root = inflater.inflate(R.layout.fragment_status, container, false);
        toggleEngage = root.findViewById(R.id.toggle_engage);
        toggleAdmin = root.findViewById(R.id.toggle_activate);
        statusAdmin = root.findViewById(R.id.status_admin);
        statusKillswitch = root.findViewById(R.id.status_killswitch);
        statusViewModel.getProducer().observe(getViewLifecycleOwner(), status -> {
            toggleEngage.setEnabled(status.isAdminActive());
            toggleEngage.setChecked(status.isKillswitchArmed());
            toggleAdmin.setChecked(status.isAdminActive());
            statusAdmin.setText(status.isAdminActive() ? R.string.label_admin_active : R.string.label_admin_inactive);
            statusKillswitch.setText(status.isKillswitchArmed() ? R.string.label_killswitch_engaged : R.string.label_killswitch_disarmed);
        });
        return root;
    }
}
