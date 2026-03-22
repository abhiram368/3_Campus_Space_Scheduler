package com.example.campus_space_scheduler.app_admin;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campus_space_scheduler.databinding.AActivityManageBookingsBinding;
import com.example.campus_space_scheduler.model.BookingModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ManageBookingsActivity extends AppCompatActivity {

    private AActivityManageBookingsBinding binding;
    private BookingAdapter adapter;
    private String selectedStatus = "All";
    private final List<DataNode> fullList = new ArrayList<>();

    public static class DataNode {
        String key;
        BookingModel model;

        DataNode(String k, BookingModel m) {
            key = k;
            model = m;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = AActivityManageBookingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        fetchBookings();
    }

    private void setupUI() {

        binding.toolbar.setTitle("Manage Bookings");
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new BookingAdapter(new ArrayList<>(), key -> showDeleteDialog(key));

//        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        setupFilters();
    }

    private void setupFilters() {

        binding.chipAll.setOnClickListener(v -> applyFilter("All"));
        binding.chipApproved.setOnClickListener(v -> applyFilter("Approved"));
        binding.chipPending.setOnClickListener(v -> applyFilter("Pending"));
        binding.chipRejected.setOnClickListener(v -> applyFilter("Rejected"));
    }

    private void applyFilter(String status) {
        selectedStatus = status;
        filterList();
    }

    private void filterList() {

        List<DataNode> filtered = new ArrayList<>();

        for (DataNode node : fullList) {

            String s = node.model.getStatus();

            if (selectedStatus.equals("All") ||
                    (s != null && s.equalsIgnoreCase(selectedStatus))) {
                filtered.add(node);
            }
        }

        adapter.updateList(filtered);
    }

    private void fetchBookings() {

        FirebaseDatabase.getInstance()
                .getReference("bookings")
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        fullList.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            BookingModel model = ds.getValue(BookingModel.class);
                            if (model != null) {
                                fullList.add(new DataNode(ds.getKey(), model));
                            }
                        }

                        filterList();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showDeleteDialog(String key) {

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Booking")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (d, w) ->
                        FirebaseDatabase.getInstance()
                                .getReference("bookings")
                                .child(key)
                                .removeValue()
                )
                .setNegativeButton("Cancel", null)
                .show();
    }
}