package com.example.campus_space_scheduler.csed_office;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_space_scheduler.R;
import com.example.campus_space_scheduler.model.BookingModel;
import com.google.firebase.database.*;

import java.util.*;

public class BookingsFragment extends Fragment {

    private TextView tvTotal, tvUpcoming;
    private RecyclerView recyclerView;

    private final List<BookingModel> upcomingList = new ArrayList<>();
    private BookingViewAdapter adapter;

    private DatabaseReference bookingsRef, spacesRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.p_fragment_bookings, container, false);

        tvTotal = view.findViewById(R.id.tvTotalBookings);
        tvUpcoming = view.findViewById(R.id.tvUpcomingBookings);
        recyclerView = view.findViewById(R.id.recyclerBookings);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingViewAdapter(upcomingList);
        recyclerView.setAdapter(adapter);

        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        spacesRef = FirebaseDatabase.getInstance().getReference("spaces");

        loadBookings();

        return view;
    }

    private void loadBookings() {
        bookingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot bookingSnap) {

                int totalCount = 0;
                upcomingList.clear();

                for (DataSnapshot bookingChild : bookingSnap.getChildren()) {

                    BookingModel booking = bookingChild.getValue(BookingModel.class);
                    if (booking == null) continue;

                    String spaceId = booking.getSpaceName();

                    spacesRef.child(spaceId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot spaceSnap) {

                                    String role = spaceSnap.child("role").getValue(String.class);

                                    if ("Classroom".equals(role)) {

                                        // total
                                        int newTotal = Integer.parseInt(tvTotal.getText().toString()) + 1;
                                        tvTotal.setText(String.valueOf(newTotal));

                                        // upcoming (Approved)
                                        if ("Approved".equals(booking.getStatus())) {
                                            upcomingList.add(booking);

                                            tvUpcoming.setText(String.valueOf(upcomingList.size()));
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}