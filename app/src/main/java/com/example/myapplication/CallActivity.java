package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class CallActivity extends AppCompatActivity {

     Button[] buttons;
     ImageButton numberClear, callingBtn;
     TextView numberText;
    private StringBuilder rawDigits = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        buttons = new Button[] {
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

        // clearBtn 초기 비활성화
        numberClear.setEnabled(false);

        View.OnClickListener commonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // v는 눌린 버튼, 텍스트를 꺼내서 이어붙이기
                String digit = ((Button) v).getText().toString();
                rawDigits.append(digit);
                numberText.setText(formatPhone(rawDigits.toString()));
                updateClearButtonState();
            }
        };

        // 3) 배열을 돌며 리스너 달기
        for (Button btn : buttons) {
            btn.setOnClickListener(commonListener);
        }

        numberClear.setOnClickListener(v -> {
            if (rawDigits.length() > 0) {
                rawDigits.deleteCharAt(rawDigits.length() - 1);
                numberText.setText(formatPhone(rawDigits.toString()));
            }
            updateClearButtonState();
        });
        numberClear.setOnLongClickListener(v -> {
            rawDigits.setLength(0);
            numberText.setText("");
            updateClearButtonState();
            return true;
        });

        callingBtn.setOnClickListener(v -> {

        });
    }

    // 전화번호 포맷 + "010"으로 시작 아니면 포맷 건너뜀
    private String formatPhone(String digits) {
        if (digits.length() <= 3 || !digits.startsWith("010") || digits.length() > 11) { return digits; }
        if (digits.length() <= 7) { return digits.substring(0, 3) + "-" + digits.substring(3); }
        if (digits.length() == 10) { return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6); }
        return digits.substring(0, 3)
                + "-" + digits.substring(3, 7)
                + "-" + digits.substring(7);
    }

    private void updateClearButtonState() {
        boolean hasText = numberText.getText().length() > 0;
        numberClear.setEnabled(hasText);
        numberClear.setVisibility(hasText ? View.VISIBLE : View.GONE);
    }
}
