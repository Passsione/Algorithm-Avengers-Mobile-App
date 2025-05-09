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
import com.pbdvmobile.app.ResourceUploadActivity;
import com.pbdvmobile.app.ScheduleActivity;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import java.util.List;

public class TutorDashboardFragment extends Fragment {

    DataManager dataManager;
    LogInUser current_user;
    CardView sessionCard;
    TextView reschedule, cancel;
    Button viewSchedule;
    LinearLayout sessionLayout, subjectTitle, date, tutorLayout, actions, upcoming, requests;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tutor_dashboard, container, false);
    }
    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    // --- Step 3: Find views within the inflated hierarchy ('view') ---
    // Initialize context-dependent instances here safely
    dataManager = DataManager.getInstance(getContext()); // Use requireContext() for safety
    current_user = LogInUser.getInstance(dataManager);

    upcoming = view.findViewById(R.id.tutor_upcoming_sessions_card);
    requests = view.findViewById(R.id.requested_sessions_card);

   drawSessions();
    anyUpdate();

    }

    private void drawSessions() {
        int user_num = current_user.getUser().getStudentNum();
        List<Session> sessions = dataManager.getSessionDao().getSessionsByTutorId(user_num);

        upcoming.removeAllViews();
        requests.removeAllViews();
        sessions.sort((n1, n2) -> n1.getStartTime().compareTo(n2.getStartTime()));

        for(Session session : sessions){
            if(session.getStatus() == Session.Status.CANCELLED ||
                    session.getStatus() == Session.Status.COMPLETED ||
                    session.getStatus() == Session.Status.DECLINED)continue;
//            if(new Date().before(session.getStartTime())){

            sessionCard = new CardView(getContext());
            sessionCard.setCardElevation(12f);

            sessionLayout = new LinearLayout(this.getContext());
            sessionLayout.setOrientation(LinearLayout.VERTICAL);

            // Set LayoutParams for the parent (e.g., fill width, wrap height)
            LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            // ***** ADD MARGIN HERE *****
            // Add a bottom margin to create a gap between cards.
            // You can adjust the dp value (e.g., 8dp) to your preference.
            int bottomMarginInPx = dpToPx(8); // Or whatever gap you want
            parentParams.setMargins(0, 0, 0, bottomMarginInPx);

            sessionCard.setLayoutParams(parentParams);
            sessionLayout.setLayoutParams(parentParams);

            // Convert 8dp padding to pixels
            int paddingPx = dpToPx(8);
            sessionCard.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

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
            txtSubject.setText(subject.getSubjectName().split(": ")[0]);
            txtSubject.setLayoutParams(titleChildParams); // Apply weighted params

            // --- Show status ---
            TextView status = new TextView(this.getContext());
            status.setText(session.getStatus().name());
            status.setTextColor(session.getStatus() == Session.Status.CONFIRMED ? Color.GREEN : Color.DKGRAY);
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
            txtDateTime.setText(dataManager.formatDateTime(session.getStartTime().toString())[0] +
                    ", " + dataManager.formatDateTime(session.getStartTime().toString())[1]);
            txtDateTime.setLayoutParams(titleChildParams); // Apply weighted params
            date.addView(dateIcon);
            date.addView(txtDateTime);


            // --- Create Tutee title ---
            tutorLayout = new LinearLayout(this.getContext());
            tutorLayout.setOrientation(LinearLayout.HORIZONTAL);
            tutorLayout.setLayoutParams(titleParams);
            // --- Icon ---
            ImageView tuteeIcon = new ImageView(this.getContext());
            tuteeIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
            tuteeIcon.setLayoutParams(iconParams);

            // --- Show Tutee name ---
            TextView txtTutee = new TextView(this.getContext());
            User tutee = dataManager.getUserDao().getUserByStudentNum(session.getTuteeId());
            txtTutee.setText(tutee.getFirstName() + " " + tutee.getLastName());
            txtTutee.setLayoutParams(titleChildParams);
            tutorLayout.addView(tuteeIcon);
            tutorLayout.addView(txtTutee);


            // --- Create Actions title ---
            actions = new LinearLayout(this.getContext());
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setLayoutParams(titleParams);

            // --- Create Reschedule Button ---
    /*    reschedule = new TextView(this.getContext());
        reschedule.setText("Reschedule");
        reschedule.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); // Set size in SP
        reschedule.setTextColor(getResources().getColor(R.color.primary, null));
        reschedule.setLayoutParams(titleChildParams);
        reschedule.setOnClickListener(v ->  {
            Intent scheduleIntent = new Intent(getActivity(), ScheduleActivity.class);
            scheduleIntent.putExtra("job_type", "session_details");
            scheduleIntent.putExtra("session", session);
            startActivity(scheduleIntent);
        });*/

            // --- Create Cancel Button ---
            cancel = new TextView(this.getContext());
            cancel.setText(session.getStatus() == Session.Status.PENDING ? "Decline" : "Cancel");
            cancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); // Set size in SP
            cancel.setTextColor(Color.RED);
            cancel.setLayoutParams(titleChildParams);
            cancel.setOnClickListener(v ->  {
                dataManager.getSessionDao().updateSessionStatus(session.getId(),
                        session.getStatus() == Session.Status.PENDING ? Session.Status.DECLINED: Session.Status.CANCELLED);
                if(session.getStatus() == Session.Status.PENDING)
                    requests.removeView(sessionCard);
                else upcoming.removeView(sessionCard);
                drawSessions();
                anyUpdate();

            });

            // --- Create View Button ---
            // Use MaterialButton to easily apply Material styles programmatically
            // Pass the style attribute directly in the constructor
            viewSchedule = new MaterialButton(this.getContext(), null, 1);
            viewSchedule.setText("View in detail");
            viewSchedule.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); // Set size in SP
            viewSchedule.setOnClickListener(v ->  {
                Intent scheduleIntent = new Intent(getActivity(), ScheduleActivity.class);
                scheduleIntent.putExtra("job_type", "session_details");
                scheduleIntent.putExtra("session", session);
                startActivity(scheduleIntent);
                drawSessions();
                anyUpdate();

            });
            //        actions.addView(reschedule);
            actions.addView(cancel);
            actions.addView(viewSchedule);

            // --- Add children to the session layout ---
            sessionLayout.addView(subjectTitle);
            sessionLayout.addView(date);
            sessionLayout.addView(tutorLayout);
            sessionLayout.addView(actions);

            sessionCard.addView(sessionLayout);
            // --- Add session to upcoming list ---
            if(session.getStatus() == Session.Status.CONFIRMED)upcoming.addView(sessionCard);
            else if(session.getStatus() == Session.Status.PENDING)requests.addView(sessionCard);

        }
    }

    private void anyUpdate() {
        if(upcoming.getChildCount() == 0){
            TextView text = new TextView(getContext());
            text.setText("No upcoming sessions");
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15); // Set size in SP
            upcoming.addView(text);
        }
        if(requests.getChildCount() == 0){
            TextView text = new TextView(getContext());
            text.setText("No requested sessions");
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15); // Set size in SP
            requests.addView(text);
        }
    }

    // Helper method to convert dp to pixels
private int dpToPx(int dp) {
    DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
    return Math.round(dp * displayMetrics.density);
}
}