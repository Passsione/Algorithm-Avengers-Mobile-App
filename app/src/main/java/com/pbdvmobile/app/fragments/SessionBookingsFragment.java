
package com.pbdvmobile.app.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log; // Import Log for debugging
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pbdvmobile.app.R;
import com.pbdvmobile.app.TutorProfile;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.Schedule.TimeSlot;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.UserSubject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
// Consider changing AtomicInteger back to AtomicLong if you encounter precision issues or larger millisecond values
// though current usage seems to fit within int.

public class SessionBookingsFragment extends Fragment {

    private static final String TAG = "SessionBookingsFrag"; // For logging

    DataManager dataManager;
    LogInUser currentUserSession;
    TextView tutorNameDisplay, tutorSubjectsDisplay; // Renamed for clarity
    Spinner locationSpinner; // Renamed
    RatingBar tutorRatingBar; // Renamed
    ImageView tutorProfileImage;
    Button viewProfileButton, cancelButton, submitButton; // Renamed
    RadioGroup subjectsRadioGroup, timeRadioGroup, durationRadioGroup; // Renamed
    CalendarView calendarView;
    Long startOfTodayMillis;

    private final String[] locations = {
            "Steve Biko Library",
            "ML Sultan Library",
    };
    private String selectedLocationId; // Renamed

    // Declare AtomicInteger fields here to be accessible by helper methods and listeners
    private AtomicInteger selectedSubjectId = new AtomicInteger(0);
    private AtomicInteger selectedDurationMillis = new AtomicInteger(0);
    private AtomicInteger selectedTimeOffsetMillis = new AtomicInteger(0);
    private Date selectedDate = new Date(); // Initialize to prevent null issues
    private User currentTutor;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_bookings, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataManager = DataManager.getInstance(getContext());
        currentUserSession = LogInUser.getInstance(dataManager);

        assert getArguments() != null;
        currentTutor = (User) getArguments().getSerializable("tutor");
        String subjectsArg = getArguments().getString("subjects");

        // Initialize Views
        initializeViews(view);
/*
        // ---- Start: Calculate Start of Tomorrow for minDate ----
        Calendar tomorrowCalendar = Calendar.getInstance();
        // Add one day to the current date
        tomorrowCalendar.add(Calendar.DAY_OF_YEAR, 1);
        // Set the time to the beginning of that day (midnight)
        tomorrowCalendar.set(Calendar.HOUR_OF_DAY, 0);
        tomorrowCalendar.set(Calendar.MINUTE, 0);
        tomorrowCalendar.set(Calendar.SECOND, 0);
        tomorrowCalendar.set(Calendar.MILLISECOND, 0);
        long startOfTomorrowMillis = tomorrowCalendar.getTimeInMillis();
        // ---- End: Calculate Start of Tomorrow ----
        // Set min date for calendar to the start of tomorrow
        calendarView.setMinDate(startOfTomorrowMillis);
*/

        // Set min date for calendar
        calendarView.setMinDate(System.currentTimeMillis());
        selectedDate.setTime(calendarView.getDate()); // Initialize with Calendar's current effective date
/*

        // Initialize selectedDate:
        // If calendarView.getDate() is somehow before startOfTomorrowMillis (it shouldn't be after setMinDate),
        // then default selectedDate to startOfTomorrowMillis. Otherwise, use calendarView's current date.
        if (calendarView.getDate() < startOfTomorrowMillis) {
            selectedDate.setTime(startOfTomorrowMillis);
            calendarView.setDate(startOfTomorrowMillis); // Also update the calendar view itself
        } else {
            selectedDate.setTime(calendarView.getDate());
        }
        Log.d(TAG, "Initial selectedDate: " + selectedDate.toString());

*/

        // ---- Start - startOfTodayMillis (midnight today) ----
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startOfTodayMillis = cal.getTimeInMillis();
        // ---- End - startOfTodayMillis ----

        populateTutorInfo(currentTutor, subjectsArg, view);
        populateSubjectsRadioGroup(currentTutor);
        populateDurationRadioGroup(); // No need to pass tutor if not used
        createLocationDropDown();

