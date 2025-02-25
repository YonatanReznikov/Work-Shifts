package com.example.work_shifts.Fragments.Admin;

import android.os.Handler;
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
import java.util.concurrent.atomic.AtomicInteger;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {

    private List<Shift> waitingShifts;
    private String requestType;
    private DatabaseReference databaseReference;

    public RequestsAdapter(List<Shift> waitingShifts, String requestType) {
        this.waitingShifts = waitingShifts;
        this.requestType = requestType;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("workIDs");
        setHasStableIds(true);
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
        holder.shiftDate.setText(getFormattedDate(shift.getDay()));

        if (requestType.equals("additions")) {
            holder.approveButton.setText("Approve Shift");
        } else {
            holder.approveButton.setText("Approve Removal");
        }

        // ✅ Use getAdapterPosition() to prevent RecyclerView inconsistencies
        holder.approveButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                approveShift(shift, adapterPosition);
            }
        });

        holder.rejectButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                rejectShift(shift, adapterPosition);
            }
        });
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
    public long getItemId(int position) {
        Shift shift = waitingShifts.get(position);
        return (shift.getWorkerId() + shift.getsTime() + shift.getfTime() + shift.getDay() + shift.getWeekType() + System.currentTimeMillis()).hashCode();
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
            if (workID == null) return;

            if (requestType.equals("additions")) {
                DatabaseReference approvedShiftsRef = databaseReference.child(workID)
                        .child("shifts").child(shift.getWeekType()).child(shift.getDay());

                String shiftKey = approvedShiftsRef.push().getKey();
                if (shiftKey != null) {
                    approvedShiftsRef.child(shiftKey).setValue(shift)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firebase", "✅ Shift approved and moved to 'shifts/" + shift.getWeekType() + "'");
                                updateTotalHoursInFirebase(workID, shift.getWorkerId(), shift.getsTime(), shift.getfTime(), true); // ✅ Add Hours
                                removeShiftFromWaitingList(shift, position, workID);
                            })
                            .addOnFailureListener(e -> Log.e("Firebase", "❌ Failed to approve shift", e));
                }
            } else if (requestType.equals("removals")) {  // ✅ Ensure correct handling for removals
                DatabaseReference shiftsRef = databaseReference.child(workID)
                        .child("shifts").child(shift.getWeekType()).child(shift.getDay());

                shiftsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String shiftKeyToRemove = null;

                        for (DataSnapshot shiftSnapshot : snapshot.getChildren()) {
                            Shift existingShift = shiftSnapshot.getValue(Shift.class);
                            if (existingShift != null &&
                                    existingShift.getWorkerId().equals(shift.getWorkerId()) &&
                                    existingShift.getsTime().equals(shift.getsTime()) &&
                                    existingShift.getfTime().equals(shift.getfTime())) {

                                shiftKeyToRemove = shiftSnapshot.getKey();
                                break;
                            }
                        }

                        if (shiftKeyToRemove != null) {
                            shiftsRef.child(shiftKeyToRemove).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        removeShiftFromWaitingList(shift, position, workID);
                                        updateTotalHoursInFirebase(workID, shift.getWorkerId(), shift.getsTime(), shift.getfTime(), false); // ✅ Deduct Hours
                                        Log.d("Firebase", "✅ Shift successfully removed from 'shifts'");
                                    })
                                    .addOnFailureListener(e -> Log.e("Firebase", "❌ Failed to remove shift", e));
                        } else {
                            Log.e("Firebase", "❌ Shift not found in 'shifts' for removal");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "❌ Failed to remove shift", error.toException());
                    }
                });
            }
        });
    }
    private void updateTotalHoursInFirebase(String workID, String workerId, String sTime, String fTime, boolean isAddition) {
        DatabaseReference totalHoursRef = databaseReference
                .child(workID)
                .child("users")
                .child(workerId)
                .child("totalHours");

        int shiftHours = calculateShiftDuration(sTime, fTime); // ✅ Calculate duration properly

        AtomicInteger currentHours = new AtomicInteger(0); // ✅ Use AtomicInteger

        totalHoursRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.exists() && snapshot.getValue() != null) {
                        currentHours.set(snapshot.getValue(Integer.class)); // ✅ Set value safely
                    }
                } catch (DatabaseException e) {
                    try {
                        currentHours.set(Integer.parseInt(snapshot.getValue(String.class)));
                    } catch (NumberFormatException ex) {
                        Log.e("ShiftDebug", "❌ Invalid totalHours format in Firebase", ex);
                    }
                }

                int newTotalHours = isAddition ? currentHours.get() + shiftHours : Math.max(0, currentHours.get() - shiftHours);

                totalHoursRef.setValue(newTotalHours)
                        .addOnSuccessListener(aVoid -> Log.d("Firebase", "✅ Updated total hours for " + workerId +
                                ": " + currentHours.get() + " → " + newTotalHours)) // ✅ Use AtomicInteger
                        .addOnFailureListener(e -> Log.e("Firebase", "❌ Failed to update total hours", e));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ShiftDebug", "❌ Error updating total hours", error.toException());
            }
        });
    }

    private int calculateShiftDuration(String startTime, String finishTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        try {
            long diffMillis = sdf.parse(finishTime).getTime() - sdf.parse(startTime).getTime();
            return (int) (diffMillis / (1000 * 60 * 60)); // Convert milliseconds to hours
        } catch (Exception e) {
            Log.e("TimeParse", "❌ Failed to parse shift duration", e);
        }

        return 0;
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
        String category = requestType.equals("additions") ? "additions" : "removals";
        DatabaseReference waitingShiftsRef = databaseReference.child(workID)
                .child("waitingShifts").child(category).child(shift.getWeekType()).child(shift.getDay());

        waitingShiftsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String shiftKeyToRemove = null;

                for (DataSnapshot shiftSnapshot : snapshot.getChildren()) {
                    Shift existingShift = shiftSnapshot.getValue(Shift.class);
                    if (existingShift != null &&
                            existingShift.getWorkerId().equals(shift.getWorkerId()) &&
                            existingShift.getsTime().equals(shift.getsTime()) &&
                            existingShift.getfTime().equals(shift.getfTime())) {
                        shiftKeyToRemove = shiftSnapshot.getKey();
                        break;
                    }
                }

                if (shiftKeyToRemove != null) {
                    waitingShiftsRef.child(shiftKeyToRemove).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firebase", "✅ Shift successfully removed from Firebase");

                                refreshRecyclerView();
                            })
                            .addOnFailureListener(e -> Log.e("Firebase", "❌ Failed to remove shift from waiting list", e));
                } else {
                    Log.e("Firebase", "❌ Shift not found in 'waitingShifts/" + category + "'");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "❌ Failed to remove shift", error.toException());
            }
        });
    }
    public void refreshRecyclerView() {
        new Handler().postDelayed(() -> {
            notifyDataSetChanged();
            Log.d("RecyclerView", "🔄 RecyclerView fully refreshed.");
        }, 100);
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
