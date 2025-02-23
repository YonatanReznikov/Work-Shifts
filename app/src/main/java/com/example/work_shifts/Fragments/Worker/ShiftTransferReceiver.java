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
        Log.d("ShiftDebug", "⏰ Alarm triggered! Moving nextWeek shifts to thisWeek...");
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

                    // Read "nextWeek" shifts
                    DataSnapshot nextWeekSnapshot = workIdSnapshot.child("shifts").child("nextWeek");
                    if (nextWeekSnapshot.exists()) {
                        workRef.child("thisWeek").setValue(nextWeekSnapshot.getValue()) /// ✅ Move shifts
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        workRef.child("nextWeek").setValue(null); /// ✅ Clear nextWeek
                                        Log.d("ShiftDebug", "✅ Moved nextWeek shifts to thisWeek for workID: " + workId);
                                    }
                                });
                    } else {
                        Log.d("ShiftDebug", "⚠️ No shifts found in nextWeek for workID: " + workId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "❌ Failed to move shifts", error.toException());
            }
        });
    }
}
