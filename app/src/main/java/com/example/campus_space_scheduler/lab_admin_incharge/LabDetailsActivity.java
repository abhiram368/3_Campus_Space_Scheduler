package com.example.campus_space_scheduler.lab_admin_incharge;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.databinding.ActivityLabDetailsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LabDetailsActivity extends BaseInchargeActivity {

    private ActivityLabDetailsBinding binding;
    private DatabaseReference spacesRef;
    private String labName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLabDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        labName = getIntent().getStringExtra("labName");

        // Setup Sidebar using BaseActivity method
        setupDrawer(binding.toolbar, binding.navView, binding.drawerLayout);

        if (labName != null) {
            binding.tvLabDetailsTitle.setText(labName + " Details");
            binding.tvLabNameSub.setText(labName + " Specifications");
        }

        spacesRef = FirebaseDatabase.getInstance().getReference("spaces");
        fetchLabDetails();
    }

    private void fetchLabDetails() {
        spacesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;
                for (DataSnapshot spaceSnap : snapshot.getChildren()) {
                    String name = String.valueOf(spaceSnap.child("roomName").getValue());
                    if (name.contains(labName)) {
                        String equipment = String.valueOf(spaceSnap.child("equipment").getValue());
                        String capacity = String.valueOf(spaceSnap.child("capacity").getValue());
                        
                        binding.tvLabEquipment.setText(equipment.equals("null") ? "Not specified" : equipment);
                        binding.tvLabCapacity.setText(capacity.equals("null") ? "Not specified" : capacity + " Students");
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    binding.tvLabEquipment.setText("Lab information not found in database.");
                    binding.tvLabCapacity.setText("N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LabDetailsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}