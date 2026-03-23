package com.example.campus_space_scheduler;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campus_space_scheduler.databinding.AActivityForgotPasswordBinding;
import com.example.campus_space_scheduler.helper.LogHelper;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private AActivityForgotPasswordBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = AActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            String email = auth.getCurrentUser().getEmail();

            if (email != null) {
                binding.etResetEmail.setText(email);
                binding.etResetEmail.setFocusable(false);
                binding.etResetEmail.setClickable(false);
            }
        }

        // Send Link Button
        binding.btnSendLink.setOnClickListener(v -> handlePasswordReset());

        // Back Buttons
        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void handlePasswordReset() {
        String email = binding.etResetEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase Service Call
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Email sent successfully
                        LogHelper.log("PASSWORD_RESET", "Password reset email sent to " + email);
                        Toast.makeText(this, "Reset link sent to your email!", Toast.LENGTH_LONG).show();
                        finish(); // Go back to login after success
                    } else {
                        // If email sending failed
                        LogHelper.log("PASSWORD_RESET_FAILED", "Failed to send password reset email to " + email);
                        String error = task.getException() != null ? task.getException().getMessage() : "Failed to send email";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}