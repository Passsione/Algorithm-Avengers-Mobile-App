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
import java.io.InputStream;
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

        initializeViews();
        setupInsets();

        loggedInUser = LogInUser.getInstance();
        currentUserPojo = loggedInUser.getUser(); // Using the method from your LogInUser
        resourceDao = new ResourceDao();
        subjectDao = new SubjectDao();
        firebaseStorage = FirebaseStorage.getInstance();

        if (currentUserPojo == null || currentUserPojo.getUid() == null || !currentUserPojo.isTutor()) {
            Toast.makeText(this, "You must be logged in as a tutor to manage resources.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "User not authorized or data missing. UID: " + (currentUserPojo != null ? currentUserPojo.getUid() : "null user"));
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
            subjectSpinner.setSelection(0); // Default selection
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
                    String existingFileName = getFileNameFromUrl(existingResourceToEdit.getFileUrl());
                    tvSelectedFileNameLabel.setText("Current file: " + (TextUtils.isEmpty(existingFileName) ? "N/A" : existingFileName) );
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
                    uploadOrUpdateButton.setEnabled(true);
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
                uploadOrUpdateButton.setEnabled(true);
                showLoading(false);
                return;
            }
            uploadFileToStorageAndSaveMetadata(resourceName, resourceDescription, selectedFileUri);
        } else if (MODE_EDIT.equals(currentMode) && existingResourceToEdit != null) {
            if (selectedFileUri != null) { // New file was selected for an existing resource
                String oldStoragePath = existingResourceToEdit.getStoragePath();
                deleteOldFileFromStorage(oldStoragePath, () -> {
                    uploadFileToStorageAndSaveMetadata(resourceName, resourceDescription, selectedFileUri);
                }, e -> { // Failure to delete old file
                    Log.e(TAG, "Failed to delete old file, but proceeding with new upload: " + e.getMessage());
                    // Proceed to upload new file even if old one couldn't be deleted, to not block user.
                    // This might leave orphaned files in storage if not handled carefully.
                    uploadFileToStorageAndSaveMetadata(resourceName, resourceDescription, selectedFileUri);
                });
            } else { // No new file, just update metadata
                updateResourceMetadataOnly(resourceName, resourceDescription);
            }
        } else {
            Log.e(TAG, "handleUploadOrUpdate: Invalid mode or existingResourceToEdit is null in EDIT mode.");
            Toast.makeText(this, "Error processing request.", Toast.LENGTH_SHORT).show();
            uploadOrUpdateButton.setEnabled(true);
            showLoading(false);
        }
    }



    private void updateResourceMetadataOnly(String name, String description) {
        if (existingResourceToEdit == null) {
            handleSaveFailure("Cannot update metadata: existing resource data is missing.", null);
            return;
        }
        existingResourceToEdit.setName(name);
        existingResourceToEdit.setDescription(description);
        existingResourceToEdit.setSubjectId(selectedSubjectId);
        existingResourceToEdit.setSubjectName(selectedSubjectName);
        // existingResourceToEdit.setUploadedAt(...); // Keep original upload time or set a "lastModified" time

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
        Log.d(TAG, "Attempting to delete old file from Storage: " + storagePath);
        StorageReference oldFileRef = firebaseStorage.getReference().child(storagePath);
        oldFileRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Old file deleted from storage: " + storagePath);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    // Log the error but still call onSuccess to allow new file upload.
                    // This prevents user from being blocked if old file deletion fails (e.g., already deleted, permissions changed).
                    // Consider if this behavior is desired or if it should call onFailure(e).
                    Log.w(TAG, "Failed to delete old file from storage: " + storagePath + ". Proceeding with new upload.", e);
                    onSuccess.run();
                    // If stricter, you might call: onFailure.onFailure(e);
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
        uploadOrUpdateButton.setEnabled(true);
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
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
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
            // Try to remove UUID prefix if present (common pattern: UUID_actualfilename.ext)
            if (fileName.length() > 37 && fileName.charAt(36) == '_') { // 36 chars for UUID
                return fileName.substring(37);
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing file name from URL: " + url, e);
        }
        return "linked_resource";
    }


    private String getFileExtension(Uri uri) {
        String extension = null;
        if (uri == null) return "bin"; // Default extension
        String fileName = getFileNameFromUri(uri); // Get the display name first

        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            String type = getContentResolver().getType(uri);
            extension = mime.getExtensionFromMimeType(type);
        } else { // For file URIs or others
            String path = uri.getPath();
            if (path != null) {
                extension = MimeTypeMap.getFileExtensionFromUrl(path);
            }
        }
        // Fallback: try to get from the filename itself if other methods fail
        if ((extension == null || extension.isEmpty()) && fileName != null && !fileName.equals("unknown_file")) {
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0 && lastDot < fileName.length() - 1) {
                extension = fileName.substring(lastDot + 1).toLowerCase(Locale.US);
            }
        }
        return (extension != null && !extension.isEmpty()) ? extension : "bin";
    }

    private String getMimeType(Uri uri) {
        String mimeType = null;
        if (uri == null) return "application/octet-stream";
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            mimeType = getContentResolver().getType(uri);
        } else {
            String fileExtension = getFileExtension(uri);
            if (fileExtension != null && !fileExtension.equals("bin")) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase(Locale.US));
            }
        }
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    private void uploadFileToStorageAndSaveMetadata(String name, String description, Uri fileUri) {
        // Validate inputs first
        if (fileUri == null) {
            handleSaveFailure("File URI is null", null);
            return;
        }

        if (currentUserPojo == null || currentUserPojo.getUid() == null) {
            handleSaveFailure("User data is invalid", null);
            return;
        }

        // Verify file exists and is accessible
        if (!verifyFileAccess(fileUri)) {
            handleSaveFailure("Cannot access selected file. Please try selecting again.", null);
            return;
        }

        String originalFileName = getFileNameFromUri(fileUri);
        String mimeType = getMimeType(fileUri);

        // Create a unique filename to avoid conflicts
        String uniqueFileName = System.currentTimeMillis() + "_" + originalFileName;

        // Build storage path
        final StorageReference fileRef = firebaseStorage.getReference()
                .child(ResourceDao.STORAGE_RESOURCES_PATH) // "study_resources"
                .child(currentUserPojo.getUid())
                .child(uniqueFileName);

        Log.d(TAG, "Starting upload:");
        Log.d(TAG, "  File URI: " + fileUri);
        Log.d(TAG, "  Storage path: " + fileRef.getPath());
        Log.d(TAG, "  MIME type: " + mimeType);
        Log.d(TAG, "  Original filename: " + originalFileName);

        // Start upload
        UploadTask uploadTask = fileRef.putFile(fileUri);

        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            Log.d(TAG, "Upload progress: " + (int) progress + "%");
        }).addOnSuccessListener(taskSnapshot -> {
            Log.d(TAG, "Upload successful, getting download URL...");

            // Get download URL
            fileRef.getDownloadUrl()
                    .addOnSuccessListener(downloadUri -> {
                        Log.d(TAG, "Download URL obtained: " + downloadUri);
                        saveResourceToFirestore(name, description, downloadUri.toString(),
                                fileRef.getPath(), mimeType, getFileSize(fileUri));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get download URL", e);
                        handleSaveFailure("Failed to get download URL", e);
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Upload failed", e);

            // Enhanced error logging for Storage exceptions
            if (e instanceof com.google.firebase.storage.StorageException) {
                com.google.firebase.storage.StorageException se = (com.google.firebase.storage.StorageException) e;
                Log.e(TAG, "StorageException details:");
                Log.e(TAG, "  Error code: " + se.getErrorCode());
                Log.e(TAG, "  HTTP result code: " + se.getHttpResultCode());
                Log.e(TAG, "  Message: " + se.getMessage());

                String userMessage = "Upload failed: ";
                switch (se.getErrorCode()) {
                    case com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND:
                        userMessage += "File not found. Please select the file again.";
                        break;
                    case com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED:
                        userMessage += "Not authorized. Check your login status.";
                        break;
                    case com.google.firebase.storage.StorageException.ERROR_BUCKET_NOT_FOUND:
                        userMessage += "Storage bucket not found. Contact support.";
                        break;
                    default:
                        userMessage += se.getMessage();
                }
                handleSaveFailure(userMessage, e);
            } else {
                handleSaveFailure("Upload failed: " + e.getMessage(), e);
            }
        });
    }

    // Add this new method to verify file access
    private boolean verifyFileAccess(Uri uri) {
        if (uri == null) return false;

        try {
            // Try to open an input stream to verify the file is accessible
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                if (inputStream == null) {
                    Log.e(TAG, "Cannot open input stream for URI: " + uri);
                    return false;
                }

                // Try to read first few bytes to ensure file is actually accessible
                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);
                Log.d(TAG, "File verification successful. Read " + bytesRead + " bytes from: " + uri);
                return bytesRead > 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot access file at URI: " + uri, e);
            return false;
        }
    }

    // Add this helper method to get file size
    private long getFileSize(Uri uri) {
        long fileSize = 0;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    fileSize = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size for URI: " + uri, e);
        }
        Log.d(TAG, "File size: " + fileSize + " bytes");
        return fileSize;
    }

    // Split the Firestore saving into a separate method for clarity
    private void saveResourceToFirestore(String name, String description, String downloadUrl,
                                         String storagePath, String mimeType, long fileSize) {
        Resource resource = new Resource(
                name, description, currentUserPojo.getUid(),
                selectedSubjectId, selectedSubjectName,
                downloadUrl, storagePath, mimeType, fileSize
        );
        resource.setUploadedAt(com.google.firebase.Timestamp.now());

        if (MODE_EDIT.equals(currentMode) && existingResourceToEdit != null) {
            resource.setId(existingResourceToEdit.getId());
            resourceDao.updateResourceMetadata(existingResourceToEdit.getId(), resource)
                    .addOnSuccessListener(aVoid -> handleSaveSuccess("Resource updated successfully."))
                    .addOnFailureListener(e -> handleSaveFailure("Failed to update resource metadata.", e));
        } else {
            resourceDao.saveResourceMetadata(resource)
                    .addOnSuccessListener(docRef -> handleSaveSuccess("Resource uploaded successfully."))
                    .addOnFailureListener(e -> handleSaveFailure("Failed to save resource metadata.", e));
        }
    }

    // Improved file selection launcher
    private void setupActivityLaunchers() {
        fileSelectionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedFileUri = result.getData().getData();
                        if (selectedFileUri != null) {
                            Log.d(TAG, "File selected: " + selectedFileUri);

                            // Verify file access immediately
                            if (verifyFileAccess(selectedFileUri)) {
                                String fileName = getFileNameFromUri(selectedFileUri);
                                tvSelectedFileNameLabel.setText("Selected: " + fileName);
                                tvSelectedFileNameLabel.setVisibility(View.VISIBLE);

                                // Auto-fill resource name if empty and in upload mode
                                if (TextUtils.isEmpty(txtResourceName.getText()) && MODE_UPLOAD.equals(currentMode)) {
                                    String nameWithoutExtension = fileName.replaceFirst("[.][^.]+$", "");
                                    txtResourceName.setText(nameWithoutExtension);
                                }

                                uploadOrUpdateButton.setEnabled(true);
                                uploadOrUpdateButton.setVisibility(View.VISIBLE);
                            } else {
                                Toast.makeText(this, "Cannot access selected file. Please try again.", Toast.LENGTH_LONG).show();
                                selectedFileUri = null;
                                tvSelectedFileNameLabel.setText("");
                                tvSelectedFileNameLabel.setVisibility(View.GONE);
                                uploadOrUpdateButton.setEnabled(false);
                                if (MODE_UPLOAD.equals(currentMode)) {
                                    uploadOrUpdateButton.setVisibility(View.GONE);
                                }
                            }
                        } else {
                            Toast.makeText(this, "Error: No file selected.", Toast.LENGTH_SHORT).show();
                            resetFileSelection();
                        }
                    } else {
                        Log.d(TAG, "File selection cancelled or failed");
                        if (MODE_UPLOAD.equals(currentMode) && selectedFileUri == null) {
                            resetFileSelection();
                        }
                    }
                });
    }

    // Helper method to reset file selection UI
    private void resetFileSelection() {
        uploadOrUpdateButton.setEnabled(false);
        uploadOrUpdateButton.setVisibility(View.GONE);
        tvSelectedFileNameLabel.setText("");
        tvSelectedFileNameLabel.setVisibility(View.GONE);
    }

    // Improved file selection method
    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");

        // Specify supported MIME types
        String[] mimeTypes = {
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain",
                "image/jpeg",
                "image/png",
                "image/jpg"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            fileSelectionLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching file selector", e);
            Toast.makeText(this, "Error opening file selector", Toast.LENGTH_SHORT).show();
        }
    }
}
