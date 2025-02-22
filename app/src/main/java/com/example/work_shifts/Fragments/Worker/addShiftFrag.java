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
import com.google.firebase.database.DatabaseException;
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
    private Spinner startTimeSpinner, endTimeSpinner, shiftSpinner;
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

        thisWeekButton = view.findViewById(R.id.thisWeekButton);
        nextWeekButton = view.findViewById(R.id.nextWeekButton);
        weekTextView = view.findViewById(R.id.weekTextView);
        selectedDayTextView = new TextView(getContext());
        selectedDayTextView.setId(View.generateViewId());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 16, 0, 16);
        selectedDayTextView.setLayoutParams(layoutParams);
        ((LinearLayout) view.findViewById(R.id.daysContainer)).addView(selectedDayTextView);
        selectedWeek = "thisWeek";

        if (thisWeekButton != null && nextWeekButton != null) {
            // Set "This Week" as selected by default
            selectedWeek = "thisWeek";
            thisWeekButton.setBackgroundColor(Color.BLUE);
            thisWeekButton.setTextColor(Color.WHITE);
            nextWeekButton.setBackgroundColor(Color.LTGRAY);
            nextWeekButton.setTextColor(Color.BLACK);

            // Click Listeners
            thisWeekButton.setOnClickListener(v -> changeWeek(0));
            nextWeekButton.setOnClickListener(v -> changeWeek(1));
        } else {
            Log.e("WeekButtonError", "❌ thisWeekButton or nextWeekButton is null! Check XML layout.");
        }

        startTimeSpinner = view.findViewById(R.id.startTimeSpinner);
        endTimeSpinner = view.findViewById(R.id.endTimeSpinner);
        addShiftButton = view.findViewById(R.id.addShiftButton);
        daysContainer = view.findViewById(R.id.daysContainer);
        totalHoursText = view.findViewById(R.id.totalHoursText);
        shiftSpinner = view.findViewById(R.id.shiftSpinner);
        List<String> shifts = Arrays.asList("Morning Shift 7:00-14:00", "Evening Shift 14:00-21:00", "Custom Shift");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, shifts);
        shiftSpinner.setAdapter(adapter);
        shiftSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedShift = parent.getItemAtPosition(position).toString();
                updateTimeSpinnersBasedOnShift(selectedShift);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        calendar = Calendar.getInstance();
        updateDaysContainer();

        addShiftButton.setOnClickListener(v -> addShiftToDatabase());

        setupSpinners();

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("workIDs");

        fetchUserData(null);

        return view;
    }

    private void updateTimeSpinnersBasedOnShift(String selectedShift) {
        if (selectedShift.contains("Morning Shift")) {
            startTimeSpinner.setSelection(getTimeOptions().indexOf("07:00"));
            endTimeSpinner.setSelection(getTimeOptions().indexOf("14:00"));
            startTimeSpinner.setEnabled(false);
            endTimeSpinner.setEnabled(false);
        } else if (selectedShift.contains("Evening Shift")) {
            startTimeSpinner.setSelection(getTimeOptions().indexOf("14:00"));
            endTimeSpinner.setSelection(getTimeOptions().indexOf("21:00"));
            startTimeSpinner.setEnabled(false);
            endTimeSpinner.setEnabled(false);
        } else { // Custom Shift
            startTimeSpinner.setSelection(getTimeOptions().indexOf("07:00"));
            endTimeSpinner.setSelection(getTimeOptions().indexOf("14:00"));
            startTimeSpinner.setEnabled(true);
            endTimeSpinner.setEnabled(true);
        }
        updateTotalHours();
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

        List<String> daysOfWeek = Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");

        for (String dayName : daysOfWeek) {
            Button dayButton = new Button(getContext());
            dayButton.setText(dayName);
            dayButton.setTextSize(16);
            dayButton.setBackgroundColor(Color.LTGRAY);
            dayButton.setTextColor(Color.BLACK);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(8, 0, 8, 0);

            dayButton.setLayoutParams(layoutParams);
            dayButton.setOnClickListener(v -> onDaySelected(dayButton));

            daysContainer.addView(dayButton);
        }
    }

    private void onDaySelected(Button selectedButton) {
        if (lastSelectedButton != null) {
            lastSelectedButton.setBackgroundColor(Color.LTGRAY);
            lastSelectedButton.setTextColor(Color.BLACK);
            lastSelectedButton.setTextSize(16);
        }

        selectedButton.setBackgroundColor(Color.BLUE);
        selectedButton.setTextColor(Color.WHITE);
        selectedButton.setTextSize(18);

        selectedDayTextView.setText(selectedButton.getText().toString());
        lastSelectedButton = selectedButton;
    }

    private void changeWeek(int offset) {
        if (offset == 0) {
            selectedWeek = "thisWeek";
            thisWeekButton.setBackgroundColor(Color.BLUE);
            thisWeekButton.setTextColor(Color.WHITE);
            nextWeekButton.setBackgroundColor(Color.LTGRAY);
            nextWeekButton.setTextColor(Color.BLACK);
        } else { // Selecting "Next Week"
            selectedWeek = "nextWeek";
            nextWeekButton.setBackgroundColor(Color.BLUE);
            nextWeekButton.setTextColor(Color.WHITE);
            thisWeekButton.setBackgroundColor(Color.LTGRAY);
            thisWeekButton.setTextColor(Color.BLACK);
        }
        updateDaysContainer();
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        endTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTotalHours();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
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

        String selectedDayName = selectedDayTextView.getText().toString().replace("Selected Day: ", "");
        if (isPastDay(selectedDayName)) {
            Toast.makeText(requireContext(), "Cannot add a shift for a past day!", Toast.LENGTH_SHORT).show();
            return;
        }

        addShiftButton.setEnabled(false);

        String startTime = startTimeSpinner.getSelectedItem().toString();
        String endTime = endTimeSpinner.getSelectedItem().toString();
        String shiftType = shiftSpinner.getSelectedItem().toString();

        if (startTime.compareTo(endTime) >= 0) {
            Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
            addShiftButton.setEnabled(true);
            return;
        }

        int startHour = Integer.parseInt(startTime.split(":")[0]);
        int endHour = Integer.parseInt(endTime.split(":")[0]);
        int shiftHours = endHour - startHour;

        Shift shift = new Shift();
        shift.sTime = startTime;
        shift.fTime = endTime;
        shift.workerId = userId;
        shift.workerName = userName;

        DatabaseReference shiftRef = databaseReference
                .child(workId)
                .child("shifts")
                .child(selectedWeek)
                .child(selectedDayName)
                .push();

        shiftRef.setValue(shift).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("ShiftDebug", "✅ Shift successfully added for " + userName);
                updateTotalHoursInFirebase(selectedDayName, shiftHours);

                // Display confirmation toast
                Toast.makeText(requireContext(),
                        "✅ Shift Added Successfully! \n\n"
                                + "📅 Day: " + selectedDayName + "\n"
                                + "🕒 Time: " + startTime + " - " + endTime + "\n"
                                + "🔹 Type: " + shiftType,
                        Toast.LENGTH_LONG).show();
            } else {
                Log.e("ShiftDebug", "🚨 Shift failed to add.");
                Toast.makeText(requireContext(), "❌ Failed to add shift. Try again.", Toast.LENGTH_SHORT).show();
            }
            addShiftButton.setEnabled(true);
        });
    }

    private void updateTotalHoursInFirebase(String selectedDay, int shiftHours) {
        DatabaseReference totalHoursRef = databaseReference
                .child(workId)
                .child("users")
                .child(userId)
                .child("totalHours");

        totalHoursRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int currentHours = 0;
                try {
                    currentHours = snapshot.exists() ? snapshot.getValue(Integer.class) : 0;
                } catch (DatabaseException e) {
                    try {
                        currentHours = Integer.parseInt(snapshot.getValue(String.class));
                    } catch (NumberFormatException ex) {
                        Log.e("ShiftDebug", "❌ Invalid totalHours format in Firebase", ex);
                    }
                }

                int newTotalHours = currentHours + shiftHours;
                totalHoursRef.setValue(newTotalHours);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ShiftDebug", "❌ Error retrieving total hours", error.toException());
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

        public Shift() {
        }
    }
}