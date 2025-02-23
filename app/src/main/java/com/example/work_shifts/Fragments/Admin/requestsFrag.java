package com.example.work_shifts.Fragments.Admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.R;
import com.example.work_shifts.Fragments.Worker.Shift;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class requestsFrag extends Fragment {

    private RecyclerView requestsRecyclerView;
    private RequestsAdapter requestsAdapter;
    private DatabaseReference databaseReference;
    private List<Shift> waitingShiftsList;

    public requestsFrag() {
    }

    public static requestsFrag newInstance() {
        return new requestsFrag();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestsRecyclerView = view.findViewById(R.id.requestsRecyclerView);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        waitingShiftsList = new ArrayList<>();
        requestsAdapter = new RequestsAdapter(waitingShiftsList);
        requestsRecyclerView.setAdapter(requestsAdapter);

        findUserWorkID();
    }

    private void findUserWorkID() {
        DatabaseReference workIdsRef = FirebaseDatabase.getInstance().getReference("workIDs");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e("Firebase", "❌ Current user is null!");
            return;
        }

        String currentUserId = currentUser.getUid();

        workIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String userWorkId = null;

                // ✅ Find the correct workID
                for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                    if (workIdSnapshot.child("users").hasChild(currentUserId)) {
                        userWorkId = workIdSnapshot.getKey();
                        break;
                    }
                }

                if (userWorkId == null) {
                    Log.e("Firebase", "❌ No workID found for user: " + currentUserId);
                    return;
                }

                Log.d("Firebase", "✅ Found workID: " + userWorkId);

                // ✅ Now load waiting shifts using the correct workID
                databaseReference = FirebaseDatabase.getInstance().getReference("workIDs").child(userWorkId).child("waitingShifts");
                loadWaitingShifts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "❌ Failed to read workIDs", error.toException());
            }
        });
    }

    private void loadWaitingShifts() {
        if (databaseReference == null) {
            Log.e("Firebase", "❌ Database reference is null. Skipping loadWaitingShifts().");
            return;
        }

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                waitingShiftsList.clear();

                for (DataSnapshot weekSnapshot : snapshot.getChildren()) { // "thisWeek" or "nextWeek"
                    String weekType = weekSnapshot.getKey();

                    for (DataSnapshot daySnapshot : weekSnapshot.getChildren()) { // Days (Sunday, Monday, ...)
                        String day = daySnapshot.getKey();

                        for (DataSnapshot shiftSnapshot : daySnapshot.getChildren()) {
                            String sTime = shiftSnapshot.child("sTime").getValue(String.class);
                            String fTime = shiftSnapshot.child("fTime").getValue(String.class);
                            String workerId = shiftSnapshot.child("workerId").getValue(String.class);
                            String workerName = shiftSnapshot.child("workerName").getValue(String.class);

                            if (sTime == null || fTime == null || workerId == null || workerName == null) {
                                Log.e("Firebase", "❌ Missing shift data in: " + shiftSnapshot.getKey());
                                continue;
                            }

                            Shift shift = new Shift(day, sTime, fTime, workerName, workerId, weekType);
                            waitingShiftsList.add(shift);
                        }
                    }
                }

                Log.d("Firebase", "✅ Loaded " + waitingShiftsList.size() + " waiting shifts.");
                requestsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "❌ Failed to load waiting shifts", error.toException());
            }
        });
    }
}