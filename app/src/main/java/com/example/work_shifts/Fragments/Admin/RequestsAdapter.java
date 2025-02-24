package com.example.work_shifts.Fragments.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.work_shifts.R;
import com.example.work_shifts.Fragments.Worker.Shift;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {

    private List<Shift> waitingShifts;
    private DatabaseReference databaseReference;

    public RequestsAdapter(List<Shift> waitingShifts) {
        this.waitingShifts = waitingShifts;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("workIDs");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Shift shift = waitingShifts.get(position);
        holder.workerName.setText(shift.getWorkerName());
        holder.shiftTime.setText(shift.getsTime() + " - " + shift.getfTime());
        String formattedDate = getFormattedDate(shift.getDay());
        holder.shiftDate.setText(formattedDate);
        holder.approveButton.setOnClickListener(v -> approveShift(shift, position));
        holder.rejectButton.setOnClickListener(v -> rejectShift(shift, position));
    }
    private String getFormattedDate(String day) {
        // Define the mapping of days to dates
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault());

        Calendar calendar = Calendar.getInstance();
        Map<String, Integer> dayMapping = new HashMap<>();
        dayMapping.put("Sunday", Calendar.SUNDAY);
        dayMapping.put("Monday", Calendar.MONDAY);
        dayMapping.put("Tuesday", Calendar.TUESDAY);
        dayMapping.put("Wednesday", Calendar.WEDNESDAY);
        dayMapping.put("Thursday", Calendar.THURSDAY);
        dayMapping.put("Friday", Calendar.FRIDAY);
        dayMapping.put("Saturday", Calendar.SATURDAY);

        if (dayMapping.containsKey(day)) {
            int targetDay = dayMapping.get(day);
            while (calendar.get(Calendar.DAY_OF_WEEK) != targetDay) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        return sdf.format(calendar.getTime());
    }

    @Override
    public int getItemCount() {
        return waitingShifts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView workerName, shiftDate, shiftTime;
        Button approveButton, rejectButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            shiftDate = itemView.findViewById(R.id.shiftDate);
            workerName = itemView.findViewById(R.id.workerName);
            shiftTime = itemView.findViewById(R.id.shiftTime);
            approveButton = itemView.findViewById(R.id.approveButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }

    private void approveShift(Shift shift, int position) {
        getWorkID(shift, workID -> {
            if (workID == null) {
                Log.e("Firebase", "❌ Work ID not found for worker: " + shift.getWorkerId());
                return;
            }

            DatabaseReference approvedShiftsRef = databaseReference.child(workID)
                    .child("shifts").child(shift.getWeekType()).child(shift.getDay());

            String shiftKey = approvedShiftsRef.push().getKey();

            if (shiftKey != null) {
                approvedShiftsRef.child(shiftKey).setValue(shift)
                        .addOnSuccessListener(aVoid -> {
                            removeShiftFromWaitingList(shift, position, workID);

                            updateTotalHoursInFirebase(workID, shift.getWorkerId(), shift.getsTime(), shift.getfTime());

                            Log.d("Firebase", "✅ Shift approved and moved to 'shifts/" + shift.getWeekType() + "'");
                        })
                        .addOnFailureListener(e -> Log.e("Firebase", "❌ Failed to approve shift", e));
            }
        });
    }
    private void updateTotalHoursInFirebase(String workID, String workerId, String sTime, String fTime) {
        DatabaseReference totalHoursRef = databaseReference
                .child(workID)
                .child("users")
                .child(workerId)
                .child("totalHours");

        int startHour = Integer.parseInt(sTime.split(":")[0]);
        int endHour = Integer.parseInt(fTime.split(":")[0]);
        int shiftHours = endHour - startHour;

        totalHoursRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int currentHours = 0;
                try {
                    currentHours = snapshot.exists() ? snapshot.getValue(Integer.class) : 0;
                } catch (DatabaseException e) {
                    try {
                        currentHours = Integer.parseInt(snapshot.getValue(String.class));
                    } catch (NumberFormatException ex) {
                        Log.e("ShiftDebug", "❌ Invalid totalHours format in Firebase", ex);
                    }
                }

                int newTotalHours = currentHours + shiftHours;
                totalHoursRef.setValue(newTotalHours);
                Log.d("Firebase", "✅ Updated total hours for worker " + workerId + ": " + newTotalHours + " hours");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ShiftDebug", "❌ Error updating total hours", error.toException());
            }
        });
    }

    private void rejectShift(Shift shift, int position) {
        getWorkID(shift, workID -> {
            if (workID != null) {
                removeShiftFromWaitingList(shift, position, workID);
            }
            Log.d("Firebase", "❌ Shift rejected and deleted");
        });
    }

    private void removeShiftFromWaitingList(Shift shift, int position, String workID) {
        DatabaseReference waitingShiftsRef = databaseReference.child(workID)
                .child("waitingShifts").child(shift.getWeekType()).child(shift.getDay());

        waitingShiftsRef.orderByChild("workerId").equalTo(shift.getWorkerId()).get()
                .addOnSuccessListener(dataSnapshot -> {
                    for (DataSnapshot shiftSnapshot : dataSnapshot.getChildren()) {
                        shiftSnapshot.getRef().removeValue();
                    }

                    if (position >= 0 && position < waitingShifts.size()) {
                        waitingShifts.remove(position);
                        notifyItemRemoved(position);
                    } else {
                        Log.e("RequestsAdapter", "❌ Invalid index: " + position + ", list size: " + waitingShifts.size());
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "❌ Failed to remove shift from waiting list", e));
    }

    private void getWorkID(Shift shift, WorkIDCallback callback) {
        DatabaseReference workIdsRef = FirebaseDatabase.getInstance().getReference("workIDs");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e("Firebase", "❌ Current user is null!");
            callback.onCallback(null);
            return;
        }

        workIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String userWorkId = null;

                for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                    if (workIdSnapshot.child("users").hasChild(shift.getWorkerId())) {
                        userWorkId = workIdSnapshot.getKey();
                        break;
                    }
                }

                callback.onCallback(userWorkId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "❌ Failed to read workIDs", error.toException());
                callback.onCallback(null);
            }
        });
    }

    private interface WorkIDCallback {
        void onCallback(String workID);
    }
}
