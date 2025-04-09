package com.pbdvmobile.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.adapters.TutorAdapter;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.services.UserService;
import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private Spinner spinnerSubject, spinnerRating;
    private EditText etMaxPrice;
    private Button btnSearch;
    private RecyclerView rvTutors;
    private TutorAdapter tutorAdapter;
    private List<User> tutorList;
    private UserService userService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        userService = new UserService();
        tutorList = new ArrayList<>();

        spinnerSubject = view.findViewById(R.id.spinner_subject);
        spinnerRating = view.findViewById(R.id.spinner_rating);
        etMaxPrice = view.findViewById(R.id.et_max_price);
        btnSearch = view.findViewById(R.id.btn_search);
        rvTutors = view.findViewById(R.id.rv_tutors);

        // Setup subjects spinner
        List<Subject> subjectList = userService.getAvailableSubjects();
        List<String> subjectNames = new ArrayList<>();
        subjectNames.add("All Subjects");
        for (Subject subject : subjectList) {
            subjectNames.add(subject.getName());
        }

        ArrayAdapter<String> subjectsAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, subjectNames);
        subjectsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subjectsAdapter);

        // Setup rating spinner
        ArrayAdapter<CharSequence> ratingAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.ratings, android.R.layout.simple_spinner_item);
        ratingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRating.setAdapter(ratingAdapter);

        // Setup recycler view
        tutorAdapter = new TutorAdapter(tutorList, getContext());
        rvTutors.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTutors.setAdapter(tutorAdapter);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedSubject = spinnerSubject.getSelectedItem().toString();
                String selectedRating = spinnerRating.getSelectedItem().toString();
                String maxPriceStr = etMaxPrice.getText().toString().trim();

                double maxPrice = maxPriceStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxPriceStr);
                double minRating = selectedRating.equals("Any Rating") ? 0 : Double.parseDouble(selectedRating.split(" ")[0]);

                // Search tutors
                userService.searchTutors(selectedSubject.equals("All Subjects") ? null : selectedSubject,
                        minRating, maxPrice, new UserService.TutorsCallback() {
                            @Override
                            public void onSuccess(List<User> tutors) {
                                tutorList.clear();
                                tutorList.addAll(tutors);
                                tutorAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onFailure(String error) {
                                // Handle error
                            }
                        });
            }
        });

        // Initial load of tutors
        loadAllTutors();

        return view;
    }

    private void loadAllTutors() {
        userService.getAllTutors(new UserService.TutorsCallback() {
            @Override
            public void onSuccess(List<User> tutors) {
                tutorList.clear();
                tutorList.addAll(tutors);
                tutorAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String error) {
                // Handle error
            }
        });
    }
}