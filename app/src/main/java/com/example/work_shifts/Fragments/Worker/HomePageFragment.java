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

    private Button infoBtn, paySlipBtn, myShiftBtn;
    private ImageButton addShiftBtn, removeShiftBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(view);

        infoBtn = view.findViewById(R.id.btnPersonalInfo);
        paySlipBtn = view.findViewById(R.id.btnPaySlip);
        myShiftBtn = view.findViewById(R.id.myShifts);
        addShiftBtn = view.findViewById(R.id.addShift);
        removeShiftBtn = view.findViewById(R.id.removeShift);

        infoBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_personalInfoFrag));
        paySlipBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_showFrag));
        myShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_myShiftFrag));
        addShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_addShiftFrag));
        removeShiftBtn.setOnClickListener(v -> navController.navigate(R.id.action_homePageFragment_to_deleteShiftFrag));
    }
}
