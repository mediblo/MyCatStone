package com.example.myapplication;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.util.Arrays;

public class CallingActivity extends AppCompatActivity {
    private static final String TAG = "AudioTest";
    private static final int    REQ_AUDIO = 123;

    private Socket     socket;
    private AudioRecord recorder;
    private boolean    isRecording = false;
    private final int  sampleRate  = 16000;
    private int        bufferSize;
    private TextView   tvTranscription;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_call);

        tvTranscription = findViewById(R.id.tvTranscription);

        // 1) Socket.IO 연결
        try {
            socket = IO.socket("http://mediblo.hopto.org:5000");
            socket.on("connect", args ->
                    Log.i(TAG, "Socket connected: " + socket.connected())
            );
            socket.on("transcription", onTranscription);
            socket.connect();
            Log.i(TAG, "Socket.IO connect() 호출");
        } catch (Exception e) {
            Log.e(TAG, "Socket.IO URL 구문 오류", e);
        }

        // 2) AudioRecord 최소 버퍼 사이즈 계산
        bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        Log.i(TAG, "계산된 버퍼 사이즈: " + bufferSize + " bytes");

        // 3) 권한 확인 및 녹음 스트리밍 시작
        ensureAudioPermissionAndStart();
    }

    private void ensureAudioPermissionAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "녹음 권한 이미 허용됨 → startStreaming()");
            startStreaming();
        } else {
            Log.w(TAG, "녹음 권한 없음 → 권한 요청");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                new AlertDialog.Builder(this)
                        .setTitle("마이크 권한 필요")
                        .setMessage("실시간 음성 전송을 위해 마이크 권한이 필요합니다.")
                        .setPositiveButton("허용", (dlg, which) ->
                                ActivityCompat.requestPermissions(
                                        this,
                                        new String[]{ Manifest.permission.RECORD_AUDIO },
                                        REQ_AUDIO
                                )
                        )
                        .setNegativeButton("취소", null)
                        .show();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{ Manifest.permission.RECORD_AUDIO },
                        REQ_AUDIO
                );
            }
        }
    }

    private void startStreaming() {
        // 재차 권한 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "startStreaming(): 권한 거부 상태");
            return;
        }

        // AudioRecord 초기화
        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );
        Log.i(TAG, "AudioRecord 생성, state=" + recorder.getState());

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord 초기화 실패: state=" + recorder.getState());
            return;
        }
        Log.i(TAG, "AudioRecord 초기화 성공");

        try {
            recorder.startRecording();
            Log.i(TAG, "startRecording() 호출");
        } catch (SecurityException se) {
            Log.e(TAG, "startRecording 보안 예외", se);
            return;
        }

        Log.i(TAG, "녹음 상태: " + recorder.getRecordingState());
        if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            Log.e(TAG, "녹음 시작 실패: recState=" + recorder.getRecordingState());
            return;
        }
        Log.i(TAG, "녹음 시작 성공");
        isRecording = true;

        // 녹음 스트리밍 스레드
        new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                int read = recorder.read(buffer, 0, buffer.length);
                Log.d(TAG, "read() 반환 바이트 수: " + read);
                if (read > 0) {
                    byte[] chunk = Arrays.copyOf(buffer, read);
                    socket.emit("audio", chunk);
                }
            }
            recorder.stop();
            recorder.release();
            Log.i(TAG, "녹음 및 스트리밍 스레드 종료");
        }).start();
    }

    // 서버 전사 결과 수신
    private final Emitter.Listener onTranscription = args -> {
        String text = (String) args[0];
        uiHandler.post(() -> {
            // append 한 번에 개행 포함
            tvTranscription.append(text + "\n");

            // 자동 스크롤
            int scroll = tvTranscription.getLayout().getLineTop(tvTranscription.getLineCount())
                    - tvTranscription.getHeight();
            if (scroll > 0) tvTranscription.scrollTo(0, scroll);
            else           tvTranscription.scrollTo(0, 0);
        });
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "사용자 권한 허용 → startStreaming()");
                startStreaming();
            } else {
                Log.w(TAG, "사용자 권한 거부");
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    Toast.makeText(this, "앱 설정에서 마이크 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null)
                    );
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRecording = false;
        if (socket != null) {
            socket.disconnect();
            socket.off("transcription", onTranscription);
            Log.i(TAG, "Socket.IO 연결 해제");
        }
    }
}
