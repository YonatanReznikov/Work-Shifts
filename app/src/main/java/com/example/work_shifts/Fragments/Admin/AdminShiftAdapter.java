package com.example.work_shifts.Fragments.Admin;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.Fragments.Worker.Shift;
import com.example.work_shifts.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class AdminShiftAdapter extends RecyclerView.Adapter<AdminShiftAdapter.ShiftViewHolder> {
    private List<Shift> shiftList;
    private boolean isMyShifts;
    private final String today;

    public AdminShiftAdapter(List<Shift> shiftList, boolean isMyShifts) {
        this.shiftList = shiftList;
        this.isMyShifts = isMyShifts;
        this.today = getTodayName();
    }

    @NonNull
    @Override
    public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.admin_shift_item, parent, false);
        return new ShiftViewHolder(view);
    }
    private String normalizeDate(String dateStr) {
        return dateStr.trim().replaceAll("\\s+", " ");
    }
    @Override
    public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {
        Shift shift = shiftList.get(position);

        // ✅ Ensure the first shift of the day has a visible day header
        if (position == 0 || !shift.getDay().equals(shiftList.get(position - 1).getDay())) {
            holder.dayTextView.setVisibility(View.VISIBLE);
            holder.dayTextView.setText(shift.getDay());

            if (normalizeDate(shift.getDay()).equalsIgnoreCase(normalizeDate(today))) {
                holder.dayTextView.setBackgroundColor(Color.parseColor("#FFD700")); // Gold highlight
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
            holder.addToCalendarBtn.setOnClickListener(v -> addShiftToCalendar(v.getContext(), shift));
        } else {
            holder.addToCalendarBtn.setVisibility(View.INVISIBLE);
        }

        holder.deleteShiftBtn.setOnClickListener(v -> deleteShift(holder.itemView.getContext(), shift));
    }


    private void addShiftToCalendar(Context context, Shift shift) {
        try {
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();

            String[] startTimeParts = shift.getsTime().split(":");
            String[] endTimeParts = shift.getfTime().split(":");

            int startHour = Integer.parseInt(startTimeParts[0]);
            int startMinute = (startTimeParts.length > 1) ? Integer.parseInt(startTimeParts[1]) : 0;

            int endHour = Integer.parseInt(endTimeParts[0]);
            int endMinute = (endTimeParts.length > 1) ? Integer.parseInt(endTimeParts[1]) : 0;

            startCal.set(Calendar.HOUR_OF_DAY, startHour);
            startCal.set(Calendar.MINUTE, startMinute);

            endCal.set(Calendar.HOUR_OF_DAY, endHour);
            endCal.set(Calendar.MINUTE, endMinute);

            // Format timestamps for Google Calendar URL
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());
            String sTime = sdf.format(startCal.getTime());
            String fTime = sdf.format(endCal.getTime());

            // Construct Google Calendar event URL
            String calendarUrl = "https://www.google.com/calendar/render?action=TEMPLATE" +
                    "&text=Work%20Shift" +
                    "&details=Shift%3A%20" + shift.getsTime() + "%20-%20" + shift.getfTime() +
                    "&location=Workplace" +
                    "&dates=" + sTime + "/" + fTime;

            // Open Google Calendar event creation in browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(calendarUrl));
            context.startActivity(intent);

            Log.d("ShiftAdapter", "✅ Opened Google Calendar in browser.");
        } catch (Exception e) {
            Log.e("ShiftAdapter", "❌ Error opening Google Calendar", e);
        }
    }

    @Override
    public int getItemCount() {
        return shiftList.size();
    }

    public void updateShifts(List<Shift> newShifts, boolean isMyShifts) {
        if (newShifts == null || newShifts.isEmpty()) {
            this.shiftList.clear();
        } else {
            this.shiftList = newShifts;
        }
        this.isMyShifts = isMyShifts;
        notifyDataSetChanged();
    }

    static class ShiftViewHolder extends RecyclerView.ViewHolder {
        TextView dayTextView, timeTextView, workerTextView;
        ImageButton addToCalendarBtn, deleteShiftBtn;
        LinearLayout shiftContainer;

        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);
            dayTextView = itemView.findViewById(R.id.shiftDay);
            timeTextView = itemView.findViewById(R.id.shiftTime);
            workerTextView = itemView.findViewById(R.id.shiftWorker);
            addToCalendarBtn = itemView.findViewById(R.id.addToCalendarBtn);
            deleteShiftBtn = itemView.findViewById(R.id.removeShift);
            shiftContainer = itemView.findViewById(R.id.shiftContainer);
        }
    }

    private String getTodayName() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE (dd/MM/yyyy)", Locale.getDefault());
        return normalizeDate(sdf.format(Calendar.getInstance().getTime()));
    }
    private void deleteShift(Context context, Shift shift) {
        DatabaseReference workIdsRef = FirebaseDatabase.getInstance().getReference("workIDs");

        workIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String workID = null;

                // 🔎 Find the correct workID for the worker
                for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                    if (workIdSnapshot.child("users").hasChild(shift.getWorkerId())) {
                        workID = workIdSnapshot.getKey();
                        break;
                    }
                }

                if (workID == null) {
                    Toast.makeText(context, "❌ Work ID not found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ✅ Normalize the day name (e.g., "Monday (26/02/2025)" → "Monday")
                String normalizedDay = shift.getDay().split(" ")[0];

                // ✅ Reference to shift location in Firebase
                DatabaseReference shiftsRef = FirebaseDatabase.getInstance()
                        .getReference("workIDs")
                        .child(workID)
                        .child("shifts")
                        .child(shift.getWeekType())
                        .child(normalizedDay);

                // 🔎 Find the correct shift ID before deleting
                shiftsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String shiftIdToDelete = null;

                        for (DataSnapshot shiftSnapshot : snapshot.getChildren()) {
                            String sTime = shiftSnapshot.child("sTime").getValue(String.class);
                            String fTime = shiftSnapshot.child("fTime").getValue(String.class);
                            String workerId = shiftSnapshot.child("workerId").getValue(String.class);

                            if (workerId != null && workerId.equals(shift.getWorkerId()) &&
                                    sTime != null && sTime.equals(shift.getsTime()) &&
                                    fTime != null && fTime.equals(shift.getfTime())) {
                                shiftIdToDelete = shiftSnapshot.getKey(); // ✅ Retrieve the correct shift ID
                                break;
                            }
                        }

                        if (shiftIdToDelete != null) {
                            // 🔥 Delete the shift using its unique ID
                            shiftsRef.child(shiftIdToDelete).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("ShiftDebug", "✅ Shift deleted successfully.");
                                        Toast.makeText(context, "✅ Shift Deleted", Toast.LENGTH_SHORT).show();

                                        // ✅ Find the index of the deleted shift
                                        int shiftIndex = shiftList.indexOf(shift);
                                        shiftList.remove(shift);

                                        // ✅ Check if there are no shifts left for that day **only in shiftList, not Firebase**
                                        boolean hasShiftsForDay = false;
                                        for (Shift s : shiftList) {
                                            if (s.getDay().equals(shift.getDay())) {
                                                hasShiftsForDay = true;
                                                break;
                                            }
                                        }

                                        if (!hasShiftsForDay) {
                                            // ✅ Show "No Shifts Yet" only in the UI, **not Firebase**
                                            shiftList.add(shiftIndex, new Shift(shift.getDay(), "", "", "No Shifts Yet", "", shift.getWeekType()));
                                        }

                                        // ✅ Refresh RecyclerView **without modifying Firebase**
                                        notifyDataSetChanged();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("ShiftDebug", "❌ Failed to delete shift", e);
                                        Toast.makeText(context, "❌ Failed to delete shift", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(context, "❌ Shift not found!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "❌ Failed to read shifts", error.toException());
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "❌ Failed to read workIDs", error.toException());
            }
        });
    }
    private void updateTotalHoursAfterDeletion(String workID, String workerId, int shiftHours) {
        DatabaseReference totalHoursRef = FirebaseDatabase.getInstance()
                .getReference("workIDs").child(workID)
                .child("users").child(workerId)
                .child("totalHours");

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

                int newTotalHours = Math.max(0, currentHours - shiftHours); // Prevent negative hours
                totalHoursRef.setValue(newTotalHours);
                Log.d("Firebase", "✅ Deducted " + shiftHours + " hours. New total: " + newTotalHours);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ShiftDebug", "❌ Error updating total hours after shift deletion", error.toException());
            }
        });
    }

    private void checkAndReplaceEmptyDay(DatabaseReference dayRef, String dayName) {
        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.hasChildren()) {
                    // Instead of removing the day, add a placeholder
                    dayRef.child("noShifts").setValue("No shifts yet");
                    Log.d("ShiftAdapter", "✅ No shifts left for " + dayName + ". Added 'No shifts yet' placeholder.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "❌ Failed to check empty day", error.toException());
            }
        });
    }
}
