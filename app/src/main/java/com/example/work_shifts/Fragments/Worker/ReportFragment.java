package com.example.work_shifts.Fragments.Worker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.work_shifts.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportFragment extends Fragment {

    private static final int PICK_FILE_REQUEST = 1;

    private TextView monthTextView, uploadTextView;
    private Button prevMonthButton, nextMonthButton, reportButton, uploadButton;
    private Spinner startDateSpinner, endDateSpinner, reportSpinner;
    private Calendar calendar;
    private Uri selectedFileUri = null;

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
        startDateSpinner = view.findViewById(R.id.startDateSpinner);
        endDateSpinner = view.findViewById(R.id.endDateSpinner);
        reportSpinner = view.findViewById(R.id.reportSpinner);
        reportButton = view.findViewById(R.id.reportButton);
        uploadButton = view.findViewById(R.id.uploadButton);
        uploadTextView = view.findViewById(R.id.uploadTextView);

        calendar = Calendar.getInstance();
        updateMonthDisplay();

        prevMonthButton.setOnClickListener(v -> changeMonth(-1));
        nextMonthButton.setOnClickListener(v -> changeMonth(1));

        setupSpinners();

        uploadButton.setOnClickListener(v -> openFilePicker());
        reportButton.setOnClickListener(v -> submitReport());
        reportButton.setEnabled(false);

        return view;
    }

    private void updateMonthDisplay() {
        monthTextView.setText(MONTHS[calendar.get(Calendar.MONTH)]);
        updateDateSpinners();
    }
    private void changeMonth(int offset) {
        calendar.add(Calendar.MONTH, offset);
        updateMonthDisplay();
    }
    private void updateDateSpinners() {
        List<String> dateOptions = getDatesForCurrentMonth();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, dateOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        startDateSpinner.setAdapter(adapter);
        endDateSpinner.setAdapter(adapter);
    }
    private List<String> getDatesForCurrentMonth() {
        List<String> dateList = new ArrayList<>();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        SimpleDateFormat dateFormat = new SimpleDateFormat("d/M/yy", Locale.US);

        for (int day = 1; day <= daysInMonth; day++) {
            Calendar date = Calendar.getInstance();
            date.set(year, month, day);
            dateList.add(dateFormat.format(date.getTime()));
        }
        return dateList;
    }

    private void setupSpinners() {
        updateDateSpinners();
        setupSpinner(reportSpinner, new String[]{"Sick", "Military", "Other"});
    }

    private void setupSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a document"), PICK_FILE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedFileUri = data.getData();

            if (selectedFileUri != null) {
                uploadTextView.setText("File Selected: " + selectedFileUri.getLastPathSegment());
                reportButton.setEnabled(true);
                Toast.makeText(getContext(), "File selected successfully!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Handles report submission */
    private void submitReport() {
        if (selectedFileUri != null) {
            Toast.makeText(getContext(), "Uploaded Successfully!", Toast.LENGTH_SHORT).show();
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            fragmentManager.popBackStack();
        }
    }
}
