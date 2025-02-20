package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.work_shifts.R;

public class deleteShiftFrag extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.delete_shift, container, false);

        TextView currentDate = view.findViewById(R.id.currentDate);
        ImageButton prevDate = view.findViewById(R.id.prevDate);
        ImageButton nextDate = view.findViewById(R.id.nextDate);
        Button deleteButton = view.findViewById(R.id.deleteButton);
        ScrollView dailyCalendar = view.findViewById(R.id.dailyCalendar);

        prevDate.setOnClickListener(v -> {
        });

        nextDate.setOnClickListener(v -> {
        });

        deleteButton.setOnClickListener(v -> {
        });

        return view;
    }
}