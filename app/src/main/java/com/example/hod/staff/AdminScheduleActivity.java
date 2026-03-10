package com.example.hod.staff;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;

public class AdminScheduleActivity extends AppCompatActivity {

    EditText etSlot1;
    Button btnEdit, btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_schedule);

        CalendarView calendarView = findViewById(R.id.calendarView);
        TextView tvSelectedDate = findViewById(R.id.tvSelectedDate);
        etSlot1 = findViewById(R.id.etSlot1);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            tvSelectedDate.setText("Schedule for: " + dayOfMonth + "/" + (month + 1) + "/" + year);
        });

        btnEdit.setOnClickListener(v -> {
            etSlot1.setEnabled(true);
            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
        });

        btnSave.setOnClickListener(v -> {
            etSlot1.setEnabled(false);
            btnSave.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
        });
    }
}