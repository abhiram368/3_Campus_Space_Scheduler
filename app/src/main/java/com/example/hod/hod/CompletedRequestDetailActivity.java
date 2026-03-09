package com.example.hod.hod;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.hod.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CompletedRequestDetailActivity extends AppCompatActivity {

    private MaterialButton btnCancelBooking;
    private MaterialButton btnViewDocuments;
    private TextView tvFinalStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_request_detail);

        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        btnViewDocuments = findViewById(R.id.btnViewDocuments);
        tvFinalStatus = findViewById(R.id.tvFinalStatus);

        // Show cancel button only if the request was approved
        if (tvFinalStatus.getText().toString().contains("Approved")) {
            btnCancelBooking.setVisibility(View.VISIBLE);
        } else {
            btnCancelBooking.setVisibility(View.GONE);
        }

        btnViewDocuments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CompletedRequestDetailActivity.this, "TODO: Implement document viewing", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCancelConfirmationDialog();
            }
        });
    }

    private void showCancelConfirmationDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Cancel Booking");
        builder.setMessage("Are you sure you want to cancel this booking?");

        final EditText input = new EditText(this);
        input.setHint("Enter remark (mandatory)");
        builder.setView(input);

        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String remark = input.getText().toString().trim();
                if (TextUtils.isEmpty(remark)) {
                    Toast.makeText(CompletedRequestDetailActivity.this, "Remark is mandatory to cancel.", Toast.LENGTH_SHORT).show();
                } else {
                    tvFinalStatus.setText("Final Decision: Cancelled");
                    tvFinalStatus.setTextColor(ContextCompat.getColor(CompletedRequestDetailActivity.this, R.color.status_booked));
                    btnCancelBooking.setVisibility(View.GONE);
                    Toast.makeText(CompletedRequestDetailActivity.this, "Booking Cancelled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Abort", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}