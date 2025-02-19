package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.work_shifts.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReportFragment extends Fragment {

    private TextView monthTextView;
    private Button prevMonthButton, nextMonthButton, reportButton;
    private RecyclerView daysRecyclerView;
    private Spinner startDateSpinner, endDateSpinner, reportSpinner;
    private Calendar calendar;
    private DaysAdapter daysAdapter;

    private static final String[] MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_request, container, false);

        monthTextView = view.findViewById(R.id.monthTextView);
        prevMonthButton = view.findViewById(R.id.prevMonthButton);
        nextMonthButton = view.findViewById(R.id.nextMonthButton);
        daysRecyclerView = view.findViewById(R.id.daysRecyclerView);
        startDateSpinner = view.findViewById(R.id.startDateSpinner);
        endDateSpinner = view.findViewById(R.id.endDateSpinner);
        reportSpinner = view.findViewById(R.id.reportSpinner);
        reportButton = view.findViewById(R.id.reportButton);

        calendar = Calendar.getInstance();
        updateMonthDisplay();

        prevMonthButton.setOnClickListener(v -> changeMonth(-1));
        nextMonthButton.setOnClickListener(v -> changeMonth(1));

        daysRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        daysAdapter = new DaysAdapter(getDaysOfMonth());
        daysRecyclerView.setAdapter(daysAdapter);

        setupSpinners();

        return view;
    }

    private void updateMonthDisplay() {
        monthTextView.setText(MONTHS[calendar.get(Calendar.MONTH)]);
    }

    private void changeMonth(int offset) {
        calendar.add(Calendar.MONTH, offset);
        updateMonthDisplay();
        daysAdapter.updateDays(getDaysOfMonth());
    }

    private List<String> getDaysOfMonth() {
        List<String> days = new ArrayList<>();
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(String.valueOf(i));
        }
        return days;
    }

    private void setupSpinners() {
        setupSpinner(startDateSpinner, new String[]{"1/1/25", "2/1/25", "3/1/25"});
        setupSpinner(endDateSpinner, new String[]{"3/1/25", "4/1/25", "5/1/25"});
        setupSpinner(reportSpinner, new String[]{"Sick", ",Military", "Other"});
    }
}
