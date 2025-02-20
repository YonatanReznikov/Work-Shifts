package com.example.work_shifts.Fragments.Worker;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ShiftAdapter extends RecyclerView.Adapter<ShiftAdapter.ShiftViewHolder> {
    private List<Shift> shiftList;
    private String today;
    private Set<String> displayedDays = new HashSet<>();

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

        // **Show the day header only once**
        if (!displayedDays.contains(shift.getDay())) {
            holder.dayTextView.setVisibility(View.VISIBLE);
            holder.dayTextView.setText(shift.getDay());
            displayedDays.add(shift.getDay());
        } else {
            holder.dayTextView.setVisibility(View.GONE);
        }

        // **Show shift details**
        if (!shift.getTime().equals("No Shift")) {
            holder.timeTextView.setText(shift.getTime());
            holder.workerTextView.setText(shift.getWorkerName());
        } else {
            holder.timeTextView.setText("No Shift");
            holder.workerTextView.setText("");
        }

        // **Highlight today's shifts**
        if (shift.getDay().equals(today)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFD700"));
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
        displayedDays.clear(); // Reset displayed days to avoid duplicates
        notifyDataSetChanged();
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
