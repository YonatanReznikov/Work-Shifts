package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.work_shifts.R;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class HomePageFragment extends Fragment {

    private TextView monthYearText;
    private RecyclerView calendarRecyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.home_page, container, false);

        initWidgets(view);

        setDayView();

        TextView paySlipButton = view.findViewById(R.id.PaySlipButton);
        paySlipButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_homePageFragment_to_paySlipFrag);
        });

        TextView scheduleButton = view.findViewById(R.id.SchedualButtonHome);
        scheduleButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_homePageFragment_to_myShiftFrag);
        });

        TextView personalInfoButton = view.findViewById(R.id.PersonalInfoButton);
        personalInfoButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_homePageFragment_to_personalInfoFrag);
        });

        TextView myScheduleButton = view.findViewById(R.id.mySchedualButton);
        myScheduleButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_homePageFragment_to_myShiftFrag);
        });

        TextView scheduleButtonTop = view.findViewById(R.id.schedualButton);
        scheduleButtonTop.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_homePageFragment_to_addShiftFrag);
        });

        return view;
    }

    private void initWidgets(View view) {
        calendarRecyclerView = view.findViewById(R.id.calendarRecyclerView);
        monthYearText = view.findViewById(R.id.monthYearTV);
    }

    private void setDayView() {
        ArrayList<LocalTime> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(LocalTime.of(i, 0));
        }

        CalendarAdapter calendarAdapter = new CalendarAdapter(hours);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(requireContext());
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);
    }

    public void previousDayAction(View view) {
        setDayView();
    }

    public void nextDayAction(View view) {
        setDayView();
    }

    public static class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

        private final ArrayList<LocalTime> hours;

        public CalendarAdapter(ArrayList<LocalTime> hours) {
            this.hours = hours;
        }

        @NonNull
        @Override
        public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.calendar_cell, parent, false);
            return new CalendarViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
            LocalTime hour = hours.get(position);
            holder.hourText.setText(hour.format(DateTimeFormatter.ofPattern("hh:mm a")));
        }

        @Override
        public int getItemCount() {
            return hours.size();
        }

        public static class CalendarViewHolder extends RecyclerView.ViewHolder {
            public final TextView hourText;
            public final TextView eventText;

            public CalendarViewHolder(@NonNull View itemView) {
                super(itemView);
                hourText = itemView.findViewById(R.id.hourText);
                eventText = itemView.findViewById(R.id.eventText);
            }
        }
    }
}