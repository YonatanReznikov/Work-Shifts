package com.example.work_shifts.Fragments.Worker;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ShiftAdapter extends RecyclerView.Adapter<ShiftAdapter.ShiftViewHolder> {
    private List<Shift> shiftList;
    private final String today;

    public ShiftAdapter(List<Shift> shiftList) {
        this.shiftList = shiftList;
        this.today = getTodayName();
    }

    @NonNull
    @Override
    public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shift_item, parent, false);
        return new ShiftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {
        Shift shift = shiftList.get(position);

        // Show day header only if it's the first occurrence
        if (position == 0 || !shift.getDay().equals(shiftList.get(position - 1).getDay())) {
            holder.dayTextView.setVisibility(View.VISIBLE);
            holder.dayTextView.setText(shift.getDay());
        } else {
            holder.dayTextView.setVisibility(View.GONE);
        }

        // Set shift details
        holder.timeTextView.setText(String.format("%s - %s", shift.getStartTime(), shift.getEndTime()));
        holder.workerTextView.setText(shift.getWorkerName());

        // Highlight today's shifts
        if (shift.getDay().equals(today)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFD700")); // Gold for highlighting
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE); // Reset default color
        }

        // Add Shift to Google Calendar
        holder.addToCalendarBtn.setOnClickListener(v -> addShiftToCalendar(v, shift));
    }

    @Override
    public int getItemCount() {
        return shiftList.size();
    }

    public void updateShifts(List<Shift> newShifts) {
        this.shiftList = newShifts;
        notifyDataSetChanged();
        Log.d("ShiftAdapter", "Shifts updated. New count: " + shiftList.size());
    }

    static class ShiftViewHolder extends RecyclerView.ViewHolder {
        TextView dayTextView, timeTextView, workerTextView;
        Button addToCalendarBtn;

        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);
            dayTextView = itemView.findViewById(R.id.shiftDay);
            timeTextView = itemView.findViewById(R.id.shiftTime);
            workerTextView = itemView.findViewById(R.id.shiftWorker);
            addToCalendarBtn = itemView.findViewById(R.id.addToCalendarBtn);
        }
    }

    private String getTodayName() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private void addShiftToCalendar(View view, Shift shift) {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(Uri.parse("content://com.android.calendar/events"));
        intent.putExtra(CalendarContract.Events.TITLE, "Work Shift");
        intent.putExtra(CalendarContract.Events.DESCRIPTION, "Shift: " + shift.getStartTime() + " - " + shift.getEndTime());
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, "Workplace");

        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(shift.getStartTime().split(":")[0]));
        startCal.set(Calendar.MINUTE, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(shift.getEndTime().split(":")[0]));
        endCal.set(Calendar.MINUTE, 0);

        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startCal.getTimeInMillis());
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endCal.getTimeInMillis());

        if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
            view.getContext().startActivity(intent);
        } else {
            Log.e("ShiftAdapter", "No Calendar app found!");
        }
    }
}
