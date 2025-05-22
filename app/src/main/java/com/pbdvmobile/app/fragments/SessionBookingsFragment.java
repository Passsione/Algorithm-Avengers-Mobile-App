package com.pbdvmobile.app.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentReference;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.PartnerProfileActivity;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.Schedule.TimeSlot; // Ensure this class is compatible
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference; // For holding subject ID

public class SessionBookingsFragment extends Fragment {

    private static final String TAG = "SessionBookingsFrag";

    private DataManager dataManager;
    private LogInUser loggedInUser;
    private UserDao userDao;
    private SubjectDao subjectDao;
    private SessionDao sessionDao;

    private TextView tutorNameDisplay, tutorSubjectsDisplayHint; // Renamed for clarity
    private Spinner locationSpinner;
    private RatingBar tutorRatingBar;
    private ImageView tutorProfileImage;
    private Button viewProfileButton, cancelButton, submitButton;
    private RadioGroup subjectsRadioGroup, durationRadioGroup;
    private Spinner timeSpinner;
    private CalendarView calendarView;

    private long startOfTomorrowMillis;
    private Date selectedCalendarDate = new Date(); // Normalized to midnight of the selected day

    private final String[] locations = {"Steve Biko Library", "ML Sultan Library"};
    private String selectedLocationId;

    private AtomicReference<String> selectedSubjectId = new AtomicReference<>(); // Firestore Subject Document ID
    private String selectedSubjectName; // For denormalizing in Session
    private int selectedDurationMillis = 0;
    private int selectedTimeOffsetMillis = 0; // Offset from midnight for the selected time slot

    private User currentTutorPojo;
    private User currentTuteePojo;

    private List<TimeSlot> tutorTakenSlots = new ArrayList<>();
    private List<Subject> commonSubjectsForRadioGroup = new ArrayList<>();


    private static class TimeSpinnerItem {
        private final String displayTime;
        private final int timeOffsetMillis; // Offset from midnight

