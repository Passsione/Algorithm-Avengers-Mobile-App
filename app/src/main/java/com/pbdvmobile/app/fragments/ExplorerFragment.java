package com.pbdvmobile.app.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pbdvmobile.app.LogInActivity;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.ScheduleActivity;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.dao.SessionDao; // For fetching ratings if not on User object
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors; // Requires API 24+

public class ExplorerFragment extends Fragment {
    private static final String TAG = "ExplorerFragment";

    private LogInUser loggedInUser;
    private UserDao userDao;
    private SubjectDao subjectDao;
    private SessionDao sessionDao; // For ratings if needed

    private LinearLayout resultsLinearLayout;
    private EditText searchViewEditText;
    private Button applyFilterButton, clearRatingButton, btnToggleFilterSection;
    private LinearLayout filterLayout;
    private RatingBar ratingBarFilter;
    private Spinner subjectSpinnerFilter, educationSpinnerFilter;
    private ProgressBar progressBar;

    private List<User> allTutorsList = new ArrayList<>();
    private List<Subject> allSubjectsList = new ArrayList<>(); // For mapping IDs to names if needed for display

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private String currentSearchQuery = "";
    private String selectedSubjectIdFilter = null; // Store Subject Document ID
    private User.EduLevel selectedEduLevelFilter = null;
    private int minRatingFilter = 0;

