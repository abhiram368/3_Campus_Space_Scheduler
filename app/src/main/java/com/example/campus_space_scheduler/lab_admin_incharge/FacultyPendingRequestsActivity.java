package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.ActivityFacultyPendingRequestsBinding;
import com.example.campus_space_scheduler.lab_admin_incharge.adapters.PendingRequestsAdapter;
import com.example.campus_space_scheduler.lab_admin_incharge.models.Booking;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class FacultyPendingRequestsActivity extends BaseInchargeActivity {

    private ActivityFacultyPendingRequestsBinding binding;
    private PendingRequestsAdapter adapter;
    private List<Booking> bookingList;
    private DatabaseReference databaseReference;
    private String labName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFacultyPendingRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        labName = getIntent().getStringExtra("labName");
        
        // Setup Sidebar using BaseActivity method
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        binding.rvFacultyPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        bookingList = new ArrayList<>();
        
        adapter = new PendingRequestsAdapter(bookingList, new PendingRequestsAdapter.OnActionClickListener() {
            @Override
            public void onItemClick(Booking booking) {
                Intent intent = new Intent(FacultyPendingRequestsActivity.this, FacultyBookingDetailsActivity.class);
                intent.putExtra("bookingId", booking.getBookingId());
                intent.putExtra("labName", labName);
                startActivity(intent);
            }
        });
        
        binding.rvFacultyPendingRequests.setAdapter(adapter);
        
        databaseReference = FirebaseDatabase.getInstance().getReference("bookings");
        fetchRequests();
    }

    private void fetchRequests() {
        binding.facultyProgressBar.setVisibility(View.VISIBLE);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookingList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String spaceName = getStringValue(dataSnapshot, "spaceName");
                    String status = getStringValue(dataSnapshot, "status");

                    // Filter by lab name and the specific status for Faculty Incharge
                    if ("forwarded_to_faculty_incharge".equalsIgnoreCase(status) && spaceName.contains(labName)) {
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
                
                binding.facultyProgressBar.setVisibility(View.GONE);
                binding.tvFacultyNoRequests.setVisibility(bookingList.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.facultyProgressBar.setVisibility(View.GONE);
                Toast.makeText(FacultyPendingRequestsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringValue(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        if (value == null) return "N/A";
        return String.valueOf(value);
    }
}