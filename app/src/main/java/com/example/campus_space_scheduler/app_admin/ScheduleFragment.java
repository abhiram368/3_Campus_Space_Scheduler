package com.example.campus_space_scheduler.app_admin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.example.campus_space_scheduler.DetailedViewScheduleActivity;
import com.example.campus_space_scheduler.EditScheduleActivity;
import com.example.campus_space_scheduler.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.example.campus_space_scheduler.enums.SlotStatus;
import com.example.campus_space_scheduler.helper.SlotColorMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private Button uploadJsonBtn;
    private Button viewScheduleBtn;
    private Button editScheduleBtn;

    private ActivityResultLauncher<String> jsonPicker;
    private DatabaseReference db;

    private String currentSpaceId;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.a_fragment_schedule, container, false);

        db = FirebaseDatabase.getInstance().getReference();

//        FirebaseUtils.setCurrentSpaceId("-Ol1cbQAGbNgCKlDPia8");

        uploadJsonBtn = view.findViewById(R.id.uploadJsonBtn);
        viewScheduleBtn = view.findViewById(R.id.viewScheduleBtn);
        editScheduleBtn = view.findViewById(R.id.editScheduleBtn);

        // --- FRESH START LOCATION ---
        // Implement your new display logic or dashboard widgets here.
        TextView header = view.findViewById(R.id.currentSpaceHeader);
        TableLayout table = view.findViewById(R.id.weekPreview);

        loadCurrentSpace(view, table, header);
        // ----------------------------

        jsonPicker =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {
                            if (uri != null) {
                                readJson(uri);
                            }
                        });

        setupButtons();

        return view;
    }

    private void setupButtons() {
        uploadJsonBtn.setOnClickListener(v -> {
            Log.d("SCHEDULE", "Upload clicked");
            jsonPicker.launch("*/*");
        });

        viewScheduleBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DetailedViewScheduleActivity.class);
            startActivity(intent);
        });

        editScheduleBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditScheduleActivity.class);
            startActivity(intent);
        });
    }

    private void loadCurrentSpace(View view, TableLayout table, TextView header) {

        db.child("appConfig").child("currentSpaceId")
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {

                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            header.setText("No Space Selected");
                            header.setTextColor(Color.RED);
                            table.setVisibility(View.GONE);
                            return;
                        }

                        currentSpaceId = snapshot.getValue(String.class);

                        loadRoomNameAndDate(view, table, header);
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        Log.e("SCHEDULE", "Failed to load current space", error.toException());
                    }
                });
    }

    private void loadRoomNameAndDate(View view, TableLayout table, TextView header) {

        db.child("spaces").child(currentSpaceId).child("roomName").get()
                .addOnSuccessListener(snapshot -> {

                    String roomName = snapshot.getValue(String.class);

                    if (roomName == null) roomName = "Unknown Room";

                    SimpleDateFormat dateFormat =
                            new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());

                    String today = dateFormat.format(Calendar.getInstance().getTime());

                    header.setText(roomName + " • " + today);
                    header.setTextColor(Color.WHITE);

                    table.setVisibility(View.VISIBLE);

                    populateGrid(view, table, currentSpaceId);
                    loadWeekFromFirebase(currentSpaceId, table);
                });
    }

    private void readJson(Uri uri) {
        try {
            InputStream inputStream =
                    requireContext().getContentResolver().openInputStream(uri);

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            reader.close();
            inputStream.close();

            String json = builder.toString();

            new Thread(() -> {
                parseSchedule(json);
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseSchedule(String jsonString) {
        try {
            JSONObject root = new JSONObject(jsonString);
            JSONObject schedules = root.getJSONObject("schedules");

            Iterator<String> spaceIds = schedules.keys();

            while (spaceIds.hasNext()) {
                String spaceId = spaceIds.next();
                JSONObject spaceSchedules = schedules.getJSONObject(spaceId);

                Iterator<String> dates = spaceSchedules.keys();

                while (dates.hasNext()) {
                    String date = dates.next();
                    JSONObject daySchedule = spaceSchedules.getJSONObject(date);
                    uploadSchedule(spaceId, date, daySchedule);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadSchedule(String spaceId, String date, JSONObject daySchedule)
            throws JSONException {

        String scheduleId = spaceId + "_" + date;

        Map<String, Object> scheduleDoc = new HashMap<>();
        scheduleDoc.put("spaceId", spaceId);
        scheduleDoc.put("date", date);

        JSONArray slots = daySchedule.getJSONArray("slots");
        Map<String, Object> slotsMap = new HashMap<>();

        for (int i = 0; i < slots.length(); i++) {
            JSONObject slot = slots.getJSONObject(i);

            Map<String, Object> slotDoc = new HashMap<>();
            slotDoc.put("start", slot.getString("start"));
            slotDoc.put("end", slot.getString("end"));
            slotDoc.put("status", slot.getString("status"));

            String slotId = slot.getString("start").replace(":", "");
            slotsMap.put(slotId, slotDoc);
        }

        scheduleDoc.put("slots", slotsMap);

        db.child("schedules")
                .child(scheduleId)
                .setValue(scheduleDoc)
                .addOnSuccessListener(aVoid ->
                        Log.d("SCHEDULE", "Uploaded: " + scheduleId))
                .addOnFailureListener(e ->
                        Log.e("SCHEDULE", "Write failed", e));
    }

    private void loadWeekFromFirebase(String spaceId, TableLayout table) {
        List<String> weekDates = getCurrentWeekDates();

        for (int j = 0; j < weekDates.size(); j++) {
            String date = weekDates.get(j);
            int dayIndex = j;
            String scheduleId = spaceId + "_" + date;

            // Path: schedules -> {spaceId}_{date} -> slots
            db.child("schedules").child(scheduleId).child("slots").get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            for (com.google.firebase.database.DataSnapshot slotSnapshot : snapshot.getChildren()) {
                                String start = slotSnapshot.child("start").getValue(String.class);
                                String status = slotSnapshot.child("status").getValue(String.class);

                                if (start != null && status != null) {
                                    int slotIndex = calculateSlotIndex(start);
                                    updateSlotColor(table, dayIndex, slotIndex, status);
                                }
                            }
                        }
                    });
        }
    }

    private int calculateSlotIndex(String startTime) {
        String[] parts = startTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return (hour - 8) * 2 + (minute >= 30 ? 1 : 0);
    }

    private void populateGrid(View rootView, TableLayout table, String spaceId) {
        table.removeAllViews();

        String[] days = {"M", "T", "W", "Th", "F", "Sa", "S"};

        // 1. Column Headings (Days)
        TableRow headerRow = new TableRow(getContext());
        headerRow.addView(createCell("Time", true));
        for (String day : days) {
            headerRow.addView(createCell(day, true));
        }
        table.addView(headerRow);

        // 2. Row Headings (Times) and Data
        for (int i = 0; i < 30; i++) {
            TableRow row = new TableRow(getContext());

            int h1 = 8 + (i / 2); String m1 = (i % 2 == 0) ? "00" : "30";
            int h2 = 8 + ((i + 1) / 2); String m2 = ((i + 1) % 2 == 0) ? "00" : "30";
            String timeRange = h1 + ":" + m1 + "\n" + h2 + ":" + m2;

            row.addView(createCell(timeRange, true));

            for (int j = 0; j < 7; j++) {
                View cell = new View(getContext());
                // Weight 1f allows horizontal stretching to match screen width
                TableRow.LayoutParams params = new TableRow.LayoutParams(0, 160, 1f);
                params.setMargins(1, 1, 1, 1); // Border effect
                cell.setLayoutParams(params);

                cell.setBackgroundColor(Color.WHITE);
                cell.setTag(j + "_" + i);
                row.addView(cell);
            }
            table.addView(row);
        }
    }

    private TextView createCell(String text, boolean isHeader) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(4, 8, 4, 8);
        tv.setTextSize(10f);
        tv.setBackgroundColor(isHeader ? Color.parseColor("#F5F5F5") : Color.WHITE);
        if (isHeader) tv.setTypeface(null, android.graphics.Typeface.BOLD);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1f);
        params.setMargins(1, 1, 1, 1);
        tv.setLayoutParams(params);
        return tv;
    }

    private List<String> getCurrentWeekDates() {
        List<String> dates = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        // Start at the first day of the week (usually Monday or Sunday depending on Locale)
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (int i = 0; i < 7; i++) {
            dates.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dates;
    }

    private void updateSlotColor(TableLayout table, int dayIndex, int slotIndex, String statusString) {
        View dot = table.findViewWithTag(dayIndex + "_" + slotIndex);
        if (dot != null) {
            try {
                // Converts String (e.g., "AVAILABLE") to SlotStatus Enum
                SlotStatus status = SlotStatus.valueOf(statusString.toUpperCase());
                // Gets color from your Mapper
                dot.setBackgroundColor(SlotColorMapper.getColor(status));
            } catch (IllegalArgumentException e) {
                dot.setBackgroundColor(Color.LTGRAY); // Fallback for unknown status
            }
        }
    }
}