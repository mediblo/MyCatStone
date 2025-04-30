package com.example.myapplication;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.text.TextWatcher;
import android.widget.Toast;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.Manifest;

public class RegisterActivity2 extends AppCompatActivity {
    private static final int REQ_READ_PERMISSION = 100;
    private View selector;
    private Button publicBtn, privateBtn, registerBtn;
    private EditText editName, editMessage;
    private ImageButton nameClear, msgClear, userImage;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register2);

        selector = findViewById(R.id.selector);
        publicBtn  = findViewById(R.id.publicBtn);
        privateBtn = findViewById(R.id.privateBtn);
        editName  = findViewById(R.id.editName);
        editMessage  = findViewById(R.id.editMessage);
        nameClear = findViewById(R.id.name_clear);
        msgClear = findViewById(R.id.msg_clear);
        registerBtn = findViewById(R.id.registerBtn);
        userImage = findViewById(R.id.userImage);

        // 초기 설정
        selectButton(true, false);
        registerBtn.setEnabled(false);
        registerBtn.setBackgroundResource(R.drawable.rounded_btn_gray_true);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri selectedUri = result.getData().getData();
                            if (selectedUri != null) {
                                // ImageButton에 선택된 이미지 세팅
                                userImage.setImageURI(selectedUri);
                                // 배경도 바꾸고 싶으면:
                                userImage.setBackgroundResource(R.drawable.circle_bg);
                            }
                        }
                    }
                }
        );

        userImage.setOnClickListener(v -> {
            if (checkReadPermission()) {
                launchImagePicker();
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                updateSubmitButtonState();
            }
        };
        editName.addTextChangedListener(watcher);
        editMessage.addTextChangedListener(watcher);

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity2.this, CallActivity.class);
                startActivity(intent);
            }
        });

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
        editName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                nameClear.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            }
        });
        editMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                msgClear.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            }
        });
        nameClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editName.setText("");
            }
        });
        msgClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editMessage.setText("");
            }
        });

        editName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                // 포커스가 있을 때만, 텍스트가 있을 때만 버튼 보이기
                if (editName.isFocused()) {
                    nameClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        editMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                // 포커스가 있을 때만, 텍스트가 있을 때만 버튼 보이기
                if (editMessage.isFocused()) {
                    msgClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
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
        int selectedColor   = getResources().getColor(R.color.black);
        int unselectedColor = getResources().getColor(R.color.deActivate);
        Drawable selectedBG = getDrawable(R.drawable.rounded_btn);
        Drawable unselectedBG = getDrawable(R.drawable.rounded_btn_gray_true);

        publicBtn.setTextColor(isLeft ? selectedColor : unselectedColor);
        publicBtn.setBackground(isLeft ? selectedBG : unselectedBG);
        privateBtn.setTextColor(!isLeft ? selectedColor : unselectedColor);
        privateBtn.setBackground(!isLeft ? selectedBG : unselectedBG);
    }

    // 회원가입 버튼 토글 함수
    private void updateSubmitButtonState() {
        String name    = editName.getText().toString().trim();
        String message = editMessage.getText().toString().trim();
        int selectedColor   = getResources().getColor(R.color.black);
        int unselectedColor = getResources().getColor(R.color.deActivate);

        boolean enable = !name.isEmpty() && !message.isEmpty();
        registerBtn.setEnabled(enable);
        registerBtn.setBackgroundResource(
                enable ? R.drawable.rounded_btn : R.drawable.rounded_btn_gray_true);
        registerBtn.setTextColor(enable ? selectedColor : unselectedColor);
    }

    private boolean checkReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{ Manifest.permission.READ_MEDIA_IMAGES },
                        REQ_READ_PERMISSION);
                return false;
            }
        } else {
            // Android 12- 이하
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                        REQ_READ_PERMISSION);
                return false;
            }
        }
        return true;
    }

    // 실제 갤러리 호출
    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                Toast.makeText(this, "이미지 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
