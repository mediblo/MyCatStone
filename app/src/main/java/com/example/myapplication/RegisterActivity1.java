package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity1 extends AppCompatActivity {

    Button register_btn_true, register_btn_anony;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register1); // 스플래시 레이아웃 설정

        register_btn_true = findViewById(R.id.register_btn_true);
        register_btn_anony = findViewById(R.id.register_btn_anony);
        register_btn_true.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity1.this, RegisterActivity2.class);
                startActivity(intent);
            }
        });

        register_btn_anony.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity1.this, CallActivity.class);
                startActivity(intent);
            }
        });
    }
}
