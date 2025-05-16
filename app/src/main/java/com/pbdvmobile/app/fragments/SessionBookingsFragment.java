
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

import com.bumptech.glide.Glide;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.PartnerProfileActivity;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.Schedule.TimeSlot;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.UserSubject;

import java.util.ArrayList;
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
    TextView tutorNameDisplay, tutorSubjectsDisplay;
    Spinner locationSpinner; 
    RatingBar tutorRatingBar;
    ImageView tutorProfileImage;
    Button viewProfileButton, cancelButton, submitButton;
    RadioGroup subjectsRadioGroup, durationRadioGroup;
    Spinner timeSpinner;
    CalendarView calendarView;
    // Use startOfTomorrowMillis for minDate logic, selectedDate for the actual chosen date (normalized)
    long startOfTomorrowMillis; // Used for CalendarView minDate
    // private long startOfTodayMillis; // Kept for reference if needed for other logic


    private final String[] locations = {
            "Steve Biko Library",
            "ML Sultan Library",
    };
    private String selectedLocationId; 

    // Declare AtomicInteger fields here to be accessible by helper methods and listeners
    private AtomicInteger selectedSubjectId = new AtomicInteger(0);
    private AtomicInteger selectedDurationMillis = new AtomicInteger(0);
    private AtomicInteger selectedTimeOffsetMillis = new AtomicInteger(0);
    private Date selectedDate = new Date(); // Initialize to prevent null issues
    private User currentTutor;

    // Helper class to store displayable time and its value for the Spinner
    private static class TimeSpinnerItem {
        private final String displayTime;
        private final int timeOffsetMillis;

        public TimeSpinnerItem(String displayTime, int timeOffsetMillis) {
            this.displayTime = displayTime;
            this.timeOffsetMillis = timeOffsetMillis;
        }

        public int getTimeOffsetMillis() {
            return timeOffsetMillis;
        }

        @NonNull
        @Override
        public String toString() {
            return displayTime; // This is what ArrayAdapter will display in the Spinner
        }
    }

    private List<TimeSpinnerItem> availableTimeSlots = new ArrayList<>();

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

        // Calculate Start of Tomorrow for minDate
        Calendar tomorrowCal = Calendar.getInstance();
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1);
        normalizeCalendarToStartOfDay(tomorrowCal); // Helper method
        startOfTomorrowMillis = tomorrowCal.getTimeInMillis();

        calendarView.setMinDate(startOfTomorrowMillis);

        // Initialize selectedDate to the start of the initially displayed/selected date in CalendarView
        Calendar initialCalendarDate = Calendar.getInstance();
        initialCalendarDate.setTimeInMillis(calendarView.getDate()); // Get current date from CalendarView
        normalizeCalendarToStartOfDay(initialCalendarDate);
        if (initialCalendarDate.getTimeInMillis() < startOfTomorrowMillis) {
            // If CalendarView's date is somehow before minDate, set to minDate
            selectedDate.setTime(startOfTomorrowMillis);
            calendarView.setDate(startOfTomorrowMillis); // Update the calendar view itself
        } else {
            selectedDate.setTime(initialCalendarDate.getTimeInMillis());
        }
        Log.d(TAG, "Initial selectedDate (normalized): " + selectedDate.toString());


        // For reference, startOfTodayMillis (midnight today)
        Calendar todayCal = Calendar.getInstance();
        normalizeCalendarToStartOfDay(todayCal);
        // startOfTodayMillis = todayCal.getTimeInMillis(); // If needed for other logic


        populateTutorInfo(currentTutor, subjectsArg, view);
        populateSubjectsRadioGroup(currentTutor);
        populateDurationRadioGroup();
        createLocationDropDown();
        setupTimeSpinnerListener(); // Added this call


        calendarView.setOnDateChangeListener((cv, year, month, dayOfMonth) -> {
            Calendar newlySelectedCalendar = Calendar.getInstance();
            newlySelectedCalendar.set(year, month, dayOfMonth);
            normalizeCalendarToStartOfDay(newlySelectedCalendar); // Normalize to 00:00:00

            if (newlySelectedCalendar.getTimeInMillis() < startOfTomorrowMillis) {
                // This case should ideally be prevented by CalendarView's setMinDate
                selectedDate.setTime(startOfTomorrowMillis);
                cv.setDate(startOfTomorrowMillis); // Force calendar back to min date
            } else {
                selectedDate.setTime(newlySelectedCalendar.getTimeInMillis());
                // cv.setDate is not strictly needed here if user interaction caused change
            }
            Log.d(TAG, "Date selected via listener (normalized): " + selectedDate.toString());
            populateTimeSpinner();
        });

        cancelButton.setOnClickListener(l -> {
            if (getActivity() != null) getActivity().finish();
        });
        submitButton.setOnClickListener(l -> handleSubmitBooking());

        populateTimeSpinner();
    }

    private void normalizeCalendarToStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void initializeViews(View view) {
        tutorSubjectsDisplay = view.findViewById(R.id.session_booking_tutor_subjects);
        tutorNameDisplay = view.findViewById(R.id.session_booking_tutor_name);
        tutorProfileImage = view.findViewById(R.id.session_booking_tutor_image);
        tutorRatingBar = view.findViewById(R.id.session_booking_tutor_rating);
        viewProfileButton = view.findViewById(R.id.booking_tutor_profile);
        subjectsRadioGroup = view.findViewById(R.id.subjects_radio_group);
        timeSpinner = view.findViewById(R.id.spinner_time);
        durationRadioGroup = view.findViewById(R.id.radio_group_duration);
        locationSpinner = view.findViewById(R.id.locations);
        cancelButton = view.findViewById(R.id.btn_cancel_booking);
        submitButton = view.findViewById(R.id.btn_confirm_booking);
        calendarView = view.findViewById(R.id.calendar);
    }

    private void populateTutorInfo(User tutor, String subjectsDisplayString, View fragmentView) {
        tutorNameDisplay.setText(String.format("%s %s", tutor.getFirstName(), tutor.getLastName()));
        tutorSubjectsDisplay.setText(subjectsDisplayString);
        Glide.with(getContext())
                .load(tutor.getProfileImageUrl())
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .circleCrop()
                .into(tutorProfileImage);
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
            Intent toProfile = new Intent(getActivity(), PartnerProfileActivity.class);
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
                populateTimeSpinner();
            });
            durationRadioGroup.addView(radioButton);
            if (isFirstDuration) {
                radioButton.setChecked(true);
                selectedDurationMillis.set(durationValueMillis);
                isFirstDuration = false;
            }
        }
    }

    private void populateTimeSpinner() {
        selectedTimeOffsetMillis.set(0); // Reset when repopulating
        availableTimeSlots.clear();

        if (getContext() == null || currentTutor == null) {
            Log.e(TAG, "Context or Tutor is null. Cannot populate time spinner.");
            // Clear spinner and show a placeholder if necessary
            ArrayAdapter<TimeSpinnerItem> emptyAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
            emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            timeSpinner.setAdapter(emptyAdapter);
            timeSpinner.setEnabled(false);
            return;
        }
        int currentSelectedDurationMillis = selectedDurationMillis.get();
        if (currentSelectedDurationMillis == 0) {
            Log.d(TAG, "No duration selected. Time spinner will be empty.");
            availableTimeSlots.add(new TimeSpinnerItem(getString(R.string.select_duration_first_for_time), 0));
            ArrayAdapter<TimeSpinnerItem> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, availableTimeSlots);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            timeSpinner.setAdapter(adapter);
            timeSpinner.setEnabled(false);
            return;
        }
        timeSpinner.setEnabled(true);

        List<TimeSlot> takenSlotsForTutor = dataManager.getSessionDao().getTakenTimeSlot(currentTutor.getStudentNum());

        long slotCheckingIntervalMillis = dataManager.getSessionDao().minutesToMseconds(30);// currentSelectedDurationMillis;
        if (slotCheckingIntervalMillis < dataManager.getSessionDao().minutesToMseconds(30)) {
            slotCheckingIntervalMillis = dataManager.getSessionDao().minutesToMseconds(30);
        }

        long serviceOpenTimeOffset = dataManager.getSessionDao().OPEN_TIME;
        long serviceCloseTimeOffset = dataManager.getSessionDao().CLOSE_TIME;
        Date now = new Date();

        Log.d(TAG, "Populating time spinner for date: " + selectedDate + ", duration: " + currentSelectedDurationMillis + "ms, interval: " + slotCheckingIntervalMillis + "ms");
        long tempInterval = slotCheckingIntervalMillis;
        for (long currentOffsetMillis = serviceOpenTimeOffset; currentOffsetMillis < serviceCloseTimeOffset; currentOffsetMillis += slotCheckingIntervalMillis) {
            Calendar candidateStartCalendar = Calendar.getInstance();
            candidateStartCalendar.setTime(selectedDate); // selectedDate is already normalized to 00:00:00
            candidateStartCalendar.add(Calendar.MILLISECOND, (int) currentOffsetMillis);
            Date actualCandidateStartTime = candidateStartCalendar.getTime();
            Date actualCandidateEndTime = new Date(actualCandidateStartTime.getTime() + currentSelectedDurationMillis);

            if (actualCandidateStartTime.before(now)) continue;

            Calendar serviceCloseOfDayCalendar = Calendar.getInstance();
            serviceCloseOfDayCalendar.setTime(selectedDate); // Normalized date
            serviceCloseOfDayCalendar.add(Calendar.MILLISECOND, (int) serviceCloseTimeOffset);
            if (actualCandidateEndTime.after(serviceCloseOfDayCalendar.getTime())) continue;

            // TimeSlot constructor takes actual UNPADDED times. Padding handled internally for 'overlaps'.
            TimeSlot candidateSlotForCheck = new TimeSlot(actualCandidateStartTime, actualCandidateEndTime);
            boolean isOverlapping = false;
            for (TimeSlot takenSlot : takenSlotsForTutor) { // takenSlot from DAO is built from UNPADDED DB times
                Calendar takenSlotActualStartCal = Calendar.getInstance();
                takenSlotActualStartCal.setTime(takenSlot.getActualStartTime()); // Use actual start for day check

                if (takenSlotActualStartCal.get(Calendar.YEAR) == candidateStartCalendar.get(Calendar.YEAR) &&
                        takenSlotActualStartCal.get(Calendar.DAY_OF_YEAR) == candidateStartCalendar.get(Calendar.DAY_OF_YEAR)) {
                    if (candidateSlotForCheck.overlaps(takenSlot)) {
                        slotCheckingIntervalMillis = dataManager.getSessionDao().minutesToMseconds(30);
                        isOverlapping = true;
                        break;
                    }
                }
            }

            if (!isOverlapping) {
                String displayTime = dataManager.formatDateTime(actualCandidateStartTime.toString())[1];
                slotCheckingIntervalMillis = tempInterval;
                availableTimeSlots.add(new TimeSpinnerItem(displayTime, (int) currentOffsetMillis));
            }

        }

        if (availableTimeSlots.isEmpty()) {
            availableTimeSlots.add(new TimeSpinnerItem(getString(R.string.no_available_times), 0)); // Placeholder
        }

        ArrayAdapter<TimeSpinnerItem> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, availableTimeSlots);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(adapter);

        // Automatically select the first valid time slot if available
        if (availableTimeSlots.size() > 0 && availableTimeSlots.get(0).getTimeOffsetMillis() != 0) { // Check it's not a placeholder
            timeSpinner.setSelection(0, false); // Select first item without triggering listener immediately
            selectedTimeOffsetMillis.set(availableTimeSlots.get(0).getTimeOffsetMillis());
        } else if (!availableTimeSlots.isEmpty() && availableTimeSlots.get(0).getTimeOffsetMillis() == 0 &&
                availableTimeSlots.get(0).displayTime.equals(getString(R.string.no_available_times))) {
            selectedTimeOffsetMillis.set(0); // No valid time selected
        }


        Log.d(TAG, "Finished populating time spinner. Items: " + availableTimeSlots.size());
    }

    private void setupTimeSpinnerListener() {
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TimeSpinnerItem selectedItem = (TimeSpinnerItem) parent.getItemAtPosition(position);
                // Ensure it's not a placeholder item before setting the time
                if (!selectedItem.displayTime.equals(getString(R.string.select_duration_first_for_time)) &&
                        !selectedItem.displayTime.equals(getString(R.string.no_available_times))) {
                    selectedTimeOffsetMillis.set(selectedItem.getTimeOffsetMillis());
                    Log.d(TAG, "Time selected via spinner: offset " + selectedItem.getTimeOffsetMillis());
                } else {
                    selectedTimeOffsetMillis.set(0); // Reset if placeholder is selected
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTimeOffsetMillis.set(0);
            }
        });
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

        // Ensure selectedDate is purely the date part (normalized to midnight)
        Calendar normalizedSelectedDateCal = Calendar.getInstance();
        normalizedSelectedDateCal.setTime(selectedDate); // selectedDate should already be normalized
        // Double check normalization if there are doubts (though it should be by now)
        // normalizeCalendarToStartOfDay(normalizedSelectedDateCal);

        if (selectedSubjectId.get() == 0) {
            Toast.makeText(getContext(), R.string.select_subject_validation, Toast.LENGTH_LONG).show(); return;
        }
        // This check uses startOfTodayMillis which is midnight of the app's current day.
        // calendarView.getMinDate() should already prevent selection of past days.
        if (normalizedSelectedDateCal.getTimeInMillis() < startOfTomorrowMillis) {
            Toast.makeText(getContext(), R.string.select_valid_date_validation, Toast.LENGTH_LONG).show(); return;
        }
        if (selectedDurationMillis.get() == 0) {
            Toast.makeText(getContext(), R.string.select_duration_validation, Toast.LENGTH_LONG).show(); return;
        }
        if (selectedLocationId == null || selectedLocationId.isEmpty()) {
            Toast.makeText(getContext(), R.string.select_location_validation, Toast.LENGTH_LONG).show(); return;
        }
        // Check if a valid time is selected from the spinner
        if (selectedTimeOffsetMillis.get() == 0) {
            // Check if the spinner has items and the first item isn't a placeholder that results in offset 0
            if (timeSpinner.getSelectedItem() instanceof TimeSpinnerItem) {
                TimeSpinnerItem currentSpinnerItem = (TimeSpinnerItem) timeSpinner.getSelectedItem();
                if (currentSpinnerItem.getTimeOffsetMillis() == 0 &&
                        (currentSpinnerItem.displayTime.equals(getString(R.string.select_duration_first_for_time)) ||
                                currentSpinnerItem.displayTime.equals(getString(R.string.no_available_times)))) {
                    Toast.makeText(getContext(), R.string.select_time_validation, Toast.LENGTH_LONG).show(); return;
                }
                // If offset is legitimately 0 (e.g. OPEN_TIME is midnight), this check needs refinement.
                // For now, assuming offset 0 is only for placeholders.
            } else { // Spinner might be empty or have non-TimeSpinnerItem if something went wrong
                Toast.makeText(getContext(), R.string.select_time_validation, Toast.LENGTH_LONG).show(); return;
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
            Toast.makeText(getContext(),dataManager.getSessionDao().getLastError(), Toast.LENGTH_LONG).show();
        }
    }
}
