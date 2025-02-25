package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class deleteShiftFrag extends Fragment {

    private TextView weekTextView;
    private Button nextWeekButton, deleteButton;
    private LinearLayout myShiftsContainer;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String userId, workId;
    private Map<Shift, String> shiftIdMap = new HashMap<>();
    private boolean showingNextWeek = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.delete_shift, container, false);

        initializeViews(view);
        initializeFirebase();

        fetchUserData(() -> {
            loadUserShifts("thisWeek");
            loadPendingRemovals("thisWeek");
        });

        nextWeekButton.setOnClickListener(v -> {
            showingNextWeek = !showingNextWeek;

            String selectedWeek = showingNextWeek ? "nextWeek" : "thisWeek";
            String removalWeek = showingNextWeek ? "thisWeek" : "nextWeek";

            weekTextView.setText(showingNextWeek ? "Next Week" : "This Week");
            nextWeekButton.setText(showingNextWeek ? "Current Week" : "Next Week");

            loadUserShifts(selectedWeek);
            loadPendingRemovals(selectedWeek);
        });

        deleteButton.setOnClickListener(v -> deleteSelectedShifts());

        return view;
    }

    private RecyclerView removePendingShiftsRecyclerView;
    private removePendingShiftAdapter adapter;
    private List<Shift> pendingShiftList = new ArrayList<>();

    private void initializeViews(View view) {
        weekTextView = view.findViewById(R.id.weekTextView);
        nextWeekButton = view.findViewById(R.id.nextWeekButton);
        deleteButton = view.findViewById(R.id.deleteButton);
        myShiftsContainer = view.findViewById(R.id.myShiftsContainer);

        removePendingShiftsRecyclerView = view.findViewById(R.id.removePendingShiftsRecyclerView);
        removePendingShiftsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new removePendingShiftAdapter(getContext(), pendingShiftList, shiftIdMap, workId);
        removePendingShiftsRecyclerView.setAdapter(adapter);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("workIDs");
    }
    private void fetchUserData(Runnable onComplete) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            return;
        }

        String lowerCaseEmail = user.getEmail().toLowerCase();

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot workIdsSnapshot) {
                if (!workIdsSnapshot.exists()) {
                    return;
                }

                for (DataSnapshot workIdEntry : workIdsSnapshot.getChildren()) {
                    String currentWorkId = workIdEntry.getKey();

                    DatabaseReference usersRef = databaseReference.child(currentWorkId).child("users");
                    usersRef.orderByChild("email").equalTo(lowerCaseEmail)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!snapshot.exists()) {
                                        return;
                                    }

                                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                        userId = userSnapshot.getKey();
                                        workId = currentWorkId;


                                        adapter = new removePendingShiftAdapter(getContext(), pendingShiftList, shiftIdMap, workId);
                                        removePendingShiftsRecyclerView.setAdapter(adapter);

                                        onComplete.run();
                                        return;
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadPendingRemovals(String weekType) {
        if (userId == null || workId == null) return;

        DatabaseReference removalsRef = databaseReference.child(workId)
                .child("waitingShifts").child("removals").child(weekType);

        removalsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Shift> pendingShifts = new ArrayList<>();
                shiftIdMap.clear();

                for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                    for (DataSnapshot shiftSnapshot : daySnapshot.getChildren()) {
                        String shiftId = shiftSnapshot.getKey();
                        String workerId = shiftSnapshot.child("workerId").getValue(String.class);
                        String workerName = shiftSnapshot.child("workerName").getValue(String.class);
                        String sTime = shiftSnapshot.child("sTime").getValue(String.class);
                        String fTime = shiftSnapshot.child("fTime").getValue(String.class);
                        String day = daySnapshot.getKey();

                        if (workerId != null && workerId.equals(userId)) {
                            Shift shift = new Shift(day, sTime, fTime, workerName, workerId, weekType);
                            pendingShifts.add(shift);
                            shiftIdMap.put(shift, shiftId);
                        }
                    }
                }

                updateRecyclerView(pendingShifts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("❌ Failed to load pending removals");
            }
        });
    }
    public String getShiftId(Shift shift) {
        return shiftIdMap.get(shift);
    }

    private void updateRecyclerView(List<Shift> shifts) {
        pendingShiftList.clear();
        pendingShiftList.addAll(shifts);
        adapter.notifyDataSetChanged();
    }
    private void loadUserShifts(String weekType) {
        if (userId == null || workId == null) return;

        myShiftsContainer.removeAllViews();
        databaseReference.child(workId).child("shifts").child(weekType)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasShifts = false;

                        for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                            for (DataSnapshot shiftSnapshot : daySnapshot.getChildren()) {
                                String shiftId = shiftSnapshot.getKey();
                                String workerId = shiftSnapshot.child("workerId").getValue(String.class);

                                if (workerId != null && workerId.equals(userId)) {
                                    String startTime = shiftSnapshot.child("sTime").getValue(String.class);
                                    String endTime = shiftSnapshot.child("fTime").getValue(String.class);
                                    String shiftDay = daySnapshot.getKey();

                                    CheckBox checkBox = new CheckBox(getContext());
                                    checkBox.setText(String.format(Locale.US, "%s: %s - %s", shiftDay, startTime, endTime));
                                    checkBox.setTag(shiftId);
                                    myShiftsContainer.addView(checkBox);
                                    hasShifts = true;
                                }
                            }
                        }

                        deleteButton.setEnabled(hasShifts);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("Failed to load shifts");
                    }
                });
    }

    private void deleteSelectedShifts() {
        if (userId == null || workId == null) return;

        String weekType = showingNextWeek ? "nextWeek" : "thisWeek";

        for (int i = 0; i < myShiftsContainer.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) myShiftsContainer.getChildAt(i);
            if (checkBox.isChecked()) {
                String shiftId = (String) checkBox.getTag();

                databaseReference.child(workId).child("shifts").child(weekType)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                                    if (daySnapshot.hasChild(shiftId)) {
                                        DataSnapshot shiftSnapshot = daySnapshot.child(shiftId);
                                        Object shiftData = shiftSnapshot.getValue();


                                        databaseReference.child(workId).child("waitingShifts")
                                                .child("removals")
                                                .child(weekType)
                                                .child(daySnapshot.getKey())
                                                .child(shiftId)
                                                .setValue(shiftData)
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        showToast("⏳ Shift marked for removal, awaiting admin approval.");
                                                    } else {
                                                        showToast("❌ Failed to mark shift for removal.");
                                                    }
                                                });
                                        return;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                showToast("❌ Failed to process shift removal.");
                            }
                        });
            }
        }
    }
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
