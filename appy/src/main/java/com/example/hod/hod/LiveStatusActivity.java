package com.example.hod.hod;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hod.R;
import com.example.hod.adapters.LiveStatusAdapter;
import com.example.hod.models.LiveStatusData;
import com.example.hod.models.Space;
import com.example.hod.repository.FirebaseRepository;
import com.example.hod.utils.Result;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LiveStatusActivity extends AppCompatActivity {

    private RecyclerView rvLiveStatus;
    private ProgressBar progressBar;
    private TextView noDataTextView;
    private LiveStatusAdapter adapter;
    private List<LiveStatusData> liveStatusList;
    private FirebaseRepository repo;
    private Map<String, ValueEventListener> activeListeners;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appy_live_status);

        rvLiveStatus = findViewById(R.id.rvLiveStatus);
        progressBar = findViewById(R.id.progressBar);
        noDataTextView = findViewById(R.id.noDataTextView);

        rvLiveStatus.setLayoutManager(new LinearLayoutManager(this));
        liveStatusList = new ArrayList<>();
        adapter = new LiveStatusAdapter(this, liveStatusList);
        rvLiveStatus.setAdapter(adapter);

        repo = new FirebaseRepository();
        activeListeners = new HashMap<>();

        updateHeader(getString(R.string.live_status), getString(R.string.subtitle_all_labs));

        loadAllSpaces();
        setupAutoRefresh();
    }

    private String listenerDate = "";

    private final android.os.Handler refreshHandler = new android.os.Handler();
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            // Check if date changed (Midnight transition)
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
            if (!currentDate.equals(listenerDate)) {
                // Remove all old listeners
                for (Map.Entry<String, ValueEventListener> entry : activeListeners.entrySet()) {
                    repo.removeLiveStatusListener(entry.getKey(), listenerDate, entry.getValue());
                }
                activeListeners.clear();
                liveStatusList.clear();
                adapter.notifyDataSetChanged();
                
                // Load again for the new date
                loadAllSpaces();
            } else {
                // Just force UI update for all current labs (the listeners provide the data)
                adapter.notifyDataSetChanged();
            }
            refreshHandler.postDelayed(this, 30000);
        }
    };

    private void setupAutoRefresh() {
        refreshHandler.postDelayed(refreshRunnable, 60000);
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
            if (result instanceof Result.Success) {
                List<Space> spaces = ((Result.Success<List<Space>>) result).data;
                if (spaces == null || spaces.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    noDataTextView.setVisibility(View.VISIBLE);
                } else {
                    noDataTextView.setVisibility(View.GONE);
                    setupLiveListeners(spaces);
                }
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, getString(R.string.error_failed_load_spaces), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLiveListeners(List<Space> spaces) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        listenerDate = currentDate;

        // For each space, attach a listener
        for (Space space : spaces) {
            String spaceId = space.getSpaceId();
            if (spaceId == null) continue;

            // repo now returns a map of all slots for the day
            ValueEventListener listener = (ValueEventListener) repo.getLiveSlotStatus(spaceId, currentDate, (Result<Map<String, LiveStatusData>> result) -> {
                if (result instanceof Result.Success) {
                    Map<String, LiveStatusData> dailySlots = ((Result.Success<Map<String, LiveStatusData>>) result).data;
                    if (dailySlots != null) {
                        // Find current slot locally
                        LiveStatusData currentData = findCurrentSlot(dailySlots);
                        if (currentData == null) {
                            // Off hours or no slots
                            currentData = new LiveStatusData("OFF_HOURS", null, null, currentDate);
                        }
                        currentData.spaceId = spaceId;
                        currentData.spaceName = space.getRoomName();
                        updateOrAddItem(currentData);
                    }
                }
            });
            activeListeners.put(spaceId, listener);
        }
        progressBar.setVisibility(View.GONE);
        rvLiveStatus.setVisibility(View.VISIBLE);
    }

    private LiveStatusData findCurrentSlot(Map<String, LiveStatusData> dailySlots) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = cal.get(java.util.Calendar.MINUTE);
        
        String currentNormalized = repo.normalizeSlotKey(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        
        LiveStatusData matchedData = dailySlots.get(currentNormalized);
        if (matchedData == null) {
            // Try one more time with exact 30-min boundary if fuzzy match failed
            int normalizedMin = (minute >= 30) ? 30 : 0;
            String boundaryKey = String.format(Locale.getDefault(), "%02d:%02d", hour, normalizedMin);
            matchedData = dailySlots.get(boundaryKey);
        }

        if (matchedData != null && matchedData.slotKey == null) {
             matchedData.slotKey = currentNormalized; 
        }
        return matchedData;
    }

    private void updateOrAddItem(LiveStatusData data) {
        int index = -1;
        for (int i = 0; i < liveStatusList.size(); i++) {
            if (liveStatusList.get(i).spaceId.equals(data.spaceId)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            liveStatusList.set(index, data);
            adapter.notifyItemChanged(index);
        } else {
            liveStatusList.add(data);
            // Sort by name if desired
            Collections.sort(liveStatusList, (o1, o2) -> o1.spaceName.compareToIgnoreCase(o2.spaceName));
            // After sort, indexes change, so we must notify DataSetChanged here or insert then sort properly
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacks(refreshRunnable);
        // Crucial: remove all listeners to prevent memory leaks and unnecessary data usage
        if (repo != null && activeListeners != null) {
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            for (Map.Entry<String, ValueEventListener> entry : activeListeners.entrySet()) {
                repo.removeLiveStatusListener(entry.getKey(), currentDate, entry.getValue());
            }
            activeListeners.clear();
            Log.d("LiveStatusActivity", "Cleaned up listeners");
        }
    }
}