package com.example.campus_space_scheduler;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.example.campus_space_scheduler.databinding.ActivityUserTableBinding;
import com.example.campus_space_scheduler.databinding.DialogAddItemBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserTableActivity extends AppCompatActivity {

    private ActivityUserTableBinding binding;
    private String mode;
    private String filterRole;
    private List<DataNode> fullList = new ArrayList<>();

    private static class DataNode {
        String key;
        ManagementModel model;
        DataNode(String k, ManagementModel m) { this.key = k; this.model = m; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserTableBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mode = getIntent().getStringExtra("DB_NODE");
        filterRole = getIntent().getStringExtra("FILTER_ROLE");

        setupUI();
        setupSearch();
        fetchData();
    }

    private void setupUI() {
        binding.toolbar.setTitle("USER".equals(mode) ? "User Directory" : "Space Directory");
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSearch() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTable(newText);
                return true;
            }
        });
    }

    private void fetchData() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(mode.equals("USER") ? "users" : "spaces");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullList.clear();
                // Inside UserTableActivity.java -> fetchData()
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ManagementModel item = ds.getValue(ManagementModel.class);
                    if (item != null) {
                        String itemRole = item.getRole() != null ? item.getRole().trim() : "";

                        // Match "All" or perform a case-insensitive check
                        if (filterRole.equals("All") || itemRole.equalsIgnoreCase(filterRole)) {
                            fullList.add(new DataNode(ds.getKey(), item));
                        }
                    }
                }
                filterTable(binding.searchView.getQuery().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserTableActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterTable(String query) {
        binding.tableLayout.removeAllViews();
        addHeaderRow(); // First row is always the header

        for (DataNode node : fullList) {
            String primary = node.model.getPrimaryValue(mode).toLowerCase();
            String secondary = node.model.getSecondaryValue(mode).toLowerCase();

            if (primary.contains(query.toLowerCase()) || secondary.contains(query.toLowerCase())) {
                addRow(node.model, node.key);
            }
        }
    }

    private void addHeaderRow() {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(Color.BLACK);

        String col1 = "USER".equals(mode) ? "Name" : "Room Name";
        String col2 = "USER".equals(mode) ? "Email/ID" : "Capacity";
        String col3 = "USER".equals(mode) ? "Role" : "Type";

        row.addView(createTableCell(col1, true));
        row.addView(createTableCell(col2, true));
        row.addView(createTableCell(col3, true));
        row.addView(createTableCell("Actions", true));

        binding.tableLayout.addView(row);
    }

    private void addRow(ManagementModel item, String key) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(Color.BLACK);

        row.addView(createTableCell(item.getPrimaryValue(mode), false));
        row.addView(createTableCell(item.getSecondaryValue(mode), false));
        row.addView(createTableCell(item.getRole(), false));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setBackgroundColor(Color.WHITE);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(1, 1, 1, 1);
        actions.setLayoutParams(lp);

        TextView edit = createActionButton("Edit", "#2196F3");
        edit.setOnClickListener(v -> showEditDialog(item, key));

        TextView remove = createActionButton("Remove", "#F44336");
        remove.setOnClickListener(v -> showDeleteConfirmation(key));

        actions.addView(edit);
        actions.addView(remove);
        row.addView(actions);

        binding.tableLayout.addView(row);
    }

    private TextView createTableCell(String text, boolean isBold) {
        TextView tv = new TextView(this);
        tv.setText(text != null ? text : "-");
        tv.setPadding(24, 24, 24, 24);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(Color.WHITE);
        tv.setTextColor(Color.BLACK);
        if (isBold) tv.setTypeface(null, Typeface.BOLD);

        TableRow.LayoutParams lp = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(1, 1, 1, 1);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void showDeleteConfirmation(String key) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to remove this?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    FirebaseDatabase.getInstance().getReference("USER".equals(mode) ? "users" : "spaces")
                            .child(key).removeValue();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(ManagementModel item, String key) {
        DialogAddItemBinding dBinding = DialogAddItemBinding.inflate(getLayoutInflater());
        dBinding.editField1.setText(item.getPrimaryValue(mode));
        dBinding.editField2.setText(item.getSecondaryValue(mode));
        dBinding.roleDropdown.setText(item.getRole(), false);

        if ("USER".equals(mode)) {
            dBinding.layoutField1.setHint("Name");
            dBinding.layoutField2.setHint("Email/ID");
            String[] roles = {"Student", "Faculty", "Hall Incharge", "Staff Incharge", "CSED Staff", "HoD", "Faculty Incharge", "Lab admin", "App admin"};
            dBinding.roleDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, roles));
        } else {
            dBinding.layoutField1.setHint("Room Name");
            dBinding.layoutField2.setHint("Capacity");
            String[] spaceTypes = {"Lab", "Hall", "Classroom"};
            dBinding.roleDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, spaceTypes));
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Details")
                .setView(dBinding.getRoot())
                .setPositiveButton("Update", (d, w) -> {
                    HashMap<String, Object> map = new HashMap<>();
                    if ("USER".equals(mode)) {
                        map.put("name", dBinding.editField1.getText().toString());
                        map.put("identifier", dBinding.editField2.getText().toString());
                    } else {
                        map.put("roomName", dBinding.editField1.getText().toString());
                        map.put("capacity", dBinding.editField2.getText().toString());
                    }
                    map.put("role", dBinding.roleDropdown.getText().toString());
                    FirebaseDatabase.getInstance().getReference("USER".equals(mode) ? "users" : "spaces")
                            .child(key).updateChildren(map);
                }).setNegativeButton("Cancel", null).show();
    }

    private TextView createActionButton(String text, String colorHex) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setPadding(12, 8, 12, 8);
        tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }
}