package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_space_scheduler.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class FacultyViewScheduleActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private RecyclerView rvSlots;
    private TextView tvSelectedInfo, tvTitle;
    
    private String selectedSpaceId = "";
    private String selectedDate = "";
    private String labName;
    
    private DatabaseReference spacesRef, schedulesRef, bookingsRef, usersRef;
    private List<Slot> slotList = new ArrayList<>();
    private SlotAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_view_schedule);

        labName = getIntent().getStringExtra("labName");
        // Try to get spaceId directly from Intent first (passed from MainActivity)
        selectedSpaceId = getIntent().getStringExtra("spaceId");
        
        calendarView = findViewById(R.id.facultyCalendarView);
        rvSlots = findViewById(R.id.rvFacultySlots);
        tvSelectedInfo = findViewById(R.id.tvFacultySelectedInfo);
        tvTitle = findViewById(R.id.tvFacultyViewScheduleTitle);

        if (labName != null) {
            tvTitle.setText(labName + " Schedule");
        }

        rvSlots.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SlotAdapter(slotList);
        rvSlots.setAdapter(adapter);

        spacesRef = FirebaseDatabase.getInstance().getReference("spaces");
        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        selectedDate = sdf.format(new java.util.Date());

        if (selectedSpaceId == null || selectedSpaceId.isEmpty()) {
            loadLabSpaceId();
        } else {
            updateSelectedText();
            fetchSlots();
        }

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            updateSelectedText();
            fetchSlots();
        });
    }

    private void loadLabSpaceId() {
        spacesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;
                for (DataSnapshot spaceSnap : snapshot.getChildren()) {
                    String name = String.valueOf(spaceSnap.child("roomName").getValue());
                    if (name.equalsIgnoreCase(labName)) {
                        selectedSpaceId = spaceSnap.getKey();
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    Toast.makeText(FacultyViewScheduleActivity.this, "Could not find space ID for: " + labName, Toast.LENGTH_LONG).show();
                } else {
                    updateSelectedText();
                    fetchSlots();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FacultyViewScheduleActivity.this, "Error loading space", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSelectedText() {
        tvSelectedInfo.setText(labName + "\nDate: " + selectedDate);
    }

    private void fetchSlots() {
        if (selectedSpaceId == null || selectedSpaceId.isEmpty()) return;
        
        String scheduleId = selectedSpaceId + "_" + selectedDate;
        schedulesRef.child(scheduleId).child("slots").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                slotList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot slotSnap : snapshot.getChildren()) {
                        String start = String.valueOf(slotSnap.child("start").getValue());
                        String end = String.valueOf(slotSnap.child("end").getValue());
                        String status = String.valueOf(slotSnap.child("status").getValue());
                        String slotKey = slotSnap.getKey();
                        slotList.add(new Slot(slotKey, start, end, status));
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void handleSlotClick(Slot slot) {
        String scheduleId = selectedSpaceId + "_" + selectedDate;
        if (slot.status == null || slot.status.isEmpty() || "AVAILABLE".equalsIgnoreCase(slot.status)) {
            new AlertDialog.Builder(this)
                .setTitle("Slot Management")
                .setMessage("This slot is currently available. Do you want to block it for maintenance?")
                .setPositiveButton("Block for Maintenance", (dialog, which) -> {
                    schedulesRef.child(scheduleId).child("slots").child(slot.key).child("status").setValue("BLOCKED")
                        .addOnSuccessListener(aVoid -> Toast.makeText(FacultyViewScheduleActivity.this, "Slot blocked for maintenance", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(FacultyViewScheduleActivity.this, "Failed to block slot", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
        } else if ("BLOCKED".equalsIgnoreCase(slot.status)) {
            // Check if it's blocked by a booking or manually
            bookingsRef.orderByChild("scheduleId").equalTo(scheduleId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    DataSnapshot foundBooking = null;
                    for (DataSnapshot b : snapshot.getChildren()) {
                        String slotStart = String.valueOf(b.child("slotStart").getValue());
                        String bookingStatus = String.valueOf(b.child("status").getValue());
                        if (slot.key.equals(slotStart) && "Approved".equalsIgnoreCase(bookingStatus)) {
                            foundBooking = b;
                            break;
                        }
                    }

                    if (foundBooking != null) {
                        showBookingDetailsDialog(foundBooking, slot, scheduleId);
                    } else {
                        new AlertDialog.Builder(FacultyViewScheduleActivity.this)
                            .setTitle("Slot Blocked")
                            .setMessage("This slot is manually blocked for maintenance.")
                            .setPositiveButton("Unblock", (dialog, which) -> {
                                schedulesRef.child(scheduleId).child("slots").child(slot.key).child("status").setValue("AVAILABLE");
                            })
                            .setNegativeButton("Close", null)
                            .show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void showBookingDetailsDialog(DataSnapshot bookingSnap, Slot slot, String scheduleId) {
        String userId = "";
        Object bookedByObj = bookingSnap.child("bookedBy").getValue();
        if (bookedByObj instanceof String) {
            userId = (String) bookedByObj;
        } else if (bookingSnap.child("bookedBy").hasChild("uid")) {
            userId = String.valueOf(bookingSnap.child("bookedBy").child("uid").getValue());
        }

        String purpose = String.valueOf(bookingSnap.child("purpose").getValue());
        String bookingId = bookingSnap.getKey();

        if (userId != null && !userId.isEmpty()) {
            usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot userSnap) {
                    String name = String.valueOf(userSnap.child("name").getValue());
                    String role = String.valueOf(userSnap.child("role").getValue());

                    new AlertDialog.Builder(FacultyViewScheduleActivity.this)
                        .setTitle("Booking Details")
                        .setMessage("Booked By: " + name + " (" + role + ")\nPurpose: " + purpose + "\nSlot: " + slot.time)
                        .setPositiveButton("Cancel Booking", (dialog, which) -> {
                            showCancelRemarksDialog(bookingId, scheduleId, slot.key);
                        })
                        .setNegativeButton("Close", null)
                        .show();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void showCancelRemarksDialog(String bookingId, String scheduleId, String slotKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancel Booking");
        builder.setMessage("Please enter the reason for cancellation:");

        final android.widget.EditText input = new android.widget.EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Confirm Cancellation", (dialog, which) -> {
            String remarks = input.getText().toString();
            if (remarks.isEmpty()) {
                Toast.makeText(this, "Remarks are required to cancel", Toast.LENGTH_SHORT).show();
            } else {
                schedulesRef.child(scheduleId).child("slots").child(slotKey).child("status").setValue("AVAILABLE");
                
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("status", "Cancelled");
                updates.put("remarks", remarks);
                updates.put("facultyInchargeApproval", true);
                
                bookingsRef.child(bookingId).updateChildren(updates);
                Toast.makeText(FacultyViewScheduleActivity.this, "Booking cancelled", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Back", null);
        builder.show();
    }

    private static class Slot {
        String key, time, status;
        Slot(String key, String start, String end, String status) {
            this.key = key;
            this.time = start + " - " + end;
            this.status = status;
        }
    }

    private class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.ViewHolder> {
        private List<Slot> slots;
        SlotAdapter(List<Slot> slots) { this.slots = slots; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slot, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Slot slot = slots.get(position);
            holder.tvTime.setText(slot.time);
            holder.tvStatus.setText(slot.status != null ? slot.status : "AVAILABLE");
            if (slot.status == null || "AVAILABLE".equalsIgnoreCase(slot.status)) {
                holder.tvStatus.setTextColor(getColor(R.color.green_icon));
            } else {
                holder.tvStatus.setTextColor(getColor(R.color.yellow_icon));
            }
            holder.itemView.setOnClickListener(v -> handleSlotClick(slot));
        }

        @Override
        public int getItemCount() { return slots.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvStatus;
            ViewHolder(View v) {
                super(v);
                tvTime = v.findViewById(R.id.tvSlotTime);
                tvStatus = v.findViewById(R.id.tvSlotStatus);
            }
        }
    }
}