package com.example.hod.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.appy.R;

import com.example.hod.models.Space;

import java.util.List;

public class SpaceSelectionAdapter extends RecyclerView.Adapter<SpaceSelectionAdapter.ViewHolder> {

    private Context context;
    private List<Space> list;
    private OnSpaceClickListener listener;

    public interface OnSpaceClickListener {
        void onSpaceClick(Space space);
    }

    public SpaceSelectionAdapter(Context context, List<Space> list, OnSpaceClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_space_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Space space = list.get(position);
        holder.tvSpaceName.setText(space.getRoomName());
        
        String details = "";
        if (space.getAddress() != null && !space.getAddress().isEmpty()) {
            details = space.getAddress();
        }
        holder.tvSpaceDetails.setText(details);
        if (details.isEmpty()) {
            holder.tvSpaceDetails.setVisibility(View.GONE);
        } else {
            holder.tvSpaceDetails.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSpaceClick(space);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpaceName, tvSpaceDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSpaceName = itemView.findViewById(R.id.tvSpaceName);
            tvSpaceDetails = itemView.findViewById(R.id.tvSpaceDetails);
        }
    }
}
