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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.Timestamp;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.adapter.PastSessionAdapter; // Will need updates for Firebase model
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors; // API 24+

public class ScheduleHistoryFragment extends Fragment implements PastSessionAdapter.OnSessionClickListener {

    private static final String TAG = "ScheduleHistoryFragment";

    private RecyclerView recyclerViewPastSessions;
    private PastSessionAdapter pastSessionAdapter;
    private List<Session> allPastSessions = new ArrayList<>();
    private List<Session> filteredSessions = new ArrayList<>();
    private TextView textViewNoPastSessions;
    private ProgressBar progressBarHistory;

    private AutoCompleteTextView actPartner, actStatus, actSubject;
    private EditText editTextLocationFilter;
    private Button buttonSelectDateFilter, buttonClearFilters, buttonToggleFilters;
    private LinearLayout filterLayout;

    private LogInUser loggedInUser;
    private User currentUserPojo;
    private SessionDao sessionDao;
    private UserDao userDao;
    private SubjectDao subjectDao;

    private Calendar selectedDateCalendar = null;
    private SimpleDateFormat filterDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private static final String ANY_PARTNER = "Any Partner";
    private static final String ANY_STATUS = "Any Status";
    private static final String ANY_SUBJECT = "Any Subject";
    private static final String ANY_DATE_BUTTON_TEXT = "Filter by Date: ANY";

    private List<User> partnerFilterList = new ArrayList<>();
    private List<Subject> subjectFilterList = new ArrayList<>();


