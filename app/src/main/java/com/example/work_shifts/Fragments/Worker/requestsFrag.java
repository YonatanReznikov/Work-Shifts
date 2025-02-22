package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.work_shifts.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class requestsFrag extends Fragment {

    private TextView selectedDateText;
    private Calendar calendar;

    public requestsFrag() {
        // Required empty public constructor
    }

    public static requestsFrag newInstance() {
        return new requestsFrag();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        selectedDateText = view.findViewById(R.id.selectedDateText);
        ImageButton prevDateButton = view.findViewById(R.id.prevDateButton);
        ImageButton nextDateButton = view.findViewById(R.id.nextDateButton);

        calendar = Calendar.getInstance();

        updateDateText();

        prevDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                updateDateText();
            }
        });

        nextDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                updateDateText();
            }
        });
    }

    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        selectedDateText.setText(sdf.format(calendar.getTime()));
    }
}