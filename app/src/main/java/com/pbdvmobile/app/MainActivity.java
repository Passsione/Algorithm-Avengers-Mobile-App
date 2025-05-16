package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.fragments.DashboardFragment;
import com.pbdvmobile.app.fragments.ExplorerFragment;
import com.pbdvmobile.app.fragments.ResourcesFragment;
import com.pbdvmobile.app.fragments.TutorDashboardFragment;

public class MainActivity extends AppCompatActivity {

    RelativeLayout main;
    ImageButton menuswitch;
    NavigationView nav;
    TextView nav_header, nav_header_email, flash;
    Button exit;
    LogInUser current_user;
    private BottomNavigationView bottomNavigationView;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);


            return insets;
        });

        DataManager dataManager = DataManager.getInstance(this);
        current_user = LogInUser.getInstance(dataManager);

        // ---- Go to log-in page if not logged in
        if(!current_user.isLoggedIn()){
            Intent toLogin = new Intent(MainActivity.this, LogInActivity.class);
            startActivity(toLogin);
            finish(); // can't come back to main activity
            return;
        }
        dataManager.getSessionDao().updatePastSessions();
        // ---- Start - Bottom Navigation Section

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.getMenu().findItem(R.id.navigation_tutor_center).setVisible(current_user.getUser().isTutor());

        // Set the listener for item selection
        bottomNavigationView.setOnItemSelectedListener(navListener);

        // Load the default fragment (e.g., Dashboard) when the activity starts
        // Use commitNow() for the initial fragment if possible, or commit() otherwise
        if (savedInstanceState == null) { // Load default only on initial creation
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mainview_fragment_container, new DashboardFragment())
                    .commit();
            // Optionally, set the Dashboard item as selected
            bottomNavigationView.setSelectedItemId(R.id.navigation_dashboard);
        }

    }

    // Listener for BottomNavigationView item selections
    private final NavigationBarView.OnItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId(); // Get item ID once
                FloatingActionButton fab = findViewById(R.id.dashboard_add_resources);
                fab.setOnClickListener( l ->{
                    Intent toResourceUpload = new Intent(MainActivity.this, ResourceUploadActivity.class);
                    startActivity(toResourceUpload);
                });


                // Determine which fragment to load based on the selected item ID
                if (itemId == R.id.navigation_dashboard) {
                    selectedFragment = new DashboardFragment();
                    fab.setVisibility(GONE);
                } else if (itemId == R.id.navigation_explorer) {
                    selectedFragment = new ExplorerFragment();
                    fab.setVisibility(GONE);
                } else if (itemId == R.id.navigation_resources) {
                    selectedFragment = new ResourcesFragment();
                    fab.setVisibility(current_user.getUser().isTutor() ? VISIBLE : GONE);
                } else if (itemId == R.id.navigation_tutor_center) {
                    selectedFragment = new TutorDashboardFragment();
                    fab.setVisibility(VISIBLE);

                }
                // Replace the current fragment with the selected one
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.mainview_fragment_container, selectedFragment)
                            // Optional: Add to back stack if you want back navigation between fragments
//                             .addToBackStack(null)
                            .commit();
                    return true; // Indicate successful handling
                }

                return false; // Indicate item selection was not handled
            };
}