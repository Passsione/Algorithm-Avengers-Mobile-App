package com.pbdvmobile.app.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pbdvmobile.app.R;
import com.pbdvmobile.app.TutorProfile;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.Schedule.TimeSlot;
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.UserSubject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SessionBookingsFragment extends Fragment {

    DataManager dataManager;
    LogInUser current_user;
    TextView tutorName, tutorSubjects, location;
    RatingBar tutorRating;
    ImageView tutorProfileImage;
    Button viewProfile, cancel, submit;
    RadioGroup rbgSubjects, time, duration;
    CalendarView calendarView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_session_bookings, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataManager = DataManager.getInstance(getContext());
        current_user = LogInUser.getInstance(dataManager);
        assert getArguments() != null;
        User tutor = (User) getArguments().getSerializable("tutor");
        String subjects = getArguments().getString("subjects");



        tutorSubjects = view.findViewById(R.id.session_booking_tutor_subjects);
        tutorName = view.findViewById(R.id.session_booking_tutor_name);
        tutorProfileImage = view.findViewById(R.id.session_booking_tutor_image);
        tutorRating = view.findViewById(R.id.session_booking_tutor_rating);
        viewProfile = view.findViewById(R.id.booking_tutor_profile);

        rbgSubjects = view.findViewById(R.id.subjects_radio_group);
        time = view.findViewById(R.id.radio_group_time);
        duration = view.findViewById(R.id.radio_group_duration);
        location = view.findViewById(R.id.edt_location);

        cancel = view.findViewById(R.id.btn_cancel_booking);
        submit = view.findViewById(R.id.btn_confirm_booking);

        calendarView = view.findViewById(R.id.calendar);
        calendarView.setMinDate(System.currentTimeMillis());


        // ---- Start - Tutor Info ----
        tutorName.setText(tutor.getFirstName() + " " + tutor.getLastName());
        tutorSubjects.setText(subjects);
        if(tutor.getAverageRating() != -1)
            tutorRating.setRating((float)tutor.getAverageRating());
        else {
            tutorRating.setVisibility(View.GONE);
            TextView not_rating = new TextView(getContext());
            not_rating.setText("Not rated yet");
            not_rating.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            LinearLayout detailsLayout = view.findViewById(R.id.session_booking_tutor_details);
            detailsLayout.addView(not_rating);
        }
        viewProfile.setOnClickListener(l ->{
            Intent toProfile = new Intent(getContext(), TutorProfile.class);
            toProfile.putExtra("tutor", tutor);
            startActivity(toProfile);
        });
        // ---- End - Tutor Info ----


        // ---- Start - Subject section ----

        List<UserSubject> tutorSubjects = dataManager.getSubjectDao().getUserSubjects(tutor.getStudentNum());
        List<UserSubject> tuteeSubjects = dataManager.getSubjectDao().getUserSubjects(current_user.getUser().getStudentNum());
        AtomicInteger subject = new AtomicInteger();
        for(UserSubject tutorSubject : tutorSubjects) {
            // Is the tutor tutoring this subject?
            if(!tutorSubject.getTutoring())continue;
            for(UserSubject tuteeSubject : tuteeSubjects) {
                // Is this subject one of the tutees subjects?
                if(tutorSubject.getSubjectId() == tuteeSubject.getSubjectId()){
                    RadioButton radioButton = new RadioButton(getContext());
                    radioButton.setText(dataManager.getSubjectDao().getSubjectById(tutorSubject.getSubjectId()).getSubjectName());
                    radioButton.setOnClickListener(l ->{
                        subject.set(tutorSubject.getSubjectId());
                    });
                    rbgSubjects.addView(radioButton);
                }
            }
        }
        // ---- End - Subject section ----


        // ---- Start - Calendar ----
        Date requestedDate = new Date(new Date().getTime() - 1000 * 60 * 60 * 24);
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);

                view.setDate(calendar.getTimeInMillis());
                requestedDate.setTime(view.getDate());
            }
        });
        // ---- End - Calender ----


        // ---- Start - Time section ----
       AtomicLong chosenTime = new AtomicLong();
        List<Session> taken = dataManager.getSessionDao().getSessionsByTutorId(tutor.getStudentNum());
        long interval = dataManager.getSessionDao().hourToMseconds(2);

        if(taken == null || taken.isEmpty()) {
            for (long i = dataManager.getSessionDao().OPEN_TIME; i < dataManager.getSessionDao().CLOSE_TIME; i += interval) {
                RadioButton radioButton = new RadioButton(getContext());
                Date date = new Date(i);
                radioButton.setText(dataManager.formatDateTime(date.toString())[1]);
                long finalI = i;
                radioButton.setOnClickListener(l -> {
                    chosenTime.set(finalI);
                });
                time.addView(radioButton);
            }
        }else{
            for (long loop = dataManager.getSessionDao().OPEN_TIME; loop < dataManager.getSessionDao().CLOSE_TIME; loop += interval) {

                Date sT = new Date(loop);
                Date eT = new Date(loop + interval);
                TimeSlot timeslot = new TimeSlot(sT, eT);
                if(loop <= taken.get(taken.size() - 1).getEndTime().getTime())
                    for(int i = 0; i < taken.size(); i++){
                        Session session = taken.get(i);
                        if(session.getStartTime().before(new Date()) || session.getStatus() == Session.Status.PENDING ||
                                session.getStatus() == Session.Status.CONFIRMED)continue;

                        TimeSlot other = new TimeSlot(session.getStartTime(), session.getEndTime());

                        if(!timeslot.overlaps(other)) {
                            RadioButton radioButton = new RadioButton(getContext());
                            radioButton.setText( dataManager.formatDateTime(sT.toString())[1]);
                            long finalI = loop;
                            radioButton.setOnClickListener(l ->{
                                chosenTime.set(finalI);
                            });
                            time.addView(radioButton);
                            break;
                        }
                    }
                else{
                    RadioButton radioButton = new RadioButton(getContext());
                    radioButton.setText( dataManager.formatDateTime(sT.toString())[1]);
                    long finalI = loop;
                    radioButton.setOnClickListener(l ->{
                        chosenTime.set(finalI);
                    });
                    time.addView(radioButton);
                }

            }

        }
        // ---- End - Time section ----



        // ---- Start - Duration section ----
        AtomicLong length = new AtomicLong();
        for(int i = 0; i < 3; i++ ){
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setText(i > 0 ? (i == 1? i + " hour" : i + " hours") : "30 minutes");
            int finalI = i;
            radioButton.setOnClickListener(l ->{
                length.set(finalI > 0 ? dataManager.getSessionDao().hourToMseconds(finalI) : dataManager.getSessionDao().minutesToMseconds(30));
            });
            duration.addView(radioButton);
        }
        // ---- End - Duration section ----



        // ---- Start - Location section ----

        // ---- End - Location section ----




        // ---- Start - Buttons ----
        cancel.setOnClickListener(l ->{
            getActivity().finish();
        });

        submit.setOnClickListener(l ->{

            if(subject.get() == 0){
                Toast.makeText(getContext(), "Please select a subject", Toast.LENGTH_LONG).show();
                return;
            }if(requestedDate.before(new Date())){
                Toast.makeText(getContext(), "Please select a date", Toast.LENGTH_LONG).show();
                return;
            }if(chosenTime.get() == 0){
                Toast.makeText(getContext(), "Please select a time", Toast.LENGTH_LONG).show();
                return;
            }if(length.get() == 0) {
                Toast.makeText(getContext(), "Please select a duration", Toast.LENGTH_LONG).show();
                return;
            }
            if(location.getText().toString().isEmpty()) {
                Toast.makeText(getContext(), "Please select a location", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(getContext(), subject.get()+","+ requestedDate+","+chosenTime+","+length.get()+","+location.getText().toString(), Toast.LENGTH_LONG).show();

            Date startTime = new Date(requestedDate.getTime() + chosenTime.get());
            Date endTime = new Date(startTime.getTime() + length.get());
          /*  if(dataManager.getSessionDao().requestSession(tutor.getStudentNum(),
                    current_user.getUser().getStudentNum(),
                    new TimeSlot(startTime, endTime),
                    subject.get(), location.getText().toString()))
                Toast.makeText(getContext(), "Session request created successful!", Toast.LENGTH_LONG).show();

            else{
                Toast.makeText(getContext(), "Session request failed!", Toast.LENGTH_LONG).show();
            }*/
        });

    }
}