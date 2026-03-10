package com.example.hod.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hod.R;
import com.example.hod.adapters.RequestAdapter;
import com.example.hod.models.Booking;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PendingRequestsActivity extends AppCompatActivity {

    private RecyclerView requestsRecyclerView;
    private ProgressBar progressBar;
    private TextView noRequestsTextView;
    private RequestAdapter requestAdapter;
    private List<Booking> bookingList;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        noRequestsTextView = findViewById(R.id.noRequestsTextView);

        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookingList = new ArrayList<>();
        requestAdapter = new RequestAdapter(this, bookingList);
        requestsRecyclerView.setAdapter(requestAdapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("bookings");

        loadPendingRequests();
    }

    private void loadPendingRequests() {
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.orderByChild("status").equalTo("pending")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        bookingList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Booking booking = snapshot.getValue(Booking.class);
                            if (booking != null) {
                                bookingList.add(booking);
                            }
                        }
                        
                        progressBar.setVisibility(View.GONE);
                        if (bookingList.isEmpty()) {
                            noRequestsTextView.setVisibility(View.VISIBLE);
                            requestsRecyclerView.setVisibility(View.GONE);
                        } else {
                            noRequestsTextView.setVisibility(View.GONE);
                            requestsRecyclerView.setVisibility(View.VISIBLE);
                            requestAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PendingRequestsActivity.this, "Failed to load requests.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