        public TimeSpinnerItem(String displayTime, int timeOffsetMillis) {
            this.displayTime = displayTime;
            this.timeOffsetMillis = timeOffsetMillis;
        }
        public int getTimeOffsetMillis() { return timeOffsetMillis; }
        @NonNull @Override public String toString() { return displayTime; }
    }
    private List<TimeSpinnerItem> availableTimeSpinnerItems = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_bookings, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() == null) return;

        dataManager = DataManager.getInstance(getContext());
        loggedInUser = LogInUser.getInstance();
        userDao = new UserDao();
        subjectDao = new SubjectDao();
        sessionDao = new SessionDao();

        currentTuteePojo = loggedInUser.getUser();
        if (currentTuteePojo == null) {
            Toast.makeText(getContext(), "Error: Current user data not found.", Toast.LENGTH_LONG).show();
            if (getActivity() != null) getActivity().finish();
            return;
        }

        assert getArguments() != null;
        currentTutorPojo = (User) getArguments().getSerializable("tutor");
        // String tutorSubjectsDisplayArg = getArguments().getString("subjects"); // This might be less reliable now

        if (currentTutorPojo == null) {
            Toast.makeText(getContext(), "Error: Tutor data not found.", Toast.LENGTH_LONG).show();
            if (getActivity() != null) getActivity().finish();
            return;
        }

        initializeViews(view);
        setupCalendarView();
        populateTutorInfo(view);
        fetchTutorAvailabilityAndCommonSubjects(); // This will then populate radio groups and time spinner

        populateDurationRadioGroup(); // Durations are static
        createLocationDropDown();     // Locations are static
        setupTimeSpinnerListener();

        cancelButton.setOnClickListener(l -> {
            if (getActivity() != null) getActivity().finish();
        });
        submitButton.setOnClickListener(l -> handleSubmitBooking());
    }

    private void initializeViews(View view) {
        tutorSubjectsDisplayHint = view.findViewById(R.id.session_booking_tutor_subjects); // Or a new TextView for actual subjects
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

    private void setupCalendarView() {
        Calendar tomorrowCal = Calendar.getInstance();
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1);
        normalizeCalendarToStartOfDay(tomorrowCal);
        startOfTomorrowMillis = tomorrowCal.getTimeInMillis();
        calendarView.setMinDate(startOfTomorrowMillis);

        Calendar initialCalendarDate = Calendar.getInstance();
        initialCalendarDate.setTimeInMillis(calendarView.getDate());
        normalizeCalendarToStartOfDay(initialCalendarDate);

        if (initialCalendarDate.getTimeInMillis() < startOfTomorrowMillis) {
            selectedCalendarDate.setTime(startOfTomorrowMillis);
            calendarView.setDate(startOfTomorrowMillis);
        } else {
            selectedCalendarDate.setTime(initialCalendarDate.getTimeInMillis());
        }
        Log.d(TAG, "Initial selectedCalendarDate (normalized): " + selectedCalendarDate.toString());

        calendarView.setOnDateChangeListener((cv, year, month, dayOfMonth) -> {
            Calendar newlySelectedCalendar = Calendar.getInstance();
            newlySelectedCalendar.set(year, month, dayOfMonth);
            normalizeCalendarToStartOfDay(newlySelectedCalendar);

            if (newlySelectedCalendar.getTimeInMillis() < startOfTomorrowMillis) {
                selectedCalendarDate.setTime(startOfTomorrowMillis);
                cv.setDate(startOfTomorrowMillis);
            } else {
                selectedCalendarDate.setTime(newlySelectedCalendar.getTimeInMillis());
            }
            Log.d(TAG, "Date selected via listener (normalized): " + selectedCalendarDate.toString());
            populateTimeSpinner(); // Repopulate time spinner for the new date
        });
    }

    private void normalizeCalendarToStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void populateTutorInfo(View fragmentView) {
        tutorNameDisplay.setText(String.format("%s %s", currentTutorPojo.getFirstName(), currentTutorPojo.getLastName()));
        Glide.with(requireContext())
                .load(currentTutorPojo.getProfileImageUrl())
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .circleCrop()
                .into(tutorProfileImage);

        // Fetch and display tutor's average rating as a tutor
        sessionDao.getAverageRatingByFirebaseUid(currentTutorPojo.getUid(), new SessionDao.RatingsCallback() {
            @Override
            public void onRatingsFetched(double averageRatingAsTutee, double averageRatingAsTutor) {
                if (averageRatingAsTutor > 0) {
                    tutorRatingBar.setRating((float) averageRatingAsTutor);
                    tutorRatingBar.setVisibility(View.VISIBLE);
                } else {
                    tutorRatingBar.setVisibility(View.GONE);
                    // Optionally show "Not rated yet"
                    TextView notRatedTextView = new TextView(getContext());
                    notRatedTextView.setText(R.string.not_rated_yet);
                    notRatedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    // Add this text view to the layout if needed
                }
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching tutor ratings", e);
                tutorRatingBar.setVisibility(View.GONE);
            }
        });


        viewProfileButton.setOnClickListener(l -> {
            Intent toProfile = new Intent(getActivity(), PartnerProfileActivity.class);
            toProfile.putExtra("tutor", currentTutorPojo); // User POJO should be Serializable
            startActivity(toProfile);
        });
    }

    private void fetchTutorAvailabilityAndCommonSubjects() {
        // 1. Fetch tutor's taken time slots
        sessionDao.getActiveTimeSlotsForUser(currentTutorPojo.getUid(), new SessionDao.TimeSlotsCallback() {
            @Override
            public void onTimeSlotsFetched(List<TimeSlot> timeSlots) {
                tutorTakenSlots.clear();
                tutorTakenSlots.addAll(timeSlots);
                Log.d(TAG, "Fetched " + tutorTakenSlots.size() + " taken slots for tutor " + currentTutorPojo.getUid());
                // After fetching availability, populate common subjects and then the time spinner
                findCommonSubjectsAndPopulateRadio();
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching tutor's taken time slots.", e);
                Toast.makeText(getContext(), "Could not fetch tutor schedule.", Toast.LENGTH_SHORT).show();
                // Still try to populate subjects, but time spinner might be inaccurate or empty
                findCommonSubjectsAndPopulateRadio();
            }
        });
    }

    private void findCommonSubjectsAndPopulateRadio() {
        commonSubjectsForRadioGroup.clear();
        List<String> tutorSubjectIds = currentTutorPojo.getTutoredSubjectIds();
        List<String> tuteeSubjectIds = currentTuteePojo.getTutoredSubjectIds(); // Or all subjects tutee is enrolled in

        if (tutorSubjectIds == null || tutorSubjectIds.isEmpty()) {
            displayNoCommonSubjects();
            return;
        }

        // For simplicity, let's assume tutee can request any subject the tutor tutors.
        // A more complex logic would find actual "common" subjects if tutees also have a list of subjects they *need* tutoring for.
        // Here, we'll list all subjects the tutor offers.

        final int[] subjectsToFetch = {tutorSubjectIds.size()};
        final ArrayList<Subject> fetchedSubjects = new ArrayList<>();

        if (subjectsToFetch[0] == 0) {
            displayNoCommonSubjects();
            return;
        }

        for (String subjId : tutorSubjectIds) {
            subjectDao.getSubjectById(subjId).addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Subject subject = documentSnapshot.toObject(Subject.class);
                    if (subject != null) {
                        subject.setId(documentSnapshot.getId()); // Set the Firestore ID
                        fetchedSubjects.add(subject);
                    }
                }
                subjectsToFetch[0]--;
                if (subjectsToFetch[0] == 0) { // All fetched
                    commonSubjectsForRadioGroup.addAll(fetchedSubjects);
                    populateSubjectsRadioGroup();
                    populateTimeSpinner(); // Now that subjects might be selected, and availability is known.
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching subject details for ID: " + subjId, e);
                subjectsToFetch[0]--;
                if (subjectsToFetch[0] == 0) {
                    commonSubjectsForRadioGroup.addAll(fetchedSubjects); // Add what we got
                    populateSubjectsRadioGroup();
                    populateTimeSpinner();
                }
            });
        }
    }


    private void populateSubjectsRadioGroup() {
        subjectsRadioGroup.removeAllViews();
        selectedSubjectId.set(null);
        selectedSubjectName = null;

        if (commonSubjectsForRadioGroup.isEmpty()) {
            displayNoCommonSubjects();
            return;
        }

        boolean isFirstSubject = true;
        for (Subject subject : commonSubjectsForRadioGroup) {
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setText(subject.getSubjectName());
            radioButton.setId(View.generateViewId());
            radioButton.setOnClickListener(l -> {
                selectedSubjectId.set(subject.getId());
                selectedSubjectName = subject.getSubjectName();
                Log.d(TAG, "Subject selected: " + subject.getSubjectName() + " (ID: " + subject.getId() + ")");
                // Optionally repopulate time spinner if subject choice affects availability (unlikely here)
            });
            subjectsRadioGroup.addView(radioButton);

            if (isFirstSubject) {
                radioButton.setChecked(true);
                selectedSubjectId.set(subject.getId());
                selectedSubjectName = subject.getSubjectName();
                isFirstSubject = false;
                Log.d(TAG, "Default subject selected: " + subject.getSubjectName() + " (ID: " + subject.getId() + ")");
            }
        }
        // Update hint about what subjects are shown
        if (tutorSubjectsDisplayHint != null) {
            tutorSubjectsDisplayHint.setText("Tutor offers the following subjects:");
        }
    }

    private void displayNoCommonSubjects() {
        subjectsRadioGroup.removeAllViews();
        TextView noSubjects = new TextView(getContext());
        noSubjects.setText(R.string.no_common_subjects_tutor);
        subjectsRadioGroup.addView(noSubjects);
        if (tutorSubjectsDisplayHint != null) {
            tutorSubjectsDisplayHint.setText("Tutor does not offer any subjects you can book at the moment.");
        }
        populateTimeSpinner(); // Populate with "select subject first" or similar
    }


    private void populateDurationRadioGroup() {
        durationRadioGroup.removeAllViews();
        selectedDurationMillis = 0;

        int[] minutes = {30, 60, 120};
        String[] labels = {"30 minutes", "1 hour", "2 hours"};
        boolean isFirstDuration = true;

        for (int i = 0; i < minutes.length; i++) {
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setText(labels[i]);
            radioButton.setId(View.generateViewId());
            final int durationValueMillis = minutes[i] * 60 * 1000;
            radioButton.setOnClickListener(l -> {
                selectedDurationMillis = durationValueMillis;
                Log.d(TAG, "Duration selected: " + durationValueMillis + "ms");
                populateTimeSpinner(); // Repopulate time spinner for new duration
            });
            durationRadioGroup.addView(radioButton);
            if (isFirstDuration) {
                radioButton.setChecked(true);
                selectedDurationMillis = durationValueMillis;
                isFirstDuration = false;
            }
        }
    }

    private void populateTimeSpinner() {
        selectedTimeOffsetMillis = 0;
        availableTimeSpinnerItems.clear();

        if (getContext() == null || currentTutorPojo == null) {
            Log.e(TAG, "Context or Tutor is null. Cannot populate time spinner.");
            setupEmptyTimeSpinner("Error loading tutor data");
            return;
        }
        if (selectedSubjectId.get() == null) {
            setupEmptyTimeSpinner(getString(R.string.select_subject_first_for_time));
            return;
        }
        if (selectedDurationMillis == 0) {
            setupEmptyTimeSpinner(getString(R.string.select_duration_first_for_time));
            return;
        }
        timeSpinner.setEnabled(true);

        // Business hours: 8 AM to 6 PM (18:00)
        final int OPEN_HOUR = 8;
        final int CLOSE_HOUR = 18; // Sessions can end at 18:00, so last start time depends on duration

        // Slot checking interval (e.g., every 30 minutes)
        final int SLOT_INTERVAL_MINUTES = 30;
        final long slotIntervalMillis = SLOT_INTERVAL_MINUTES * 60 * 1000;

        Calendar slotCandidate = Calendar.getInstance();
        slotCandidate.setTime(selectedCalendarDate); // Already normalized to midnight
        slotCandidate.set(Calendar.HOUR_OF_DAY, OPEN_HOUR);
        slotCandidate.set(Calendar.MINUTE, 0);

        Calendar dayEndLimit = Calendar.getInstance();
        dayEndLimit.setTime(selectedCalendarDate);
        dayEndLimit.set(Calendar.HOUR_OF_DAY, CLOSE_HOUR);
        dayEndLimit.set(Calendar.MINUTE, 0);

        Date now = new Date(); // To prevent booking past slots on the current day

        while (slotCandidate.getTimeInMillis() < dayEndLimit.getTimeInMillis()) {
            Date proposedStartTime = slotCandidate.getTime();
            Date proposedEndTime = new Date(proposedStartTime.getTime() + selectedDurationMillis);

            // Check if start time is in the past (for today)
            if (selectedCalendarDate.equals(normalizeDate(now)) && proposedStartTime.before(now)) {
                slotCandidate.add(Calendar.MILLISECOND, (int) slotIntervalMillis);
                continue;
            }

            // Check if proposed session ends after closing time
            if (proposedEndTime.after(dayEndLimit.getTime())) {
                break; // No more valid slots for this duration today
            }

            TimeSlot candidateSlotForCheck = new TimeSlot(proposedStartTime, proposedEndTime);
            boolean isOverlapping = false;
            for (TimeSlot taken : tutorTakenSlots) {
                // Ensure taken slot is on the same day as the proposed slot
                if (isSameDay(taken.getActualStartTime(), proposedStartTime)) {
                    if (candidateSlotForCheck.overlaps(taken)) {
                        isOverlapping = true;
                        break;
                    }
                }
            }

            if (!isOverlapping) {
                String displayTime = dataManager.formatDateTime(proposedStartTime.toString())[1]; // HH:mm format
                Calendar midnight = Calendar.getInstance();
                midnight.setTime(selectedCalendarDate); // Normalized date
                long offset = proposedStartTime.getTime() - midnight.getTimeInMillis();
                availableTimeSpinnerItems.add(new TimeSpinnerItem(displayTime, (int) offset));
            }
            slotCandidate.add(Calendar.MILLISECOND, (int) slotIntervalMillis);
        }


        if (availableTimeSpinnerItems.isEmpty()) {
            availableTimeSpinnerItems.add(new TimeSpinnerItem(getString(R.string.no_available_times), 0));
        }

        ArrayAdapter<TimeSpinnerItem> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, availableTimeSpinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(adapter);

        if (!availableTimeSpinnerItems.isEmpty() && availableTimeSpinnerItems.get(0).getTimeOffsetMillis() != 0) {
            timeSpinner.setSelection(0, false);
            selectedTimeOffsetMillis = availableTimeSpinnerItems.get(0).getTimeOffsetMillis();
        } else {
            selectedTimeOffsetMillis = 0; // Placeholder or no valid time
        }
        Log.d(TAG, "Finished populating time spinner. Items: " + availableTimeSpinnerItems.size());
    }

    private void setupEmptyTimeSpinner(String message) {
        availableTimeSpinnerItems.clear();
        availableTimeSpinnerItems.add(new TimeSpinnerItem(message, 0));
        ArrayAdapter<TimeSpinnerItem> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, availableTimeSpinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(adapter);
        timeSpinner.setEnabled(false);
    }


    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private Date normalizeDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        normalizeCalendarToStartOfDay(cal);
        return cal.getTime();
    }


    private void setupTimeSpinnerListener() {
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TimeSpinnerItem selectedItem = (TimeSpinnerItem) parent.getItemAtPosition(position);
                if (!selectedItem.displayTime.equals(getString(R.string.select_duration_first_for_time)) &&
                        !selectedItem.displayTime.equals(getString(R.string.no_available_times)) &&
                        !selectedItem.displayTime.equals(getString(R.string.select_subject_first_for_time))) {
                    selectedTimeOffsetMillis = selectedItem.getTimeOffsetMillis();
                    Log.d(TAG, "Time selected via spinner: offset " + selectedItem.getTimeOffsetMillis());
                } else {
                    selectedTimeOffsetMillis = 0;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { selectedTimeOffsetMillis = 0; }
        });
    }

    private void createLocationDropDown() {
        selectedLocationId = null;
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, locations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);
        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLocationId = parent.getItemAtPosition(position).toString();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { selectedLocationId = null; }
        });
        if (locations.length > 0) {
            locationSpinner.setSelection(0);
            selectedLocationId = locations[0];
        }
    }

    private void handleSubmitBooking() {
        if (selectedSubjectId.get() == null) {
            Toast.makeText(getContext(), R.string.select_subject_validation, Toast.LENGTH_LONG).show(); return;
        }
        if (selectedCalendarDate.before(normalizeDate(new Date())) && !isSameDay(selectedCalendarDate, normalizeDate(new Date()))) { // Allow today, but not past days
            Toast.makeText(getContext(), R.string.select_valid_date_validation, Toast.LENGTH_LONG).show(); return;
        }
        if (selectedDurationMillis == 0) {
            Toast.makeText(getContext(), R.string.select_duration_validation, Toast.LENGTH_LONG).show(); return;
        }
        if (selectedLocationId == null || selectedLocationId.isEmpty()) {
            Toast.makeText(getContext(), R.string.select_location_validation, Toast.LENGTH_LONG).show(); return;
        }
        if (selectedTimeOffsetMillis == 0 && availableTimeSpinnerItems.size() > 0 &&
                (availableTimeSpinnerItems.get(0).displayTime.equals(getString(R.string.no_available_times)) ||
                        availableTimeSpinnerItems.get(0).displayTime.equals(getString(R.string.select_duration_first_for_time)) ||
                        availableTimeSpinnerItems.get(0).displayTime.equals(getString(R.string.select_subject_first_for_time))
                )
        ) {
            Toast.makeText(getContext(), R.string.select_time_validation, Toast.LENGTH_LONG).show(); return;
        }
        if (timeSpinner.getSelectedItemPosition() == AdapterView.INVALID_POSITION ||
                ((TimeSpinnerItem)timeSpinner.getSelectedItem()).getTimeOffsetMillis() == 0 &&
                        ( ((TimeSpinnerItem)timeSpinner.getSelectedItem()).displayTime.equals(getString(R.string.no_available_times)) ||
                                ((TimeSpinnerItem)timeSpinner.getSelectedItem()).displayTime.equals(getString(R.string.select_duration_first_for_time)) ||
                                ((TimeSpinnerItem)timeSpinner.getSelectedItem()).displayTime.equals(getString(R.string.select_subject_first_for_time))
                        )
        ){
            Toast.makeText(getContext(), R.string.select_time_validation, Toast.LENGTH_LONG).show(); return;
        }


        Calendar finalSessionStartCalendar = Calendar.getInstance();
        finalSessionStartCalendar.setTime(selectedCalendarDate); // Base: selected date (normalized to 00:00)
        finalSessionStartCalendar.add(Calendar.MILLISECOND, selectedTimeOffsetMillis);

        Date actualSessionStartTimeJavaUtil = finalSessionStartCalendar.getTime();
        Date actualSessionEndTimeJavaUtil = new Date(actualSessionStartTimeJavaUtil.getTime() + selectedDurationMillis);

        // Prevent booking if start time is in the past for today
        if (isSameDay(selectedCalendarDate, normalizeDate(new Date())) && actualSessionStartTimeJavaUtil.before(new Date())) {
            Toast.makeText(getContext(), "Cannot book a session in the past.", Toast.LENGTH_LONG).show();
            return;
        }


        Log.d(TAG, "Attempting to book session: " +
                "TuteeUID: " + currentTuteePojo.getUid() +
                ", TutorUID: " + currentTutorPojo.getUid() +
                ", SubjectID: " + selectedSubjectId.get() + " (" + selectedSubjectName + ")" +
                ", Start: " + actualSessionStartTimeJavaUtil +
                ", End: " + actualSessionEndTimeJavaUtil +
                ", Location: " + selectedLocationId);

        submitButton.setEnabled(false); // Prevent double clicks

        sessionDao.requestSession(
                currentTuteePojo.getUid(),
                currentTutorPojo.getUid(),
                selectedSubjectId.get(),
                selectedSubjectName, // Pass denormalized subject name
                selectedLocationId,
                actualSessionStartTimeJavaUtil,
                actualSessionEndTimeJavaUtil,
                new SessionDao.SessionRequestListener() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(getContext(), R.string.session_request_success, Toast.LENGTH_LONG).show();
                        if (getActivity() != null) getActivity().finish();
                    }
                    @Override
                    public void onFailure(String errorMessage, Exception e) {
                        Toast.makeText(getContext(), "Booking failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Session request failed: " + errorMessage, e);
                        submitButton.setEnabled(true);
                    }
                }
        );
    }
}