        calendarView.setOnDateChangeListener((cv, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth, 0, 0, 0); // Normalize to start of day
            calendar.set(Calendar.MILLISECOND, 0);

            // Check if selected date is before minDate (should not happen with setMinDate but good for safety)
            if (calendar.getTimeInMillis() < calendarView.getMinDate()) {
                cv.setDate(calendarView.getMinDate()); // Reset to minDate if somehow invalid
                selectedDate.setTime(calendarView.getMinDate());
            } else {
                cv.setDate(calendar.getTimeInMillis());
                selectedDate.setTime(calendar.getTimeInMillis());
            }
            Log.d(TAG, "Date selected: " + selectedDate.toString());
            createTimesRadioButtons();
        });

        cancelButton.setOnClickListener(l -> {
            if (getActivity() != null) getActivity().finish();
        });
        submitButton.setOnClickListener(l -> handleSubmitBooking());

        // Initial population of time slots (after default duration is set)
        createTimesRadioButtons();
    }

    private void initializeViews(View view) {
        tutorSubjectsDisplay = view.findViewById(R.id.session_booking_tutor_subjects);
        tutorNameDisplay = view.findViewById(R.id.session_booking_tutor_name);
        tutorProfileImage = view.findViewById(R.id.session_booking_tutor_image); // Make sure this ID exists
        tutorRatingBar = view.findViewById(R.id.session_booking_tutor_rating);
        viewProfileButton = view.findViewById(R.id.booking_tutor_profile);
        subjectsRadioGroup = view.findViewById(R.id.subjects_radio_group);
        timeRadioGroup = view.findViewById(R.id.radio_group_time);
        durationRadioGroup = view.findViewById(R.id.radio_group_duration);
        locationSpinner = view.findViewById(R.id.locations);
        cancelButton = view.findViewById(R.id.btn_cancel_booking);
        submitButton = view.findViewById(R.id.btn_confirm_booking);
        calendarView = view.findViewById(R.id.calendar);
    }

    private void populateTutorInfo(User tutor, String subjectsDisplayString, View fragmentView) {
        tutorNameDisplay.setText(String.format("%s %s", tutor.getFirstName(), tutor.getLastName()));
        tutorSubjectsDisplay.setText(subjectsDisplayString);

        double ratingValue = dataManager.getSessionDao().getAverageRatingByStudentNum(tutor.getStudentNum())[1]; // Assuming [1] is tutor's rating as tutee
        if (ratingValue > 0) { // Usually ratings are > 0 if set. -1 or 0 might mean not rated.
            tutorRatingBar.setRating((float) ratingValue);
            tutorRatingBar.setVisibility(View.VISIBLE);
        } else {
            tutorRatingBar.setVisibility(View.GONE);
            TextView notRatedTextView = new TextView(getContext());
            notRatedTextView.setText(R.string.not_rated_yet); // Use string resource
            notRatedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            LinearLayout detailsLayout = fragmentView.findViewById(R.id.session_booking_tutor_details);
            if (detailsLayout != null) {
                // Remove previous "Not rated yet" if any, before adding a new one.
                for (int i = 0; i < detailsLayout.getChildCount(); i++) {
                    View child = detailsLayout.getChildAt(i);
                    if (child instanceof TextView && ((TextView) child).getText().toString().equals(getString(R.string.not_rated_yet))) {
                        detailsLayout.removeViewAt(i);
                        break;
                    }
                }
                detailsLayout.addView(notRatedTextView);
            }
        }
        viewProfileButton.setOnClickListener(l -> {
            Intent toProfile = new Intent(getContext(), TutorProfile.class);
            toProfile.putExtra("tutor", tutor);
            startActivity(toProfile);
        });
    }

    private void populateSubjectsRadioGroup(User tutor) {
        subjectsRadioGroup.removeAllViews();
        selectedSubjectId.set(0); // Reset
        List<UserSubject> tutorUserSubjects = dataManager.getSubjectDao().getUserSubjects(tutor.getStudentNum());
        List<UserSubject> tuteeUserSubjects = dataManager.getSubjectDao().getUserSubjects(currentUserSession.getUser().getStudentNum());
        boolean isFirstSubject = true;

        for (UserSubject tutorSubj : tutorUserSubjects) {
            if (!tutorSubj.getTutoring()) continue;
            for (UserSubject tuteeSubj : tuteeUserSubjects) {
                if (tutorSubj.getSubjectId() == tuteeSubj.getSubjectId()) {
                    RadioButton radioButton = new RadioButton(getContext());
                    String subjectName = dataManager.getSubjectDao().getSubjectById(tutorSubj.getSubjectId()).getSubjectName();
                    radioButton.setText(subjectName);
                    radioButton.setId(View.generateViewId()); // Important for RadioGroup
                    final int currentSubjectId = tutorSubj.getSubjectId();
                    radioButton.setOnClickListener(l -> selectedSubjectId.set(currentSubjectId));
                    subjectsRadioGroup.addView(radioButton);

                    if (isFirstSubject) {
                        radioButton.setChecked(true);
                        selectedSubjectId.set(currentSubjectId);
                        isFirstSubject = false;
                    }
                    break;
                }
            }
        }
        if (subjectsRadioGroup.getChildCount() == 0) {
            TextView noSubjects = new TextView(getContext());
            noSubjects.setText(R.string.no_common_subjects_tutor); // Use string resource
            subjectsRadioGroup.addView(noSubjects);
        }
    }

    private void populateDurationRadioGroup() {
        durationRadioGroup.removeAllViews();
        selectedDurationMillis.set(0); // Reset

        int[] minutes = {30, 60, 120}; // 30 min, 1 hr, 2 hr
        String[] labels = {"30 minutes", "1 hour", "2 hours"};
        boolean isFirstDuration = true;

        for (int i = 0; i < minutes.length; i++) {
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setText(labels[i]);
            radioButton.setId(View.generateViewId());
            final int durationValueMillis = (int) dataManager.getSessionDao().minutesToMseconds(minutes[i]);
            radioButton.setOnClickListener(l -> {
                selectedDurationMillis.set(durationValueMillis);
                Log.d(TAG, "Duration selected: " + durationValueMillis + "ms");
                createTimesRadioButtons();
            });
            durationRadioGroup.addView(radioButton);
            if (isFirstDuration) {
                radioButton.setChecked(true);
                selectedDurationMillis.set(durationValueMillis);
                isFirstDuration = false;
            }
        }
    }

    private void createTimesRadioButtons() {
        timeRadioGroup.removeAllViews();
        selectedTimeOffsetMillis.set(0); // Reset

        if (getContext() == null || currentTutor == null) {
            Log.e(TAG, "Context or Tutor is null. Cannot create times.");
            return;
        }
        if (selectedDurationMillis.get() == 0) {
            Log.d(TAG, "No duration selected. Time slots will not be populated.");
            TextView noDurationMsg = new TextView(getContext());
            noDurationMsg.setText(R.string.select_duration_prompt); // Use string resource
            timeRadioGroup.addView(noDurationMsg);
            return;
        }

        List<TimeSlot> takenSlotsForTutor = dataManager.getSessionDao().getTakenTimeSlot(currentTutor.getStudentNum());
        // You might also want to get taken slots for the current tutee
        // List<TimeSlot> takenSlotsForTutee = dataManager.getSessionDao().getTakenTimeSlotForUser(currentUserSession.getUser().getStudentNum(), true);
        // List<TimeSlot> takenSlotsForTuteeAsTutor = dataManager.getSessionDao().getTakenTimeSlotForUser(currentUserSession.getUser().getStudentNum(), false);


        long slotCheckingIntervalMillis = dataManager.getSessionDao().minutesToMseconds(0); // e.g., check every 30 mins
        long serviceOpenTimeOffset = dataManager.getSessionDao().OPEN_TIME;
        long serviceCloseTimeOffset = dataManager.getSessionDao().CLOSE_TIME;

        Log.d(TAG, "Creating times for date: " + selectedDate + ", duration: " + selectedDurationMillis.get() + "ms");
        Log.d(TAG, "Service hours: OPEN_TIME offset=" + serviceOpenTimeOffset + ", CLOSE_TIME offset=" + serviceCloseTimeOffset);

        int timesAdded = 0;
        Date now = new Date(); // Current time

        // Iterate through potential start times for the selected day
        for (long currentOffsetMillis = serviceOpenTimeOffset; currentOffsetMillis < serviceCloseTimeOffset; currentOffsetMillis += slotCheckingIntervalMillis) {

            Calendar candidateStartCalendar = Calendar.getInstance();
            candidateStartCalendar.setTime(selectedDate); // Base on the selected day (already normalized to 00:00)
            candidateStartCalendar.add(Calendar.MILLISECOND, (int) currentOffsetMillis); // Add the time-of-day offset

            Date candidateStartTime = candidateStartCalendar.getTime();
            Date candidateEndTime = new Date(candidateStartTime.getTime() + selectedDurationMillis.get());

            // 1. Check if candidate slot starts in the past (only if selectedDate is today)
            //    and also ensure it doesn't start before OPEN_TIME (already handled by loop init)
            if (candidateStartTime.before(now)) {
                Log.v(TAG, "Slot " + dataManager.formatDateTime(candidateStartTime.toString())[1] + " is in the past. Skipping.");
                continue;
            }

            // 2. Check if candidate slot ends after service CLOSE_TIME for that day
            Calendar serviceCloseOfDayCalendar = Calendar.getInstance();
            serviceCloseOfDayCalendar.setTime(selectedDate);
            serviceCloseOfDayCalendar.add(Calendar.MILLISECOND, (int) serviceCloseTimeOffset);

            if (candidateEndTime.after(serviceCloseOfDayCalendar.getTime())) {
                Log.v(TAG, "Slot " + dataManager.formatDateTime(candidateStartTime.toString())[1] + " ends after service close time. Skipping.");
                continue; // This slot would end too late.
            }
            // Create a TimeSlot object for the candidate (NO PADDING as per your update)
            TimeSlot candidateSlot = new TimeSlot(new Date(candidateStartTime.getTime()), new Date(candidateEndTime.getTime()));


            // 3. Check for overlaps with tutor's existing sessions
            boolean isOverlapping = false;
            for (TimeSlot takenSlot : takenSlotsForTutor) {
                // Ensure takenSlot is for the same day as candidateSlot before precise overlap check
                Calendar takenSlotCal = Calendar.getInstance();
                takenSlotCal.setTime(takenSlot.getStartTime()); // Assuming takenSlot stores absolute date/time

                if (takenSlotCal.get(Calendar.YEAR) == candidateStartCalendar.get(Calendar.YEAR) &&
                        takenSlotCal.get(Calendar.DAY_OF_YEAR) == candidateStartCalendar.get(Calendar.DAY_OF_YEAR)) {
                    if (candidateSlot.overlaps(takenSlot)) {
                        isOverlapping = true;
                        Log.d(TAG, "Slot " + dataManager.formatDateTime(candidateStartTime.toString())[1] + " overlaps with tutor's taken slot: " + takenSlot.toString());
                        break;
                    }
                }
            }

            // Add similar overlap check for tutee's schedule if implementing that
            // for (TimeSlot tuteeTaken : takenSlotsForTutee) { ... }
            // for (TimeSlot tuteeTakenAsTutor : takenSlotsForTuteeAsTutor) { ... }

            if (!isOverlapping) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setText(dataManager.formatDateTime(candidateStartTime.toString())[1]);
                radioButton.setId(View.generateViewId());
                final long finalCurrentOffsetMillis = currentOffsetMillis;
                radioButton.setOnClickListener(l -> {
                    selectedTimeOffsetMillis.set((int) finalCurrentOffsetMillis);
                    Log.d(TAG, "Time selected: offset " + finalCurrentOffsetMillis);
                });
                timeRadioGroup.addView(radioButton);
                timesAdded++;
            }
        }

        if (timesAdded == 0) {
            TextView noTimesMsg = new TextView(getContext());
            if (selectedDurationMillis.get() > 0) {
                noTimesMsg.setText(R.string.no_available_times); // Use string resource
            } else {
                noTimesMsg.setText(R.string.select_duration_prompt); // Should have been caught earlier
            }
            timeRadioGroup.addView(noTimesMsg);
        }
        Log.d(TAG, "Finished creating times for " + selectedDate + ". Added: " + timesAdded);
    }


    private void createLocationDropDown() {
        if (getContext() == null) return;
        selectedLocationId = null; // Reset

        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, locations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);
        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLocationId = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedLocationId = null;
            }
        });
        if (locations.length > 0) {
            locationSpinner.setSelection(0); // Default to first location
            selectedLocationId = locations[0];
        }
    }

    private void handleSubmitBooking() {
        // Normalize selectedDate to the start of the day for consistent comparison
        Calendar normalizedSelectedDateCal = Calendar.getInstance();
        normalizedSelectedDateCal.setTime(selectedDate);
        normalizedSelectedDateCal.set(Calendar.HOUR_OF_DAY, 0);
        normalizedSelectedDateCal.set(Calendar.MINUTE, 0);
        normalizedSelectedDateCal.set(Calendar.SECOND, 0);
        normalizedSelectedDateCal.set(Calendar.MILLISECOND, 0);

        if (selectedSubjectId.get() == 0) {
            Toast.makeText(getContext(), R.string.select_subject_validation, Toast.LENGTH_LONG).show(); return;
        }
        // This check uses startOfTodayMillis which is midnight of the app's current day.
        // calendarView.getMinDate() should already prevent selection of past days.
        if (normalizedSelectedDateCal.getTimeInMillis() < startOfTodayMillis) {
            Toast.makeText(getContext(), R.string.select_valid_date_validation, Toast.LENGTH_LONG).show(); return;
        }
        if (selectedDurationMillis.get() == 0) {
            Toast.makeText(getContext(), R.string.select_duration_validation, Toast.LENGTH_LONG).show(); return;
        }
        if (selectedLocationId == null || selectedLocationId.isEmpty()) {
            Toast.makeText(getContext(), R.string.select_location_validation, Toast.LENGTH_LONG).show(); return;
        }
        // Check if a time radio button is actually selected
        if (timeRadioGroup.getCheckedRadioButtonId() == -1 && selectedTimeOffsetMillis.get() == 0 ) {
            // selectedTimeOffsetMillis.get() == 0 could be a valid selection if OPEN_TIME is 00:00
            // A better check is if any radio button in the time group is selected.
            // However, selectedTimeOffsetMillis is set on click, so if it's still 0 (and not legitimately 0 for midnight),
            // it likely means nothing was viable or clicked.
            boolean timeSelected = false;
            for(int i=0; i<timeRadioGroup.getChildCount(); ++i){
                View child = timeRadioGroup.getChildAt(i);
                if(child instanceof RadioButton){
                    if(((RadioButton) child).isChecked()){
                        timeSelected = true;
                        break;
                    }
                }
            }
            if(!timeSelected && timeRadioGroup.getChildCount() > 0 && timeRadioGroup.getChildAt(0) instanceof RadioButton){
                Toast.makeText(getContext(), R.string.select_time_validation, Toast.LENGTH_LONG).show(); return;
            } else if (!timeSelected && !(timeRadioGroup.getChildAt(0) instanceof RadioButton)) {
                Toast.makeText(getContext(), R.string.no_available_times_for_booking, Toast.LENGTH_LONG).show(); return;
            }
        }


        // Construct actual start and end times for the session
        Calendar finalSessionStartCalendar = Calendar.getInstance();
        finalSessionStartCalendar.setTime(selectedDate); // Base: selected date (already 00:00)
        finalSessionStartCalendar.add(Calendar.MILLISECOND, selectedTimeOffsetMillis.get()); // Add chosen time-of-day offset

        Date actualSessionStartTime = finalSessionStartCalendar.getTime();
        Date actualSessionEndTime = new Date(actualSessionStartTime.getTime() + selectedDurationMillis.get());

        Log.d(TAG, "Attempting to book session: " +
                "TuteeID: " + currentUserSession.getUser().getStudentNum() +
                ", TutorID: " + currentTutor.getStudentNum() +
                ", SubjectID: " + selectedSubjectId.get() +
                ", Start: " + actualSessionStartTime +
                ", End: " + actualSessionEndTime +
                ", Location: " + selectedLocationId);

        // Create TimeSlot with ACTUAL (unpadded) times for the request method.
        // SessionDao.requestSession will use these for its logic, including overlap checks.
        TimeSlot requestedTimeSlot = new TimeSlot(new Date(actualSessionStartTime.getTime()), new Date(actualSessionEndTime.getTime()));

        boolean success = dataManager.getSessionDao().requestSession(
                currentUserSession.getUser().getStudentNum(),
                currentTutor.getStudentNum(),
                requestedTimeSlot, // This is now UNPADDED
                selectedSubjectId.get(),
                selectedLocationId,
                actualSessionStartTime, // Actual start for DB
                actualSessionEndTime    // Actual end for DB
        );

        if (success) {
            Toast.makeText(getContext(), R.string.session_request_success, Toast.LENGTH_LONG).show();
            if (getActivity() != null) getActivity().finish();
        } else {
            Toast.makeText(getContext(),"No Insert database", Toast.LENGTH_LONG).show();
        }
    }
}
