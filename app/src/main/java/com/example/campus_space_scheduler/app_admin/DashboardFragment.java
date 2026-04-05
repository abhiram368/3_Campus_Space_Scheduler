package com.example.campus_space_scheduler.app_admin;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.AFragmentDashboardBinding;
import com.example.campus_space_scheduler.enums.SlotStatus;
import com.example.campus_space_scheduler.helper.SlotColorMapper;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private AFragmentDashboardBinding binding;
    private DatabaseReference usersRef;
    private DatabaseReference bookingsRef;
    private DatabaseReference schedulesRef;
    private DatabaseReference spacesRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = AFragmentDashboardBinding.inflate(inflater, container, false);

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");
        spacesRef = FirebaseDatabase.getInstance().getReference("spaces");

        binding.btnRefresh.setOnClickListener(v -> refreshDashboard());

        startLiveUpdates();
        refreshDashboard();

        return binding.getRoot();
    }

    private void refreshDashboard() {

        loadSpaceStatuses();

        String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date());

        binding.tvLastRefresh.setText("Last refresh: " + time);
    }

    // ---------- SPACE LIVE STATUS ----------

    private void loadSpaceStatuses() {
        binding.tvLoadingSpaceStatus.setVisibility(View.VISIBLE);
        binding.containerSpaceStatus.setVisibility(View.GONE);

        spacesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot spacesSnap) {


                if (binding == null) return;
                binding.tvLoadingSpaceStatus.setVisibility(View.GONE);
                binding.containerSpaceStatus.setVisibility(View.VISIBLE);
                binding.containerSpaceStatus.removeAllViews();

                String today = new SimpleDateFormat("yyyy-MM-dd",
                        Locale.getDefault()).format(new Date());

                Calendar now = Calendar.getInstance();
                int minute = now.get(Calendar.MINUTE) < 30 ? 0 : 30;
                int hour = now.get(Calendar.HOUR_OF_DAY);

                String slotId = String.format(Locale.getDefault(),
                        "%02d%02d", hour, minute);

                Query todaySchedules =
                        schedulesRef.orderByChild("date").equalTo(today);

                todaySchedules.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot schedSnap) {

                        if (binding == null) return;

                        for (DataSnapshot space : spacesSnap.getChildren()) {

                            String spaceId = space.getKey();
                            String roomName = space.child("roomName").getValue(String.class);
                            if (roomName == null) roomName = "Unknown";

                            SlotStatus currentStatus = SlotStatus.AVAILABLE;

                            for (DataSnapshot schedule : schedSnap.getChildren()) {

                                String date = schedule.child("date").getValue(String.class);
//                                if (!today.equals(date)) continue;

                                String scheduleSpaceId =
                                        schedule.child("spaceId").getValue(String.class);

                                if (scheduleSpaceId == null ||
                                        !scheduleSpaceId.equals(spaceId))
                                    continue;

                                DataSnapshot slot =
                                        schedule.child("slots").child(slotId);

                                if (slot.exists()) {

                                    String statusStr =
                                            slot.child("status").getValue(String.class);

                                    try {
                                        if (statusStr != null) {
                                            try {
                                                currentStatus = SlotStatus.valueOf(statusStr);
                                            } catch (IllegalArgumentException ignored) {}
                                        }
                                    } catch (Exception ignored) {}

                                    break;
                                }
                            }

                            addSpaceStatusView(roomName, currentStatus);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (binding == null) return;
                        binding.tvLoadingSpaceStatus.setText("Failed to load");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding == null) return;
                binding.tvLoadingSpaceStatus.setText("Failed to load");
            }
        });
    }

    // ---------- UI ROW FOR SPACE STATUS ----------

    private void addSpaceStatusView(String name, SlotStatus status) {

        Context ctx = requireContext();

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0,8,0,8);

        View indicator = new View(ctx);

        LinearLayout.LayoutParams dotParams =
                new LinearLayout.LayoutParams(16,16);
        dotParams.setMargins(0,0,16,0);

        indicator.setLayoutParams(dotParams);

        indicator.setBackgroundResource(R.drawable.a_circle_indicator);

        int color = SlotColorMapper.getColor(status);

        indicator.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(color)
        );

        TextView text = new TextView(ctx);

        text.setText(name + " (" + status.name() + ")");
        text.setTextSize(14);
        text.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(indicator);
        row.addView(text);

        binding.containerSpaceStatus.addView(row);
    }

    // ---------- STAT COUNTERS ----------

    private void startLiveUpdates() {

        usersRef.addValueEventListener(createStatListener(binding.tvUserCount));
        bookingsRef.addValueEventListener(createStatListener(binding.tvTotalBookings));

        DatabaseReference connectedRef =
                FirebaseDatabase.getInstance().getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (binding == null) return;

                Boolean connected = snapshot.getValue(Boolean.class);

                binding.tvFirebaseStatus.setText(
                        connected != null && connected ? "Online" : "Offline"
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private ValueEventListener createStatListener(TextView textView) {

        return new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (binding == null) return;

                textView.setText(String.valueOf(snapshot.getChildrenCount()));
                binding.tvDatabaseSync.setText("OK");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (binding == null) return;
                binding.tvDatabaseSync.setText("Error");
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}