package com.example.labmanagementui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RequestDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);

        TextView usernameTextView = findViewById(R.id.usernameTextView);
        TextView timeSlotTextView = findViewById(R.id.timeSlotTextView);
        EditText remarkBox = findViewById(R.id.remarkBox);
        Button btnApprove = findViewById(R.id.btnApprove);
        Button btnReject = findViewById(R.id.btnReject);
        Button btnForward = findViewById(R.id.btnForward);

        // Sample data
        usernameTextView.setText(R.string.username_jane);
        timeSlotTextView.setText(R.string.timeslot_2_4);

        // Initially disabled
        btnReject.setEnabled(false);
        btnForward.setEnabled(false);

        remarkBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = !s.toString().trim().isEmpty();
                btnReject.setEnabled(hasText);
                btnForward.setEnabled(hasText);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnApprove.setOnClickListener(v -> {
            btnApprove.setText("Approved");
            btnApprove.setEnabled(false);
            Toast.makeText(RequestDetailActivity.this, "Request Approved", Toast.LENGTH_SHORT).show();
            // finishing activity to simulate removal from pending requests
            finish();
        });

        btnReject.setOnClickListener(v -> {
            Toast.makeText(RequestDetailActivity.this, "Request Rejected", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnForward.setOnClickListener(v -> {
            Toast.makeText(RequestDetailActivity.this, "Request Forwarded", Toast.LENGTH_SHORT).show();
            finish();
        });

        Button btnViewLor = findViewById(R.id.btnViewLor);

        btnViewLor.setOnClickListener(v -> {
            Intent intent = new Intent(
                    RequestDetailActivity.this,
                    LorViewerActivity.class
            );
            startActivity(intent);
        });


// Dummy student role flag
        boolean isStudent = true;



    }
}
