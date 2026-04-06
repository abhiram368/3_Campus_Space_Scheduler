package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewScheduleActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private RecyclerView rvSlots;
    private TextView tvSelectedInfo;
    private MaterialButton btnCsedSeminar, btnCsedDiscussion, btnApjHall;
    
    private String selectedSpaceId = "";
    private String selectedDate = "";
    private String selectedSpaceName = "CSED Seminar Hall";
    
    private DatabaseReference spacesRef, schedulesRef, bookingsRef, usersRef;
    private List<Slot> slotList = new ArrayList<>();
    private SlotAdapter adapter;
    
    private Map<String, String> spaceNameToId = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_schedule);

        calendarView = findViewById(R.id.calendarView);
        rvSlots = findViewById(R.id.rvSlots);
        tvSelectedInfo = findViewById(R.id.tvSelectedInfo);
        btnCsedSeminar = findViewById(R.id.btnCsedSeminar);
        btnCsedDiscussion = findViewById(R.id.btnCsedDiscussion);
        btnApjHall = findViewById(R.id.btnApjHall);

        rvSlots.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SlotAdapter(slotList);
        rvSlots.setAdapter(adapter);

        spacesRef = FirebaseDatabase.getInstance().getReference("spaces");
        schedulesRef = FirebaseDatabase.getInstance().getReference("schedules");
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Set default date to today
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        selectedDate = sdf.format(new java.util.Date());

        loadSpaceIds();

        btnCsedSeminar.setOnClickListener(v -> selectSpace("CSED Seminar Hall"));
        btnCsedDiscussion.setOnClickListener(v -> selectSpace("CSED Discussion Room"));
        btnApjHall.setOnClickListener(v -> selectSpace("APJ Hall"));

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            updateSelectedText();
            fetchSlots();
        });
    }

    private void loadSpaceIds() {
        spacesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                spaceNameToId.clear();
                for (DataSnapshot spaceSnap : snapshot.getChildren()) {
                    Object nameObj = spaceSnap.child("roomName").getValue();
                    if (nameObj != null) {
                        String name = String.valueOf(nameObj).trim();
                        String id = spaceSnap.getKey();
                        spaceNameToId.put(name, id);
                    }
                }
                selectSpace(selectedSpaceName);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewScheduleActivity.this, "Error loading spaces", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectSpace(String name) {
        selectedSpaceName = name;
        selectedSpaceId = spaceNameToId.get(name);
        updateSelectedText();
        fetchSlots();
        
        float active = 1.0f;
        float inactive = 0.4f;
        btnCsedSeminar.setAlpha(name.equals("CSED Seminar Hall") ? active : inactive);
        btnCsedDiscussion.setAlpha(name.equals("CSED Discussion Room") ? active : inactive);
        btnApjHall.setAlpha(name.equals("APJ Hall") ? active : inactive);
    }

    private void updateSelectedText() {
        tvSelectedInfo.setText(selectedSpaceName + "\nDate: " + selectedDate);
    }

    private void fetchSlots() {
        if (selectedSpaceId == null || selectedSpaceId.isEmpty()) {
            slotList.clear();
            adapter.notifyDataSetChanged();
            return;
        }
        
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
            String[] options = {"Block for Maintenance", "Cancel"};
            new AlertDialog.Builder(this)
                .setTitle("Manage Slot")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        schedulesRef.child(scheduleId).child("slots").child(slot.key).child("status").setValue("BLOCKED");
                        Toast.makeText(this, "Slot blocked for maintenance", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
        } else if ("BLOCKED".equalsIgnoreCase(slot.status)) {
            bookingsRef.orderByChild("scheduleId").equalTo(scheduleId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    DataSnapshot foundBooking = null;
                    for (DataSnapshot b : snapshot.getChildren()) {
                        String slotStart = String.valueOf(b.child("slotStart").getValue());
                        if (slot.key.equals(slotStart)) {
                            foundBooking = b;
                            break;
                        }
                    }

                    if (foundBooking != null) {
                        showBookingDetailsDialog(foundBooking, slot, scheduleId);
                    } else {
                        new AlertDialog.Builder(ViewScheduleActivity.this)
                            .setTitle("Slot Blocked")
                            .setMessage("This slot is under maintenance.")
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
        String userId = String.valueOf(bookingSnap.child("bookedBy").getValue());
        String purpose = String.valueOf(bookingSnap.child("purpose").getValue());
        String bookingId = bookingSnap.getKey();

        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnap) {
                String name = String.valueOf(userSnap.child("name").getValue());
                String role = String.valueOf(userSnap.child("role").getValue());

                new AlertDialog.Builder(ViewScheduleActivity.this)
                    .setTitle("Booking Details")
                    .setMessage("Booked By: " + name + " (" + role + ")\nPurpose: " + purpose + "\nSlot: " + slot.time)
                    .setPositiveButton("Cancel Booking", (dialog, which) -> {
                        schedulesRef.child(scheduleId).child("slots").child(slot.key).child("status").setValue("AVAILABLE");
                        bookingsRef.child(bookingId).child("status").setValue("cancelled");
                        Toast.makeText(ViewScheduleActivity.this, "Booking cancelled", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Close", null)
                    .show();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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