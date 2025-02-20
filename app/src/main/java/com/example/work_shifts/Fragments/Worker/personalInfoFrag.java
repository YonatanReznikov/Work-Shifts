package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.work_shifts.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class personalInfoFrag extends Fragment {

    private EditText companyInput, emailField, phoneField, totalHoursInput;
    private TextView userGreeting;
    private Button updateInfoButton, reportShiftButton;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.personal_info_client, container, false);

        userGreeting = view.findViewById(R.id.userGreeting);
        companyInput = view.findViewById(R.id.CompanyInput);
        emailField = view.findViewById(R.id.emailField);
        phoneField = view.findViewById(R.id.phoneField);
        totalHoursInput = view.findViewById(R.id.totalHoursInput);
        updateInfoButton = view.findViewById(R.id.updateInfoButton);
        reportShiftButton = view.findViewById(R.id.reportShiftButton);

        disableEditText(companyInput);
        disableEditText(emailField);
        disableEditText(phoneField);
        disableEditText(totalHoursInput);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            fetchUserData();
        } else {
            Toast.makeText(getContext(), "No logged-in user found", Toast.LENGTH_SHORT).show();
        }
        updateInfoButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_personalInfoFrag_to_updateInfoFrag));

        reportShiftButton.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.action_personalInfoFrag_to_reportFrag));
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
                                String hours = userSnapshot.child("totalHours").getValue(String.class);

                                if (emailField != null) emailField.setText(email);
                                if (phoneField != null) phoneField.setText(phone != null ? phone : "");
                                if (companyInput != null) companyInput.setText(companyName != null ? companyName : "");
                                if (userGreeting != null) {
                                    userGreeting.setText("Hey, " + email.split("@")[0] + "!");
                                }
                                if (totalHoursInput != null) totalHoursInput.setText(hours);
                                return;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void disableEditText(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(false);
    }
}