    public ScheduleHistoryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null) return;

        loggedInUser = LogInUser.getInstance();
        currentUserPojo = loggedInUser.getUser();
        sessionDao = new SessionDao();
        userDao = new UserDao();
        subjectDao = new SubjectDao();

        initializeViews(view);
        setupRecyclerView();

        if (currentUserPojo == null) {
            Toast.makeText(getContext(), "User data not available.", Toast.LENGTH_LONG).show();
            return;
        }
        loadFilterDataAndThenSessions();
    }

    private void initializeViews(View view) {
        recyclerViewPastSessions = view.findViewById(R.id.recyclerViewPastSessions);
        textViewNoPastSessions = view.findViewById(R.id.textViewNoPastSessions);
        progressBarHistory = view.findViewById(R.id.progressBarScheduleHistory); // Add to XML
        actPartner = view.findViewById(R.id.autoCompleteTextViewPartner);
        actStatus = view.findViewById(R.id.autoCompleteTextViewStatus);
        actSubject = view.findViewById(R.id.autoCompleteTextViewSubject);
        editTextLocationFilter = view.findViewById(R.id.editTextLocationFilter);
        buttonSelectDateFilter = view.findViewById(R.id.buttonSelectDateFilter);
        filterLayout = view.findViewById(R.id.filtersView);
        buttonToggleFilters = view.findViewById(R.id.buttonToggleFilters);
        buttonClearFilters = view.findViewById(R.id.buttonClearFilters);
    }

    private void setupRecyclerView() {
        // Pass current user's UID to adapter if needed for display logic (e.g. "vs. You")
        pastSessionAdapter = new PastSessionAdapter(getContext(), filteredSessions, loggedInUser.getUid(), this);
        recyclerViewPastSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPastSessions.setAdapter(pastSessionAdapter);
        recyclerViewPastSessions.setFocusable(false); // Usually not needed for RecyclerView
    }

    private void showLoading(boolean isLoading) {
        if (progressBarHistory != null) progressBarHistory.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (recyclerViewPastSessions != null) recyclerViewPastSessions.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (textViewNoPastSessions != null && isLoading) textViewNoPastSessions.setVisibility(View.GONE);
    }

    private void loadFilterDataAndThenSessions() {
        showLoading(true);
        // Step 1: Load all subjects for the subject filter
        subjectDao.getAllSubjects().addOnSuccessListener(subjectSnapshots -> {
            subjectFilterList.clear();
            if (subjectSnapshots != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : subjectSnapshots.getDocuments()) {
                    Subject s = doc.toObject(Subject.class);
                    if (s != null) {
                        s.setId(doc.getId());
                        subjectFilterList.add(s);
                    }
                }
            }
            // Step 2: Load all users (potential partners) for the partner filter
            // This could be optimized if we only want users involved in past sessions
            userDao.getAllUsers().addOnSuccessListener(userSnapshots -> {
                partnerFilterList.clear();
                if (userSnapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : userSnapshots.getDocuments()) {
                        User u = doc.toObject(User.class);
                        if (u != null && !u.getUid().equals(currentUserPojo.getUid())) { // Exclude self
                            u.setUid(doc.getId());
                            partnerFilterList.add(u);
                        }
                    }
                }
                setupFilterControls(); // Populate spinners now that data is ready
                loadPastSessions();    // Then load the actual sessions
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load users for filter", e);
                setupFilterControls(); // Still setup filters, might be empty
                loadPastSessions();
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load subjects for filter", e);
            setupFilterControls(); // Still setup filters, might be empty
            loadPastSessions();
        });
    }


    private void setupFilterControls() {
        // Partner Spinner
        List<String> partnerDisplayNames = new ArrayList<>();
        partnerDisplayNames.add(ANY_PARTNER);
        for (User u : partnerFilterList) {
            partnerDisplayNames.add(u.getFirstName() + " " + u.getLastName());
        }
        ArrayAdapter<String> partnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, partnerDisplayNames);
        actPartner.setAdapter(partnerAdapter);
        actPartner.setOnItemClickListener((parent, view, position, id) -> applyFiltersToList());
        actPartner.setText(ANY_PARTNER, false);

        // Status Spinner
        List<String> statuses = new ArrayList<>();
        statuses.add(ANY_STATUS);
        statuses.add(Session.Status.COMPLETED.name());
        statuses.add(Session.Status.CANCELLED.name());
        statuses.add(Session.Status.DECLINED.name());
        // Add other relevant past statuses if necessary
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses);
        actStatus.setAdapter(statusAdapter);
        actStatus.setOnItemClickListener((parent, view, position, id) -> applyFiltersToList());
        actStatus.setText(ANY_STATUS, false);

        // Subject Spinner
        List<String> subjectDisplayNames = new ArrayList<>();
        subjectDisplayNames.add(ANY_SUBJECT);
        for (Subject s : subjectFilterList) {
            subjectDisplayNames.add(s.getSubjectName());
        }
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, subjectDisplayNames);
        actSubject.setAdapter(subjectAdapter);
        actSubject.setOnItemClickListener((parent, view, position, id) -> applyFiltersToList());
        actSubject.setText(ANY_SUBJECT, false);

        // Other filter controls
        buttonToggleFilters.setOnClickListener(v -> {
            filterLayout.setVisibility(filterLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            buttonToggleFilters.setText(filterLayout.getVisibility() == View.VISIBLE ? "Close Filters" : "Open Filters");
        });

        editTextLocationFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFiltersToList(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        buttonSelectDateFilter.setText(ANY_DATE_BUTTON_TEXT);
        buttonSelectDateFilter.setOnClickListener(v -> showDatePickerDialog());

        buttonClearFilters.setOnClickListener(v -> {
            actPartner.setText(ANY_PARTNER, false);
            actStatus.setText(ANY_STATUS, false);
            actSubject.setText(ANY_SUBJECT, false);
            editTextLocationFilter.setText("");
            selectedDateCalendar = null;
            buttonSelectDateFilter.setText(ANY_DATE_BUTTON_TEXT);
            applyFiltersToList();
        });
    }

    private void loadPastSessions() {
        // Fetch sessions where current user is tutee
        sessionDao.getSessionsByTuteeUid(currentUserPojo.getUid(), new SessionDao.SessionsCallback() {
            @Override
            public void onSessionsFetched(List<Session> sessionsAsTutee) {
                allPastSessions.clear(); // Clear before adding from first call
                filterAndAddSessions(sessionsAsTutee);

                // Fetch sessions where current user is tutor
                sessionDao.getSessionsByTutorUid(currentUserPojo.getUid(), new SessionDao.SessionsCallback() {
                    @Override
                    public void onSessionsFetched(List<Session> sessionsAsTutor) {
                        filterAndAddSessions(sessionsAsTutor); // Add to the same list

                        // Sort all collected sessions
                        Collections.sort(allPastSessions, (s1, s2) -> {
                            if (s1.getStartTime() == null && s2.getStartTime() == null) return 0;
                            if (s1.getStartTime() == null) return 1; // nulls last
                            if (s2.getStartTime() == null) return -1;
                            return s2.getStartTime().compareTo(s1.getStartTime()); // Descending
                        });
                        Log.d(TAG, "Total past sessions loaded: " + allPastSessions.size());
                        applyFiltersToList();
                        showLoading(false);
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error fetching sessions as tutor", e);
                        // Still apply filters with what we have from tutee sessions
                        applyFiltersToList();
                        showLoading(false);
                        if(getContext()!=null) Toast.makeText(getContext(), "Error loading tutor sessions.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching sessions as tutee", e);
                showLoading(false);
                if(getContext()!=null) Toast.makeText(getContext(), "Error loading tutee sessions.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterAndAddSessions(List<Session> sessions) {
        Date now = new Date();
        Set<String> existingIds = new HashSet<>();
        for(Session s : allPastSessions) existingIds.add(s.getId());

        for (Session s : sessions) {
            if (s.getId() != null && !existingIds.contains(s.getId())) { // Avoid duplicates
                // A session is "past" if its end time is before now OR its status is terminal (COMPLETED, CANCELLED, DECLINED)
                boolean isTerminalStatus = s.getStatus() == Session.Status.COMPLETED ||
                        s.getStatus() == Session.Status.CANCELLED ||
                        s.getStatus() == Session.Status.DECLINED;
                boolean endTimeInPast = s.getEndTime() != null && s.getEndTime().toDate().before(now);

                if (isTerminalStatus || endTimeInPast) {
                    allPastSessions.add(s);
                    existingIds.add(s.getId());
                }
            }
        }
    }


    private void showDatePickerDialog() {
        Calendar cal = selectedDateCalendar != null ? selectedDateCalendar : Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDateCalendar = Calendar.getInstance();
                    selectedDateCalendar.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDateCalendar.set(Calendar.MILLISECOND, 0);
                    buttonSelectDateFilter.setText("Date: " + filterDateFormat.format(selectedDateCalendar.getTime()));
                    applyFiltersToList();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void applyFiltersToList() {
        String selectedPartnerName = actPartner.getText().toString().trim();
        String selectedStatusName = actStatus.getText().toString().trim();
        String selectedSubjectNameText = actSubject.getText().toString().trim();
        String locationQuery = editTextLocationFilter.getText().toString().toLowerCase(Locale.ROOT).trim();

        filteredSessions.clear();

        for (Session session : allPastSessions) {
            boolean pass = true;

            // Partner Filter
            if (!ANY_PARTNER.equals(selectedPartnerName) && !selectedPartnerName.isEmpty()) {
                String partnerUid = session.getTutorUid().equals(currentUserPojo.getUid()) ? session.getTuteeUid() : session.getTutorUid();
                User partner = findUserInList(partnerUid, partnerFilterList);
                if (partner == null || !(partner.getFirstName() + " " + partner.getLastName()).equals(selectedPartnerName)) {
                    pass = false;
                }
            }

            // Status Filter
            if (pass && !ANY_STATUS.equals(selectedStatusName) && !selectedStatusName.isEmpty()) {
                try {
                    if (session.getStatus() != Session.Status.valueOf(selectedStatusName)) pass = false;
                } catch (IllegalArgumentException e) { pass = false; }
            }

            // Subject Filter
            if (pass && !ANY_SUBJECT.equals(selectedSubjectNameText) && !selectedSubjectNameText.isEmpty()) {
                if (session.getSubjectName() == null || !session.getSubjectName().equals(selectedSubjectNameText)) {
                    pass = false;
                }
            }

            // Location Filter
            if (pass && !locationQuery.isEmpty()) {
                if (session.getLocation() == null || !session.getLocation().toLowerCase(Locale.ROOT).contains(locationQuery)) {
                    pass = false;
                }
            }

            // Date Filter
            if (pass && selectedDateCalendar != null && session.getStartTime() != null) {
                Calendar sessionCalStart = Calendar.getInstance();
                sessionCalStart.setTime(session.getStartTime().toDate());
                normalizeCalendarDate(sessionCalStart);
                if (sessionCalStart.get(Calendar.YEAR) != selectedDateCalendar.get(Calendar.YEAR) ||
                        sessionCalStart.get(Calendar.DAY_OF_YEAR) != selectedDateCalendar.get(Calendar.DAY_OF_YEAR)) {
                    pass = false;
                }
            }
            if (pass) filteredSessions.add(session);
        }

        pastSessionAdapter.notifyDataSetChanged();
        updateNoSessionsView();
    }

    private User findUserInList(String uid, List<User> userList) {
        for (User u : userList) {
            if (u.getUid().equals(uid)) return u;
        }
        return null;
    }

    private void normalizeCalendarDate(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }


    private void updateNoSessionsView() {
        if (filteredSessions.isEmpty() && !progressBarHistory.isShown()) {
            recyclerViewPastSessions.setVisibility(View.GONE);
            textViewNoPastSessions.setVisibility(View.VISIBLE);
        } else {
            recyclerViewPastSessions.setVisibility(View.VISIBLE);
            textViewNoPastSessions.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSessionClick(Session session) {
        SessionDetailsFragment detailsFragment = new SessionDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable("session_id", session.getId()); // Pass session ID
        detailsFragment.setArguments(args);

        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.session_fragment_container, detailsFragment); // Ensure this container ID is correct
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUserPojo != null && allPastSessions != null && !progressBarHistory.isShown()) {
            Log.d(TAG, "onResume: Refreshing schedule history.");
            loadPastSessions(); // Reloads all data and reapplies filters
        }
    }
}
