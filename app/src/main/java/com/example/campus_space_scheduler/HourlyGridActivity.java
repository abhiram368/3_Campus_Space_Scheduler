package com.example.campus_space_scheduler;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.campus_space_scheduler.databinding.ActivityHourlyGridBinding;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;

public class HourlyGridActivity extends AppCompatActivity {
    private ActivityHourlyGridBinding binding;
    private List<HourSlot> hourSlots = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHourlyGridBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String room = getIntent().getStringExtra("ROOM_NAME");
        String date = getIntent().getStringExtra("SELECTED_DATE");

        // Initialize 24 slots
        for (int i = 0; i < 24; i++) {
            hourSlots.add(new HourSlot(i, false));
        }

        binding.rvHourlyGrid.setLayoutManager(new LinearLayoutManager(this));
        HourlyGridAdapter adapter = new HourlyGridAdapter(hourSlots);
        binding.rvHourlyGrid.setAdapter(adapter);

        binding.btnSaveGrid.setOnClickListener(v -> {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("schedules")
                    .child(room).child(date);

            // Converting list to a simple map for Firebase
            ref.setValue(hourSlots).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Schedule Saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}