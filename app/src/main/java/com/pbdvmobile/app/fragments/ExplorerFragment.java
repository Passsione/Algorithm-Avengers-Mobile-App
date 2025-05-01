package com.pbdvmobile.app.fragments;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
    Button apply_filter;
    SearchView searchView;


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

        List<User> tutors = dataManager.getUserDao().getAllTutors();
        results = view.findViewById(R.id.tutor_list);

        // ---- Start - Search and filter ----

        List<Integer> itemsPositions = new ArrayList<>(Arrays.asList(0, 0));
        // --- Open / close filter section ---
        Button filter = view.findViewById(R.id.btnFilter);
        LinearLayout filterLayout = view.findViewById(R.id.filter_section);
        filter.setOnClickListener(l ->{
            filterLayout.setVisibility(filterLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            filter.setText(filterLayout.getVisibility() == View.VISIBLE ? "Close" : "Filters");
        });
        // --- Rating controls ---


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
        // Optional: Handle item selection
        subject_search.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                itemsPositions.set(0, position);
                String selectedItem = parent.getItemAtPosition(position).toString();
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
        // Optional: Handle item selection
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

//        searchView = view.findViewById(R.id.search_view);
        apply_filter = view.findViewById(R.id.apply_filters);
        AtomicBoolean filtered = new AtomicBoolean(false);
        apply_filter.setOnClickListener(l -> {
            filtered.set(true);

            display_tutors(tutors, itemsPositions);
        });
        if(!filtered.get())display_tutors(tutors, itemsPositions);
    }

    private void display_tutors(List<User> tutors, List<Integer> search_criteria) {
        results.removeAllViews();
        for(User tutor : tutors){
            if(tutor.getStudentNum() == current_user.getUser().getStudentNum()) continue;
            int subject = search_criteria.get(0);
            int education = search_criteria.get(1);

            if(subject != 0) {
                UserSubject c_subject = dataManager.getSubjectDao().getUserSubjects(current_user.getUser().getStudentNum()).get(subject - 1);
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

            if(education != 0) {
                if(tutor.getEducationLevel() != User.EduLevel.values()[education - 1]) continue;
            }
            // checks for common subjects
//            if(!match_student(tutor)) continue;

            LinearLayout tutorCard = new LinearLayout(getContext());
            tutorCard.setOrientation(LinearLayout.HORIZONTAL);
            // Set LayoutParams for the parent (e.g., fill width, wrap height)
            LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            tutorCard.setLayoutParams(parentParams);
            tutorCard.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

            tutorCard.setBackgroundColor(getResources().getColor(R.color.primary_light));

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
            double stars = tutor.getAverageRating();
            tutorRating.setRating(stars < 0 ? 0f : (float)stars);
            detailsLayout.addView(tutorRating);

            // Create TextView for tutor subjects
            TextView tutorSubjects = new TextView(getContext());
            LinearLayout.LayoutParams subjectsParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            subjectsParams.topMargin = dpToPx(4);
            tutorSubjects.setLayoutParams(subjectsParams);
            tutorSubjects.setText("Mathematics, Physics");
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
            requestSessionButton.setOnClickListener(l ->{
                Intent i = new Intent(getContext(), ScheduleActivity.class);
                i.putExtra("tutor", tutor);
                startActivity(i);
            });
            requestSessionButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

            tutorCard.addView(detailsLayout);
            tutorCard.addView(requestSessionButton);

            results.addView(tutorCard);
        }
    }

    private boolean match_student(User tutor) {

        List<UserSubject> c_subjects = dataManager.getSubjectDao().getUserSubjects(current_user.getUser().getStudentNum());
        List<UserSubject> t_subjects = dataManager.getSubjectDao().getUserSubjects(tutor.getStudentNum());

        for(UserSubject t_subject : t_subjects){
            if((!t_subject.getTutoring())) continue;
            for(UserSubject c_subject : c_subjects){
                if(t_subject.getSubjectId() == c_subject.getSubjectId()) return true;
            }
        }
        return false;
    }


    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
        // A simpler way to convert dp to pixels
        return Math.round(dp * displayMetrics.density);
    }
}