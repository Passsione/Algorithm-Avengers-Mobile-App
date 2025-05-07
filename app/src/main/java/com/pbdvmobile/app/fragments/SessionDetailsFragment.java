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

import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;

public class SessionDetailsFragment extends Fragment {

    TextView subjectTitle, status, dateView, time, location, tutorName, tutorSubject;
    Button viewProfile, cancelSession, rescheduleSession, confirmSession;
    ImageView tutorImage;
    RatingBar tutorRating, detailsRating;
    LinearLayout reviewSection;
    CardView tutorCard;
    EditText detailsReview;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_session_details, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DataManager dataManager = DataManager.getInstance(getContext());
        LogInUser current_user = LogInUser.getInstance(dataManager);
        assert getArguments() != null;
        User tutor = (User) getArguments().getSerializable("tutor");
        String subjects = (String) getArguments().get("subjects");
        Session session = (Session) getArguments().getSerializable("session");


        // ---- Start - Session Details ----
        subjectTitle = view.findViewById(R.id.session_detail_subject);
        status = view.findViewById(R.id.session_detail_status);
        dateView = view.findViewById(R.id.session_detail_date);
        time = view.findViewById(R.id.session_detail_time);
        location = view.findViewById(R.id.session_detail_location);
        tutorName = view.findViewById(R.id.session_detail_tutor_name);
        tutorSubject = view.findViewById(R.id.session_detail_tutor_subjects);

//        assert session != null;
        subjectTitle.setText(dataManager.getSubjectDao().getSubjectById(session.getSubjectId()).getSubjectName());
        status.setText(session.getStatus().name());
        String[] dateTime = dataManager.formatDateTime(session.getStartTime().toString());
        dateView.setText(dateTime[0]);
        time.setText(dateTime[1]);
        location.setText(session.getLocation() == null ? "Unknown" : session.getLocation());

        // ---- End - Session Details ----

        // ---- Start - Tutor Details ----
        tutorCard = view.findViewById(R.id.session_detail_tutor_card);
        tutorCard.setVisibility(session.getTutorId() == current_user.getUser().getStudentNum()? View.GONE : View.VISIBLE);
        tutorName.setText(tutor.getFirstName() + " " + tutor.getLastName());
        tutorSubject.setText(subjects);
        tutorImage = view.findViewById(R.id.session_detail_tutor_image);
        tutorRating = view.findViewById(R.id.session_detail_tutor_rating);
        if(tutor.getAverageRating() == -1){
            tutorRating.setVisibility(View.GONE);
            view.findViewById(R.id.session_detail_no_rating).setVisibility(View.VISIBLE);
        }
        detailsRating = view.findViewById(R.id.rabDetails);
        detailsReview = view.findViewById(R.id.redDetailsReview);


        // ---- End - Tutor Details ----

        // ---- Start - Review Section ----
        reviewSection = view.findViewById(R.id.cardReview);

        reviewSection.setVisibility(session.getStatus() == Session.Status.COMPLETED ? View.VISIBLE : View.GONE);

        // ---- End - Review Section ----

        // Button Listeners
        viewProfile = view.findViewById(R.id.btn_view_tutor_profile);
        cancelSession = view.findViewById(R.id.btn_cancel_booking);
//        rescheduleSession = view.findViewById(R.id.btn_reschedule_session);
        confirmSession = view.findViewById(R.id.btn_confirm_booking);





    }
}