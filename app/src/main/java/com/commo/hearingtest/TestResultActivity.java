package com.commo.hearingtest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TestResultActivity extends AppCompatActivity {

    private TextView tvTestType;
    private TextView tvResults;
    private TextView tvAnalysis;
    private Button btnBackToMain;
    private Button btnRetakeTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_result);

        initializeViews();
        displayResults();
        setupClickListeners();
    }

    private void initializeViews() {
        tvTestType = findViewById(R.id.tv_test_type);
        tvResults = findViewById(R.id.tv_results);
        tvAnalysis = findViewById(R.id.tv_analysis);
        btnBackToMain = findViewById(R.id.btn_back_to_main);
        btnRetakeTest = findViewById(R.id.btn_retake_test);
    }

    private void displayResults() {
        Intent intent = getIntent();
        String testType = intent.getStringExtra("test_type");
        
        if (testType == null) {
            testType = "테스트";
        }
        
        tvTestType.setText(testType + " 결과");

        if ("좌우 청력 테스트".equals(testType)) {
            displayLeftRightResults(intent);
        } else if ("주파수 감도 테스트".equals(testType)) {
            displayFrequencyResults(intent);
        }
    }

    private void displayLeftRightResults(Intent intent) {
        double accuracy = intent.getDoubleExtra("accuracy", 0);
        int correctAnswers = intent.getIntExtra("correct_answers", 0);
        int totalTests = intent.getIntExtra("total_tests", 0);

        String results = String.format("정확도: %.1f%%\n정답: %d/%d", accuracy, correctAnswers, totalTests);
        tvResults.setText(results);

        String analysis;
        if (accuracy >= 80) {
            analysis = "우수한 좌우 청력 분별능력을 보여줍니다.\n좌우 귀의 청력이 균형적으로 잘 작동하고 있습니다.";
        } else if (accuracy >= 60) {
            analysis = "보통 수준의 좌우 청력 분별능력입니다.\n좀 더 집중해서 다시 테스트해보시거나\n헤드폰/이어폰 상태를 확인해보세요.";
        } else {
            analysis = "좌우 청력 분별에 어려움이 있을 수 있습니다.\n조용한 환경에서 다시 테스트하거나\n청력 전문의 상담을 권장합니다.";
        }
        
        tvAnalysis.setText(analysis);
    }

    private void displayFrequencyResults(Intent intent) {
        int optimalFrequency = intent.getIntExtra("optimal_frequency", -1);
        int lowestFrequency = intent.getIntExtra("lowest_frequency", -1);
        String analysis = intent.getStringExtra("analysis");

        StringBuilder results = new StringBuilder();
        if (optimalFrequency != -1) {
            results.append("최적 청취 주파수: ").append(optimalFrequency).append("Hz\n");
        }
        if (lowestFrequency != -1) {
            results.append("최저 감지 주파수: ").append(lowestFrequency).append("Hz");
        }
        
        if (results.length() == 0) {
            results.append("측정된 주파수가 없습니다.\n볼륨을 높이고 다시 시도해보세요.");
        }

        tvResults.setText(results.toString());
        tvAnalysis.setText(analysis != null ? analysis : "추가 분석 정보가 없습니다.");
    }

    private void setupClickListeners() {
        if (btnBackToMain != null) {
            btnBackToMain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TestResultActivity.this != null) {
                        Intent intent = new Intent(TestResultActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                }
            });
        }

        if (btnRetakeTest != null) {
            btnRetakeTest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TestResultActivity.this != null) {
                        String testType = getIntent().getStringExtra("test_type");
                        if (testType == null) {
                            testType = "";
                        }
                        
                        Class<?> activityClass;
                        
                        if ("좌우 청력 테스트".equals(testType)) {
                            activityClass = LeftRightTestActivity.class;
                        } else {
                            activityClass = FrequencyTestActivity.class;
                        }
                        
                        Intent intent = new Intent(TestResultActivity.this, activityClass);
                        startActivity(intent);
                        finish();
                    }
                }
            });
        }
    }
}