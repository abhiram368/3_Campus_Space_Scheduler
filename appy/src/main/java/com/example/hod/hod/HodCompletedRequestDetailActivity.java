package com.example.hod.hod;

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
import com.example.hod.models.User;
import com.example.hod.repository.FirebaseRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.text.Html;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HodCompletedRequestDetailActivity extends AppCompatActivity {

    private TextView tvRequester, tvRequesterEmail, tvRequesterPhone, tvRequesterRole, tvPurpose, tvSlotUsage, tvBookingDate, tvRemarkContent, tvBlockStatus, tvFinalStatus, tvDecisionBy, tvDecisionTime;
    private Button btnViewDocuments, btnCancelBooking, btnBlockStudent, btnUnblockStudent, btnReApprove;
    private Booking booking;
    private FirebaseRepository repository;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_completed_request_detail);

        // Find views
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

        repository = new FirebaseRepository();
        booking = (Booking) getIntent().getSerializableExtra("booking");

        if (booking == null) {
            Toast.makeText(this, R.string.error_booking_data_missing, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        updateHeader(getString(R.string.request_detail_title), getString(R.string.completed_booking_subtitle));
        populateDetails();
        loadRequesterAndBlockStatus();

        setupClickListeners();
    }

    private void setupClickListeners() {
        if (btnViewDocuments != null) {
            btnViewDocuments.setOnClickListener(v -> {
                if (booking.getLorUpload() != null && !booking.getLorUpload().isEmpty()) {
                    openUrl(booking.getLorUpload());
                } else {
                    Toast.makeText(this, R.string.no_documents_available, Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnCancelBooking != null) {
            btnCancelBooking.setOnClickListener(v -> showCancelBookingDialog());
        }

        // Block/Unblock is handled by Staff only – hide for HOD
        if (btnBlockStudent != null) btnBlockStudent.setVisibility(View.GONE);
        if (btnUnblockStudent != null) btnUnblockStudent.setVisibility(View.GONE);
        if (tvBlockStatus != null) tvBlockStatus.setVisibility(View.GONE);

        if (btnReApprove != null) {
            btnReApprove.setOnClickListener(v -> showReApproveDialog());
        }
    }

    private void populateDetails() {
        tvPurpose.setText(getString(R.string.label_purpose_prefix, (booking.getPurpose() != null ? booking.getPurpose() : "N/A")));
        tvSlotUsage.setText(getString(R.string.label_slot_usage_prefix, booking.getDate() + " | " + booking.getTimeSlot()));

        // Booking Date (Time it was made)
        String bookedDateDisplay = "N/A";
        Map<String, String> bookedTime = booking.getBookedTime();
        if (bookedTime != null && bookedTime.get("date") != null) {
            bookedDateDisplay = bookedTime.get("date");
        } else if (booking.getDate() != null) {
            bookedDateDisplay = booking.getDate();
        }
        tvBookingDate.setText(Html.fromHtml("<b>Booking Date:</b> " + bookedDateDisplay, Html.FROM_HTML_MODE_LEGACY));

        // Status
        String status = booking.getStatus() != null ? booking.getStatus() : getString(R.string.status_unknown);
        tvFinalStatus.setText(Html.fromHtml("<b>Final Decision:</b> " + capitalize(status.replace("_", " ")), Html.FROM_HTML_MODE_LEGACY));
        
        // Colors and Button Visibility
        String lowerStatus = status.toLowerCase();
        if (lowerStatus.contains("rejected") || lowerStatus.equals("cancelled") || lowerStatus.contains("expired")) {
            tvFinalStatus.setTextColor(Color.parseColor("#EF4444")); // Red
            btnCancelBooking.setVisibility(View.GONE);
            if (lowerStatus.contains("rejected") && !isBookingExpired(booking)) {
                btnReApprove.setVisibility(View.VISIBLE);
            } else {
                btnReApprove.setVisibility(View.GONE);
            }
        } else if (lowerStatus.equals("approved") || lowerStatus.equals("booked")) {
            tvFinalStatus.setTextColor(Color.parseColor("#10B981")); // Green
            btnCancelBooking.setVisibility(View.VISIBLE);
            btnReApprove.setVisibility(View.GONE);
        } else {
            tvFinalStatus.setTextColor(Color.parseColor("#F59E0B")); // Amber
            btnCancelBooking.setVisibility(View.GONE);
            btnReApprove.setVisibility(View.GONE);
        }

        // Apply time-based restriction: Hide buttons for past bookings
        if (isBookingExpired(booking)) {
            btnCancelBooking.setVisibility(View.GONE);
            btnReApprove.setVisibility(View.GONE);
        }

        // Remarks
        String remark = booking.getRemark();
        tvRemarkContent.setVisibility(View.VISIBLE);
        if (remark != null && !remark.trim().isEmpty()) {
            tvRemarkContent.setText(Html.fromHtml("<b>Remark:</b> " + remark, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvRemarkContent.setText(Html.fromHtml("<b>Remark:</b> -", Html.FROM_HTML_MODE_LEGACY));
        }

        // Decision By & Time
        String decById = booking.getApprovedBy();
        String decTime = booking.getDecisionTime();
        
        tvDecisionBy.setVisibility(View.VISIBLE);
        if (decById != null && !decById.isEmpty()) {
            resolveDecisionByName(decById);
        } else {
            tvDecisionBy.setText("Decided By: -");
        }

        tvDecisionTime.setVisibility(View.VISIBLE);
        if (decTime != null && !decTime.isEmpty()) {
            tvDecisionTime.setText(Html.fromHtml("<b>Time:</b> " + decTime, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvDecisionTime.setText(Html.fromHtml("<b>Time:</b> -", Html.FROM_HTML_MODE_LEGACY));
        }
    }

    private void loadRequesterAndBlockStatus() {
        String uid = booking.getBookedBy();
        if (uid == null) return;

        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                User u = snapshot.getValue(User.class);
                if (u != null) {
                    tvRequester.setText(Html.fromHtml("<b>Requester:</b> " + (u.name != null ? u.name : getString(R.string.status_unknown)), Html.FROM_HTML_MODE_LEGACY));
                    tvRequesterEmail.setText(Html.fromHtml("<b>Email:</b> " + (u.emailId != null ? u.emailId : "N/A"), Html.FROM_HTML_MODE_LEGACY));
                    tvRequesterPhone.setText(Html.fromHtml("<b>Contact No:</b> " + (u.phoneNumber != null ? u.phoneNumber : "N/A"), Html.FROM_HTML_MODE_LEGACY));
                    tvRequesterRole.setText(Html.fromHtml("<b>Role:</b> " + (u.role != null ? u.role : "N/A"), Html.FROM_HTML_MODE_LEGACY));

                    // HOD: always hide block/unblock regardless of role
                    tvBlockStatus.setVisibility(View.GONE);
                    btnBlockStudent.setVisibility(View.GONE);
                    btnUnblockStudent.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateBlockUI(boolean blocked) {
        if (blocked) {
            tvBlockStatus.setText(R.string.status_student_blocked);
            tvBlockStatus.setTextColor(Color.parseColor("#EF4444"));
            btnBlockStudent.setVisibility(View.GONE);
            btnUnblockStudent.setVisibility(View.VISIBLE);
        } else {
            tvBlockStatus.setText(R.string.status_student_active);
            tvBlockStatus.setTextColor(Color.parseColor("#10B981"));
            btnBlockStudent.setVisibility(View.VISIBLE);
            btnUnblockStudent.setVisibility(View.GONE);
        }
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

    private void openUrl(String url) {
        try {
            android.net.Uri uri = android.net.Uri.parse(url);
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_cannot_open_link, Toast.LENGTH_SHORT).show();
        }
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
        if (tvTitle != null) tvTitle.setText(R.string.dialog_title_cancel_booking);

        EditText etRemark = dialogView.findViewById(R.id.et_cancel_remark);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String remark = etRemark.getText().toString().trim();
            if (remark.isEmpty()) {
                etRemark.setError(getString(R.string.error_remark_required));
            } else {
                cancelBooking(remark);
                dialog.dismiss();
            }
        });

        dialog.show(); 
    }

    private void cancelBooking(String remark) {
        if (booking.getBookingId() == null) return;

        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Cancelled");
        updates.put("remark", remark);
        updates.put("approvedBy", FirebaseAuth.getInstance().getUid());
        updates.put("decisionTime", currentTime);

        FirebaseDatabase.getInstance().getReference("bookings")
                .child(booking.getBookingId())
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.msg_booking_cancelled, Toast.LENGTH_SHORT).show();
                    tvFinalStatus.setText(getString(R.string.label_final_decision_prefix, "Cancelled"));
                    tvFinalStatus.setTextColor(Color.parseColor("#EF4444"));
                    btnCancelBooking.setVisibility(View.GONE);
                    
                    String currentUid = FirebaseAuth.getInstance().getUid();
                    if (currentUid != null) resolveDecisionByName(currentUid);
                    tvDecisionBy.setVisibility(View.VISIBLE);
                    tvDecisionTime.setVisibility(View.VISIBLE);
                    tvDecisionTime.setText("Time: " + currentTime);

                    tvRemarkContent.setVisibility(View.VISIBLE);
                    tvRemarkContent.setText("Remark: " + remark);

                    // Sync slot status to AVAILABLE
                    repository.updateSlotStatus(booking.getScheduleId(), booking.getTimeSlot(), "AVAILABLE", r -> {});
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
        if (tvTitle != null) tvTitle.setText(R.string.dialog_title_block_student);
        
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.msg_enter_block_reason);

        EditText etRemark = dialogView.findViewById(R.id.et_cancel_remark);
        etRemark.setHint(R.string.hint_block_reason);

        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String reason = etRemark.getText().toString().trim();
            if (reason.isEmpty()) {
                etRemark.setError(getString(R.string.error_reason_mandatory));
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
        if (tvTitle != null) tvTitle.setText(R.string.unblock_student);
        
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.msg_confirm_unblock);

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
        if (userRef == null) return;
        String currentStaffUid = FirebaseAuth.getInstance().getUid();
        userRef.child("blocked").setValue(true);
        userRef.child("blockReason").setValue(reason);
        if (currentStaffUid != null) {
            userRef.child("blockedBy").setValue(currentStaffUid);
        }
        updateBlockUI(true);
        Toast.makeText(this, R.string.msg_student_blocked, Toast.LENGTH_SHORT).show();
    }

    private void unblockStudent() {
        if (userRef == null) return;
        userRef.child("blocked").setValue(false);
        userRef.child("blockReason").removeValue();
        userRef.child("blockedBy").removeValue();
        updateBlockUI(false);
        Toast.makeText(this, R.string.msg_student_unblocked, Toast.LENGTH_SHORT).show();
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
        if (tvTitle != null) tvTitle.setText(R.string.dialog_title_reapprove);
        
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.msg_provide_reapprove_reason);

        EditText etRemark = dialogView.findViewById(R.id.et_cancel_remark);
        etRemark.setHint(R.string.hint_reapprove_reason);
        
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
        btnConfirm.setText(R.string.approve);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            String remark = etRemark.getText().toString().trim();
            if (remark.isEmpty()) {
                etRemark.setError(getString(R.string.error_remark_required));
            } else {
                reApproveBooking(remark);
                dialog.dismiss();
            }
        });

        dialog.show(); 
    }

    private void reApproveBooking(String remark) {
        if (booking.getBookingId() == null) return;

        // First check the current slot status before re-approving
        String scheduleId = booking.getScheduleId();
        String timeSlot   = booking.getTimeSlot();

        if (scheduleId != null && timeSlot != null) {
            FirebaseDatabase.getInstance().getReference("schedules")
                    .child(scheduleId).child("slots").child(timeSlot).child("status")
                    .get()
                    .addOnSuccessListener(snap -> {
                        String slotStatus = snap.getValue(String.class);
                        if ("BOOKED".equalsIgnoreCase(slotStatus)) {
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Cannot Re-Approve")
                                    .setMessage("This slot is already booked by another request. Re-approval is not possible.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else if ("BLOCKED".equalsIgnoreCase(slotStatus)) {
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Cannot Re-Approve")
                                    .setMessage("This slot is currently blocked. Please unblock it before re-approving.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else {
                            executeReApprove(remark);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // If we can't read the slot, proceed anyway
                        executeReApprove(remark);
                    });
        } else {
            executeReApprove(remark);
        }
    }

    private void executeReApprove(String remark) {
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "approved");
        updates.put("remark", remark);
        updates.put("approvedBy", FirebaseAuth.getInstance().getUid());
        updates.put("hodApproval", true);
        updates.put("decisionTime", currentTime);

        FirebaseDatabase.getInstance().getReference("bookings")
                .child(booking.getBookingId())
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.msg_booking_approved, Toast.LENGTH_SHORT).show();
                    tvFinalStatus.setText(getString(R.string.label_final_decision_prefix, "Approved"));
                    tvFinalStatus.setTextColor(Color.parseColor("#10B981"));
                    btnReApprove.setVisibility(View.GONE);
                    btnCancelBooking.setVisibility(View.VISIBLE);
                    
                    String currentUid = FirebaseAuth.getInstance().getUid();
                    if (currentUid != null) resolveDecisionByName(currentUid);
                    tvDecisionBy.setVisibility(View.VISIBLE);
                    tvDecisionTime.setVisibility(View.VISIBLE);
                    tvDecisionTime.setText(Html.fromHtml("<b>Time:</b> " + currentTime, Html.FROM_HTML_MODE_LEGACY));

                    tvRemarkContent.setVisibility(View.VISIBLE);
                    tvRemarkContent.setText(Html.fromHtml("<b>Remark:</b> " + remark, Html.FROM_HTML_MODE_LEGACY));

                    // Sync slot status to BOOKED
                    repository.updateSlotStatus(booking.getScheduleId(), booking.getTimeSlot(), "BOOKED", r -> {});
                });
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
            if (b.getDate() != null) {
                java.util.Date parsed = sdf.parse(b.getDate());
                if (parsed != null) {
                    bookingDate.setTime(parsed);
                }
            }

            if (bookingDate.before(today)) return true;
            if (bookingDate.after(today)) return false;

            String timeSlot = b.getTimeSlot();
            if (timeSlot == null) return false;
            String normalized = timeSlot.replaceAll("\\s", "");
            String[] parts = normalized.split("[-–]");
            if (parts.length < 2) return false;
            
            String endTimeStr = parts[1];
            int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            int endMinutes = timeToMinutes(endTimeStr);

            return endMinutes != -1 && currentMinutes >= endMinutes;
        } catch (Exception e) { return false; }
    }

    private int timeToMinutes(String time) {
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
        } catch (Exception e) { return -1; }
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
}
