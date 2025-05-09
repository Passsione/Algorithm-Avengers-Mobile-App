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

import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.ResourceUploadActivity;
import com.pbdvmobile.app.adapter.ResourceAdapter;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ResourcesFragment extends Fragment implements ResourceAdapter.OnResourceActionsListener {

    private static final String TAG = "ResourcesFragment";
    private static final String ALL_TUTORS_FILTER = "All Tutors";
    private static final String ALL_SUBJECTS_FILTER = "All Subjects";

    private RecyclerView recyclerViewResources;
    private ResourceAdapter resourceAdapter;
    private List<Resource> allFetchedResources; // Raw list from DB
    private List<Resource> currentlyDisplayedResources; // List for the adapter after filtering
    private TextView textViewNoResources;


    private AutoCompleteTextView spinnerFilterTutor, spinnerFilterSubject;
    private Button buttonClearResourceFilters;

    private DataManager dataManager;
    private LogInUser currentUser;

    private ActivityResultLauncher<Intent> resourceUploadEditLauncher;

    private ActivityResultLauncher<Intent> saveFileLauncher; // For ACTION_CREATE_DOCUMENT
    private Resource pendingDownloadResource; // To hold resource info during SAF flow


    public ResourcesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_resources, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() == null) return;
        dataManager = DataManager.getInstance(getContext());
        currentUser = LogInUser.getInstance(dataManager);

        initializeViews(view);
        setupRecyclerView();
        setupFilters();

        resourceUploadEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(getContext(), "Resource list updated.", Toast.LENGTH_SHORT).show();
                        loadAndDisplayResources();
                    }
                });

        // Initialize the launcher for saving a file
        saveFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri destinationUri = result.getData().getData();
                        if (destinationUri != null && pendingDownloadResource != null) {
                            copyResourceToUri(Uri.parse(pendingDownloadResource.getResource()), destinationUri, pendingDownloadResource.getName());
                        } else {
                            Toast.makeText(getContext(), "Save location not selected or resource error.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Save cancelled.", Toast.LENGTH_SHORT).show();
                    }
                    pendingDownloadResource = null; // Clear pending resource
                });

        loadAndDisplayResources();
    }

    private void initializeViews(View view) {
        recyclerViewResources = view.findViewById(R.id.recyclerViewResources);
        textViewNoResources = view.findViewById(R.id.textViewNoResources);
        spinnerFilterTutor = view.findViewById(R.id.spinner_filter_tutor);
        spinnerFilterSubject = view.findViewById(R.id.spinner_filter_subject);
        buttonClearResourceFilters = view.findViewById(R.id.buttonClearResourceFilters);
    }

    private void setupRecyclerView() {
        currentlyDisplayedResources = new ArrayList<>();
        resourceAdapter = new ResourceAdapter(getContext(), currentlyDisplayedResources, dataManager,
                currentUser.getUser().getStudentNum(), currentUser.getUser().isTutor(), this);
        recyclerViewResources.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewResources.setAdapter(resourceAdapter);
    }


    private void loadAndDisplayResources() {
        // This is where we decide which resources the user can see.
        // For a tutee: only resources from tutors they've had a COMPLETED session with.
        // For a tutor: their own resources by default.
        allFetchedResources = new ArrayList<>();
        if (currentUser.getUser().isTutor()) {
            // Tutors see all their resources
            allFetchedResources.addAll(dataManager.getResourceDao().getResourcesByTutorId(currentUser.getUser().getStudentNum()));
        }
        // Get IDs of tutors the current tutee has had COMPLETED sessions with
        List<Session> completedSessions = dataManager.getSessionDao().getSessionsByTuteeId(currentUser.getUser().getStudentNum())
                .stream()
                .filter(s -> s.getStatus() == Session.Status.COMPLETED) // Only completed sessions
                .collect(Collectors.toList());

        Set<Integer> tutorIdsFromSessions = new HashSet<>();
        for (Session session : completedSessions) {
            tutorIdsFromSessions.add(session.getTutorId());
        }

        if (!tutorIdsFromSessions.isEmpty()) {
            for (Integer tutorId : tutorIdsFromSessions) {
                allFetchedResources.addAll(dataManager.getResourceDao().getResourcesByTutorId(tutorId));
            }
        }
        // To avoid duplicates if a resource is somehow linked to multiple relevant tutors (unlikely by design)
        // Could use a Set<Resource> here or filter duplicates by resourceId
        allFetchedResources = new ArrayList<>(new HashSet<>(allFetchedResources)); // Simple way to get unique




        //  sort by resource name for consistency
        if (allFetchedResources != null) {
            Collections.sort(allFetchedResources, (r1, r2) -> {
                if (r1.getName() == null && r2.getName() == null) return 0;
                if (r1.getName() == null) return 1;
                if (r2.getName() == null) return -1;
                return r1.getName().compareToIgnoreCase(r2.getName());
            });
        }

        populateFilterSpinners(); // Populate spinners based on the loaded resources
        applyFilters(); // Apply default filters and display
    }


    private void populateFilterSpinners() {
        if (getContext() == null || allFetchedResources == null) return;

        // Tutor Spinner
        Set<String> tutorNames = new HashSet<>();
        for (Resource resource : allFetchedResources) {
            User tutor = dataManager.getUserDao().getUserByStudentNum(resource.getTutorId());
            if (tutor != null) {
                tutorNames.add(currentUser.getUser().getStudentNum() == resource.getTutorId() ? "My Resources" : tutor.getFirstName() + " " + tutor.getLastName());
            }
        }
        tutorNames.add(ALL_TUTORS_FILTER);

        ArrayAdapter<String> tutorAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(tutorNames));
        spinnerFilterTutor.setAdapter(tutorAdapter);
        spinnerFilterTutor.setText(ALL_TUTORS_FILTER, false); // Set default without triggering listener

        // Subject Spinner
        Set<String> subjectNames = new HashSet<>();
        for (Resource resource : allFetchedResources) {
            Subject subject = dataManager.getSubjectDao().getSubjectById(resource.getSubjectId());
            if (subject != null) {
                subjectNames.add(subject.getSubjectName());
            }
        }
        subjectNames.add(ALL_SUBJECTS_FILTER);

        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(subjectNames));
        spinnerFilterSubject.setAdapter(subjectAdapter);
        spinnerFilterSubject.setText(ALL_SUBJECTS_FILTER, false); // Set default
    }


    private void setupFilters() {
        AdapterView.OnItemClickListener filterListener = (parent, view, position, id) -> applyFilters();
        spinnerFilterTutor.setOnItemClickListener(filterListener);
        spinnerFilterSubject.setOnItemClickListener(filterListener);

        buttonClearResourceFilters.setOnClickListener(v -> {
            spinnerFilterTutor.setText(ALL_TUTORS_FILTER, false);
            spinnerFilterSubject.setText(ALL_SUBJECTS_FILTER, false);
            applyFilters();
        });
    }

    private void applyFilters() {
        if (allFetchedResources == null) return;

        String selectedTutorName = spinnerFilterTutor.getText().toString();
        String selectedSubjectName = spinnerFilterSubject.getText().toString();

        List<Resource> filteredList = new ArrayList<>();
        for (Resource resource : allFetchedResources) {
            boolean passesTutorFilter = true;
            if (!ALL_TUTORS_FILTER.equals(selectedTutorName) && !TextUtils.isEmpty(selectedTutorName)) {
                User tutor = dataManager.getUserDao().getUserByStudentNum(resource.getTutorId());
                if (tutor == null || !(tutor.getFirstName() + " " + tutor.getLastName()).equals(selectedTutorName)) {
                    passesTutorFilter = false;
                }
            }

            boolean passesSubjectFilter = true;
            if (!ALL_SUBJECTS_FILTER.equals(selectedSubjectName) && !TextUtils.isEmpty(selectedSubjectName)) {
                Subject subject = dataManager.getSubjectDao().getSubjectById(resource.getSubjectId());
                if (subject == null || !subject.getSubjectName().equals(selectedSubjectName)) {
                    passesSubjectFilter = false;
                }
            }

            if (passesTutorFilter && passesSubjectFilter) {
                filteredList.add(resource);
            }
        }

        currentlyDisplayedResources.clear();
        currentlyDisplayedResources.addAll(filteredList);
        resourceAdapter.notifyDataSetChanged(); // Notify adapter with the final filtered list
        updateNoResourcesView();
    }

    private void updateNoResourcesView() {
        if (resourceAdapter.getItemCount() == 0) {
            recyclerViewResources.setVisibility(View.GONE);
            textViewNoResources.setVisibility(View.VISIBLE);
        } else {
            recyclerViewResources.setVisibility(View.VISIBLE);
            textViewNoResources.setVisibility(View.GONE);
        }
    }

    // --- OnResourceActionsListener Implementation ---


    @Override
    public void onDownloadResource(Resource resource) {
        if (getContext() == null || resource == null || resource.getResource() == null) {
            Toast.makeText(getContext(), "Resource data or URI is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        pendingDownloadResource = resource; // Store for SAF callback
        Uri internalFileUri = Uri.parse(resource.getResource()); // This is now file:///...

        // Check if the internal file actually exists before attempting to offer it for download
        // This is an important sanity check
        File checkFile = null;
        if ("file".equals(internalFileUri.getScheme()) && internalFileUri.getPath() != null) {
            checkFile = new File(internalFileUri.getPath());
        }

        if (checkFile == null || !checkFile.exists() || !checkFile.canRead()) {
            Log.e(TAG, "Internal resource file does not exist or cannot be read: " + internalFileUri.toString());
            Toast.makeText(getContext(), "Resource file is missing or corrupted. Please try re-uploading.", Toast.LENGTH_LONG).show();
            pendingDownloadResource = null;
            // Optional: You might want to delete this invalid resource entry from the DB
            // dataManager.getResourceDao().deleteResource(resource.getResourcesId());
            // loadAndDisplayResources(); // Refresh list
            return;
        }


        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        String fileName = resource.getName(); // Use the display name for the downloaded file
        String mimeType = determineMimeTypeFromFileName(resource.getResource()); // Helper to get MIME from name/extension

        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        try {
            Log.d(TAG, "Launching ACTION_CREATE_DOCUMENT for: " + fileName + " with MIME: " + mimeType + " from internal URI: " + internalFileUri);
            saveFileLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No app found to handle file saving.", Toast.LENGTH_LONG).show();
            pendingDownloadResource = null;
        }
    }

    private String determineMimeTypeFromFileName(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".pdf")) return "application/pdf";
        if (lowerFileName.endsWith(".doc")) return "application/msword";
        if (lowerFileName.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lowerFileName.endsWith(".txt")) return "text/plain";
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerFileName.endsWith(".png")) return "image/png";
        // Add more common types as needed
        return "application/octet-stream"; // Default fallback
    }


    private void copyResourceToUri(Uri internalSourceFileUri, Uri userSelectedDestinationUri, String displayName) {
        if (getContext() == null || internalSourceFileUri == null || userSelectedDestinationUri == null) {
            Log.e(TAG, "Copy failed: null context or URI during copy operation.");
            if (getActivity() != null) {
                getActivity().runOnUiThread(()-> Toast.makeText(getContext(), "Failed to save file: Invalid parameters.", Toast.LENGTH_LONG).show());
            }
            return;
        }
        Log.d(TAG, "Copying from internal: " + internalSourceFileUri.toString() + " to user selected: " + userSelectedDestinationUri.toString());

        new Thread(() -> {
            boolean success = false;
            long bytesCopied = 0; // To track if anything was written

            try (InputStream inputStream = getContext().getContentResolver().openInputStream(internalSourceFileUri);
                 OutputStream outputStream = getContext().getContentResolver().openOutputStream(userSelectedDestinationUri)) {

                // Check if streams were successfully opened
                if (inputStream == null) {
                    throw new IOException("Failed to open input stream from internal source: " + internalSourceFileUri);
                }
                if (outputStream == null) {
                    throw new IOException("Failed to open output stream to user selected destination: " + userSelectedDestinationUri);
                }

                byte[] buffer = new byte[1024 * 4]; // 4KB buffer
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    bytesCopied += bytesRead;
                }
                outputStream.flush(); // Ensure all buffered data is written
                success = true;
                Log.d(TAG, "Copy successful. Bytes copied: " + bytesCopied);

            } catch (FileNotFoundException fnfe) {
                Log.e(TAG, "FileNotFoundException during file copy. Source: " + internalSourceFileUri + " Dest: " + userSelectedDestinationUri, fnfe);
            } catch (IOException e) {
                Log.e(TAG, "IOException during file copy", e);
            } catch (SecurityException se) {
                Log.e(TAG, "SecurityException during file copy (should not happen with internal files if path is correct)", se);
            } catch (Exception ex) { // Catch any other unexpected exceptions
                Log.e(TAG, "Unexpected exception during file copy", ex);
            }

            // --- Verification Step (Optional but Recommended) ---
            if (success && bytesCopied > 0) {
                try {
                    ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(userSelectedDestinationUri, "r");
                    if (pfd != null) {
                        long fileSize = pfd.getStatSize();
                        Log.d(TAG, "Verification: Saved file URI: " + userSelectedDestinationUri + ", Size: " + fileSize + " bytes");
                        pfd.close();
                        if (fileSize == 0 && bytesCopied > 0) {
                            Log.w(TAG, "Verification Warning: File saved but size is 0. Copied bytes: " + bytesCopied);
                            // This might indicate an issue with how the outputStream was handled or closed by the system
                            // or an issue with the ContentProvider serving userSelectedDestinationUri
                            success = false; // Consider it a failure if file size is 0 but we copied data
                        } else if (fileSize == 0 && bytesCopied == 0) {
                            Log.w(TAG, "Verification Warning: Source file might be empty. Copied 0 bytes, saved file size is 0.");
                            // This is okay if the source was empty, but might be an issue otherwise.
                        }
                    } else {
                        Log.w(TAG, "Verification Warning: Could not open ParcelFileDescriptor for destination URI to check size.");
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Verification Error: File not found at destination URI after saving.", e);
                    success = false; // File seems to have disappeared or not been properly committed.
                } catch (Exception e) {
                    Log.w(TAG, "Verification Warning: Exception during file size check.", e);
                }
            } else if (success && bytesCopied == 0) {
                Log.w(TAG, "Copy reported success, but 0 bytes were copied. Source file might be empty.");
            }
            // --- End Verification Step ---


            boolean finalSuccess = success;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (finalSuccess) {
                        Toast.makeText(getContext(), displayName + " saved successfully!", Toast.LENGTH_LONG).show();
                        // Optional: Offer to open the file immediately
                        // offerToOpenFile(userSelectedDestinationUri, determineMimeTypeFromFileName(displayName));
                    } else {
                        Toast.makeText(getContext(), "Failed to save " + displayName + ". Check logs.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    @Override
    public void onEditResource(Resource resource) {
        Intent intent = new Intent(getContext(), ResourceUploadActivity.class);
        intent.putExtra(ResourceUploadActivity.EXTRA_MODE, ResourceUploadActivity.MODE_EDIT);
        intent.putExtra(ResourceUploadActivity.EXTRA_RESOURCE_ID, resource.getResourcesId());
        resourceUploadEditLauncher.launch(intent);
    }

    @Override
    public void onDeleteResource(final Resource resource, final int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Resource")
                .setMessage("Are you sure you want to delete '" + resource.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Perform delete in background
                    new Thread(() -> {
                        int rowsAffected = dataManager.getResourceDao().deleteResource(resource.getResourcesId());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (rowsAffected > 0) {
                                    Toast.makeText(getContext(), "Resource deleted.", Toast.LENGTH_SHORT).show();
                                    // Remove from the local list and notify adapter
                                    if (position >= 0 && position < currentlyDisplayedResources.size()) {
                                        if(currentlyDisplayedResources.get(position).getResourcesId() == resource.getResourcesId()){
                                            currentlyDisplayedResources.remove(position);
                                            resourceAdapter.notifyItemRemoved(position);
                                            // resourceAdapter.notifyItemRangeChanged(position, currentlyDisplayedResources.size());
                                        } else { // Fallback if position became invalid
                                            loadAndDisplayResources();
                                        }
                                    } else { // Fallback
                                        loadAndDisplayResources();
                                    }
                                    updateNoResourcesView();
                                    // also remove from allFetchedResources to keep spinners consistent or repopulate them
                                    allFetchedResources.removeIf(r -> r.getResourcesId() == resource.getResourcesId());
                                    populateFilterSpinners(); // Repopulate spinners as data changed
                                } else {
                                    Toast.makeText(getContext(), "Failed to delete resource.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    @Override
    public void onResume() {
        super.onResume();
        // Refresh the list if coming back from upload/edit activity
        loadAndDisplayResources();
    }
}