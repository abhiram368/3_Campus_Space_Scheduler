package com.example.campus_space_scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BookingHistoryActivity extends AppCompatActivity implements BookingAdapter.OnItemClickListener {

    private String spaceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        TextView spaceNameTextView = findViewById(R.id.textViewSpaceName);
        spaceName = getIntent().getStringExtra("SPACE_NAME");

        if (spaceName != null) {
            spaceNameTextView.setText(spaceName);
        }

        RecyclerView recyclerViewHistory = findViewById(R.id.recyclerViewHistory);

        List<Booking> historyList = new ArrayList<>();
        historyList.add(new Booking("2024-07-28", "10:00 AM - 11:00 AM", "Mid-term exam", "Accepted"));
        historyList.add(new Booking("2024-07-27", "02:00 PM - 03:00 PM", "Project discussion", "Rejected"));
        historyList.add(new Booking("2024-07-26", "11:00 AM - 12:00 PM", "Guest lecture", "Pending"));

        BookingAdapter adapter = new BookingAdapter(historyList, this);

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHistory.setAdapter(adapter);
    }

    @Override
    public void onItemClick(Booking booking) {
        Intent intent = new Intent(this, BookingDetailsActivity.class);
        intent.putExtra("SPACE_NAME", spaceName);
        intent.putExtra("BOOKED_BY", "Seiza"); // Dummy data
        intent.putExtra("DATE", booking.getDate());
        intent.putExtra("TIME_SLOT", booking.getTimeSlot());
        intent.putExtra("PURPOSE", booking.getPurpose());
        intent.putExtra("DESCRIPTION", "This is a sample description of the event. It can be a bit longer to see how it wraps."); // Dummy data
        intent.putExtra("STATUS", booking.getStatus());

        if ("Accepted".equals(booking.getStatus())) {
            intent.putExtra("APPROVED_OR_REJECTED_BY", "Sundar Sir"); // Dummy data
            intent.putExtra("HAS_LOR", true); // Dummy data
        } else if ("Rejected".equals(booking.getStatus())) {
            intent.putExtra("APPROVED_OR_REJECTED_BY", "Here Sir"); // Dummy data
            intent.putExtra("HAS_LOR", true); // Dummy data
        } else {
            intent.putExtra("HAS_LOR", true); // Dummy data for pending
        }

        startActivity(intent);
    }
}
