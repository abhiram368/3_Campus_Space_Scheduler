package com.example.campus_space_scheduler;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campus_space_scheduler.databinding.ActivityUserTableBinding;
import com.example.campus_space_scheduler.databinding.DialogAddItemBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserTableActivity extends AppCompatActivity {

    private ActivityUserTableBinding binding;
    private String mode, filterRole;

    private final List<DataNode> fullList = new ArrayList<>();
    private UserAdapter adapter;

    public static class DataNode {
        String key;
        ManagementModel model;

        DataNode(String k, ManagementModel m) {
            key = k;
            model = m;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityUserTableBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mode = getIntent().getStringExtra("DB_NODE");
        filterRole = getIntent().getStringExtra("FILTER_ROLE");

        setupUI();
        fetchData();
    }

    private void setupUI() {

        binding.toolbar.setTitle("USER".equals(mode) ? "User Directory" : "Space Directory");
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new UserAdapter(new ArrayList<>(), mode, new UserAdapter.OnItemClickListener() {

            @Override
            public void onEdit(ManagementModel model, String key) {
                showEditDialog(model, key);
            }

            @Override
            public void onDelete(String key) {
                showDeleteConfirmation(key);
            }
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String q) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String q) {
                filterTable(q);
                return true;
            }
        });
    }

    private void fetchData() {

        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference("USER".equals(mode) ? "users" : "spaces");

        ref.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                fullList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {

                    ManagementModel item = ds.getValue(ManagementModel.class);

                    if (item == null) continue;

                    if ("All".equals(filterRole) ||
                            (item.getRole() != null && item.getRole().equalsIgnoreCase(filterRole))) {
                        fullList.add(new DataNode(ds.getKey(), item));
                    }
                }

                filterTable(binding.searchView.getQuery().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterTable(String query) {

        List<DataNode> filtered = new ArrayList<>();

        for (DataNode node : fullList) {

            String primary = node.model.getPrimaryValue(mode);
            String secondary = node.model.getSecondaryValue(mode);

            primary = primary == null ? "" : primary;
            secondary = secondary == null ? "" : secondary;

            if (primary.contains(query.toLowerCase()) || secondary.contains(query.toLowerCase())) {
                filtered.add(node);
            }
        }

        adapter.updateList(filtered);
    }

    private void showDeleteConfirmation(String key) {

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to remove this?")
                .setPositiveButton("Remove", (dialog, which) -> {

                    FirebaseDatabase.getInstance()
                            .getReference("USER".equals(mode) ? "users" : "spaces")
                            .child(key)
                            .removeValue();

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(ManagementModel item, String key) {

        DialogAddItemBinding dBinding = DialogAddItemBinding.inflate(getLayoutInflater());

        if ("USER".equals(mode)) {

            dBinding.layoutField1.setHint("Name");
            dBinding.layoutField2.setHint("Email");
            dBinding.layoutField3.setHint("Phone");
            dBinding.layoutField4.setHint("Roll Number");

            dBinding.layoutField3.setVisibility(android.view.View.VISIBLE);
            dBinding.layoutField4.setVisibility(android.view.View.VISIBLE);

            dBinding.editField1.setText(item.getName());
            dBinding.editField2.setText(item.getEmailId());
            dBinding.editField3.setText(item.getPhoneNumber());
            dBinding.editField4.setText(item.getRollNo());

            String[] roles = {
                    "Student",
                    "Faculty",
                    "Hall Incharge",
                    "Staff Incharge",
                    "CSED Staff",
                    "HoD",
                    "Faculty Incharge",
                    "Lab admin",
                    "App admin"
            };

            dBinding.roleDropdown.setAdapter(
                    new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, roles)
            );

        } else {

            dBinding.layoutField1.setHint("Room Name");
            dBinding.layoutField2.setHint("Capacity");

            dBinding.layoutField3.setVisibility(android.view.View.GONE);
            dBinding.layoutField4.setVisibility(android.view.View.GONE);

            dBinding.editField1.setText(item.getRoomName());
            dBinding.editField2.setText(item.getCapacity());

            String[] spaceTypes = {"Lab", "Hall", "Classroom"};

            dBinding.roleDropdown.setAdapter(
                    new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, spaceTypes)
            );
        }

        dBinding.roleDropdown.setText(item.getRole(), false);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Details")
                .setView(dBinding.getRoot())
                .setPositiveButton("Update", (d, w) -> {

                    HashMap<String, Object> map = new HashMap<>();

                    if ("USER".equals(mode)) {

                        map.put("name", dBinding.editField1.getText().toString());
                        map.put("emailId", dBinding.editField2.getText().toString());
                        map.put("phoneNumber", dBinding.editField3.getText().toString());
                        map.put("rollNo", dBinding.editField4.getText().toString());
                        map.put("role", dBinding.roleDropdown.getText().toString());

                    } else {

                        map.put("roomName", dBinding.editField1.getText().toString());
                        map.put("capacity", dBinding.editField2.getText().toString());
                        map.put("role", dBinding.roleDropdown.getText().toString());
                    }

                    FirebaseDatabase.getInstance()
                            .getReference("USER".equals(mode) ? "users" : "spaces")
                            .child(key)
                            .updateChildren(map);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}