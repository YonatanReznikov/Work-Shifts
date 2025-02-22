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
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginFrag extends Fragment {

    private FirebaseAuth mAuth;
    private EditText emailInput;
    private EditText passwordInput;

    public LoginFrag() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_frag, container, false);

        emailInput = view.findViewById(R.id.emailInput);
        passwordInput = view.findViewById(R.id.passwordInput);
        TextView resetButton = view.findViewById(R.id.forgot);
        Button loginButton = view.findViewById(R.id.SignInButton);
        Button registerButton = view.findViewById(R.id.RegisterButton);

        loginButton.setOnClickListener(v -> login());
        registerButton.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.registerFrag));
        resetButton.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.resetPassFrag));

        return view;
    }
    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*");
    }
    private void login() {
        String email = emailInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString().trim();

        if (!isValidEmail(email)) {
            Toast.makeText(getActivity(), "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(getActivity(), "Password must be 8+ chars, with upper, lower, and digit", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String userId = mAuth.getCurrentUser().getUid();

                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference workIdsRef = database.getReference("workIDs");

                            workIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    boolean userFound = false;

                                    for (DataSnapshot workIdSnapshot : snapshot.getChildren()) {
                                        DataSnapshot usersSnapshot = workIdSnapshot.child("users");

                                        for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                                            String userEmail = userSnapshot.child("email").getValue(String.class);
                                            if (email.equals(userEmail)) {
                                                userFound = true;
                                                String isAdminStr = userSnapshot.child("isAdmin").getValue(String.class);

                                                if ("true".equalsIgnoreCase(isAdminStr)) {
                                                    Toast.makeText(getActivity(), "Welcome, Admin!", Toast.LENGTH_LONG).show();
                                                    Intent intent = new Intent(getActivity(), AdminMainActivity.class);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    startActivity(intent);
                                                } else {
                                                    Toast.makeText(getActivity(), "Login successful!", Toast.LENGTH_LONG).show();
                                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                                    startActivity(intent);
                                                }
                                                requireActivity().finish();
                                                break;
                                            }
                                        }
                                        if (userFound) break;
                                    }

                                    if (!userFound) {
                                        Toast.makeText(getActivity(), "User data not found.", Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(getActivity(), "Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });

                        } else {
                            Toast.makeText(getActivity(), "Login failed: Email or Password are incorrect", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
