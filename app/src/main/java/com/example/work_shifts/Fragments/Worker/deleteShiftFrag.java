package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.work_shifts.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class deleteShiftFrag extends Fragment {

    private TextView currentDate;
    private ImageButton prevDate, nextDate;
    private Button deleteButton;
    private ScrollView dailyCalendar;
    private Calendar calendar;
    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.delete_shift, container, false);

        currentDate = view.findViewById(R.id.currentDate);
        prevDate = view.findViewById(R.id.prevDate);
        nextDate = view.findViewById(R.id.nextDate);
        deleteButton = view.findViewById(R.id.deleteButton);
        dailyCalendar = view.findViewById(R.id.dailyCalendar);

        calendar = Calendar.getInstance();
        updateDateDisplay();

        prevDate.setOnClickListener(v -> changeDate(-1));
        nextDate.setOnClickListener(v -> changeDate(1));
        deleteButton.setOnClickListener(v -> deleteShift());

        databaseReference = FirebaseDatabase.getInstance().getReference("shifts");

        return view;
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
        currentDate.setText(dateFormat.format(calendar.getTime()));
    }

    private void changeDate(int offset) {
        calendar.add(Calendar.DAY_OF_MONTH, offset);
        updateDateDisplay();
    }

    private void deleteShift() {
        String date = new SimpleDateFormat("d_M_yyyy", Locale.US).format(calendar.getTime());
        databaseReference.child(date).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Shift deleted successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to delete shift", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}