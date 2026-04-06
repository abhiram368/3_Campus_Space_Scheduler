package com.example.campus_space_scheduler.booking_user;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class BookingDetailsActivity extends AppCompatActivity {

    private static final String TAG = "BookingDetailsActivity";
    private static final int CALENDAR_PERMISSION_REQUEST_CODE = 101;

    private TextView spaceNameTextView, statusTextView, dateTimeTextView, purposeTextView,
            descriptionTextView, requestedOnTextView, bookedByTextView,
            approvedByTextView, textViewRemarks;
    private MaterialButton buttonViewLor, buttonCancelBooking, buttonAddToCalendar;

    private String bookingId, scheduleId, timeSlot, slotStart, spaceName, date, purpose, description, spaceType;
    private DatabaseReference bookingRef;
    private ValueEventListener bookingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.t_activity_booking_details);

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        spaceNameTextView = findViewById(R.id.textViewSpaceName);
        statusTextView = findViewById(R.id.textViewStatus);
        dateTimeTextView = findViewById(R.id.textViewDateTime);
        purposeTextView = findViewById(R.id.textViewPurpose);
        descriptionTextView = findViewById(R.id.textViewDescription);
        requestedOnTextView = findViewById(R.id.textViewRequestedOn);
        bookedByTextView = findViewById(R.id.textViewBookedBy);
        approvedByTextView = findViewById(R.id.textViewApprovedBy);
        textViewRemarks = findViewById(R.id.textViewRemarks);
        buttonViewLor = findViewById(R.id.buttonViewLor);
        buttonCancelBooking = findViewById(R.id.buttonCancelBooking);
        buttonAddToCalendar = findViewById(R.id.buttonAddToCalendar);

        // Get initial data from intent
        bookingId = getIntent().getStringExtra("BOOKING_ID");
        if (bookingId == null) {
            bookingId = getIntent().getStringExtra("bookingId");
        }
        scheduleId = getIntent().getStringExtra("SCHEDULE_ID");
        slotStart = getIntent().getStringExtra("SLOT_START");
        spaceName = getIntent().getStringExtra("SPACE_NAME");
        String bookedById = getIntent().getStringExtra("BOOKED_BY_ID");
        date = getIntent().getStringExtra("DATE");
        timeSlot = getIntent().getStringExtra("TIME_SLOT");
        purpose = getIntent().getStringExtra("PURPOSE");
        description = getIntent().getStringExtra("DESCRIPTION");
        String status = getIntent().getStringExtra("STATUS");
        String remark = getIntent().getStringExtra("REMARKS");
        String actionBy = getIntent().getStringExtra("ACTION_BY");
        String requestedOn = getIntent().getStringExtra("REQUESTED_ON");
        boolean hasLor = getIntent().getBooleanExtra("HAS_LOR", false);
        String lorUrl = getIntent().getStringExtra("LOR_UPLOAD");
        boolean showCancelButton = getIntent().getBooleanExtra("SHOW_CANCEL_BUTTON", false);
        String approvedByUid = getIntent().getStringExtra("APPROVED_BY");
        spaceType = getIntent().getStringExtra("SPACE_TYPE");

        // Set initial data to views
        updateUI(spaceName, status, date, timeSlot, purpose, description, requestedOn, bookedById, approvedByUid, actionBy, remark, hasLor, lorUrl, showCancelButton, spaceType);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Real-time listener to always show updated remarks and status
        if (bookingId != null) {
            bookingRef = FirebaseDatabase.getInstance().getReference("bookings").child(bookingId);
            setupBookingListener();
        }
    }

    private void setupBookingListener() {
        bookingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                Booking booking = snapshot.getValue(Booking.class);
                if (booking != null) {
                    String status = booking.getStatus();
                    String remark = booking.getRemark();
                    String actionBy = booking.getActionBy();
                    String approvedByUid = booking.getApprovedBy();
                    String lorUrl = booking.getLorUpload();
                    boolean hasLor = lorUrl != null && !lorUrl.isEmpty();

                    // Update variables used for calendar
                    spaceName = booking.getSpaceName();
                    date = booking.getDate();
                    timeSlot = booking.getTimeSlot();
                    purpose = booking.getPurpose();
                    description = booking.getDescription();
                    spaceType = booking.getSpaceType();

                    updateUI(spaceName, status, date, timeSlot, purpose, description,
                            null, // requestedOn handled by intent initially
                            booking.getBookedBy(), approvedByUid, actionBy, remark, hasLor, lorUrl,
                            getIntent().getBooleanExtra("SHOW_CANCEL_BUTTON", false), spaceType);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        };
        bookingRef.addValueEventListener(bookingListener);
    }

    private void updateUI(String spaceName, String status, String date, String timeSlot,
                          String purpose, String description, String requestedOn,
                          String bookedById, String approvedByUid, String actionBy,
                          String remark, boolean hasLor, String lorUrl, boolean showCancelButton,
                          String spaceType) {

        if (spaceNameTextView != null) spaceNameTextView.setText(spaceName != null ? spaceName : "N/A");
        if (statusTextView != null) {
            statusTextView.setText(status != null ? status.toUpperCase() : "PENDING");
            updateStatusUI(status, statusTextView);
        }
        if (dateTimeTextView != null) dateTimeTextView.setText((date != null ? date : "") + " | " + (timeSlot != null ? timeSlot : ""));
        if (purposeTextView != null) purposeTextView.setText(purpose != null ? purpose : "N/A");
        if (descriptionTextView != null) descriptionTextView.setText(description != null ? description : "No description provided.");

        if (requestedOn != null && requestedOnTextView != null) {
            requestedOnTextView.setText(requestedOn);
        }

        // If data is missing (common with notifications), fetch it from Firebase
        if (bookingId != null && (spaceName == null || spaceName.isEmpty() || spaceName.equalsIgnoreCase("N/A"))) {
            Log.d(TAG, "Triggering full booking fetch for ID: " + bookingId);
            fetchFullBookingDetails(bookingId);
        }

        if (bookedById != null && bookedByTextView != null) {
            fetchUserName(bookedById, bookedByTextView, null);
        }

        // Determine if it's a Classroom (direct booking)
        boolean isClassroom = (spaceType != null && spaceType.equalsIgnoreCase("Classroom")) ||
                (remark != null && remark.contains("Directly booked"));

        // Handle Status logic
        if (status != null) {
            boolean isApproved = status.equalsIgnoreCase("Approved") || status.equalsIgnoreCase("Accepted");
            boolean isForwarded = status.equalsIgnoreCase("Forwarded") || status.toLowerCase().contains("forwarded");
            boolean isRejected = status.equalsIgnoreCase("Rejected");
            boolean isCancelled = status.equalsIgnoreCase("Cancelled");
            boolean isExpired = status.toLowerCase().contains("expired");

            if (isApproved && buttonAddToCalendar != null) {
                buttonAddToCalendar.setVisibility(View.VISIBLE);
                buttonAddToCalendar.setOnClickListener(v -> checkCalendarPermissions());
            } else if (buttonAddToCalendar != null) {
                buttonAddToCalendar.setVisibility(View.GONE);
            }

            if (approvedByTextView != null) {
                // If it's a Classroom and status is Approved, hide "Approved by"
                if (isClassroom && isApproved) {
                    approvedByTextView.setVisibility(View.GONE);
                } else {
                    String label;
                    if (isApproved) label = "Approved by: ";
                    else if (isForwarded) label = "Forwarded by: ";
                    else if (isRejected) label = "Rejected by: ";
                    else label = "Action by: ";

                    if (!status.equalsIgnoreCase("Pending") && !isCancelled && !isExpired) {
                        if (approvedByUid != null && !approvedByUid.isEmpty()) {
                            fetchUserName(approvedByUid, approvedByTextView, label);
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
            }
        }

        // Always show updated remarks if not null/empty
        if (textViewRemarks != null) {
            if (remark != null && !remark.trim().isEmpty() && !remark.equalsIgnoreCase("null")) {
                textViewRemarks.setText("Remarks: " + remark);
                textViewRemarks.setVisibility(View.VISIBLE);
            } else {
                textViewRemarks.setVisibility(View.GONE);
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
            // Allow cancel only if showCancelButton is true AND (not a classroom OR it's coming from CancelRequestActivity)
            // Wait, you said "add cancel request option... only for classroom".
            // Actually, based on your previous messages, you want Classrooms to be cancellable ONLY from the Cancel screen.

            boolean isFromCancelScreen = getIntent().getBooleanExtra("SHOW_CANCEL_BUTTON", false);

            boolean canCancel = false;
            if (isFromCancelScreen) {
                // If it's from the Cancel screen, we allow cancellation for EVERYTHING (including classrooms)
                canCancel = true;
            } else {
                // If NOT from cancel screen (e.g. History), classrooms cannot be cancelled
                canCancel = !isClassroom;
            }

            if (canCancel && status != null && !status.equalsIgnoreCase("Rejected")
                    && !status.equalsIgnoreCase("Cancelled") && !status.toLowerCase().contains("expired")) {
                buttonCancelBooking.setVisibility(View.VISIBLE);
                buttonCancelBooking.setOnClickListener(v -> showCancelConfirmationDialog());
            } else {
                buttonCancelBooking.setVisibility(View.GONE);
            }
        }
    }

    private void fetchFullBookingDetails(String id) {
        DatabaseReference bookingRef = FirebaseDatabase.getInstance().getReference("bookings").child(id);
        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    bookingId = snapshot.child("bookingId").getValue(String.class);
                    scheduleId = snapshot.child("scheduleId").getValue(String.class);
                    slotStart = snapshot.child("slotStart").getValue(String.class);
                    spaceName = snapshot.child("spaceName").getValue(String.class);
                    date = snapshot.child("date").getValue(String.class);
                    timeSlot = snapshot.child("timeSlot").getValue(String.class);
                    purpose = snapshot.child("purpose").getValue(String.class);
                    description = snapshot.child("description").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);
                    String remarks = snapshot.child("remarks").getValue(String.class);
                    if (remarks == null) {
                        remarks = snapshot.child("remark").getValue(String.class);
                    }
                    String actionBy = snapshot.child("actionBy").getValue(String.class);
                    String bookedById = snapshot.child("bookedBy").getValue(String.class);
                    String approvedByUid = snapshot.child("approvedBy").getValue(String.class);
                    String lorUrl = snapshot.child("lorUpload").getValue(String.class);
                    boolean hasLor = (lorUrl != null && !lorUrl.isEmpty());

                    // Requested Time
                    String requestedDate = snapshot.child("bookedTime").child("date").getValue(String.class);
                    String requestedTime = snapshot.child("bookedTime").child("time").getValue(String.class);
                    String requestedOn = (requestedDate != null ? requestedDate : "") + " " + (requestedTime != null ? requestedTime : "");

                    // Update UI components
                    updateUI(spaceName, status, date, timeSlot, purpose, description, requestedOn, bookedById, approvedByUid, remarks, actionBy, hasLor, lorUrl, getIntent().getBooleanExtra("SHOW_CANCEL_BUTTON", false), spaceType);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching booking details: " + error.getMessage());
            }
        });
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
                .setPositiveButton("Yes, Cancel", (dialog, which) -> fetchCurrentUserNameAndCancel())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void fetchCurrentUserNameAndCancel() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            performCancellation("User");
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String displayName = "User";
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name == null) name = snapshot.child("displayName").getValue(String.class);
                    if (name == null) name = snapshot.child("full_name").getValue(String.class);

                    if (name != null) displayName = name;
                    else displayName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User";
                }
                performCancellation(displayName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                performCancellation("User");
            }
        });
    }

    private void performCancellation(String userName) {
        if (bookingId == null || scheduleId == null) {
            Toast.makeText(this, "Error: Missing IDs", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference slotsRef = FirebaseDatabase.getInstance().getReference("schedules").child(scheduleId).child("slots");

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Cancelled");
        updates.put("remark", "Cancelled by " + userName);
        updates.put("actionBy", userName);

        bookingRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
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

                    // Show notification for the user who cancelled
                    NotificationHelper.showNotification(
                            BookingDetailsActivity.this,
                            "Booking Cancelled",
                            "Your booking for " + spaceName + " has been successfully cancelled.",
                            bookingId
                    );

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

        String statusLower = status.toLowerCase();
        if (statusLower.contains("expired")) {
            statusTextView.setBackgroundResource(R.drawable.status_rejected_bg);
            statusTextView.getBackground().setTint(Color.parseColor("#757575")); // Grey for Expired
            return;
        }

        switch (status.toUpperCase()) {
            case "ACCEPTED":
            case "APPROVED":
                statusTextView.setBackgroundResource(R.drawable.status_accepted_bg);
                statusTextView.getBackground().setTintList(null);
                break;
            case "REJECTED":
                statusTextView.setBackgroundResource(R.drawable.status_rejected_bg);
                statusTextView.getBackground().setTintList(null);
                break;
            case "FORWARDED":
                statusTextView.setBackgroundResource(R.drawable.status_pending_bg);
                statusTextView.getBackground().setTint(Color.parseColor("#FF9800")); // Orange for Forwarded
                break;
            case "CANCELLED":
                statusTextView.setBackgroundResource(R.drawable.status_rejected_bg);
                statusTextView.getBackground().setTint(Color.GRAY);
                break;
            default:
                statusTextView.setBackgroundResource(R.drawable.status_pending_bg);
                statusTextView.getBackground().setTintList(null);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingRef != null && bookingListener != null) {
            bookingRef.removeEventListener(bookingListener);
        }
    }
}