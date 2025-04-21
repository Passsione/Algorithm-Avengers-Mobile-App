package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
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
        }
        flash = findViewById(R.id.txtError);

        // changes to profile
        if(current_user.signedIn != null){
            dataManager.displayError(flash, flash, current_user.signedIn);
        }


        nav = findViewById(R.id.nav_view);
        nav_header = nav.getHeaderView(0).findViewById(R.id.nav_header_name);
        nav_header_email = nav.getHeaderView(0).findViewById(R.id.nav_header_email);
        nav_header.setText(current_user.getUser().getFirstName() +" "+ current_user.getUser().getLastName());
        nav_header_email.setText(current_user.getUser().getEmail());

        Menu menu = nav.getMenu();

        menu.findItem(R.id.nav_profile).setOnMenuItemClickListener(v ->{

            Intent toProfile = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(toProfile);
            return false;
        });
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