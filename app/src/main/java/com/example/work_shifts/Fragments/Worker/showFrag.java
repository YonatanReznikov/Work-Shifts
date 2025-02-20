package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.work_shifts.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class showFrag extends Fragment {

    private EditText companyField, emailField, phoneField;
    private TextView totalHoursText, paycheckText, paycheckAfterTaxesText;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.show, container, false);

        companyField = view.findViewById(R.id.companyField);
        emailField = view.findViewById(R.id.emailField);
        phoneField = view.findViewById(R.id.phoneField);
        totalHoursText = view.findViewById(R.id.hours);
        paycheckText = view.findViewById(R.id.payments);
        paycheckAfterTaxesText = view.findViewById(R.id.paycheck);

        disableEditText(companyField);
        disableEditText(emailField);
        disableEditText(phoneField);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            fetchUserData();
        } else {
            Toast.makeText(getContext(), "No logged-in user found", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void fetchUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        final String userEmail = user.getEmail();
        if (userEmail == null) return;

        final String lowerCaseEmail = userEmail.toLowerCase();

        DatabaseReference workIDsRef = FirebaseDatabase.getInstance().getReference("workIDs");
        workIDsRef.keepSynced(true);

        workIDsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot workIdsSnapshot) {
                if (!workIdsSnapshot.exists()) return;

                for (DataSnapshot workIdEntry : workIdsSnapshot.getChildren()) {
                    String workId = workIdEntry.getKey();
                    DatabaseReference usersRef = workIDsRef.child(workId).child("users");

                    usersRef.orderByChild("email").equalTo(lowerCaseEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) return;

                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                String email = userSnapshot.child("email").getValue(String.class);
                                String phone = userSnapshot.child("phone").getValue(String.class);
                                String companyName = workIdsSnapshot.child(workId).child("companyName").getValue(String.class);
                                String totalHoursStr = userSnapshot.child("totalHours").getValue(String.class);

                                int totalHours = Integer.parseInt(totalHoursStr);
                                int paycheck = totalHours * 40;
                                int paycheckAfterTaxes = (int) (paycheck * 0.82);

                                if (emailField != null) emailField.setText(email);
                                if (phoneField != null) phoneField.setText(phone != null ? phone : "");
                                if (companyField != null) companyField.setText(companyName != null ? companyName : "");
                                if (totalHoursText != null) totalHoursText.setText("Total Hours: " + totalHours);
                                if (paycheckText != null) paycheckText.setText("Paycheck: $" + paycheck);
                                if (paycheckAfterTaxesText != null) paycheckAfterTaxesText.setText("Paycheck After Taxes: $" + paycheckAfterTaxes);

                                return;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(getContext(), "Error fetching data", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error connecting to database", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void disableEditText(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(false);
    }
}
