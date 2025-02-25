package com.example.work_shifts.Fragments.Auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.work_shifts.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.register_frag, container, false);
        Button registerBtn = view.findViewById(R.id.RegisterButton);
        registerBtn.setOnClickListener(v -> register(view));
        return view;
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("\\d{10}");
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*");
    }

    public void register(View view) {
        EditText workIdInput = view.findViewById(R.id.workID);
        EditText nameInput = view.findViewById(R.id.name);
        EditText emailInput = view.findViewById(R.id.email);
        EditText passwordInput = view.findViewById(R.id.pass1);
        EditText rePasswordInput = view.findViewById(R.id.pass2);
        EditText phoneInput = view.findViewById(R.id.phone);

        String name = nameInput.getText().toString().trim();
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
        } else if (!isValidPhone(phone)) {
            phoneInput.setError("Phone number must be exactly 10 digits");
            return;
        }

        checkPhoneUniqueness(workId, phone, name, email, password, view, emailInput, phoneInput);
    }

    private void checkPhoneUniqueness(String workId, String phone, String name, String email, String password, View view, EditText emailInput, EditText phoneInput) {
        DatabaseReference usersRef = databaseReference.child("workIDs").child(workId).child("users");

        usersRef.orderByChild("phone").equalTo(phone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    phoneInput.setError("Phone number is already registered!");
                } else {
                    validateWorkId(workId, email, name, password, phone, view, emailInput);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getActivity(), "Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void validateWorkId(String workId, String email, String name, String password, String phone, View view, EditText emailInput) {
        String companyName = getCompanyNameByWorkId(workId);

        if (companyName == null) {
            Toast.makeText(getActivity(), "Invalid Work ID", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference workIdRef = databaseReference.child("workIDs").child(workId);

        workIdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> workIdDetails = new HashMap<>();
                    workIdDetails.put("companyName", companyName);
                    workIdRef.setValue(workIdDetails).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            registerUser(email, workId, name, companyName, password, phone, view, emailInput);
                        } else {
                            Toast.makeText(getActivity(), "Failed to create Work ID: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    registerUser(email, workId, name, companyName, password, phone, view, emailInput);
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

    private void registerUser(String email, String workId, String name, String companyName, String password, String phone, View view, EditText emailInput) {
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        String lowerEmail = email.toLowerCase();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity(), task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(getActivity(), "Error: User not found after registration", Toast.LENGTH_LONG).show();
                    return;
                }

                String userId = user.getUid();

                Map<String, String> userDetails = new HashMap<>();
                userDetails.put("email", lowerEmail);
                userDetails.put("name", name);
                userDetails.put("phone", phone);
                userDetails.put("totalHours", "0");
                userDetails.put("isAdmin", "false");

                DatabaseReference usersRef = databaseReference.child("workIDs").child(workId).child("users").child(userId);
                usersRef.setValue(userDetails).addOnCompleteListener(dbTask -> {
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
                    Toast.makeText(getActivity(), "Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}