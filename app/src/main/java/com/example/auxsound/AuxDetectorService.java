package com.example.auxsound;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class AuxDetectorService extends Service {

    private static final int NOTIFICATION_ID = 101;
    private MediaPlayer mediaPlayer;
    private BroadcastReceiver headsetReceiver;

    private boolean lastKnownPlugged = false;
    private long lastPlaybackTime = 0;
    private static final long MIN_INTERVAL_MS = 2000;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, buildNotification());
        lastKnownPlugged = isHeadsetCurrentlyPlugged();
        registerHeadsetReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean isHeadsetCurrentlyPlugged() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                return true;
            }
        }
        return false;
    }

    private void registerHeadsetReceiver() {
        headsetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AudioManager.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                    int state = intent.getIntExtra("state", -1);
                    boolean nowPlugged = (state == 1);

                    if (nowPlugged && !lastKnownPlugged) {
                        long now = System.currentTimeMillis();
                        if (now - lastPlaybackTime > MIN_INTERVAL_MS) {
                            lastPlaybackTime = now;
                            playSound();
                        }
                    }

                    lastKnownPlugged = nowPlugged;
                }
            }
        };
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_HEADSET_PLUG);
        registerReceiver(headsetReceiver, filter);
    }

    private void playSound() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        mediaPlayer = MediaPlayer.create(
                this,
                R.raw.aux_sound,
                attributes,
                getSystemService(AudioManager.class).generateAudioSessionId()
        );

        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
            });
            mediaPlayer.start();
        }
    }

    private Notification buildNotification() {
        String channelId = "aux_detector_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Aux Detector", NotificationManager.IMPORTANCE_MIN);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("")
                .setContentText("")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (headsetReceiver != null) {
            unregisterReceiver(headsetReceiver);
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
