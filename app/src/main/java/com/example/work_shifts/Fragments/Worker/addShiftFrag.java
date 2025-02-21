package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class addShiftFrag extends Fragment {

    private TextView weekTextView;
    private TextView selectedDayTextView;
    private Calendar calendar;
    private Spinner startTimeSpinner, endTimeSpinner;
    private Button addShiftButton;
    private EditText notesEditText;
    private DatabaseReference databaseReference;
    private LinearLayout daysContainer;
    private FirebaseAuth mAuth;
    private String userId;
    private String userName;
    private String workId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_shift, container, false);

        // Initialize views
        weekTextView = view.findViewById(R.id.weekTextView);
        selectedDayTextView = new TextView(getContext());
        selectedDayTextView.setId(View.generateViewId());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 16, 0, 16);
        selectedDayTextView.setLayoutParams(layoutParams);
        ((LinearLayout) view.findViewById(R.id.daysContainer)).addView(selectedDayTextView);

        Button thisWeekButton = view.findViewById(R.id.thisWeekButton);
        Button nextWeekButton = view.findViewById(R.id.nextWeekButton);
        startTimeSpinner = view.findViewById(R.id.startTimeSpinner);
        endTimeSpinner = view.findViewById(R.id.endTimeSpinner);
        notesEditText = view.findViewById(R.id.notesEditText);
        addShiftButton = view.findViewById(R.id.addShiftButton);
        daysContainer = view.findViewById(R.id.daysContainer);

        calendar = Calendar.getInstance();
        updateDaysContainer();

        thisWeekButton.setOnClickListener(v -> changeWeek(0));
        nextWeekButton.setOnClickListener(v -> changeWeek(1));
        addShiftButton.setOnClickListener(v -> addShiftToDatabase());

        setupSpinners();

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("workIDs");

        fetchUserData();

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
                                userName = userSnapshot.child("name").getValue(String.class);
                                workId = currentWorkId;
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

    private void updateDaysContainer() {
        daysContainer.removeAllViews();
        daysContainer.addView(selectedDayTextView);

        List<String> daysOfWeek = Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");

        for (String dayName : daysOfWeek) {
            Button dayButton = new Button(getContext());
            dayButton.setText(dayName);
            dayButton.setOnClickListener(v -> onDaySelected(dayName));
            daysContainer.addView(dayButton);
        }
    }

    private void changeWeek(int offset) {
        calendar.add(Calendar.WEEK_OF_YEAR, offset);
        updateDaysContainer();
    }

    private void onDaySelected(String dayName) {
        selectedDayTextView.setText(dayName);
        Toast.makeText(getContext(), "Selected day: " + dayName, Toast.LENGTH_SHORT).show();
    }

    private void setupSpinners() {
        List<String> timeOptions = getTimeOptions();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, timeOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        startTimeSpinner.setAdapter(adapter);
        endTimeSpinner.setAdapter(adapter);
    }

    private List<String> getTimeOptions() {
        List<String> timeList = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                timeList.add(String.format(Locale.US, "%02d:%02d", hour, minute));
            }
        }
        return timeList;
    }

    private void addShiftToDatabase() {
        if (userId == null || userName == null || workId == null) {
            Toast.makeText(requireContext(), "User data not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDayTextView.getText().toString().isEmpty()) {
            Toast.makeText(requireContext(), "Please select a day first", Toast.LENGTH_SHORT).show();
            return;
        }

        String startTime = startTimeSpinner.getSelectedItem().toString();
        String endTime = endTimeSpinner.getSelectedItem().toString();
        String notes = notesEditText.getText().toString();

        String dayOfWeek = selectedDayTextView.getText().toString();

        Shift shift = new Shift();
        shift.sTime = startTime;
        shift.fTime = endTime;
        shift.workerId = userId;
        shift.workerName = userName;

        DatabaseReference shiftRef = databaseReference
                .child(workId)
                .child("shifts")
                .child(dayOfWeek)
                .push();

        shiftRef.setValue(shift)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Shift added successfully", Toast.LENGTH_SHORT).show();
                        notesEditText.setText("");
                    } else {
                        Toast.makeText(requireContext(), "Failed to add shift", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static class Shift {
        public String sTime;
        public String fTime;
        public String workerId;
        public String workerName;

        public Shift() {}
    }
}