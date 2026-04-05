package com.example.hod.staff;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.example.hod.models.Booking;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.hod.models.User;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RequestDetailActivity extends AppCompatActivity {

    private FirebaseRepository repo;
    private Booking booking;
    private String currentUserRole; // To store the role for correct status transitions

    private TextView tvUsername, tvUserRoll, tvUserRole, tvUserEmail, tvUserPhone, tvLabName, tvDateTime, tvBookedDate,
            tvReason, tvLorLink;
    private Button btnApprove, btnReject, btnForward, btnDeleteLateRequest;
    private View bottomActionBar, llExpiredRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);

        repo = new FirebaseRepository();

        // UI Initialization
        tvUsername = findViewById(R.id.tvUsername);
        tvUserRoll = findViewById(R.id.tvUserRoll);
        tvUserRole = findViewById(R.id.tvUserRole);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvLabName = findViewById(R.id.tvLabName);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvBookedDate = findViewById(R.id.tvBookedDate);
        tvReason = findViewById(R.id.tvReason);
        tvLorLink = findViewById(R.id.tvLorLink);

        // Fetch current user details to get their role for correct status mapping
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid != null) {
            repo.getUserDetails(currentUid, result -> {
                if (result instanceof Result.Success) {
                    User u = ((Result.Success<User>) result).data;
                    if (u != null) {
                        currentUserRole = u.role;
                        runOnUiThread(this::updateButtonLabelsBasedOnRole);
                    }
                }
            });
        }

        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        btnForward = findViewById(R.id.btnForward);

        bottomActionBar = findViewById(R.id.bottom_action_bar);
        llExpiredRequest = findViewById(R.id.llExpiredRequest);
        btnDeleteLateRequest = findViewById(R.id.btnDeleteLateRequest);

        // Load booking only ONCE — fix: was being deserialized twice, losing the bookingId
        booking = (Booking) getIntent().getSerializableExtra("booking");
        String bookingIdExtra = getIntent().getStringExtra("bookingId");

        // CRITICAL FIX: bookingId is stored as the Firebase node key, NOT as a field.
        // So it may be null after Serializable deserialization. Use the Intent extra as fallback.
        if (booking != null && (booking.getBookingId() == null || booking.getBookingId().isEmpty())) {
            if (bookingIdExtra != null && !bookingIdExtra.isEmpty()) {
                booking.setBookingId(bookingIdExtra);
            }
        }

        if (booking == null && (bookingIdExtra == null || bookingIdExtra.isEmpty())) {
            Toast.makeText(this, "Error: No booking data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (booking == null) {
            fetchBookingAndLoad(bookingIdExtra);
        } else {
            updateHeader("Request Details", "Pending Approval");
            loadDynamicData();
            setupClickListeners();
            checkExpirationAndAdjustUI();
        }
    }

    private void fetchBookingAndLoad(String bookingId) {
        FirebaseDatabase.getInstance().getReference("bookings").child(bookingId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        booking = snapshot.getValue(Booking.class);
                        if (booking != null) {
                            if (booking.getBookingId() == null) booking.setBookingId(snapshot.getKey());
                            updateHeader("Request Details", "Pending Approval");
                            loadDynamicData();
                            setupClickListeners();
                            checkExpirationAndAdjustUI();
                        } else {
                            Toast.makeText(RequestDetailActivity.this, "Booking parse error", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(RequestDetailActivity.this, "Booking not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    finish();
                }
            });
    }

    private void checkExpirationAndAdjustUI() {
        if (booking == null) {
            bottomActionBar.setVisibility(View.VISIBLE);
            llExpiredRequest.setVisibility(View.GONE);
            return;
        }

        boolean isExpired = (booking != null && booking.isExpired());
        if (isExpired) {
            bottomActionBar.setVisibility(View.GONE);
            llExpiredRequest.setVisibility(View.VISIBLE);

            String status = booking.getStatus();
            String expiredMessage = "Expired";

            if ("forwarded_to_faculty_incharge".equalsIgnoreCase(status)) {
                expiredMessage = "Expired (Forwarded to Faculty Incharge)";
            } else if ("forwarded_to_hod".equalsIgnoreCase(status)) {
                expiredMessage = "Expired (Forwarded to HOD)";
            }

            btnDeleteLateRequest.setText(expiredMessage + "\nDelete this request");
        } else {
            bottomActionBar.setVisibility(View.VISIBLE);
            llExpiredRequest.setVisibility(View.GONE);
        }
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        View btnBack = findViewById(R.id.btnBack);

        if (tvTitle != null)
            tvTitle.setText(title);
        if (tvSubtitle != null)
            tvSubtitle.setText(subtitle);
        if (btnBack != null)
            btnBack.setOnClickListener(v -> finish());
    }

    private void updateButtonLabelsBasedOnRole() {
        if (currentUserRole == null) return;
        
        String roleNorm = currentUserRole.toLowerCase().trim();
        if (roleNorm.contains("staff") || roleNorm.contains("faculty") || roleNorm.contains("admin") || roleNorm.contains("hod")) {
            if (btnApprove != null) {
                btnApprove.setText("Approve");
            }
        }
    }

    private void loadDynamicData() {
        if (booking == null) return;
        
        // Redirection Guard: If already processed or forwarded, go to Completed Detail
        String status = booking.getStatus() != null ? booking.getStatus().toLowerCase() : "";
        if (status.equals("approved") || status.contains("rejected") || status.equals("cancelled")
                || status.equals("booked") || status.equals("used") || status.contains("forwarded")) {
            Intent intent = new Intent(this, StaffCompletedRequestDetailActivity.class);
            intent.putExtra("booking", booking);
            intent.putExtra("bookingId", booking.getBookingId());
            startActivity(intent);
            finish();
            return;
        }

        // 1. Basic booking info
        tvDateTime.setText(booking.getDate() + " | " + booking.getTimeSlot());
        String bookedAt = booking.getBookedTimeDisplay();
        if (bookedAt == null || bookedAt.trim().isEmpty()) {
            bookedAt = booking.getDate() != null ? booking.getDate() : "Not Available";
        }
        if (tvBookedDate != null) {
            tvBookedDate.setText(Html.fromHtml("<b>Requested At:</b> " + bookedAt, Html.FROM_HTML_MODE_LEGACY));
        }
        tvReason.setText(booking.getPurpose() != null ? booking.getPurpose() : "No reason provided");

        String lor = booking.getLorUpload();
        if (lor != null && !lor.isEmpty()) {
            tvLorLink.setText("View Attachment");
            tvLorLink.setTag(lor); // Store actual URL in tag
            tvLorLink.setPaintFlags(tvLorLink.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            tvLorLink.setTextColor(getResources().getColor(R.color.primary_blue));
        } else {
            tvLorLink.setText("No document attached");
            tvLorLink.setPaintFlags(tvLorLink.getPaintFlags() & (~android.graphics.Paint.UNDERLINE_TEXT_FLAG));
            tvLorLink.setTextColor(getResources().getColor(R.color.text_secondary));
            tvLorLink.setClickable(false);
        }

        // 2. Fetch User Name
        if (booking.getBookedBy() != null) {
            repo.getUserDetails(booking.getBookedBy(), result -> {
                if (result instanceof Result.Success) {
                    User u = ((Result.Success<User>) result).data;
                    if (u != null) {
                        tvUsername.setText(u.name != null ? u.name : "Unknown User");
                        tvUserRoll.setText(
                                Html.fromHtml("<b>Roll No:</b> " + (u.rollNo != null ? u.rollNo : "Not provided"),
                                        Html.FROM_HTML_MODE_LEGACY));
                        if (tvUserRole != null)
                            tvUserRole.setText(
                                    Html.fromHtml("<b>Role:</b> " + u.getRoleLabel(), Html.FROM_HTML_MODE_LEGACY));
                        tvUserEmail.setText(
                                Html.fromHtml("<b>Email:</b> " + (u.emailId != null ? u.emailId : "Not provided"),
                                        Html.FROM_HTML_MODE_LEGACY));
                        tvUserPhone.setText(Html.fromHtml(
                                "<b>Contact No:</b> " + (u.phoneNumber != null ? u.phoneNumber : "Not provided"),
                                Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        tvUsername.setText("ID: " + booking.getBookedBy());
                    }
                }
            });
        }

        // 3. Fetch Lab Name (Chain: scheduleId -> spaceId -> spaces)
        if (booking.getScheduleId() != null) {
            repo.getSpaceIdFromSchedule(booking.getScheduleId(), result -> {
                if (result instanceof Result.Success) {
                    String spaceId = ((Result.Success<String>) result).data;
                    if (spaceId != null) {
                        repo.getSpaceDetails(spaceId, spaceResult -> {
                            if (spaceResult instanceof Result.Success) {
                                com.example.hod.models.Space space = ((Result.Success<com.example.hod.models.Space>) spaceResult).data;
                                if (space != null && space.getRoomName() != null) {
                                    tvLabName.setText("Lab: " + space.getRoomName());
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void setupClickListeners() {
        tvLorLink.setOnClickListener(v -> {
            String url = (String) tvLorLink.getTag();
            if (url != null && url.startsWith("http")) {
                try {
                    android.net.Uri uri = android.net.Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot open link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Invalid or missing LOR link", Toast.LENGTH_SHORT).show();
            }
        });

        btnApprove.setOnClickListener(v -> {
            showActionDialog("approved", "Request Approved", "Approve Booking", "Please provide a reason for approval", false);
        });

        btnReject.setOnClickListener(v -> {
            showActionDialog("rejected", "rejected", "Reject Booking", "Please provide a reason for rejection", true);
        });

        btnForward.setOnClickListener(v -> {
            showActionDialog("forwarded_to_faculty_incharge", "forwarded to Faculty Incharge", "Forward Booking",
                    "Please provide a reason for forwarding", true);
        });

        if (btnDeleteLateRequest != null) {
            btnDeleteLateRequest.setOnClickListener(v -> {
                showActionDialog("expired", "Marked as Expired", "Mark as Expired",
                        "Explain why this request is being marked as expired", true);
            });
        }
    }

    private void showActionDialog(String statusValue, String toastMsg, String title, String subtitle,
            boolean isMandatory) {
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

        if (tvTitle != null)
            tvTitle.setText(title);
        if (tvSubtitle != null)
            tvSubtitle.setText(subtitle);
        if (etRemark != null)
            etRemark.setHint("Enter your remark here...");
        if (btnConfirm != null)
            btnConfirm.setText("Submit");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String remark = etRemark.getText().toString().trim();
            if (isMandatory && remark.isEmpty()) {
                etRemark.setError("Remark is required for this action");
            } else {
                updateStatus(statusValue, toastMsg, remark);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void updateStatus(String statusValue, String toastMsg, String remark) {
        String uid = FirebaseAuth.getInstance().getUid();

        // CRITICAL: bookingId might be null if it was not stored as a field in Firebase
        // (Firebase stores it as the node key). Use the bookingIdExtra from the Intent as fallback.
        String resolvedBookingId = booking.getBookingId();
        if (resolvedBookingId == null || resolvedBookingId.isEmpty()) {
            resolvedBookingId = getIntent().getStringExtra("bookingId");
        }
        if (resolvedBookingId == null || resolvedBookingId.isEmpty()) {
            Toast.makeText(this, "Error: Could not resolve booking ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Map raw statusValue to the approved boolean + role expected by updateApprovalStatus
        boolean approved = !statusValue.startsWith("rejected") && !statusValue.equalsIgnoreCase("expired");
        final String finalBookingId = resolvedBookingId;

        repo.updateApprovalStatus(
                finalBookingId,
                currentUserRole != null ? currentUserRole : "staff",
                approved,
                remark,
                uid,
                statusValue,
                result -> runOnUiThread(() -> {
                    if (result instanceof com.example.hod.utils.Result.Success) {
                        // Sync slot to AVAILABLE if rejected or expired
                        if (!approved) {
                            if (booking.getScheduleId() != null && booking.getTimeSlot() != null) {
                                repo.updateSlotStatus(booking.getScheduleId(), booking.getTimeSlot(), "AVAILABLE",
                                        r -> android.util.Log.d("RequestDetail", "Slot synced to AVAILABLE"));
                            }
                        }
                        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }

    private boolean isBookingExpired(Booking b) {
        if (b == null || b.getDate() == null || b.getTimeSlot() == null)
            return false;
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

            if (bookingDate.before(today))
                return true;
            if (bookingDate.after(today))
                return false;

            // Same day: Check time slot start
            String timeSlot = b.getTimeSlot();
            if (timeSlot == null)
                return false;

            String normalized = timeSlot.replaceAll("\\s", "");
            String[] parts = normalized.split("[-–]");
            if (parts.length < 2)
                return false;

            String startTimeStr = parts[0];
            int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            int startMinutes = timeToMinutes(startTimeStr);

            if (startMinutes == -1)
                return false;
            return currentMinutes >= startMinutes;
        } catch (Exception e) {
            return false;
        }
    }

    private int timeToMinutes(String time) {
        if (time == null || time.isEmpty())
            return -1;
        try {
            String upper = time.trim().toUpperCase();
            boolean isPM = upper.contains("PM");
            boolean isAM = upper.contains("AM");

            String cleanTime = upper.replaceAll("[^0-9:]", "");
            String[] parts = cleanTime.split(":");
            if (parts.length < 2)
                return -1;

            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);

            if (isPM && hours < 12)
                hours += 12;
            if (isAM && hours == 12)
                hours = 0;

            return hours * 60 + minutes;
        } catch (Exception e) {
            return -1;
        }
    }
}
