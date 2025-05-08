package com.pbdvmobile.app.fragments;

import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pbdvmobile.app.R;
import com.pbdvmobile.app.ResourceUploadActivity;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.Session;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ResourcesFragment extends Fragment {

    private static final String ALL_TUTORS = "All Tutors"; // Constant for filter option
    private static final String MY_RESOURCES = "My resources"; // Constant for filter option

    DataManager dataManager;
    LogInUser current_user;
    private Spinner tutorSpinner, subjectSpinner;
    private LinearLayout mathResourcesContainer;
    String tutorName;
    private ImageButton toggleMathButton;
    // Add containers/buttons for other subjects if needed

    private List<Resource> allResources = new ArrayList<>(); // Store all fetched resources
    private List<Resource>  myResources = new ArrayList<>(); // Store tutors fetched resources
    private List<String> tutorList = new ArrayList<>(); // Store tutor names for spinner

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_resources, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Null check for context
        if (getContext() == null) {
            return;
        }

        dataManager = DataManager.getInstance(getContext());
        current_user = LogInUser.getInstance(dataManager);


        // Find Views
        tutorSpinner = view.findViewById(R.id.tutor_spinner);
        subjectSpinner = view.findViewById(R.id.resource_subject_spinner);
        mathResourcesContainer = view.findViewById(R.id.subject_container);
        toggleMathButton = view.findViewById(R.id.toggle_math_button);


        // --- Load Data (Replace with your actual DataManager calls) ---
        loadResourceData(); // Load resources into allResources
        extractTutors();    // Populate tutorList from allResources

        // --- Setup UI Components ---
        setupTutorSpinner();
        setupToggleButtons();

        // --- Initial Display ---
        filterAndDisplayResources(); // Display initially (likely all resources)
    }

    // --- Data Loading (Replace with actual DataManager calls) ---
    private void loadResourceData() {
        allResources.clear();
        myResources.clear();
        for(Resource resource : dataManager.getResourceDao().getAllResources()){
            // the resources uploaded by the current user are separate
            if(current_user.getUser().isTutor() && resource.getTutorId() == current_user.getUser().getStudentNum()){
                myResources.add(resource);
            }else{
                for(Session session : dataManager.getSessionDao().getSessionsByTuteeId(resource.getTutorId())){
                    if(session.getTuteeId() == current_user.getUser().getStudentNum()){
                        allResources.add(resource);
                    }
                }
            }

        }
        // Sort resources if needed (e.g., by date descending)
//      allResources.sort((r1, r2) -> r2.getDate().compareTo(r1.getDate()));
    }

    private void extractTutors() {
        // Use a Set to avoid duplicate tutor names
        Set<String> tutors = new HashSet<>();
        for (Resource resource : allResources) {

            tutors.add(dataManager.getUserDao().getUserByStudentNum(resource.getTutorId()).getFirstName()
                    + " "+ dataManager.getUserDao().getUserByStudentNum(resource.getTutorId()).getLastName());

        }
        tutorList.clear();
        if(current_user.getUser().isTutor())
            tutorList.add(MY_RESOURCES); // Add the current user resource option

        if(!tutors.isEmpty()) {
            tutorList.add(ALL_TUTORS); // Add the "All" option
            tutorList.addAll(tutors);
        }
        // Optionally sort the tutor names alphabetically (after "All Tutors")
        // Collections.sort(tutorList.subList(1, tutorList.size()));
    }

    // --- UI Setup ---
    private void setupTutorSpinner() {
        Context context = getContext();
        if (context == null) return;

        ArrayAdapter<String> tutorAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, tutorList);
        tutorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tutorSpinner.setAdapter(tutorAdapter);

        tutorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterAndDisplayResources(); // Re-filter and display when selection changes
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Optional: Handle case where nothing is selected
            }
        });
    }

    private void setupToggleButtons() {
        toggleMathButton.setOnClickListener(v -> {
            toggleVisibility(mathResourcesContainer, toggleMathButton);
            // Add similar listeners for other subject toggle buttons
        });
    }

    private void toggleVisibility(View container, ImageButton button) {
        if (container.getVisibility() == View.VISIBLE) {
            container.setVisibility(GONE);
            button.setImageResource(android.R.drawable.arrow_down_float); // Point down when hidden
//            button.setContentDescription(getString(R.string.show_section)); // Update description
        } else {
            container.setVisibility(View.VISIBLE);
            button.setImageResource(android.R.drawable.arrow_up_float); // Point up when visible
//            button.setContentDescription(getString(R.string.hide_section)); // Update description
        }
        // Add these strings to your strings.xml:
        // <string name="show_section">Show section</string>
        // <string name="hide_section">Hide section</string>
    }


    // --- Filtering and Displaying ---
    private void filterAndDisplayResources() {
        String selectedTutor = tutorSpinner.getSelectedItem() != null ? tutorSpinner.getSelectedItem().toString() : ALL_TUTORS;

        List<Resource> filteredResources;

        // Filter by tutor
        // Show my resource
        if (selectedTutor.equals(MY_RESOURCES)) {
            filteredResources = new ArrayList<>(myResources);

        }else if (selectedTutor.equals(ALL_TUTORS)) {
            filteredResources = new ArrayList<>(allResources); // Show all
        } else {
            filteredResources = new ArrayList<>();
            for (Resource resource : allResources) {
                if ((dataManager.getUserDao().getUserByStudentNum(resource.getTutorId()).getFirstName()
                        + " "+ dataManager.getUserDao().getUserByStudentNum(resource.getTutorId()).getLastName()).equals(selectedTutor)) {
                    filteredResources.add(resource);
                }
            }
        }

        displayResources(filteredResources, selectedTutor);
    }


    @SuppressLint({"SetTextI18n", "ResourceAsColor"})
    private void displayResources(List<Resource> resourcesToShow, String selectedTutor ) {
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        if (context == null || inflater == null) return;

        // Clear previous items
        mathResourcesContainer.removeAllViews();
        // Clear other subject containers if they exist

        if (resourcesToShow.isEmpty()) {
            // Optional: Show a "No resources found" message
            TextView noResults = new TextView(context);
            noResults.setText(selectedTutor != MY_RESOURCES ? "No resource found" : "You haven't uploaded any resource yet"); // Add to strings.xml
            noResults.setPadding(16,16,16,16); // Add some padding
            mathResourcesContainer.addView(noResults); // Add to a default container or handle appropriately
            tutorSpinner.setVisibility(GONE);
            subjectSpinner.setVisibility(GONE);
            return;
        }


        for (Resource resource : resourcesToShow) {
            // Inflate the item layout
            View itemView = inflater.inflate(R.layout.item_resource, null, false); // Pass null, false initially

            // Find views within the item layout
            ImageView icon = itemView.findViewById(R.id.resource_type_icon);
            TextView title = itemView.findViewById(R.id.resource_title);
//            TextView description = itemView.findViewById(R.id.resource_description);
            TextView tutor = itemView.findViewById(R.id.resource_tutor);
//            TextView date = itemView.findViewById(R.id.resource_date);

            ImageButton downloadButton = itemView.findViewById(R.id.download_button);
            if(selectedTutor == MY_RESOURCES) {
                downloadButton.setImageResource(R.drawable.ic_settings_black_24dp);
            }

            // Populate the views
            title.setText(resource.getName());
//            description.setText(resource.getDescription());
            tutor.setText("By: " + dataManager.getUserDao().getUserByStudentNum(resource.getTutorId()).getFirstName()
                    + " "+ dataManager.getUserDao().getUserByStudentNum(resource.getTutorId()).getLastName()); // Use string resource
//            date.setText(dateFormat.format(resource.getDate()));
            icon.setImageResource(android.R.drawable.ic_menu_agenda); // Example icon

            // Set download button action
            downloadButton.setOnClickListener(v -> {
                if(selectedTutor != MY_RESOURCES) {
                    // ** TODO: Implement actual download logic here **
                    // For now, show a Toast message
                    Toast.makeText(context, "Downloading: " + resource.getName(), Toast.LENGTH_SHORT).show();
                    // Example: startDownload(resource.getDownloadUrl());
                }else{
                    Intent toEdit = new Intent(context, ResourceUploadActivity.class);
//                    toEdit.putExtra("mode", "edit");
//                    toEdit.putExtra("resource", resource);
                    startActivity(toEdit);
                    Toast.makeText(context, "To edit resource" + resource.getName(), Toast.LENGTH_SHORT).show();

                }
            });

            // Add the populated item view
                LinearLayout resourceCard = new LinearLayout(context);
                resourceCard.setOrientation(LinearLayout.VERTICAL);
                resourceCard.setBackgroundColor(R.color.primary_light);

                resourceCard.addView(itemView);
                mathResourcesContainer.addView(resourceCard);
        }
        // Ensure the Math section is visible if it contains items after filtering
        // (or hide if empty, depending on desired behavior)
        mathResourcesContainer.setVisibility(mathResourcesContainer.getChildCount() > 0 ? View.VISIBLE : GONE);
        // Update toggle button state accordingly if hiding when empty
        toggleMathButton.setImageResource((mathResourcesContainer.getVisibility() == GONE) ? android.R.drawable.arrow_down_float : android.R.drawable.arrow_up_float);
    }

    // --- TODO: Helper Methods (Example) ---
    /*
    private void startDownload(String urlOrPath) {
        // Implement download logic using DownloadManager or other libraries
        Log.d("ResourcesFragment", "Attempting download for: " + urlOrPath);
    }
    */
}