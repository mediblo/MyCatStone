package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class CallActivity extends AppCompatActivity {

    Button[] buttons;
    ImageButton numberClear, callingBtn;
    TextView numberText;
    private final StringBuilder rawDigits = new StringBuilder();

    private MediaProjectionManager projectionManager;
    private String lastDialNumber = null;

    private ActivityResultLauncher<Intent> projectionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        projectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // 권한 결과를 서비스로 전달하여 서비스 시작
                        Intent serviceIntent = new Intent(this, AudioCaptureService.class);
                        serviceIntent.setAction(AudioCaptureService.ACTION_START);
                        serviceIntent.putExtra(AudioCaptureService.EXTRA_RESULT_CODE, result.getResultCode());
                        serviceIntent.putExtra(AudioCaptureService.EXTRA_DATA, result.getData());

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent);
                        } else {
                            startService(serviceIntent);
                        }

                        // 서비스 시작 후 전화 걸기
                        if (lastDialNumber != null) {
                            dialPhoneNumber(lastDialNumber);
                        }
                    } else {
                        Toast.makeText(this, "화면 녹화 권한을 허용해야 통화 자막 기능이 작동합니다.", Toast.LENGTH_LONG).show();
                    }
                });

        buttons = new Button[]{
                findViewById(R.id.oneBtn),
                findViewById(R.id.twoBtn),
                findViewById(R.id.threeBtn),
                findViewById(R.id.fourBtn),
                findViewById(R.id.fiveBtn),
                findViewById(R.id.sixBtn),
                findViewById(R.id.sevenBtn),
                findViewById(R.id.eightBtn),
                findViewById(R.id.nineBtn),
                findViewById(R.id.zeroBtn),
                findViewById(R.id.starBtn),
                findViewById(R.id.sharpBtn),
        };

        numberText = findViewById(R.id.numberText);
        numberClear = findViewById(R.id.numberClear);
        callingBtn = findViewById(R.id.callingBtn);

        numberClear.setEnabled(false);

        View.OnClickListener numberListener = v -> {
            String digit = ((Button) v).getText().toString();
            rawDigits.append(digit);
            numberText.setText(formatPhone(rawDigits.toString()));
            updateClearButtonState();
        };

        for (Button btn : buttons) {
            btn.setOnClickListener(numberListener);
        }

        numberClear.setOnClickListener(v -> {
            if (rawDigits.length() > 0) {
                rawDigits.deleteCharAt(rawDigits.length() - 1);
                numberText.setText(formatPhone(rawDigits.toString()));
                updateClearButtonState();
            }
        });

        numberClear.setOnLongClickListener(v -> {
            rawDigits.setLength(0);
            numberText.setText("");
            updateClearButtonState();
            return true;
        });

        callingBtn.setOnClickListener(v -> {
            if (rawDigits.length() == 0) {
                Toast.makeText(this, "전화번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            lastDialNumber = rawDigits.toString();
            requestMediaProjection();
        });
    }

    private String formatPhone(String digits) {
        if (digits.length() <= 3 || !digits.startsWith("010") || digits.length() > 11) return digits;
        if (digits.length() <= 7) return digits.substring(0, 3) + "-" + digits.substring(3);
        if (digits.length() == 10) return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
    }

    private void updateClearButtonState() {
        boolean hasText = numberText.getText().length() > 0;
        numberClear.setEnabled(hasText);
        numberClear.setVisibility(hasText ? View.VISIBLE : View.GONE);
    }

    private void requestMediaProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent intent = projectionManager.createScreenCaptureIntent();
            projectionLauncher.launch(intent);
        } else {
            Toast.makeText(this, "이 기능은 Android 10 이상에서만 지원됩니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void dialPhoneNumber(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }
}