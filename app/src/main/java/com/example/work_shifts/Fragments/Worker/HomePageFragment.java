package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.work_shifts.R;

public class HomePageFragment extends Fragment {

    private Button infoBtn;
    private Button scheduleBtn;
    private Button paySlipBtn;
    private Button myShiftBtn;
    private ImageButton addShiftBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_page, container, false);

        infoBtn = view.findViewById(R.id.btnPersonalInfo);
        scheduleBtn = view.findViewById(R.id.btnSchedule);
        paySlipBtn = view.findViewById(R.id.btnPaySlip);
        myShiftBtn = view.findViewById(R.id.myShifts);
        addShiftBtn = view.findViewById(R.id.addShift);

        NavController navController = Navigation.findNavController(requireActivity(), R.id.main_nav_graph);

        infoBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_personalInfoFrag));
        paySlipBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_paySlipFrag));
        myShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_myShiftFrag));
        addShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_addShiftFrag));

        return view;
    }
}