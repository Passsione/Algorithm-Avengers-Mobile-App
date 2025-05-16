package com.pbdvmobile.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils; // Import for TextUtils
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.dao.ResourceDao;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.UserSubject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ResourceUploadActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "com.pbdvmobile.app.EXTRA_MODE";
    public static final String MODE_CREATE = "MODE_CREATE";
    public static final String MODE_EDIT = "MODE_EDIT";
    public static final String EXTRA_RESOURCE_ID = "com.pbdvmobile.app.EXTRA_RESOURCE_ID";
    private static final String TAG = "Resource Upload";

    private Button selectPdfButton, uploadPdfButton, cancelButton;
    private Uri selectedPdfUri;
    TextInputEditText txtResourceName; // Changed from txtFileName for clarity
    TextView docNameLabel, jobTitle; // Renamed from docName
    Spinner subjectSpinner; // Renamed from subjects
    private DataManager db;
    private LogInUser current_user;
    ResourceDao resourceDao; // Renamed from pdfDao
    AtomicInteger selectedSubjectId = new AtomicInteger(0); // Renamed

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    // private String savedFilePath; // URI string is better for content URIs

    private String currentMode = MODE_CREATE;
    private Resource resourceToEdit;
    private int resourceToEditId = -1;

    private final ActivityResultLauncher<Intent> pdfSelectionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            selectedPdfUri = result.getData().getData();
                            if (selectedPdfUri != null) {
                                String fileName = getFileNameFromUri(selectedPdfUri);
                                Toast.makeText(this, "File Selected: " + fileName, Toast.LENGTH_SHORT).show();
                                uploadPdfButton.setVisibility(View.VISIBLE);
                                docNameLabel.setText(fileName);
                                if (TextUtils.isEmpty(txtResourceName.getText())) {
                                    txtResourceName.setText(fileName.replaceFirst("[.][^.]+$", "")); // Remove extension for title
                                }
                            } else {
                                Toast.makeText(this, "Error selecting file", Toast.LENGTH_SHORT).show();
                                uploadPdfButton.setVisibility(View.GONE);
                            }
                        } else {
                            Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show();                        uploadPdfButton.setEnabled(selectedPdfUri != null || currentMode.equals(MODE_EDIT));
                        }
                    });
    private List<Integer> subjectIdListForSpinner = new ArrayList<>(); // To map spinner position to subject ID


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resource_upload);

        selectPdfButton = findViewById(R.id.selectPdfButton);
        uploadPdfButton = findViewById(R.id.uploadPdfButton);
        jobTitle = findViewById(R.id.textViewUploadTitle);
        txtResourceName = findViewById(R.id.resourceNameEditText); // Changed ID
        docNameLabel = findViewById(R.id.upload_doc_name_label);     // Changed ID
        subjectSpinner = findViewById(R.id.subjectDropdown);
        cancelButton = findViewById(R.id.upload_cancel_button);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainResourceUploadLayout), (v, insets) -> { // Changed ID
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = DataManager.getInstance(getApplicationContext()); // Use application context
        current_user = LogInUser.getInstance(db);
        resourceDao = db.getResourceDao();

        Intent intent = getIntent();
        currentMode = intent.getStringExtra(EXTRA_MODE) != null ? intent.getStringExtra(EXTRA_MODE) : MODE_CREATE;
        resourceToEditId = intent.getIntExtra(EXTRA_RESOURCE_ID, -1);

        createSubjectDropDown(); // Call before potentially pre-filling for edit mode

        if (currentMode.equals(MODE_EDIT) && resourceToEditId != -1) {
            setTitle("Edit Resource");
            uploadPdfButton.setText("Update Resource");
            loadResourceForEditing();
        } else {
            setTitle("Upload Resource");
            uploadPdfButton.setText("Upload Resource");
//            uploadPdfButton.setEnabled(false); // Enabled only after file selection in create mode
        }


        selectPdfButton.setOnClickListener(v -> selectPdfFile());
        uploadPdfButton.setOnClickListener(v -> handleUploadOrUpdate());
        cancelButton.setOnClickListener(v -> {
            finish();
            Toast.makeText(this, "Operation cancelled", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadResourceForEditing() {
        executorService.execute(() -> {
            resourceToEdit = resourceDao.getResourceById(resourceToEditId);
            runOnUiThread(() -> {
                if (resourceToEdit != null) {
                    txtResourceName.setText(resourceToEdit.getName());
                    jobTitle.setText("Edit Resource");
                    docNameLabel.setText(getFileNameFromUriString(resourceToEdit.getResource())); // Display existing file name or URI
                    // Pre-select subject in spinner
                    if (subjectSpinner.getAdapter() != null) {
                        for (int i = 0; i < subjectSpinner.getAdapter().getCount(); i++) {
                            // This assumes subjectSpinner items are Subject objects or that you can map ID
                            // For now, let's assume IDs are stored and matched.
                            // This part needs careful implementation based on how subjects are populated in spinner.
                            // We'll use the 'addedSubjectIds' list from createSubjectDropDown.
                            if (subjectIdListForSpinner.get(i) == resourceToEdit.getSubjectId()){
                                subjectSpinner.setSelection(i);
                                selectedSubjectId.set(resourceToEdit.getSubjectId());
                                break;
                            }
                        }
                    }
                    uploadPdfButton.setEnabled(true); // Can update details even without changing file
                } else {
                    Toast.makeText(this, "Error loading resource for editing.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        });
    }

    private void createSubjectDropDown() {
        List<UserSubject> userSubjects = db.getSubjectDao().getTutoredUserSubjects(current_user.getUser().getStudentNum()); // Get only subjects user tutors
        subjectIdListForSpinner.clear();
        List<String> subjectsName = new ArrayList<>();

        if (userSubjects.isEmpty() && currentMode.equals(MODE_CREATE)) {
            Toast.makeText(this, "You are not registered to tutor any subjects. Cannot upload resources.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        for (UserSubject userSubject : userSubjects) {
            if(!userSubject.getTutoring())continue;
            Subject sub = db.getSubjectDao().getSubjectById(userSubject.getSubjectId());
            if (sub != null) {
                subjectsName.add(sub.getSubjectName());
                subjectIdListForSpinner.add(sub.getSubjectId()); // Store ID in parallel
            }
        }

        if (subjectsName.isEmpty() && currentMode.equals(MODE_CREATE)) {
            Toast.makeText(this, "No subjects available for you to upload resources to.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        ArrayAdapter<String> subjectsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                subjectsName
        );
        subjectsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(subjectsAdapter);

        subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < subjectIdListForSpinner.size()) {
                    selectedSubjectId.set(subjectIdListForSpinner.get(position));
                } else {
                    selectedSubjectId.set(0); // Reset or handle error
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSubjectId.set(0);
            }
        });

        // If creating, select first by default if list is not empty
        if (currentMode.equals(MODE_CREATE) && !subjectIdListForSpinner.isEmpty()) {
            subjectSpinner.setSelection(0);
            selectedSubjectId.set(subjectIdListForSpinner.get(0));
        }
    }


    private void selectPdfFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allow any file type, can be restricted e.g. "application/pdf"
        String[] mimeTypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        pdfSelectionLauncher.launch(intent);
    }

    private void handleUploadOrUpdate() {
        String resourceName = txtResourceName.getText().toString().trim();
        if (TextUtils.isEmpty(resourceName)) {
            txtResourceName.setError("Resource name cannot be empty");
            txtResourceName.requestFocus();
            return;
        }
        if (selectedSubjectId.get() == 0) {
            Toast.makeText(this, "Please select a subject.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentMode.equals(MODE_CREATE)) {
            if (selectedPdfUri == null) {
                Toast.makeText(this, "No file selected for upload", Toast.LENGTH_SHORT).show();
                return;
            }
            // Call to copy the selected file to internal storage and then save its metadata
            copyFileToInternalStorageAndSave(selectedPdfUri, resourceName, true); // isCreateMode = true
        } else if (currentMode.equals(MODE_EDIT) && resourceToEdit != null) {
            if (selectedPdfUri != null) {
                // User selected a new file to replace the old one during edit
                // Call to copy the NEW selected file to internal storage and then update metadata
                copyFileToInternalStorageAndSave(selectedPdfUri, resourceName, false); // isCreateMode = false

            } else {
                // User is editing details but NOT changing the file itself
                updateExistingResource(resourceName);
            }
        }
    }

    private void uploadNewResource(Uri fileUri, String displayName) {
        String fileUriString = fileUri.toString(); // Store the URI as a string

        Resource newResource = new Resource(current_user.getUser().getStudentNum(), selectedSubjectId.get(), fileUriString, displayName);
        executorService.execute(() -> {
            long newId = resourceDao.insertResource(newResource);
            runOnUiThread(() -> {
                if (newId != -1) {
                    Toast.makeText(ResourceUploadActivity.this, "Resource uploaded successfully.", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK); // Indicate success to calling fragment/activity
                    finish();
                } else {
                    Toast.makeText(ResourceUploadActivity.this, "Failed to upload resource to database.", Toast.LENGTH_LONG).show();
                }
                selectedPdfUri = null;
            });
        });
    }

    private void updateExistingResource(String newDisplayName) {
        resourceToEdit.setName(newDisplayName);
        resourceToEdit.setSubjectId(selectedSubjectId.get()); // Update subject if changed

        // If a new file was selected, update the resource URI
        if (selectedPdfUri != null) {
            resourceToEdit.setResource(selectedPdfUri.toString());
        }
        // Note: TutorId should not change during an edit by the same tutor.

        executorService.execute(() -> {
            int rowsAffected = resourceDao.updateResource(resourceToEdit);
            runOnUiThread(() -> {
                if (rowsAffected > 0) {
                    Toast.makeText(ResourceUploadActivity.this, "Resource updated successfully.", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK); // Indicate success
                    finish();
                } else {
                    Toast.makeText(ResourceUploadActivity.this, "Failed to update resource.", Toast.LENGTH_LONG).show();
                }
                selectedPdfUri = null; // Reset after attempt
                uploadPdfButton.setEnabled(true); // Keep enabled as it's edit mode
            });
        });
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "selected_file"; // Fallback
            }
        }
        return fileName;
    }

    private String getFileNameFromUriString(String uriString) {
        if (uriString == null) return "Unknown File";
        try {
            Uri uri = Uri.parse(uriString);
            String path = uri.getPath();
            if (path != null) {
                return new File(path).getName();
            }
        } catch (Exception e) {
            // Fallback for non-standard URIs or if path parsing fails
            int lastSlash = uriString.lastIndexOf('/');
            if (lastSlash != -1 && lastSlash < uriString.length() -1) {
                return uriString.substring(lastSlash + 1);
            }
        }
        return uriString; // Return the URI string itself as a last resort
    }

    private void copyFileToInternalStorageAndSave(Uri sourceContentUri, String displayName, boolean isCreateMode) {
        executorService.execute(() -> {
            String originalFileName = getFileNameFromUri(sourceContentUri); // Make sure this method is robust
            String uniqueInternalFileName = UUID.randomUUID().toString() + "_" + sanitizeFileName(originalFileName);
            File internalDir = getFilesDir();
            File internalFile = new File(internalDir, uniqueInternalFileName);
            Uri internalFileUri = null;

            Log.d(TAG, "Attempting to copy to internal storage: " + internalFile.getAbsolutePath());

            try (InputStream inputStream = getContentResolver().openInputStream(sourceContentUri);
                 OutputStream outputStream = new FileOutputStream(internalFile)) {

                if (inputStream == null) {
                    throw new IOException("Could not open input stream from source URI: " + sourceContentUri);
                }

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                internalFileUri = Uri.fromFile(internalFile); // URI of the internal copy
                Log.d(TAG, "File copied to internal storage: " + internalFileUri.toString());

            } catch (Exception e) {
                Log.e(TAG, "Failed to copy file to internal storage from URI: " + sourceContentUri, e);
                runOnUiThread(() -> Toast.makeText(ResourceUploadActivity.this, "Error saving file internally. " + e.getMessage(), Toast.LENGTH_LONG).show());
                return;
            }

            // Now proceed with DB operation using internalFileUri
            if (internalFileUri != null) {
                String internalUriStringToStore = internalFileUri.toString(); // THIS IS WHAT GETS STORED

                if (isCreateMode) {
                    Resource newResource = new Resource(
                            current_user.getUser().getStudentNum(),
                            selectedSubjectId.get(),
                            internalUriStringToStore,
                            displayName
                    );
                    // --- Start: Rest of insert logic ---
                    long newId = resourceDao.insertResource(newResource);
                    runOnUiThread(() -> {
                        if (newId != -1) {
                            Toast.makeText(ResourceUploadActivity.this, "Resource uploaded successfully.", Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK); // Indicate success to calling fragment/activity
                            finish();
                        } else {
                            Toast.makeText(ResourceUploadActivity.this, "Failed to save resource to database.", Toast.LENGTH_LONG).show();
                        }
                        // Reset UI elements specific to file selection in create mode if not finishing
                        // selectedFileSourceUri = null; // Already doing this based on your flow
                        // uploadPdfButton.setEnabled(false);
                    });
                    // --- End: Rest of insert logic ---
                } else { // Edit mode, and a new file was selected and copied
                    // Optional: Delete old internal file if replacing - This logic is already present above this block
                    // (Keep the old file deletion logic as it was, it's fine there if internalFileUri for new file is successfully created)

                    resourceToEdit.setResource(internalUriStringToStore); // Update with new internal file URI
                    resourceToEdit.setName(displayName);                  // Update display name
                    resourceToEdit.setSubjectId(selectedSubjectId.get()); // Update subject
                    // Note: resourceToEdit.setTutorId() should not change here as it's an edit by the same tutor.

                    // --- Start: Rest of update logic ---
                    int rowsAffected = resourceDao.updateResource(resourceToEdit);
                    runOnUiThread(() -> {
                        if (rowsAffected > 0) {
                            Toast.makeText(ResourceUploadActivity.this, "Resource (including file) updated successfully.", Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK); // Indicate success
                            finish();
                        } else {
                            Toast.makeText(ResourceUploadActivity.this, "Failed to update resource in database.", Toast.LENGTH_LONG).show();
                        }
                        // Reset UI for next potential selection if not finishing
                        // selectedFileSourceUri = null;
                        // uploadPdfButton.setEnabled(true); // Still in edit mode, button stays active for detail changes
                    });
                    // --- End: Rest of update logic ---
                }
            } else {
                runOnUiThread(() -> Toast.makeText(ResourceUploadActivity.this, "Failed to get internal file URI after copy.", Toast.LENGTH_LONG).show());
            }
        });
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null) return "unknown_file";
        // Replace characters that might be problematic in file names on some systems
        return originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}