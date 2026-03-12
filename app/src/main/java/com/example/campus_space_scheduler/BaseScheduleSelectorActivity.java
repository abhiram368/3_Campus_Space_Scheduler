package com.example.campus_space_scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.util.*;

public abstract class BaseScheduleSelectorActivity extends AppCompatActivity {

    protected Spinner spaceSpinner;
    protected CalendarView calendarView;

    protected List<String> spaceIds = new ArrayList<>();
    protected List<String> spaceNames = new ArrayList<>();

    protected String selectedSpaceId;

    protected abstract boolean editFlag();
    protected abstract boolean detailedFlag();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.a_activity_select_schedule);

        spaceSpinner = findViewById(R.id.spaceSpinner);
        calendarView = findViewById(R.id.calendarView);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        spaceNames
                );

        spaceSpinner.setAdapter(adapter);

        fetchSpaces(adapter);

        spaceSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               android.view.View view,
                                               int position,
                                               long id) {

                        selectedSpaceId = spaceIds.get(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
        ImageButton backBtn = findViewById(R.id.backButton);

        backBtn.setOnClickListener(v -> finish());

        Button btn = findViewById(R.id.viewScheduleBtn);

        btn.setOnClickListener(v -> {

            if(selectedSpaceId == null) return;

            Calendar cal = Calendar.getInstance();

            Intent intent = new Intent(
                    BaseScheduleSelectorActivity.this,
                    WeekScheduleActivity.class
            );

            intent.putExtra("spaceId", selectedSpaceId);
            intent.putExtra("dateMillis", cal.getTimeInMillis());
            intent.putExtra("edit", editFlag());
            intent.putExtra("detailed", detailedFlag());

            startActivity(intent);
        });
    }

    private void fetchSpaces(ArrayAdapter<String> adapter){

        DatabaseReference ref =
                FirebaseDatabase.getInstance().getReference("spaces");

        ref.addListenerForSingleValueEvent(new ValueEventListener(){

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){

                spaceIds.clear();
                spaceNames.clear();

                for(DataSnapshot ds:snapshot.getChildren()){

                    String id = ds.getKey();
                    String name =
                            ds.child("roomName").getValue(String.class);

                    if(id!=null && name!=null){

                        spaceIds.add(id);
                        spaceNames.add(name);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error){}
        });
    }
}