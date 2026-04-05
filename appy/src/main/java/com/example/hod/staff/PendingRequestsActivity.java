package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hod.R;
import com.example.hod.adapters.RequestAdapter;
import com.example.hod.models.Booking;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;

import java.util.ArrayList;
import java.util.List;

public class PendingRequestsActivity extends AppCompatActivity {

    private RecyclerView requestsRecyclerView;
    private ProgressBar progressBar;
    private RequestAdapter requestAdapter;
    private List<Booking> bookingList;
    private String labId;
    private com.google.firebase.database.ValueEventListener pendingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        labId = getIntent().getStringExtra("labId");
        if (labId == null || labId.isEmpty()) {
            Toast.makeText(this, "Error: missing Lab ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        TextView noRequestsTextView = findViewById(R.id.noRequestsTextView); // Initialize here

        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookingList = new ArrayList<>();
        requestAdapter = new RequestAdapter(this, bookingList, "pending");
        requestsRecyclerView.setAdapter(requestAdapter);

        // Header and Back Button Setup
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            View btnBack = headerView.findViewById(R.id.btnBack);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }
        }

        loadPendingRequests();
    }

    private void loadPendingRequests() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseRepository repo = new FirebaseRepository();
        pendingListener = repo.observePendingRequests(labId, result -> {
            if (result instanceof Result.Success) {
                bookingList.clear();
                List<Booking> fetchedBookings = ((Result.Success<List<Booking>>) result).data;
                if (fetchedBookings != null) {
                    bookingList.addAll(fetchedBookings);
                    // Sort by bookedTime (ascending -> oldest first)
                    bookingList.sort((b1, b2) -> {
                        String t1 = b1.getBookedTimeDisplay();
                        String t2 = b2.getBookedTimeDisplay();
                        if (t1 == null) return (t2 == null) ? 0 : 1;
                        if (t2 == null) return -1;
                        return t1.compareTo(t2);
                    });
                }

                progressBar.setVisibility(View.GONE);
                View emptyState = findViewById(R.id.empty_state_view);
                if (bookingList.isEmpty()) {
                    if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                    requestsRecyclerView.setVisibility(View.GONE);
                    updateHeader("Pending Requests", 0);
                } else {
                    if (emptyState != null) emptyState.setVisibility(View.GONE);
                    requestsRecyclerView.setVisibility(View.VISIBLE);
                    requestAdapter.notifyDataSetChanged();
                    updateHeader("Pending Requests", bookingList.size());
                }
            } else if (result instanceof Result.Error) {
                progressBar.setVisibility(View.GONE);
                String errorMsg = ((Result.Error<List<Booking>>) result).exception.getMessage();
                com.google.android.material.snackbar.Snackbar.make(requestsRecyclerView, "Failed to load requests: " + errorMsg, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingListener != null) {
            new FirebaseRepository().removePendingRequestsListener(pendingListener);
        }
    }

    private void updateHeader(String title, int count) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(getString(R.string.pending_requests_total, count)); // Use string resource
    }
}