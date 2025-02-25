package com.example.work_shifts.Fragments.Worker;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.R;
import com.example.work_shifts.Fragments.Worker.Shift;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Map;

public class removePendingShiftAdapter extends RecyclerView.Adapter<removePendingShiftAdapter.ViewHolder> {

    private List<Shift> shiftList;
    private String workId;
    private Map<Shift, String> shiftIdMap;
    private DatabaseReference databaseReference;
    private Context context;

    public removePendingShiftAdapter(Context context, List<Shift> shiftList, Map<Shift, String> shiftIdMap, String workId) {
        this.shiftList = shiftList;
        this.shiftIdMap = shiftIdMap;
        this.workId = workId;
        this.context = context;
        databaseReference = FirebaseDatabase.getInstance().getReference("workIDs");

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_shift, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Shift shift = shiftList.get(position);

        holder.workerName.setText(shift.getWorkerName());
        holder.shiftTime.setText(String.format("Time: %s - %s", shift.getsTime(), shift.getfTime()));
        holder.shiftDay.setText(String.format("Day: %s", shift.getDay()));

        holder.deleteShiftButton.setOnClickListener(v -> removeShift(position, shift));
    }

    @Override
    public int getItemCount() {
        return shiftList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView workerName, shiftTime, shiftDay;
        ImageButton deleteShiftButton;

        public ViewHolder(View itemView) {
            super(itemView);
            workerName = itemView.findViewById(R.id.workerName);
            shiftTime = itemView.findViewById(R.id.shiftTime);
            shiftDay = itemView.findViewById(R.id.shiftDay);
            deleteShiftButton = itemView.findViewById(R.id.deleteShiftButton);
        }
    }

    private void removeShift(int position, Shift shift) {
        String shiftId = shiftIdMap.get(shift);

        Log.e("removeShift", "workId: " + workId + ", shiftId: " + shiftId +
                ", weekType: " + shift.getWeekType() + ", day: " + shift.getDay());

        if (workId == null) {
            Toast.makeText(context, "Error: Work ID is missing. Cannot remove shift.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (shiftId == null || shift.getWeekType() == null || shift.getDay() == null) {
            Toast.makeText(context, "Error: Missing shift data. Cannot remove.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference shiftRef = databaseReference.child(workId)
                .child("waitingShifts").child("removals")
                .child(shift.getWeekType()).child(shift.getDay()).child(shiftId);


        shiftRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                shiftList.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(context, "✅ Shift removed successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to remove shift", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
