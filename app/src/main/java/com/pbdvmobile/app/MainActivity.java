package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.pbdvmobile.app.adapters.DashboardAdapter;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    RelativeLayout main;
    TabLayout tablayout;
    ViewPager2 viewpager2;
    ImageButton menuswitch;
    NavigationView nav;
    TextView nav_header, nav_header_email, flash;
    DashboardAdapter dashboardAdapter;

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

        // Go to log-in page
        if(!current_user.isLoggedIn()){
            Intent toLogin = new Intent(MainActivity.this, LogInActivity.class);
            startActivity(toLogin);
            finish(); // can't come back to main activity
            return;

        }

        flash = findViewById(R.id.txtError);

        // changes from other activities
        if(current_user.message != null){
            dataManager.displayError(flash, flash, current_user.message);
            current_user.message = null;
        }

      // navigation
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
        // To payments page
        menu.findItem(R.id.nav_payments).setOnMenuItemClickListener(v ->{
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

        // Main view
        menuswitch = findViewById(R.id.imgMenu);
        tablayout = findViewById(R.id.tab_layout);
        viewpager2 = findViewById(R.id.view_pager);
        dashboardAdapter = new DashboardAdapter(this);
        viewpager2.setAdapter(dashboardAdapter);

        main = findViewById(R.id.main);
        main.setOnClickListener(v -> {
            flash = findViewById(R.id.txtError);
            nav.setVisibility(GONE);
        });
        menuswitch.setOnClickListener(v -> nav.setVisibility(VISIBLE));

       tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                viewpager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        viewpager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Objects.requireNonNull(tablayout.getTabAt(position)).select();
            }
        });
    }
}