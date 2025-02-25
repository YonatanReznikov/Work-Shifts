package com.example.work_shifts.Fragments.Worker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.*;

public class ShiftTransferReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        moveNextWeekToThisWeek();
    }

    private void moveNextWeekToThisWeek() {
        DatabaseReference shiftsRef = FirebaseDatabase.getInstance().getReference("workIDs");

        shiftsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                    String workId = workIdSnapshot.getKey();
                    DatabaseReference workRef = shiftsRef.child(workId).child("shifts");

                    DataSnapshot nextWeekSnapshot = workIdSnapshot.child("shifts").child("nextWeek");
                    if (nextWeekSnapshot.exists()) {
                        workRef.child("thisWeek").setValue(nextWeekSnapshot.getValue())
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        workRef.child("nextWeek").setValue(null);
                                    }
                                });
                    } else {
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
