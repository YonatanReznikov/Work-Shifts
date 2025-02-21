package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.work_shifts.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class deleteShiftFrag extends Fragment {

    private TextView currentDate, currentDay;
    private ImageButton prevDate, nextDate;
    private Button deleteButton;
    private ScrollView dailyCalendar;
    private Calendar calendar;
    private DatabaseReference databaseReference;
    private LinearLayout myShiftsContainer;
    private FirebaseAuth mAuth;
    private String userId;
    private String workId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.delete_shift, container, false);

        currentDate = view.findViewById(R.id.currentDate);
        currentDay = view.findViewById(R.id.currentDay);
        prevDate = view.findViewById(R.id.prevDate);
        nextDate = view.findViewById(R.id.nextDate);
        deleteButton = view.findViewById(R.id.deleteButton);
        dailyCalendar = view.findViewById(R.id.dailyCalendar);
        myShiftsContainer = view.findViewById(R.id.myShiftsContainer);

        calendar = Calendar.getInstance();
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("workIDs");

        fetchUserData();
        updateDateDisplay();

        prevDate.setOnClickListener(v -> changeDate(-1));
        nextDate.setOnClickListener(v -> changeDate(1));
        deleteButton.setOnClickListener(v -> deleteSelectedShifts());

        return view;
    }

    private void fetchUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        final String userEmail = user.getEmail();
        if (userEmail == null) return;

        final String lowerCaseEmail = userEmail.toLowerCase();

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot workIdsSnapshot) {
                if (!workIdsSnapshot.exists()) return;

                for (DataSnapshot workIdEntry : workIdsSnapshot.getChildren()) {
                    String currentWorkId = workIdEntry.getKey();
                    DatabaseReference usersRef = databaseReference.child(currentWorkId).child("users");

                    usersRef.orderByChild("email").equalTo(lowerCaseEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) return;

                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                userId = userSnapshot.getKey();
                                workId = currentWorkId;
                                loadShifts();
                                return;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(getContext(), "Failed to retrieve user info", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to retrieve work IDs", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.US);
        currentDate.setText(dateFormat.format(calendar.getTime()));
        currentDay.setText(dayFormat.format(calendar.getTime()));
        loadShifts();
    }

    private void changeDate(int offset) {
        calendar.add(Calendar.DAY_OF_MONTH, offset);
        updateDateDisplay();
    }

    private void loadShifts() {
        if (userId == null || workId == null) return;

        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.US).format(calendar.getTime());
        databaseReference.child(workId).child("shifts").child(dayOfWeek).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myShiftsContainer.removeAllViews();
                for (DataSnapshot shiftSnapshot : snapshot.getChildren()) {
                    String shiftId = shiftSnapshot.getKey();
                    String startTime = shiftSnapshot.child("sTime").getValue(String.class);
                    String endTime = shiftSnapshot.child("fTime").getValue(String.class);

                    CheckBox checkBox = new CheckBox(getContext());
                    checkBox.setText(String.format(Locale.US, "Shift: %s - %s", startTime, endTime));
                    checkBox.setTag(shiftId);
                    myShiftsContainer.addView(checkBox);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load shifts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSelectedShifts() {
        if (userId == null || workId == null) return;

        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.US).format(calendar.getTime());
        for (int i = 0; i < myShiftsContainer.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) myShiftsContainer.getChildAt(i);
            if (checkBox.isChecked()) {
                String shiftId = (String) checkBox.getTag();
                databaseReference.child(workId).child("shifts").child(dayOfWeek).child(shiftId).removeValue()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "Shift deleted successfully", Toast.LENGTH_SHORT).show();
                                loadShifts();
                            } else {
                                Toast.makeText(getContext(), "Failed to delete shift", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }
}