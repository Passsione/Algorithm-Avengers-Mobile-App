package com.pbdvmobile.app.fragments;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.ScheduleActivity;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import java.util.List;

public class DashboardFragment extends Fragment {

    TextView reschedule, cancel;
    Button viewSchedule;
    CardView upcoming, pending;
    LinearLayout sessionLayout, subjectTitle, date, tutorLayout, actions;
    DataManager dataManager;
    LogInUser current_user;
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Step 3: Find views within the inflated hierarchy ('view') ---
        // Initialize context-dependent instances here safely
        dataManager = DataManager.getInstance(getContext()); // Use requireContext() for safety
        current_user = LogInUser.getInstance(dataManager);

        upcoming = view.findViewById(R.id.upcoming_sessions_card);
        pending = view.findViewById(R.id.pending_sessions_card);

        int user_num = current_user.getUser().getStudentNum();
        List<Session> sessions = dataManager.getSessionDao().getSessionsByTuteeId(user_num);

        for(Session session : sessions){

            if(session.getStatus() == Session.Status.CANCELLED ||
                    session.getStatus() == Session.Status.COMPLETED ||
                    session.getStatus() == Session.Status.DECLINED)continue;
//            if(new Date().before(session.getStartTime())){

            sessionLayout = new LinearLayout(this.getContext());
            sessionLayout.setOrientation(LinearLayout.VERTICAL);

            // Set LayoutParams for the parent (e.g., fill width, wrap height)
            LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            sessionLayout.setLayoutParams(parentParams);

            // Convert 8dp padding to pixels
            int paddingPx = dpToPx(8);
            sessionLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

            // --- Define LayoutParams for title
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, //
                    LinearLayout.LayoutParams.WRAP_CONTENT, // Height = wrap_content
                    1.0f // Weight = 1
            );
            // --- Define LayoutParams for children in title ---
            LinearLayout.LayoutParams titleChildParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f // Weight = 1
            );
            // --- Define LayoutParams for icon ---
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    dpToPx(24), // Width = 24dp,
                    dpToPx(24) // height = 24dp,
            );


            // --- Create Subject title ---
            subjectTitle = new LinearLayout(this.getContext());
            subjectTitle.setOrientation(LinearLayout.HORIZONTAL);
            subjectTitle.setLayoutParams(titleParams);
            // --- Show subject name ---
            TextView txtSubject = new TextView(this.getContext());
            Subject subject = dataManager.getSubjectDao().getSubjectById(session.getSubjectId());
            txtSubject.setText(subject.getSubjectName());
            txtSubject.setLayoutParams(titleChildParams); // Apply weighted params
            // --- Show status ---
            TextView status = new TextView(this.getContext());
            status.setText(session.getStatus().name());
            status.setTextColor(Color.GREEN);
            subjectTitle.addView(txtSubject);
            subjectTitle.addView(status);


            // --- Create Date title ---
            date = new LinearLayout(this.getContext());
            date.setOrientation(LinearLayout.HORIZONTAL);
            date.setLayoutParams(titleParams);
            // --- Icon ---
            ImageView dateIcon = new ImageView(this.getContext());
            dateIcon.setImageResource(android.R.drawable.ic_menu_my_calendar);
            dateIcon.setLayoutParams(iconParams);
            // --- Show Date & Time ---
            TextView txtDateTime = new TextView(this.getContext());
            txtDateTime.setText(session.getStartTime().toLocaleString());
            txtDateTime.setLayoutParams(titleChildParams); // Apply weighted params
            date.addView(dateIcon);
            date.addView(txtDateTime);


            // --- Create Tutor title ---
            tutorLayout = new LinearLayout(this.getContext());
            tutorLayout.setOrientation(LinearLayout.HORIZONTAL);
            tutorLayout.setLayoutParams(titleParams);
            // --- Icon ---
            ImageView tutorIcon = new ImageView(this.getContext());
            tutorIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
            tutorIcon.setLayoutParams(iconParams);
            // --- Show Tutor name ---
            TextView txtTutor = new TextView(this.getContext());
            User tutor = dataManager.getUserDao().getUserByStudentNum(session.getTutorId());
            txtTutor.setText(tutor.getFirstName() + " " + tutor.getLastName());
            txtTutor.setLayoutParams(titleChildParams);
            tutorLayout.addView(tutorIcon);
            tutorLayout.addView(txtTutor);


            // --- Create Actions title ---
            actions = new LinearLayout(this.getContext());
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setLayoutParams(titleParams);
            // --- Create Reschedule Button ---
            reschedule = new TextView(this.getContext());
            reschedule.setText("Reschedule");
            reschedule.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); // Set size in SP
            reschedule.setTextColor(getResources().getColor(R.color.primary));
            reschedule.setLayoutParams(titleChildParams);
            reschedule.setOnClickListener(v ->  {

            });
            // --- Create Cancel Button ---
            cancel = new TextView(this.getContext());
            cancel.setText("Cancel");
            cancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); // Set size in SP
            cancel.setTextColor(Color.RED);
            cancel.setLayoutParams(titleChildParams);
            cancel.setOnClickListener(v ->  {

            });
            // --- Create View Button ---
            // Use MaterialButton to easily apply Material styles programmatically
            // Pass the style attribute directly in the constructor
            viewSchedule = new MaterialButton(this.getContext(), null, 1);
            viewSchedule.setText("View");
            viewSchedule.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); // Set size in SP
            viewSchedule.setOnClickListener(v ->  {
                Intent scheduleIntent = new Intent(getActivity(), ScheduleActivity.class);
                scheduleIntent.putExtra("job_type", "session_details");
                scheduleIntent.putExtra("session", session);
                startActivity(scheduleIntent);
            });
            actions.addView(reschedule);
            actions.addView(cancel);
            actions.addView(viewSchedule);

            // --- Add children to the session layout ---
            sessionLayout.addView(subjectTitle);
            sessionLayout.addView(date);
            sessionLayout.addView(tutorLayout);
            sessionLayout.addView(actions);

            // --- Add session to upcoming list ---
            if(session.getStatus() == Session.Status.CONFIRMED)upcoming.addView(sessionLayout);
            else if(session.getStatus() == Session.Status.PENDING)pending.addView(sessionLayout);
//            }
        }

    }
    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
        // A simpler way to convert dp to pixels
        return Math.round(dp * displayMetrics.density);
    }
}