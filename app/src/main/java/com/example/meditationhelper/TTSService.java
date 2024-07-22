package com.example.meditationhelper;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.core.content.ContextCompat;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;

public class TTSService extends Service {
    private TextToSpeech tts;
    private int noOfTimesToRepeat = 1;

    private String textToSpeak = "";

    private final BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_STOP_TTS_SERVICE".equals(intent.getAction())) {
                unregisterReceiver(stopServiceReceiver);
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter("ACTION_STOP_TTS_SERVICE");
//        registerReceiver(stopServiceReceiver, filter);
        ContextCompat.registerReceiver(this, stopServiceReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        System.out.println("inside onstartcommand of ttsservice");
//        String textToSpeak = intent.getStringExtra("text");
//        SharedPreferences preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
//        long startTime = preferences.getLong("start_time", 0);
//        long intervalMillis = preferences.getLong("interval_millis", 0);
////        nextTime = intent.getLongExtra("next_time", 0);
//        long nextTime = preferences.getLong("next_time", 0) - intervalMillis;

        System.out.println("Inside onstartCommand");
        long startTime = intent.getLongExtra("startTime", 0);
        long nextTime = intent.getLongExtra("nextTime", 0);

        long elapsedMillis = nextTime - startTime;
        int hours = (int) (elapsedMillis / 3600000);
        int minutes = (int) (elapsedMillis % 3600000) / 60000;
        int seconds = (int) (elapsedMillis % 60000) / 1000;
//        System.out.println("Inside TTS Service");
//        System.out.println(nextTime + " : " + startTime + " : " + intervalMillis);
//        System.out.println(hours + " : " + minutes + " : " + seconds);
//        System.out.println();

        LocalTime localTime = Instant.ofEpochMilli(System.currentTimeMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalTime();

        int curHours = localTime.getHour();
        int curMinutes = localTime.getMinute();
        int curSeconds = localTime.getSecond();
        long endTime = System.nanoTime();

        textToSpeak = "";
        if (hours > 0)
            textToSpeak += hours + " hours";
        if (minutes > 0)
            textToSpeak += minutes + " minutes";
        if (seconds > 0)
            textToSpeak += seconds + " seconds";
        System.out.println(textToSpeak);


        if (!textToSpeak.isEmpty()) {
            textToSpeak += " elapsed\nCurrent Time " + curHours  +  " hour " + curMinutes + " minute " + curSeconds + " second";
        }

        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
                tts.setSpeechRate(0.75f);
                speakText(textToSpeak);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        // No action needed on start
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        tts.shutdown();
                        stopSelf();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        tts.shutdown();
                        stopSelf();
                    }
                });
            }
        });

        return START_STICKY;
    }

    private void speakText(String textToSpeak) {
        for (int i = 0; i < noOfTimesToRepeat; ++i) {
            tts.speak(textToSpeak, TextToSpeech.QUEUE_ADD, null, null);
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
//        unregisterReceiver(stopServiceReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
