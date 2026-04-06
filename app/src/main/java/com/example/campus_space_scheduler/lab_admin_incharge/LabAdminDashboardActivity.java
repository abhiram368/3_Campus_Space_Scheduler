package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.ActivityLabAdminDashboardBinding;
import com.example.campus_space_scheduler.lab_admin_incharge.adapters.PendingRequestsAdapter;
import com.example.campus_space_scheduler.lab_admin_incharge.models.Booking;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LabAdminDashboardActivity extends BaseInchargeActivity {

    private ActivityLabAdminDashboardBinding binding;
    private String labName;
    private List<Booking> bookingList;
    private PendingRequestsAdapter adapter;
    private DatabaseReference bookingsRef, weeklyScheduleRef;
    private boolean showingToday = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLabAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        labName = getIntent().getStringExtra("labName");
        
        // Setup Sidebar using BaseActivity method
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        if (labName != null) {
            binding.tvLabAdminTitle.setText(labName + " Lab Admin");
        }

        binding.rvLabAdminBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingList = new ArrayList<>();
        adapter = new PendingRequestsAdapter(bookingList, booking -> {
            Intent intent = new Intent(LabAdminDashboardActivity.this, LabAdminBookingDetailsActivity.class);
            intent.putExtra("bookingId", booking.getBookingId());
            intent.putExtra("isToday", showingToday);
            startActivity(intent);
        });
        binding.rvLabAdminBookings.setAdapter(adapter);

        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        // Using SSL as default for schedule if labName is not provided or different logic needed
        weeklyScheduleRef = FirebaseDatabase.getInstance().getReference("labAdminWeeklySchedule").child(labName != null ? labName : "SSL");

        binding.btnToday.setOnClickListener(v -> {
            showingToday = true;
            updateButtonUI();
            fetchBookings();
        });

        binding.btnUpcoming.setOnClickListener(v -> {
            showingToday = false;
            updateButtonUI();
            fetchBookings();
        });

        binding.btnSwitchMode.setOnClickListener(v -> {
            Toast.makeText(this, "Student module is not linked", Toast.LENGTH_SHORT).show();
        });

        binding.btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });

        updateButtonUI();
        fetchBookings();
    }

    private void updateButtonUI() {
        if (showingToday) {
            binding.btnToday.setAlpha(1.0f);
            binding.btnUpcoming.setAlpha(0.6f);
        } else {
            binding.btnToday.setAlpha(0.6f);
            binding.btnUpcoming.setAlpha(1.0f);
        }
    }

    private void fetchBookings() {
        binding.pbLabAdmin.setVisibility(View.VISIBLE);
        binding.tvNoLabAdminBookings.setVisibility(View.GONE);
        bookingList.clear();
        adapter.notifyDataSetChanged();

        weeklyScheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot weeklySnap) {
                bookingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot bookingsSnap) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String todayStr = sdf.format(new Date());

                        for (DataSnapshot snapshot : bookingsSnap.getChildren()) {
                            String spaceName = String.valueOf(snapshot.child("spaceName").getValue());
                            String dateStr = String.valueOf(snapshot.child("date").getValue());
                            String status = String.valueOf(snapshot.child("status").getValue());
                            String timeSlot = String.valueOf(snapshot.child("timeSlot").getValue());

                            if (spaceName.equalsIgnoreCase(labName) && "approved".equalsIgnoreCase(status)) {
                                boolean matchesTimeFilter = false;
                                if (showingToday) {
                                    if (dateStr.equals(todayStr)) matchesTimeFilter = true;
                                } else {
                                    if (dateStr.compareTo(todayStr) > 0) matchesTimeFilter = true;
                                }

                                if (matchesTimeFilter) {
                                    if (weeklySnap.exists()) {
                                        if (isBookingInAdminShift(weeklySnap, dateStr, timeSlot)) {
                                            addBookingToList(snapshot);
                                        }
                                    } else {
                                        addBookingToList(snapshot);
                                    }
                                }
                            }
                        }
                        binding.pbLabAdmin.setVisibility(View.GONE);
                        binding.tvNoLabAdminBookings.setVisibility(bookingList.isEmpty() ? View.VISIBLE : View.GONE);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.pbLabAdmin.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.pbLabAdmin.setVisibility(View.GONE);
            }
        });
    }

    private void addBookingToList(DataSnapshot snapshot) {
        Booking booking = new Booking();
        booking.setBookingId(snapshot.getKey());
        booking.setSpaceName(String.valueOf(snapshot.child("spaceName").getValue()));
        booking.setStatus(String.valueOf(snapshot.child("status").getValue()));
        booking.setPurpose(String.valueOf(snapshot.child("purpose").getValue()));
        booking.setDate(String.valueOf(snapshot.child("date").getValue()));
        booking.setTimeSlot(String.valueOf(snapshot.child("timeSlot").getValue()));
        booking.setBookedBy(String.valueOf(snapshot.child("bookedBy").getValue()));
        bookingList.add(booking);
    }

    private boolean isBookingInAdminShift(DataSnapshot weeklySnap, String dateStr, String bookingTimeSlot) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            SimpleDateFormat daySdf = new SimpleDateFormat("EEEE", Locale.getDefault());
            String dayOfWeek = daySdf.format(date);

            DataSnapshot daySnap = weeklySnap.child(dayOfWeek);
            if (!daySnap.exists()) return false;

            String[] bParts = bookingTimeSlot.replace(" ", "").split("-");
            if (bParts.length != 2) return false;
            
            SimpleDateFormat tSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date bStart = tSdf.parse(bParts[0]);
            Date bEnd = tSdf.parse(bParts[1]);

            for (DataSnapshot slotSnap : daySnap.getChildren()) {
                String shiftKey = slotSnap.getKey().replace(" ", "");
                String[] sParts = shiftKey.split("-");
                if (sParts.length != 2) continue;

                Date sStart = tSdf.parse(sParts[0]);
                Date sEnd = tSdf.parse(sParts[1]);

                if ((bStart.after(sStart) || bStart.equals(sStart)) && (bEnd.before(sEnd) || bEnd.equals(sEnd))) {
                    return true;
                }
            }
        } catch (ParseException e) {
            Log.e("LabAdmin", "Time parsing error", e);
        } catch (Exception e) {
            Log.e("LabAdmin", "General error", e);
        }
        return false;
    }
}