package com.example.campus_space_scheduler;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campus_space_scheduler.databinding.AActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {

    private AActivitySignUpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = AActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Button to trigger Forgot Password flow
        binding.btnGoToReset.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        // Navigation back to Login
        binding.btnBack.setOnClickListener(v -> finish());

        setupFooterText();
    }

    private void setupFooterText() {
        String text = "Already changed it? Sign In";
        SpannableString ss = new SpannableString(text);

        int start = text.indexOf("Sign In");
        int end = text.length();

        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#135bec")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        binding.tvSignInLink.setText(ss);
        binding.tvSignInLink.setOnClickListener(v -> finish());
    }
}