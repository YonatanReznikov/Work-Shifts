package com.example.work_shifts.Fragments.Worker;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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

public class updateInfoFrag extends Fragment {

    private EditText emailUpdate, phoneUpdate, nameUpdate;
    private Button confirmButton;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private FirebaseUser user;
    private String workId;
    private String userKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.update_info, container, false);

        emailUpdate = view.findViewById(R.id.emailUpdate);
        nameUpdate = view.findViewById(R.id.nameUpdate);
        phoneUpdate = view.findViewById(R.id.phoneUpdate);
        confirmButton = view.findViewById(R.id.confirm);
        progressBar = new ProgressBar(getActivity());
        progressBar.setVisibility(View.GONE);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        confirmButton.setOnClickListener(v -> {
            if (user != null) {
                confirmButton.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                findUserWorkID(this::updateUserInfo);
            } else {
                Toast.makeText(getActivity(), "User is not logged in", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void findUserWorkID(Runnable onComplete) {
        DatabaseReference workRef = FirebaseDatabase.getInstance().getReference("workIDs");

        workRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean userFound = false;

                for (DataSnapshot workEntry : snapshot.getChildren()) {
                    String currentWorkId = workEntry.getKey();
                    DataSnapshot usersSnapshot = workEntry.child("users");

                    for (DataSnapshot userEntry : usersSnapshot.getChildren()) {
                        String email = userEntry.child("email").getValue(String.class);
                        if (email != null && email.equals(user.getEmail())) {
                            workId = currentWorkId;
                            userKey = userEntry.getKey();
                            Log.d("Firebase", "Found user in Work ID: " + workId);
                            userFound = true;
                            break;
                        }
                    }

                    if (userFound) {
                        break;
                    }
                }

                if (workId != null && userKey != null) {
                    onComplete.run();
                } else {
                    confirmButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                confirmButton.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Log.e("FirebaseError", "Error fetching work ID", error.toException());
            }
        });
    }

    private void updateUserInfo() {
        if (workId == null || userKey == null) {
            confirmButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getActivity(), "User not found in database", Toast.LENGTH_SHORT).show();
            return;
        }

        String newEmail = emailUpdate.getText().toString().trim();
        String newPhone = phoneUpdate.getText().toString().trim();
        String newName = nameUpdate.getText().toString().trim();


        if (TextUtils.isEmpty(newEmail) && TextUtils.isEmpty(newPhone)&& TextUtils.isEmpty(newName)) {
            confirmButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getActivity(), "Please enter at least one field", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("workIDs")
                .child(workId)
                .child("users")
                .child(userKey);

        if (!TextUtils.isEmpty(newEmail) && !newEmail.equals(user.getEmail())) {
            user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    userRef.child("email").setValue(newEmail);
                    Toast.makeText(getActivity(), "Email updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Failed to update email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("FirebaseError", "Email Update Error", task.getException());
                }
            });
        }
        if (!TextUtils.isEmpty(newName)) {
            userRef.child("name").setValue(newName).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getActivity(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Failed to update the name", Toast.LENGTH_SHORT).show();
                }
            });
        }
            if (!TextUtils.isEmpty(newPhone)) {
                userRef.child("phone").setValue(newPhone).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Phone number updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "Failed to update phone number", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            confirmButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        }
    }