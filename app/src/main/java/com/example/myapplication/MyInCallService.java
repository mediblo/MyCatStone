// MyInCallService.java
package com.example.myapplication;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telecom.CallAudioState;
import android.telecom.InCallService;
import android.telecom.Call;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.net.URISyntaxException;
import java.util.Arrays;

public class MyInCallService extends InCallService {
    private static final String TAG = "InCallService";
    private static final String ACTION_PROJ = "com.example.myapplication.ACTION_MEDIA_PROJECTION";
    private Socket socket;
    private MediaProjection mediaProjection;
    private AudioRecord recorder;
    private boolean isRecording = false;
    private Thread recordThread;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private MediaProjectionManager mpManager;
    private int bufSize;

    private final BroadcastReceiver projReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            Intent data = intent.getParcelableExtra("data");
            mediaProjection = mpManager.getMediaProjection(resultCode, data);
            initAudioCapture();
            unregisterReceiver(this);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            socket = IO.socket("http://mediblo.hopto.org:5000");
            socket.connect();
            socket.on("transcription", onTranscription);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket init failed", e);
        }
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        AudioFocusRequest afr = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .build();
        am.requestAudioFocus(afr);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mpManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            registerReceiver(projReceiver, new IntentFilter(ACTION_PROJ));
        }
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState state) {
        super.onCallAudioStateChanged(state);
        if (!isRecording
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && state.getRoute() == CallAudioState.ROUTE_EARPIECE) {
            Intent i = new Intent(this, ProjectionRequestActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initAudioCapture() {
        AudioPlaybackCaptureConfiguration config =
                new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                        .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .build();
        bufSize = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        recorder = new AudioRecord.Builder()
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(16000)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(bufSize)
                .setAudioPlaybackCaptureConfig(config)
                .build();
        recorder.startRecording();
        isRecording = true;
        recordThread = new Thread(() -> {
            byte[] buffer = new byte[bufSize];
            while (isRecording) {
                int read = recorder.read(buffer, 0, buffer.length);
                if (read > 0) {
                    byte[] chunk = Arrays.copyOf(buffer, read);
                    socket.emit("audio", chunk);
                }
            }
            recorder.stop();
            recorder.release();
        }, "CallCaptureThread");
        recordThread.start();
    }

    private final Emitter.Listener onTranscription = args -> {
        String text = (String) args[0];
        uiHandler.post(() ->
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    };

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        isRecording = false;
        if (recordThread != null) recordThread.interrupt();
        if (socket != null) socket.disconnect();
    }
}