    private User currentUserPojo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explorer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null) return;

        loggedInUser = LogInUser.getInstance();
        userDao = new UserDao();
        subjectDao = new SubjectDao();
        sessionDao = new SessionDao(); // Initialize if fetching ratings separately

        currentUserPojo = loggedInUser.getUser();
        if (currentUserPojo == null) {
            // Fallback: if LogInUser singleton doesn't have it, try to fetch
            FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
            if (fbUser != null) {
                userDao.getUserByUid(fbUser.getUid()).addOnSuccessListener(doc -> {
                    if (doc.exists()) currentUserPojo = doc.toObject(User.class);
                    // Proceed with setup after current user is confirmed or fails
                    initializeFragment(view);
                }).addOnFailureListener(e -> initializeFragment(view)); // Proceed even if fetch fails
            } else {
                // Should not happen if auth check is done before reaching here
                Toast.makeText(getContext(), "User not authenticated.", Toast.LENGTH_SHORT).show();
                // Redirect to login
                Intent toLogin = new Intent(getContext(), LogInActivity.class);
                toLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(toLogin);
                if (getActivity() != null) getActivity().finishAffinity();
                return;
            }
        } else {
            initializeFragment(view);
        }
    }

    private void initializeFragment(View view) {
        resultsLinearLayout = view.findViewById(R.id.tutor_list);
        searchViewEditText = view.findViewById(R.id.search_view);
        btnToggleFilterSection = view.findViewById(R.id.btnFilter);
        filterLayout = view.findViewById(R.id.filter_section);
        ratingBarFilter = view.findViewById(R.id.rating_filter);
        clearRatingButton = view.findViewById(R.id.clear_rating);
        subjectSpinnerFilter = view.findViewById(R.id.subject_spinner);
        educationSpinnerFilter = view.findViewById(R.id.education_level_spinner);
        applyFilterButton = view.findViewById(R.id.apply_filters);
        progressBar = view.findViewById(R.id.explorer_progress_bar); // Add ProgressBar to your XML

        setupSearchView();
        setupFilterControls();
        loadInitialData();
    }


    private void setupSearchView() {
        searchViewEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = ExplorerFragment.this::applyFiltersAndDisplayTutors;
                searchHandler.postDelayed(searchRunnable, 500);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterControls() {
        btnToggleFilterSection.setOnClickListener(l -> {
            filterLayout.setVisibility(filterLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            btnToggleFilterSection.setText(filterLayout.getVisibility() == View.VISIBLE ? "Close Filters" : "Show Filters");
        });

        ratingBarFilter.setOnRatingBarChangeListener((rb, rating, fromUser) -> {
            if (fromUser) minRatingFilter = (int) rating;
        });

        clearRatingButton.setOnClickListener(l -> {
            minRatingFilter = 0;
            ratingBarFilter.setRating(0);
            // applyFiltersAndDisplayTutors(); // Optionally apply immediately
        });

        // Education Level Spinner
        List<String> eduLevelsDisplay = new ArrayList<>();
        eduLevelsDisplay.add("Any Level"); // Index 0
        for (User.EduLevel level : User.EduLevel.values()) {
            eduLevelsDisplay.add(level.name().replace("_", " "));
        }
        ArrayAdapter<String> eduAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, eduLevelsDisplay);
        eduAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        educationSpinnerFilter.setAdapter(eduAdapter);
        educationSpinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedEduLevelFilter = null;
                } else {
                    selectedEduLevelFilter = User.EduLevel.values()[position - 1];
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { selectedEduLevelFilter = null; }
        });

        applyFilterButton.setOnClickListener(l -> applyFiltersAndDisplayTutors());
    }

    private void loadInitialData() {
        showLoading(true);
        // Fetch all subjects for the filter spinner and for mapping IDs to names
        subjectDao.getAllSubjects().addOnSuccessListener(queryDocumentSnapshots -> {
            allSubjectsList.clear();
            if (queryDocumentSnapshots != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    Subject subject = doc.toObject(Subject.class);
                    if (subject != null) {
                        subject.setId(doc.getId());
                        allSubjectsList.add(subject);
                    }
                }
            }
            populateSubjectSpinnerFilter();
            // After subjects are loaded, load tutors
            loadAllTutors();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching subjects", e);
            Toast.makeText(getContext(), "Could not load subject filters.", Toast.LENGTH_SHORT).show();
            loadAllTutors(); // Still try to load tutors
        });
    }

    private void populateSubjectSpinnerFilter() {
        List<String> subjectNamesDisplay = new ArrayList<>();
        subjectNamesDisplay.add("Any Subject"); // Index 0

        // Requires API 24+ for streams, or use a loop
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            subjectNamesDisplay.addAll(allSubjectsList.stream().map(Subject::getSubjectName).collect(Collectors.toList()));
        } else {
            for (Subject s : allSubjectsList) {
                subjectNamesDisplay.add(s.getSubjectName());
            }
        }

        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, subjectNamesDisplay);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinnerFilter.setAdapter(subjectAdapter);
        subjectSpinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0 || allSubjectsList.isEmpty()) { // "Any Subject" or no subjects loaded
                    selectedSubjectIdFilter = null;
                } else {
                    // Ensure position-1 is a valid index for allSubjectsList
                    if (position -1 < allSubjectsList.size()) {
                        selectedSubjectIdFilter = allSubjectsList.get(position - 1).getId();
                    } else {
                        selectedSubjectIdFilter = null; // Should not happen if lists are in sync
                        Log.e(TAG, "Subject spinner position out of bounds for allSubjectsList.");
                    }
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { selectedSubjectIdFilter = null; }
        });
    }

    private void loadAllTutors() {
        userDao.getAllTutors().addOnSuccessListener(queryDocumentSnapshots -> {
            allTutorsList.clear();
            if (queryDocumentSnapshots != null) {
                for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    User tutor = document.toObject(User.class);
                    if (tutor != null) {
                        tutor.setUid(document.getId());
                        // Exclude current user if they are also a tutor
                        if (currentUserPojo != null && tutor.getUid().equals(currentUserPojo.getUid())) {
                            continue;
                        }
                        allTutorsList.add(tutor);
                    }
                }
            }
            Log.d(TAG, "Fetched " + allTutorsList.size() + " tutors.");
            applyFiltersAndDisplayTutors();
            showLoading(false);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching tutors", e);
            if(getContext() != null) Toast.makeText(getContext(), "Error loading tutors.", Toast.LENGTH_SHORT).show();
            applyFiltersAndDisplayTutors(); // Update UI (e.g., show "no results")
            showLoading(false);
        });
    }

    private void applyFiltersAndDisplayTutors() {
        List<User> filteredTutors = new ArrayList<>();
        for (User tutor : allTutorsList) {
            // Name Filter
            if (!currentSearchQuery.isEmpty()) {
                String tutorFullName = (tutor.getFirstName() + " " + tutor.getLastName()).toLowerCase(Locale.getDefault());
                if (!tutorFullName.contains(currentSearchQuery)) {
                    continue;
                }
            }
            // Subject Filter
            if (selectedSubjectIdFilter != null) {
                if (tutor.getTutoredSubjectIds() == null || !tutor.getTutoredSubjectIds().contains(selectedSubjectIdFilter)) {
                    continue;
                }
            }
            // Education Level Filter
            if (selectedEduLevelFilter != null) {
                if (tutor.getEducationLevel() == null || tutor.getEducationLevel() != selectedEduLevelFilter) {
                    continue;
                }
            }
            // Rating Filter (using averageRatingAsTutor from User POJO)
            if (minRatingFilter > 0) {
                if (tutor.getAverageRatingAsTutor() < minRatingFilter) {
                    continue;
                }
            }
            filteredTutors.add(tutor);
        }
        displayTutorCards(filteredTutors);
    }

    @SuppressLint("SetTextI18n")
    private void displayTutorCards(List<User> tutorsToDisplay) {
        resultsLinearLayout.removeAllViews();
        if (getContext() == null) return;

        if (tutorsToDisplay.isEmpty()) {
            TextView noMatch = new TextView(getContext());
            noMatch.setText("No tutors match your criteria.");
            noMatch.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            noMatch.setGravity(Gravity.CENTER);
            resultsLinearLayout.addView(noMatch);
            return;
        }

        for (User tutor : tutorsToDisplay) {
            LinearLayout tutorCard = new LinearLayout(getContext());
            tutorCard.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int marginInPx = dpToPx(8);
            parentParams.setMargins(marginInPx, 0, marginInPx, marginInPx);
            tutorCard.setLayoutParams(parentParams);
            tutorCard.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            tutorCard.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primary_light)); // Use ContextCompat

            ImageView tutorImage = new ImageView(getContext());
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dpToPx(80), dpToPx(80));
            tutorImage.setLayoutParams(imageParams);
            Glide.with(getContext())
                    .load(tutor.getProfileImageUrl()).placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round).circleCrop().into(tutorImage);
            tutorImage.setContentDescription(tutor.getFirstName() + " profile picture");
            tutorCard.addView(tutorImage);

            LinearLayout detailsLayout = new LinearLayout(getContext());
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            detailsParams.setMarginStart(dpToPx(12));
            detailsLayout.setLayoutParams(detailsParams);
            detailsLayout.setOrientation(LinearLayout.VERTICAL);

            TextView tutorNameTextView = new TextView(getContext());
            tutorNameTextView.setText(tutor.getFirstName() + " " + tutor.getLastName());
            tutorNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tutorNameTextView.setTypeface(null, Typeface.BOLD);
            detailsLayout.addView(tutorNameTextView);

            RatingBar cardTutorRatingBar = new RatingBar(getContext(), null, android.R.attr.ratingBarStyleSmall);
            LinearLayout.LayoutParams ratingParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ratingParams.topMargin = dpToPx(4);
            cardTutorRatingBar.setLayoutParams(ratingParams);
            cardTutorRatingBar.setNumStars(5);
            cardTutorRatingBar.setStepSize(0.5f);
            if (tutor.getAverageRatingAsTutor() > 0) {
                cardTutorRatingBar.setRating((float) tutor.getAverageRatingAsTutor());
                detailsLayout.addView(cardTutorRatingBar);
            } else {
                TextView notRated = new TextView(getContext());
                notRated.setText("Not rated yet");
                notRated.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                detailsLayout.addView(notRated);
            }

            TextView tutorSubjectsTextView = new TextView(getContext());
            if (tutor.getTutoredSubjectIds() != null && !tutor.getTutoredSubjectIds().isEmpty()) {
                List<String> subjectNames = new ArrayList<>();
                for(String subjId : tutor.getTutoredSubjectIds()){
                    // Find subject name from allSubjectsList
                    for(Subject s : allSubjectsList){
                        if(s.getId() != null && s.getId().equals(subjId)){
                            subjectNames.add(s.getSubjectName().split(":")[0]); // Get part before colon
                            break;
                        }
                    }
                }
                tutorSubjectsTextView.setText("Teaches: " + TextUtils.join(", ", subjectNames));
            } else {
                tutorSubjectsTextView.setText("Subjects not specified");
            }
            detailsLayout.addView(tutorSubjectsTextView);

            TextView tutorEducationTextView = new TextView(getContext());
            if (tutor.getEducationLevel() != null) {
                tutorEducationTextView.setText("Education: " + tutor.getEducationLevel().name().replace("_"," "));
            } else {
                tutorEducationTextView.setText("Education: N/A");
            }
            detailsLayout.addView(tutorEducationTextView);
            tutorCard.addView(detailsLayout);

            Button requestButton = new Button(getContext());
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            buttonParams.gravity = Gravity.CENTER_VERTICAL;
            requestButton.setLayoutParams(buttonParams);
            requestButton.setText("Request");
            requestButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            requestButton.setOnClickListener(l -> {
                Intent i = new Intent(getContext(), ScheduleActivity.class);
                i.putExtra("tutor", tutor);
                i.putExtra("job_type", "create_session");
                startActivity(i);
            });
            tutorCard.addView(requestButton);
            resultsLinearLayout.addView(tutorCard);
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (resultsLinearLayout != null) {
            resultsLinearLayout.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(dp * displayMetrics.density);
    }
}
