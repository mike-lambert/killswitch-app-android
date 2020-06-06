package com.github.mikelambert.killswitch.ui.status;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.github.mikelambert.killswitch.Intents;
import com.github.mikelambert.killswitch.KillswitchApplication;
import com.github.mikelambert.killswitch.KillswitchDeviceAdministrator;
import com.github.mikelambert.killswitch.R;
import com.github.mikelambert.killswitch.event.KillswitchAdminStatus;
import com.github.mikelambert.killswitch.event.KillswitchArmedStatus;
import com.github.mikelambert.killswitch.model.KillswitchStatus;

import org.greenrobot.eventbus.Subscribe;

import static com.github.mikelambert.killswitch.Intents.FLAG_KILLSWITCH_TRIGGER_RED_BUTTON;

public class StatusFragment extends Fragment {
    public static final int REQUEST_CODE_INSTALL_ADMIN = 0x0000ADAD;

    private StatusViewModel statusViewModel;
    private ToggleButton toggleEngage;
    private ToggleButton toggleAdmin;
    private TextView statusAdmin;
    private TextView statusKillswitch;
    private KillswitchStatus lastStatus;
    private Button wipeButton;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        statusViewModel = ViewModelProviders.of(this).get(StatusViewModel.class);
        View root = inflater.inflate(R.layout.fragment_status, container, false);
        toggleEngage = root.findViewById(R.id.toggle_engage);
        toggleAdmin = root.findViewById(R.id.toggle_activate);
        statusAdmin = root.findViewById(R.id.status_admin);
        statusKillswitch = root.findViewById(R.id.status_killswitch);
        wipeButton = root.findViewById(R.id.button_wipe);
        statusViewModel.getProducer().observe(getViewLifecycleOwner(), status -> {
            lastStatus = status;
            toggleEngage.setEnabled(status.isAdminActive());
            toggleEngage.setChecked(status.isKillswitchArmed());
            toggleEngage.setTextColor(status.isAdminActive() ?
                            (status.isKillswitchArmed() ? colorResource(R.color.colorAccent) : Color.WHITE):
                    colorResource(android.R.color.tab_indicator_text)
            );

            toggleAdmin.setEnabled(!status.isKillswitchArmed());
            toggleAdmin.setChecked(status.isAdminActive());
            toggleAdmin.setTextColor(!status.isKillswitchArmed() ?
                    (status.isAdminActive() ? colorResource(R.color.colorAccent) : Color.WHITE):
                    colorResource(android.R.color.tab_indicator_text)
            );

            statusAdmin.setText(status.isAdminActive() ? R.string.label_admin_active : R.string.label_admin_inactive);
            statusAdmin.setTextColor(status.isAdminActive() ? colorResource(R.color.danger) : colorResource(R.color.color_ok));

            statusKillswitch.setText(status.isKillswitchArmed() ? R.string.label_killswitch_engaged : R.string.label_killswitch_disarmed);
            statusKillswitch.setTextColor(status.isKillswitchArmed() ? colorResource(R.color.danger) : colorResource(R.color.color_ok));

            wipeButton.setEnabled(status.isAdminActive() && status.isKillswitchArmed());
        });

        toggleAdmin.setOnClickListener(view -> {
            if (toggleAdmin.isChecked()) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, KillswitchApplication.getInstance(getActivity()).getKillswitch().getAdminComponentName());
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.explanation));
                startActivityForResult(intent, REQUEST_CODE_INSTALL_ADMIN);
            } else {
                Log.v("StatusFragment", "Revoking admin permissions");
                KillswitchApplication.getInstance(getActivity()).getKillswitch().disable();
            }
        });

        toggleEngage.setOnClickListener(view -> {
            if (toggleEngage.isChecked()) {
                Log.v("StatusFragment", "Sending ARMED intent");
                getActivity().sendBroadcast(Intents.createKillswitchArmedIntent());
            } else {
                Log.v("StatusFragment", "Sending DISARMED intent");
                getActivity().sendBroadcast(Intents.createKillswitchDisarmedIntent());
            }
        });

        wipeButton.setOnClickListener(view -> {
            KillswitchApplication.getInstance(getActivity()).getKillswitch().onTrigger(FLAG_KILLSWITCH_TRIGGER_RED_BUTTON);
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        KillswitchApplication.getEventBus(getActivity()).register(this);
        refreshState();
    }

    @Override
    public void onPause() {
        KillswitchApplication.getEventBus(getActivity()).unregister(this);
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_INSTALL_ADMIN) {
            refreshState();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Subscribe
    public void onArmedStateChanged(KillswitchArmedStatus status) {
        Log.v("StatusFragment", "EventBus: Armed - " + status.isValue());
        final boolean enabled = KillswitchApplication.getInstance(getActivity()).getKillswitch().isEnabled();
        KillswitchStatus data;
        if (lastStatus != null) {
            data = lastStatus;
            data.setKillswitchArmed(status.isValue());
        } else {
            data = new KillswitchStatus(enabled, status.isValue());
        }
        statusViewModel.post(data);
    }

    @Subscribe
    public void onDeviceAdministratorStateChanged(KillswitchAdminStatus status) {
        Log.v("StatusFragment", "EventBus: isAdmin - " + status.isValue());
        final boolean armed = KillswitchApplication.getInstance(getActivity()).getKillswitch().isArmed();
        KillswitchStatus data;
        if (lastStatus != null) {
            data = lastStatus;
            data.setAdminActive(status.isValue());
        } else {
            data = new KillswitchStatus(status.isValue(), armed);
        }
        statusViewModel.post(data);
    }

    private void refreshState() {
        final KillswitchDeviceAdministrator entrypoint = KillswitchApplication.getInstance(getActivity()).getKillswitch();
        final KillswitchStatus status = new KillswitchStatus(entrypoint.isEnabled(), entrypoint.isArmed());
        statusViewModel.post(status);
    }

    private int colorResource(int colorId) {
        return getResources().getColor(colorId, getActivity().getTheme());
    }
}
