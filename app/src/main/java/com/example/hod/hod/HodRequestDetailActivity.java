package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.example.hod.models.Booking;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;

public class HodRequestDetailActivity extends AppCompatActivity {

    private EditText etHodRemark;
    private Button btnApprove;
    private Button btnReject;
    private Button btnViewDocuments;
    private ProgressBar progressBar;

    private FirebaseRepository repository;
    private Booking booking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_request_detail);

        etHodRemark = findViewById(R.id.etHodRemark);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        btnViewDocuments = findViewById(R.id.btnViewDocuments);
        progressBar = findViewById(R.id.progressBar);

        repository = new FirebaseRepository();
        booking = (Booking) getIntent().getSerializableExtra("booking");

        btnApprove.setOnClickListener(v -> updateApproval(true));

        btnReject.setOnClickListener(v -> {
            String remark = etHodRemark.getText().toString().trim();
            if (TextUtils.isEmpty(remark)) {
                etHodRemark.setError("Remark is mandatory for rejection.");
            } else {
                updateApproval(false);
            }
        });

        btnViewDocuments.setOnClickListener(v ->
                startActivity(new Intent(HodRequestDetailActivity.this, ViewDocumentActivity.class)));
    }

    private void updateApproval(boolean approved) {
        if (booking == null || booking.getBookingId() == null) {
            Toast.makeText(this, "Booking information unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        String remark = etHodRemark.getText().toString().trim();
        setLoading(true);
        repository.updateApprovalStatus(booking.getBookingId(), "hod", approved, remark, result -> runOnUiThread(() -> {
            setLoading(false);
            if (result instanceof Result.Success) {
                Toast.makeText(this, approved ? "Request Approved" : "Request Rejected", Toast.LENGTH_SHORT).show();
                finish();
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Failed to update approval.", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        btnApprove.setEnabled(!loading);
        btnReject.setEnabled(!loading);
        btnViewDocuments.setEnabled(!loading);
    }
}
