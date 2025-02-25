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

        if (position == 0 || !shift.getDay().equals(shiftList.get(position - 1).getDay())) {
            holder.dayTextView.setVisibility(View.VISIBLE);
            holder.dayTextView.setText(shift.getDay());

            if (normalizeDate(shift.getDay()).equalsIgnoreCase(normalizeDate(today))) {
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

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());
            String sTime = sdf.format(startCal.getTime());
            String fTime = sdf.format(endCal.getTime());

            String calendarUrl = "https://www.google.com/calendar/render?action=TEMPLATE" +
                    "&text=Work%20Shift" +
                    "&details=Shift%3A%20" + shift.getsTime() + "%20-%20" + shift.getfTime() +
                    "&location=Workplace" +
                    "&dates=" + sTime + "/" + fTime;

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(calendarUrl));
            context.startActivity(intent);

        } catch (Exception e) {
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

        AtomicReference<String> workID = new AtomicReference<>(null);

        workIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                    if (workIdSnapshot.child("users").hasChild(shift.getWorkerId())) {
                        workID.set(workIdSnapshot.getKey());
                        break;
                    }
                }

                if (workID.get() == null) {
                    Toast.makeText(context, "❌ Work ID not found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String normalizedDay = shift.getDay().split(" ")[0];

                DatabaseReference shiftsRef = FirebaseDatabase.getInstance()
                        .getReference("workIDs")
                        .child(workID.get())
                        .child("shifts")
                        .child(shift.getWeekType())
                        .child(normalizedDay);

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
                                shiftIdToDelete = shiftSnapshot.getKey();
                                break;
                            }
                        }

                        if (shiftIdToDelete != null) {
                            shiftsRef.child(shiftIdToDelete).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(context, "✅ Shift Deleted", Toast.LENGTH_SHORT).show();

                                        int shiftDuration = calculateShiftDuration(shift.getsTime(), shift.getfTime());

                                        updateTotalHoursAfterDeletion(workID.get(), shift.getWorkerId(), shiftDuration);

                                        int shiftIndex = shiftList.indexOf(shift);
                                        shiftList.remove(shift);

                                        boolean hasShiftsForDay = false;
                                        for (Shift s : shiftList) {
                                            if (s.getDay().equals(shift.getDay())) {
                                                hasShiftsForDay = true;
                                                break;
                                            }
                                        }

                                        if (!hasShiftsForDay) {
                                            shiftList.add(shiftIndex, new Shift(shift.getDay(), "", "", "No Shifts Yet", "", shift.getWeekType()));
                                        }

                                        notifyDataSetChanged();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(context, "❌ Failed to delete shift", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(context, "❌ Shift not found!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private int calculateShiftDuration(String startTime, String finishTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        try {
            long diffMillis = sdf.parse(finishTime).getTime() - sdf.parse(startTime).getTime();
            return (int) (diffMillis / (1000 * 60 * 60));
        } catch (Exception e) {
        }

        return 0;
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
                    }
                }

                int newTotalHours = Math.max(0, currentHours - shiftHours);
                totalHoursRef.setValue(newTotalHours);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    private void checkAndReplaceEmptyDay(DatabaseReference dayRef, String dayName) {
        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.hasChildren()) {
                    dayRef.child("noShifts").setValue("No shifts yet");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
