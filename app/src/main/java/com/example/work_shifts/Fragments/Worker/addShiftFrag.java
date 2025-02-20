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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class addShiftFrag extends Fragment {

    private TextView monthTextView;
    private Calendar calendar;
    private Spinner startTimeSpinner, endTimeSpinner;
    private Button addShiftButton;
    private EditText notesEditText;
    private DatabaseReference databaseReference;
    private LinearLayout daysContainer;

    private static final String[] MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_shift, container, false);

        monthTextView = view.findViewById(R.id.monthTextView);
        Button prevMonthButton = view.findViewById(R.id.prevMonthButton);
        Button nextMonthButton = view.findViewById(R.id.nextMonthButton);
        startTimeSpinner = view.findViewById(R.id.startTimeSpinner);
        endTimeSpinner = view.findViewById(R.id.endTimeSpinner);
        notesEditText = view.findViewById(R.id.notesEditText);
        addShiftButton = view.findViewById(R.id.addShiftButton);
        daysContainer = view.findViewById(R.id.daysContainer);

        calendar = Calendar.getInstance();
        updateMonthTextView();

        prevMonthButton.setOnClickListener(v -> changeMonth(-1));
        nextMonthButton.setOnClickListener(v -> changeMonth(1));
        addShiftButton.setOnClickListener(v -> addShiftToDatabase());

        setupSpinners();

        databaseReference = FirebaseDatabase.getInstance().getReference("shifts");

        return view;
    }

    private void updateMonthTextView() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.US);
        monthTextView.setText(dateFormat.format(calendar.getTime()));
        updateDaysContainer();
    }

    private void changeMonth(int offset) {
        calendar.add(Calendar.MONTH, offset);
        updateMonthTextView();
    }

    private void updateDaysContainer() {
        daysContainer.removeAllViews();

        Calendar tempCalendar = (Calendar) calendar.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= daysInMonth; day++) {
            final int selectedDay = day; // make the day effectively final
            Button dayButton = new Button(getContext());
            dayButton.setText(String.valueOf(day));
            dayButton.setOnClickListener(v -> onDaySelected(selectedDay));
            daysContainer.addView(dayButton);
        }
    }

    private void onDaySelected(int day) {
        calendar.set(Calendar.DAY_OF_MONTH, day);
        Toast.makeText(getContext(), "Selected day: " + day, Toast.LENGTH_SHORT).show();
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
        String startTime = startTimeSpinner.getSelectedItem().toString();
        String endTime = endTimeSpinner.getSelectedItem().toString();
        String notes = notesEditText.getText().toString();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String date = dateFormat.format(calendar.getTime());
        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String timestamp = timestampFormat.format(Calendar.getInstance().getTime());

        Shift shift = new Shift(startTime, endTime, notes, date, timestamp);
        DatabaseReference dateRef = databaseReference.child(date).push();

        dateRef.setValue(shift)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Shift added successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to add shift", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static class Shift {
        public String startTime;
        public String endTime;
        public String notes;
        public String date;
        public String timestamp;

        public Shift() {}

        public Shift(String startTime, String endTime, String notes, String date, String timestamp) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.notes = notes;
            this.date = date;
            this.timestamp = timestamp;
        }
    }
}