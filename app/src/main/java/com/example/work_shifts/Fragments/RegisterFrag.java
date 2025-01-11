package com.example.work_shifts.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.work_shifts.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class RegisterFrag extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    public RegisterFrag() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.register_frag, container, false);
        Button registerBtn = view.findViewById(R.id.RegisterButton);
        registerBtn.setOnClickListener(v -> register(view));
        return view;
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*");
    }

    public void register(View view) {
        EditText workIdInput = view.findViewById(R.id.workID);
        EditText emailInput = view.findViewById(R.id.email);
        EditText passwordInput = view.findViewById(R.id.pass1);
        EditText rePasswordInput = view.findViewById(R.id.pass2);
        EditText phoneInput = view.findViewById(R.id.phone);

        String workId = workIdInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String rePassword = rePasswordInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        } else if (!isValidEmail(email)) {
            emailInput.setError("Please enter a valid email");
            return;
        }

        if (!isValidPassword(password)) {
            passwordInput.setError("Password must be 8+ chars, with upper, lower, and digit");
            return;
        }

        if (!password.equals(rePassword)) {
            rePasswordInput.setError("Passwords do not match");
            return;
        }

        if (phone.isEmpty()) {
            phoneInput.setError("Phone number is required");
            return;
        } else if (phone.length() != 10 || !phone.matches("\\d+")) {
            phoneInput.setError("Phone number is not valid");
            return;
        }

        validateWorkId(workId, email, password, phone, view, emailInput);
    }

    private void validateWorkId(String workId, String email, String password, String phone, View view, EditText emailInput) {
        String companyName = getCompanyNameByWorkId(workId);

        if (companyName == null) {
            Toast.makeText(getActivity(), "Invalid Work ID", Toast.LENGTH_LONG).show();
            return;
        }

        databaseReference.child("workIDs").child(workId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    registerUser(email, workId, companyName, password, phone, view, emailInput);
                } else {
                    Toast.makeText(getActivity(), "Invalid Work ID", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getActivity(), "Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getCompanyNameByWorkId(String workId) {
        switch (workId) {
            case "101":
                return "Samsung";
            case "102":
                return "Apple";
            case "103":
                return "Google";
            case "104":
                return "Microsoft";
            case "105":
                return "Nvidia";
            default:
                return null;
        }
    }

    private void registerUser(String email,
                              String workId,
                              String companyName,
                              String password,
                              String phone,
                              View view,
                              EditText emailInput) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, String> userDetails = new HashMap<>();
                        userDetails.put("email", email);
                        userDetails.put("phone", phone);
                        Map<String, Object> companyDetails = new HashMap<>();
                        companyDetails.put("companyName", companyName);

                        DatabaseReference usersRef = databaseReference.child("workIDs").child(workId).child("users");
                        usersRef.push().setValue(userDetails)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(getActivity(), "Registration successful!", Toast.LENGTH_LONG).show();
                                        Navigation.findNavController(view).navigate(R.id.loginFrag);
                                    } else {
                                        Toast.makeText(getActivity(), "Error storing user data: " + dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            emailInput.setError("Email is already in use");
                        } else {
                            Toast.makeText(getActivity(),
                                    "Error: " + (task.getException() != null
                                            ? task.getException().getMessage()
                                            : "Unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}