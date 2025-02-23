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

        databaseReference = FirebaseDatabase.getInstance().getReference("workIDs/101/waitingShifts");

        loadWaitingShifts();
    }

    private void loadWaitingShifts() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                waitingShiftsList.clear();

                for (DataSnapshot weekSnapshot : snapshot.getChildren()) { // nextWeek or thisWeek
                    for (DataSnapshot daySnapshot : weekSnapshot.getChildren()) { // Days (Sunday, Monday, ...)
                        for (DataSnapshot shiftSnapshot : daySnapshot.getChildren()) {
                            String sTime = shiftSnapshot.child("sTime").getValue(String.class);
                            String fTime = shiftSnapshot.child("fTime").getValue(String.class);
                            String workerId = shiftSnapshot.child("workerId").getValue(String.class);
                            String workerName = shiftSnapshot.child("workerName").getValue(String.class);

                            if (sTime == null || fTime == null || workerId == null) continue;

                            Shift shift = new Shift(daySnapshot.getKey(), sTime, fTime, workerName, workerId);
                            waitingShiftsList.add(shift);
                        }
                    }
                }

                // Sort shifts by start time
                Collections.sort(waitingShiftsList, Comparator.comparing(Shift::getDay));

                requestsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load waiting shifts", error.toException());
            }
        });
    }
}
