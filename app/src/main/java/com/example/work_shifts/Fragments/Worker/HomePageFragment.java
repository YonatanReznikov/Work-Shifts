package com.example.work_shifts.Fragments.Worker;

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

    private Button infoBtn, paySlipBtn;
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

        shiftRecyclerView = view.findViewById(R.id.shiftRecyclerView);
        shiftRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        shiftAdapter = new ShiftAdapter(new ArrayList<>());
        shiftRecyclerView.setAdapter(shiftAdapter);

        checkAndMoveNextWeekToThisWeek(); // Move shifts if needed
        loadShifts(); // Load shifts for this week

        scheduleBtn.setChecked(true);

        infoBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_personalInfoFrag));
        paySlipBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_showFrag));
        addShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_addShiftFrag));
        removeShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_deleteShiftFrag));

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.schedule) {
                    shiftAdapter.updateShifts(allShifts);
                    Log.d("ShiftDebug", "📅 Displaying Schedule (All Workers)");
                } else if (checkedId == R.id.myShifts) {
                    shiftAdapter.updateShifts(userShifts);
                    Log.d("ShiftDebug", "👤 Displaying My Shifts: " + userShifts.size());
                }
            }
        });
    }

    private void loadShifts() {
        DatabaseReference workIdsRef = FirebaseDatabase.getInstance().getReference("workIDs");

        workIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allShifts.clear();
                userShifts.clear();

                if (currentUser == null) {
                    Log.e("ShiftDebug", "❌ Current user is null!");
                    return;
                }

                String currentUserId = currentUser.getUid(); // Firebase Auth UID
                Log.d("ShiftDebug", "🔍 Authenticated User ID: " + currentUserId);

                LinkedHashMap<String, List<Shift>> allWorkerShiftsMap = new LinkedHashMap<>();
                List<Shift> userShiftsList = new ArrayList<>();

                for (String day : WEEKDAYS) {
                    allWorkerShiftsMap.put(day, new ArrayList<>());
                }

                for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                    DataSnapshot shiftsSnapshot = workIdSnapshot.child("shifts").child("thisWeek");

                    if (shiftsSnapshot.exists()) {
                        for (String day : WEEKDAYS) {
                            DataSnapshot daySnapshot = shiftsSnapshot.child(day);

                            if (daySnapshot.exists()) {
                                for (DataSnapshot shiftSnapshot : daySnapshot.getChildren()) {
                                    String sTime = shiftSnapshot.child("sTime").getValue(String.class);
                                    String fTime = shiftSnapshot.child("fTime").getValue(String.class);
                                    String workerId = shiftSnapshot.child("workerId").getValue(String.class);
                                    String workerName = shiftSnapshot.child("workerName").getValue(String.class);

                                    if (sTime == null || fTime == null || workerId == null) continue;

                                    Shift shift = new Shift(day, sTime, fTime, workerName, workerId);
                                    allWorkerShiftsMap.get(day).add(shift);

                                    // **Log each shift to check workerId**
                                    Log.d("ShiftDebug", "📌 Found shift: " + day + " " + sTime + " - " + fTime + " | workerId: " + workerId);

                                    // **Fix: Ensure `workerId` matches logged-in user ID**
                                    if (workerId.equals(currentUserId)) {
                                        userShiftsList.add(shift);
                                        Log.d("ShiftDebug", "✅ Shift added to My Shifts: " + day + " " + sTime + " - " + fTime);
                                    } else {
                                        Log.w("ShiftDebug", "⚠️ workerId (" + workerId + ") does not match currentUserId (" + currentUserId + ")");
                                    }
                                }
                            }
                        }
                    }
                }

                for (String day : WEEKDAYS) {
                    if (allWorkerShiftsMap.get(day).isEmpty()) {
                        allShifts.add(new Shift(day, "", "", "No Shifts Yet", ""));
                    } else {
                        allShifts.addAll(allWorkerShiftsMap.get(day));
                    }
                }

                userShifts.addAll(userShiftsList);

                shiftAdapter.updateShifts(allShifts);
                Log.d("ShiftDebug", "✅ Total Schedule Shifts: " + allShifts.size());
                Log.d("ShiftDebug", "✅ Total User Shifts: " + userShifts.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "❌ Failed to read shifts", error.toException());
            }
        });
    }
    private void checkAndMoveNextWeekToThisWeek() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());

        if (sdf.format(calendar.getTime()).equals("Sunday") && calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) == 1) {
            DatabaseReference shiftsRef = FirebaseDatabase.getInstance().getReference("workIDs");

            shiftsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                        DatabaseReference workRef = shiftsRef.child(workIdSnapshot.getKey()).child("shifts");

                        workRef.child("thisWeek").setValue(workIdSnapshot.child("shifts").child("nextWeek").getValue())
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        workRef.child("nextWeek").setValue(null);
                                        Log.d("ShiftDebug", "Moved nextWeek shifts to thisWeek and cleared nextWeek");
                                    }
                                });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Firebase", "Failed to move shifts", error.toException());
                }
            });
        }
    }
}
