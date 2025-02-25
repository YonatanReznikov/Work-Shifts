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
    private boolean isMyShifts;
    private final String today;

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
        String normalizedShiftDay = shift.getDay().split(" ")[0].trim();

        if (position == 0 || !shift.getDay().equals(shiftList.get(position - 1).getDay())) {
            holder.dayTextView.setVisibility(View.VISIBLE);
            holder.dayTextView.setText(shift.getDay());

            if (normalizedShiftDay.equalsIgnoreCase(today)) {
                holder.dayTextView.setBackgroundColor(Color.parseColor("#FFD700"));
                holder.dayTextView.setTextColor(Color.BLACK);
            } else {
                holder.dayTextView.setBackgroundColor(Color.TRANSPARENT);
                holder.dayTextView.setTextColor(Color.DKGRAY);
            }
        } else {
            holder.dayTextView.setVisibility(View.GONE);
        }

        holder.timeTextView.setText(String.format("%s - %s", shift.getsTime(), shift.getfTime()));
        holder.workerTextView.setText(shift.getWorkerName());

        if (isMyShifts) {
            holder.addToCalendarBtn.setVisibility(View.VISIBLE);
            holder.addToCalendarBtn.setOnClickListener(v -> {
                addShiftToCalendar(v.getContext(), shift);
            });
        } else {
            holder.addToCalendarBtn.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return shiftList.size();
    }

    public void updateShifts(List<Shift> newShifts, boolean isMyShifts) {
        this.shiftList = newShifts;
        this.isMyShifts = isMyShifts;
        notifyDataSetChanged();
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
            addToCalendarBtn = itemView.findViewById(R.id.addToCalendarBtn);
            shiftContainer = itemView.findViewById(R.id.shiftContainer);
        }
    }

    private String getTodayName() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime()).trim();
    }



    private void addShiftToCalendar(Context context, Shift shift) {
        try {
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(getDateForDay(shift.getDay()));
            startCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(shift.getsTime().split(":")[0]));
            startCal.set(Calendar.MINUTE, 0);

            Calendar endCal = (Calendar) startCal.clone();
            endCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(shift.getfTime().split(":")[0]));
            endCal.set(Calendar.MINUTE, 0);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());
            String startTime = sdf.format(startCal.getTime());
            String endTime = sdf.format(endCal.getTime());

            String calendarUrl = "https://www.google.com/calendar/render?action=TEMPLATE" +
                    "&text=Work%20Shift" +
                    "&details=Shift%3A%20" + shift.getsTime() + "%20-%20" + shift.getfTime() +
                    "&location=Workplace" +
                    "&dates=" + startTime + "/" + endTime;

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(calendarUrl));
            context.startActivity(intent);

        } catch (Exception e) {
        }
    }

    private Date getDateForDay(String fullDayText) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE (dd/MM/yyyy)", Locale.getDefault());
            return sdf.parse(fullDayText);
        } catch (Exception e) {
            return new Date();
        }
    }
}
