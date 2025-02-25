package com.example.work_shifts.Fragments.Worker;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class PendingShiftAdapter extends RecyclerView.Adapter<PendingShiftAdapter.ViewHolder> {
    private List<addShiftFrag.PendingShift> shiftList;
    private String workId;
    private String selectedWeek;
    private WorkIdFetcher workIdFetcher;

    public PendingShiftAdapter(List<addShiftFrag.PendingShift> shiftList, String workId, String selectedWeek, WorkIdFetcher workIdFetcher) {
        this.shiftList = shiftList;
        this.workId = (workId != null && !workId.equals("UNKNOWN_WORK_ID")) ? workId : "INVALID_WORK_ID";
        this.selectedWeek = (selectedWeek != null) ? selectedWeek : "thisWeek";
        this.workIdFetcher = workIdFetcher;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pending_shift_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        addShiftFrag.PendingShift shift = shiftList.get(position);
        holder.workerName.setText(shift.workerName);
        holder.shiftTime.setText(shift.sTime + " - " + shift.fTime);
        holder.shiftDay.setText(shift.day);

        holder.deleteShiftButton.setOnClickListener(v -> {
            deleteShiftFromFirebase(shift, position);
        });
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

    private void deleteShiftFromFirebase(addShiftFrag.PendingShift shift, int position) {
        if (workId == null || shift.shiftId == null) {
            return;
        }

        String path = "workIDs/" + workId + "/waitingShifts/additions/" + selectedWeek + "/" + shift.day + "/" + shift.shiftId;

        DatabaseReference shiftRef = FirebaseDatabase.getInstance().getReference(path);

        shiftRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (position >= 0 && position < shiftList.size()) {
                    shiftList.remove(position);
                    notifyItemRemoved(position);
                } else {
                }

                if (shiftList.isEmpty()) {
                    notifyDataSetChanged();
                }
            } else {
            }
        });
    }


    public interface WorkIdFetcher {
        void fetchWorkId(Runnable onComplete);
    }
}
