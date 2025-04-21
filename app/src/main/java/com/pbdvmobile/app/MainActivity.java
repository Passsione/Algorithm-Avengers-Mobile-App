package com.pbdvmobile.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.pbdvmobile.app.adapters.DashboardAdapter;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;

public class MainActivity extends AppCompatActivity {

    TabLayout tablayout;
    ViewPager2 viewpager2;
    DashboardAdapter dashboardAdapter;

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

        if(!current_user.isLoggedIn()){ // Go to log-in page
            Intent toLogin = new Intent(MainActivity.this, LogInActivity.class);
            startActivity(toLogin);
        }

        tablayout = findViewById(R.id.tab_layout);
        viewpager2 = findViewById(R.id.view_pager);
        dashboardAdapter = new DashboardAdapter(this);
        viewpager2.setAdapter(dashboardAdapter);

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
                tablayout.getTabAt(position).select();
            }
        });
    }
}