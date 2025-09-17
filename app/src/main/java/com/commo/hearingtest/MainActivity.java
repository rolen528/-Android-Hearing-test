package com.commo.hearingtest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnLeftRightTest;
    private Button btnFrequencyTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btnLeftRightTest = findViewById(R.id.btn_left_right_test);
        btnFrequencyTest = findViewById(R.id.btn_frequency_test);
    }

    private void setupClickListeners() {
        btnLeftRightTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LeftRightTestActivity.class);
                startActivity(intent);
            }
        });

        btnFrequencyTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FrequencyTestActivity.class);
                startActivity(intent);
            }
        });
    }
}