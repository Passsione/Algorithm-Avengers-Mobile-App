package com.pbdvmobile.app.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.UserSubject;

import java.util.Date;
import java.util.List;

public class SessionDetailsFragment extends Fragment {

    TextView subjectTitle, txtStatus, dateView, time, location, tutorName, tutorSubject;
    Button viewProfile, cancelSession, rescheduleSession, confirmSession;
    ImageView tutorImage;
    RatingBar tutorRating, detailsRating;
    EditText detailsReview;
    CardView cardReviewSection;
    LinearLayout layoutReviewInputArea, layoutDisplayReview;
    Button btnSubmitReview;
    User tutor;
    DataManager dataManager;
    LogInUser current_user;
    String subjects = "";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_session_details, container, false);
    }

    @SuppressLint({"SetTextI18n", "ResourceAsColor"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataManager = DataManager.getInstance(getContext());
        current_user = LogInUser.getInstance(dataManager);
        assert getArguments() != null;
        Session session = (Session) getArguments().getSerializable("session");
        tutor = dataManager.getUserDao().getUserByStudentNum(session.getTutorId());


        // ---- Start - Session Details ----
        subjectTitle = view.findViewById(R.id.session_detail_subject);
        txtStatus = view.findViewById(R.id.session_detail_status);
        dateView = view.findViewById(R.id.session_detail_date);
        time = view.findViewById(R.id.session_detail_time);
        location = view.findViewById(R.id.session_detail_location);

//        assert session != null;
        subjectTitle.setText(dataManager.getSubjectDao().getSubjectById(session.getSubjectId()).getSubjectName());

        // Populate Status (with improved styling - see point 2)
        updateStatusUI(session.getStatus());

        String[] dateTimeStart = dataManager.formatDateTime(session.getStartTime().toString());
        String[] dateTimeEnd = dataManager.formatDateTime(session.getEndTime().toString());
        dateView.setText(dateTimeStart[0]);
        time.setText(dateTimeStart[1] + " - " + dateTimeEnd[1]);
        location.setText(session.getLocation() == null ? "Unknown" : session.getLocation());

        // ---- End - Session Details ----


        // ---- Start - Tutor Details ----
        tutorName = view.findViewById(R.id.session_detail_tutor_name);
        tutorSubject = view.findViewById(R.id.session_detail_tutor_subjects);
        tutorImage = view.findViewById(R.id.session_detail_tutor_image);
        tutorRating = view.findViewById(R.id.session_detail_tutor_rating);


        /*List<UserSubject> userSubjects = dataManager.getSubjectDao().getUserSubjects(tutor.getStudentNum());
        for (UserSubject userSubject : userSubjects) {
            String sub = dataManager.getSubjectDao().getSubjectById(userSubject.getSubjectId()).getSubjectName();
            subjects += sub.split(": ")[0] + " ";
        }*/

        // Populate Tutor Details
        populateTutorDetails(session.getTutorId());

        // ---- End - Tutor Details ----

        // ---- Start - Review Section ----
        cardReviewSection = view.findViewById(R.id.card_review_section);
        if(session.getStatus() == Session.Status.COMPLETED) cardReviewSection.setVisibility(View.GONE);
        layoutReviewInputArea = view.findViewById(R.id.layout_review_input_area);
        detailsRating = view.findViewById(R.id.rabDetails);
        detailsReview = view.findViewById(R.id.redDetailsReview);

        btnSubmitReview = view.findViewById(R.id.btn_submit_review);

        configureReviewSection(session);
        // ---- End - Review Section ----

        // ---- Start - Buttons ----
        viewProfile = view.findViewById(R.id.btn_view_tutor_profile);
        cancelSession = view.findViewById(R.id.btn_cancel_booking);
        rescheduleSession = view.findViewById(R.id.btn_reschedule_session);
        confirmSession = view.findViewById(R.id.btn_confirm_booking);

        // Configure buttons based on session status
        configureActionButtons(session);

        // ---- End - Buttons ----
    }

    private void configureReviewSection(Session session) {
        // Reset visibility
//        cardReviewSection.setVisibility(session.getStatus() == Session.Status.COMPLETED ?View.GONE : View.VISIBLE); // Make sure card is visible

        // Determine whose review we are dealing with (tutor's or tutee's)
        boolean isTutee = current_user.getUser().getStudentNum() == session.getTuteeId();
        String existingReview = isTutee ? session.getTuteeReview() : session.getTutorReview();
        Double existingRating = isTutee ? session.getTuteeRating() : session.getTutorRating();


        if (existingReview != null && !existingReview.isEmpty()) {
            detailsReview.setText(existingReview);
        }
        if (existingRating != null) {
            detailsRating.setRating(existingRating.floatValue());
        } else {
            detailsRating.setRating(0); // Or hide it
        }
        btnSubmitReview.setOnClickListener(v -> submitReview(session));

    }

    private void submitReview(Session session) {
        float rating = detailsRating.getRating();
        String reviewText = detailsReview.getText().toString().trim();

        // Basic validation
        if (rating == 0) {
            Toast.makeText(getContext(), "Please provide a rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isTutee = current_user.getUser().getStudentNum() == session.getTuteeId();
        boolean success;

        if (isTutee) {
            session.setTuteeRating((double) rating);
            session.setTuteeReview(reviewText);
            // Update in DB
            dataManager.getSessionDao().updateTuteeRating(session.getId(), rating);
            success = dataManager.getSessionDao().addTuteeReview(session.getId(), reviewText) > 0;
        } else { // Current user is Tutor
            session.setTutorRating((double) rating);
            session.setTutorReview(reviewText);
            // Update in DB
            dataManager.getSessionDao().updateTutorRating(session.getId(), rating);
            success = dataManager.getSessionDao().addTutorReview(session.getId(), reviewText) > 0;
        }

        if (success) {
            dataManager.displayError(getString(R.string.review_submitted_toast));
            // Update UI to show the submitted review and hide input fields
            configureReviewSection(session); // Re-call to refresh the review section
        } else {
            dataManager.displayError("Failed to submit review. Please try again.");
        }
    }
    private void configureActionButtons(Session session) {

    }

    private void populateTutorDetails(int tutorId) {

        User tutor = dataManager.getUserDao().getUserByStudentNum(tutorId);
        if (tutor != null) {
            tutorName.setText(String.format("%s %s", tutor.getFirstName(), tutor.getLastName()));
            tutorRating.setRating((float)tutor.getAverageRating());
            tutorSubject.setText(tutor.getEducationLevel().name()); // Replace with actual data
             Glide.with(this).load(tutor.getProfileImageUrl())
                     .placeholder(R.mipmap.ic_launcher_round)
                     .error(R.mipmap.ic_launcher_round)
                     .into(tutorImage);
//            tutorImage.setImageResource(R.mipmap.ic_launcher_round); // Placeholder
        } else {
            tutorName.setText(getString(R.string.tutor_details_unavailable));
            tutorSubject.setText("");
            tutorRating.setRating(0);
            tutorImage.setImageResource(R.mipmap.ic_launcher_round); // Default placeholder
        }
    }

    private void updateStatusUI(Session.Status status) {
        txtStatus.setText(status.name());
        txtStatus.setTextColor(status == Session.Status.CONFIRMED ?
                getResources().getColor(R.color.status_confirmed, null):
                getResources().getColor(R.color.status_pending, null));
    }
}