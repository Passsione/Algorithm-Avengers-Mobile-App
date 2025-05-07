package com.pbdvmobile.app.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class SessionBookingsFragment extends Fragment {

    /*DataManager dataManager;
    LogInUser current_user;
    TextView tutorName, tutorSubjects, location;
    RatingBar tutorRating;
    ImageView tutorProfileImage;
    Button viewProfile, cancel, submit;
    RadioGroup rbgSubjects, time, duration;
    CalendarView calendarView;
    final AtomicReference<Date> requestedDate = new AtomicReference<>();

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
// Initialize requestedDate with the CalendarView's current date (at midnight)
        Calendar initCal = Calendar.getInstance();
        initCal.setTimeInMillis(calendarView.getDate()); // Get current date from CalendarView
        initCal.set(Calendar.HOUR_OF_DAY, 0);
        initCal.set(Calendar.MINUTE, 0);
        initCal.set(Calendar.SECOND, 0);
        initCal.set(Calendar.MILLISECOND, 0);
        requestedDate.set(initCal.getTime());
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth, 0, 0, 0); // Set to midnight of the selected day
                calendar.set(Calendar.MILLISECOND, 0);
                requestedDate.set(calendar.getTime());
                // DO NOT call view.setDate() here as it might trigger the listener again.
                // The CalendarView UI already reflects the change.
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
            Date actualRequestedDate = requestedDate.get();
            if(subject.get() == 0){
                Toast.makeText(getContext(), "Please select a subject", Toast.LENGTH_LONG).show();
                return;
            }if(actualRequestedDate == null){
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

            // Validate selected date (though minDate should handle past dates)
            Calendar todayCal = Calendar.getInstance();
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0); // Midnight today

            if (actualRequestedDate.before(todayCal.getTime())) {
                Toast.makeText(getContext(), "Please select a valid date (today or in the future).", Toast.LENGTH_LONG).show();
                return;
            }

            Calendar startCalendar = Calendar.getInstance(); // Uses default (local) timezone
            startCalendar.setTime(actualRequestedDate); // actualRequestedDate should be from the calendar, e.g., May 7th 00:00:00 local

            // chosenTime.get() holds milliseconds for a time of day in UTC (e.g., 6 AM UTC if 8 AM local was chosen in GMT+2)
            // Convert this UTC time-of-day to local hour and minute
            Date timeOfDayUtcDate = new Date(chosenTime.get());
            Calendar timeOfDayLocalCal = Calendar.getInstance(); // Uses local timezone
            timeOfDayLocalCal.setTime(timeOfDayUtcDate); // Converts the UTC point-in-time to local calendar fields


            // Set these local hour and minute to the startCalendar (which is for the correct date)
            startCalendar.set(Calendar.HOUR_OF_DAY, timeOfDayLocalCal.get(Calendar.HOUR_OF_DAY));
            startCalendar.set(Calendar.MINUTE, timeOfDayLocalCal.get(Calendar.MINUTE));
            startCalendar.set(Calendar.SECOND, 0);
            startCalendar.set(Calendar.MILLISECOND, 0);

            Date startTime = startCalendar.getTime();
            Date endTime = new Date(startTime.getTime() + length.get());

            if(dataManager.getSessionDao().requestSession(current_user.getUser().getStudentNum(),
                    tutor.getStudentNum(),
                    new TimeSlot(startTime, endTime),
                    subject.get(), location.getText().toString()))
                Toast.makeText(getContext(), "Session request created successful!", Toast.LENGTH_LONG).show();

            else{
                Toast.makeText(getContext(), "Session request failed!", Toast.LENGTH_LONG).show();
            }
        });

    }
}*/

    private static final String TAG = "SessionBookingsFrag"; // Logging Tag

    // --- Views ---
    TextView tutorName, tutorSubjects, location;
    RatingBar tutorRating;
    ImageView tutorProfileImage;
    Button viewProfile, cancel, submit;
    RadioGroup rbgSubjects, time, duration;
    CalendarView calendarView;

    // --- Data ---
    DataManager dataManager;
    LogInUser current_user;
    User tutor; // Made class member
    private long SLOT_INTERVAL; // Slot interval in milliseconds

    // --- State ---
    final AtomicReference<Date> requestedDate = new AtomicReference<>();
    final AtomicLong chosenTime = new AtomicLong(0); // Stores the selected UTC time-of-day value
    final AtomicLong length = new AtomicLong(0);
    final AtomicInteger subject = new AtomicInteger(0);


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

        // --- Initialize Data Managers ---
        // Ensure context is valid before proceeding
        if (getContext() == null) {
            Toast.makeText(getContext(), "Context is null in onViewCreated", Toast.LENGTH_LONG).show();

            return;
        }
        dataManager = DataManager.getInstance(getContext());
        current_user = LogInUser.getInstance(dataManager);
        SLOT_INTERVAL = dataManager.getSessionDao().hourToMseconds(2); // Example: 2-hour slots

        // --- Retrieve arguments ---
        if (!loadArguments()) {
            // If arguments failed to load, finish the activity/fragment
            if (getActivity() != null) getActivity().finish();
            return;
        }

        // --- Find Views ---
        findViews(view);

        // --- Configure CalendarView: Prevent booking for today ---
        configureCalendarView();

        // --- Populate Static UI Sections ---
        populateTutorInfo();
        populateSubjectRadioButtons();
        populateDurationRadioButtons();

        // --- Setup Listeners ---
        setupListeners();

        // --- Initial Population of Dynamic UI (Time Slots) ---
        // Ensure initial date is set before updating times
        if (requestedDate.get() != null) {
            updateAvailableTimes(requestedDate.get());
        } else {
            Toast.makeText(getContext(), "Initial requested date is null. Cannot update times.", Toast.LENGTH_LONG).show();
            // Handle this case, maybe default to tomorrow explicitly if calendar setup failed
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            tomorrow.set(Calendar.HOUR_OF_DAY, 0); tomorrow.set(Calendar.MINUTE, 0); tomorrow.set(Calendar.SECOND, 0); tomorrow.set(Calendar.MILLISECOND, 0);
            requestedDate.set(tomorrow.getTime());
            updateAvailableTimes(requestedDate.get());
        }
    } // End onViewCreated

    // --- Helper Methods for Setup ---

    private boolean loadArguments() {
        if (getArguments() == null) {
            Toast.makeText(getContext(), "Error: Booking data missing.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Arguments are null.");
//            Toast.makeText(getContext(), "Initial requested date is null. Cannot update times.", Toast.LENGTH_LONG).show();

            return false;
        }
        tutor = (User) getArguments().getSerializable("tutor");
        if (tutor == null) {
            Toast.makeText(getContext(), "Error: Tutor data invalid.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Tutor object from arguments is null.");
            return false;
        }
        // subjectStr = getArguments().getString("subjects"); // Get if needed for display
        return true;
    }

    private void findViews(@NonNull View view) {
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
    }

    private void configureCalendarView() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1); // Move to tomorrow
        tomorrow.set(Calendar.HOUR_OF_DAY, 0); tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0); tomorrow.set(Calendar.MILLISECOND, 0);
        calendarView.setMinDate(tomorrow.getTimeInMillis());

        // Initialize requestedDate with the CalendarView's current DEFAULT date (at midnight)
        // This should be tomorrow due to setMinDate
        Calendar initCal = Calendar.getInstance();
        initCal.setTimeInMillis(calendarView.getDate()); // Get initial date set in CalendarView
        initCal.set(Calendar.HOUR_OF_DAY, 0); initCal.set(Calendar.MINUTE, 0);
        initCal.set(Calendar.SECOND, 0); initCal.set(Calendar.MILLISECOND, 0);
        requestedDate.set(initCal.getTime());
        Log.d(TAG, "CalendarView configured. MinDate: " + tomorrow.getTime() + ", Initial requestedDate: " + requestedDate.get());

    }

    @SuppressLint("SetTextI18n")
    private void populateTutorInfo() {
        if (tutor == null) return;
        tutorName.setText(tutor.getFirstName() + " " + tutor.getLastName());
        // tutorSubjects.setText(subjectStr); // Set if you have the string

        if (tutor.getAverageRating() != -1) {
            tutorRating.setRating((float) tutor.getAverageRating());
        } else {
            tutorRating.setVisibility(View.GONE);
            // Add "Not rated yet" text if needed (ensure layout ID is correct)
            LinearLayout detailsLayout = getView().findViewById(R.id.session_booking_tutor_details);
            if (detailsLayout != null && detailsLayout.findViewById(R.id.not_rated_text) == null) { // Avoid adding multiple times
                TextView not_rating = new TextView(getContext());
                not_rating.setId(R.id.not_rated_text); // Give it an ID
                not_rating.setText("Not rated yet");
                not_rating.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                detailsLayout.addView(not_rating);
            }
        }
    }

    private void populateSubjectRadioButtons() {
        if (tutor == null || current_user == null || dataManager == null || getContext() == null) return;

        rbgSubjects.removeAllViews();
        subject.set(0);

        List<UserSubject> tutorSubjectsList = dataManager.getSubjectDao().getUserSubjects(tutor.getStudentNum());
        List<UserSubject> tuteeSubjectsList = dataManager.getSubjectDao().getUserSubjects(current_user.getUser().getStudentNum());
        boolean foundSubjects = false;

        for (UserSubject tutorSubject : tutorSubjectsList) {
            if (!tutorSubject.getTutoring()) continue;

            for (UserSubject tuteeSubject : tuteeSubjectsList) {
                if (tutorSubject.getSubjectId() == tuteeSubject.getSubjectId()) {
                    RadioButton radioButton = new RadioButton(getContext());
                    // Fetch subject name safely
                    String subjectName = "Subject ID: " + tutorSubject.getSubjectId(); // Fallback name
                    try {
                        subjectName = dataManager.getSubjectDao().getSubjectById(tutorSubject.getSubjectId()).getSubjectName();
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting subject name for ID: " + tutorSubject.getSubjectId(), e);
                    }
                    radioButton.setText(subjectName);
                    radioButton.setId(View.generateViewId());
                    final int currentSubjectId = tutorSubject.getSubjectId();
                    radioButton.setOnClickListener(l -> subject.set(currentSubjectId));
                    rbgSubjects.addView(radioButton);
                    foundSubjects = true;
                    break;
                }
            }
        }
        if (!foundSubjects) {
            TextView noSubjects = new TextView(getContext());
            noSubjects.setText("No matching subjects found.");
            rbgSubjects.addView(noSubjects);
        }
    }

    private void populateDurationRadioButtons() {
        if (getContext() == null || dataManager == null) return;
        duration.removeAllViews();
        length.set(0);

        int[] minutesOptions = {30, 60, 120};
        String[] labels = {"30 minutes", "1 hour", "2 hours"};

        for (int i = 0; i < minutesOptions.length; i++) {
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setText(labels[i]);
            radioButton.setId(View.generateViewId());
            final long durationMillis = dataManager.getSessionDao().minutesToMseconds(minutesOptions[i]);
            radioButton.setOnClickListener(l -> length.set(durationMillis));
            duration.addView(radioButton);
        }
    }

    private void setupListeners() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Log.d(TAG, "Date changed: " + year + "-" + (month + 1) + "-" + dayOfMonth);
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date newSelectedDate = calendar.getTime();
            requestedDate.set(newSelectedDate);

            // Update available time slots when the date changes
            updateAvailableTimes(newSelectedDate);
        });

        viewProfile.setOnClickListener(l -> {
            if (tutor == null || getContext() == null) return;
            Intent toProfile = new Intent(getContext(), TutorProfile.class);
            toProfile.putExtra("tutor", tutor);
            startActivity(toProfile);
        });

        cancel.setOnClickListener(l -> {
            if (getActivity() != null) getActivity().finish();
        });

        submit.setOnClickListener(this::handleSubmit); // Use method reference
    }

    // --- Dynamic Time Slot Update ---

    @SuppressLint("SetTextI18n")
    private void updateAvailableTimes(Date selectedDate) {
        // Ensure context and necessary data are available
        if (getContext() == null || dataManager == null || tutor == null || selectedDate == null) {
            Log.w(TAG, "Cannot update time slots - missing context, data, or selected date.");
            time.removeAllViews(); // Clear anyway
            TextView errorMsg = new TextView(getContext());
            errorMsg.setText("Could not load times.");
            time.addView(errorMsg);
            return;
        }
        Log.d(TAG, "Updating available times for date: " + selectedDate);

        time.removeAllViews(); // Clear existing time slots
        chosenTime.set(0);     // Reset chosen time selection

        // Call the DAO method to get available slots
        List<Long> availableStartsUTC = dataManager.getSessionDao().getAvailableTimeSlots(
                tutor.getStudentNum(),
                selectedDate,
                SLOT_INTERVAL
        );

        if (availableStartsUTC.isEmpty()) {
            TextView noSlots = new TextView(getContext());
            noSlots.setText("No available slots for this date.");
            time.addView(noSlots);
            Log.d(TAG, "No available slots returned from DAO.");
        } else {
            Log.d(TAG, "DAO returned " + availableStartsUTC.size() + " available slots.");
            // SimpleDateFormat for local time display
            SimpleDateFormat sdfLocal = new SimpleDateFormat("HH:mm", Locale.getDefault());
            sdfLocal.setTimeZone(TimeZone.getDefault()); // Use device's local timezone

            for (long startTimeMillisUTC : availableStartsUTC) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setId(View.generateViewId());

                // Format the UTC time-of-day value for display in local time
                Date utcTimeDate = new Date(startTimeMillisUTC); // Represents time on Jan 1 1970 UTC
                String displayTime = sdfLocal.format(utcTimeDate); // Format using local timezone

                radioButton.setText(displayTime);
                radioButton.setOnClickListener(l -> {
                    chosenTime.set(startTimeMillisUTC); // Store the UTC time-of-day value
                    Log.d(TAG, "Time selected: " + displayTime + " (UTC value: " + startTimeMillisUTC + ")");
                });
                time.addView(radioButton);
            }
        }
    }

    // --- Submission Logic ---

    private void handleSubmit(View v) {
        if (getContext() == null || dataManager == null || current_user == null || tutor == null) {
            Log.e(TAG, "Cannot submit, data manager or user info is null.");
            Toast.makeText(getContext(), "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Get current values from Atomic variables
        int selectedSubjectId = subject.get();
        Date selectedDate = requestedDate.get();
        long selectedTimeUTCValue = chosenTime.get(); // UTC time-of-day value
        long selectedDuration = length.get();
        String locationStr = location.getText().toString().trim();

        // --- Validation ---
        if (!validateInputs(selectedSubjectId, selectedDate, selectedTimeUTCValue, selectedDuration, locationStr)) {
            return; // Validation failed, message shown in validateInputs
        }

        // --- Calculate StartTime and EndTime (Corrected Logic) ---
        Calendar startCalendar = Calendar.getInstance(); // Local Timezone
        startCalendar.setTime(selectedDate); // Set to YYYY, MM, DD of selected date

        Calendar timePartCalUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        timePartCalUTC.setTimeInMillis(selectedTimeUTCValue); // Time part in UTC

        // Apply UTC hour/minute to local calendar instance
        startCalendar.set(Calendar.HOUR_OF_DAY, timePartCalUTC.get(Calendar.HOUR_OF_DAY));
        startCalendar.set(Calendar.MINUTE, timePartCalUTC.get(Calendar.MINUTE));
        startCalendar.set(Calendar.SECOND, 0); startCalendar.set(Calendar.MILLISECOND, 0);

        Date startTimeLocal = startCalendar.getTime(); // Final start time in local TZ
        Date endTimeLocal = new Date(startTimeLocal.getTime() + selectedDuration); // Final end time in local TZ

        Log.d(TAG, "Submitting request:");
        Log.d(TAG, "  Subject ID: " + selectedSubjectId);
        Log.d(TAG, "  Tutee ID: " + current_user.getUser().getStudentNum());
        Log.d(TAG, "  Tutor ID: " + tutor.getStudentNum());
        Log.d(TAG, "  Start Time (Local): " + startTimeLocal);
        Log.d(TAG, "  End Time (Local): " + endTimeLocal);
        Log.d(TAG, "  Location: " + locationStr);

        // Final check: Ensure startTime is not in the past (should be prevented by CalendarView minDate and DAO checks)
        if (startTimeLocal.before(new Date())) {
            Log.w(TAG, "Submit blocked: Calculated start time is in the past!");
            Toast.makeText(getContext(), "Cannot book a time slot that has already passed.", Toast.LENGTH_LONG).show();
            updateAvailableTimes(selectedDate); // Refresh available times
            return;
        }

        // --- Call requestSession (Corrected Arguments) ---
        boolean success = dataManager.getSessionDao().requestSession(
                current_user.getUser().getStudentNum(), // tuteeId
                tutor.getStudentNum(),                  // tutorId
                new TimeSlot(startTimeLocal, endTimeLocal),
                selectedSubjectId,
                locationStr
        );

        if (success) {
            Log.i(TAG, "Session request successful!");
            Toast.makeText(getContext(), "Session request created successfully!", Toast.LENGTH_LONG).show();
            if (getActivity() != null) {
                getActivity().finish(); // Close the booking screen
            }
        } else {
            Log.w(TAG, "Session request failed (DAO returned false).");
            Toast.makeText(getContext(), "Session request failed! The selected slot might no longer be available or is outside operating hours.", Toast.LENGTH_LONG).show();
            // Refresh the available times as the slot might be taken now
            updateAvailableTimes(selectedDate);
        }
    }

    private void attemptToBookSession(int sessionIdToBook) {
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot proceed with booking.");
            Toast.makeText(getContext(), "Error: Cannot book session.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure dataManager and current_user are initialized
        // dataManager = DataManager.getInstance(getContext());
        // current_user = LogInUser.getInstance(dataManager);

        if (current_user == null || !current_user.isLoggedIn()) {
            Toast.makeText(getContext(), "You must be logged in to book a session.", Toast.LENGTH_SHORT).show();
            // Redirect to login
            return;
        }

        int currentUserId = current_user.getUser().getStudentNum();
        SessionDao sessionDao = dataManager.getSessionDao();

        // It's good practice to run database operations off the main UI thread.
        // Use AsyncTask (older), Executors, or Kotlin Coroutines/RxJava if you are using them.
        // For simplicity in this example, direct call, but ideally, use background thread.
        // Example with a simple Executor:
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // Background work
            Session sessionToBook = sessionDao.getSessionById(sessionIdToBook); // Fetch the session

            handler.post(() -> {
                // UI Thread work
                if (sessionToBook != null) {
                    // Check if the session is still in a bookable state (e.g., PENDING or CONFIRMED and not in the past)
                    // This depends on your Session.Status enum and business rules.
                    // For example:
                    // if (sessionToBook.getStatus() == Session.Status.PENDING ||
                    //     (sessionToBook.getStatus() == Session.Status.CONFIRMED &&
                    //      sessionToBook.getStartTime().after(new Date()))) {

                    List<Integer> tutees = sessionToBook.getTuteeIds(); // Uses the getter in Session.java

                    if (tutees.size() < sessionToBook.getSessionMax()) {
                        if (!tutees.contains(currentUserId)) {
                            // You might want to add more checks here:
                            // 1. Does the tutee meet prerequisites for the subject?
                            // 2. Payment/credit check if applicable.

                            sessionToBook.addTutee(currentUserId); // Use the helper method in Session.java
                            // This internally calls setTuteeIds
                            // sessionToBook.setTuteeIds(tutees); // Alternative if not using addTutee helper

                            // Update the session in the database (again, ideally in a background thread for the DAO op)
                            executor.execute(() -> {
                                int rowsAffected = sessionDao.updateSession(sessionToBook); // Assuming updateSession exists
                                handler.post(() -> {
                                    if (rowsAffected > 0) {
                                        Toast.makeText(getContext(), "Successfully booked session!", Toast.LENGTH_SHORT).show();
                                        // Navigate away, update UI, etc.
                                        if (getActivity() != null) {
                                            getActivity().finish(); // Example: close the booking fragment/activity
                                        }
                                    } else {
                                        Toast.makeText(getContext(), "Failed to update session. Please try again.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });

                        } else {
                            Toast.makeText(getContext(), "You are already booked for this session.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Sorry, this session is full.", Toast.LENGTH_SHORT).show();
                    }
                    // } else {
                    //    Toast.makeText(getContext(), "This session is no longer available for booking.", Toast.LENGTH_SHORT).show();
                    // }
                } else {
                    Toast.makeText(getContext(), "Session not found.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // You would call attemptToBookSession(someSessionId) from your button's OnClickListener.
// For example, in onViewCreated inside your SessionBookingsFragment:
// Button bookButton = view.findViewById(R.id.your_book_button_id);
// bookButton.setOnClickListener(v -> {
//     int selectedSessionId = ...; // Get the ID of the session the user selected from your UI
//     attemptToBookSession(selectedSessionId);
// });
    private boolean validateInputs(int subjectId, Date date, long timeValue, long durationValue, String location) {
        if (subjectId == 0) {
            Toast.makeText(getContext(), "Please select a subject", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (date == null) {
            Toast.makeText(getContext(), "Please select a date", Toast.LENGTH_SHORT).show();
            return false;
        }
        // Check if date is valid (tomorrow or later)
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0); tomorrow.set(Calendar.MINUTE, 0); tomorrow.set(Calendar.SECOND, 0); tomorrow.set(Calendar.MILLISECOND, 0);
        if (date.before(tomorrow.getTime())) {
            Toast.makeText(getContext(), "Please select a date from tomorrow onwards.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (timeValue == 0) {
            Toast.makeText(getContext(), "Please select a time", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (durationValue == 0) {
            Toast.makeText(getContext(), "Please select a duration", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (location == null || location.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a location", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

} // End Fragment class