package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LabLiveStatusActivity extends AppCompatActivity {

    private TextView tvStatus, tvCurrentSlot, tvBookedBy;
    private LinearLayout bookedDetailsLayout;
    private DatabaseReference mDatabase;
    private static final String LAB_ID = "CS-101";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_live_status);

        tvStatus = findViewById(R.id.tvStatus);
        tvCurrentSlot = findViewById(R.id.tvCurrentSlot);
        tvBookedBy = findViewById(android.R.id.text1); 
        bookedDetailsLayout = findViewById(R.id.bookedDetailsLayout);

        mDatabase = FirebaseDatabase.getInstance().getReference("live_status").child(LAB_ID);

        fetchLiveStatus();
    }

    private void fetchLiveStatus() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    String slot = snapshot.child("current_slot").getValue(String.class);
                    String bookedBy = snapshot.child("booked_by").getValue(String.class);

                    tvStatus.setText(status);
                    tvCurrentSlot.setText("Current Slot: " + slot);

                    if ("Occupied".equalsIgnoreCase(status) || "Booked".equalsIgnoreCase(status)) {
                        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        bookedDetailsLayout.setVisibility(View.VISIBLE);
                    } else {
                        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        bookedDetailsLayout.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LabLiveStatusActivity.this, "Error fetching live status.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
