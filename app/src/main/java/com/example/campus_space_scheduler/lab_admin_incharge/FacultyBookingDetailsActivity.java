package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_space_scheduler.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FacultyBookingDetailsActivity extends AppCompatActivity {

    private TextView tvSpaceName, tvUserName, tvDate, tvTime, tvPurpose, tvDescription, tvStatus, tvRemarks, tvLabelStatus, tvLabelRemarks;
    private MaterialButton btnApprove, btnReject, btnForward, btnViewLor, btnCancelApprovedBooking;
    private LinearLayout footerButtons;
    private DatabaseReference bookingsRef, usersRef, schedulesRef;
    private String bookingId, scheduleId, slotStart;
    private String lorUrl;
    private String labName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_booking_details);

        tvSpaceName = findViewById(R.id.facultyDetailsSpaceName);
        tvUserName = findViewById(R.id.facultyDetailsUserName);
        tvDate = findViewById(R.id.facultyDetailsDate);
        tvTime = findViewById(R.id.facultyDetailsTime);
        tvPurpose = findViewById(R.id.facultyDetailsPurpose);
        tvDescription = findViewById(R.id.facultyDetailsDescription);
        tvStatus = findViewById(R.id.facultyDetailsStatus);
        tvRemarks = findViewById(R.id.facultyDetailsRemarks);
        tvLabelStatus = findViewById(R.id.facultyLabelStatus);
        tvLabelRemarks = findViewById(R.id.facultyLabelRemarks);
        
        btnApprove = findViewById(R.id.btnFacultyApproveDetail);
        btnForward = findViewById(R.id.btnFacultyForwardDetail);
        btnReject = findViewById(R.id.btnFacultyRejectDetail);
        btnViewLor = findViewById(R.id.btnFacultyViewLor);
        btnCancelApprovedBooking = findViewById(R.id.btnFacultyCancelApprovedBooking);
        footerButtons = findViewById(R.id.facultyFooterButtons);

        bookingId = getIntent().getStringExtra("bookingId");
        labName = getIntent().getStringExtra("labName");
        
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");

        if (bookingId != null) {
            loadBookingDetails();
        }

        btnApprove.setOnClickListener(v -> updateBooking("Approved", true, null));
        btnForward.setOnClickListener(v -> updateBooking("forwarded_to_hod", true, null));
        btnReject.setOnClickListener(v -> showRemarksDialog("Rejected", true));
        btnViewLor.setOnClickListener(v -> viewLor());
        btnCancelApprovedBooking.setOnClickListener(v -> handleCancellation());
    }

    private void loadBookingDetails() {
        bookingsRef.child(bookingId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String dateStr = String.valueOf(snapshot.child("date").getValue());
                    tvSpaceName.setText(String.valueOf(snapshot.child("spaceName").getValue()));
                    tvDate.setText(dateStr);
                    tvTime.setText(String.valueOf(snapshot.child("timeSlot").getValue()));
                    tvPurpose.setText(String.valueOf(snapshot.child("purpose").getValue()));
                    tvDescription.setText(String.valueOf(snapshot.child("description").getValue()));
                    lorUrl = String.valueOf(snapshot.child("lorUpload").getValue());
                    scheduleId = String.valueOf(snapshot.child("scheduleId").getValue());
                    slotStart = String.valueOf(snapshot.child("slotStart").getValue());

                    String currentStatus = String.valueOf(snapshot.child("status").getValue());

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String todayStr = sdf.format(new Date());
                    boolean isOutdated = dateStr != null && dateStr.compareTo(todayStr) < 0;

                    if ("Approved".equalsIgnoreCase(currentStatus)) {
                        footerButtons.setVisibility(View.GONE);
                        btnCancelApprovedBooking.setVisibility(isOutdated ? View.GONE : View.VISIBLE);
                        tvLabelStatus.setVisibility(View.VISIBLE);
                        tvStatus.setVisibility(View.VISIBLE);
                        tvStatus.setText("APPROVED");
                    } else if ("Rejected".equalsIgnoreCase(currentStatus) || "Cancelled".equalsIgnoreCase(currentStatus) || "forwarded_to_hod".equalsIgnoreCase(currentStatus)) {
                        footerButtons.setVisibility(View.GONE);
                        btnCancelApprovedBooking.setVisibility(View.GONE);
                        tvLabelStatus.setVisibility(View.VISIBLE);
                        tvStatus.setVisibility(View.VISIBLE);
                        tvStatus.setText(currentStatus.toUpperCase().replace("_", " "));
                    } else {
                        footerButtons.setVisibility(View.VISIBLE);
                        btnCancelApprovedBooking.setVisibility(View.GONE);
                        tvLabelStatus.setVisibility(View.GONE);
                        tvStatus.setVisibility(View.GONE);
                    }

                    // Checking "remarks" field as per database structure
                    if (snapshot.hasChild("remarks")) {
                        String remarks = String.valueOf(snapshot.child("remarks").getValue());
                        if (remarks != null && !remarks.isEmpty() && !remarks.equals("null")) {
                            tvLabelRemarks.setVisibility(View.VISIBLE);
                            tvRemarks.setVisibility(View.VISIBLE);
                            tvRemarks.setText(remarks);
                        }
                    }

                    Object bookedByObj = snapshot.child("bookedBy").getValue();
                    String userId = "";
                    if (bookedByObj instanceof String) {
                        userId = (String) bookedByObj;
                    } else if (snapshot.child("bookedBy").hasChild("uid")) {
                        userId = String.valueOf(snapshot.child("bookedBy").child("uid").getValue());
                    } else if (bookedByObj != null) {
                        userId = String.valueOf(bookedByObj);
                    }

                    if (userId != null && !userId.isEmpty() && !userId.equals("null")) {
                        loadUserName(userId.trim());
                    } else {
                        tvUserName.setText("No User ID");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FacultyBookingDetailsActivity.this, "Booking Read Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCancellation() {
        showRemarksDialog("Cancelled", true);
    }

    private void loadUserName(String userId) {
        usersRef.child(userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvUserName.setText(String.valueOf(snapshot.getValue()));
                } else {
                    tvUserName.setText("User Not Found (" + userId + ")");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvUserName.setText("Error: " + error.getMessage());
            }
        });
    }

    private void showRemarksDialog(String status, Object approvalValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(status + " Booking");
        builder.setMessage("Please enter the remarks for " + status.toLowerCase() + ":");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String remarks = input.getText().toString();
            if (remarks.isEmpty()) {
                Toast.makeText(this, "Remarks are required", Toast.LENGTH_SHORT).show();
            } else {
                updateBooking(status, approvalValue, remarks);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void viewLor() {
        if (lorUrl != null && !lorUrl.isEmpty() && !lorUrl.equals("null") && lorUrl.startsWith("http")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(lorUrl));
            startActivity(intent);
        } else {
            Toast.makeText(this, "LOR Link not available or invalid", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBooking(String status, Object facultyInchargeApproval, String remarks) {
        String approverUid = FirebaseAuth.getInstance().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("facultyInchargeApproval", facultyInchargeApproval);
        updates.put("approvedBy", approverUid);

        if (remarks != null) {
            updates.put("remarks", remarks);
        }

        bookingsRef.child(bookingId).updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                if ("Approved".equalsIgnoreCase(status)) {
                    updateScheduleStatus("BLOCKED");
                } else if ("Rejected".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
                    updateScheduleStatus("AVAILABLE");
                } else {
                    Toast.makeText(this, "Booking updated", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to update booking", Toast.LENGTH_SHORT).show());
    }

    private void updateScheduleStatus(String slotStatus) {
        if (scheduleId != null && slotStart != null && !scheduleId.equals("null") && !slotStart.equals("null")) {
            schedulesRef.child(scheduleId).child("slots").child(slotStart).child("status").setValue(slotStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking and Schedule updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Booking updated, but schedule failed", Toast.LENGTH_SHORT).show();
                    finish();
                });
        } else {
            Toast.makeText(this, "Booking updated (No Schedule Info)", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}