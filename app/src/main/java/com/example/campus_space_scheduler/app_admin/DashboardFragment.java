package com.example.campus_space_scheduler.app_admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.campus_space_scheduler.databinding.AFragmentDashboardBinding;
import com.google.firebase.database.*;

public class DashboardFragment extends Fragment {
    private AFragmentDashboardBinding binding;
    private DatabaseReference usersRef, bookingsRef, requestsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AFragmentDashboardBinding.inflate(inflater, container, false);

        // Initialize Firebase
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        requestsRef = FirebaseDatabase.getInstance().getReference("requests");

        startLiveUpdates();
        return binding.getRoot();
    }

    private void startLiveUpdates() {
        usersRef.addValueEventListener(createStatListener(binding.tvUserCount));
        bookingsRef.addValueEventListener(createStatListener(binding.tvTotalBookings));
        requestsRef.orderByChild("status").equalTo("pending")
                .addValueEventListener(createStatListener(binding.tvPendingCount));
    }

    private ValueEventListener createStatListener(android.widget.TextView textView) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                textView.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}