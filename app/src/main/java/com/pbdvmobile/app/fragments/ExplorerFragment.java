package com.pbdvmobile.app.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
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
import android.widget.RatingBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import com.pbdvmobile.app.LogInActivity;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.ScheduleActivity;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.UserSubject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class ExplorerFragment extends Fragment {
    DataManager dataManager;
    LogInUser current_user;
    LinearLayout results;
    Button apply_filter, clearRating;
    EditText searchView;

    RatingBar ratingBar;
    List<User> tutors; // Make tutors an instance variable if needed across methods
    List<Integer> itemsPositions; // Make itemsPositions an instance variable

//    // Handler for debouncing search input
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private String currentSearchQuery = ""; // Store the latest query
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_explorer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataManager = DataManager.getInstance(getContext());
        current_user = LogInUser.getInstance(dataManager);

        // ---- Go to log-in page if not logged in
        if(!current_user.isLoggedIn()){
            Intent toLogin = new Intent(getContext(), LogInActivity.class);
            startActivity(toLogin);
        }

        tutors = dataManager.getUserDao().getAllTutors();
        results = view.findViewById(R.id.tutor_list);

        // ---- Start - Search and filter ----

        // --- Search tutors by name ---
        searchView = view.findViewById(R.id.search_view); // Connect to your SearchView XML element
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This method is called AS the text is changing
                final String query = s.toString();
                currentSearchQuery = query; // Update the current query state

                // Debounce mechanism (same as before)

                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> display_tutors(tutors, itemsPositions, query);
                searchHandler.postDelayed(searchRunnable, 300); // 300ms delay
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed here (logic moved to onTextChanged with debounce)
            }
        });


        itemsPositions = new ArrayList<>(Arrays.asList(0, 0, -1));

        // --- Open / close filter section ---
        Button filter = view.findViewById(R.id.btnFilter);
        LinearLayout filterLayout = view.findViewById(R.id.filter_section);
        filter.setOnClickListener(l ->{
            filterLayout.setVisibility(filterLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            filter.setText(filterLayout.getVisibility() == View.VISIBLE ? "Close" : "Filters");
        });

        // --- Rating controls ---
        ratingBar = view.findViewById(R.id.rating_filter);
//        ratingBar.getRating();
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            itemsPositions.set(2, (int)rating);
        });
        clearRating = view.findViewById(R.id.clear_rating);
        clearRating.setOnClickListener(l-> {
            itemsPositions.set(2, -1);
            ratingBar.setRating(0);
        });


        // --- Subjects drop down ---
        Spinner subject_search = view.findViewById(R.id.subject_spinner);

        List<UserSubject> subjects = dataManager.getSubjectDao().getUserSubjects(current_user.getUser().getStudentNum());
        List<String> subjectsName = new ArrayList<>();
        subjectsName.add("All");
        for(UserSubject subject : subjects){
                int subId = subject.getSubjectId();
                subjectsName.add(dataManager.getSubjectDao().getSubjectById(subId).getSubjectName());
            }
        ArrayAdapter<String> subjectsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                subjectsName
        );
        // Specify dropdown layout style
        subjectsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter
        subject_search.setAdapter(subjectsAdapter);
        // Handle item selection
        subject_search.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                itemsPositions.set(0, position);
//                String selectedItem = parent.getItemAtPosition(position).toString();
//                Toast.makeText(getContext(), "Selected: " + selectedItem, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                itemsPositions.set(0, 0);

            }
        });


        // --- Education level drop down ---
        Spinner education_search = view.findViewById(R.id.education_level_spinner);

        List<User.EduLevel> eduLevels = Arrays.stream(User.EduLevel.values()).collect(Collectors.toList());
        List<String> eduLevelsName = new ArrayList<>();
        eduLevelsName.add("All");
        for(User.EduLevel level : eduLevels){
                eduLevelsName.add(level.name());
            }
        ArrayAdapter<String> eduLevelsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                eduLevelsName
        );
        // Specify dropdown layout style
        eduLevelsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter
        education_search.setAdapter(eduLevelsAdapter);
        // Handle item selection
        education_search.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                itemsPositions.set(1, position);
                String selectedItem = parent.getItemAtPosition(position).toString();
