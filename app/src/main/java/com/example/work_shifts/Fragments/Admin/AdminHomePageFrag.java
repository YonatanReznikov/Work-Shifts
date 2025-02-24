package com.example.work_shifts.Fragments.Admin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import com.example.work_shifts.Fragments.Worker.Shift;
import com.example.work_shifts.Fragments.Worker.ShiftTransferReceiver;
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

public class AdminHomePageFrag extends Fragment {

    private Button infoBtn, paySlipBtn, nextWeekBtn, requestBtn;
    private boolean showingNextWeek = false;
    private ImageButton addShiftBtn;
    private MaterialButton myShiftBtn, scheduleBtn;
    private MaterialButtonToggleGroup toggleGroup;
    private RecyclerView shiftRecyclerView;
    private AdminShiftAdapter shiftAdapter;
    private List<Shift> allShifts = new ArrayList<>();
    private List<Shift> userShifts = new ArrayList<>();
    private FirebaseUser currentUser;

    private static final String[] WEEKDAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_home_page, container, false);
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
        toggleGroup = view.findViewById(R.id.toggleGroup);
        nextWeekBtn = view.findViewById(R.id.nextWeekBtn);
        requestBtn = view.findViewById(R.id.btnRequests);

        shiftRecyclerView = view.findViewById(R.id.shiftRecyclerView);
        shiftRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        shiftAdapter = new AdminShiftAdapter(new ArrayList<>(), false);
        shiftRecyclerView.setAdapter(shiftAdapter);
        scheduleShiftTransfer();

        showingNextWeek = false;
        nextWeekBtn.setText("Next Week");
        loadShifts("thisWeek");
        scheduleBtn.setChecked(true);

        if (infoBtn != null) {
            infoBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_personalInfoFrag));
        } else {
            Log.e("AdminHomePageFrag", "infoBtn is null");
        }

        if (paySlipBtn != null) {
            paySlipBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_showFrag));
        } else {
            Log.e("AdminHomePageFrag", "paySlipBtn is null");
        }

        if (addShiftBtn != null) {
            addShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_adminHomePageFragment_to_adminAddShift));
        } else {
            Log.e("AdminHomePageFrag", "addShiftBtn is null");
        }

        if (requestBtn != null) {
            requestBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_requestsFrag));
        } else {
            Log.e("AdminHomePageFrag", "requestBtn is null");
        }

        nextWeekBtn.setOnClickListener(v -> {
            showingNextWeek = !showingNextWeek;
            String week = showingNextWeek ? "nextWeek" : "thisWeek";

            nextWeekBtn.setText(showingNextWeek ? "Current Week" : "Next Week");

            loadShifts(week);
            shiftRecyclerView.post(() -> shiftAdapter.notifyDataSetChanged());
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

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    private void loadShifts(String weekType) {
        DatabaseReference workIdsRef = FirebaseDatabase.getInstance().getReference("workIDs");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e("FirebaseDebug", "❌ Current user is null!");
            return;
        }

        String currentUserId = currentUser.getUid();

        workIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String userWorkId = null;

                // ✅ Find the user's workID dynamically
                for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                    if (workIdSnapshot.child("users").hasChild(currentUserId)) {
                        userWorkId = workIdSnapshot.getKey();
                        break;
                    }
                }

                if (userWorkId == null) {
                    Log.e("FirebaseDebug", "❌ No workID found for user: " + currentUserId);
                    return;
                }

                Log.d("FirebaseDebug", "✅ Found workID: " + userWorkId);

                fetchShiftsForWorkId(userWorkId, weekType);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "❌ Failed to read workIDs", error.toException());
            }
        });
    }
    private void fetchShiftsForWorkId(String workId, String weekType) {
        DatabaseReference shiftsRef = FirebaseDatabase.getInstance()
                .getReference("workIDs").child(workId).child("shifts").child(weekType);

        allShifts.clear();
        userShifts.clear();

        LinkedHashMap<String, List<Shift>> allWorkerShiftsMap = new LinkedHashMap<>();
        List<Shift> userShiftsList = new ArrayList<>();

        // 📅 Get today’s date and initialize week mapping
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        if (weekType.equals("nextWeek")) {
            calendar.add(Calendar.DAY_OF_MONTH, 7);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE (dd/MM/yyyy)", Locale.getDefault());
        LinkedHashMap<String, String> dateMap = new LinkedHashMap<>();

        for (String day : WEEKDAYS) {
            allWorkerShiftsMap.put(day, new ArrayList<>());
            dateMap.put(day, sdf.format(calendar.getTime())); // Map each day to a formatted date
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        shiftsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("FirebaseDebug", "🔥 Data snapshot: " + snapshot.getValue());

                for (String day : WEEKDAYS) {
                    DataSnapshot daySnapshot = snapshot.child(day);
                    String dateWithDay = dateMap.get(day);

                    if (daySnapshot.exists()) {
                        for (DataSnapshot shiftData : daySnapshot.getChildren()) {
                            String sTime = shiftData.child("sTime").getValue(String.class);
                            String fTime = shiftData.child("fTime").getValue(String.class);
                            String workerId = shiftData.child("workerId").getValue(String.class);
                            String workerName = shiftData.child("workerName").getValue(String.class);

                            if (sTime == null || fTime == null || workerId == null) continue;

                            Shift shift = new Shift(day, sTime, fTime, workerName, workerId, weekType);
                            allWorkerShiftsMap.get(day).add(shift);

                            if (workerId.equals(currentUser.getUid())) {
                                userShiftsList.add(shift);
                            }
                        }
                    }

                    // ✅ If no shifts exist for the day, add a placeholder to keep the date
                    if (allWorkerShiftsMap.get(day).isEmpty()) {
                        allWorkerShiftsMap.get(day).add(new Shift(dateWithDay, "", "", "No Shifts Yet", "", weekType));
                    }
                }

                // ✅ Add all shifts to the list
                for (String day : WEEKDAYS) {
                    allShifts.addAll(allWorkerShiftsMap.get(day));
                }

                userShifts = new ArrayList<>(userShiftsList);

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
}