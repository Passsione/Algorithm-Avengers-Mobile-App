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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;


public class ScheduleHistoryFragment extends Fragment implements PastSessionAdapter.OnSessionClickListener {

    private RecyclerView recyclerViewPastSessions;
    private PastSessionAdapter pastSessionAdapter;
    private List<Session> allPastSessions;
    private List<Session> filteredSessions;
    private TextView textViewNoPastSessions;

    private AutoCompleteTextView actPartner, actStatus, actSubject;
    private EditText editTextLocationFilter;
    private Button buttonSelectDateFilter, buttonClearFilters;

    private DataManager dataManager;
    private LogInUser currentUser;
    private Calendar selectedDateCalendar = null; // For date filter
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
        buttonClearFilters = view.findViewById(R.id.buttonClearFilters);

        allPastSessions = new ArrayList<>();
        filteredSessions = new ArrayList<>();
        pastSessionAdapter = new PastSessionAdapter(getContext(), filteredSessions, dataManager, currentUser.getUser().getStudentNum(), this);
        recyclerViewPastSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPastSessions.setAdapter(pastSessionAdapter);

        loadPastSessions();
        setupFilters();
    }

    private void loadPastSessions() {
        allPastSessions.clear();
        // Fetch all sessions for the user (as tutor or tutee) that are COMPLETED, CANCELLED, or DECLINED
        List<Session> sessionsAsTutor = dataManager.getSessionDao().getSessionsByTutorId(currentUser.getUser().getStudentNum());
        List<Session> sessionsAsTutee = dataManager.getSessionDao().getSessionsByTuteeId(currentUser.getUser().getStudentNum());

        Set<Integer> addedSessionIds = new HashSet<>(); // To avoid duplicates if a user tutored themselves (unlikely)

        Date now = new Date();
        for (Session s : sessionsAsTutor) {
            if (!addedSessionIds.contains(s.getId()) && s.getEndTime().before(now)) { // Consider only sessions that have ended
                allPastSessions.add(s);
                addedSessionIds.add(s.getId());
            }
        }
        for (Session s : sessionsAsTutee) {
            if (!addedSessionIds.contains(s.getId()) && s.getEndTime().before(now)) {
                allPastSessions.add(s);
                addedSessionIds.add(s.getId());
            }
        }
        // Or fetch specifically by status: COMPLETED, CANCELLED, DECLINED
        // allPastSessions = dataManager.getSessionDao().getPastUserSessions(currentUser.getUser().getStudentNum()); // You'd need this DAO method

        // Sort by date, newest first
        allPastSessions.sort((s1, s2) -> s2.getStartTime().compareTo(s1.getStartTime()));
        applyFilters(); // Initial display
    }

    private void setupFilters() {
        // Populate Partner Spinner
        Set<String> partnerNames = new HashSet<>();
        partnerNames.add("Any Partner"); // Default option
        for (Session session : allPastSessions) {
            int partnerId = (session.getTutorId() == currentUser.getUser().getStudentNum()) ? session.getTuteeId() : session.getTutorId();
            User partner = dataManager.getUserDao().getUserByStudentNum(partnerId);
            if (partner != null) {
                partnerNames.add(partner.getFirstName() + " " + partner.getLastName());
            }
        }
        ArrayAdapter<String> partnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(partnerNames));
        actPartner.setAdapter(partnerAdapter);
        actPartner.setOnItemClickListener((parent, view, position, id) -> applyFilters());

        // Populate Status Spinner
        List<String> statuses = new ArrayList<>();
        statuses.add("Any Status");
        for (Session.Status status : Session.Status.values()) {
            if (status == Session.Status.COMPLETED || status == Session.Status.CANCELLED || status == Session.Status.DECLINED) { // Filter relevant past statuses
                statuses.add(status.name());
            }
        }
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, statuses);
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
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(subjectNames));
        actSubject.setAdapter(subjectAdapter);
        actSubject.setOnItemClickListener((parent, view, position, id) -> applyFilters());

        // Location Filter
        editTextLocationFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Date Filter
        buttonSelectDateFilter.setOnClickListener(v -> showDatePickerDialog());

        // Clear Filters Button
        buttonClearFilters.setOnClickListener(v -> {
            actPartner.setText("Any Partner", false);
            actStatus.setText("Any Status", false);
            actSubject.setText("Any Subject", false);
            editTextLocationFilter.setText("");
            selectedDateCalendar = null;
            buttonSelectDateFilter.setText("Filter by Date: ANY");
            applyFilters();
        });

        // Set initial filter values (optional, if you want them pre-filled)
        actPartner.setText("Any Partner", false);
        actStatus.setText("Any Status", false);
        actSubject.setText("Any Subject", false);
    }


    private void showDatePickerDialog() {
        Calendar cal = selectedDateCalendar != null ? selectedDateCalendar : Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDateCalendar = Calendar.getInstance();
                    selectedDateCalendar.set(year, month, dayOfMonth);
                    buttonSelectDateFilter.setText("Date: " + filterDateFormat.format(selectedDateCalendar.getTime()));
                    applyFilters();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    @SuppressLint("NewApi") // For Stream API, ensure minSDK is appropriate or use loops
    private void applyFilters() {
        String selectedPartnerName = actPartner.getText().toString();
        String selectedStatusName = actStatus.getText().toString();
        String selectedSubjectName = actSubject.getText().toString();
        String locationQuery = editTextLocationFilter.getText().toString().toLowerCase().trim();

        filteredSessions.clear();

        // Use Java 8 streams for filtering if minSDK >= 24, otherwise use loops
        List<Session> tempFiltered = allPastSessions.stream()
                .filter(session -> {
                    // Partner Filter
                    if (!"Any Partner".equals(selectedPartnerName) && selectedPartnerName.length() > 0) {
                        int partnerId = (session.getTutorId() == currentUser.getUser().getStudentNum()) ? session.getTuteeId() : session.getTutorId();
                        User partner = dataManager.getUserDao().getUserByStudentNum(partnerId);
                        if (partner == null || !(partner.getFirstName() + " " + partner.getLastName()).equals(selectedPartnerName)) {
                            return false;
                        }
                    }
                    // Status Filter
                    if (!"Any Status".equals(selectedStatusName) && selectedStatusName.length() > 0) {
                        if (session.getStatus() != Session.Status.valueOf(selectedStatusName)) {
                            return false;
                        }
                    }
                    // Subject Filter
                    if (!"Any Subject".equals(selectedSubjectName) && selectedSubjectName.length() > 0) {
                        Subject subject = dataManager.getSubjectDao().getSubjectById(session.getSubjectId());
                        if (subject == null || !subject.getSubjectName().equals(selectedSubjectName)) {
                            return false;
                        }
                    }
                    // Location Filter
                    if (!locationQuery.isEmpty()) {
                        if (session.getLocation() == null || !session.getLocation().toLowerCase().contains(locationQuery)) {
                            return false;
                        }
                    }
                    // Date Filter
                    if (selectedDateCalendar != null) {
                        Calendar sessionCal = Calendar.getInstance();
                        sessionCal.setTime(session.getStartTime());
                        if (sessionCal.get(Calendar.YEAR) != selectedDateCalendar.get(Calendar.YEAR) ||
                                sessionCal.get(Calendar.DAY_OF_YEAR) != selectedDateCalendar.get(Calendar.DAY_OF_YEAR)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        filteredSessions.addAll(tempFiltered);
        pastSessionAdapter.notifyDataSetChanged(); // Notify adapter with the final filtered list
        updateNoSessionsView();
    }


    private void updateNoSessionsView() {
        if (pastSessionAdapter.getItemCount() == 0) {
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
        args.putSerializable("session", session);
        detailsFragment.setArguments(args);

        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.session_fragment_container, detailsFragment); // Replace R.id.your_main_fragment_container_id with your actual container ID in the activity
        transaction.addToBackStack(null); // So user can press back to return to history
        transaction.commit();
    }
}