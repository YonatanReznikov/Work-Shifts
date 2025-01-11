package com.example.work_shifts.Fragments;

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
import android.widget.Toast;

import com.example.work_shifts.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

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
        mAuth = FirebaseAuth.getInstance();
        EditText emailInput = view.findViewById(R.id.emailInput);
        EditText passwordInput = view.findViewById(R.id.passwordInput);
        Button loginButton = view.findViewById(R.id.SigninButton);
        Button registerButton = view.findViewById(R.id.RegisterButton);

        loginButton.setOnClickListener(v -> {
            log(view);
        });

        registerButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Navigation.findNavController(view).navigate(R.id.action_loginFrag_to_registerFrag);

            }
        });

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

    public void login(View view) {
        String email = ((EditText) view.findViewById(R.id.emailInput)).getText().toString();
        String password = ((EditText) view.findViewById(R.id.passwordInput)).getText().toString();

        if (!isValidEmail(email)) {
            Toast.makeText(getActivity(), "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidPassword(password)) {
            Toast.makeText(getActivity(), "Password must be 8+ chars, with upper, lower, and digit", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public void  log(View view){

        EditText emailInput = view.findViewById(R.id.emailInput);
        EditText passwordInput = view.findViewById(R.id.passwordInput);
        String emailValue = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        mAuth.signInWithEmailAndPassword(emailValue, password)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Login successful!", Toast.LENGTH_LONG).show();
                            Navigation.findNavController(view).navigate(R.id.action_loginFrag_to_homePageFrag);
                        } else {
                            Toast.makeText(getActivity(), "Login failed!", Toast.LENGTH_LONG).show();            }
                    }
                });
    }
}