package com.example.campus_space_scheduler.lab_admin_incharge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.lab_admin_incharge.adapters.PendingRequestsAdapter;
import com.example.campus_space_scheduler.lab_admin_incharge.models.Booking;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FacultyApprovalHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private PendingRequestsAdapter adapter;
    private List<Booking> historyList;
    private DatabaseReference databaseReference;
    private ProgressBar progressBar;
    private TextView tvNoHistory, tvTitle;
    private String labName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_approval_history);

        labName = getIntent().getStringExtra("labName");
        
        rvHistory = findViewById(R.id.rvFacultyHistory);
        progressBar = findViewById(R.id.facultyProgressBarHistory);
        tvNoHistory = findViewById(R.id.tvFacultyNoHistory);
        tvTitle = findViewById(R.id.tvFacultyHistoryTitle);
        
        if (labName != null) {
            tvTitle.setText(labName + " History");
        }
        
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();
        
        adapter = new PendingRequestsAdapter(historyList, new PendingRequestsAdapter.OnActionClickListener() {
            @Override
            public void onItemClick(Booking booking) {
                Intent intent = new Intent(FacultyApprovalHistoryActivity.this, FacultyBookingDetailsActivity.class);
                intent.putExtra("bookingId", booking.getBookingId());
                intent.putExtra("labName", labName);
                startActivity(intent);
            }
        });
        
        rvHistory.setAdapter(adapter);
        
        databaseReference = FirebaseDatabase.getInstance().getReference("bookings");
        fetchHistory();
    }

    private void fetchHistory() {
        progressBar.setVisibility(View.VISIBLE);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String spaceName = getStringValue(dataSnapshot, "spaceName");
                    Object approvalObj = dataSnapshot.child("facultyInchargeApproval").getValue();
                    String status = getStringValue(dataSnapshot, "status");
                    String dateStr = getStringValue(dataSnapshot, "date");

                    // Convert approvalObj to boolean check
                    boolean isApprovedByFaculty = false;
                    if (approvalObj instanceof Boolean) {
                        isApprovedByFaculty = (Boolean) approvalObj;
                    } else if (approvalObj instanceof String) {
                        isApprovedByFaculty = "true".equalsIgnoreCase((String) approvalObj) || "approved".equalsIgnoreCase((String) approvalObj);
                    }

                    // Condition: Lab Name match + facultyInchargeApproval is true
                    // + status is Approved, Rejected, forwarded_to_hod, or Cancelled
                    if (spaceName.contains(labName) && isApprovedByFaculty) {
                        if ("Approved".equalsIgnoreCase(status) || 
                            "Rejected".equalsIgnoreCase(status) || 
                            "forwarded_to_hod".equalsIgnoreCase(status) ||
                            "Cancelled".equalsIgnoreCase(status)) {
                            
                            Booking booking = new Booking();
                            booking.setBookingId(dataSnapshot.getKey());
                            booking.setSpaceName(spaceName);
                            // Display status nicely: replace underscores and capitalize
                            booking.setStatus(status.toUpperCase().replace("_", " "));
                            booking.setPurpose(getStringValue(dataSnapshot, "purpose"));
                            booking.setDate(dateStr);
                            booking.setTimeSlot(getStringValue(dataSnapshot, "timeSlot"));
                            booking.setBookedBy(getStringValue(dataSnapshot, "bookedBy"));
                            
                            historyList.add(booking);
                        }
                    }
                }
                
                progressBar.setVisibility(View.GONE);
                tvNoHistory.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(FacultyApprovalHistoryActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringValue(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        if (value == null) return "N/A";
        return String.valueOf(value);
    }
}