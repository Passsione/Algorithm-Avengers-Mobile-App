package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.fragments.DashboardFragment;
import com.pbdvmobile.app.fragments.ExplorerFragment;
import com.pbdvmobile.app.fragments.ResourcesFragment;
import com.pbdvmobile.app.fragments.TutorDashboardFragment;

public class MainActivity extends AppCompatActivity {

    RelativeLayout main;
    ImageButton menuswitch;
    NavigationView nav;
    TextView nav_header, nav_header_email, flash;
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
        LogInUser current_user = LogInUser.getInstance(dataManager);

        // ---- Go to log-in page if not logged in
        if(!current_user.isLoggedIn()){
            Intent toLogin = new Intent(MainActivity.this, LogInActivity.class);
            startActivity(toLogin);
            finish(); // can't come back to main activity
            return;
        }



        /* ---- Start - side navigation section */

        // Side navigation Switch
        menuswitch = findViewById(R.id.imgMenu);
        main = findViewById(R.id.main);
        main.setOnClickListener(v -> {
            flash = findViewById(R.id.txtError);
            nav.setVisibility(GONE);
        });
        menuswitch.setOnClickListener(v -> nav.setVisibility(VISIBLE));

        // Side navigation
        nav = findViewById(R.id.nav_view);
        var headerView = nav.getHeaderView(0);
        nav_header = headerView.findViewById(R.id.nav_header_name);
        nav_header_email = headerView.findViewById(R.id.nav_header_email);

        // Handle potential null values for first and last name
        String firstName = current_user.getUser().getFirstName();
        String lastName = current_user.getUser().getLastName();
        String email = current_user.getUser().getEmail();
        String fullName = "";
        if(email == null) email = "";
        if(firstName != null) fullName += firstName;
        if(lastName != null) fullName += " " + lastName;

        nav_header.setText(fullName.trim());
        nav_header_email.setText(email.trim());

        //navigation menu
        Menu menu = nav.getMenu();
        // To profile page
        menu.findItem(R.id.nav_profile).setOnMenuItemClickListener(v ->{

            Intent toProfile = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(toProfile);
            return false;
        });
        // To Schedule History page
        menu.findItem(R.id.nav_schedule).setOnMenuItemClickListener(v ->{
            Intent toHistory = new Intent(MainActivity.this, ScheduleHistoryActivity.class);
            startActivity(toHistory);
            return false;
        });
        // To Notifications page
        menu.findItem(R.id.nav_notifications).setOnMenuItemClickListener(v ->{
            Intent toNotifications = new Intent(MainActivity.this, NotificationsActivity.class);
            startActivity(toNotifications);
            return false;
        });
        // To payments page
        menu.findItem(R.id.nav_payments_history).setOnMenuItemClickListener(v ->{
            Intent toPayment = new Intent(MainActivity.this, PaymentGaywayActivity.class);
            startActivity(toPayment);
            return false;
        });
        // Logout
        menu.findItem(R.id.nav_logout).setOnMenuItemClickListener(v ->{
            current_user.logOut();
            current_user.message = "Successfully logged out";
            Intent toLogin = new Intent(MainActivity.this, LogInActivity.class);
            startActivity(toLogin);
            finish();
            return false;
        });

        /* ---- End - side navigation section */


        /* ---- Start - Bottom Navigation Section */

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.getMenu().findItem(R.id.navigation_tutor_center).setVisible(current_user.getUser().isTutor());

        // Set the listener for item selection
        bottomNavigationView.setOnItemSelectedListener(navListener);

        // Load the default fragment (e.g., Dashboard) when the activity starts
        // Use commitNow() for the initial fragment if possible, or commit() otherwise
        if (savedInstanceState == null) { // Load default only on initial creation
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
            // Optionally, set the Dashboard item as selected
            bottomNavigationView.setSelectedItemId(R.id.navigation_dashboard);
        }

        flash = findViewById(R.id.txtError);
        // changes from other activities
        if(current_user.message != null){
            dataManager.displayError(flash, flash, current_user.message);
            current_user.message = null;
        }

        /* ---- End - Bottom Navigation Section */

    }

    // Listener for BottomNavigationView item selections
    private final NavigationBarView.OnItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId(); // Get item ID once

                // Determine which fragment to load based on the selected item ID
                if (itemId == R.id.navigation_dashboard) {
                    selectedFragment = new DashboardFragment();
                } else if (itemId == R.id.navigation_explorer) {
                    selectedFragment = new ExplorerFragment();
                } else if (itemId == R.id.navigation_resources) {
                    selectedFragment = new ResourcesFragment();
                } else if (itemId == R.id.navigation_tutor_center) {
                    selectedFragment = new TutorDashboardFragment();
                }
                // Replace the current fragment with the selected one
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            // Optional: Add to back stack if you want back navigation between fragments
//                             .addToBackStack(null)
                            .commit();
                    return true; // Indicate successful handling
                }

                return false; // Indicate item selection was not handled
            };
}