package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SettingActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;

    private final String[] requiredPermissions = new String[]{
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button requestButton = findViewById(R.id.settingChkBtn);

        requestButton.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                goToNextActivity();
            } else {
                ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_PERMISSIONS);
            }
        });
    }

    private boolean hasAllPermissions() {
        for (String permission : requiredPermissions) {
            int check = ContextCompat.checkSelfPermission(this, permission);
            if (check != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "권한 미허용: " + permission, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void goToNextActivity() {
        Toast.makeText(this, "모든 권한 허용됨. 다음 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(SettingActivity.this, CallActivity.class);
        startActivity(intent);
        finish(); // 뒤로가기 시 이 화면 안 나오게
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                goToNextActivity();
            } else {
                Toast.makeText(this, "모든 권한을 허용해야 다음 화면으로 이동할 수 있습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendAudioToServer(byte[] data) {
        // 이후 socket.emit("audio_chunk", data) 등으로 전송 예정
    }
}
