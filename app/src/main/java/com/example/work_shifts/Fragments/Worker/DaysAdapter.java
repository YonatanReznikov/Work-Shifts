package com.example.work_shifts.Fragments.Worker;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.work_shifts.R;
import java.util.List;

public class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.DayViewHolder> {

    private List<String> days;
    private int selectedPosition = -1; // No selection by default

    public DaysAdapter(List<String> days) {
        this.days = days;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        String day = days.get(position);
        holder.dayTextView.setText(day);

        // Highlight the selected day
        if (position == selectedPosition) {
            holder.dayTextView.setBackgroundResource(R.drawable.selected_day_background);
            holder.dayTextView.setTextColor(Color.WHITE);
        } else {
            holder.dayTextView.setBackgroundResource(R.drawable.unselected_day_background);
            holder.dayTextView.setTextColor(Color.BLACK);
        }

        // Click listener for selecting a day
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = position;

            // Refresh UI for both previously selected and new selected items
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public void updateDays(List<String> newDays) {
        this.days = newDays;
        selectedPosition = -1; // Reset selection when month changes
        notifyDataSetChanged();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayTextView;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayTextView = itemView.findViewById(R.id.dayTextView);
        }
    }
}
