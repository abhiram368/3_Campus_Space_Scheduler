package com.example.hod.hod;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.campussync.appy.R;
import com.example.hod.models.LiveStatusData;

public class HodLabSlotDetailActivity extends AppCompatActivity {

    private TextView tvStatus, tvCurrentSlot, tvRequester, tvPurpose, tvLabName;
    private LinearLayout bookedDetailsLayout;
    private View pulseView, statusDot;
    private LiveStatusData slotData;
    private String spaceName;
    private String date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_lab_slot_detail);

        // Header Configuration
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            TextView title = headerView.findViewById(R.id.header_title);
            View btnBack = headerView.findViewById(R.id.btnBack);
            if (title != null) title.setText("Slot Details");
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }

        tvStatus = findViewById(R.id.tvStatus);
        tvCurrentSlot = findViewById(R.id.tvCurrentSlot);
        tvRequester = findViewById(R.id.tvBookedByUser);
        tvPurpose = findViewById(R.id.tvBookingPurpose);
        tvLabName = findViewById(R.id.tvLabName);
        bookedDetailsLayout = findViewById(R.id.bookedDetailsLayout);
        pulseView = findViewById(R.id.pulse_view);
        statusDot = findViewById(R.id.status_dot);

        slotData = (LiveStatusData) getIntent().getSerializableExtra("slotData");
        spaceName = getIntent().getStringExtra("spaceName");
        date = getIntent().getStringExtra("date");

        if (slotData == null) {
            finish();
            return;
        }

        updateHeader("Slot Details", date != null ? date : "");

        tvLabName.setText(spaceName != null ? spaceName : "Space");

        String timeText;
        if (slotData.startTime != null && slotData.endTime != null) {
            timeText = formatTime(slotData.startTime) + " – " + formatTime(slotData.endTime);
        } else {
            timeText = slotData.slotKey != null ? formatSlotKey(slotData.slotKey) : "Unknown Slot";
        }
        tvCurrentSlot.setText("Slot: " + timeText);

        updateUI();
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
    }

    private String formatTime(String time) {
        if (time == null) return "";
        if (time.length() == 4 && !time.contains(":")) {
            return time.substring(0, 2) + ":" + time.substring(2);
        }
        return time;
    }

    private String formatSlotKey(String key) {
        if (key == null || key.length() < 4) return key;
        String digits = key.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return key;
        int startHour = Integer.parseInt(digits.substring(0, 2));
        int startMin = Integer.parseInt(digits.substring(2, 4));
        int endMin = startMin + 30;
        int endHour = startHour;
        if (endMin >= 60) {
            endMin -= 60;
            endHour += 1;
        }
        return String.format("%02d:%02d - %02d:%02d", startHour, startMin, endHour, endMin);
    }

    private void updateUI() {
        String status = slotData.status != null ? slotData.status.toUpperCase() : "AVAILABLE";
        tvStatus.setText(status);

        int colorPrimary;
        int colorBackground;

        switch (status) {
            case "AVAILABLE":
            case "UNUSED":
                colorPrimary = Color.parseColor("#10B981"); // Green
                colorBackground = Color.parseColor("#D1FAE5");
                bookedDetailsLayout.setVisibility(View.GONE);
                break;
            case "BOOKED":
            case "USED":
            case "COMPLETED":
                colorPrimary = Color.parseColor("#3B82F6"); // Blue
                colorBackground = Color.parseColor("#DBEAFE");
                bookedDetailsLayout.setVisibility(View.VISIBLE);
                if (slotData.booking != null) {
                    tvRequester.setText(slotData.booking.getRequesterName() != null ? slotData.booking.getRequesterName() : slotData.booking.getBookedBy());
                    tvPurpose.setText(slotData.booking.getPurpose() != null ? slotData.booking.getPurpose() : "N/A");
                } else {
                    tvRequester.setText("Unknown User");
                    tvPurpose.setText("-");
                }
                break;
            case "BLOCKED":
                colorPrimary = Color.parseColor("#EF4444"); // Red
                colorBackground = Color.parseColor("#FEE2E2");
                bookedDetailsLayout.setVisibility(View.GONE);
                break;
            default:
                colorPrimary = Color.parseColor("#6B7280"); // Gray
                colorBackground = Color.parseColor("#F3F4F6");
                bookedDetailsLayout.setVisibility(View.GONE);
                break;
        }

        tvStatus.setTextColor(colorPrimary);
        
        android.graphics.drawable.GradientDrawable dotDrawable = new android.graphics.drawable.GradientDrawable();
        dotDrawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        dotDrawable.setColor(colorPrimary);
        statusDot.setBackground(dotDrawable);

        android.graphics.drawable.GradientDrawable pulseDrawable = new android.graphics.drawable.GradientDrawable();
        pulseDrawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        pulseDrawable.setColor(colorBackground);
        pulseView.setBackground(pulseDrawable);

        // Add simple breathing animation
        pulseView.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .alpha(0.1f)
                .setDuration(1500)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        pulseView.animate()
                                .scaleX(0.8f)
                                .scaleY(0.8f)
                                .alpha(0.5f)
                                .setDuration(1500)
                                .withEndAction(this)
                                .start();
                    }
                })
                .start();
                
        // Hide the 'updates automatically' text since this is static details
        TextView tvUpdates = findViewById(R.id.tvUpdatesText);
        if (tvUpdates == null) {
            // Find the text view by its text if no ID is present
            for (int i = 0; i < ((LinearLayout) findViewById(R.id.pulse_view).getParent().getParent()).getChildCount(); i++) {
                View child = ((LinearLayout) findViewById(R.id.pulse_view).getParent().getParent()).getChildAt(i);
                if (child instanceof TextView && ((TextView) child).getText().toString().contains("Updates automatically")) {
                    child.setVisibility(View.GONE);
                }
            }
        } else {
            tvUpdates.setVisibility(View.GONE);
        }
    }
}
