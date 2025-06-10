package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.net.URI;
import java.util.Arrays;

import io.socket.client.IO;
import io.socket.client.Socket;

public class AudioCaptureService extends Service {

    private static final String TAG = "AudioCaptureService";
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    public static final String EXTRA_DATA = "EXTRA_DATA";

    private static final String CHANNEL_ID = "AudioCaptureChannel";
    private static final int NOTIFICATION_ID = 12345;
    private static final String SERVER_URL = "http://mediblo.hopto.org:5000"; // 서버 주소

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;
    private Thread recordingThread;

    private Socket mSocket;
    private Handler mainThreadHandler; // UI(Toast)를 위한 핸들러

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"1");
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mainThreadHandler = new Handler(Looper.getMainLooper()); // 메인 스레드 핸들러 초기화
        createNotificationChannel();
        initializeSocket();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
            Intent data = intent.getParcelableExtra(EXTRA_DATA);
            Log.d(TAG,"Test LOG "+String.valueOf(resultCode));
            Log.d(TAG,"Test LOG "+ (data == null));

            if (resultCode == -1 && data != null) {
                startForeground(NOTIFICATION_ID, createNotification());

                // 소켓 연결 시도
                mSocket.connect();
                mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection != null) {
                    Log.d(TAG, "MediaProjection 획득 성공");
                    startAudioCapture();
                } else {
                    Log.e(TAG, "MediaProjection 획득 실패");
                    stopSelf();
                }
            }
        } else if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAudioCapture();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void initializeSocket() {
        try {
            mSocket = IO.socket(URI.create(SERVER_URL));


            mSocket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "Socket.IO 서버에 연결되었습니다."));

            mSocket.on("transcription", args -> {
                if (args.length > 0 && args[0] instanceof String) {
                    String receivedText = (String) args[0];
                    Log.d(TAG, "수신된 자막: " + receivedText);

                    // 백그라운드 스레드에서 Toast를 띄우기 위해 핸들러 사용
                    mainThreadHandler.post(() -> {
                        Toast.makeText(getApplicationContext(), receivedText, Toast.LENGTH_SHORT).show();
                    });
                }
            });

            mSocket.on(Socket.EVENT_DISCONNECT, args -> Log.d(TAG, "Socket.IO 서버 연결이 끊어졌습니다."));

            mSocket.on(Socket.EVENT_CONNECT_ERROR, args -> Log.e(TAG, "Socket.IO 연결 오류: " + args[0]));

        } catch (Exception e) {
            Log.e(TAG, "소켓 초기화 중 오류 발생", e);
        }
    }

    private void startAudioCapture() {

        Log.d(TAG, "Test1");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .build();

        AudioFormat format = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(16000)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build();

        int bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        try {
            audioRecord = new AudioRecord.Builder()
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .setAudioPlaybackCaptureConfig(config)
                    .build();

            audioRecord.startRecording();
            Log.d(TAG, "오디오 캡처 시작됨");
            Toast.makeText(this, "통화 자막 기능이 시작되었습니다.", Toast.LENGTH_SHORT).show();

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[bufferSize];
                while (!Thread.currentThread().isInterrupted()) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        Log.d(TAG, "🔊 오디오 read 성공: " + read + "바이트");
                        byte[] chunk = Arrays.copyOf(buffer, read);
                        sendAudioToServer(chunk);
                    } else {
                        Log.w(TAG, "❌ 오디오 read 실패 or silence: " + read);
                    }

                }
            });
            recordingThread.start();

        } catch (SecurityException e) {
            Log.e(TAG, "녹음 권한 오류", e);
            Toast.makeText(this, "녹음 권한이 없습니다.", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    private void stopAudioCapture() {
        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }
        if (audioRecord != null) {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        Log.d(TAG, "오디오 캡처 중지됨");
    }

    private void sendAudioToServer(byte[] data) {
        if (mSocket != null && mSocket.connected()) {
            mSocket.emit("audio", data);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "통화 오디오 캡처 채널",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("통화 자막 활성화됨")
                .setContentText("통화 오디오를 캡처하고 있습니다.")
                // .setSmallIcon(R.drawable.ic_your_icon) // TODO: res/drawable에 적절한 아이콘을 추가하세요.
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAudioCapture();
        if (mSocket != null) {
            mSocket.off();
            mSocket.disconnect();
        }
    }
}