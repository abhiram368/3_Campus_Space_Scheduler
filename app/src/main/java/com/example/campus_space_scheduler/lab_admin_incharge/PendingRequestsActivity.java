package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.ActivityPendingRequestsBinding;
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

public class PendingRequestsActivity extends BaseInchargeActivity {

    private ActivityPendingRequestsBinding binding;
    private PendingRequestsAdapter adapter;
    private List<Booking> bookingList;
    private DatabaseReference databaseReference;
    private final List<String> allowedSpaces = Arrays.asList("CSED Seminar Hall", "CSED Discussion Room", "APJ Hall");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Sidebar using BaseActivity method
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        binding.rvPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        bookingList = new ArrayList<>();
        
        adapter = new PendingRequestsAdapter(bookingList, new PendingRequestsAdapter.OnActionClickListener() {
            @Override
            public void onItemClick(Booking booking) {
                Intent intent = new Intent(PendingRequestsActivity.this, BookingDetailsActivity.class);
                intent.putExtra("bookingId", booking.getBookingId());
                startActivity(intent);
            }
        });
        
        binding.rvPendingRequests.setAdapter(adapter);
        
        databaseReference = FirebaseDatabase.getInstance().getReference("bookings");
        fetchPendingRequests();
    }

    private void fetchPendingRequests() {
        binding.progressBar.setVisibility(View.VISIBLE);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookingList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String spaceName = getStringValue(dataSnapshot, "spaceName").trim();
                    String status = getStringValue(dataSnapshot, "status").trim();

                    if ("Pending".equalsIgnoreCase(status) && allowedSpaces.contains(spaceName)) {
                        Booking booking = new Booking();
                        booking.setBookingId(dataSnapshot.getKey());
                        booking.setSpaceName(spaceName);
                        booking.setStatus(status);
                        booking.setPurpose(getStringValue(dataSnapshot, "purpose"));
                        booking.setDate(getStringValue(dataSnapshot, "date"));
                        booking.setTimeSlot(getStringValue(dataSnapshot, "timeSlot"));
                        booking.setBookedBy(getStringValue(dataSnapshot, "bookedBy"));
                        
                        bookingList.add(booking);
                    }
                }
                
                binding.progressBar.setVisibility(View.GONE);
                binding.tvNoRequests.setVisibility(bookingList.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(PendingRequestsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringValue(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        if (value == null) return "N/A";
        return String.valueOf(value);
    }
}