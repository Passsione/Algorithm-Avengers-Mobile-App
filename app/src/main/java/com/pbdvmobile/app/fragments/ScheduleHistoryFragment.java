
package com.pbdvmobile.app.fragments;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.adapter.PastSessionAdapter;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ScheduleHistoryFragment extends Fragment implements PastSessionAdapter.OnSessionClickListener {

    private static final String TAG = "ScheduleHistoryFragment";

    private RecyclerView recyclerViewPastSessions;
    private PastSessionAdapter pastSessionAdapter;
    private List<Session> allPastSessions;
    private List<Session> filteredSessions; // This list is given to the adapter
    private TextView textViewNoPastSessions;

    private AutoCompleteTextView actPartner, actStatus, actSubject;
    private EditText editTextLocationFilter;
    private Button buttonSelectDateFilter, buttonClearFilters, buttonToggleFilters;
    private LinearLayout filterLayout;

    private DataManager dataManager;
    private LogInUser currentUser;
    private Calendar selectedDateCalendar = null;
    private SimpleDateFormat filterDateFormat;

    // Define "Any" constants to avoid string literal typos
    private static final String ANY_PARTNER = "Any Partner";
    private static final String ANY_STATUS = "Any Status";
    private static final String ANY_SUBJECT = "Any Subject";
    private static final String ANY_DATE_BUTTON_TEXT = "Filter by Date: ANY";


    public ScheduleHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataManager = DataManager.getInstance(getContext());
        currentUser = LogInUser.getInstance(dataManager);
        filterDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        // Initialize Views
        recyclerViewPastSessions = view.findViewById(R.id.recyclerViewPastSessions);
        textViewNoPastSessions = view.findViewById(R.id.textViewNoPastSessions);
        actPartner = view.findViewById(R.id.autoCompleteTextViewPartner);
        actStatus = view.findViewById(R.id.autoCompleteTextViewStatus);
        actSubject = view.findViewById(R.id.autoCompleteTextViewSubject);
        editTextLocationFilter = view.findViewById(R.id.editTextLocationFilter);
        buttonSelectDateFilter = view.findViewById(R.id.buttonSelectDateFilter);
        filterLayout = view.findViewById(R.id.filtersView);
        buttonToggleFilters = view.findViewById(R.id.buttonToggleFilters);
        buttonClearFilters = view.findViewById(R.id.buttonClearFilters);

        // Initialize Lists and Adapter
        allPastSessions = new ArrayList<>();
        filteredSessions = new ArrayList<>(); // Adapter uses this list
        pastSessionAdapter = new PastSessionAdapter(getContext(), filteredSessions, dataManager, currentUser.getUser().getStudentNum(), this);
        recyclerViewPastSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPastSessions.setAdapter(pastSessionAdapter);
        recyclerViewPastSessions.setFocusable(false);

        // Load data, then setup filters, then apply initial filters
        loadPastSessions(); // Populates allPastSessions
        setupFilters();     // Sets up UI, populates dropdowns based on allPastSessions, sets default "Any" text
        applyFilters();     // Applies the initial "Any" filters to show all data from allPastSessions
    }

    private void loadPastSessions() {
        allPastSessions.clear();
        List<Session> sessionsAsTutor = dataManager.getSessionDao().getSessionsByTutorId(currentUser.getUser().getStudentNum());
        List<Session> sessionsAsTutee = dataManager.getSessionDao().getSessionsByTuteeId(currentUser.getUser().getStudentNum());

        Set<Integer> addedSessionIds = new HashSet<>();
        Date now = new Date();

        for (Session s : sessionsAsTutor) {
            if (!addedSessionIds.contains(s.getId()) && qualifiesAsPast(s, now)) {
                allPastSessions.add(s);
                addedSessionIds.add(s.getId());
            }
        }
        for (Session s : sessionsAsTutee) {
            if (!addedSessionIds.contains(s.getId()) && qualifiesAsPast(s, now)) {
                allPastSessions.add(s);
                addedSessionIds.add(s.getId());
            }
        }

        allPastSessions.sort((s1, s2) -> s2.getStartTime().compareTo(s1.getStartTime()));
        Log.d(TAG, "loadPastSessions: allPastSessions size = " + allPastSessions.size());
        // applyFilters() is called after setupFilters() in onViewCreated
    }

    private boolean qualifiesAsPast(Session s, Date currentDate) {
        return (s.getEndTime() != null && s.getEndTime().before(currentDate)) ||
                s.getStatus() == Session.Status.COMPLETED ||
                s.getStatus() == Session.Status.CANCELLED ||
                s.getStatus() == Session.Status.DECLINED;
    }

    private void setupFilters() {
        // Populate Partner Spinner
        Set<String> partnerNames = new HashSet<>();
        partnerNames.add(ANY_PARTNER);
        for (Session session : allPastSessions) { // Use allPastSessions to populate
            int partnerId = (session.getTutorId() == currentUser.getUser().getStudentNum()) ? session.getTuteeId() : session.getTutorId();
            User partnerUser = dataManager.getUserDao().getUserByStudentNum(partnerId);
            if (partnerUser != null) {
                partnerNames.add(partnerUser.getFirstName() + " " + partnerUser.getLastName());
            }
        }
        ArrayAdapter<String> partnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(partnerNames));
        actPartner.setAdapter(partnerAdapter);
        actPartner.setOnItemClickListener((parent, view, position, id) -> applyFilters());

        // Populate Status Spinner
        List<String> statuses = new ArrayList<>();
        statuses.add(ANY_STATUS);
        statuses.add(Session.Status.COMPLETED.name());
        statuses.add(Session.Status.CANCELLED.name());
        statuses.add(Session.Status.DECLINED.name());
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses);
        actStatus.setAdapter(statusAdapter);
        actStatus.setOnItemClickListener((parent, view, position, id) -> applyFilters());

        // Populate Subject Spinner
        Set<String> subjectNames = new HashSet<>();
        subjectNames.add(ANY_SUBJECT);
        for (Session session : allPastSessions) { // Use allPastSessions to populate
            Subject subject = dataManager.getSubjectDao().getSubjectById(session.getSubjectId());
            if (subject != null) {
                subjectNames.add(subject.getSubjectName());
            }
        }
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(subjectNames));
        actSubject.setAdapter(subjectAdapter);
        actSubject.setOnItemClickListener((parent, view, position, id) -> applyFilters());

        buttonToggleFilters.setOnClickListener(v -> {
            filterLayout.setVisibility(filterLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            buttonToggleFilters.setText(filterLayout.getVisibility() == View.VISIBLE ? "Close Filters" : "Open Filters");
        });

        editTextLocationFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        buttonSelectDateFilter.setOnClickListener(v -> showDatePickerDialog());

        buttonClearFilters.setOnClickListener(v -> {
            Log.d(TAG, "Clear Filters button clicked.");
            actPartner.setText(ANY_PARTNER, false);
            actStatus.setText(ANY_STATUS, false);
            actSubject.setText(ANY_SUBJECT, false);
            editTextLocationFilter.setText("");
            selectedDateCalendar = null;
            buttonSelectDateFilter.setText(ANY_DATE_BUTTON_TEXT);
            // After setting the text, explicitly call applyFilters
            applyFilters();
        });

        // Set initial text for AutoCompleteTextViews to ensure "Any" state
        actPartner.setText(ANY_PARTNER, false);
        actStatus.setText(ANY_STATUS, false);
        actSubject.setText(ANY_SUBJECT, false);
        buttonSelectDateFilter.setText(ANY_DATE_BUTTON_TEXT);
    }

    private void showDatePickerDialog() {
        Calendar cal = selectedDateCalendar != null ? selectedDateCalendar : Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDateCalendar = Calendar.getInstance();
                    selectedDateCalendar.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDateCalendar.set(Calendar.MILLISECOND, 0);
                    buttonSelectDateFilter.setText("Date: " + filterDateFormat.format(selectedDateCalendar.getTime()));
                    applyFilters();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    @SuppressLint("NewApi") // For Stream API
    private void applyFilters() {
        String selectedPartnerName = actPartner.getText().toString().trim();
        String selectedStatusName = actStatus.getText().toString().trim();
        String selectedSubjectName = actSubject.getText().toString().trim();
        String locationQuery = editTextLocationFilter.getText().toString().toLowerCase().trim();

        Log.d(TAG, "Applying filters:");
        Log.d(TAG, "  Partner: '" + selectedPartnerName + "' (Is Any: " + ANY_PARTNER.equals(selectedPartnerName) + ")");
        Log.d(TAG, "  Status: '" + selectedStatusName + "' (Is Any: " + ANY_STATUS.equals(selectedStatusName) + ")");
        Log.d(TAG, "  Subject: '" + selectedSubjectName + "' (Is Any: " + ANY_SUBJECT.equals(selectedSubjectName) + ")");
        Log.d(TAG, "  Location: '" + locationQuery + "'");
        Log.d(TAG, "  Date: " + (selectedDateCalendar != null ? filterDateFormat.format(selectedDateCalendar.getTime()) : "ANY"));
        Log.d(TAG, "  allPastSessions size before filter: " + allPastSessions.size());

        filteredSessions.clear(); // Clear the list that adapter uses

        if (allPastSessions.isEmpty()) {
            Log.d(TAG, "allPastSessions is empty, no filtering needed.");
            // No need to stream an empty list. updateNoSessionsView will handle it.
        } else {
            List<Session> tempFiltered = allPastSessions.stream()
                    .filter(session -> {
                        boolean pass = true;

                        // Partner Filter
                        if (!ANY_PARTNER.equals(selectedPartnerName) && !selectedPartnerName.isEmpty()) {
                            int partnerId = (session.getTutorId() == currentUser.getUser().getStudentNum()) ? session.getTuteeId() : session.getTutorId();
                            User partnerUser = dataManager.getUserDao().getUserByStudentNum(partnerId);
                            if (partnerUser == null || !(partnerUser.getFirstName() + " " + partnerUser.getLastName()).equals(selectedPartnerName)) {
                                pass = false;
                            }
                        }

                        // Status Filter
                        if (pass && !ANY_STATUS.equals(selectedStatusName) && !selectedStatusName.isEmpty()) {
                            try {
                                if (session.getStatus() != Session.Status.valueOf(selectedStatusName)) {
                                    pass = false;
                                }
                            } catch (IllegalArgumentException e) {
                                Log.e(TAG, "Invalid status string for valueOf: " + selectedStatusName);
                                pass = false;
                            }
                        }

                        // Subject Filter
                        if (pass && !ANY_SUBJECT.equals(selectedSubjectName) && !selectedSubjectName.isEmpty()) {
                            Subject subject = dataManager.getSubjectDao().getSubjectById(session.getSubjectId());
                            if (subject == null || !subject.getSubjectName().equals(selectedSubjectName)) {
                                pass = false;
                            }
                        }

                        // Location Filter
                        if (pass && !locationQuery.isEmpty()) {
                            if (session.getLocation() == null || !session.getLocation().toLowerCase().contains(locationQuery)) {
                                pass = false;
                            }
                        }

                        // Date Filter
                        if (pass && selectedDateCalendar != null) {
                            Calendar sessionCalStart = Calendar.getInstance();
                            sessionCalStart.setTime(session.getStartTime());
                            // Normalize to compare dates only
                            sessionCalStart.set(Calendar.HOUR_OF_DAY, 0);
                            sessionCalStart.set(Calendar.MINUTE, 0);
                            sessionCalStart.set(Calendar.SECOND, 0);
                            sessionCalStart.set(Calendar.MILLISECOND, 0);

                            if (sessionCalStart.get(Calendar.YEAR) != selectedDateCalendar.get(Calendar.YEAR) ||
                                    sessionCalStart.get(Calendar.DAY_OF_YEAR) != selectedDateCalendar.get(Calendar.DAY_OF_YEAR)) {
                                pass = false;
                            }
                        }
                        return pass;
                    })
                    .collect(Collectors.toList());
            filteredSessions.addAll(tempFiltered);
        }

        Log.d(TAG, "  filteredSessions size after filter: " + filteredSessions.size());

        if (pastSessionAdapter != null) {
            pastSessionAdapter.notifyDataSetChanged(); // Notify adapter with the current filteredSessions
        }
        updateNoSessionsView();
    }

    private void updateNoSessionsView() {
        if (filteredSessions.isEmpty()) {
            recyclerViewPastSessions.setVisibility(View.GONE);
            textViewNoPastSessions.setVisibility(View.VISIBLE);
            Log.d(TAG, "updateNoSessionsView: No sessions to show.");
        } else {
            recyclerViewPastSessions.setVisibility(View.VISIBLE);
            textViewNoPastSessions.setVisibility(View.GONE);
            Log.d(TAG, "updateNoSessionsView: Showing " + filteredSessions.size() + " sessions.");
        }
    }

    @Override
    public void onSessionClick(Session session) {
        SessionDetailsFragment detailsFragment = new SessionDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable("session", session);
        detailsFragment.setArguments(args);

        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.session_fragment_container, detailsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}

/*
package com.pbdvmobile.app.fragments;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log; // Import Log for debugging
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
// import android.widget.DatePicker; // Not directly used as a field
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.adapter.PastSessionAdapter;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
// import java.util.Collections; // Not used
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;


public class ScheduleHistoryFragment extends Fragment implements PastSessionAdapter.OnSessionClickListener {

    private static final String TAG = "ScheduleHistoryFragment"; // Tag for logging

    private RecyclerView recyclerViewPastSessions;
    private PastSessionAdapter pastSessionAdapter;
    private List<Session> allPastSessions;
    private List<Session> filteredSessions;
    private TextView textViewNoPastSessions;

    private AutoCompleteTextView actPartner, actStatus, actSubject;
    private EditText editTextLocationFilter;
    private Button buttonSelectDateFilter, buttonClearFilters, buttonToggleFilters;
    private LinearLayout filterLayout;

    private DataManager dataManager;
    private LogInUser currentUser;
    private Calendar selectedDateCalendar = null;
    private SimpleDateFormat filterDateFormat;


    public ScheduleHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataManager = DataManager.getInstance(getContext());
        currentUser = LogInUser.getInstance(dataManager);
        filterDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        recyclerViewPastSessions = view.findViewById(R.id.recyclerViewPastSessions);
        textViewNoPastSessions = view.findViewById(R.id.textViewNoPastSessions);
        actPartner = view.findViewById(R.id.autoCompleteTextViewPartner);
        actStatus = view.findViewById(R.id.autoCompleteTextViewStatus);
        actSubject = view.findViewById(R.id.autoCompleteTextViewSubject);
        editTextLocationFilter = view.findViewById(R.id.editTextLocationFilter);
        buttonSelectDateFilter = view.findViewById(R.id.buttonSelectDateFilter);
        filterLayout = view.findViewById(R.id.filtersView);
        buttonToggleFilters = view.findViewById(R.id.buttonToggleFilters);
        buttonClearFilters = view.findViewById(R.id.buttonClearFilters);

        allPastSessions = new ArrayList<>();
        filteredSessions = new ArrayList<>();
        pastSessionAdapter = new PastSessionAdapter(getContext(), filteredSessions, dataManager, currentUser.getUser().getStudentNum(), this);
        recyclerViewPastSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPastSessions.setAdapter(pastSessionAdapter);
        recyclerViewPastSessions.setFocusable(false);

        loadPastSessions(); // This will populate allPastSessions and then call applyFilters
        setupFilters();     // This will set up filter UI elements and their listeners
    }

    private void loadPastSessions() {
        allPastSessions.clear();
        List<Session> sessionsAsTutor = dataManager.getSessionDao().getSessionsByTutorId(currentUser.getUser().getStudentNum());
        List<Session> sessionsAsTutee = dataManager.getSessionDao().getSessionsByTuteeId(currentUser.getUser().getStudentNum());

        Set<Integer> addedSessionIds = new HashSet<>();
        Date now = new Date();

        for (Session s : sessionsAsTutor) {
            if (!addedSessionIds.contains(s.getId()) && qualifiesAsPast(s, now)) {
                allPastSessions.add(s);
                addedSessionIds.add(s.getId());
            }
        }
        for (Session s : sessionsAsTutee) {
            if (!addedSessionIds.contains(s.getId()) && qualifiesAsPast(s, now)) {
                allPastSessions.add(s);
                addedSessionIds.add(s.getId());
            }
        }

        allPastSessions.sort((s1, s2) -> s2.getStartTime().compareTo(s1.getStartTime()));
        Log.d(TAG, "loadPastSessions: allPastSessions size = " + allPastSessions.size());
        applyFilters(); // Apply filters initially (should show all if filters are "Any")
    }

    private boolean qualifiesAsPast(Session s, Date currentDate) {
        return (s.getEndTime() != null && s.getEndTime().before(currentDate)) ||
                s.getStatus() == Session.Status.COMPLETED ||
                s.getStatus() == Session.Status.CANCELLED ||
                s.getStatus() == Session.Status.DECLINED;
    }

    private void setupFilters() {
        // Populate Partner Spinner
        Set<String> partnerNames = new HashSet<>();
        partnerNames.add("Any Partner");
        for (Session session : allPastSessions) {
            int partnerId = (session.getTutorId() == currentUser.getUser().getStudentNum()) ? session.getTuteeId() : session.getTutorId();
            User partnerUser = dataManager.getUserDao().getUserByStudentNum(partnerId);
            if (partnerUser != null) {
                partnerNames.add(partnerUser.getFirstName() + " " + partnerUser.getLastName());
            }
        }
        ArrayAdapter<String> partnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(partnerNames));
        actPartner.setAdapter(partnerAdapter);
        actPartner.setOnItemClickListener((parent, view, position, id) -> applyFilters());

        // Populate Status Spinner
        List<String> statuses = new ArrayList<>();
        statuses.add("Any Status");
        statuses.add(Session.Status.COMPLETED.name());
        statuses.add(Session.Status.CANCELLED.name());
        statuses.add(Session.Status.DECLINED.name());
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses);
        actStatus.setAdapter(statusAdapter);
        actStatus.setOnItemClickListener((parent, view, position, id) -> applyFilters());

        // Populate Subject Spinner
        Set<String> subjectNames = new HashSet<>();
        subjectNames.add("Any Subject");
        for (Session session : allPastSessions) {
            Subject subject = dataManager.getSubjectDao().getSubjectById(session.getSubjectId());
            if (subject != null) {
                subjectNames.add(subject.getSubjectName());
            }
        }
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(subjectNames));
        actSubject.setAdapter(subjectAdapter);
        actSubject.setOnItemClickListener((parent, view, position, id) -> applyFilters());

        buttonToggleFilters.setOnClickListener(v -> {
            filterLayout.setVisibility(filterLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            buttonToggleFilters.setText(filterLayout.getVisibility() == View.VISIBLE ? "Close Filters" : "Open Filters");
        });

        editTextLocationFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        buttonSelectDateFilter.setOnClickListener(v -> showDatePickerDialog());

        buttonClearFilters.setOnClickListener(v -> {
            Log.d(TAG, "Clear Filters button clicked.");
            actPartner.setText("Any Partner", false);
            actStatus.setText("Any Status", false);
            actSubject.setText("Any Subject", false);
            editTextLocationFilter.setText("");
            selectedDateCalendar = null;
            buttonSelectDateFilter.setText("Filter by Date: ANY");
            applyFilters();
        });

        // Set initial text for AutoCompleteTextViews to ensure they are not empty
        // and to trigger the "Any" logic correctly on first load if needed.
        actPartner.setText("Any Partner", false);
        actStatus.setText("Any Status", false);
        actSubject.setText("Any Subject", false);
    }


    private void showDatePickerDialog() {
        Calendar cal = selectedDateCalendar != null ? selectedDateCalendar : Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDateCalendar = Calendar.getInstance();
                    selectedDateCalendar.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDateCalendar.set(Calendar.MILLISECOND, 0);
                    buttonSelectDateFilter.setText("Date: " + filterDateFormat.format(selectedDateCalendar.getTime()));
                    applyFilters();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    @SuppressLint("NewApi") // For Stream API
    private void applyFilters() {
        // Trim the input from AutoCompleteTextViews to avoid issues with leading/trailing whitespace
        String selectedPartnerName = actPartner.getText().toString().trim();
        String selectedStatusName = actStatus.getText().toString().trim();
        String selectedSubjectName = actSubject.getText().toString().trim();
        String locationQuery = editTextLocationFilter.getText().toString().toLowerCase().trim();

        Log.d(TAG, "Applying filters:");
        Log.d(TAG, "  Partner: '" + selectedPartnerName + "'");
        Log.d(TAG, "  Status: '" + selectedStatusName + "'");
        Log.d(TAG, "  Subject: '" + selectedSubjectName + "'");
        Log.d(TAG, "  Location: '" + locationQuery + "'");
        Log.d(TAG, "  Date: " + (selectedDateCalendar != null ? filterDateFormat.format(selectedDateCalendar.getTime()) : "ANY"));
        Log.d(TAG, "  allPastSessions size before filter: " + allPastSessions.size());


        filteredSessions.clear();

        List<Session> tempFiltered = allPastSessions.stream()
                .filter(session -> {
                    boolean pass = true;

                    // Partner Filter
                    if (!"Any Partner".equals(selectedPartnerName) && !selectedPartnerName.isEmpty()) {
                        int partnerId = (session.getTutorId() == currentUser.getUser().getStudentNum()) ? session.getTuteeId() : session.getTutorId();
                        User partnerUser = dataManager.getUserDao().getUserByStudentNum(partnerId);
                        if (partnerUser == null || !(partnerUser.getFirstName() + " " + partnerUser.getLastName()).equals(selectedPartnerName)) {
                            pass = false;
                        }
                    }

                    // Status Filter
                    if (pass && !"Any Status".equals(selectedStatusName) && !selectedStatusName.isEmpty()) {
                        try {
                            if (session.getStatus() != Session.Status.valueOf(selectedStatusName)) {
                                pass = false;
                            }
                        } catch (IllegalArgumentException e) {
                            // This might happen if selectedStatusName is not a valid enum string,
                            // though with ArrayAdapter it should be.
                            Log.e(TAG, "Invalid status string for valueOf: " + selectedStatusName);
                            pass = false; // Or handle as appropriate
                        }
                    }

                    // Subject Filter
                    if (pass && !"Any Subject".equals(selectedSubjectName) && !selectedSubjectName.isEmpty()) {
                        Subject subject = dataManager.getSubjectDao().getSubjectById(session.getSubjectId());
                        if (subject == null || !subject.getSubjectName().equals(selectedSubjectName)) {
                            pass = false;
                        }
                    }

                    // Location Filter
                    if (pass && !locationQuery.isEmpty()) {
                        if (session.getLocation() == null || !session.getLocation().toLowerCase().contains(locationQuery)) {
                            pass = false;
                        }
                    }

                    // Date Filter
                    if (pass && selectedDateCalendar != null) {
                        Calendar sessionCalStart = Calendar.getInstance();
                        sessionCalStart.setTime(session.getStartTime());
                        sessionCalStart.set(Calendar.HOUR_OF_DAY, 0);
                        sessionCalStart.set(Calendar.MINUTE, 0);
                        sessionCalStart.set(Calendar.SECOND, 0);
                        sessionCalStart.set(Calendar.MILLISECOND, 0);

                        if (sessionCalStart.get(Calendar.YEAR) != selectedDateCalendar.get(Calendar.YEAR) ||
                                sessionCalStart.get(Calendar.DAY_OF_YEAR) != selectedDateCalendar.get(Calendar.DAY_OF_YEAR)) {
                            pass = false;
                        }
                    }
                    return pass;
                })
                .collect(Collectors.toList());

        filteredSessions.addAll(tempFiltered);
        Log.d(TAG, "  filteredSessions size after filter: " + filteredSessions.size());

        if(pastSessionAdapter !=null) {
            pastSessionAdapter.notifyDataSetChanged();
        }
        updateNoSessionsView();
    }


    private void updateNoSessionsView() {
        if (filteredSessions.isEmpty()) {
            recyclerViewPastSessions.setVisibility(View.GONE);
            textViewNoPastSessions.setVisibility(View.VISIBLE);
            Log.d(TAG, "updateNoSessionsView: No sessions to show.");
        } else {
            recyclerViewPastSessions.setVisibility(View.VISIBLE);
            textViewNoPastSessions.setVisibility(View.GONE);
            Log.d(TAG, "updateNoSessionsView: Showing " + filteredSessions.size() + " sessions.");
        }
    }

    @Override
    public void onSessionClick(Session session) {
        SessionDetailsFragment detailsFragment = new SessionDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable("session", session);
        detailsFragment.setArguments(args);

        FragmentManager fragmentManager = getParentFragmentManager(); // Use getParentFragmentManager for fragments
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.session_fragment_container, detailsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
*/
