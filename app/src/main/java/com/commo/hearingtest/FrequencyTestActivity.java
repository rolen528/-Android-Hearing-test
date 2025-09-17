package com.commo.hearingtest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class FrequencyTestActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1002;

    private TextView tvInstructions;
    private TextView tvCurrentFrequency;
    private TextView tvProgress;
    private Button btnCannotHear;
    private Button btnHearFaintly;
    private Button btnHearClearly;
    private Button btnStartTest;
    private Button btnPlaySound;
    
    private AudioTrack audioTrack;
    private AudioManager audioManager;
    private boolean isTestRunning = false;
    private int currentFrequency;
    private int[] testFrequencies = {8000, 6000, 4000, 2000, 1000, 500, 250, 125}; // Hz
    private int currentFrequencyIndex = 0;
    private int lowestHeardFrequency = -1;
    private int optimalFrequency = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frequency_test);

        initializeViews();
        setupClickListeners();
        
        if (checkAudioPermissions()) {
            initializeAudio();
        } else {
            requestAudioPermissions();
        }
        
        updateUI();
    }

    private boolean checkAudioPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeAudio();
            } else {
                Toast.makeText(this, "오디오 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeViews() {
        tvInstructions = findViewById(R.id.tv_instructions);
        tvCurrentFrequency = findViewById(R.id.tv_current_frequency);
        tvProgress = findViewById(R.id.tv_progress);
        btnCannotHear = findViewById(R.id.btn_cannot_hear);
        btnHearFaintly = findViewById(R.id.btn_hear_faintly);
        btnHearClearly = findViewById(R.id.btn_hear_clearly);
        btnStartTest = findViewById(R.id.btn_start_test);
        btnPlaySound = findViewById(R.id.btn_play_sound);
    }

    private void initializeAudio() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        audioTrack = new AudioTrack(audioAttributes,
                new AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build(),
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    private void setupClickListeners() {
        btnStartTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTest();
            }
        });

        btnPlaySound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playCurrentFrequency();
            }
        });

        btnCannotHear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleResponse(0); // Cannot hear
            }
        });

        btnHearFaintly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleResponse(1); // Hear faintly
            }
        });

        btnHearClearly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleResponse(2); // Hear clearly
            }
        });
    }

    private void startTest() {
        isTestRunning = true;
        currentFrequencyIndex = 0;
        currentFrequency = testFrequencies[currentFrequencyIndex];
        lowestHeardFrequency = -1;
        optimalFrequency = -1;
        
        nextFrequency();
        updateUI();
    }

    private void nextFrequency() {
        if (currentFrequencyIndex < testFrequencies.length) {
            currentFrequency = testFrequencies[currentFrequencyIndex];
            tvInstructions.setText("현재 주파수: " + currentFrequency + "Hz\n" +
                    "소리 재생 버튼을 눌러 테스트음을 들어보세요.\n" +
                    "아래 버튼 중 해당하는 것을 선택하세요.");
            tvCurrentFrequency.setText(currentFrequency + " Hz");
            playCurrentFrequency();
        } else {
            finishTest();
        }
        updateUI();
    }

    private void playCurrentFrequency() {
        if (audioTrack == null) return;

        // Generate test tone at current frequency
        int duration = 2000; // 2 seconds
        int sampleRate = 44100;
        
        short[] buffer = generateTone(currentFrequency, duration, sampleRate);
        
        audioTrack.write(buffer, 0, buffer.length);
        audioTrack.play();
    }

    private short[] generateTone(int frequency, int duration, int sampleRate) {
        int numSamples = duration * sampleRate / 1000;
        short[] buffer = new short[numSamples * 2]; // Stereo
        
        double phase = 0;
        double phaseIncrement = 2 * Math.PI * frequency / sampleRate;
        
        for (int i = 0; i < numSamples; i++) {
            short sample = (short) (Short.MAX_VALUE * Math.sin(phase) * 0.4); // 40% volume
            
            buffer[i * 2] = sample;     // Left channel
            buffer[i * 2 + 1] = sample; // Right channel
            
            phase += phaseIncrement;
        }
        
        return buffer;
    }

    private void handleResponse(int response) {
        if (!isTestRunning) return;

        // Record responses for analysis
        switch (response) {
            case 0: // Cannot hear
                // Continue to next frequency
                break;
            case 1: // Hear faintly
                if (lowestHeardFrequency == -1) {
                    lowestHeardFrequency = currentFrequency;
                }
                break;
            case 2: // Hear clearly
                if (optimalFrequency == -1) {
                    optimalFrequency = currentFrequency;
                }
                if (lowestHeardFrequency == -1) {
                    lowestHeardFrequency = currentFrequency;
                }
                break;
        }

        currentFrequencyIndex++;
        nextFrequency();
    }

    private void finishTest() {
        isTestRunning = false;
        
        // Analyze results
        String analysis = analyzeResults();
        
        Intent resultIntent = new Intent(this, TestResultActivity.class);
        resultIntent.putExtra("test_type", "주파수 감도 테스트");
        resultIntent.putExtra("optimal_frequency", optimalFrequency);
        resultIntent.putExtra("lowest_frequency", lowestHeardFrequency);
        resultIntent.putExtra("analysis", analysis);
        startActivity(resultIntent);
        finish();
    }

    private String analyzeResults() {
        StringBuilder analysis = new StringBuilder();
        
        if (optimalFrequency != -1) {
            analysis.append("최적 주파수: ").append(optimalFrequency).append("Hz\n");
        } else {
            analysis.append("최적 주파수: 감지되지 않음\n");
        }
        
        if (lowestHeardFrequency != -1) {
            analysis.append("감지 가능한 최저 주파수: ").append(lowestHeardFrequency).append("Hz\n");
        } else {
            analysis.append("감지 가능한 주파수: 없음\n");
        }
        
        // Add age-based analysis
        if (lowestHeardFrequency >= 4000) {
            analysis.append("\n분석: 젊은 연령대의 정상적인 청력 범위입니다.");
        } else if (lowestHeardFrequency >= 2000) {
            analysis.append("\n분석: 성인 평균 수준의 청력입니다.");
        } else if (lowestHeardFrequency >= 1000) {
            analysis.append("\n분석: 고주파수 청력이 약간 감소되었을 수 있습니다.");
        } else {
            analysis.append("\n분석: 전반적인 청력 검진을 권장합니다.");
        }
        
        return analysis.toString();
    }

    private void updateUI() {
        if (isTestRunning) {
            btnStartTest.setVisibility(View.GONE);
            btnPlaySound.setVisibility(View.VISIBLE);
            btnCannotHear.setVisibility(View.VISIBLE);
            btnHearFaintly.setVisibility(View.VISIBLE);
            btnHearClearly.setVisibility(View.VISIBLE);
            tvProgress.setText("진행률: " + (currentFrequencyIndex + 1) + "/" + testFrequencies.length);
            tvCurrentFrequency.setVisibility(View.VISIBLE);
        } else {
            btnStartTest.setVisibility(View.VISIBLE);
            btnPlaySound.setVisibility(View.GONE);
            btnCannotHear.setVisibility(View.GONE);
            btnHearFaintly.setVisibility(View.GONE);
            btnHearClearly.setVisibility(View.GONE);
            tvProgress.setText("");
            tvCurrentFrequency.setVisibility(View.GONE);
            tvInstructions.setText("주파수 감도 테스트\n\n연령대별 평균 주파수에서 시작하여\n점차 낮은 주파수로 테스트합니다.\n\n헤드폰이나 이어폰을 착용하고 테스트를 시작하세요.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }
    }
}