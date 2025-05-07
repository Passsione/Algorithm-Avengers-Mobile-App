package com.pbdvmobile.app.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputLayout;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.ScheduleActivity;
import com.pbdvmobile.app.fragments.SessionDetailsFragment; // Ensure this activity exists
import com.pbdvmobile.app.adapters.SessionHistoryAdapter;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ScheduleHistoryFragment extends Fragment implements SessionHistoryAdapter.OnRatingButtonClickListener {

    private static final String TAG = "ScheduleHistoryFrag";

    private RecyclerView rvSessionHistory;
    private SessionHistoryAdapter adapter;
    private List<Session> sessionList = new ArrayList<>();
    private TextView tvNoSessionsHistory;

    private AutoCompleteTextView actvPartnerFilter, actvSubjectFilter, actvStatusFilter;
    private TextInputLayout tilPartnerFilter;
    private Button btnStartDateFilter, btnEndDateFilter, btnClearFilters;

    private DataManager dataManager;
    private User currentUser;
    private boolean isCurrentUserTutor;

    private List<User> partnerList = new ArrayList<>();
    private List<Subject> subjectList = new ArrayList<>();

    // Selected filter values
    private User selectedPartner = null;
    private Subject selectedSubject = null;
    private Session.Status selectedStatus = null;
    private Date selectedStartDate = null;
    private Date selectedEndDate = null;

    private final SimpleDateFormat filterDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());


    public ScheduleHistoryFragment() {
        // Required empty public constructor
    }

    public static ScheduleHistoryFragment newInstance() { // Simplified newInstance
        return new ScheduleHistoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataManager = DataManager.getInstance(getContext());
        currentUser = LogInUser.getInstance(dataManager).getUser();
        isCurrentUserTutor = currentUser.isTutor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findViews(view);
        setupRecyclerView();
        populateFilterSpinners();
        setupFilterListeners();
        loadHistoricalSessions(); // Load initial data
    }

    private void findViews(View view) {
        rvSessionHistory = view.findViewById(R.id.rvSessionHistory);
        tvNoSessionsHistory = view.findViewById(R.id.tvNoSessionsHistory);
        actvPartnerFilter = view.findViewById(R.id.actvPartnerFilter);
        tilPartnerFilter = view.findViewById(R.id.tilPartnerFilter);
        actvSubjectFilter = view.findViewById(R.id.actvSubjectFilter);
        actvStatusFilter = view.findViewById(R.id.actvStatusFilter);
        btnStartDateFilter = view.findViewById(R.id.btnStartDateFilter);
        btnEndDateFilter = view.findViewById(R.id.btnEndDateFilter);
        btnClearFilters = view.findViewById(R.id.btnClearFilters);
    }

    private void setupRecyclerView() {
        adapter = new SessionHistoryAdapter(getContext(), new ArrayList<>(), this);
        rvSessionHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSessionHistory.setAdapter(adapter);
    }

    private void populateFilterSpinners() {
        // Partner Filter
        partnerList = dataManager.getSessionDao().getDistinctPartnersForHistory(currentUser.getStudentNum(), isCurrentUserTutor);
        List<String> partnerNames = new ArrayList<>();
        partnerNames.add("All Partners"); // Option for no filter
        for (User user : partnerList) {
            partnerNames.add(user.getFirstName() + " " + user.getLastName());
        }
        ArrayAdapter<String> partnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, partnerNames);
        actvPartnerFilter.setAdapter(partnerAdapter);
        tilPartnerFilter.setHint(isCurrentUserTutor ? "Filter by Tutee" : "Filter by Tutor");

        // Subject Filter
        subjectList = dataManager.getSessionDao().getDistinctSubjectsForHistory(currentUser.getStudentNum(), isCurrentUserTutor);
        List<String> subjectNames = new ArrayList<>();
        subjectNames.add("All Subjects");
        for (Subject subject : subjectList) {
            subjectNames.add(subject.getSubjectName());
        }
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, subjectNames);
        actvSubjectFilter.setAdapter(subjectAdapter);

        // Status Filter
        List<String> statusNames = new ArrayList<>();
        statusNames.add("All Statuses");
        statusNames.add(Session.Status.COMPLETED.name());
        statusNames.add(Session.Status.CANCELLED.name());
        statusNames.add(Session.Status.DECLINED.name());
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, statusNames);
        actvStatusFilter.setAdapter(statusAdapter);
    }

    private void setupFilterListeners() {
        actvPartnerFilter.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) { // "All Partners"
                selectedPartner = null;
            } else {
                selectedPartner = partnerList.get(position - 1); // Adjust for "All"
            }
            loadHistoricalSessions();
        });

        actvSubjectFilter.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) { // "All Subjects"
                selectedSubject = null;
            } else {
                selectedSubject = subjectList.get(position - 1); // Adjust for "All"
            }
            loadHistoricalSessions();
        });

        actvStatusFilter.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) { // "All Statuses"
                selectedStatus = null;
            } else {
                String statusName = (String) parent.getItemAtPosition(position);
                selectedStatus = Session.Status.valueOf(statusName);
            }
            loadHistoricalSessions();
        });

        btnStartDateFilter.setOnClickListener(v -> showDatePickerDialog(true));
        btnEndDateFilter.setOnClickListener(v -> showDatePickerDialog(false));

        btnClearFilters.setOnClickListener(v -> clearFiltersAndReload());
    }

    private void showDatePickerDialog(final boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        if (isStartDate && selectedStartDate != null) {
            calendar.setTime(selectedStartDate);
        } else if (!isStartDate && selectedEndDate != null) {
            calendar.setTime(selectedEndDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    if (isStartDate) {
                        selectedStartDate = selectedCal.getTime();
                        btnStartDateFilter.setText("Start: " + filterDateFormat.format(selectedStartDate));
                    } else {
                        selectedEndDate = selectedCal.getTime();
                        btnEndDateFilter.setText("End: " + filterDateFormat.format(selectedEndDate));
                    }
                    loadHistoricalSessions();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void clearFiltersAndReload() {
        selectedPartner = null;
        selectedSubject = null;
        selectedStatus = null;
        selectedStartDate = null;
        selectedEndDate = null;

        actvPartnerFilter.setText("", false); // Clear selection text
        actvSubjectFilter.setText("", false);
        actvStatusFilter.setText("", false);

        btnStartDateFilter.setText("Start Date: All");
        btnEndDateFilter.setText("End Date: All");

        // Reset dropdown hints if they changed
        actvPartnerFilter.clearFocus();
        actvSubjectFilter.clearFocus();
        actvStatusFilter.clearFocus();


        loadHistoricalSessions();
    }


    private void loadHistoricalSessions() {
        Log.d(TAG, "Loading historical sessions...");
        int partnerId = (selectedPartner != null) ? selectedPartner.getStudentNum() : 0;
        int subjectIdVal = (selectedSubject != null) ? selectedSubject.getSubjectId() : 0;

        sessionList = dataManager.getSessionDao().getHistoricalSessions(
                currentUser.getStudentNum(),
                isCurrentUserTutor,
                partnerId,
                subjectIdVal,
                selectedStatus,
                selectedStartDate,
                selectedEndDate
        );

        adapter.updateSessions(sessionList);

        if (sessionList.isEmpty()) {
            tvNoSessionsHistory.setVisibility(View.VISIBLE);
            rvSessionHistory.setVisibility(View.GONE);
        } else {
            tvNoSessionsHistory.setVisibility(View.GONE);
            rvSessionHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRatingButtonClick(int sessionId) {
        // Navigate to SessionDetailsActivity to leave a rating
        // This assumes SessionDetailsActivity can handle receiving a session ID
        // and provides a UI for rating.
        Log.d(TAG, "Leave rating clicked for session ID: " + sessionId);
        Intent intent = new Intent(getContext(), ScheduleActivity.class);
        intent.putExtra("job_type", "session_history");
        intent.putExtra("SESSION_ID_FOR_RATING", sessionId); // Use a clear key
        startActivity(intent);
        // Optionally, you might want to refresh this list if a rating changes session display
        // or if SessionDetailsActivity returns a result.
    }
}