package com.example.work_shifts.Fragments.Worker;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private String today;

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

        // Show the day header only if it's the first occurrence of that day
        if (position == 0 || !shift.getDay().equals(shiftList.get(position - 1).getDay())) {
            holder.dayTextView.setVisibility(View.VISIBLE);
            holder.dayTextView.setText(shift.getDay());
        } else {
            holder.dayTextView.setVisibility(View.GONE);
        }

        // Display shift details
        holder.timeTextView.setText(shift.getTime());
        holder.workerTextView.setText(shift.getWorkerName());

        // Highlight today's shifts
        if (shift.getDay().equals(today)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFD700")); // Gold for highlighting
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
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

        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);
            dayTextView = itemView.findViewById(R.id.shiftDay);
            timeTextView = itemView.findViewById(R.id.shiftTime);
            workerTextView = itemView.findViewById(R.id.shiftWorker);
        }
    }

    private String getTodayName() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }
}
