package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.ActivityApprovalHistoryBinding;
import com.example.campus_space_scheduler.lab_admin_incharge.adapters.PendingRequestsAdapter;
import com.example.campus_space_scheduler.lab_admin_incharge.models.Booking;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApprovalHistoryActivity extends BaseInchargeActivity {

    private ActivityApprovalHistoryBinding binding;
    private PendingRequestsAdapter adapter;
    private List<Booking> historyList;
    private DatabaseReference databaseReference;
    private final List<String> allowedSpaces = Arrays.asList("CSED Seminar Hall", "CSED Discussion Room", "APJ Hall");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityApprovalHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Sidebar using BaseActivity method
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();
        
        adapter = new PendingRequestsAdapter(historyList, new PendingRequestsAdapter.OnActionClickListener() {
            @Override
            public void onItemClick(Booking booking) {
                Intent intent = new Intent(ApprovalHistoryActivity.this, BookingDetailsActivity.class);
                intent.putExtra("bookingId", booking.getBookingId());
                startActivity(intent);
            }
        });
        
        binding.rvHistory.setAdapter(adapter);
        
        databaseReference = FirebaseDatabase.getInstance().getReference("bookings");
        fetchApprovalHistory();
    }

    private void fetchApprovalHistory() {
        binding.progressBarHistory.setVisibility(View.VISIBLE);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String spaceName = getStringValue(dataSnapshot, "spaceName").trim();
                    String status = getStringValue(dataSnapshot, "status");
                    String dateStr = getStringValue(dataSnapshot, "date");

                    // Hall Incharge History Condition: Approved, Rejected, or forwarded_to_hod
                    if (allowedSpaces.contains(spaceName)) {
                        if ("approved".equalsIgnoreCase(status) || 
                            "rejected".equalsIgnoreCase(status) || 
                            "forwarded_to_hod".equalsIgnoreCase(status) ||
                            "cancelled".equalsIgnoreCase(status)) {
                            
                            Booking booking = new Booking();
                            booking.setBookingId(dataSnapshot.getKey());
                            booking.setSpaceName(spaceName);
                            booking.setStatus(status.toUpperCase());
                            booking.setPurpose(getStringValue(dataSnapshot, "purpose"));
                            booking.setDate(dateStr);
                            booking.setTimeSlot(getStringValue(dataSnapshot, "timeSlot"));
                            booking.setBookedBy(getStringValue(dataSnapshot, "bookedBy"));
                            
                            historyList.add(booking);
                        }
                    }
                }
                
                binding.progressBarHistory.setVisibility(View.GONE);
                binding.tvNoHistory.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBarHistory.setVisibility(View.GONE);
                Toast.makeText(ApprovalHistoryActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringValue(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        if (value == null) return "N/A";
        return String.valueOf(value);
    }
}