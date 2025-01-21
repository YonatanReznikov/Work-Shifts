package com.example.work_shifts.Fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.R;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private final ArrayList<LocalTime> hours;

    public CalendarAdapter(ArrayList<LocalTime> hours) {
        this.hours = hours;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.calendar_cell, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        LocalTime hour = hours.get(position);
        holder.hourText.setText(hour.format(DateTimeFormatter.ofPattern("hh:mm a")));
    }

    @Override
    public int getItemCount() {
        return hours.size();
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        public final TextView hourText;
        public final TextView eventText;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            hourText = itemView.findViewById(R.id.hourText);
            eventText = itemView.findViewById(R.id.eventText);
        }
    }
}