//                Toast.makeText(getContext(), "Selected: " + selectedItem, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                itemsPositions.set(1, 0);

            }
        });



        // --- Apply filter button ---
        apply_filter = view.findViewById(R.id.apply_filters);
        AtomicBoolean filtered = new AtomicBoolean(false);
        apply_filter.setOnClickListener(l -> {
            filtered.set(true);
            currentSearchQuery = searchView.getText().toString();
            display_tutors(tutors, itemsPositions, currentSearchQuery);
        });
        if(!filtered.get()) {
            display_tutors(tutors, itemsPositions, currentSearchQuery);
        }
    }

    @SuppressLint("SetTextI18n")
    private void display_tutors(List<User> tutors, List<Integer> search_criteria, String searchQuery) {
        results.removeAllViews();

        // Convert search query to lowercase for case-insensitive comparison
        String lowerCaseSearchQuery = searchQuery.toLowerCase();
        for(User tutor : tutors){
            if(tutor.getStudentNum() == current_user.getUser().getStudentNum()) continue;
            int subject = Integer.parseInt(String.valueOf(search_criteria.get(0)));
            int education = Integer.parseInt(String.valueOf(search_criteria.get(1)));
            double rating = search_criteria.get(2);


            // Apply Search Filter (by subject)
            UserSubject c_subject = null;
            if(subject != 0) {
                c_subject = dataManager.getSubjectDao().getUserSubjects(current_user.getUser().getStudentNum()).get(subject - 1);
                List<UserSubject> t_subjects = dataManager.getSubjectDao().getUserSubjects(tutor.getStudentNum());
                boolean found = false;
                for(UserSubject t : t_subjects) {
                    if ((t.getTutoring()) && (t.getSubjectId() == c_subject.getSubjectId())) {
                        found = true;
                        break;
                    }
                }
                if(!found)continue;
            }

            // Apply Search Filter (by education)
            if(education != 0) {
                if(tutor.getEducationLevel() != User.EduLevel.values()[education - 1]) continue;
            }

            // Apply Search Filter (by rating)
            if(rating >= 0) {
                if(tutor.getAverageRating(dataManager)[1] < rating) continue;
            }

            // Apply Search View Filter (by name)
            String tutorFullName = (tutor.getFirstName() + tutor.getLastName()).toLowerCase();
            if (!lowerCaseSearchQuery.isEmpty() && !tutorFullName.contains(lowerCaseSearchQuery)) continue;


            LinearLayout tutorCard = new LinearLayout(getContext());
            tutorCard.setOrientation(LinearLayout.HORIZONTAL);
            // Set LayoutParams for the parent (e.g., fill width, wrap height)
            LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            // ***** ADD MARGIN HERE *****
            // Add a bottom margin to create a gap between cards.
            // You can adjust the dp value (e.g., 8dp) to your preference.
            int bottomMarginInPx = dpToPx(8); // Or whatever gap you want
            parentParams.setMargins(0, 0, 0, bottomMarginInPx);

            tutorCard.setLayoutParams(parentParams);
            tutorCard.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            tutorCard.setBackgroundColor(getResources().getColor(R.color.primary_light, null));

            // Create ImageView for tutor image
            ImageView tutorImage = new ImageView(getContext());
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    dpToPx(80),
                    dpToPx(80));
            tutorImage.setLayoutParams(imageParams);
            tutorImage.setImageResource(R.mipmap.ic_launcher); // Replace with your actual drawable resource
            tutorImage.setContentDescription("Tutor profile picture");
            tutorCard.addView(tutorImage);

            // Create LinearLayout for tutor details
            LinearLayout detailsLayout = new LinearLayout(getContext());
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            detailsParams.weight = 1;
            detailsParams.setMarginStart(dpToPx(12));
            detailsLayout.setLayoutParams(detailsParams);
            detailsLayout.setOrientation(LinearLayout.VERTICAL);

            // Create TextView for tutor name
            TextView tutorName = new TextView(getContext());
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            tutorName.setLayoutParams(nameParams);
            tutorName.setText(tutor.getFirstName() + " " + tutor.getLastName());
            tutorName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tutorName.setTypeface(null, Typeface.BOLD);
            detailsLayout.addView(tutorName);

            // Create RatingBar for tutor rating
            RatingBar tutorRating = new RatingBar(getContext(), null, android.R.attr.ratingBarStyleSmall);
            LinearLayout.LayoutParams ratingParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            ratingParams.topMargin = dpToPx(4);
            tutorRating.setLayoutParams(ratingParams);
            tutorRating.setNumStars(5);
            tutorRating.setStepSize(0.5f);
            double stars = dataManager.getSessionDao().getAverageRatingByStudentNum(tutor.getStudentNum())[1];
            if(stars > 0){
                tutorRating.setRating((float)stars);
                detailsLayout.addView(tutorRating);
            }else{
                tutorRating.setVisibility(View.GONE);
                TextView not_rating = new TextView(getContext());
                not_rating.setText("Not rated yet");
                not_rating.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                detailsLayout.addView(not_rating);
            }

            // Create TextView for tutor subjects
            TextView tutorSubjects = new TextView(getContext());
            LinearLayout.LayoutParams subjectsParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            subjectsParams.topMargin = dpToPx(4);
            tutorSubjects.setLayoutParams(subjectsParams);

            String subjects = "";
            if(subject == 0)
                for(UserSubject sub : dataManager.getSubjectDao().getUserSubjects(tutor.getStudentNum())) {
                    if(!sub.getTutoring()) continue;
                    String subjectName = dataManager.getSubjectDao().getSubjectById(sub.getSubjectId()).getSubjectName();
                    subjects += subjectName.split(": ")[0] +", ";
                }
            else subjects = dataManager.getSubjectDao().getSubjectById(c_subject.getSubjectId()).getSubjectName();

            tutorSubjects.setText(subjects);
            detailsLayout.addView(tutorSubjects);


            // Create TextView for tutor education
            TextView tutorEducation = new TextView(getContext());
            LinearLayout.LayoutParams educationParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            educationParams.topMargin = dpToPx(4);
            tutorEducation.setLayoutParams(educationParams);
            tutorEducation.setText(tutor.getEducationLevel().name());
            detailsLayout.addView(tutorEducation);

            // Create Button for requesting session
            Button requestSessionButton = new Button(getContext());
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            buttonParams.gravity = Gravity.BOTTOM;
            requestSessionButton.setLayoutParams(buttonParams);
            requestSessionButton.setText("Request");
            requestSessionButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            requestSessionButton.setOnClickListener(l -> {
                Intent i = new Intent(getContext(), ScheduleActivity.class);
                String subjectsExtra = "";
                for(UserSubject sub : dataManager.getSubjectDao().getUserSubjects(tutor.getStudentNum())) {
                    if(!sub.getTutoring()) continue;
                    String subjectName = dataManager.getSubjectDao().getSubjectById(sub.getSubjectId()).getSubjectName();
                    subjectsExtra += subjectName.split(": ")[0] +", ";
                }
                i.putExtra("tutor", tutor);
                i.putExtra("subjects", subjectsExtra);
                i.putExtra("job_type", "create_session");
                startActivity(i);
            });

            tutorCard.addView(detailsLayout);
            tutorCard.addView(requestSessionButton);

            results.addView(tutorCard);
        }

        if(results.getChildCount() == 0){
            TextView text = new TextView(getContext());
            text.setText("No matches found");
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            results.addView(text);
        }
    }


    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
        // A simpler way to convert dp to pixels
        return Math.round(dp * displayMetrics.density);
    }
}