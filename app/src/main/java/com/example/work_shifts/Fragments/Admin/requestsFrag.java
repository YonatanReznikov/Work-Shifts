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
import java.util.List;

public class requestsFrag extends Fragment {

    private RecyclerView additionsRecyclerView;
    private RecyclerView removalsRecyclerView;
    private RequestsAdapter additionsAdapter;
    private RequestsAdapter removalsAdapter;
    private List<Shift> additionsList;
    private List<Shift> removalsList;

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

        additionsRecyclerView = view.findViewById(R.id.additionsRecyclerView);
        removalsRecyclerView = view.findViewById(R.id.removalsRecyclerView);

        additionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        removalsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        additionsList = new ArrayList<>();
        removalsList = new ArrayList<>();

        additionsAdapter = new RequestsAdapter(additionsList, "additions");
        removalsAdapter = new RequestsAdapter(removalsList, "removals");

        additionsRecyclerView.setAdapter(additionsAdapter);
        removalsRecyclerView.setAdapter(removalsAdapter);

        findUserWorkID();
    }

    private void findUserWorkID() {
        DatabaseReference workIdsRef = FirebaseDatabase.getInstance().getReference("workIDs");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e("Firebase", " Current user is null!");
            return;
        }

        String currentUserId = currentUser.getUid();

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
                    Log.e("Firebase", "❌ No workID found for user: " + currentUserId);
                    return;
                }

                loadWaitingShifts(userWorkId, "additions", additionsList, additionsAdapter);
                loadWaitingShifts(userWorkId, "removals", removalsList, removalsAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadWaitingShifts(String workID, String requestType, List<Shift> shiftList, RequestsAdapter adapter) {
        DatabaseReference waitingRef = FirebaseDatabase.getInstance()
                .getReference("workIDs").child(workID).child("waitingShifts").child(requestType);

        waitingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                shiftList.clear();

                if (!snapshot.exists()) {
                    adapter.notifyDataSetChanged();
                    return;
                }

                for (DataSnapshot weekSnapshot : snapshot.getChildren()) {
                    String weekType = weekSnapshot.getKey();

                    for (DataSnapshot daySnapshot : weekSnapshot.getChildren()) {
                        String day = daySnapshot.getKey();

                        for (DataSnapshot shiftSnapshot : daySnapshot.getChildren()) {
                            String sTime = shiftSnapshot.child("sTime").getValue(String.class);
                            String fTime = shiftSnapshot.child("fTime").getValue(String.class);
                            String workerId = shiftSnapshot.child("workerId").getValue(String.class);
                            String workerName = shiftSnapshot.child("workerName").getValue(String.class);

                            if (sTime == null || fTime == null || workerId == null || workerName == null) {
                                continue;
                            }

                            Shift shift = new Shift(day, sTime, fTime, workerName, workerId, weekType);
                            shiftList.add(shift);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
