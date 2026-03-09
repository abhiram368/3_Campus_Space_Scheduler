package com.example.labmanagementui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.labmanagementui.R;
import com.example.labmanagementui.RequestDetailActivity;
import com.example.labmanagementui.models.Request;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private Context context;
    private List<Request> requestList;

    public RequestAdapter(Context context, List<Request> requestList) {
        this.context = context;
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.list_item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        Request request = requestList.get(position);

        holder.tvUsername.setText("User: " + request.getUsername());
        holder.tvLabName.setText("Lab: " + request.getLabName());
        holder.tvSlot.setText("Slot: " + request.getSlot());
        holder.tvReason.setText("Reason: " + request.getReason());
        holder.tvStatus.setText("Status: " + request.getStatus());

        // ONLY navigation — no actions here
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RequestDetailActivity.class);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {

        TextView tvUsername, tvLabName, tvSlot, tvReason, tvStatus;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLabName = itemView.findViewById(R.id.tvLabName);
            tvSlot = itemView.findViewById(R.id.tvSlot);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
