package com.example.meditationhelper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;
import android.os.PowerManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleObserver;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.meditationhelper.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements LifecycleObserver {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private NumberPicker hoursPicker;
    private NumberPicker minutesPicker;
    private NumberPicker secondsPicker;
    private TextView stopwatchText;
    //    private ImageButton playButton;
    private Button playButton;
    private Button resetButton;
    private Handler handler = new Handler();
    private boolean isRunning = false;
    private boolean isPaused = false;
    private long pauseOffset = 0;
    private static long startTime = 0;
    private static long intervalMillis = 0;

    private static long nextTime = 0;
    private long nextInterval;

    private boolean isForeground = true;
    private static int noOfTimesToRepeat = 2;
    private boolean isTtsInitialized = false;

    private static final int REQUEST_ALARM_PERMISSION = 101; // Define a request code
    private boolean shouldSpeak = true;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
//                Toast.makeText(this, "Some features might benefit from disabling battery optimization. Go to Settings > Battery > (More apps) > All Apps and find this app to adjust its settings.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            }
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hoursPicker = findViewById(R.id.hoursPicker);
        minutesPicker = findViewById(R.id.minutesPicker);
        secondsPicker = findViewById(R.id.secondsPicker);
        stopwatchText = findViewById(R.id.stopwatchText);
        playButton = findViewById(R.id.playButton);
        resetButton = findViewById(R.id.resetButton);

        playButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.playButtonPlayColor));

        disableResetButton();

        setupNumberPickers();

        playButton.setOnClickListener(v -> {
            if (isRunning) {
                pauseStopwatch();
            } else {
                startStopwatch();
            }
        });


        getLifecycle().addObserver(this);
        resetButton.setOnClickListener(v -> resetStopwatch());

        // Start the Foreground Service
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

//        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MeditationHelper::StopwatchWakeLock");
    }

    private void setupNumberPickers() {
        hoursPicker.setMinValue(0);
        hoursPicker.setMaxValue(23);
        hoursPicker.setFormatter(i -> String.format("%02d", i));

        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(59);
        minutesPicker.setFormatter(i -> String.format("%02d", i));

        secondsPicker.setMinValue(0);
        secondsPicker.setMaxValue(59);
        secondsPicker.setFormatter(i -> String.format("%02d", i));
    }

    private void startStopwatch() {

        if (isPaused) {
            startTime = System.currentTimeMillis() - pauseOffset;
            isPaused = false;
        } else {
            startTime = System.currentTimeMillis();
//            nextTime = System.currentTimeMillis() + intervalMillis;
            intervalMillis = (hoursPicker.getValue() * 3600 + minutesPicker.getValue() * 60 + secondsPicker.getValue()) * 1000;
            nextInterval = intervalMillis;
        }
        nextTime = startTime + intervalMillis;
        nextTime = System.currentTimeMillis() + intervalMillis;
        playButton.setText("Pause");
        playButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.playButtonPauseColor));
        isRunning = true;
        handler.post(updateStopwatch);
        shouldSpeak = true;
        enableResetButton();

//        if (!wakeLock.isHeld()) {
//            wakeLock.acquire();
//        }
    }


    private void pauseStopwatch() {

//        playPauseButton.setImageResource(R.drawable.play_icon); // Replace with your play icon resource
        playButton.setText("Play");
        playButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.playButtonPlayColor));
        System.out.println(playButton.getBackground() + "HEllo");
        isRunning = false;
        isPaused = true;
        pauseOffset = System.currentTimeMillis() - startTime;
        handler.removeCallbacks(updateStopwatch);
        shouldSpeak = false;

//        if (wakeLock.isHeld()) {
//            wakeLock.release();
//        }
        stopTTSservice();
    }

    private void resetStopwatch() {
//        playPauseButton.setImageResource(R.drawable.play_icon); // Replace with your play icon resource
        playButton.setText("Play");
        playButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.playButtonPlayColor));
        isRunning = false;
        isPaused = false;
        shouldSpeak = false;
        pauseOffset = 0;
        stopwatchText.setText("00:00:00");
        handler.removeCallbacks(updateStopwatch);

        disableResetButton();
        nextTime = 0;

//        if (wakeLock.isHeld()) {
//            wakeLock.release();
//        }

        stopTTSservice();
    }


    private Runnable updateStopwatch = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                int hours = (int) (elapsedMillis / 3600000);
                int minutes = (int) (elapsedMillis % 3600000) / 60000;
                int seconds = (int) (elapsedMillis % 60000) / 1000;

                if (startTime + elapsedMillis >= nextTime) {
                    System.out.println("Inside tts intent if");
                    Intent serviceIntent = new Intent(MainActivity.this, TTSService.class);
                    serviceIntent.putExtra("startTime", startTime);
                    serviceIntent.putExtra("nextTime", nextTime);
                    MainActivity.this.startService(serviceIntent);
                    nextTime += intervalMillis;
                }

                if (isForeground) {
                    stopwatchText.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                }

                handler.postDelayed(this, 1000);
            }
        }
    };

    private void stopTTSservice() {
        Intent stopTTSIntent = new Intent(this, TTSService.class);
        stopService(stopTTSIntent);

    }

    private void enableResetButton() {
        resetButton.setEnabled(true);
        resetButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.resetButtonColor));
    }

    private void disableResetButton() {
        resetButton.setEnabled(false);
        resetButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.resetButtonColorDim));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
}