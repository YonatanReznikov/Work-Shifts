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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    private LinearLayout myShiftsContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.delete_shift, container, false);

        currentDate = view.findViewById(R.id.currentDate);
        prevDate = view.findViewById(R.id.prevDate);
        nextDate = view.findViewById(R.id.nextDate);
        deleteButton = view.findViewById(R.id.deleteButton);
        dailyCalendar = view.findViewById(R.id.dailyCalendar);
        myShiftsContainer = view.findViewById(R.id.myShiftsContainer);

        calendar = Calendar.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("shifts");

        updateDateDisplay();

        prevDate.setOnClickListener(v -> changeDate(-1));
        nextDate.setOnClickListener(v -> changeDate(1));
        deleteButton.setOnClickListener(v -> deleteSelectedShifts());

        return view;
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
        currentDate.setText(dateFormat.format(calendar.getTime()));
        loadShifts();
    }

    private void changeDate(int offset) {
        calendar.add(Calendar.DAY_OF_MONTH, offset);
        updateDateDisplay();
    }

    private void loadShifts() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
        databaseReference.child(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myShiftsContainer.removeAllViews();
                for (DataSnapshot shiftSnapshot : snapshot.getChildren()) {
                    String shiftId = shiftSnapshot.getKey();
                    String startTime = shiftSnapshot.child("startTime").getValue(String.class);
                    String endTime = shiftSnapshot.child("endTime").getValue(String.class);
                    String notes = shiftSnapshot.child("notes").getValue(String.class);

                    CheckBox checkBox = new CheckBox(getContext());
                    checkBox.setText(String.format(Locale.US, "Shift: %s - %s\nNotes: %s", startTime, endTime, notes));
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
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
        for (int i = 0; i < myShiftsContainer.getChildCount(); i++) {
            CheckBox checkBox = (CheckBox) myShiftsContainer.getChildAt(i);
            if (checkBox.isChecked()) {
                String shiftId = (String) checkBox.getTag();
                databaseReference.child(date).child(shiftId).removeValue()
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