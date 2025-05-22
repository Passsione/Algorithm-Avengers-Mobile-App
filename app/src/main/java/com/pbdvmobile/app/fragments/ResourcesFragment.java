package com.pbdvmobile.app.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.provider.OpenableColumns; // For getting file details from URI
import android.database.Cursor; // For getting file details from URI

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText; // If you add a text search for resource name
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Make sure this is used or remove
import com.google.firebase.storage.StorageReference;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.ResourceUploadActivity; // You'll need this Activity
import com.pbdvmobile.app.adapter.ResourceAdapter;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.dao.ResourceDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors; // API 24+

public class ResourcesFragment extends Fragment implements ResourceAdapter.OnResourceActionsListener {

    private static final String TAG = "ResourcesFragment";
    private static final String ALL_TUTORS_FILTER = "All Tutors"; // Or "All Uploaders"
    private static final String ALL_SUBJECTS_FILTER = "All Subjects";

    private RecyclerView recyclerViewResources;
    private ResourceAdapter resourceAdapter;
    private List<Resource> allFetchedResources = new ArrayList<>();
    private List<Resource> currentlyDisplayedResources = new ArrayList<>();
    private TextView textViewNoResources;
    private ProgressBar progressBarResources;
//    private FloatingActionButton fabAddResource;


    private AutoCompleteTextView spinnerFilterTutor, spinnerFilterSubject;
    private Button buttonClearResourceFilters;
    // private EditText editTextSearchResourceName; // Optional: for text search

    private LogInUser loggedInUser;
    private User currentUserPojo;
    private ResourceDao resourceDao;
    private SubjectDao subjectDao;
    private UserDao userDao;

    private List<User> tutorFilterList = new ArrayList<>(); // For tutor filter spinner
    private List<Subject> subjectFilterList = new ArrayList<>(); // For subject filter spinner

    private ActivityResultLauncher<Intent> resourceUploadLauncher;
    private ActivityResultLauncher<Intent> saveFileLauncher;
    private Resource pendingDownloadResource;


    public ResourcesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_resources, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null) return;

        loggedInUser = LogInUser.getInstance();
        currentUserPojo = loggedInUser.getUser(); // Assumes LogInUser holds this
        resourceDao = new ResourceDao();
        subjectDao = new SubjectDao();
        userDao = new UserDao();

        initializeViews(view);
        setupRecyclerView();
        setupFiltersAndLoadData(); // Combines filter setup with initial data load

        resourceUploadLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(getContext(), "Resource list potentially updated.", Toast.LENGTH_SHORT).show();
                        loadInitialResources(); // Refresh the list
                    }
                });

        saveFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri destinationUri = result.getData().getData();
                        if (destinationUri != null && pendingDownloadResource != null && pendingDownloadResource.getFileUrl() != null) {
                            // For downloading from Firebase Storage URL
                            downloadFileFromStorage(pendingDownloadResource.getFileUrl(), destinationUri, pendingDownloadResource.getName());
                        } else {
                            Toast.makeText(getContext(), "Save location not selected or resource URL missing.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Save cancelled.", Toast.LENGTH_SHORT).show();
                    }
                    pendingDownloadResource = null;
                });

