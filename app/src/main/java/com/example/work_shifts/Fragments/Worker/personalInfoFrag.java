package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
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

public class personalInfoFrag extends Fragment {

    private EditText companyInput, emailField, phoneField, totalHoursInput;
    private TextView userGreeting, userIdText;
    private ImageView backgroundImage;
    private DatabaseReference databaseReference;
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

        // Disable input fields
        disableEditText(companyInput);
        disableEditText(emailField);
        disableEditText(phoneField);
        disableEditText(totalHoursInput);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            userIdText.setText("User ID: " + userId);

            databaseReference = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(userId);

            fetchUserData();
        } else {
            Toast.makeText(getContext(), "No logged-in user found", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void fetchUserData() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String companyName = snapshot.child("companyName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);

                    Log.d("FirebaseData", "Company: " + companyName);
                    Log.d("FirebaseData", "Email: " + email);
                    Log.d("FirebaseData", "Phone: " + phone);

                    companyInput.setText(companyName);
                    emailField.setText(email);
                    phoneField.setText(phone);
                    userGreeting.setText("Hey, " + (email != null ? email.split("@")[0] : "User") + "!");
                } else {
                    Log.e("FirebaseData", "No data found for this user.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching data: " + error.getMessage());
            }
        });

    }

    private void disableEditText(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(false);
    }
}
