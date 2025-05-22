/*
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
        current_user = LogInUser.getInstance();

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
}*/
package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.View;
import android.widget.Toast;
// Removed Button, ImageButton, RelativeLayout, TextView, NavigationView, exit Button imports not directly used by this version
// import android.widget.Button;
// import android.widget.ImageButton;
// import android.widget.RelativeLayout;
// import android.widget.TextView;
// import com.google.android.material.navigation.NavigationView;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
// DataManager is no longer the primary source for current user data directly
// import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser; // Your updated LogInUser
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User; // Your User POJO
import com.pbdvmobile.app.fragments.DashboardFragment;
import com.pbdvmobile.app.fragments.ExplorerFragment;
import com.pbdvmobile.app.fragments.ResourcesFragment;
import com.pbdvmobile.app.fragments.TutorDashboardFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // For logging

    private BottomNavigationView bottomNavigationView;
    // LogInUser instance for accessing cached POJO or FirebaseUser details indirectly
    private LogInUser localLogInUserInstance;
    private FirebaseAuth mAuth;
    private FirebaseFirestore dbFirestore;
    private User currentUserPojo; // To hold the fetched User object

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main); // Make sure R.id.main is your root view in activity_main.xml
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        dbFirestore = FirebaseFirestore.getInstance();
        localLogInUserInstance = LogInUser.getInstance(); // Updated getInstance()


        FirebaseUser firebaseCurrentUser = mAuth.getCurrentUser();
        if ( !localLogInUserInstance.isLoggedIn()) {
            // Not logged in, redirect to LogInActivity
            Intent toLogin = new Intent(MainActivity.this, LogInActivity.class);
            toLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(toLogin);
            finishAffinity(); // Finish this and all parent activities
            return; // Stop further execution of onCreate
        }

        // User is logged in with Firebase, now fetch their profile data from Firestore
        fetchUserProfileAndSetupUI(firebaseCurrentUser.getUid());

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(navListener);


        if (savedInstanceState == null) {
            // Load default fragment only if user data is already available or will be loaded by fragment
            // It's better to load default fragment *after* essential user data (like role) is fetched
            // For now, let's assume DashboardFragment handles its own data loading.
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mainview_fragment_container, new DashboardFragment())
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.navigation_dashboard);
        }
    }

    private void fetchUserProfileAndSetupUI(String uid) {
        Log.d(TAG, "Fetching user profile for UID: " + uid);
        dbFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserPojo = documentSnapshot.toObject(User.class);
                        if (currentUserPojo != null) {
                            currentUserPojo.setUid(uid); // Ensure UID is set on the POJO

                            localLogInUserInstance.setUser(currentUserPojo); // Update the singleton
                            Log.d(TAG, "User profile fetched: " + currentUserPojo.getFirstName());
                            // Now setup UI elements that depend on user role
                            setupNavigationBasedOnRole();
                        } else {
                            Log.e(TAG, "Failed to parse user document into User POJO.");
                            handleCriticalDataLoadFailure();
                        }
                    } else {
                        Log.e(TAG, "User document does not exist in Firestore for UID: " + uid);
                        // This is a critical error state: Firebase Auth user exists, but no profile data.
                        // Should ideally not happen if SignUpActivity correctly creates the Firestore document.
                        handleCriticalDataLoadFailure();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user profile from Firestore", e);
                    handleCriticalDataLoadFailure();
                });
    }

    private void handleCriticalDataLoadFailure() {
        Toast.makeText(this, "Error loading user profile. Logging out.", Toast.LENGTH_LONG).show();
        mAuth.signOut(); // Log out the Firebase Auth user
        localLogInUserInstance.logOut(); // Clear local state
        Intent toLogin = new Intent(MainActivity.this, LogInActivity.class);
        toLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(toLogin);
        finishAffinity();
    }


    private void setupNavigationBasedOnRole() {
        if (currentUserPojo != null && bottomNavigationView != null) {
            bottomNavigationView.getMenu().findItem(R.id.navigation_tutor_center).setVisible(currentUserPojo.isTutor());
            // Also, the FAB visibility in navListener now can reliably use currentUserPojo
        }
    }

    private final NavigationBarView.OnItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();
                FloatingActionButton fab = findViewById(R.id.dashboard_add_resources); // Ensure this ID exists in activity_main.xml
                setupNavigationBasedOnRole();

                // FAB click listener (should only be set up once or managed carefully)
                // If FAB is part of fragment layouts, it's better to handle its click listener there.
                // If it's part of MainActivity layout, this is okay.
                if (fab != null) {
                    fab.setOnClickListener( l ->{
                        if (currentUserPojo != null && currentUserPojo.isTutor()) { // Check if current user is tutor
                            Intent toResourceUpload = new Intent(MainActivity.this, ResourceUploadActivity.class);
                            toResourceUpload.putExtra(ResourceUploadActivity.EXTRA_MODE, ResourceUploadActivity.MODE_UPLOAD);
                            startActivity(toResourceUpload);
                        } else {
                            Toast.makeText(MainActivity.this, "Only tutors can upload resources.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }


                if (itemId == R.id.navigation_dashboard) {
                    selectedFragment = new DashboardFragment();
                    if (fab != null) fab.setVisibility(GONE);
                } else if (itemId == R.id.navigation_explorer) {
                    selectedFragment = new ExplorerFragment();
                    if (fab != null) fab.setVisibility(GONE);
                } else if (itemId == R.id.navigation_resources) {
                    selectedFragment = new ResourcesFragment();
                    if (fab != null) {
                        // Use the fetched currentUserPojo to determine FAB visibility
                        fab.setVisibility(currentUserPojo != null && currentUserPojo.isTutor() ? VISIBLE : GONE);
                    }
                } else if (itemId == R.id.navigation_tutor_center) {
                    // This item itself should only be visible if currentUserPojo.isTutor() is true
                    if (currentUserPojo != null && currentUserPojo.isTutor()) {
                        selectedFragment = new TutorDashboardFragment();
                        if (fab != null) fab.setVisibility(VISIBLE); // Example: Tutor center always has FAB action
                    } else {
                        // User somehow clicked a non-visible item, or role changed.
                        // Fallback to dashboard or show a message.
                        selectedFragment = new DashboardFragment();
                        if (fab != null) fab.setVisibility(GONE);
                        Toast.makeText(this, "Access denied.", Toast.LENGTH_SHORT).show();
                    }
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.mainview_fragment_container, selectedFragment) // Ensure this ID is in activity_main.xml
                            .commit();
                    return true;
                }
                return false;
            };
}
