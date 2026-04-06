package com.example.hod.hod;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hod.R;

import com.example.hod.adapters.RequestAdapter;
import com.example.hod.models.Booking;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class HodApprovalHistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private ProgressBar progressBar;
    private RequestAdapter requestAdapter;
    private List<Booking> bookingList;
    private List<Booking> fullBookingList;
    private TabLayout filterTabLayout;
    private View selectionActions;
    private boolean isSelectionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_hod_approval_history);

            historyRecyclerView = findViewById(R.id.historyRecyclerView);
            progressBar         = findViewById(R.id.progressBar);
            filterTabLayout     = findViewById(R.id.filterTabLayout);
            selectionActions    = findViewById(R.id.selection_actions);

            historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            bookingList     = new ArrayList<>();
            fullBookingList = new ArrayList<>();
            // "hod_history" mode opens HodCompletedRequestDetailActivity on card click
            requestAdapter = new RequestAdapter(this, bookingList, "hod_history");
            historyRecyclerView.setAdapter(requestAdapter);

            updateHeader("Approval History", 0);
            setupFilterTabs();
            setupSelectionActions();
            loadHistory();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(e.getMessage())
                    .setPositiveButton("OK", (d, w) -> finish())
                    .show();
        }
    }

    private void setupFilterTabs() {
        filterTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (isSelectionMode) toggleSelectionMode(false);
                String text = tab.getText().toString();
                applyFilter(text);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSelectionActions() {
        MaterialButton btnSelectAll = findViewById(R.id.btn_select_all);
        MaterialButton btnDelete = findViewById(R.id.btn_delete_selected);

        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v -> {
                boolean allSelected = requestAdapter.getSelectedItems().size() == bookingList.size();
                requestAdapter.selectAll(!allSelected);
                btnSelectAll.setText(allSelected ? "Select All" : "Deselect All");
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                int count = requestAdapter.getSelectedItems().size();
                if (count == 0) return;

                new AlertDialog.Builder(this)
                        .setTitle("Delete Records")
                        .setMessage("Are you sure you want to delete " + count + " history records permanently?")
                        .setPositiveButton("DELETE", (d, w) -> deleteSelected())
                        .setNegativeButton("CANCEL", null)
                        .show();
            });
        }
    }

    private void deleteSelected() {
        java.util.Set<String> toDelete = requestAdapter.getSelectedItems();
        FirebaseRepository repo = new FirebaseRepository();
        
        progressBar.setVisibility(View.VISIBLE);
        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

        for (String id : toDelete) {
            repo.deleteBooking(id, result -> {
                if (count.incrementAndGet() >= toDelete.size()) {
                    runOnUiThread(() -> {
                        toggleSelectionMode(false);
                        loadHistory();
                    });
                }
            });
        }
    }

    private void toggleSelectionMode(boolean enabled) {
        this.isSelectionMode = enabled;
        requestAdapter.setSelectionMode(enabled);
        if (selectionActions != null) {
            selectionActions.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
        
        android.widget.ImageView btnAction = findViewById(R.id.btnAction);
        if (btnAction != null) {
            btnAction.setImageResource(enabled ? R.drawable.ic_close : R.drawable.ic_edit);
        }
    }

    private void applyFilter(String filter) {
        bookingList.clear();
        for (Booking b : fullBookingList) {
            String status = b.getStatus() != null ? b.getStatus().toLowerCase() : "";
            boolean isExpired = b.isExpired();
            if (filter.equalsIgnoreCase("All")) {
                bookingList.add(b);
            } else if (filter.equalsIgnoreCase("Approved") && status.equals("approved")) {
                bookingList.add(b);
            } else if (filter.equalsIgnoreCase("Rejected") && status.equals("rejected")) {
                bookingList.add(b);
            } else if (filter.equalsIgnoreCase("Expired") && 
                      (status.contains("expired") || (isExpired && !status.equals("approved") && !status.equals("rejected") && !status.equals("cancelled")))) {
                bookingList.add(b);
            } else if (filter.equalsIgnoreCase("Cancelled") && status.equals("cancelled")) {
                bookingList.add(b);
            }
        }

        progressBar.setVisibility(View.GONE);
        View emptyState = findViewById(R.id.empty_state_view);
        if (bookingList.isEmpty()) {
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
                android.widget.TextView tvEmptySubtitle = emptyState.findViewById(R.id.tv_empty_subtitle);
                if (tvEmptySubtitle != null) {
                    tvEmptySubtitle.setText("There are no " + filter.toLowerCase() + " items to display.");
                }
            }
            historyRecyclerView.setVisibility(View.GONE);
            updateHeader(getString(R.string.approval_history_title), 0);
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            historyRecyclerView.setVisibility(View.VISIBLE);
            requestAdapter.notifyDataSetChanged();
            updateHeader(getString(R.string.approval_history_title), bookingList.size());
        }
    }

    private void updateHeader(String title, int count) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        View btnBack = findViewById(R.id.btnBack);
        android.widget.ImageView btnAction = findViewById(R.id.btnAction);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(getString(R.string.label_records_found, count));
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            if (isSelectionMode) toggleSelectionMode(false);
            else finish();
        });

        if (btnAction != null) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setImageResource(isSelectionMode ? R.drawable.ic_close : R.drawable.ic_edit);
            btnAction.setOnClickListener(v -> toggleSelectionMode(!isSelectionMode));
        }
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseRepository repo = new FirebaseRepository();
        repo.observeHodHistory(result -> {
            progressBar.setVisibility(View.GONE);
            if (result instanceof Result.Success) {
                fullBookingList.clear();
                List<Booking> fetched = ((Result.Success<List<Booking>>) result).data;
                if (fetched != null) {
                    fullBookingList.addAll(fetched);
                    // Sort by decisionTime (descending -> newest first)
                    java.util.Collections.sort(fullBookingList, (b1, b2) -> {
                        String t1 = b1.getDecisionTime() != null ? b1.getDecisionTime() : "";
                        String t2 = b2.getDecisionTime() != null ? b2.getDecisionTime() : "";
                        return t2.compareTo(t1); // Reverse order
                    });
                }
                applyFilter("All");
            } else if (result instanceof Result.Error) {
                new AlertDialog.Builder(this)
                        .setTitle("Firebase Error")
                        .setMessage(((Result.Error<List<Booking>>) result).exception.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }
}