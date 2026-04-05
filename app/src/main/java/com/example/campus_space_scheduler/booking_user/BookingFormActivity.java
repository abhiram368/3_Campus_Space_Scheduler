package com.example.campus_space_scheduler.booking_user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_space_scheduler.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.example.hod.repository.FirebaseRepository;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingFormActivity extends AppCompatActivity {

    private static final String TAG = "BookingFormActivity";
    private String spaceName, date, timeSlot, userRole, spaceType, scheduleId, slotStart;

    private TextInputEditText etPurpose, etLorUrl, etDescription;
    private TextInputLayout textInputLayoutLorUrl;
    private MaterialButton buttonDownloadLorFormat;
    private MaterialButton buttonSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.t_activity_booking_form);

        // Get data from intent
        spaceName = getIntent().getStringExtra("SPACE_NAME");
        date = getIntent().getStringExtra("DATE");
        timeSlot = getIntent().getStringExtra("TIME_SLOT");
        userRole = getIntent().getStringExtra("ROLE");
        spaceType = getIntent().getStringExtra("SPACE_TYPE");
        scheduleId = getIntent().getStringExtra("SCHEDULE_ID");
        slotStart = getIntent().getStringExtra("SLOT_START");

        // Initialize views
        View btnBack = findViewById(R.id.buttonBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView textViewSpaceName = findViewById(R.id.textViewSpaceName);
        TextView textViewSelectedSlot = findViewById(R.id.textViewSelectedSlot);

        etPurpose = findViewById(R.id.etPurpose);
        etLorUrl = findViewById(R.id.etLorUrl);
        etDescription = findViewById(R.id.etDescription);
        textInputLayoutLorUrl = findViewById(R.id.textInputLayoutLorUrl);
        buttonDownloadLorFormat = findViewById(R.id.buttonDownloadLorFormat);

        buttonSubmit = findViewById(R.id.btnSubmit);

        if (spaceName != null) textViewSpaceName.setText(spaceName);
        if (date != null && timeSlot != null) textViewSelectedSlot.setText(date + " | " + timeSlot);

        updateUIBasedOnRole(userRole, spaceType);
        buttonSubmit.setOnClickListener(v -> {
            if (validateForm(userRole, spaceType)) {
                checkAvailabilityAndSubmit();
            }
        });
    }

    private void updateUIBasedOnRole(String role, String spaceType) {
        if (role == null || spaceType == null) return;
        
        // Direct booking for Classrooms - hide LOR requirements regardless of role
        if (spaceType.equalsIgnoreCase("Classroom")) {
            textInputLayoutLorUrl.setVisibility(View.GONE);
            buttonDownloadLorFormat.setVisibility(View.GONE);
            return;
        }

        if (role.equalsIgnoreCase("Student") && (spaceType.equalsIgnoreCase("Lab") || spaceType.equalsIgnoreCase("Hall"))) {
            textInputLayoutLorUrl.setVisibility(View.VISIBLE);
            buttonDownloadLorFormat.setVisibility(View.VISIBLE);
        } else if (role.equalsIgnoreCase("Faculty") && (spaceType.equalsIgnoreCase("Lab") || spaceType.equalsIgnoreCase("Hall"))) {
            textInputLayoutLorUrl.setVisibility(View.VISIBLE);
            buttonDownloadLorFormat.setVisibility(View.VISIBLE);
            textInputLayoutLorUrl.setHint("LOR URL (Optional for Faculty)");
        } else {
            textInputLayoutLorUrl.setVisibility(View.GONE);
            buttonDownloadLorFormat.setVisibility(View.GONE);
        }
    }

    private boolean validateForm(String role, String spaceType) {
        if (TextUtils.isEmpty(etPurpose.getText().toString().trim())) {
            etPurpose.setError("Purpose is required");
            return false;
        }
        
        // Skip LOR validation for Classrooms (Direct booking)
        if (spaceType != null && spaceType.equalsIgnoreCase("Classroom")) {
            return true;
        }

        if ("Student".equalsIgnoreCase(role) && ("Lab".equalsIgnoreCase(spaceType) || "Hall".equalsIgnoreCase(spaceType))) {
            if (TextUtils.isEmpty(etLorUrl.getText().toString().trim())) {
                etLorUrl.setError("LOR URL is required");
                return false;
            }
        }
        return true;
    }

    private void checkAvailabilityAndSubmit() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (scheduleId == null || slotStart == null) {
            Toast.makeText(this, "Missing schedule information", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSubmit.setEnabled(false);
        DatabaseReference slotsRef = FirebaseDatabase.getInstance().getReference("schedules").child(scheduleId).child("slots");

        boolean isClassroom = spaceType != null && spaceType.equalsIgnoreCase("Classroom");

        slotsRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                boolean found = false;
                for (MutableData slot : currentData.getChildren()) {
                    String start = slot.child("start").getValue(String.class);
                    if (start != null && start.replace(":", "").equals(slotStart)) {
                        String status = slot.child("status").getValue(String.class);
                        if ("AVAILABLE".equalsIgnoreCase(status)) {
                            // If classroom, mark as BOOKED immediately (direct booking)
                            // Otherwise, mark as Pending for approval
                            slot.child("status").setValue(isClassroom ? "BOOKED" : "Pending");
                            found = true;
                            break;
                        } else {
                            // Slot taken
                            return Transaction.abort();
                        }
                    }
                }
                return found ? Transaction.success(currentData) : Transaction.abort();
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (committed) {
                    saveBookingData();
                } else {
                    buttonSubmit.setEnabled(true);
                    String msg = "Slot no longer available.";
                    if (error != null) {
                        Log.e(TAG, "Transaction failed: " + error.getMessage());
                        if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                            msg = "Permission Denied: Please check Firebase rules.";
                        } else {
                            msg = "Error: " + error.getMessage();
                        }
                    }
                    Toast.makeText(BookingFormActivity.this, msg, Toast.LENGTH_LONG).show();
                    if (!committed && error == null) {
                        // Aborted because not available
                        finish();
                    }
                }
            }
        });
    }

    private void saveBookingData() {
        String uid = FirebaseAuth.getInstance().getUid();
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        String bookingId = bookingsRef.push().getKey();

        if (bookingId == null) return;

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date now = new Date();

        Map<String, String> reqTime = new HashMap<>();
        reqTime.put("date", sdfDate.format(now));
        reqTime.put("time", sdfTime.format(now));

        boolean isClassroom = spaceType != null && spaceType.equalsIgnoreCase("Classroom");

        Map<String, Object> data = new HashMap<>();
        data.put("bookingId", bookingId);
        data.put("bookedBy", uid);
        data.put("bookedTime", reqTime);
        data.put("purpose", etPurpose.getText().toString().trim());
        data.put("description", etDescription.getText().toString().trim());
        data.put("lorUpload", etLorUrl.getText().toString().trim());
        data.put("scheduleId", scheduleId);
        data.put("slotStart", slotStart);
        data.put("date", date);
        data.put("timeSlot", timeSlot);
        data.put("spaceName", spaceName);
        
        // Direct booking for Classrooms
        data.put("status", isClassroom ? "Approved" : "Pending");
        data.put("approvedBy", isClassroom ? "System (Auto)" : "");
        data.put("facultyInchargeApproval", isClassroom);
        data.put("hodApproval", isClassroom);
        if (isClassroom) {
            data.put("remarks", "Directly booked (First Come First Serve)");
        }

        bookingsRef.child(bookingId).setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String toastMsg = isClassroom ? "Booking Confirmed!" : "Booking Request Sent";
                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
                
                // Firebase Notification (Receipt to Student)
                String receiptMsg = isClassroom ? "Your booking for " + spaceName + " is confirmed!" : "Your booking for " + spaceName + " is successfully submitted and pending approval.";
                FirebaseRepository repo = new FirebaseRepository();
                repo.sendNotification(uid, receiptMsg, bookingId, isClassroom ? "Approved" : "Pending", uid, "receipt", uid, r -> {});

                // Firebase Notification (Alert to Staff Incharge)
                if (!isClassroom) {
                    String spaceId = scheduleId.split("_")[0];
                    String staffMsg = "New pending request for " + spaceName + " is awaiting your approval.";
                    repo.notifyStaffInchargeForSpace(spaceId, spaceName, staffMsg, bookingId, uid, "booking", "staff");
                }

                // Close the form and return to Dashboard
                Intent intent = new Intent(this, BookingUserActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("ROLE", userRole);
                startActivity(intent);
                finish();
            } else {
                // Rollback slot status to AVAILABLE if booking save fails
                FirebaseDatabase.getInstance().getReference("schedules").child(scheduleId).child("slots")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot slot : snapshot.getChildren()) {
                                    String start = slot.child("start").getValue(String.class);
                                    if (start != null && start.replace(":", "").equals(slotStart)) {
                                        slot.getRef().child("status").setValue("AVAILABLE");
                                        break;
                                    }
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });

                buttonSubmit.setEnabled(true);
                String msg = "Failed to save booking.";
                if (task.getException() != null) {
                    Log.e(TAG, "Save failed: " + task.getException().getMessage());
                    msg += " " + task.getException().getMessage();
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
