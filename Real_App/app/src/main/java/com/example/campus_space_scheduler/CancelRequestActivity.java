package com.example.campus_space_scheduler;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class CancelRequestActivity extends AppCompatActivity {

    private List<Booking> bookingList;
    private BookingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancel_request);

        ImageView buttonBack = findViewById(R.id.buttonBack);
        TextView textViewSpaceName = findViewById(R.id.textViewSpaceName);
        RecyclerView recyclerViewBookings = findViewById(R.id.recyclerViewBookings);

        String spaceName = getIntent().getStringExtra("SPACE_NAME");
        if (spaceName != null) {
            textViewSpaceName.setText(spaceName);
        }

        // Dummy data for booking requests
        bookingList = new ArrayList<>();
        bookingList.add(new Booking("12 Aug", "10:00 – 11:00", "Mid-term exam", "Pending"));
        bookingList.add(new Booking("15 Aug", "14:00 – 15:00", "Project discussion", "Accepted"));
        bookingList.add(new Booking("20 Aug", "09:00 – 10:00", "Guest lecture", "Booked"));

        adapter = new BookingAdapter(bookingList, booking -> {
            showCancelConfirmationDialog(booking);
        });

        recyclerViewBookings.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBookings.setAdapter(adapter);

        buttonBack.setOnClickListener(v -> finish());
    }

    private void showCancelConfirmationDialog(Booking booking) {
        String spaceName = getIntent().getStringExtra("SPACE_NAME");
        if (spaceName == null) spaceName = "this space";
        
        String message = "Are you sure you want to cancel the booking for " + spaceName + 
                " on " + booking.getDate() + " (" + booking.getTimeSlot() + ")?";

        new MaterialAlertDialogBuilder(this, R.style.Theme_CampusSpaceScheduler_Dialog)
                .setTitle("Cancel Booking")
                .setMessage(message)
                .setPositiveButton("Cancel Booking", (dialog, which) -> {
                    int position = bookingList.indexOf(booking);
                    if (position != -1) {
                        bookingList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, bookingList.size());
                        
                        showSuccessSnackbar("Booking cancelled successfully");
                    }
                })
                .setNegativeButton("Keep Booking", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSuccessSnackbar(String message) {
        View contextView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(contextView, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(getResources().getColor(R.color.primary_blue));
        snackbar.setTextColor(getResources().getColor(R.color.white));
        snackbar.show();
    }
}
