package com.example.campus_space_scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campus_space_scheduler.databinding.ActivitySpaceSelectionBinding;
import com.example.campus_space_scheduler.databinding.ItemSpaceCardBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;

public class SpaceSelectionActivity extends AppCompatActivity {

    private ActivitySpaceSelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpaceSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.rvSpaces.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns for a clean grid

        fetchSpaces();
    }

    private void fetchSpaces() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("spaces");
        db.get().addOnSuccessListener(snapshot -> {
            List<ManagementModel> spaceList = new ArrayList<>();
            for (DataSnapshot ds : snapshot.getChildren()) {
                ManagementModel space = ds.getValue(ManagementModel.class);
                if (space != null) spaceList.add(space);
            }
            binding.rvSpaces.setAdapter(new SpaceAdapter(spaceList));
        });
    }

    private class SpaceAdapter extends RecyclerView.Adapter<SpaceAdapter.SpaceViewHolder> {
        private final List<ManagementModel> spaces;
        SpaceAdapter(List<ManagementModel> spaces) { this.spaces = spaces; }

        @NonNull
        @Override
        public SpaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSpaceCardBinding b = ItemSpaceCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new SpaceViewHolder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull SpaceViewHolder holder, int position) {
            ManagementModel space = spaces.get(position);
            holder.binding.roomTitle.setText(space.getRoomName());
            holder.binding.roomCapacity.setText("Capacity: " + space.getCapacity());

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(SpaceSelectionActivity.this, ScheduleCalendarActivity.class);
                intent.putExtra("ROOM_NAME", space.getRoomName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return spaces.size(); }

        class SpaceViewHolder extends RecyclerView.ViewHolder {
            ItemSpaceCardBinding binding;
            SpaceViewHolder(ItemSpaceCardBinding b) { super(b.getRoot()); this.binding = b; }
        }
    }
}