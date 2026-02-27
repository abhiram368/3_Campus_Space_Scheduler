package com.example.campus_space_scheduler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * ManagementAdapter handles the display of Users or Spaces in a list.
 * It reuses the item_admin_stat_card for a consistent Material 3 look.
 */
public class ManagementAdapter extends RecyclerView.Adapter<ManagementAdapter.ViewHolder> {

    private List<ManagementModel> list = new ArrayList<>();
    private final String mode; // "USER" or "SPACE"

    public ManagementAdapter(String mode) {
        this.mode = mode;
    }

    /**
     * Updates the adapter data and refreshes the UI.
     */
    public void setData(List<ManagementModel> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reusing the theme-safe stat card layout
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_stat_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ManagementModel item = list.get(position);

        // Logic check: Aligning with the updated ManagementModel fields
        if ("USER".equals(mode)) {
            // Displays: "User Name (Role)" as the label
            String userLabel = item.getName() + " (" + item.getRole() + ")";
            holder.label.setText(userLabel);

            // Displays: Email/Roll No as the primary value
            holder.value.setText(item.getIdentifier());
        } else {
            // Displays: "Room: Name" as the label
            String roomLabel = "Room: " + item.getRoomName();
            holder.label.setText(roomLabel);

            // Displays: "Capacity: 50" as the primary value
            String capacityValue = "Capacity: " + item.getCapacity();
            holder.value.setText(capacityValue);
        }

        /* Note for Future Me:
           Text colors are NOT set here. They are inherited from the XML
           (?attr/colorOnSurface) to ensure they automatically adjust
           to Light and Dark themes.
        */
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * ViewHolder caches references to the views to improve scrolling performance.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView label, value;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // These IDs must match the ones in item_admin_stat_card.xml
            label = itemView.findViewById(R.id.stat_label);
            value = itemView.findViewById(R.id.stat_value);
        }
    }
}