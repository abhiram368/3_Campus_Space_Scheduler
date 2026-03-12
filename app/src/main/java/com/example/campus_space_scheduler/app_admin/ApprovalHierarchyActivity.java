package com.example.campus_space_scheduler.app_admin;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_space_scheduler.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ApprovalHierarchyActivity extends AppCompatActivity {

    Spinner labsHod, labsFaculty, labsStaff;
    Spinner hallsHod, hallsCsed, hallsIncharge;

    DatabaseReference db;

    Integer[] orders = {1, 2, 3};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_activity_approval_hierarchy);

//        FirebaseUtils.ensureApprovalHierarchy();

        db = FirebaseDatabase.getInstance().getReference("approvalHierarchy");

        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                orders
        );

        labsHod = findViewById(R.id.rowLabsHod).findViewById(R.id.spinnerOrder);
        labsFaculty = findViewById(R.id.rowLabsFaculty).findViewById(R.id.spinnerOrder);
        labsStaff = findViewById(R.id.rowLabsStaff).findViewById(R.id.spinnerOrder);

        hallsHod = findViewById(R.id.rowHallsHod).findViewById(R.id.spinnerOrder);
        hallsCsed = findViewById(R.id.rowHallsCsed).findViewById(R.id.spinnerOrder);
        hallsIncharge = findViewById(R.id.rowHallsIncharge).findViewById(R.id.spinnerOrder);

        ((TextView)findViewById(R.id.rowLabsHod).findViewById(R.id.tvRole))
                .setText("HoD");

        ((TextView)findViewById(R.id.rowLabsFaculty).findViewById(R.id.tvRole))
                .setText("Faculty Incharge");

        ((TextView)findViewById(R.id.rowLabsStaff).findViewById(R.id.tvRole))
                .setText("Staff Incharge");

        ((TextView)findViewById(R.id.rowHallsHod).findViewById(R.id.tvRole))
                .setText("HoD");

        ((TextView)findViewById(R.id.rowHallsCsed).findViewById(R.id.tvRole))
                .setText("CSED Staff");

        ((TextView)findViewById(R.id.rowHallsIncharge).findViewById(R.id.tvRole))
                .setText("Hall Incharge");

        labsHod.setAdapter(adapter);
        labsFaculty.setAdapter(adapter);
        labsStaff.setAdapter(adapter);
        hallsHod.setAdapter(adapter);
        hallsCsed.setAdapter(adapter);
        hallsIncharge.setAdapter(adapter);

        loadHierarchy();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
    }

    private void loadHierarchy() {

        db.child("labs").get().addOnSuccessListener(snapshot -> {

            Long hod = snapshot.child("HoD").getValue(Long.class);
            Long faculty = snapshot.child("Faculty Incharge").getValue(Long.class);
            Long staff = snapshot.child("Staff Incharge").getValue(Long.class);

            if (hod != null) labsHod.setSelection(hod.intValue() - 1);
            if (faculty != null) labsFaculty.setSelection(faculty.intValue() - 1);
            if (staff != null) labsStaff.setSelection(staff.intValue() - 1);
        });

        db.child("halls").get().addOnSuccessListener(snapshot -> {

            Long hod = snapshot.child("HoD").getValue(Long.class);
            Long csed = snapshot.child("CSED Staff").getValue(Long.class);
            Long hall = snapshot.child("Hall Incharge").getValue(Long.class);

            if (hod != null) hallsHod.setSelection(hod.intValue() - 1);
            if (csed != null) hallsCsed.setSelection(csed.intValue() - 1);
            if (hall != null) hallsIncharge.setSelection(hall.intValue() - 1);
        });
    }

    private void save() {

        Map<String, Object> labs = new HashMap<>();
        labs.put("HoD", labsHod.getSelectedItem());
        labs.put("Faculty Incharge", labsFaculty.getSelectedItem());
        labs.put("Staff Incharge", labsStaff.getSelectedItem());

        Map<String, Object> halls = new HashMap<>();
        halls.put("HoD", hallsHod.getSelectedItem());
        halls.put("CSED Staff", hallsCsed.getSelectedItem());
        halls.put("Hall Incharge", hallsIncharge.getSelectedItem());

        db.child("labs").setValue(labs);
        db.child("halls").setValue(halls);

        Toast.makeText(this, "Hierarchy saved", Toast.LENGTH_SHORT).show();
    }
}