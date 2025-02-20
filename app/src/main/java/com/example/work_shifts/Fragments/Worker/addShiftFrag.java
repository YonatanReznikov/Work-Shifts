package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.work_shifts.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

class AddShiftFragment extends Fragment {

    private TextView monthTextView;
    private Calendar calendar;
    private Spinner startTimeSpinner, endTimeSpinner;
    private Button addShiftButton, submitShiftButton;
    private EditText notesEditText;
    private DatabaseReference databaseReference;

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
        submitShiftButton = view.findViewById(R.id.submitShiftButton);

        calendar = Calendar.getInstance();
        updateMonthTextView();

        prevMonthButton.setOnClickListener(v -> changeMonth(-1));
        nextMonthButton.setOnClickListener(v -> changeMonth(1));
        addShiftButton.setOnClickListener(v -> {
            addShiftToDatabase();
        });
        submitShiftButton.setOnClickListener(v -> {
        });

        setupSpinners();

        databaseReference = FirebaseDatabase.getInstance().getReference("shifts");

        return view;
    }

    private void updateMonthTextView() {
        monthTextView.setText(MONTHS[calendar.get(Calendar.MONTH)]);
    }

    private void changeMonth(int offset) {
        calendar.add(Calendar.MONTH, offset);
        updateMonthTextView();
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

        Shift shift = new Shift(startTime, endTime, notes);
        databaseReference.push().setValue(shift)
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

        public Shift() {
        }

        public Shift(String startTime, String endTime, String notes) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.notes = notes;
        }
    }
}