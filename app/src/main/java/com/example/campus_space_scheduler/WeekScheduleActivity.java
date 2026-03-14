package com.example.campus_space_scheduler;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_space_scheduler.enums.SlotStatus;
import com.example.campus_space_scheduler.helper.SlotColorMapper;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class WeekScheduleActivity extends AppCompatActivity {

    private TableLayout table;
    private DatabaseReference db;

    private String spaceId;
    private Calendar selectedDate;

    private boolean editMode;
    private boolean detailedMode;

    private Map<String, Map<String,Object>> slotCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        spaceId = getIntent().getStringExtra("spaceId");

        if(spaceId == null){
            Toast.makeText(this,"spaceId missing",Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        long millis = getIntent().getLongExtra("dateMillis",-1);

        if(millis == -1){
            Toast.makeText(this,"date missing",Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_week_schedule);

        ImageButton backBtn = findViewById(R.id.backButton);
        backBtn.setOnClickListener(v -> finish());

        table = findViewById(R.id.weekPreview);
        db = FirebaseDatabase.getInstance().getReference();

        spaceId = getIntent().getStringExtra("spaceId");

        long dateMillis = getIntent().getLongExtra("dateMillis",0);

        selectedDate = Calendar.getInstance();
        selectedDate.setTimeInMillis(dateMillis);

        editMode = getIntent().getBooleanExtra("edit",false);
        detailedMode = getIntent().getBooleanExtra("detailed",false);

        if(editMode) detailedMode = true;

        populateGrid();
        loadWeekFromFirebase();
    }

    private void populateGrid(){

        table.removeAllViews();

        SimpleDateFormat dayFormat =
                new SimpleDateFormat("EEE\ndd",Locale.getDefault());

        TableRow header = new TableRow(this);

        header.addView(createHeaderCell("Time"));

        Calendar cal = (Calendar) selectedDate.clone();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);

        for(int i=0;i<7;i++){

            header.addView(createHeaderCell(dayFormat.format(cal.getTime())));
            cal.add(Calendar.DAY_OF_MONTH,1);
        }

        table.addView(header);

        for(int slot=0;slot<30;slot++){

            TableRow row = new TableRow(this);

            int h1 = 8 + slot/2;
            String m1 = slot%2==0 ? "00":"30";

            int h2 = 8 + (slot+1)/2;
            String m2 = (slot+1)%2==0 ? "00":"30";

            row.addView(createHeaderCell(h1+":"+m1+"\n"+h2+":"+m2));

            for(int day=0;day<7;day++){

                View cell = new View(this);

                TableRow.LayoutParams params =
                        new TableRow.LayoutParams(0,160,1f);

                params.setMargins(1,1,1,1);

                cell.setLayoutParams(params);
                // ---------------------------------------------------------
                // UI COLOR CONFIGURATION: Default Slot Background
                // ---------------------------------------------------------
                int defaultSlotBackgroundColor = Color.WHITE;
                cell.setBackgroundColor(defaultSlotBackgroundColor);

                cell.setTag(day+"_"+slot);

                int finalDay = day;
                int finalSlot = slot;

                cell.setOnClickListener(v -> onSlotClicked(finalDay,finalSlot));

                row.addView(cell);
            }

            table.addView(row);
        }
    }

    private TextView createHeaderCell(String text){

        TextView tv = new TextView(this);

        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(10f);
        tv.setPadding(4,8,4,8);

        // ---------------------------------------------------------
        // UI COLOR CONFIGURATION: Header Background (Time/Days)
        // ---------------------------------------------------------
        String headerBackgroundHex = "#F5F5F5";
        int headerBackgroundColor = Color.parseColor(headerBackgroundHex);
        tv.setBackgroundColor(headerBackgroundColor);

        TableRow.LayoutParams params =
                new TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.MATCH_PARENT,
                        1f);

        params.setMargins(1,1,1,1);

        tv.setLayoutParams(params);

        return tv;
    }

    private void loadWeekFromFirebase(){

        List<String> weekDates = getWeekDates();

        for(int day=0;day<weekDates.size();day++){

            String date = weekDates.get(day);
            int dayIndex = day;

            String scheduleId = spaceId+"_"+date;

            db.child("schedules")
                    .child(scheduleId)
                    .child("slots")
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        if(!snapshot.exists()) return;

                        for(DataSnapshot slot:snapshot.getChildren()){

                            Map<String,Object> data =
                                    (Map<String,Object>) slot.getValue();

                            if(data==null) continue;

                            String start = (String) data.get("start");

                            if(start==null) continue;

                            int slotIndex = calculateSlotIndex(start);

                            String key = dayIndex+"_"+slotIndex;

                            slotCache.put(key,data);

                            updateSlotColor(dayIndex,slotIndex,data);
                        }
                    });
        }
    }

    private void updateSlotColor(int day,int slot,Map<String,Object> data){

        View cell = table.findViewWithTag(day+"_"+slot);

        if(cell==null) return;

        try{

            String statusStr = String.valueOf(data.get("status"));

            SlotStatus status =
                    SlotStatus.valueOf(statusStr.toUpperCase());

            cell.setBackgroundColor(
                    SlotColorMapper.getColor(status)
            );

        }catch(Exception e){

            // ---------------------------------------------------------
            // UI COLOR CONFIGURATION: Fallback Color if mapping fails
            // ---------------------------------------------------------
            int slotErrorFallbackColor = Color.LTGRAY;
            cell.setBackgroundColor(slotErrorFallbackColor);
        }
    }

    private int calculateSlotIndex(String start){

        String[] parts = start.split(":");

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        return (hour-8)*2 + (minute>=30?1:0);
    }

    private List<String> getWeekDates(){

        List<String> dates = new ArrayList<>();

        Calendar cal = (Calendar) selectedDate.clone();

        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);

        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());

        for(int i=0;i<7;i++){

            dates.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH,1);
        }

        return dates;
    }

    private void onSlotClicked(int day,int slot){

        String key = day+"_"+slot;

        Map<String,Object> data = slotCache.get(key);

        if(data==null){

            Toast.makeText(this,"Empty slot",Toast.LENGTH_SHORT).show();
            return;
        }

        if(!detailedMode && !editMode) return;

        showSlotDialog(day,slot,data);
    }

    private void showSlotDialog(int day,int slot,Map<String,Object> data){

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        Map<String,EditText> editors = new HashMap<>();

        for(String key:data.keySet()){

            if(key.equals("start") || key.equals("end")) continue;

            TextView label = new TextView(this);
            label.setText(key);

            EditText value = new EditText(this);
            value.setText(String.valueOf(data.get(key)));

            if(!editMode) value.setEnabled(false);

            layout.addView(label);
            layout.addView(value);

            editors.put(key,value);
        }

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this)
                        .setTitle("Slot Details")
                        .setView(layout)
                        .setPositiveButton("Close",(d,w)->{});

        if(editMode){

            builder.setNeutralButton("Save",(d,w)->{

                Map<String,Object> updates = new HashMap<>();

                for(String key:editors.keySet()){

                    updates.put(key,
                            editors.get(key).getText().toString());
                }

                saveSlot(day,slot,updates);
            });
        }

        builder.show();
    }

    private void saveSlot(int day,int slot,Map<String,Object> updates){

        List<String> weekDates = getWeekDates();

        String date = weekDates.get(day);

        String scheduleId = spaceId+"_"+date;

        String slotKey = null;

        Map<String,Object> cached =
                slotCache.get(day+"_"+slot);

        if(cached!=null && cached.containsKey("slotId"))
            slotKey = String.valueOf(cached.get("slotId"));

        if(slotKey==null) return;

        db.child("schedules")
                .child(scheduleId)
                .child("slots")
                .child(slotKey)
                .updateChildren(updates);
    }
}