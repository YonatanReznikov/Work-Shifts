package com.example.work_shifts.Fragments.Auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.work_shifts.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ResetPassFrag extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText emailInput;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");
    }

    public void resetPassword(String email) {
        if (email.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter your email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Password reset email sent!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(getView()).navigate(R.id.loginFrag);
                    } else {
                        Toast.makeText(getActivity(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void onResetPasswordClick(View view) {
        emailInput = getView().findViewById(R.id.emailInput);
        String email = emailInput.getText().toString().trim();
        resetPassword(email);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.forgotpass_frag, container, false);

        View resetPasswordButton = view.findViewById(R.id.resetPasswordButton);
        resetPasswordButton.setOnClickListener(v -> onResetPasswordClick(v));

        return view;
    }
}
