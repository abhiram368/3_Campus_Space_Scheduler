package com.example.hod.hod;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.appy.R;
import com.example.hod.adapters.RequestAdapter;
import com.example.hod.models.Booking;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;

import java.util.ArrayList;
import java.util.List;

public class HodEscalatedRequestsActivity extends AppCompatActivity {

    private RecyclerView requestsRecyclerView;
    private ProgressBar progressBar;
    private RequestAdapter requestAdapter;
    private List<Booking> bookingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_escalated_requests);

        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        progressBar = findViewById(R.id.progressBar);

        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookingList = new ArrayList<>();
        requestAdapter = new RequestAdapter(this, bookingList, "escalated");
        requestsRecyclerView.setAdapter(requestAdapter);

        updateHeader(getString(R.string.escalated_requests_title), 0);
        loadEscalatedRequests();
    }

    private void updateHeader(String title, int count) {
        View header = findViewById(R.id.header_layout);
        if (header != null) {
            TextView tvTitle = header.findViewById(R.id.header_title);
            TextView tvSubtitle = header.findViewById(R.id.header_subtitle);
            View btnBack = header.findViewById(R.id.btnBack);
            
            if (tvTitle != null) tvTitle.setText(title);
            if (tvSubtitle != null) tvSubtitle.setText(getString(R.string.label_pending_actions, count));
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        }
    }

    private void loadEscalatedRequests() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseRepository repo = new FirebaseRepository();
        repo.observeEscalatedRequests(result -> {
            progressBar.setVisibility(View.GONE);
            if (result instanceof Result.Success) {
                bookingList.clear();
                List<Booking> fetched = ((Result.Success<List<Booking>>) result).data;
                if (fetched != null) {
                    bookingList.addAll(fetched);
                    // Sort by bookedTime (ascending -> oldest first)
                    bookingList.sort((b1, b2) -> {
                        String t1 = b1.getBookedTimeDisplay();
                        String t2 = b2.getBookedTimeDisplay();
                        if (t1 == null) return (t2 == null) ? 0 : 1;
                        if (t2 == null) return -1;
                        return t1.compareTo(t2);
                    });
                }

                View emptyState = findViewById(R.id.empty_state_view);
                if (bookingList.isEmpty()) {
                    if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                    requestsRecyclerView.setVisibility(View.GONE);
                    updateHeader(getString(R.string.escalated_requests_title), 0);
                } else {
                    if (emptyState != null) emptyState.setVisibility(View.GONE);
                    requestsRecyclerView.setVisibility(View.VISIBLE);
                    requestAdapter.notifyDataSetChanged();
                    updateHeader(getString(R.string.escalated_requests_title), bookingList.size());
                }
            } else if (result instanceof Result.Error) {
                Toast.makeText(this, "Failed to load escalated requests", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
