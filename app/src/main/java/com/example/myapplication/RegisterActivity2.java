package com.example.myapplication;

import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity2 extends AppCompatActivity {
    private View selector;
    private Button publicBtn;
    private Button privateBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register2);

        selector = findViewById(R.id.selector);
        publicBtn  = findViewById(R.id.publicBtn);
        privateBtn = findViewById(R.id.privateBtn);

        // 초기 선택: 왼쪽
        selectButton(true, false);

        publicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectButton(true, true);
            }
        });
        privateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectButton(false, true);
            }
        });
    }

    /**
     * @param isLeft  true면 왼쪽, false면 오른쪽
     * @param animate true면 애니메이션 적용, false면 즉시 이동
     */
    private void selectButton(boolean isLeft, boolean animate) {
        float targetX = isLeft ? 0f : selector.getWidth();

        if (animate) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(selector, "x", targetX);
            animator.setDuration(250);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.start();
        } else {
            selector.setX(targetX);
        }

        // 버튼 텍스트 색 업데이트
        int selectedColor   = getResources().getColor(R.color.white);
        int unselectedColor = getResources().getColor(R.color.deActivate);
        Drawable selectedBG = getDrawable(R.drawable.rounded_btn);
        Drawable unselectedBG = getDrawable(R.drawable.rounded_btn_gray_true);

        publicBtn.setTextColor(isLeft ? selectedColor : unselectedColor);
        publicBtn.setBackground(isLeft ? selectedBG : unselectedBG);
        privateBtn.setTextColor(!isLeft ? selectedColor : unselectedColor);
        privateBtn.setBackground(!isLeft ? selectedBG : unselectedBG);
    }
}
