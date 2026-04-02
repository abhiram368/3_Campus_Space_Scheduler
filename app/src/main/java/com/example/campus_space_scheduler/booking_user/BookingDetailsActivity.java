package com.example.campus_space_scheduler.booking_user;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.campus_space_scheduler.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BookingDetailsActivity extends AppCompatActivity {

    private static final String TAG = "BookingDetailsActivity";
    private static final int CALENDAR_PERMISSION_REQUEST_CODE = 101;
    
    private TextView bookedByTextView;
    private TextView approvedByTextView;
    private TextView textViewRemarks;
    private String bookingId, scheduleId, timeSlot, slotStart, spaceName, date, purpose, description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.t_activity_booking_details);

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextView spaceNameTextView = findViewById(R.id.textViewSpaceName);
        TextView statusTextView = findViewById(R.id.textViewStatus);
        TextView dateTimeTextView = findViewById(R.id.textViewDateTime);
        TextView purposeTextView = findViewById(R.id.textViewPurpose);
        TextView descriptionTextView = findViewById(R.id.textViewDescription);
        TextView requestedOnTextView = findViewById(R.id.textViewRequestedOn);
        bookedByTextView = findViewById(R.id.textViewBookedBy);
        approvedByTextView = findViewById(R.id.textViewApprovedBy);
        textViewRemarks = findViewById(R.id.textViewRemarks);
        MaterialButton buttonViewLor = findViewById(R.id.buttonViewLor);
        MaterialButton buttonCancelBooking = findViewById(R.id.buttonCancelBooking);
        MaterialButton buttonAddToCalendar = findViewById(R.id.buttonAddToCalendar);

        // Get data from intent
        bookingId = getIntent().getStringExtra("BOOKING_ID");
        scheduleId = getIntent().getStringExtra("SCHEDULE_ID");
        slotStart = getIntent().getStringExtra("SLOT_START");
        spaceName = getIntent().getStringExtra("SPACE_NAME");
        String bookedById = getIntent().getStringExtra("BOOKED_BY_ID");
        date = getIntent().getStringExtra("DATE");
        timeSlot = getIntent().getStringExtra("TIME_SLOT");
        purpose = getIntent().getStringExtra("PURPOSE");
        description = getIntent().getStringExtra("DESCRIPTION");
        String status = getIntent().getStringExtra("STATUS");
        String remarks = getIntent().getStringExtra("REMARKS");
        String actionBy = getIntent().getStringExtra("ACTION_BY");
        String requestedOn = getIntent().getStringExtra("REQUESTED_ON");
        boolean hasLor = getIntent().getBooleanExtra("HAS_LOR", false);
        String lorUrl = getIntent().getStringExtra("LOR_UPLOAD");
        boolean showCancelButton = getIntent().getBooleanExtra("SHOW_CANCEL_BUTTON", false);
        String approvedByUid = getIntent().getStringExtra("APPROVED_BY");

        // Set initial data to views
        if (spaceNameTextView != null) spaceNameTextView.setText(spaceName != null ? spaceName : "N/A");
        if (statusTextView != null) statusTextView.setText(status != null ? status.toUpperCase() : "PENDING");
        if (dateTimeTextView != null) dateTimeTextView.setText((date != null ? date : "") + " | " + (timeSlot != null ? timeSlot : ""));
        if (purposeTextView != null) purposeTextView.setText(purpose != null ? purpose : "N/A");
        if (descriptionTextView != null) descriptionTextView.setText(description != null ? description : "No description provided.");
        if (requestedOnTextView != null) requestedOnTextView.setText(requestedOn != null ? requestedOn : "N/A");

        if (bookedById != null && bookedByTextView != null) {
            fetchUserName(bookedById, bookedByTextView, null);
        } else if (bookedByTextView != null) {
            bookedByTextView.setText(R.string.unknown_user);
        }

        // UI styling for status
        if (statusTextView != null) {
            updateStatusUI(status, statusTextView);
        }

        // Handle Approval/Rejection/Forwarded and Remarks
        if (status != null) {
            boolean isApproved = status.equalsIgnoreCase("Approved") || status.equalsIgnoreCase("Accepted");
            boolean isForwarded = status.equalsIgnoreCase("Forwarded");
            boolean isRejected = status.equalsIgnoreCase("Rejected");

            // Google Calendar Integration for Approved bookings
            if (isApproved && buttonAddToCalendar != null) {
                buttonAddToCalendar.setVisibility(View.VISIBLE);
                buttonAddToCalendar.setOnClickListener(v -> checkCalendarPermissions());
            } else if (buttonAddToCalendar != null) {
                buttonAddToCalendar.setVisibility(View.GONE);
            }

            if (approvedByTextView != null) {
                String label;
                if (isApproved) label = "Approved by: ";
                else if (isForwarded) label = "Forwarded by: ";
                else if (isRejected) label = "Rejected by: ";
                else label = "Action by: ";

                if (!status.equalsIgnoreCase("Pending")) {
                    if (approvedByUid != null && !approvedByUid.isEmpty()) {
                        if (approvedByUid.length() > 15 && !approvedByUid.contains(" ")) {
                            fetchUserName(approvedByUid, approvedByTextView, label);
                        } else {
                            approvedByTextView.setText(label + approvedByUid);
                            approvedByTextView.setVisibility(View.VISIBLE);
                        }
                    } else if (actionBy != null && !actionBy.isEmpty()) {
                        approvedByTextView.setText(label + actionBy);
                        approvedByTextView.setVisibility(View.VISIBLE);
                    } else {
                        approvedByTextView.setText(label + "Authority");
                        approvedByTextView.setVisibility(View.VISIBLE);
                    }
                } else {
                    approvedByTextView.setVisibility(View.GONE);
                }
            }

            // Always show updated remarks if not null/empty
            if (textViewRemarks != null) {
                if (remarks != null && !remarks.trim().isEmpty()) {
                    textViewRemarks.setText("Remarks: " + remarks);
                    textViewRemarks.setVisibility(View.VISIBLE);
                } else {
                    textViewRemarks.setVisibility(View.GONE);
                }
            }
        }

        if (buttonViewLor != null) {
            if (hasLor && lorUrl != null && !lorUrl.isEmpty()) {
                buttonViewLor.setVisibility(View.VISIBLE);
                buttonViewLor.setOnClickListener(v -> {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(lorUrl));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.invalid_lor_link, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                buttonViewLor.setVisibility(View.GONE);
            }
        }

        if (buttonCancelBooking != null) {
            if (showCancelButton && status != null && !status.equalsIgnoreCase("Rejected")) {
                buttonCancelBooking.setVisibility(View.VISIBLE);
                buttonCancelBooking.setOnClickListener(v -> showCancelConfirmationDialog());
            } else {
                buttonCancelBooking.setVisibility(View.GONE);
            }
        }

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void checkCalendarPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR}, CALENDAR_PERMISSION_REQUEST_CODE);
        } else {
            addToCalendar();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addToCalendar();
            } else {
                Toast.makeText(this, "Calendar permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addToCalendar() {
        CalendarHelper.addBookingToCalendar(
                this,
                "Booking: " + (spaceName != null ? spaceName : "Space"),
                "Purpose: " + (purpose != null ? purpose : "N/A") + "\nDescription: " + (description != null ? description : ""),
                spaceName != null ? spaceName : "Campus",
                date != null ? date : "",
                timeSlot != null ? timeSlot : ""
        );
    }

    private void showCancelConfirmationDialog() {
        new MaterialAlertDialogBuilder(this, R.style.Theme_CampusSpaceScheduler_Dialog_Custom)
                .setTitle("Confirm Cancellation")
                .setMessage("Are you sure you want to cancel this booking? This action cannot be undone.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> performCancellation())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performCancellation() {
        if (bookingId == null || scheduleId == null) {
            Toast.makeText(this, "Error: Missing IDs", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference bookingRef = FirebaseDatabase.getInstance().getReference("bookings").child(bookingId);
        DatabaseReference slotsRef = FirebaseDatabase.getInstance().getReference("schedules").child(scheduleId).child("slots");

        bookingRef.removeValue().addOnSuccessListener(aVoid -> {
            slotsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean updated = false;
                    for (DataSnapshot slotSnapshot : snapshot.getChildren()) {
                        String start = slotSnapshot.child("start").getValue(String.class);
                        if (start != null) {
                            String normalizedStart = start.replace(":", "");
                            if (normalizedStart.equals(slotStart)) {
                                slotSnapshot.getRef().child("status").setValue("AVAILABLE");
                                updated = true;
                                break;
                            }
                        }
                    }

                    if (!updated && timeSlot != null) {
                        for (DataSnapshot slotSnapshot : snapshot.getChildren()) {
                            String start = slotSnapshot.child("start").getValue(String.class);
                            String end = slotSnapshot.child("end").getValue(String.class);
                            if (start != null && end != null) {
                                String label = start + " - " + end;
                                if (timeSlot.contains(label)) {
                                    slotSnapshot.getRef().child("status").setValue("AVAILABLE");
                                    break;
                                }
                            }
                        }
                    }

                    Toast.makeText(BookingDetailsActivity.this, R.string.booking_cancelled_successfully, Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Cancel failed: " + error.getMessage());
                    Toast.makeText(BookingDetailsActivity.this, R.string.failed_update_schedule, Toast.LENGTH_SHORT).show();
                }
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(BookingDetailsActivity.this, R.string.failed_cancel_booking, Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchUserName(String uid, TextView textView, String prefix) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name == null) name = snapshot.child("displayName").getValue(String.class);
                    if (name == null) name = snapshot.child("full_name").getValue(String.class);

                    String rollNumber = snapshot.child("roolno").getValue(String.class);
                    if (rollNumber == null) rollNumber = snapshot.child("rollnumber").getValue(String.class);
                    if (rollNumber == null) rollNumber = snapshot.child("rollNumber").getValue(String.class);

                    String displayStr = (name != null) ? name : "User";
                    if (rollNumber != null && !rollNumber.isEmpty() && !rollNumber.equals("null")) {
                        displayStr += " (" + rollNumber + ")";
                    }

                    textView.setText((prefix != null ? prefix : "") + displayStr);
                    textView.setVisibility(View.VISIBLE);
                } else {
                    textView.setText((prefix != null ? prefix : "") + "Authority");
                    textView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                textView.setText(R.string.error_loading_info);
            }
        });
    }

    private void updateStatusUI(String status, TextView statusTextView) {
        if (status == null) return;

        switch (status.toUpperCase()) {
            case "ACCEPTED":
            case "APPROVED":
                statusTextView.setBackgroundResource(R.drawable.status_accepted_bg);
                break;
            case "REJECTED":
                statusTextView.setBackgroundResource(R.drawable.status_rejected_bg);
                break;
            case "FORWARDED":
                statusTextView.setBackgroundResource(R.drawable.status_pending_bg);
                statusTextView.getBackground().setTint(Color.parseColor("#FF9800")); // Orange for Forwarded
                break;
            default:
                statusTextView.setBackgroundResource(R.drawable.status_pending_bg);
                statusTextView.getBackground().setTintList(null);
                break;
        }
    }
}
