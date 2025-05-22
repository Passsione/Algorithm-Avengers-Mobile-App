package com.pbdvmobile.app.fragments;

import static android.widget.LinearLayout.VERTICAL;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.ScheduleActivity;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TutorDashboardFragment extends Fragment {

    private static final String TAG = "TutorDashboardFragment";
    private LinearLayout upcomingSessionsLayout, requestedSessionsLayout;
    private ProgressBar progressBarTutorDashboard;
    private TextView textViewNoTutorUpcoming, textViewNoTutorRequests;

    private LogInUser loggedInUser;
    private User currentTutorPojo;
    private SessionDao sessionDao;
    private UserDao userDao;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutor_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null) return;

        loggedInUser = LogInUser.getInstance();
        currentTutorPojo = loggedInUser.getUser();
        sessionDao = new SessionDao();
        userDao = new UserDao();

        upcomingSessionsLayout = view.findViewById(R.id.tutor_upcoming_sessions_card);
        requestedSessionsLayout = view.findViewById(R.id.tutor_requested_sessions_card);
        progressBarTutorDashboard = view.findViewById(R.id.progressBarTutorDashboard); 
        textViewNoTutorUpcoming = view.findViewById(R.id.textViewNoTutorUpcoming); 
        textViewNoTutorRequests = view.findViewById(R.id.textViewNoTutorRequests);   

        if (currentTutorPojo == null || !currentTutorPojo.isTutor()) {
            Toast.makeText(getContext(), "User data not available or not a tutor.", Toast.LENGTH_LONG).show();
            // Handle UI for non-tutors or error state
            return;
        }
        loadTutorSessions();
    }

    private void showLoading(boolean isLoading) {
        if (progressBarTutorDashboard != null) progressBarTutorDashboard.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (upcomingSessionsLayout != null) upcomingSessionsLayout.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        if (requestedSessionsLayout != null) requestedSessionsLayout.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        if (textViewNoTutorUpcoming != null && isLoading) textViewNoTutorUpcoming.setVisibility(View.GONE);
        if (textViewNoTutorRequests != null && isLoading) textViewNoTutorRequests.setVisibility(View.GONE);
    }

    private void loadTutorSessions() {
        showLoading(true);
        sessionDao.getSessionsByTutorUid(currentTutorPojo.getUid(), new SessionDao.SessionsCallback() {
            @Override
            public void onSessionsFetched(List<Session> sessions) {
                processAndDisplayTutorSessions(sessions);
                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching tutor sessions", e);
                Toast.makeText(getContext(), "Could not load sessions.", Toast.LENGTH_SHORT).show();
                updateNoSessionsViews(true, true);
                showLoading(false);
            }
        });
    }

    private void processAndDisplayTutorSessions(List<Session> sessions) {
        List<Session> upcoming = new ArrayList<>();
        List<Session> requests = new ArrayList<>(); // PENDING sessions are requests for the tutor
        Date now = new Date();

        for (Session session : sessions) {
            if (session.getStatus() == Session.Status.CONFIRMED &&
                    (session.getEndTime() == null || !session.getEndTime().toDate().before(now))) {
                upcoming.add(session);
            } else if (session.getStatus() == Session.Status.PENDING &&
                    (session.getEndTime() == null || !session.getEndTime().toDate().before(now))) {
                requests.add(session);
            }
        }

        Collections.sort(upcoming, (s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime()));
        Collections.sort(requests, (s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime()));

        drawTutorSessionCards(upcomingSessionsLayout, upcoming, "upcoming");
        drawTutorSessionCards(requestedSessionsLayout, requests, "requests");

        updateNoSessionsViews(upcoming.isEmpty(), requests.isEmpty());
    }

    private void updateNoSessionsViews(boolean noUpcoming, boolean noRequests) {
        if (textViewNoTutorUpcoming != null) textViewNoTutorUpcoming.setVisibility(noUpcoming ? View.VISIBLE : View.GONE);
        if (textViewNoTutorRequests != null) textViewNoTutorRequests.setVisibility(noRequests ? View.VISIBLE : View.GONE);
    }

    @SuppressLint("SetTextI18n")
    private void drawTutorSessionCards(LinearLayout layout, List<Session> sessions, String type) {
        layout.removeAllViews();
        if (getContext() == null) return;

        if (sessions.isEmpty()) {
            // updateNoSessionsViews will handle the "No sessions" text
            return;
        }

        for (Session session : sessions) {
            CardView sessionCard = new CardView(getContext());
            // CardView styling
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int marginPx = dpToPx(8);
            cardParams.setMargins(marginPx, marginPx, marginPx, marginPx);
            sessionCard.setLayoutParams(cardParams);
            sessionCard.setCardElevation(dpToPx(4));
            sessionCard.setRadius(dpToPx(8));
            sessionCard.setUseCompatPadding(true);

            LinearLayout cardContentLayout = new LinearLayout(getContext());
            cardContentLayout.setOrientation(LinearLayout.VERTICAL);
            int paddingPx = dpToPx(12);
            cardContentLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

            // Subject and Status
            LinearLayout subjectStatusLayout = new LinearLayout(getContext());
            subjectStatusLayout.setOrientation(LinearLayout.HORIZONTAL);
            TextView txtSubject = new TextView(getContext());
            txtSubject.setText(session.getSubjectName() != null ? session.getSubjectName().split(":")[0] : "N/A");
            txtSubject.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            txtSubject.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams subjectParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            txtSubject.setLayoutParams(subjectParams);

            TextView txtStatus = new TextView(getContext());
            txtStatus.setText(session.getStatus().name());
            txtStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            setStatusTextColor(txtStatus, session.getStatus());

            subjectStatusLayout.addView(txtSubject);
            subjectStatusLayout.addView(txtStatus);
            cardContentLayout.addView(subjectStatusLayout);

            // Date and Time
            TextView txtDateTime = new TextView(getContext());
            if (session.getStartTime() != null && session.getEndTime() != null) {
                txtDateTime.setText(
                        dateFormat.format(session.getStartTime().toDate()) + " at " +
                                timeFormat.format(session.getStartTime().toDate()) + " - " +
                                timeFormat.format(session.getEndTime().toDate())
                );
            } else {
                txtDateTime.setText("Date/Time not set");
            }
            txtDateTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            cardContentLayout.addView(txtDateTime);


            // Partner (Tutee for tutor's dashboard)
            LinearLayout partnerLayout = new LinearLayout(getContext());
            partnerLayout.setOrientation(VERTICAL);
            TextView txtPartner = new TextView(getContext());
            TextView partnerRating = new TextView(getContext());
            userDao.getUserByUid(session.getTuteeUid()).addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    User partner = doc.toObject(User.class);
                    if (partner != null) {
                        txtPartner.setText("With: " + partner.getFirstName() + " " + partner.getLastName());
                        partnerRating.setText(partner.getAverageRatingAsTutee() +"/ 5 Stars");

                    } else {
                        txtPartner.setText("With: Unknown Tutee");
                    }
                }
            }).addOnFailureListener(e -> txtPartner.setText("With: Error loading tutee"));
            txtPartner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            partnerLayout.addView(txtPartner);
            partnerLayout.addView(partnerRating);

            cardContentLayout.addView(partnerLayout);
            // Location
            TextView txtLocation = new TextView(getContext());
            txtLocation.setText("Location: " + (session.getLocation() != null ? session.getLocation() : "N/A"));
            txtLocation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            cardContentLayout.addView(txtLocation);

            // Action Button
            Button btnViewDetails = new Button(getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnViewDetails.setText("View Details");
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnParams.gravity = Gravity.END;
            btnParams.topMargin = dpToPx(8);
            btnViewDetails.setLayoutParams(btnParams);
            btnViewDetails.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ScheduleActivity.class);
                intent.putExtra("job_type", "session_details");
                intent.putExtra("session_id", session.getId()); // Pass session ID
                startActivity(intent);
            });
            cardContentLayout.addView(btnViewDetails);

            sessionCard.addView(cardContentLayout);
            layout.addView(sessionCard);
        }
    }

    private void setStatusTextColor(TextView textView, Session.Status status) {
        int colorResId;
        switch (status) {
            case CONFIRMED: colorResId = R.color.status_confirmed; break;
            case PENDING: colorResId = R.color.status_pending; break;
            default: colorResId = R.color.status_generic_gray; break;
        }
        textView.setTextColor(ContextCompat.getColor(getContext(), colorResId));
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(dp * displayMetrics.density);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentTutorPojo != null && currentTutorPojo.isTutor()) {
            loadTutorSessions();
        }
    }
}
