package com.example.auxsound;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private Button btnAuto, btnManual, btnOff, btnOn, btnExit;
    private LinearLayout manualRow;

    private static final String PREFS = "aux_prefs";
    private static final String KEY_MODE = "mode";
    private static final String KEY_MANUAL_STATE = "manual_state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAuto = findViewById(R.id.btnAuto);
        btnManual = findViewById(R.id.btnManual);
        btnOff = findViewById(R.id.btnOff);
        btnOn = findViewById(R.id.btnOn);
        btnExit = findViewById(R.id.btnExit);
        manualRow = findViewById(R.id.manualRow);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        btnAuto.setOnClickListener(v -> setMode("auto"));
        btnManual.setOnClickListener(v -> setMode("manual"));
        btnOff.setOnClickListener(v -> setManualState("off"));
        btnOn.setOnClickListener(v -> setManualState("on"));
        btnExit.setOnClickListener(v -> exitApp());

        String savedMode = getPrefs().getString(KEY_MODE, "auto");
        String savedManualState = getPrefs().getString(KEY_MANUAL_STATE, "off");
        applyModeUI(savedMode);
        applyManualUI(savedManualState);

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

    private void setMode(String mode) {
        getPrefs().edit().putString(KEY_MODE, mode).apply();
        applyModeUI(mode);
        if (mode.equals("auto")) {
            startAuxService();
        } else {
            String manualState = getPrefs().getString(KEY_MANUAL_STATE, "off");
            applyManualUI(manualState);
            if (manualState.equals("on")) {
                startAuxService();
            } else {
                stopAuxService();
            }
        }
    }

    private void setManualState(String state) {
        getPrefs().edit().putString(KEY_MANUAL_STATE, state).apply();
        applyManualUI(state);
        if (state.equals("on")) {
            startAuxService();
        } else {
            stopAuxService();
        }
    }

    private void applyModeUI(String mode) {
        if (mode.equals("auto")) {
            btnAuto.setBackgroundColor(Color.parseColor("#4CAF50"));
            btnManual.setBackgroundColor(Color.parseColor("#DDDDDD"));
            manualRow.setVisibility(View.GONE);
        } else {
            btnManual.setBackgroundColor(Color.parseColor("#4CAF50"));
            btnAuto.setBackgroundColor(Color.parseColor("#DDDDDD"));
            manualRow.setVisibility(View.VISIBLE);
        }
    }

    private void applyManualUI(String state) {
        if (state.equals("on")) {
            btnOn.setBackgroundColor(Color.parseColor("#4CAF50"));
            btnOff.setBackgroundColor(Color.parseColor("#DDDDDD"));
        } else {
            btnOff.setBackgroundColor(Color.parseColor("#F44336"));
            btnOn.setBackgroundColor(Color.parseColor("#DDDDDD"));
        }
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
