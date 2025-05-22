package com.pbdvmobile.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.dao.ResourceDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ResourceUploadActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "com.pbdvmobile.app.EXTRA_MODE";
    public static final String MODE_UPLOAD = "MODE_UPLOAD";
    public static final String MODE_EDIT = "MODE_EDIT";
    public static final String EXTRA_RESOURCE_ID = "com.pbdvmobile.app.EXTRA_RESOURCE_ID";
    private static final String TAG = "ResourceUploadActivity";

    private Button selectFileButton, uploadOrUpdateButton, cancelButton;
    private Uri selectedFileUri;
    private TextInputEditText txtResourceName, txtResourceDescription;
    private TextView tvSelectedFileNameLabel, tvActivityTitle;
    private Spinner subjectSpinner;
    private ProgressBar uploadProgressBar;

    private LogInUser loggedInUser;
    private User currentUserPojo;
    private ResourceDao resourceDao;
    private SubjectDao subjectDao;
    private FirebaseStorage firebaseStorage;

    private String currentMode = MODE_UPLOAD;
    private String resourceToEditId;
    private Resource existingResourceToEdit;

    private List<Subject> availableSubjectsForSpinner = new ArrayList<>();
    private String selectedSubjectId;
    private String selectedSubjectName;

    private ActivityResultLauncher<Intent> fileSelectionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource_upload);

        initializeViews(); // Make sure these IDs match your XML
        setupInsets();

        loggedInUser = LogInUser.getInstance();
        // Corrected to use the method from the LogInUser class you provided
        currentUserPojo = loggedInUser.getUser();
        resourceDao = new ResourceDao();
        subjectDao = new SubjectDao();
        firebaseStorage = FirebaseStorage.getInstance();

        if (currentUserPojo == null || !currentUserPojo.isTutor()) {
            Toast.makeText(this, "You must be logged in as a tutor to upload resources.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent intent = getIntent();
        currentMode = intent.getStringExtra(EXTRA_MODE) != null ? intent.getStringExtra(EXTRA_MODE) : MODE_UPLOAD;
        resourceToEditId = intent.getStringExtra(EXTRA_RESOURCE_ID);

        setupActivityLaunchers();
        loadSubjectsForSpinner();

        selectFileButton.setOnClickListener(v -> selectFile());
        uploadOrUpdateButton.setOnClickListener(v -> handleUploadOrUpdate());
        cancelButton.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    private void initializeViews() {
        // Using IDs from the Java file you provided in THIS turn
        selectFileButton = findViewById(R.id.selectPdfButton);
        uploadOrUpdateButton = findViewById(R.id.uploadPdfButton);
        tvActivityTitle = findViewById(R.id.textViewUploadTitle);
        txtResourceName = findViewById(R.id.resourceNameEditText);
        txtResourceDescription = findViewById(R.id.resourceDescriptionEditText);
        tvSelectedFileNameLabel = findViewById(R.id.upload_doc_name_label);
        subjectSpinner = findViewById(R.id.subjectDropdown);
        cancelButton = findViewById(R.id.upload_cancel_button);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainResourceUploadLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupActivityLaunchers() {
        fileSelectionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedFileUri = result.getData().getData();
                        if (selectedFileUri != null) {
                            String fileName = getFileNameFromUri(selectedFileUri);
                            tvSelectedFileNameLabel.setText("Selected: " + fileName);
                            tvSelectedFileNameLabel.setVisibility(View.VISIBLE);
                            if (TextUtils.isEmpty(txtResourceName.getText()) && MODE_UPLOAD.equals(currentMode)) {
                                txtResourceName.setText(fileName.replaceFirst("[.][^.]+$", ""));
                            }
                            uploadOrUpdateButton.setEnabled(true); // Enable button on successful selection
                            uploadOrUpdateButton.setVisibility(View.VISIBLE); // Ensure it's visible
                        } else {
                            Toast.makeText(this, "Error selecting file.", Toast.LENGTH_SHORT).show();
                            uploadOrUpdateButton.setEnabled(false); // Disable if selection failed
                            uploadOrUpdateButton.setVisibility(View.GONE);
                        }
                    } else {
                        // File selection cancelled or failed, ensure button is appropriately set
                        if(selectedFileUri == null && MODE_UPLOAD.equals(currentMode)) { // Only disable if no file was previously selected in UPLOAD mode
                            uploadOrUpdateButton.setEnabled(false);
                            uploadOrUpdateButton.setVisibility(View.GONE);
                        }
                        // In EDIT mode, button might remain enabled if user just wants to update metadata
                    }
                });
    }

    private void loadSubjectsForSpinner() {
        showLoading(true);
        List<String> tutoredIds = currentUserPojo.getTutoredSubjectIds();
        if (tutoredIds == null || tutoredIds.isEmpty()) {
            Toast.makeText(this, "You are not set to tutor any subjects. Please update your profile.", Toast.LENGTH_LONG).show();
            showLoading(false);
            finish();
            return;
        }

        List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot>> subjectTasks = new ArrayList<>();
        for (String id : tutoredIds) {
            subjectTasks.add(subjectDao.getSubjectById(id));
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(subjectTasks).addOnSuccessListener(results -> {
            availableSubjectsForSpinner.clear();
            for (Object snapshot : results) {
                com.google.firebase.firestore.DocumentSnapshot doc = (com.google.firebase.firestore.DocumentSnapshot) snapshot;
                if (doc.exists()) {
                    Subject subject = doc.toObject(Subject.class);
                    if (subject != null) {
                        subject.setId(doc.getId());
                        availableSubjectsForSpinner.add(subject);
                    }
                }
            }
            if (availableSubjectsForSpinner.isEmpty()) {
                Toast.makeText(this, "No tutored subjects found. Please update your profile.", Toast.LENGTH_LONG).show();
                showLoading(false);
                finish();
                return;
            }
            populateSubjectSpinner();
            if (MODE_EDIT.equals(currentMode) && resourceToEditId != null) {
                loadResourceForEditing();
            } else {
                tvActivityTitle.setText("Upload New Resource");
                uploadOrUpdateButton.setText("Upload Resource");
                // Button is disabled initially in UPLOAD mode until a file is selected
                uploadOrUpdateButton.setEnabled(false);
                uploadOrUpdateButton.setVisibility(View.GONE);
                showLoading(false);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching tutored subjects", e);
            Toast.makeText(this, "Could not load subjects for selection.", Toast.LENGTH_LONG).show();
            showLoading(false);
            finish();
        });
    }

    private void populateSubjectSpinner() {
        List<String> subjectNames = new ArrayList<>();
        for (Subject subject : availableSubjectsForSpinner) {
            subjectNames.add(subject.getSubjectName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(adapter);

        subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < availableSubjectsForSpinner.size()) {
                    selectedSubjectId = availableSubjectsForSpinner.get(position).getId();
                    selectedSubjectName = availableSubjectsForSpinner.get(position).getSubjectName();
                } else {
                    selectedSubjectId = null;
                    selectedSubjectName = null;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSubjectId = null;
                selectedSubjectName = null;
            }
        });
        if (!availableSubjectsForSpinner.isEmpty()) {
            subjectSpinner.setSelection(0);
            selectedSubjectId = availableSubjectsForSpinner.get(0).getId();
            selectedSubjectName = availableSubjectsForSpinner.get(0).getSubjectName();
        }
    }

    private void loadResourceForEditing() {
        tvActivityTitle.setText("Edit Resource");
        uploadOrUpdateButton.setText("Update Resource");
        showLoading(true);

        resourceDao.getResourceById(resourceToEditId).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                existingResourceToEdit = documentSnapshot.toObject(Resource.class);
                if (existingResourceToEdit != null) {
                    existingResourceToEdit.setId(documentSnapshot.getId());
                    txtResourceName.setText(existingResourceToEdit.getName());
                    txtResourceDescription.setText(existingResourceToEdit.getDescription());
                    tvSelectedFileNameLabel.setText("Current file: " + getFileNameFromUrl(existingResourceToEdit.getFileUrl()));
                    tvSelectedFileNameLabel.setVisibility(View.VISIBLE);

                    if (subjectSpinner.getAdapter() != null && existingResourceToEdit.getSubjectId() != null) {
                        for (int i = 0; i < availableSubjectsForSpinner.size(); i++) {
                            if (availableSubjectsForSpinner.get(i).getId().equals(existingResourceToEdit.getSubjectId())) {
                                subjectSpinner.setSelection(i);
                                selectedSubjectId = existingResourceToEdit.getSubjectId();
                                selectedSubjectName = availableSubjectsForSpinner.get(i).getSubjectName();
                                break;
                            }
                        }
                    }
                    uploadOrUpdateButton.setEnabled(true); // Enable for metadata update
                    uploadOrUpdateButton.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(this, "Error parsing resource data for editing.", Toast.LENGTH_LONG).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "Resource to edit not found.", Toast.LENGTH_LONG).show();
                finish();
            }
            showLoading(false);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading resource for editing", e);
            Toast.makeText(this, "Error loading resource: " + e.getMessage(), Toast.LENGTH_LONG).show();
            showLoading(false);
            finish();
        });
    }


    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain", "image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        fileSelectionLauncher.launch(intent);
    }

    private void handleUploadOrUpdate() {
        String resourceName = txtResourceName.getText().toString().trim();
        String resourceDescription = txtResourceDescription.getText().toString().trim();

        if (TextUtils.isEmpty(resourceName)) {
            txtResourceName.setError("Resource name cannot be empty");
            txtResourceName.requestFocus();
            return;
        }
        if (selectedSubjectId == null || selectedSubjectId.isEmpty()) {
            Toast.makeText(this, "Please select a subject.", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadOrUpdateButton.setEnabled(false);
        showLoading(true);

        if (MODE_UPLOAD.equals(currentMode)) {
            if (selectedFileUri == null) {
                Toast.makeText(this, "No file selected for upload.", Toast.LENGTH_SHORT).show();
                uploadOrUpdateButton.setEnabled(true); // Re-enable
                showLoading(false);
                return;
            }
            uploadFileToStorageAndSaveMetadata(resourceName, resourceDescription, selectedFileUri);
        } else if (MODE_EDIT.equals(currentMode) && existingResourceToEdit != null) {
            if (selectedFileUri != null) {
                String oldStoragePath = existingResourceToEdit.getStoragePath();
                deleteOldFileFromStorage(oldStoragePath, () -> {
                    uploadFileToStorageAndSaveMetadata(resourceName, resourceDescription, selectedFileUri);
                }, e -> {
                    Toast.makeText(this, "Failed to delete old file. Update aborted. " + e.getMessage(), Toast.LENGTH_LONG).show();
                    uploadOrUpdateButton.setEnabled(true); // Re-enable
                    showLoading(false);
                });
            } else {
                updateResourceMetadataOnly(resourceName, resourceDescription);
            }
        }
    }

    private void uploadFileToStorageAndSaveMetadata(String name, String description, Uri fileUri) {
        String originalFileName = getFileNameFromUri(fileUri);
        String fileExtension = getFileExtension(fileUri);
        String mimeType = getMimeType(fileUri);

        final StorageReference fileRef = firebaseStorage.getReference()
                .child(ResourceDao.STORAGE_RESOURCES_PATH)
                .child(currentUserPojo.getUid())
                .child(UUID.randomUUID().toString() + (fileExtension != null ? "." + fileExtension : "")); // Ensure extension is added

        UploadTask uploadTask = fileRef.putFile(fileUri);
        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            Log.d(TAG, "Upload is " + progress + "% done");
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return fileRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                long fileSize = 0;
                try (Cursor cursor = getContentResolver().query(fileUri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) fileSize = cursor.getLong(sizeIndex);
                    }
                } catch (Exception e) { Log.e(TAG, "Error getting file size", e); }


                Resource resource = new Resource(
                        name, description, currentUserPojo.getUid(),
                        selectedSubjectId, selectedSubjectName,
                        downloadUri.toString(), fileRef.getPath(),
                        mimeType, fileSize
                );

                if (MODE_EDIT.equals(currentMode) && existingResourceToEdit != null) {
                    resource.setId(existingResourceToEdit.getId());
                    resource.setUploadedAt(existingResourceToEdit.getUploadedAt()); // Preserve original upload date unless new file
                    if(selectedFileUri != null) resource.setUploadedAt(com.google.firebase.Timestamp.now()); // Update if new file

                    resourceDao.updateResourceMetadata(existingResourceToEdit.getId(), resource)
                            .addOnSuccessListener(aVoid -> handleSaveSuccess("Resource updated successfully."))
                            .addOnFailureListener(e -> handleSaveFailure("Failed to update resource metadata.", e));
                } else {
                    resourceDao.saveResourceMetadata(resource)
                            .addOnSuccessListener(docRef -> handleSaveSuccess("Resource uploaded successfully."))
                            .addOnFailureListener(e -> handleSaveFailure("Failed to save resource metadata.", e));
                }
            } else {
                handleSaveFailure("File upload failed.", task.getException());
            }
        });
    }

    private void updateResourceMetadataOnly(String name, String description) {
        existingResourceToEdit.setName(name);
        existingResourceToEdit.setDescription(description);
        existingResourceToEdit.setSubjectId(selectedSubjectId);
        existingResourceToEdit.setSubjectName(selectedSubjectName);
        // existingResourceToEdit.setUploadedAt(com.google.firebase.Timestamp.now()); // Optionally update modified time

        resourceDao.updateResourceMetadata(existingResourceToEdit.getId(), existingResourceToEdit)
                .addOnSuccessListener(aVoid -> handleSaveSuccess("Resource details updated successfully."))
                .addOnFailureListener(e -> handleSaveFailure("Failed to update resource details.", e));
    }

    private void deleteOldFileFromStorage(String storagePath, Runnable onSuccess, com.google.android.gms.tasks.OnFailureListener onFailure) {
        if (storagePath == null || storagePath.isEmpty()) {
            Log.d(TAG, "No old file storage path to delete.");
            onSuccess.run();
            return;
        }
        StorageReference oldFileRef = firebaseStorage.getReference().child(storagePath);
        oldFileRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Old file deleted from storage: " + storagePath);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to delete old file from storage: " + storagePath + ". This might be okay if the path was invalid or file already deleted.", e);
                    onSuccess.run(); // Proceed even if old file deletion fails, to not block user.
                    // Consider if this should call onFailure(e) instead for stricter error handling.
                });
    }


    private void handleSaveSuccess(String message) {
        Toast.makeText(ResourceUploadActivity.this, message, Toast.LENGTH_LONG).show();
        setResult(Activity.RESULT_OK);
        finish();
    }

    private void handleSaveFailure(String message, Exception e) {
        Toast.makeText(ResourceUploadActivity.this, message + (e != null ? ": " + e.getMessage() : ""), Toast.LENGTH_LONG).show();
        Log.e(TAG, message, e);
        uploadOrUpdateButton.setEnabled(true); // Re-enable button on failure
        showLoading(false);
    }

    private void showLoading(boolean show) {
        if (uploadProgressBar != null) {
            uploadProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (uploadOrUpdateButton != null) uploadOrUpdateButton.setEnabled(!show);
        if (selectFileButton != null) selectFileButton.setEnabled(!show);
        if (cancelButton != null) cancelButton.setEnabled(!show);
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri == null) return "unknown_file";
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) { // Check for -1 and null
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name from content URI", e);
            }
        }
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        return fileName != null ? fileName : "unknown_file";
    }

    private String getFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) return "unknown_file";
        try {
            String decodedUrl = Uri.decode(url);
            int lastSlash = decodedUrl.lastIndexOf('/');
            String segmentWithParams = (lastSlash != -1 && lastSlash < decodedUrl.length() - 1) ? decodedUrl.substring(lastSlash + 1) : decodedUrl;
            int queryParamStart = segmentWithParams.indexOf('?');
            String fileName = (queryParamStart != -1) ? segmentWithParams.substring(0, queryParamStart) : segmentWithParams;
            int firstUnderscore = fileName.indexOf('_');
            // Heuristic for UUID_filename.ext, try to extract filename.ext
            if (fileName.length() > 37 && firstUnderscore == 36) {
                return fileName.substring(firstUnderscore + 1);
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing file name from URL: " + url, e);
        }
        return "linked_resource";
    }


    private String getFileExtension(Uri uri) {
        String extension = null;
        if (uri == null) return "bin";
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(getContentResolver().getType(uri));
        } else {
            String path = uri.getPath();
            if (path != null) {
                extension = MimeTypeMap.getFileExtensionFromUrl(path);
            }
        }
        // Fallback if extension is still null, try to get from filename itself
        if (extension == null) {
            String name = getFileNameFromUri(uri);
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0 && lastDot < name.length() - 1) {
                extension = name.substring(lastDot + 1).toLowerCase(Locale.US);
            }
        }
        return extension != null ? extension : "bin";
    }

    private String getMimeType(Uri uri) {
        String mimeType = null;
        if (uri == null) return "application/octet-stream";
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            mimeType = getContentResolver().getType(uri);
        } else {
            String fileExtension = getFileExtension(uri); // Use our improved getFileExtension
            if (fileExtension != null && !fileExtension.equals("bin")) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase(Locale.US));
            }
        }
        return mimeType != null ? mimeType : "application/octet-stream";
    }
}
