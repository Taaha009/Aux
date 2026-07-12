package com.example.auxsound;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat switchMode, switchOnOff;
    private ImageButton btnExit;

    private static final String PREFS = "aux_prefs";
    private static final String KEY_MODE = "mode";
    private static final String KEY_MANUAL_STATE = "manual_state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchMode = findViewById(R.id.switchMode);
        switchOnOff = findViewById(R.id.switchOnOff);
        btnExit = findViewById(R.id.btnExit);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        btnExit.setOnClickListener(v -> exitApp());

        switchMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String mode = isChecked ? "manual" : "auto";
            getPrefs().edit().putString(KEY_MODE, mode).apply();
            applyModeUI(mode);
            if (mode.equals("auto")) {
                startAuxService();
            } else {
                String manualState = getPrefs().getString(KEY_MANUAL_STATE, "off");
                if (manualState.equals("on")) {
                    startAuxService();
                } else {
                    stopAuxService();
                }
            }
        });

        switchOnOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String state = isChecked ? "on" : "off";
            getPrefs().edit().putString(KEY_MANUAL_STATE, state).apply();
            if (state.equals("on")) {
                startAuxService();
            } else {
                stopAuxService();
            }
        });

        String savedMode = getPrefs().getString(KEY_MODE, "auto");
        String savedManualState = getPrefs().getString(KEY_MANUAL_STATE, "off");

        switchMode.setChecked(savedMode.equals("manual"));
        switchOnOff.setChecked(savedManualState.equals("on"));
        applyModeUI(savedMode);

        if (savedMode.equals("auto")) {
            startAuxService();
        } else if (savedManualState.equals("on")) {
            startAuxService();
        } else {
            stopAuxService();
        }
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private void applyModeUI(String mode) {
        findViewById(R.id.manualRow).setVisibility(mode.equals("manual") ? View.VISIBLE : View.GONE);
    }

    private void startAuxService() {
        Intent serviceIntent = new Intent(this, AuxDetectorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopAuxService() {
        Intent serviceIntent = new Intent(this, AuxDetectorService.class);
        stopService(serviceIntent);
    }

    private void exitApp() {
        stopAuxService();
        finishAffinity();
        System.exit(0);
    }
}
