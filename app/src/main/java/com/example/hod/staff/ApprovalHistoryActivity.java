package com.example.hod.staff;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hod.R;

public class ApprovalHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approval_history);

        View historyItem1 = findViewById(R.id.history_item_1);
        View historyItem2 = findViewById(R.id.history_item_2);

        View.OnClickListener listener = v -> {
            startActivity(new Intent(
                    ApprovalHistoryActivity.this,
                    LabCompletedRequestDetailActivity.class
            ));
        };

        if (historyItem1 != null) historyItem1.setOnClickListener(listener);
        if (historyItem2 != null) historyItem2.setOnClickListener(listener);
    }
}
