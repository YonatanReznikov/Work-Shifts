package com.example.work_shifts.Fragments.Admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.Fragments.Worker.Shift;
import com.example.work_shifts.Fragments.Worker.ShiftAdapter;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class AdminHomePageFrag extends Fragment {

    private Button infoBtn, paySlipBtn, requestBtn;
    private MaterialButton scheduleBtn, myShiftBtn;
    private MaterialButtonToggleGroup toggleGroup;
    private RecyclerView shiftRecyclerView;
    private ShiftAdapter shiftAdapter;
    private List<Shift> allShifts = new ArrayList<>();
    private List<Shift> userShifts = new ArrayList<>();
    private boolean showingAllShifts = true;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_home_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(requireView());

        try {
            // Initialize buttons
            infoBtn = view.findViewById(R.id.btnPersonalInfo);
            paySlipBtn = view.findViewById(R.id.btnPaySlip);
            requestBtn = view.findViewById(R.id.btnRequests);  // Updated to match the new button ID
            scheduleBtn = view.findViewById(R.id.schedule);

            if (scheduleBtn != null) {
                scheduleBtn.setChecked(true); // ✅ Prevents crash
            } else {
                Log.e("AdminHomePageFrag", "❌ scheduleBtn is NULL!");
            }
            myShiftBtn = view.findViewById(R.id.myShifts);
            toggleGroup = view.findViewById(R.id.toggleGroup);

            shiftRecyclerView = view.findViewById(R.id.shiftRecyclerView);
            shiftRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            currentUser = FirebaseAuth.getInstance().getCurrentUser();

            shiftAdapter = new ShiftAdapter(new ArrayList<>(), false);
            shiftRecyclerView.setAdapter(shiftAdapter);

            // Load initial shifts
            loadShifts("thisWeek");

            scheduleBtn.setChecked(true);
            showingAllShifts = true;

            // Navigation buttons
            infoBtn.setOnClickListener(v -> {
                Log.d("AdminHomePageFrag", "📌 Personal Info button clicked!");
                try {
                    navController.navigate(R.id.action_adminHomePageFragment_to_personalInfoFrag);
                    Log.d("AdminHomePageFrag", "✅ Navigation to Personal Info successful!");
                } catch (Exception e) {
                    Log.e("AdminHomePageFrag", "❌ Navigation failed!", e);
                }
            });

            paySlipBtn.setOnClickListener(v -> {
                try {
                    navController.navigate(R.id.action_adminHomePageFragment_to_showFrag);
                } catch (Exception e) {
                    Log.e("Navigation", "Error navigating to pay slip", e);
                }
            });

            requestBtn.setOnClickListener(v -> {
                try {
                    navController.navigate(R.id.action_adminHomePageFragment_to_requestsFrag);
                } catch (Exception e) {
                    Log.e("Navigation", "Error navigating to requests", e);
                }
            });

            // Toggle button behavior
            toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    boolean isMyShifts = (checkedId == R.id.myShifts);
                    shiftAdapter.updateShifts(isMyShifts ? userShifts : allShifts, isMyShifts);
                    Log.d("ShiftDebug", "👤 Displaying " + (isMyShifts ? "My Shifts" : "Schedule"));
                }
            });
        } catch (Exception e) {
            Log.e("InitializationError", "Error initializing views", e);
        }
    }

    private void loadShifts(String weekType) {
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

                String userId = currentUser.getUid();
                Log.d("ShiftDebug", "🔍 User ID: " + userId);

                String[] weekdays = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

                LinkedHashMap<String, List<Shift>> shiftMap = new LinkedHashMap<>();
                for (String day : weekdays) {
                    shiftMap.put(day, new ArrayList<>());
                }

                for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                    DataSnapshot shiftsSnapshot = workIdSnapshot.child("shifts").child(weekType);

                    for (String day : weekdays) {
                        DataSnapshot daySnapshot = shiftsSnapshot.child(day);

                        if (daySnapshot.exists() && daySnapshot.hasChildren()) {
                            for (DataSnapshot shiftSnapshot : daySnapshot.getChildren()) {
                                if (shiftSnapshot.hasChild("fTime") && shiftSnapshot.hasChild("sTime") && shiftSnapshot.hasChild("workerId")) {
                                    String fTime = shiftSnapshot.child("fTime").getValue(String.class);
                                    String sTime = shiftSnapshot.child("sTime").getValue(String.class);
                                    String workerId = shiftSnapshot.child("workerId").getValue(String.class);
                                    String workerName = shiftSnapshot.child("workerName").getValue(String.class);

                                    if (fTime == null || sTime == null || workerId == null) continue;

                                    Shift shift = new Shift(day, sTime, fTime, workerName, workerId);
                                    shiftMap.get(day).add(shift);

                                    if (workerId.equals(userId)) {
                                        userShifts.add(shift);
                                        Log.d("ShiftDebug", "✅ Added user shift: " + sTime + " - " + fTime + " on " + day);
                                    }
                                }
                            }
                        }
                    }
                }

                for (String day : weekdays) {
                    List<Shift> shifts = shiftMap.get(day);
                    if (shifts.isEmpty()) {
                        allShifts.add(new Shift(day, "No Shift", "", "", ""));
                    } else {
                        allShifts.addAll(shifts);
                    }
                }

                Log.d("ShiftDebug", "📊 Total user shifts: " + userShifts.size());

                shiftAdapter.updateShifts(allShifts, false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "❌ Failed to read shifts", error.toException());
            }
        });
    }
}