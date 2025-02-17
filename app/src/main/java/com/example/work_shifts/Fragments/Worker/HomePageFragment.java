package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.R;
import com.google.firebase.auth.FirebaseAuth;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class HomePageFragment extends Fragment {

    private Button infoBtn;
    private Button scheduleBtn;
    private Button PaySlipBbtn;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_page, container, false);
        infoBtn = view.findViewById(R.id.btnPersonalInfo);
        scheduleBtn= view.findViewById(R.id.btnSchedule);
        PaySlipBbtn=  view.findViewById(R.id.btnPaySlip);
        return view;
    }
}