//        if (currentUserPojo != null && currentUserPojo.isTutor()) {
//            fabAddResource.setVisibility(View.VISIBLE);
//            fabAddResource.setOnClickListener(v -> {
//                Intent intent = new Intent(getContext(), ResourceUploadActivity.class);
//                intent.putExtra(ResourceUploadActivity.EXTRA_MODE, ResourceUploadActivity.MODE_CREATE);
//                resourceUploadLauncher.launch(intent);
//            });
//        } else {
//            fabAddResource.setVisibility(View.GONE);
//        }
    }

    private void initializeViews(View view) {
        recyclerViewResources = view.findViewById(R.id.recyclerViewResources);
        textViewNoResources = view.findViewById(R.id.textViewNoResources);
        progressBarResources = view.findViewById(R.id.progressBarResources); // Add to XML
//        fabAddResource = view.findViewById(R.id.fabA);
        spinnerFilterTutor = view.findViewById(R.id.spinner_filter_tutor);
        spinnerFilterSubject = view.findViewById(R.id.spinner_filter_subject);
        buttonClearResourceFilters = view.findViewById(R.id.buttonClearResourceFilters);
        // editTextSearchResourceName = view.findViewById(R.id.editTextSearchResourceName); // If adding text search
    }

    private void setupRecyclerView() {
        resourceAdapter = new ResourceAdapter(getContext(), currentlyDisplayedResources, loggedInUser.getUid(), this);
        recyclerViewResources.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewResources.setAdapter(resourceAdapter);
    }

    private void setupFiltersAndLoadData() {
        showLoading(true);

        // Load subjects for subject filter first
        subjectDao.getAllSubjects().addOnSuccessListener(subjectSnapshots -> {
            subjectFilterList.clear();
            if (subjectSnapshots != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : subjectSnapshots.getDocuments()) {
                    Subject subject = doc.toObject(Subject.class);
                    if (subject != null) {
                        subject.setId(doc.getId());
                        subjectFilterList.add(subject);
                    }
                }
            }
            populateSubjectFilterSpinner();

            // Then load tutors for tutor filter
            userDao.getAllTutors().addOnSuccessListener(tutorSnapshots -> { // Or getAllUsers if non-tutors can upload
                tutorFilterList.clear();
                if (tutorSnapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : tutorSnapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUid(doc.getId());
                            tutorFilterList.add(user);
                        }
                    }
                }
                populateTutorFilterSpinner();
                loadInitialResources(); // Now load resources as filters are ready
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching tutors for filter", e);
                Toast.makeText(getContext(), "Could not load tutor filters.", Toast.LENGTH_SHORT).show();
                loadInitialResources(); // Still try to load resources
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching subjects for filter", e);
            Toast.makeText(getContext(), "Could not load subject filters.", Toast.LENGTH_SHORT).show();
            loadInitialResources(); // Still try to load resources
        });


        AdapterView.OnItemClickListener filterListener = (parent, view, position, id) -> applyFiltersToList();
        spinnerFilterTutor.setOnItemClickListener(filterListener);
        spinnerFilterSubject.setOnItemClickListener(filterListener);

        buttonClearResourceFilters.setOnClickListener(v -> {
            spinnerFilterTutor.setText(ALL_TUTORS_FILTER, false);
            spinnerFilterSubject.setText(ALL_SUBJECTS_FILTER, false);
            // if (editTextSearchResourceName != null) editTextSearchResourceName.setText("");
            applyFiltersToList();
        });
    }
    private void populateTutorFilterSpinner() {
        List<String> tutorNames = new ArrayList<>();
        tutorNames.add(ALL_TUTORS_FILTER);
        for (User u : tutorFilterList) {
            tutorNames.add(u.getFirstName() + " " + u.getLastName() + " (" + u.getEmail().split("@")[0] + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, tutorNames);
        spinnerFilterTutor.setAdapter(adapter);
        spinnerFilterTutor.setText(ALL_TUTORS_FILTER, false);
    }

    private void populateSubjectFilterSpinner() {
        List<String> subjectNames = new ArrayList<>();
        subjectNames.add(ALL_SUBJECTS_FILTER);
        for (Subject s : subjectFilterList) {
            subjectNames.add(s.getSubjectName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, subjectNames);
        spinnerFilterSubject.setAdapter(adapter);
        spinnerFilterSubject.setText(ALL_SUBJECTS_FILTER, false);
    }


    private void loadInitialResources() {
        showLoading(true);
        resourceDao.getAllResources().addOnSuccessListener(queryDocumentSnapshots -> {
            allFetchedResources.clear();
            if (queryDocumentSnapshots != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    Resource resource = doc.toObject(Resource.class);
                    if (resource != null) {
                        resource.setId(doc.getId());
                        allFetchedResources.add(resource);
                    }
                }
            }
            Log.d(TAG, "Fetched " + allFetchedResources.size() + " total resources.");
            applyFiltersToList(); // This will filter and update the adapter
            showLoading(false);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching resources", e);
            Toast.makeText(getContext(), "Failed to load resources.", Toast.LENGTH_SHORT).show();
            allFetchedResources.clear();
            applyFiltersToList();
            showLoading(false);
        });
    }

    private void applyFiltersToList() {
        String selectedTutorFilterText = spinnerFilterTutor.getText().toString();
        String selectedSubjectFilterText = spinnerFilterSubject.getText().toString();
        // String nameQuery = editTextSearchResourceName != null ? editTextSearchResourceName.getText().toString().toLowerCase().trim() : "";

        String selectedTutorUid = null;
        if (!ALL_TUTORS_FILTER.equals(selectedTutorFilterText) && !selectedTutorFilterText.isEmpty()) {
            for (User u : tutorFilterList) {
                if ((u.getFirstName() + " " + u.getLastName() + " (" + u.getEmail().split("@")[0] + ")").equals(selectedTutorFilterText)) {
                    selectedTutorUid = u.getUid();
                    break;
                }
            }
        }

        String selectedSubjectId = null;
        if (!ALL_SUBJECTS_FILTER.equals(selectedSubjectFilterText) && !selectedSubjectFilterText.isEmpty()) {
            for (Subject s : subjectFilterList) {
                if (s.getSubjectName().equals(selectedSubjectFilterText)) {
                    selectedSubjectId = s.getId();
                    break;
                }
            }
        }

        currentlyDisplayedResources.clear();
        for (Resource resource : allFetchedResources) {
            boolean passesTutorFilter = (selectedTutorUid == null) || (resource.getTutorUid() != null && resource.getTutorUid().equals(selectedTutorUid));
            boolean passesSubjectFilter = (selectedSubjectId == null) || (resource.getSubjectId() != null && resource.getSubjectId().equals(selectedSubjectId));
            // boolean passesNameFilter = nameQuery.isEmpty() || (resource.getName() != null && resource.getName().toLowerCase().contains(nameQuery));

            if (passesTutorFilter && passesSubjectFilter /*&& passesNameFilter*/) {
                currentlyDisplayedResources.add(resource);
            }
        }
        resourceAdapter.updateList(currentlyDisplayedResources);
        updateNoResourcesView();
    }


    private void updateNoResourcesView() {
        if (currentlyDisplayedResources.isEmpty() && !progressBarResources.isShown()) {
            recyclerViewResources.setVisibility(View.GONE);
            textViewNoResources.setVisibility(View.VISIBLE);
        } else {
            recyclerViewResources.setVisibility(View.VISIBLE);
            textViewNoResources.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBarResources != null) progressBarResources.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (recyclerViewResources != null) recyclerViewResources.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (textViewNoResources != null && isLoading) textViewNoResources.setVisibility(View.GONE); // Hide "no resources" while loading
    }


    // --- OnResourceActionsListener Implementation ---
    @Override
    public void onDownloadResource(Resource resource) {
        if (getContext() == null || resource == null || resource.getFileUrl() == null || resource.getFileUrl().isEmpty()) {
            Toast.makeText(getContext(), "Resource URL is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingDownloadResource = resource;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(resource.getFileType() != null ? resource.getFileType() : "application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, resource.getName());

        try {
            saveFileLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No app found to handle file saving.", Toast.LENGTH_LONG).show();
            pendingDownloadResource = null;
        }
    }

    private void downloadFileFromStorage(String downloadUrl, Uri destinationUri, String displayName) {
        if (getContext() == null || downloadUrl == null || destinationUri == null) {
            Toast.makeText(getContext(), "Download failed: Invalid parameters.", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d(TAG, "Attempting to download from URL: " + downloadUrl + " to Dest: " + destinationUri);
        showLoading(true); // Show progress indicator

        StorageReference httpsReference = com.google.firebase.storage.FirebaseStorage.getInstance().getReferenceFromUrl(downloadUrl);

        httpsReference.getStream().addOnSuccessListener(taskSnapshot -> {
            try (InputStream inputStream = taskSnapshot.getStream();
                 OutputStream outputStream = getContext().getContentResolver().openOutputStream(destinationUri)) {
                if (inputStream == null || outputStream == null) {
                    throw new java.io.IOException("Failed to open streams for download.");
                }
                byte[] buffer = new byte[1024 * 4];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), displayName + " downloaded successfully!", Toast.LENGTH_LONG).show();
                    showLoading(false);
                });
            } catch (java.io.IOException e) {
                Log.e(TAG, "IOException during file download stream processing", e);
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to save " + displayName + ".", Toast.LENGTH_LONG).show();
                    showLoading(false);
                });
            }
        }).addOnFailureListener(exception -> {
            Log.e(TAG, "Firebase Storage download failed for: " + downloadUrl, exception);
            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Download failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                showLoading(false);
            });
        });
    }


    @Override
    public void onEditResource(Resource resource) {
        if (currentUserPojo == null || !currentUserPojo.getUid().equals(resource.getTutorUid())) {
            Toast.makeText(getContext(), "You can only edit your own resources.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getContext(), ResourceUploadActivity.class);
        intent.putExtra(ResourceUploadActivity.EXTRA_MODE, ResourceUploadActivity.MODE_EDIT);
        intent.putExtra(ResourceUploadActivity.EXTRA_RESOURCE_ID, resource.getId()); // Pass Firestore doc ID
        resourceUploadLauncher.launch(intent);
    }

    @Override
    public void onDeleteResource(final Resource resource, final int position) {
        if (currentUserPojo == null || !currentUserPojo.getUid().equals(resource.getTutorUid())) {
            Toast.makeText(getContext(), "You can only delete your own resources.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Resource")
                .setMessage("Are you sure you want to delete '" + resource.getName() + "'? This will also remove the file from storage.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    showLoading(true);
                    resourceDao.deleteResource(resource.getId(), resource.getStoragePath())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Resource deleted.", Toast.LENGTH_SHORT).show();
                                loadInitialResources(); // Refresh the list from Firestore
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to delete resource: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Failed to delete resource " + resource.getId(), e);
                                showLoading(false);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh if coming back, e.g., after an upload/edit.
        // Only reload if it's not the initial load triggered by onViewCreated->setupFiltersAndLoadData
        if (allFetchedResources != null && !allFetchedResources.isEmpty() && progressBarResources!=null && !progressBarResources.isShown()) {
            Log.d(TAG, "onResume: Refreshing resources list.");
            loadInitialResources();
        }
    }
}
