package com.example.work_shifts.Activities;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.work_shifts.R;

public class AdminMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        Toast.makeText(this, "Admin Activity Loaded!", Toast.LENGTH_LONG).show();

        // Ensure NavController is properly set
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.admin_nav_host_fragment);
        if (navHostFragment == null) {
            Log.e("AdminMainActivity", "❌ NavHostFragment is NULL!");
            return;
        }
        NavController navController = navHostFragment.getNavController();
    }

}
