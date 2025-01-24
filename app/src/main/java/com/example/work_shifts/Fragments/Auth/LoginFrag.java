package com.example.work_shifts.Fragments.Auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.work_shifts.Activities.AdminMainActivity;
import com.example.work_shifts.Activities.MainActivity;
import com.example.work_shifts.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginFrag extends Fragment {

    private FirebaseAuth mAuth;
    private EditText emailInput;
    private EditText passwordInput;

    public LoginFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.login_frag, container, false);

        // Initialize input fields and buttons
        emailInput = view.findViewById(R.id.emailInput);
        passwordInput = view.findViewById(R.id.passwordInput);
        TextView resetButton = view.findViewById(R.id.forgot);
        Button loginButton = view.findViewById(R.id.SigninButton);
        Button registerButton = view.findViewById(R.id.RegisterButton);

        // Set button listeners
        loginButton.setOnClickListener(v -> login());
        registerButton.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.registerFrag));
        resetButton.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.resetPassFrag));

        return view;
    }

    // Email validation method
    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Password validation method
    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*");
    }

    // Login method
    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validate email
        if (!isValidEmail(email)) {
            Toast.makeText(getActivity(), "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password
        if (!isValidPassword(password)) {
            Toast.makeText(getActivity(), "Password must be 8+ chars, with upper, lower, and digit", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase login logic
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Get the current user's unique ID
                            String userId = mAuth.getCurrentUser().getUid();

                            // Reference to Firebase Realtime Database
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference usersRef = database.getReference("users").child(userId);

                            usersRef.get().addOnCompleteListener(userTask -> {
                                if (userTask.isSuccessful() && userTask.getResult().exists()) {
                                    // Retrieve user details
                                    DataSnapshot userSnapshot = userTask.getResult();
                                    String isAdminStr = userSnapshot.child("isAdmin").getValue(String.class);

                                    if ("true".equalsIgnoreCase(isAdminStr)) {
                                        Toast.makeText(getActivity(), "Welcome, Admin!", Toast.LENGTH_LONG).show();
                                        // Redirect to admin-specific activity
                                        Intent intent = new Intent(getActivity(), AdminMainActivity.class);
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(getActivity(), "Login successful!", Toast.LENGTH_LONG).show();
                                        // Redirect to regular user activity
                                        Intent intent = new Intent(getActivity(), MainActivity.class);
                                        startActivity(intent);
                                    }
                                    requireActivity().finish();
                                } else {
                                    Toast.makeText(getActivity(), "User data not found.", Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            Toast.makeText(getActivity(), "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}

