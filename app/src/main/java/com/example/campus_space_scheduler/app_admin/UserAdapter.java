package com.example.campus_space_scheduler.app_admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_space_scheduler.databinding.AItemUserCardBinding;
import com.example.campus_space_scheduler.model.ManagementModel;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<UserTableActivity.DataNode> list;
    private String mode;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEdit(ManagementModel model, String key);
        void onDelete(String key);
    }

    public UserAdapter(List<UserTableActivity.DataNode> list, String mode, OnItemClickListener listener) {
        this.list = list;
        this.mode = mode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AItemUserCardBinding binding = AItemUserCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserTableActivity.DataNode node = list.get(position);
        holder.binding.txtPrimary.setText(node.model.getPrimaryValue(mode));
        holder.binding.txtSecondary.setText(node.model.getSecondaryValue(mode));
        holder.binding.txtRoleBadge.setText(node.model.getRole().toUpperCase());

        holder.binding.btnEditCard.setOnClickListener(v -> listener.onEdit(node.model, node.key));
        holder.binding.btnDeleteCard.setOnClickListener(v -> listener.onDelete(node.key));
    }

    @Override
    public int getItemCount() { return list.size(); }

    public void updateList(List<UserTableActivity.DataNode> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        AItemUserCardBinding binding;
        ViewHolder(AItemUserCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}