package com.example.work_shifts.Fragments.Worker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class HomePageFragment extends Fragment {

    private Button infoBtn, paySlipBtn, nextWeekBtn;
    private boolean showingNextWeek = false;
    private ImageButton addShiftBtn, removeShiftBtn;
    private MaterialButton myShiftBtn, scheduleBtn;
    private MaterialButtonToggleGroup toggleGroup;
    private RecyclerView shiftRecyclerView;
    private ShiftAdapter shiftAdapter;
    private List<Shift> allShifts = new ArrayList<>();
    private List<Shift> userShifts = new ArrayList<>();
    private FirebaseUser currentUser;


    private static final String[] WEEKDAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(view);

        // Initialize buttons
        infoBtn = view.findViewById(R.id.btnPersonalInfo);
        paySlipBtn = view.findViewById(R.id.btnPaySlip);
        myShiftBtn = view.findViewById(R.id.myShifts);
        scheduleBtn = view.findViewById(R.id.schedule);
        addShiftBtn = view.findViewById(R.id.addShift);
        removeShiftBtn = view.findViewById(R.id.removeShift);
        toggleGroup = view.findViewById(R.id.toggleGroup);
        nextWeekBtn = view.findViewById(R.id.nextWeekBtn);

        shiftRecyclerView = view.findViewById(R.id.shiftRecyclerView);
        shiftRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        shiftAdapter = new ShiftAdapter(new ArrayList<>(), false);
        shiftRecyclerView.setAdapter(shiftAdapter);
        scheduleShiftTransfer();

        showingNextWeek = false;
        nextWeekBtn.setText("Next Week");
        loadShifts("thisWeek");
        scheduleBtn.setChecked(true);

        infoBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_personalInfoFrag));
        paySlipBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_showFrag));
        addShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_addShiftFrag));
        removeShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_deleteShiftFrag));

        nextWeekBtn.setOnClickListener(v -> {
            showingNextWeek = !showingNextWeek;
            String week = showingNextWeek ? "nextWeek" : "thisWeek";

            nextWeekBtn.setText(showingNextWeek ? "Current Week" : "Next Week");

            loadShifts(week);
            shiftRecyclerView.post(() -> shiftAdapter.notifyDataSetChanged()); // ✅ Force full UI refresh
        });



        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                boolean isMyShifts = (checkedId == R.id.myShifts);

                if (isMyShifts) {
                    myShiftBtn.setBackgroundColor(getResources().getColor(R.color.blue));
                    scheduleBtn.setBackgroundColor(getResources().getColor(R.color.green));
                } else {
                    scheduleBtn.setBackgroundColor(getResources().getColor(R.color.blue));
                    myShiftBtn.setBackgroundColor(getResources().getColor(R.color.green));
                }

                shiftAdapter.updateShifts(isMyShifts ? userShifts : allShifts, isMyShifts);
                Log.d("ShiftDebug", "👤 Displaying " + (isMyShifts ? "My Shifts" : "Schedule"));
            }
        });

    }
    private void scheduleShiftTransfer() {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), ShiftTransferReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);

        // If Sunday 00:01 AM already passed today, schedule for next week
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }

        // Set an exact alarm
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
    private void loadShifts(String weekType) {
        DatabaseReference workIdsRef = FirebaseDatabase.getInstance().getReference("workIDs");

        if (currentUser == null) {
            Log.e("ShiftDebug", "❌ Current user is null!");
            return;
        }

        String currentUserId = currentUser.getUid();

        // Step 1: Find the user's workID
        workIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String userWorkId = null;
                for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                    if (workIdSnapshot.child("users").hasChild(currentUserId)) {
                        userWorkId = workIdSnapshot.getKey();
                        break;
                    }
                }

                if (userWorkId == null) {
                    Log.e("ShiftDebug", "❌ User has no assigned workID!");
                    return;
                }

                Log.d("ShiftDebug", "🏢 Found workID for user: " + userWorkId);

                // Step 2: Load shifts only from the user's workID
                DatabaseReference shiftsRef = workIdsRef.child(userWorkId).child("shifts").child(weekType);
                shiftsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot shiftSnapshot) {
                        allShifts.clear();
                        userShifts.clear();

                        LinkedHashMap<String, List<Shift>> allWorkerShiftsMap = new LinkedHashMap<>();
                        List<Shift> userShiftsList = new ArrayList<>();

                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                        if (weekType.equals("nextWeek")) {
                            calendar.add(Calendar.DAY_OF_MONTH, 7);
                        }

                        SimpleDateFormat sdf = new SimpleDateFormat("EEEE (dd/MM/yyyy)", Locale.getDefault());
                        LinkedHashMap<String, String> dateMap = new LinkedHashMap<>();
                        for (String day : WEEKDAYS) {
                            allWorkerShiftsMap.put(day, new ArrayList<>());
                            dateMap.put(day, sdf.format(calendar.getTime()));
                            calendar.add(Calendar.DAY_OF_MONTH, 1);
                        }

                        for (String day : WEEKDAYS) {
                            DataSnapshot daySnapshot = shiftSnapshot.child(day);
                            String dateWithDay = dateMap.get(day);

                            if (daySnapshot.exists()) {
                                for (DataSnapshot shiftData : daySnapshot.getChildren()) {
                                    String sTime = shiftData.child("sTime").getValue(String.class);
                                    String fTime = shiftData.child("fTime").getValue(String.class);
                                    String workerId = shiftData.child("workerId").getValue(String.class);
                                    String workerName = shiftData.child("workerName").getValue(String.class);

                                    if (sTime == null || fTime == null || workerId == null) continue;

                                    // ✅ Store the full date in Shift object
                                    Shift shift = new Shift(dateWithDay, sTime, fTime, workerName, workerId, weekType);
                                    allWorkerShiftsMap.get(day).add(shift);

                                    if (workerId.equals(currentUserId)) {
                                        userShiftsList.add(shift);
                                    }
                                }
                            }
                        }

                        for (String day : WEEKDAYS) {
                            String dateWithDay = dateMap.get(day);
                            if (allWorkerShiftsMap.get(day).isEmpty()) {
                                allShifts.add(new Shift(dateWithDay, "", "", "No Shifts Yet", "", weekType));
                            } else {
                                allShifts.addAll(allWorkerShiftsMap.get(day));
                            }
                        }


                        userShifts = new ArrayList<>(userShiftsList);

                        // ✅ Ensure toggleGroup is not null before accessing its methods
                        if (toggleGroup != null) {
                            boolean isMyShifts = toggleGroup.getCheckedButtonId() == R.id.myShifts;
                            shiftAdapter.updateShifts(isMyShifts ? userShifts : allShifts, isMyShifts);
                        }

                        shiftRecyclerView.post(() -> shiftAdapter.notifyDataSetChanged());
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

}
