package com.example.hod.staff;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.example.hod.models.Booking;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RequestDetailActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;

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

        mDatabase = FirebaseDatabase.getInstance().getReference();

        Booking booking = (Booking) getIntent().getSerializableExtra("booking");

        if (booking != null) {
            usernameTextView.setText("User: " + booking.getUserId());
            timeSlotTextView.setText("Slot: " + booking.getSlotId());
        }

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
            if (booking != null) {
                mDatabase.child("bookings").child(booking.getBookingId()).child("status").setValue("approved");
                Toast.makeText(RequestDetailActivity.this, "Request Approved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnReject.setOnClickListener(v -> {
            if (booking != null) {
                mDatabase.child("bookings").child(booking.getBookingId()).child("status").setValue("rejected");
                Toast.makeText(RequestDetailActivity.this, "Request Rejected", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnForward.setOnClickListener(v -> {
            if (booking != null) {
                mDatabase.child("bookings").child(booking.getBookingId()).child("status").setValue("forwarded");
                Toast.makeText(RequestDetailActivity.this, "Request Forwarded to HOD", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        Button btnViewLor = findViewById(R.id.btnViewLor);
        btnViewLor.setOnClickListener(v -> {
            Intent intent = new Intent(RequestDetailActivity.this, LorViewerActivity.class);
            startActivity(intent);
        });
    }
}
