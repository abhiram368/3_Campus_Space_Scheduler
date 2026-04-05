package com.example.hod.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hod.R;
import com.example.hod.models.User;
import com.example.hod.staff.StaffBlockedUserDetailActivity;

import java.util.List;

public class BlockedUserAdapter extends RecyclerView.Adapter<BlockedUserAdapter.ViewHolder> {

    private final Context context;
    private final List<User> blockedUsers;

    public BlockedUserAdapter(Context context, List<User> blockedUsers) {
        this.context = context;
        this.blockedUsers = blockedUsers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_blocked_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = blockedUsers.get(position);

        String name = user.getName() != null && !user.getName().isEmpty() ? user.getName() : "Unknown User";
        holder.tvUserName.setText(name);
        holder.tvAvatarInitial.setText(name.substring(0, 1).toUpperCase());
        holder.tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");
        
        String reason = user.getBlockReason() != null ? user.getBlockReason() : "No reason provided";
        holder.tvBlockReason.setText("Reason: " + reason);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, StaffBlockedUserDetailActivity.class);
            intent.putExtra("userId", user.getUserId());
            intent.putExtra("userName", user.getName());
            intent.putExtra("userEmail", user.getEmail());
            intent.putExtra("userRole", user.getRole());
            intent.putExtra("blockReason", user.getBlockReason());
            intent.putExtra("blockedBy", user.getBlockedBy());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return blockedUsers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail, tvBlockReason, tvAvatarInitial;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvBlockReason = itemView.findViewById(R.id.tvBlockReason);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
        }
    }
}
