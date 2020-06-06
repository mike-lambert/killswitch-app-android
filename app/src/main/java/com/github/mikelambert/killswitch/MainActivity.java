package com.github.mikelambert.killswitch;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import static com.github.mikelambert.killswitch.ui.status.StatusFragment.REQUEST_CODE_KEYGUARD;

public class MainActivity extends AppCompatActivity {

    private boolean authenticated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("Main", "Resumed. Checking secure status");
        final KeyguardManager keyguardManager = (KeyguardManager) this.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        if (KillswitchApplication.getInstance(this).getKillswitch().isArmed() && !authenticated) {
            Log.v("Main", " armed, checking credentials");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (keyguardManager.isDeviceLocked()){
                    Log.v("Main", " device locked;hiding app");
                    moveTaskToBack(true);
                    return;
                }
                Intent k = keyguardManager.createConfirmDeviceCredentialIntent("Killswitch", "Secure Killswitch disarming");
                Log.v("Main", "  intent: " + k);
                if (k != null) {
                    Log.v("Main", "   starting keyguard");
                    startActivityForResult(k, REQUEST_CODE_KEYGUARD);
                }
            }
        } else {
            Log.v("Main", " authenticated or not armed ...");
        }
        authenticated = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_KEYGUARD) {
            if (RESULT_OK != resultCode) {
                Log.v("Main", "KEYGUARD FAILED");
                finish();
            } else {
                Log.v("Main", "KEYGUARD SUCCEEDED");
                authenticated = true;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
