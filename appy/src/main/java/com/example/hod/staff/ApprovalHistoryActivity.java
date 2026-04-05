package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hod.R;
import com.example.hod.adapters.RequestAdapter;
import com.example.hod.models.Booking;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ApprovalHistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private ProgressBar progressBar;
    private TextView noHistoryTextView;
    private RequestAdapter requestAdapter;
    private List<Booking> bookingList;
    private List<Booking> fullBookingList;
    private TabLayout filterTabLayout;
    private String labId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_approval_history);

            labId = getIntent().getStringExtra("labId");
            if (labId == null || labId.isEmpty()) {
                android.util.Log.w("ApprovalHistory", "labId not provided, showing all approvals.");
                labId = null;
            }

            historyRecyclerView = findViewById(R.id.historyRecyclerView);
            progressBar = findViewById(R.id.progressBar);
            noHistoryTextView = findViewById(R.id.noHistoryTextView);
            filterTabLayout = findViewById(R.id.filterTabLayout);

            if (historyRecyclerView == null) throw new RuntimeException("historyRecyclerView not found in layout");
            if (progressBar == null) throw new RuntimeException("progressBar not found in layout");
            if (noHistoryTextView == null) throw new RuntimeException("noHistoryTextView not found in layout");
            if (filterTabLayout == null) throw new RuntimeException("filterTabLayout not found in layout");

            historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            bookingList = new ArrayList<>();
            fullBookingList = new ArrayList<>();
            requestAdapter = new RequestAdapter(this, bookingList, "history");
            historyRecyclerView.setAdapter(requestAdapter);

            setupFilterTabs();
            loadApprovalHistory();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("ApprovalHistory Crash")
                    .setMessage(e.getClass().getSimpleName() + ": " + e.getMessage())
                    .setPositiveButton("OK", (d, w) -> finish())
                    .show();
        }
    }

    private void loadApprovalHistory() {
        try {
            progressBar.setVisibility(View.VISIBLE);
            FirebaseRepository repo = new FirebaseRepository();
            // Use a real-time listener to handle status updates
            repo.observeApprovals(labId, result -> {
                try {
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
                        // Re-apply the current filter
                        int selectedTabPosition = filterTabLayout.getSelectedTabPosition();
                        String currentFilter = "All";
                        if (selectedTabPosition != -1) {
                            TabLayout.Tab selectedTab = filterTabLayout.getTabAt(selectedTabPosition);
                            if (selectedTab != null && selectedTab.getText() != null) {
                                currentFilter = selectedTab.getText().toString();
                            }
                        }
                        applyFilter(currentFilter);

                    } else if (result instanceof Result.Error) {
                        progressBar.setVisibility(View.GONE);
                        String errorMsg = ((Result.Error<List<Booking>>) result).exception.getMessage();
                        new AlertDialog.Builder(ApprovalHistoryActivity.this)
                                .setTitle("Firebase Error")
                                .setMessage(errorMsg)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                } catch (Exception e) {
                    new AlertDialog.Builder(ApprovalHistoryActivity.this)
                            .setTitle("Callback Crash")
                            .setMessage(e.getClass().getSimpleName() + ": " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Load Crash")
                    .setMessage(e.getClass().getSimpleName() + ": " + e.getMessage())
                    .setPositiveButton("OK", (d, w) -> finish())
                    .show();
        }
    }

    private void setupFilterTabs() {
        filterTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String text = tab.getText().toString();
                applyFilter(text);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

private void applyFilter(String filter) {
    bookingList.clear();
    for (Booking b : fullBookingList) {
        // 1. Safe status extraction with trim()
        String status = b.getStatus() != null ? b.getStatus().trim().toLowerCase() : "";
        boolean isDynamicallyExpired = false;

        // 2. Only apply dynamic expiry to strictly PENDING items.
        // Forwarded items represent a completed Staff action and should stay in the Forwarded tab.
        if (status.equals("pending") || status.equals("requested")) {
            try {
                String dateStr = b.getDate();
                String timeSlot = b.getTimeSlot();
                
                if (dateStr != null && timeSlot != null) {
                    // 3. Robust split: Handles regular hyphens (-), en-dashes (–), and em-dashes (—)
                    String[] timeParts = timeSlot.split("[-–—]");
                    String timeStr = timeParts.length > 1 ? timeParts[1].trim() : timeSlot.trim();

                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                    java.util.Date bookingDateTime = sdf.parse(dateStr + " " + timeStr);
                    
                    if (bookingDateTime != null && bookingDateTime.before(new java.util.Date())) {
                        isDynamicallyExpired = true;
                    }
                }
            } catch (Exception e) {
                // DO NOT leave this empty! This prints the exact reason why parsing fails to Logcat.
                android.util.Log.e("ApprovalHistory", "Failed to parse date/time for booking ID: " + b.getBookingId(), e);
            }
        }

        // 4. Apply the filter routing
        if (filter.equalsIgnoreCase("All")) {
            bookingList.add(b);
        } else if (filter.equalsIgnoreCase("Approved") && status.equals("approved")) {
            bookingList.add(b);
        } else if (filter.equalsIgnoreCase("Rejected") && status.equals("rejected")) {
            bookingList.add(b);
        } else if (filter.equalsIgnoreCase("Expired") && (status.equals("expired") || status.equals("rejected_expired") || isDynamicallyExpired)) {
            // Note: It's better practice to let the RequestAdapter handle setting the "EXPIRED" UI badge 
            // dynamically rather than using b.setStatus("expired") here, as it mutates your cached list.
            bookingList.add(b);
        } else if (filter.equalsIgnoreCase("Cancelled") && status.equals("cancelled")) {
            bookingList.add(b);
        } else if (filter.equalsIgnoreCase("Forwarded") && status.contains("forwarded") && !isDynamicallyExpired) {
            bookingList.add(b);
        }
    }

    // UI Updates
    progressBar.setVisibility(View.GONE);
    View emptyState = findViewById(R.id.empty_state_view);
    
    if (bookingList.isEmpty()) {
        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
            TextView tvEmptySubtitle = emptyState.findViewById(R.id.tv_empty_subtitle);
            if (tvEmptySubtitle != null) {
                tvEmptySubtitle.setText("There are no " + filter.toLowerCase() + " items to display.");
            }
        }
        historyRecyclerView.setVisibility(View.GONE);
        updateHeader("Approval History", 0);
    } else {
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        historyRecyclerView.setVisibility(View.VISIBLE);
        requestAdapter.notifyDataSetChanged();
        updateHeader("Approval History", bookingList.size());
    }
}

    private void updateHeader(String title, int count) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        View btnBack = findViewById(R.id.btnBack);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(count + " records found");
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }
}
