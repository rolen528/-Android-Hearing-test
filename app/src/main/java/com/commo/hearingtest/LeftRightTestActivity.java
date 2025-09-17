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

import java.util.Random;

public class LeftRightTestActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private TextView tvInstructions;
    private TextView tvProgress;
    private Button btnLeft;
    private Button btnRight;
    private Button btnStartTest;
    private Button btnPlaySound;
    
    private AudioTrack audioTrack;
    private AudioManager audioManager;
    private boolean isTestRunning = false;
    private int currentTestNumber = 0;
    private int correctAnswers = 0;
    private int totalTests = 5;
    private boolean currentSoundIsLeft = false;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_left_right_test);

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
        tvProgress = findViewById(R.id.tv_progress);
        btnLeft = findViewById(R.id.btn_left);
        btnRight = findViewById(R.id.btn_right);
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
                playTestSound();
            }
        });

        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAnswer(true); // true for left
            }
        });

        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAnswer(false); // false for right
            }
        });
    }

    private void startTest() {
        isTestRunning = true;
        currentTestNumber = 0;
        correctAnswers = 0;
        nextTest();
        updateUI();
    }

    private void nextTest() {
        if (currentTestNumber < totalTests) {
            currentTestNumber++;
            currentSoundIsLeft = random.nextBoolean();
            tvInstructions.setText("테스트 " + currentTestNumber + "/" + totalTests + 
                    "\n소리 재생 버튼을 눌러 소리를 들어보세요.\n그 후 소리가 들린 방향을 선택하세요.");
            playTestSound();
        } else {
            finishTest();
        }
        updateUI();
    }

    private void playTestSound() {
        if (audioTrack == null) return;

        // Generate a test tone
        int duration = 1000; // 1 second
        int sampleRate = 44100;
        int frequency = 1000; // 1kHz test tone
        
        short[] buffer = generateTone(frequency, duration, sampleRate, currentSoundIsLeft);
        
        audioTrack.write(buffer, 0, buffer.length);
        audioTrack.play();
    }

    private short[] generateTone(int frequency, int duration, int sampleRate, boolean leftChannel) {
        int numSamples = duration * sampleRate / 1000;
        short[] buffer = new short[numSamples * 2]; // Stereo
        
        double phase = 0;
        double phaseIncrement = 2 * Math.PI * frequency / sampleRate;
        
        for (int i = 0; i < numSamples; i++) {
            short sample = (short) (Short.MAX_VALUE * Math.sin(phase) * 0.3); // 30% volume
            
            if (leftChannel) {
                buffer[i * 2] = sample;     // Left channel
                buffer[i * 2 + 1] = 0;      // Right channel (silent)
            } else {
                buffer[i * 2] = 0;          // Left channel (silent)
                buffer[i * 2 + 1] = sample; // Right channel
            }
            
            phase += phaseIncrement;
        }
        
        return buffer;
    }

    private void handleAnswer(boolean selectedLeft) {
        if (!isTestRunning) return;

        if (selectedLeft == currentSoundIsLeft) {
            correctAnswers++;
        }

        nextTest();
    }

    private void finishTest() {
        isTestRunning = false;
        double accuracy = (double) correctAnswers / totalTests * 100;
        
        Intent resultIntent = new Intent(this, TestResultActivity.class);
        resultIntent.putExtra("test_type", "좌우 청력 테스트");
        resultIntent.putExtra("accuracy", accuracy);
        resultIntent.putExtra("correct_answers", correctAnswers);
        resultIntent.putExtra("total_tests", totalTests);
        startActivity(resultIntent);
        finish();
    }

    private void updateUI() {
        if (isTestRunning) {
            btnStartTest.setVisibility(View.GONE);
            btnPlaySound.setVisibility(View.VISIBLE);
            btnLeft.setVisibility(View.VISIBLE);
            btnRight.setVisibility(View.VISIBLE);
            tvProgress.setText("진행률: " + currentTestNumber + "/" + totalTests);
        } else {
            btnStartTest.setVisibility(View.VISIBLE);
            btnPlaySound.setVisibility(View.GONE);
            btnLeft.setVisibility(View.GONE);
            btnRight.setVisibility(View.GONE);
            tvProgress.setText("");
            tvInstructions.setText("좌우 청력 테스트\n\n헤드폰이나 이어폰을 착용하고 테스트를 시작하세요.");
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