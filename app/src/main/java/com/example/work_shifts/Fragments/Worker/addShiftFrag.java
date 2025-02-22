package com.example.work_shifts.Fragments.Worker;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

    private TextView weekTextView, selectedDayTextView;
    private Calendar calendar;
    private Spinner startTimeSpinner, endTimeSpinner;
    private Button addShiftButton, thisWeekButton, nextWeekButton;
    private DatabaseReference databaseReference;
    private LinearLayout daysContainer;
    private FirebaseAuth mAuth;
    private Button lastSelectedButton = null;
    private String userId, userName, workId;
    private String selectedWeek = "thisWeek";
    private TextView totalHoursText;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_shift, container, false);

        weekTextView = view.findViewById(R.id.weekTextView);
        selectedDayTextView = new TextView(getContext());
        selectedDayTextView.setId(View.generateViewId());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 16, 0, 16);
        selectedDayTextView.setLayoutParams(layoutParams);
        ((LinearLayout) view.findViewById(R.id.daysContainer)).addView(selectedDayTextView);

        thisWeekButton = view.findViewById(R.id.thisWeekButton);
        nextWeekButton = view.findViewById(R.id.nextWeekButton);
        startTimeSpinner = view.findViewById(R.id.startTimeSpinner);
        endTimeSpinner = view.findViewById(R.id.endTimeSpinner);
        addShiftButton = view.findViewById(R.id.addShiftButton);
        daysContainer = view.findViewById(R.id.daysContainer);
        totalHoursText = view.findViewById(R.id.totalHoursText);

        calendar = Calendar.getInstance();
        updateDaysContainer();

        thisWeekButton.setOnClickListener(v -> changeWeek(0));
        nextWeekButton.setOnClickListener(v -> changeWeek(1));
        addShiftButton.setOnClickListener(v -> addShiftToDatabase());

        setupSpinners();

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("workIDs");

        fetchUserData(null);

        return view;
    }

    private void fetchUserData(@Nullable Runnable onComplete) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e("UserData", "❌ User is not signed in.");
            return;
        }

        final String authUserId = user.getUid(); // Firebase Auth UID
        Log.d("UserData", "🔍 Fetching data for Auth User ID: " + authUserId);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot workIdsSnapshot) {
                if (!workIdsSnapshot.exists()) {
                    Log.e("UserData", "❌ No workIDs found in Firebase.");
                    return;
                }

                for (DataSnapshot workIdEntry : workIdsSnapshot.getChildren()) {
                    String currentWorkId = workIdEntry.getKey();
                    if (workId != null) break;

                    DatabaseReference usersRef = databaseReference.child(currentWorkId).child("users");

                    usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                String storedUserId = userSnapshot.getKey();
                                String storedEmail = userSnapshot.child("email").getValue(String.class);

                                if (storedUserId.equals(authUserId)) {
                                    userId = storedUserId; // Match Firebase Auth UID with the database user ID
                                    userName = userSnapshot.child("name").getValue(String.class);
                                    workId = currentWorkId;

                                    if (userName == null || userName.isEmpty()) {
                                        userName = "Unknown Worker";
                                    }

                                    Log.d("UserData", "✅ Database User ID: " + userId);
                                    Log.d("UserData", "✅ User Name: " + userName);
                                    Log.d("UserData", "✅ Work ID Found: " + workId);

                                    if (onComplete != null) {
                                        onComplete.run();
                                    }
                                    return;
                                }
                            }

                            Log.e("UserData", "❌ User ID not found in workID: " + currentWorkId);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("UserData", "❌ Failed to retrieve user info", error.toException());
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("UserData", "❌ Failed to retrieve work IDs", error.toException());
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
            dayButton.setOnClickListener(v -> onDaySelected(dayName, dayButton));
            daysContainer.addView(dayButton);
        }
    }
    private void onDaySelected(String dayName, Button selectedButton) {
        selectedDayTextView.setText(dayName);
        Toast.makeText(getContext(), "Selected day: " + dayName, Toast.LENGTH_SHORT).show();

        if (lastSelectedButton != null) {
            lastSelectedButton.setBackgroundColor(Color.LTGRAY);
        }

        selectedButton.setBackgroundColor(Color.BLUE);
        lastSelectedButton = selectedButton;
    }
    private void changeWeek(int offset) {
        if (offset == 0) {
            selectedWeek = "thisWeek";
        } else {
            selectedWeek = "nextWeek";
        }
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

        int defaultStartPosition = timeOptions.indexOf("07:00");
        int defaultEndPosition = timeOptions.indexOf("14:00");

        if (defaultStartPosition != -1) {
            startTimeSpinner.setSelection(defaultStartPosition);
        }
        if (defaultEndPosition != -1) {
            endTimeSpinner.setSelection(defaultEndPosition);
        }

        startTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTotalHours();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        endTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTotalHours();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        updateTotalHours();
    }

    private void updateTotalHours() {
        String startTime = startTimeSpinner.getSelectedItem().toString();
        String endTime = endTimeSpinner.getSelectedItem().toString();

        int startHour = Integer.parseInt(startTime.split(":")[0]);
        int endHour = Integer.parseInt(endTime.split(":")[0]);

        int totalHours = endHour - startHour;

        if (totalHours <= 0) {
            totalHoursText.setText("Invalid Selection");
            totalHoursText.setTextColor(Color.RED);
        } else {
            totalHoursText.setText(String.format("%d Hours", totalHours));
            totalHoursText.setTextColor(Color.BLACK);
        }
    }

    private List<String> getTimeOptions() {
        List<String> timeList = new ArrayList<>();
        for (int hour = 7; hour < 22; hour++) {
            timeList.add(String.format(Locale.US, "%02d:00", hour));
        }
        return timeList;
    }

    private void addShiftToDatabase() {
        if (workId == null || workId.isEmpty() || userName == null || userName.isEmpty() || userId == null) {
            Log.w("ShiftDebug", "❌ Missing user data, retrying...");
            Toast.makeText(requireContext(), "Fetching user data... Please wait.", Toast.LENGTH_SHORT).show();

            // Fetch user data, then retry adding shift
            fetchUserData(() -> {
                if (workId != null && !workId.isEmpty() && userId != null) {
                    addShiftToDatabase();
                } else {
                    Log.e("ShiftDebug", "❌ Work ID or User ID still missing after fetching.");
                }
            });

            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Error: User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDayTextView.getText().toString().isEmpty()) {
            Toast.makeText(requireContext(), "Please select a day first", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedDayName = selectedDayTextView.getText().toString();
        if (isPastDay(selectedDayName)) {
            Toast.makeText(requireContext(), "Cannot add a shift for a past day!", Toast.LENGTH_SHORT).show();
            return;
        }

        addShiftButton.setEnabled(false);

        String startTime = startTimeSpinner.getSelectedItem().toString();
        String endTime = endTimeSpinner.getSelectedItem().toString();

        if (startTime.compareTo(endTime) >= 0) {
            Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
            addShiftButton.setEnabled(true);
            return;
        }

        Shift shift = new Shift();
        shift.sTime = startTime;
        shift.fTime = endTime;
        shift.workerId = userId;
        shift.workerName = userName;

        Log.d("ShiftDebug", "✅ Adding Shift: " + startTime + " - " + endTime + " for " + userName + " (UserID: " + userId + ") in Work ID: " + workId);

        DatabaseReference shiftRef = databaseReference
                .child(workId)
                .child("shifts")
                .child(selectedWeek)
                .child(selectedDayName)
                .push();

        shiftRef.setValue(shift).addOnCompleteListener(task -> {
            addShiftButton.setEnabled(true);
            if (task.isSuccessful()) {
                Toast.makeText(requireContext(), "Shift added successfully!", Toast.LENGTH_SHORT).show();
                Log.d("ShiftDebug", "✅ Shift successfully added for " + userName);
            } else {
                Toast.makeText(requireContext(), "Failed to add shift", Toast.LENGTH_SHORT).show();
                Log.e("ShiftDebug", "🚨 Shift failed to add.");
            }
        });
    }
    private boolean isPastDay(String selectedDayName) {
        Calendar today = Calendar.getInstance();
        Calendar selectedDay = Calendar.getInstance();

        // Find the selected day's date
        List<String> daysOfWeek = Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
        int selectedDayIndex = daysOfWeek.indexOf(selectedDayName);

        if (selectedWeek.equals("nextWeek")) {
            selectedDay.add(Calendar.DAY_OF_YEAR, 7);
        }

        if (selectedDayIndex != -1) {
            selectedDay.set(Calendar.DAY_OF_WEEK, selectedDayIndex + 1);
        }

        return selectedDay.before(today);
    }

    public static class Shift {
        public String sTime, fTime, workerId, workerName;

        public Shift() {}
    }
}
