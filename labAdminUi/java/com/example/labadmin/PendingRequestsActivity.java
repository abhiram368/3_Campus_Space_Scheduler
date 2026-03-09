package com.example.labadmin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PendingRequestsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        RecyclerView rv = findViewById(R.id.rvPendingRequests);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // Mock data for display
        List<Request> requests = new ArrayList<>();
        requests.add(new Request("Dr. Smith", "Main Hall", "Oct 28, 2023", "Slot 1 (9-11 AM)", "Faculty"));
        requests.add(new Request("Student Council", "Seminar Room", "Oct 29, 2023", "Slot 3 (2-4 PM)", "Student Org"));
        requests.add(new Request("Prof. Jones", "Lab 101", "Oct 30, 2023", "Slot 2 (11-1 PM)", "Faculty"));

        rv.setAdapter(new RequestAdapter(requests, request -> {
            Intent intent = new Intent(this, RequestDetailsActivity.class);
            intent.putExtra("bookedBy", request.bookedBy);
            intent.putExtra("hallName", request.hallName);
            intent.putExtra("date", request.date);
            intent.putExtra("slot", request.slot);
            intent.putExtra("role", request.role);
            startActivity(intent);
        }));
    }

    static class Request {
        String bookedBy, hallName, date, slot, role;
        Request(String b, String h, String d, String s, String r) {
            bookedBy = b; hallName = h; date = d; slot = s; role = r;
        }
    }

    static class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {
        private final List<Request> requests;
        private final OnClickListener listener;

        interface OnClickListener { void onClick(Request request); }

        RequestAdapter(List<Request> requests, OnClickListener listener) {
            this.requests = requests;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_request, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Request r = requests.get(position);
            holder.name.setText(r.bookedBy);
            holder.info.setText(r.hallName + " | " + r.date + " | " + r.slot);
            holder.itemView.setOnClickListener(v -> listener.onClick(r));
        }

        @Override
        public int getItemCount() { return requests.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, info;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.txtRequesterName);
                info = v.findViewById(R.id.txtRequestInfo);
            }
        }
    }
}