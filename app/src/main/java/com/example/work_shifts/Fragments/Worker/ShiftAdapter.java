package com.example.work_shifts.Fragments.Worker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShiftAdapter extends RecyclerView.Adapter<ShiftAdapter.ShiftViewHolder> {
    private List<Shift> shiftList;
    private final String today;
    private final boolean isMyShifts; // ✅ Determines if "My Shifts" is active

    public ShiftAdapter(List<Shift> shiftList, boolean isMyShifts) {
        this.shiftList = shiftList;
        this.isMyShifts = isMyShifts;
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

        if (position == 0 || !shift.getDay().equals(shiftList.get(position - 1).getDay())) {
            holder.dayTextView.setVisibility(View.VISIBLE);
            holder.dayTextView.setText(shift.getDay());

            if (shift.getDay().contains(getTodayName())) {
                holder.dayTextView.setBackgroundColor(Color.parseColor("#FFD700")); // Gold highlight
                holder.dayTextView.setTextColor(Color.BLACK);
            } else {
                holder.dayTextView.setBackgroundColor(Color.TRANSPARENT);
                holder.dayTextView.setTextColor(Color.DKGRAY);
            }
        } else {
            holder.dayTextView.setVisibility(View.GONE);
        }

        // Set shift details
        holder.timeTextView.setText(String.format("%s - %s", shift.getStartTime(), shift.getEndTime()));
        holder.workerTextView.setText(shift.getWorkerName());

        // Remove highlighting from worker name or "No Shifts Yet"
        holder.shiftContainer.setBackgroundColor(Color.WHITE);

        // Show "Add to Calendar" button only in "My Shifts" mode
        if (isMyShifts) {
            holder.addToCalendarBtn.setVisibility(View.VISIBLE);
            holder.addToCalendarBtn.setOnClickListener(v -> {
                Log.d("ShiftAdapter", "🗓️ Adding Shift to Calendar: " + shift.getStartTime() + " - " + shift.getEndTime());
                addShiftToCalendar(v.getContext(), shift);
            });
        } else {
            holder.addToCalendarBtn.setVisibility(View.GONE);
        }
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
        ImageButton addToCalendarBtn;
        LinearLayout shiftContainer;
        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);
            dayTextView = itemView.findViewById(R.id.shiftDay);
            timeTextView = itemView.findViewById(R.id.shiftTime);
            workerTextView = itemView.findViewById(R.id.shiftWorker);
            addToCalendarBtn = (ImageButton) itemView.findViewById(R.id.addToCalendarBtn);
            shiftContainer = itemView.findViewById(R.id.shiftContainer);
        }
    }

    private String getTodayName() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE (dd/MM/yyyy)", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime()).trim(); // Ensure formatting is identical
    }

    private void addShiftToCalendar(Context context, Shift shift) {
        try {
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(getDateForDay(shift.getDay())); // Convert day name to a real date
            startCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(shift.getStartTime().split(":")[0]));
            startCal.set(Calendar.MINUTE, 0);

            Calendar endCal = (Calendar) startCal.clone();
            endCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(shift.getEndTime().split(":")[0]));
            endCal.set(Calendar.MINUTE, 0);

            // Format timestamps for Google Calendar URL
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());
            String startTime = sdf.format(startCal.getTime());
            String endTime = sdf.format(endCal.getTime());

            // Construct Google Calendar event URL
            String calendarUrl = "https://www.google.com/calendar/render?action=TEMPLATE" +
                    "&text=Work%20Shift" +
                    "&details=Shift%3A%20" + shift.getStartTime() + "%20-%20" + shift.getEndTime() +
                    "&location=Workplace" +
                    "&dates=" + startTime + "/" + endTime;

            // Open the Google Calendar event creation page in a browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(calendarUrl));
            context.startActivity(intent);

            Log.d("ShiftAdapter", "✅ Opened Google Calendar in browser.");
        } catch (Exception e) {
            Log.e("ShiftAdapter", "❌ Error opening Google Calendar", e);
        }
    }

    private Date getDateForDay(String fullDayText) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE (dd/MM/yyyy)", Locale.getDefault());
            return sdf.parse(fullDayText); // Extract actual date from formatted string
        } catch (Exception e) {
            Log.e("ShiftAdapter", "❌ Failed to parse date from: " + fullDayText, e);
            return new Date();
        }
    }
}