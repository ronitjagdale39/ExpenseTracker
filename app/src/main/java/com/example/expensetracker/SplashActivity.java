package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executor;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                // User is signed in, check biometrics
                boolean biometricEnabled = getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("biometric_enabled", false);
                if (biometricEnabled) {
                    showBiometricPrompt();
                } else {
                    navigateToMain();
                }
            } else {
                // No user is signed in
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }

        }, SPLASH_DELAY);
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(SplashActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                finish(); // Close app on error for security
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                navigateToMain();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for Expense Tracker")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password") // Use credential fallback if possible or just cancel
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void navigateToMain() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }
}
