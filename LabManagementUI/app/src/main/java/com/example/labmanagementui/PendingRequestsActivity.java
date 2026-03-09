package com.example.labmanagementui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.labmanagementui.adapters.RequestAdapter;
import com.example.labmanagementui.models.Request;
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
    private List<Request> requestList;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        noRequestsTextView = findViewById(R.id.noRequestsTextView);

        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(this, requestList);
        requestsRecyclerView.setAdapter(requestAdapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("requests");

        loadPendingRequests();
    }

    private void loadPendingRequests() {
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.orderByChild("status").equalTo("Pending")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                requestList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Request request = snapshot.getValue(Request.class);
                    if (request != null) {
                        requestList.add(request);
                    }
                }
                
                progressBar.setVisibility(View.GONE);
                if (requestList.isEmpty()) {
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
