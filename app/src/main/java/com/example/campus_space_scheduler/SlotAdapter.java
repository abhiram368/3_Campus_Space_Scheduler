package com.example.campus_space_scheduler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_space_scheduler.enums.SlotStatus;
import com.example.campus_space_scheduler.helper.SlotColorMapper;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.ViewHolder>{

    private final String scheduleId;
    private List<Map<String,Object>> list;

    private boolean editMode;
    private boolean detailedMode;
    private Context context;

    public SlotAdapter(List<Map<String,Object>> list,
                       boolean editMode,
                       boolean detailedMode,
                       Context context,
                       String scheduleId){

        // remove invalid slots
        list.removeIf(slot -> {
            String start = (String) slot.get("start");
            String end = (String) slot.get("end");

            return start == null || start.trim().isEmpty()
                    || end == null || end.trim().isEmpty();
        });

        this.list = list;
        this.editMode = editMode;
        this.detailedMode = detailedMode;
        this.context = context;
        this.scheduleId = scheduleId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_slot_timeline, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position){

        Map<String,Object> data = list.get(position);

        String start = data.get("start") != null ? data.get("start").toString() : "--";
        String end = data.get("end") != null ? data.get("end").toString() : "--";
        String statusStr = data.get("status") != null ? data.get("status").toString() : "AVAILABLE";

        holder.time.setText(formatHour(start));
        holder.range.setText(start + " - " + end);
        holder.status.setText(statusStr);

        holder.block.setOnClickListener(v -> {
            if(!detailedMode && !editMode) return;
            showSlotDialog(data, position);
        });

        // safe enum
        SlotStatus status;
        try {
            status = SlotStatus.valueOf(statusStr.toUpperCase());
        } catch (Exception e) {
            status = SlotStatus.AVAILABLE;
        }

        holder.block.setBackgroundTintList(
                ColorStateList.valueOf(
                        SlotColorMapper.getColor(status)
                )
        );
    }

    @Override
    public int getItemCount(){
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        TextView time, range, status;
        LinearLayout block;

        public ViewHolder(@NonNull View itemView){
            super(itemView);

            time = itemView.findViewById(R.id.slotTime);
            range = itemView.findViewById(R.id.slotRange);
            status = itemView.findViewById(R.id.slotStatus);
            block = itemView.findViewById(R.id.slotBlock);
        }
    }

    private String formatHour(String time){
        if(time == null || !time.contains(":")) return "--";

        try{
            int hour = Integer.parseInt(time.split(":")[0]);

            if(hour == 0) return "12 AM";
            if(hour < 12) return hour + " AM";
            if(hour == 12) return "12 PM";
            return (hour - 12) + " PM";

        }catch(Exception e){
            return "--";
        }
    }

    private void showSlotDialog(Map<String,Object> data, int position){

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 10);

        Map<String, View> editors = new HashMap<>();

        for(String key : data.keySet()){

            if(key.equals("slotId") || key.equals("start") || key.equals("end")) continue;

            TextView label = new TextView(context);
            label.setText(key.toUpperCase());
            label.setTextSize(12);
            label.setTextColor(Color.GRAY);
            layout.addView(label);

            if(key.equals("status")){

                Spinner spinner = new Spinner(context);

                String[] options = new String[SlotStatus.values().length];
                for(int i = 0; i < options.length; i++){
                    options[i] = SlotStatus.values()[i].name();
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_spinner_dropdown_item,
                        options
                );

                spinner.setAdapter(adapter);

                String current = data.get(key) != null ? data.get(key).toString() : "AVAILABLE";
                int selectedIndex = 0;

                for(int i = 0; i < options.length; i++){
                    if(options[i].equalsIgnoreCase(current)){
                        selectedIndex = i;
                        break;
                    }
                }

                spinner.setSelection(selectedIndex);

                if(!editMode) spinner.setEnabled(false);

                layout.addView(spinner);
                editors.put(key, spinner);
            }
            else{
                EditText value = new EditText(context);
                value.setText(String.valueOf(data.get(key)));

                if(!editMode) value.setEnabled(false);

                layout.addView(value);
                editors.put(key, value);
            }
        }

        new AlertDialog.Builder(context)
                .setTitle("Slot Details")
                .setView(layout)
                .setPositiveButton("Close", null)
                .setNegativeButton(editMode ? "Save" : null, (d,w)->{

                    Map<String,Object> updates = new HashMap<>();

                    for(String key : editors.keySet()){
                        View v = editors.get(key);

                        if(v instanceof Spinner){
                            updates.put(key,
                                    ((Spinner) v).getSelectedItem().toString());
                        }
                        else if(v instanceof EditText){
                            updates.put(key,
                                    ((EditText) v).getText().toString());
                        }
                    }

                    saveSlot(data, updates);

                    // update UI instantly
                    data.putAll(updates);
                    notifyItemChanged(position);
                })
                .show();
    }

    private void saveSlot(Map<String,Object> data,
                          Map<String,Object> updates){

        String slotId = data.get("slotId") != null ? data.get("slotId").toString() : null;

        if(slotId == null){
            Toast.makeText(context, "slotId missing", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child("schedules")
                .child(scheduleId)
                .child("slots")
                .child(slotId)
                .updateChildren(updates);
    }
}