package com.pbdvmobile.app;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.fragments.ScheduleHistoryFragment;
import com.pbdvmobile.app.fragments.SessionBookingsFragment;
import com.pbdvmobile.app.fragments.SessionDetailsFragment;

public class ScheduleActivity extends AppCompatActivity {

    ImageView imageView;
    TextView tutorName, subjects;
    RatingBar ratingBar;
    Button viewProfile, cancelSession, sumbmit;
    CalendarView calendarView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_schedule);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        User tutor = (User) getIntent().getSerializableExtra("tutor");
        LogInUser current_user = (LogInUser) getIntent().getSerializableExtra("current_user");
        Session session = (Session) getIntent().getSerializableExtra("session");
        String job_type = getIntent().getStringExtra("job_type");


        Fragment selectedFragment = null;
        assert job_type != null;
        Bundle bundle = new Bundle();
        switch (job_type) {
            case "create_session":
                selectedFragment = new SessionBookingsFragment();
                break;
            case "session_details":
                selectedFragment = new SessionDetailsFragment();
                bundle.putSerializable("session", session);
                break;
            case "session_history":
                selectedFragment = new ScheduleHistoryFragment();
                break;
        }
        // Replace the current fragment with the selected one
        if (selectedFragment != null) {

            bundle.putSerializable("tutor", tutor);
            bundle.putSerializable("current_user", current_user);
            selectedFragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.session_fragment_container, selectedFragment)
                    // Optional: Add to back stack if you want back navigation between fragments
//                             .addToBackStack(null)
                    .commit();

        }
    }
}