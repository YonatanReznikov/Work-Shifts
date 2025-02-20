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
    private boolean showingAllShifts = true;
    private FirebaseUser currentUser;

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

        // Initialize RecyclerView
        shiftRecyclerView = view.findViewById(R.id.shiftRecyclerView);
        shiftRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Firebase setup
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Set adapter
        shiftAdapter = new ShiftAdapter(new ArrayList<>());
        shiftRecyclerView.setAdapter(shiftAdapter);

        // Load shifts from Firebase
        loadShifts();

        // Set default selection to "Schedule"
        scheduleBtn.setChecked(true);
        showingAllShifts = true;

        // Button click listeners
        infoBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_personalInfoFrag));
        paySlipBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_showFrag));
        addShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_addShiftFrag));
        removeShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_deleteShiftFrag));

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.schedule) {
                    showingAllShifts = true;
                    shiftAdapter.updateShifts(allShifts);
                } else if (checkedId == R.id.myShifts) {
                    showingAllShifts = false;
                    Log.d("ShiftDebug", "Switching to My Shifts view. User shifts count: " + userShifts.size());
                    shiftAdapter.updateShifts(userShifts);
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
                    Log.e("ShiftDebug", "Current user is null!");
                    return;
                }

                String userId = currentUser.getUid();
                Log.d("ShiftDebug", "User ID: " + userId);

                String[] weekdays = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

                LinkedHashMap<String, List<Shift>> shiftMap = new LinkedHashMap<>();
                for (String day : weekdays) {
                    shiftMap.put(day, new ArrayList<>());
                }

                // Loop through each work ID (e.g., 101)
                for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                    DataSnapshot shiftsSnapshot = workIdSnapshot.child("shifts");

                    // Loop through each day of the week
                    for (String day : weekdays) {
                        DataSnapshot daySnapshot = shiftsSnapshot.child(day);

                        if (daySnapshot.exists() && daySnapshot.hasChildren()) {
                            for (DataSnapshot shiftSnapshot : daySnapshot.getChildren()) {
                                // ✅ Correct way to access data inside `shiftId1`, `shiftId2`, etc.
                                if (shiftSnapshot.hasChild("fTime") && shiftSnapshot.hasChild("sTime") && shiftSnapshot.hasChild("workerId")) {
                                    String fTime = shiftSnapshot.child("fTime").getValue(String.class);
                                    String sTime = shiftSnapshot.child("sTime").getValue(String.class);
                                    String workerId = shiftSnapshot.child("workerId").getValue(String.class);
                                    String workerName = shiftSnapshot.child("workerName").getValue(String.class);

                                    if (fTime == null || sTime == null || workerId == null) continue;

                                    Shift shift = new Shift(day, sTime + " - " + fTime, workerName, workerId);
                                    shiftMap.get(day).add(shift);

                                    // ✅ Add shift to `userShifts` if it belongs to the logged-in user
                                    if (workerId.equals(userId)) {
                                        userShifts.add(shift);
                                        Log.d("ShiftDebug", "Added user shift: " + sTime + " - " + fTime + " on " + day);
                                    }
                                }
                            }
                        }
                    }
                }

                // Populate `allShifts` list
                for (String day : weekdays) {
                    List<Shift> shifts = shiftMap.get(day);
                    if (shifts.isEmpty()) {
                        allShifts.add(new Shift(day, "No Shift", "", ""));
                    } else {
                        allShifts.addAll(shifts);
                    }
                }

                Log.d("ShiftDebug", "Total user shifts: " + userShifts.size());

                // ✅ Show all shifts initially
                shiftAdapter.updateShifts(allShifts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to read shifts", error.toException());
            }
        });
    }

}
