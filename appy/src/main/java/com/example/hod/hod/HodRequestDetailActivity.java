package com.example.hod.hod;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.campussync.appy.R;
import com.example.hod.models.Booking;
import com.example.hod.models.User;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HodRequestDetailActivity extends AppCompatActivity {

    private TextView tvUsername, tvUserRoll, tvUserEmail, tvUserPhone, tvLabName, tvSlotDisplay, tvReason, tvLorLink;
    private Button btnApprove, btnReject, btnForward, btnViewDocuments, btnDeleteLateRequest;
    private View bottomActionBar, llExpiredRequest;
    private ProgressBar progressBar;

    private FirebaseRepository repository;
    private Booking booking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_request_detail);

        tvUsername = findViewById(R.id.tvUsername);
        tvUserRoll = findViewById(R.id.tvUserRoll);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvLabName = findViewById(R.id.tvLabName);
        tvSlotDisplay = findViewById(R.id.tvSlotDisplay);
        tvReason = findViewById(R.id.tvReason);
        tvLorLink = findViewById(R.id.tvLorLink);

        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        btnViewDocuments = findViewById(R.id.btnViewDocuments);
        btnDeleteLateRequest = findViewById(R.id.btnDeleteLateRequest);
        
        bottomActionBar = findViewById(R.id.bottom_action_bar);
        llExpiredRequest = findViewById(R.id.llExpiredRequest);
        progressBar = findViewById(R.id.progressBar);

        repository = new FirebaseRepository();
        booking = (Booking) getIntent().getSerializableExtra("booking");

        if (booking == null) {
            Toast.makeText(this, "Error: No booking data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        updateHeader(getString(R.string.request_detail_title), getString(R.string.pending_hod_approval_subtitle));
        loadDynamicData();
        setupClickListeners();
        checkExpirationAndAdjustUI();
    }

    private void checkExpirationAndAdjustUI() {
        if (isBookingExpired(booking)) {
            if (bottomActionBar != null) bottomActionBar.setVisibility(View.GONE);
            if (llExpiredRequest != null) llExpiredRequest.setVisibility(View.VISIBLE);
        } else {
            if (bottomActionBar != null) bottomActionBar.setVisibility(View.VISIBLE);
            if (llExpiredRequest != null) llExpiredRequest.setVisibility(View.GONE);
        }
    }

    private void loadDynamicData() {
        tvLabName.setText(booking.getSpaceName() != null ? booking.getSpaceName() : "Unknown Lab");
        tvSlotDisplay.setText(booking.getDate() + " | " + booking.getTimeSlot());
        tvReason.setText(booking.getPurpose() != null ? booking.getPurpose() : "No reason provided");

        if (booking.getLorUpload() != null && !booking.getLorUpload().isEmpty()) {
            tvLorLink.setVisibility(View.VISIBLE);
            tvLorLink.setOnClickListener(v -> openUrl(booking.getLorUpload()));
        }

        // Fetch User Details
        if (booking.getBookedBy() != null) {
            repository.getUserDetails(booking.getBookedBy(), result -> {
                if (result instanceof Result.Success) {
                    User u = ((Result.Success<User>) result).data;
                    if (u != null) {
                        tvUsername.setText(u.name != null ? u.name : "Unknown User");
                        tvUserRoll.setText("Roll No: " + (u.rollNo != null ? u.rollNo : "Not provided"));
                        tvUserEmail.setText("Email: " + (u.emailId != null ? u.emailId : "Not provided"));
                        tvUserPhone.setText("Contact No: " + (u.phoneNumber != null ? u.phoneNumber : "Not provided"));
                    } else {
                        tvUsername.setText("ID: " + booking.getBookedBy());
                    }
                }
            });
        }
    }

    private void setupClickListeners() {
        btnApprove.setOnClickListener(v -> {
            showActionDialog(true, "Approve Booking", "Please provide a reason for approval", false);
        });

        btnReject.setOnClickListener(v -> {
            showActionDialog(false, "Reject Booking", "Please provide a reason for rejection", true);
        });

        btnViewDocuments.setOnClickListener(v -> {
            if (booking.getLorUpload() != null && !booking.getLorUpload().isEmpty()) {
                openUrl(booking.getLorUpload());
            } else {
                Toast.makeText(this, getString(R.string.label_no_document), Toast.LENGTH_SHORT).show();
            }
        });

        if (btnDeleteLateRequest != null) {
            btnDeleteLateRequest.setOnClickListener(v -> {
                showActionDialog(false, "Delete Request", "Reason for Deletion", true);
            });
        }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_cannot_open_link), Toast.LENGTH_SHORT).show();
        }
    }

    private void showActionDialog(boolean approved, String title, String subtitle, boolean isMandatory) {
        View dialogView = getLayoutInflater().inflate(R.layout.layout_cancel_booking_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        EditText etRemark = dialogView.findViewById(R.id.et_cancel_remark);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
        if (etRemark != null) etRemark.setHint(getString(R.string.hint_enter_remark_mandatory));
        if (btnConfirm != null) btnConfirm.setText("Submit");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String remark = etRemark.getText().toString().trim();
            if (isMandatory && remark.isEmpty()) {
                etRemark.setError("Remark is required for this action");
            } else {
                updateApproval(approved, remark);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void updateApproval(boolean approved, String remark) {
        setLoading(true);
        repository.updateApprovalStatus(booking.getBookingId(), "hod", approved, remark, FirebaseAuth.getInstance().getUid(), result -> runOnUiThread(() -> {
            setLoading(false);
            if (result instanceof Result.Success) {
                if (!approved && isBookingExpired(booking)) {
                    // Logic for deleting/expiring late request if needed
                    FirebaseDatabase.getInstance().getReference("bookings").child(booking.getBookingId()).child("status").setValue("rejected_expired");
                }
                Toast.makeText(this, approved ? "Request Approved" : "Request Updated", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update approval.", Toast.LENGTH_SHORT).show();
            }
        }));
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

            String timeSlot = b.getTimeSlot();
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

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnApprove.setEnabled(!loading);
        btnReject.setEnabled(!loading);
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

