package com.example.hod.hod;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campussync.appy.R;
import com.example.hod.adapters.SpaceSelectionAdapter;
import com.example.hod.models.Space;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;

import java.util.ArrayList;
import java.util.List;

public class HodViewScheduleHomeActivity extends AppCompatActivity {

    private RecyclerView rvSpaces;
    private ProgressBar progressBar;
    private TextView noDataTextView;
    private SpaceSelectionAdapter adapter;
    private List<Space> spaceList;
    private FirebaseRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_view_schedule_home);

        rvSpaces = findViewById(R.id.rvSpaces);
        progressBar = findViewById(R.id.progressBar);
        noDataTextView = findViewById(R.id.noDataTextView);

        rvSpaces.setLayoutManager(new LinearLayoutManager(this));
        spaceList = new ArrayList<>();
        adapter = new SpaceSelectionAdapter(this, spaceList, space -> {
            Intent intent = new Intent(this, HodSpaceScheduleActivity.class);
            intent.putExtra("labId", space.getSpaceId());
            intent.putExtra("roomName", space.getRoomName());
            startActivity(intent);
        });
        rvSpaces.setAdapter(adapter);

        repo = new FirebaseRepository();

        updateHeader("View Schedule", "Select a space");
        loadAllSpaces();
    }

    private void updateHeader(String title, String subtitle) {
        TextView tvTitle = findViewById(R.id.header_title);
        TextView tvSubtitle = findViewById(R.id.header_subtitle);
        View btnBack = findViewById(R.id.btnBack);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSubtitle != null) tvSubtitle.setText(subtitle);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void loadAllSpaces() {
        progressBar.setVisibility(View.VISIBLE);
        repo.getAllSpaces(result -> {
            progressBar.setVisibility(View.GONE);
            if (result instanceof Result.Success) {
                List<Space> spaces = ((Result.Success<List<Space>>) result).data;
                if (spaces == null || spaces.isEmpty()) {
                    noDataTextView.setVisibility(View.VISIBLE);
                    rvSpaces.setVisibility(View.GONE);
                } else {
                    noDataTextView.setVisibility(View.GONE);
                    rvSpaces.setVisibility(View.VISIBLE);
                    spaceList.clear();
                    spaceList.addAll(spaces);
                    adapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(this, getString(R.string.error_failed_load_spaces), Toast.LENGTH_SHORT).show();
            }
        });
    }
}