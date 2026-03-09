package com.example.labadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class LabAdminDashboardActivity extends AppCompatActivity {

    private FirestoreRepository repository;
    private String currentUserId = "admin_001"; // Default for testing
    private String currentBookingIdValue;

    private CardView cardViewSession;
    private CardView cardNoAssignment;
    private TextView txtVenue, txtDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_admin_dashboard);

        // Initialize Repository (Backend Handler)
        repository = new FirestoreRepository();
        
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        cardViewSession = findViewById(R.id.cardViewSession);
        cardNoAssignment = findViewById(R.id.cardNoAssignment);
        txtVenue = findViewById(R.id.txtVenue);
        txtDateTime = findViewById(R.id.txtDateTime);
        
        loadAssignedBooking();

        cardViewSession.setOnClickListener(v -> {
            if (currentBookingIdValue != null) {
                Intent intent = new Intent(this, AssignedSessionDetailsActivity.class);
                intent.putExtra("bookingId", currentBookingIdValue);
                startActivity(intent);
            }
        });

        animateBackground();
    }

    private void loadAssignedBooking() {
        // Backend call through repository
        repository.getAdminSessions(currentUserId, new FirestoreRepository.Callback<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot result) {
                if (result != null && !result.isEmpty()) {
                    DocumentSnapshot doc = result.getDocuments().get(0);
                    currentBookingIdValue = doc.getString("bookingId"); 
                    
                    if (txtVenue != null) txtVenue.setText(doc.getString("venue"));
                    if (txtDateTime != null) {
                        txtDateTime.setText(doc.getString("date") + " | " + doc.getString("slot"));
                    }
                    cardViewSession.setVisibility(View.VISIBLE);
                    cardNoAssignment.setVisibility(View.GONE);
                } else {
                    cardViewSession.setVisibility(View.GONE);
                    cardNoAssignment.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(LabAdminDashboardActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void animateBackground() {
        View bgView = findViewById(android.R.id.content);
        if (bgView != null) {
            bgView.animate().alpha(0.85f).setDuration(3000).withEndAction(() -> {
                bgView.animate().alpha(1.0f).setDuration(3000).withEndAction(this::animateBackground).start();
            }).start();
        }
    }
}
