package com.example.hod.staff;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.campussync.appy.R;
import com.example.hod.models.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.hod.repository.FirebaseRepository; // Added import
import android.text.Html;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import java.util.Map;

public class StaffCompletedRequestDetailActivity extends AppCompatActivity {

    private TextView tvRequester, tvRequesterEmail, tvRequesterPhone, tvRequesterRole, tvPurpose, tvSlotUsage, tvBookingDate, tvRemarkContent, tvBlockStatus, tvFinalStatus, tvDecisionBy, tvDecisionTime;
    private Button btnViewDocuments, btnCancelBooking, btnBlockStudent, btnUnblockStudent, btnReApprove;
    private Booking booking;
    private String requesterName;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_request_detail);

        // 1. Find all views
        tvRequester = findViewById(R.id.tvRequester);
        tvRequesterEmail = findViewById(R.id.tvRequesterEmail);
        tvRequesterPhone = findViewById(R.id.tvRequesterPhone);
        tvRequesterRole = findViewById(R.id.tvRequesterRole);
        tvPurpose = findViewById(R.id.tvPurpose);
        tvSlotUsage = findViewById(R.id.tvSlotUsage);
        tvBookingDate = findViewById(R.id.tvBookingDate);
        tvRemarkContent = findViewById(R.id.tvRemarkContent);
        tvBlockStatus = findViewById(R.id.tvBlockStatus);
        tvFinalStatus = findViewById(R.id.tvFinalStatus);
        tvDecisionBy = findViewById(R.id.tvDecisionBy);
        tvDecisionTime = findViewById(R.id.tvDecisionTime);
        btnViewDocuments = findViewById(R.id.btnViewDocuments);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        btnBlockStudent = findViewById(R.id.btnBlockStudent);
        btnUnblockStudent = findViewById(R.id.btnUnblockStudent);
        btnReApprove = findViewById(R.id.btnReApprove);

        // 2. Null safety check
        if (tvRequester == null || tvRequesterEmail == null || tvRequesterPhone == null || tvRequesterRole == null ||
            tvPurpose == null || tvSlotUsage == null || tvBookingDate == null || tvRemarkContent == null ||
            tvBlockStatus == null || tvFinalStatus == null || tvDecisionBy == null || tvDecisionTime == null ||
            btnViewDocuments == null || btnCancelBooking == null || btnBlockStudent == null || 
            btnUnblockStudent == null || btnReApprove == null) {
            throw new RuntimeException("XML id mismatch in activity_completed_request_detail");
        }

        // 3. Get data from intent
        booking = (Booking) getIntent().getSerializableExtra("booking");
        requesterName = getIntent().getStringExtra("requesterName");

        if (booking != null) {
            updateHeader("Request Detail", "Completed Booking");
            populateBookingDetails();
            checkUserRoleAndStatus();
        }

        // 4. Set up button click listeners
        btnViewDocuments.setOnClickListener(v -> {
            if (booking != null && booking.getLorUpload() != null && !booking.getLorUpload().isEmpty()) {
                String url = booking.getLorUpload();
                if (url.startsWith("http")) {
                    try {
                        android.net.Uri uri = android.net.Uri.parse(url);
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Cannot open link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Invalid LOR link format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No documents available", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelBooking.setOnClickListener(v -> showCancelBookingDialog());

        btnBlockStudent.setOnClickListener(v -> showBlockStudentDialog());
        
        btnUnblockStudent.setOnClickListener(v -> showUnblockStudentDialog());

        btnReApprove.setOnClickListener(v -> showReApproveDialog());
    }

    private void showCancelBookingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.layout_cancel_booking_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null) tvTitle.setText("Cancel Booking");

        EditText etRemark = dialogView.findViewById(R.id.et_cancel_remark);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            String remark = etRemark.getText().toString().trim();
            if (remark.isEmpty()) {
                etRemark.setError("Remark is required");
            } else {
                cancelBookingInFirebase(remark);
                dialog.dismiss();
            }
        });

        dialog.show(); 
    }

    private void populateBookingDetails() {
        // 1. Requester name
        tvRequester.setText(Html.fromHtml("<b>Requester:</b> " + (requesterName != null ? requesterName : booking.getBookedBy()), Html.FROM_HTML_MODE_LEGACY));

        // 2. Purpose of booking
        tvPurpose.setText(Html.fromHtml("<b>Purpose:</b> " + (booking.getPurpose() != null ? booking.getPurpose() : "N/A"), Html.FROM_HTML_MODE_LEGACY));

        // 3. Slot and usage date
        String dateStr = booking.getDate() != null ? booking.getDate() : "";
        String timeStr = booking.getTimeSlot() != null ? booking.getTimeSlot() : "";
        tvSlotUsage.setText(Html.fromHtml("<b>Slot & Usage:</b> " + dateStr + (dateStr.isEmpty() || timeStr.isEmpty() ? "" : " | ") + timeStr, Html.FROM_HTML_MODE_LEGACY));

        // 4. Date of booking
        String bookedDate = booking.getBookedTimeDisplay();
        if (bookedDate == null || bookedDate.trim().isEmpty()) {
            bookedDate = booking.getDate() != null ? booking.getDate() : "N/A";
        }
        tvBookingDate.setText(Html.fromHtml("<b>Booking Date:</b> " + bookedDate, Html.FROM_HTML_MODE_LEGACY));

        // 5. Remark (show if not null/empty)
        String remark = booking.getRemark();
        tvRemarkContent.setVisibility(View.VISIBLE);
        if (remark != null && !remark.trim().isEmpty()) {
            tvRemarkContent.setText(Html.fromHtml("<b>Remark:</b> " + remark, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvRemarkContent.setText(Html.fromHtml("<b>Remark:</b> -", Html.FROM_HTML_MODE_LEGACY));
        }
        
        // Final Status
        String status = booking.getStatus() != null ? booking.getStatus() : "Unknown";
        tvFinalStatus.setText(Html.fromHtml("<b>Final Decision:</b> " + capitalize(status.replace("_", " ")), Html.FROM_HTML_MODE_LEGACY));
        
        // 6. Decided By & Time visibility
        String decById = booking.getApprovedBy();
        String decTime = booking.getDecisionTime();
        
        tvDecisionBy.setVisibility(View.VISIBLE);
        if (decById != null && !decById.isEmpty()) {
            resolveDecisionByName(decById);
        } else {
            tvDecisionBy.setText(Html.fromHtml("<b>Decided By:</b> -", Html.FROM_HTML_MODE_LEGACY));
        }

        tvDecisionTime.setVisibility(View.VISIBLE);
        if (decTime != null && !decTime.isEmpty()) {
            tvDecisionTime.setText(Html.fromHtml("<b>Time:</b> " + decTime, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvDecisionTime.setText(Html.fromHtml("<b>Time:</b> -", Html.FROM_HTML_MODE_LEGACY));
        }

        // Final Status Color & Button Visibility
        String lowerStatus = status.toLowerCase();
        if (lowerStatus.contains("rejected") || lowerStatus.equals("cancelled")) {
            tvFinalStatus.setTextColor(Color.parseColor("#EF4444")); // Red
            btnCancelBooking.setVisibility(View.GONE);
            btnReApprove.setVisibility(View.VISIBLE);
        } else if (lowerStatus.equals("approved") || lowerStatus.equals("available") || lowerStatus.equals("booked")) {
            tvFinalStatus.setTextColor(Color.parseColor("#10B981")); // Green
            btnCancelBooking.setVisibility(View.VISIBLE);
            btnReApprove.setVisibility(View.GONE);
        } else {
            // Forwarded or other transition status
            tvFinalStatus.setTextColor(Color.parseColor("#F59E0B")); // Amber
            btnCancelBooking.setVisibility(View.GONE);
            btnReApprove.setVisibility(View.GONE);
        }

        // Apply time-based restriction: Hide buttons for past bookings
        if (isBookingExpired(booking)) {
            btnCancelBooking.setVisibility(View.GONE);
            btnReApprove.setVisibility(View.GONE);
            // Optional: You could also hide Block/Unblock if desired, 
            // but the request was specific about cancel/reject/reapproval.
        }
    }

    private void checkUserRoleAndStatus() {
        String uid = booking.getBookedBy();
        if (uid == null) return;

        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // Populate Email and Phone
                String email = snapshot.child("emailId").getValue(String.class);
                String phone = snapshot.child("phoneNumber").getValue(String.class);
                if (email != null) tvRequesterEmail.setText(Html.fromHtml("<b>Email:</b> " + email, Html.FROM_HTML_MODE_LEGACY));
                if (phone != null) tvRequesterPhone.setText(Html.fromHtml("<b>Contact No:</b> " + phone, Html.FROM_HTML_MODE_LEGACY));

                String role = snapshot.child("role").getValue(String.class);
                if (role != null) tvRequesterRole.setText(Html.fromHtml("<b>Role:</b> " + role, Html.FROM_HTML_MODE_LEGACY));

                Boolean blocked = snapshot.child("blocked").getValue(Boolean.class);
                if (blocked == null) blocked = false;

                if ("Student".equalsIgnoreCase(role)) {
                    tvBlockStatus.setVisibility(View.VISIBLE);
                    if (blocked) {
                        tvBlockStatus.setText("Student Status: Blocked");
                        tvBlockStatus.setTextColor(Color.parseColor("#EF4444"));
                        btnBlockStudent.setVisibility(View.GONE);
                        btnUnblockStudent.setVisibility(View.VISIBLE);
                    } else {
                        tvBlockStatus.setText("Student Status: Active");
                        tvBlockStatus.setTextColor(Color.parseColor("#10B981"));
                        btnBlockStudent.setVisibility(View.VISIBLE);
                        btnUnblockStudent.setVisibility(View.GONE);
                    }
                } else {
                    tvBlockStatus.setVisibility(View.GONE);
                    btnBlockStudent.setVisibility(View.GONE);
                    btnUnblockStudent.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void resolveDecisionByName(String uid) {
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        if (name != null) {
                            tvDecisionBy.setText(Html.fromHtml("<b>Decided By:</b> " + name, Html.FROM_HTML_MODE_LEGACY));
                        } else {
                            tvDecisionBy.setText(Html.fromHtml("<b>Decided By:</b> " + uid, Html.FROM_HTML_MODE_LEGACY));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showBlockStudentDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.layout_cancel_booking_dialog, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null) tvTitle.setText("Block Student");
        
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        if (tvSubtitle != null) tvSubtitle.setText("Enter reason for blocking student:");

        EditText etRemark = dialogView.findViewById(R.id.et_cancel_remark);
        etRemark.setHint("Reason for blocking student");

        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String reason = etRemark.getText().toString().trim();
            if (reason.isEmpty()) {
                etRemark.setError("Reason is mandatory");
            } else {
                blockStudent(reason);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showUnblockStudentDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.layout_cancel_booking_dialog, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null) tvTitle.setText("Unblock Student");
        
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        if (tvSubtitle != null) tvSubtitle.setText("Are you sure you want to unblock this student?");

        EditText etRemark = dialogView.findViewById(R.id.et_cancel_remark);
        etRemark.setVisibility(View.GONE);

        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            unblockStudent();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void blockStudent(String reason) {
        String currentStaffUid = FirebaseAuth.getInstance().getUid();
        userRef.child("blocked").setValue(true);
        userRef.child("blockReason").setValue(reason);
        if (currentStaffUid != null) {
            userRef.child("blockedBy").setValue(currentStaffUid);
        }

        // Refresh UI
        tvBlockStatus.setText("Student Status: Blocked");
        tvBlockStatus.setTextColor(Color.parseColor("#EF4444"));
        btnBlockStudent.setVisibility(View.GONE);
        btnUnblockStudent.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Student blocked successfully", Toast.LENGTH_SHORT).show();
    }

    private void unblockStudent() {
        userRef.child("blocked").setValue(false);
        userRef.child("blockReason").removeValue();
        userRef.child("blockedBy").removeValue();

        // Refresh UI
        tvBlockStatus.setText("Student Status: Active");
        tvBlockStatus.setTextColor(Color.parseColor("#10B981"));
        btnBlockStudent.setVisibility(View.VISIBLE);
        btnUnblockStudent.setVisibility(View.GONE);
        Toast.makeText(this, "Student unblocked successfully", Toast.LENGTH_SHORT).show();
    }

    private void cancelBookingInFirebase(String remark) {
        if (booking.getBookingId() == null) return;
        
        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null 
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", "Cancelled");
        updates.put("remark", remark);
        updates.put("approvedBy", FirebaseAuth.getInstance().getUid());
        updates.put("decisionTime", currentTime);

        FirebaseDatabase.getInstance().getReference("bookings")
                .child(booking.getBookingId())
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking cancelled", Toast.LENGTH_SHORT).show();
                    tvFinalStatus.setText("Final Decision: Cancelled");
                    tvFinalStatus.setTextColor(Color.parseColor("#EF4444"));
                    btnCancelBooking.setVisibility(View.GONE);
                    
                    tvDecisionBy.setVisibility(View.VISIBLE);
                    tvDecisionTime.setVisibility(View.VISIBLE);
                    tvDecisionTime.setText(Html.fromHtml("<b>Time:</b> " + currentTime, Html.FROM_HTML_MODE_LEGACY));

                    // Show remark in UI
                    tvRemarkContent.setVisibility(View.VISIBLE);
                    tvRemarkContent.setText(Html.fromHtml("<b>Remark:</b> " + remark, Html.FROM_HTML_MODE_LEGACY));

                    // Sync slot status to Available
                    FirebaseRepository repo = new FirebaseRepository();
                    repo.updateSlotStatus(booking.getScheduleId(), booking.getTimeSlot(), "AVAILABLE", r -> {}); // Standardized to AVAILABLE
                });
    }

    private void showReApproveDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.layout_cancel_booking_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null) tvTitle.setText("Re-Approve Booking");
        
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        if (tvSubtitle != null) tvSubtitle.setText("Please provide a reason for re-approval");

        EditText etRemark = dialogView.findViewById(R.id.et_cancel_remark);
        etRemark.setHint("Enter reason for re-approval");
        
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
        btnConfirm.setText("Submit");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            String remark = etRemark.getText().toString().trim();
            if (remark.isEmpty()) {
                etRemark.setError("Remark is required");
            } else {
                reApproveBooking(remark);
                dialog.dismiss();
            }
        });

        dialog.show(); 
    }

    private void reApproveBooking(String remark) {
        if (booking.getBookingId() == null) return;

        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());
        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", "forwarded_to_faculty_incharge"); // Staff re-approving sends it to Faculty Incharge
        updates.put("remark", remark);
        updates.put("approvedBy", FirebaseAuth.getInstance().getUid());
        updates.put("decisionTime", currentTime);

        FirebaseDatabase.getInstance().getReference("bookings")
                .child(booking.getBookingId())
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking re-submitted for approval", Toast.LENGTH_SHORT).show();
                    tvFinalStatus.setText("Final Decision: Forwarded To Faculty Incharge");
                    tvFinalStatus.setTextColor(Color.parseColor("#F59E0B"));
                    btnReApprove.setVisibility(View.GONE);
                    
                    String currentUid = FirebaseAuth.getInstance().getUid();
                    tvDecisionBy.setVisibility(View.VISIBLE);
                    tvDecisionTime.setVisibility(View.VISIBLE);
                    tvDecisionTime.setText(Html.fromHtml("<b>Time:</b> " + currentTime, Html.FROM_HTML_MODE_LEGACY));

                    tvRemarkContent.setVisibility(View.VISIBLE);
                    tvRemarkContent.setText(Html.fromHtml("<b>Remark:</b> " + remark, Html.FROM_HTML_MODE_LEGACY));
                });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        View btnBack = findViewById(R.id.btnBack);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private boolean isBookingExpired(Booking b) {
        if (b == null || b.getDate() == null || b.getTimeSlot() == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar now = Calendar.getInstance();
            Calendar today = (Calendar) now.clone();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar bookingDate = Calendar.getInstance();
            bookingDate.setTime(sdf.parse(b.getDate()));

            if (bookingDate.before(today)) return true;
            if (bookingDate.after(today)) return false;

            // Same day: Check time slot end
            String timeSlot = b.getTimeSlot();
            if (timeSlot == null) return false;
            
            String normalized = timeSlot.replaceAll("\\s", "");
            String[] parts = normalized.split("[-–]");
            if (parts.length < 2) return false;
            
            String endTimeStr = parts[1];
            int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            int endMinutes = timeToMinutes(endTimeStr);

            if (endMinutes == -1) return false;
            return currentMinutes >= endMinutes;
        } catch (Exception e) {
            return false;
        }
    }

    private int timeToMinutes(String time) {
        if (time == null || time.isEmpty()) return -1;
        try {
            String upper = time.trim().toUpperCase();
            boolean isPM = upper.contains("PM");
            boolean isAM = upper.contains("AM");

            String cleanTime = upper.replaceAll("[^0-9:]", "");
            String[] parts = cleanTime.split(":");
            if (parts.length < 2) return -1;
            
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);

            if (isPM && hours < 12) hours += 12;
            if (isAM && hours == 12) hours = 0;

            return hours * 60 + minutes;
        } catch (Exception e) {
            return -1;
        }
    }
}
