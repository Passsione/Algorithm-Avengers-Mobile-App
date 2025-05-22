package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.app.Activity;
// Removed ContentResolver import as it's not directly used after getFileNameFromUri was removed from here
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Switch; // android.widget.Switch
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final String PROFILE_IMAGES_PATH = "profile_images/";

    private LogInUser loggedInUser;
    private UserDao userDao;
    private SubjectDao subjectDao;
    private SessionDao sessionDao;
    private FirebaseStorage firebaseStorage;

    private ImageView imgProfileImage;
    private FloatingActionButton fabUpdateProfileImage;
    private Button btnSaveProfile, btnProfileLogOut;
    private LinearLayout subjectSelectionLayout;
    private Switch tutorSwitch;
    private EditText edtFirstName, edtLastName, edtBio;
    private TextView txtEmailDisplay, txtCreditsDisplay;
    private RatingBar ratingBarTutorOverall, ratingBarTuteeOverall;
    private ProgressBar profileProgressBar;

    private User currentUserPojo;
    private List<Subject> allSubjectsList = new ArrayList<>();
    private List<String> selectedTutoredSubjectIds = new ArrayList<>();

    private Uri cameraImageUri; // To store URI from camera after FileProvider creates it

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher; // For new contract

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        loggedInUser = LogInUser.getInstance();
        userDao = new UserDao();
        subjectDao = new SubjectDao();
        sessionDao = new SessionDao();
        firebaseStorage = FirebaseStorage.getInstance();

        // Use the method from your LogInUser.java
        currentUserPojo = loggedInUser.getUser();
        if (currentUserPojo == null || currentUserPojo.getUid() == null) { // Also check if UID is present
            Toast.makeText(this, "User data not found or invalid. Please log in again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "currentUserPojo or its UID is null in onCreate.");
            logoutUser(); // Defined method for logout
            return;
        }

        initializeActivityLaunchers();
        initializeViews();
        showLoading(true);
        loadAllSubjectsAndThenUserData();
        setupListeners();
    }

    private void initializeViews() {
        imgProfileImage = findViewById(R.id.imgProfileImage);
        fabUpdateProfileImage = findViewById(R.id.updateProfileImage);
        btnSaveProfile = findViewById(R.id.bthSaveProfile);
        btnProfileLogOut = findViewById(R.id.btnProfileLogOut);
        tutorSwitch = findViewById(R.id.togProfileTutor);
        txtEmailDisplay = findViewById(R.id.txtEmail);
        edtFirstName = findViewById(R.id.edtProfileFirstName);
        edtLastName = findViewById(R.id.edtProfileLastName);
        edtBio = findViewById(R.id.edtProfileBio);
        ratingBarTuteeOverall = findViewById(R.id.ProfileTuteeRating);
        ratingBarTutorOverall = findViewById(R.id.ProfileTutorRating);
        subjectSelectionLayout = findViewById(R.id.ProfileSubjectCard);
        profileProgressBar = findViewById(R.id.profileProgressBar);
        txtCreditsDisplay = findViewById(R.id.txtProfileCredits);
    }

    private void initializeActivityLaunchers() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            Log.d(TAG, "Image picked from gallery: " + imageUri.toString());
                            uploadProfileImageToStorage(imageUri);
                        } else {
                            Log.w(TAG, "pickImageLauncher: imageUri is null from gallery data.");
                            Toast.makeText(this, "Failed to get image from gallery.", Toast.LENGTH_SHORT).show();
                        }
                    }  else {
                        Log.d(TAG, "pickImageLauncher: Gallery selection cancelled or failed. Result code: " + result.getResultCode());
                    }
                });

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success) {
                        if (cameraImageUri != null) {
                            Log.d(TAG, "Image captured with camera: " + cameraImageUri.toString());
                            uploadProfileImageToStorage(cameraImageUri);
                        } else {
                            // This case should ideally not happen if createImageFileForCamera and FileProvider are correct.
                            Log.e(TAG, "takePictureLauncher: Camera success but cameraImageUri is null! This indicates an issue with file creation or URI passing.");
                            Toast.makeText(this, "Error: Could not retrieve image from camera.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "takePictureLauncher: Camera capture failed or was cancelled by user.");
                        // No need for a toast if user cancels.
                    }
                });
    }

    private void loadAllSubjectsAndThenUserData() {
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
            Log.d(TAG, "Loaded " + allSubjectsList.size() + " subjects.");
            loadUserData();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load subjects", e);
            Toast.makeText(this, "Could not load subject list.", Toast.LENGTH_LONG).show();
            loadUserData();
        });
    }

    private void loadUserData() {
        if (currentUserPojo == null) { // Double check after async subject load
            Toast.makeText(this, "User data became unavailable. Please try again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "currentUserPojo is null in loadUserData.");
            logoutUser();
            showLoading(false);
            return;
        }

        Glide.with(this)
                .load(currentUserPojo.getProfileImageUrl())
                .placeholder(R.drawable.avatar_1)
                .error(R.drawable.avatar_1)
                .circleCrop()
                .into(imgProfileImage);

        edtFirstName.setText(currentUserPojo.getFirstName());
        edtLastName.setText(currentUserPojo.getLastName());
        edtBio.setText(currentUserPojo.getBio());
        txtEmailDisplay.setText(currentUserPojo.getEmail());
        txtCreditsDisplay.setText(String.format(Locale.getDefault(),"Credits: %.2f", currentUserPojo.getCredits()));

        sessionDao.getAverageRatingByFirebaseUid(currentUserPojo.getUid(), new SessionDao.RatingsCallback() {
            @Override
            public void onRatingsFetched(double averageRatingAsTutee, double averageRatingAsTutor) {
                if (currentUserPojo != null && ProfileActivity.this != null && !isFinishing()) {
                    currentUserPojo.setAverageRatingAsTutee(averageRatingAsTutee);
                    currentUserPojo.setAverageRatingAsTutor(averageRatingAsTutor);
                    updateRatingDisplayLocal();
                }
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching average ratings: ", e);
                if (currentUserPojo != null && ProfileActivity.this != null && !isFinishing()) updateRatingDisplayLocal();
            }
        });

        tutorSwitch.setEnabled(true);
        tutorSwitch.setChecked(currentUserPojo.isTutor());
        updateTutorSpecificUI(currentUserPojo.isTutor());
        showLoading(false);
    }

    private void updateRatingDisplayLocal() {
        if (currentUserPojo == null || isFinishing()) return;

        findViewById(R.id.layout_profile_tutee_rating).setVisibility(currentUserPojo.getAverageRatingAsTutee() > 0 ? VISIBLE : GONE);
        if (currentUserPojo.getAverageRatingAsTutee() > 0) ratingBarTuteeOverall.setRating((float) currentUserPojo.getAverageRatingAsTutee());

        updateTutorSpecificUI(currentUserPojo.isTutor());
    }


    private void updateTutorSpecificUI(boolean isTutor) {
        if (isFinishing()) return;
        LinearLayout tutorRatingLayout = findViewById(R.id.layout_profile_tutor_rating);
        if (isTutor) {
            if (currentUserPojo != null && currentUserPojo.getAverageRatingAsTutor() > 0) {
                tutorRatingLayout.setVisibility(VISIBLE);
                ratingBarTutorOverall.setRating((float) currentUserPojo.getAverageRatingAsTutor());
            } else {
                // Show "Not rated" or hide if 0, only if they are a tutor
                tutorRatingLayout.setVisibility(GONE);
            }
            subjectSelectionLayout.setVisibility(VISIBLE);
            displaySubjectSelectionCheckboxes();
        } else {
            tutorRatingLayout.setVisibility(GONE);
            subjectSelectionLayout.setVisibility(GONE);
        }
    }

    private void displaySubjectSelectionCheckboxes() {
        if (isFinishing()) return;
        subjectSelectionLayout.removeAllViews();
        selectedTutoredSubjectIds.clear();
        if (currentUserPojo.getTutoredSubjectIds() != null) {
            selectedTutoredSubjectIds.addAll(currentUserPojo.getTutoredSubjectIds());
        }

        if (allSubjectsList.isEmpty()) {
            TextView noSubjectsText = new TextView(this);
            noSubjectsText.setText("No subjects available to select for tutoring.");
            subjectSelectionLayout.addView(noSubjectsText);
            return;
        }

        TextView instructionText = new TextView(this);
        instructionText.setText("Select subjects you wish to tutor:");
        instructionText.setTextSize(16);
        instructionText.setPadding(0,0,0, dpToPx(8));
        subjectSelectionLayout.addView(instructionText);

        for (Subject subject : allSubjectsList) {
            CheckBox subjectCheckBox = new CheckBox(this);
            subjectCheckBox.setText(subject.getSubjectName());
            subjectCheckBox.setTag(subject.getId());
            subjectCheckBox.setChecked(selectedTutoredSubjectIds.contains(subject.getId()));

            subjectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String subjectIdFromTag = (String) buttonView.getTag();
                if (isChecked) {
                    if (!selectedTutoredSubjectIds.contains(subjectIdFromTag)) {
                        selectedTutoredSubjectIds.add(subjectIdFromTag);
                    }
                } else {
                    selectedTutoredSubjectIds.remove(subjectIdFromTag);
                }
            });
            subjectSelectionLayout.addView(subjectCheckBox);
        }
    }

    private void setupListeners() {
        fabUpdateProfileImage.setOnClickListener(v -> showImagePickerDialog());
        imgProfileImage.setOnClickListener(v -> showImagePickerDialog());
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
        btnProfileLogOut.setOnClickListener(v -> logoutUser());

        tutorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTutorSpecificUI(isChecked);
            if (!isChecked) {
                selectedTutoredSubjectIds.clear();
            }
        });
    }

    private void saveProfileChanges() {
        showLoading(true);
        String firstName = edtFirstName.getText().toString().trim();
        String lastName = edtLastName.getText().toString().trim();
        String bio = edtBio.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "First and Last name cannot be empty.", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("bio", bio);
        updates.put("tutor", tutorSwitch.isChecked());
        updates.put("tutoredSubjectIds", new ArrayList<>(selectedTutoredSubjectIds));

        userDao.updateUserSpecificFields(currentUserPojo.getUid(), updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                    currentUserPojo.setFirstName(firstName);
                    currentUserPojo.setLastName(lastName);
                    currentUserPojo.setBio(bio);
                    currentUserPojo.setTutor(tutorSwitch.isChecked());
                    currentUserPojo.setTutoredSubjectIds(new ArrayList<>(selectedTutoredSubjectIds));
                    loggedInUser.setUser(currentUserPojo);
                    showLoading(false);
                    updateRatingDisplayLocal();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to save profile", e);
                    showLoading(false);
                });
    }

    private void logoutUser() {
        loggedInUser.logOut();
        Intent intent = new Intent(ProfileActivity.this, LogInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    private void showImagePickerDialog() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Update Profile Photo")
                .setItems(options, (dialog, item) -> {
                    String option = options[item].toString();
                    if ("Take Photo".equals(option)) {
                        checkCameraPermissionAndLaunch();
                    } else if ("Choose from Gallery".equals(option)) {
                        pickImageFromGallery();
                    } else if ("Cancel".equals(option)) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            launchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFileForCamera();
        } catch (IOException ex) {
            Log.e(TAG, "Error creating image file for camera", ex);
            Toast.makeText(this, "Could not prepare camera.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            // Store the path for later use if needed, but FileProvider URI is primary
            // mCurrentPhotoPath = photoFile.getAbsolutePath();
            cameraImageUri = FileProvider.getUriForFile(this,
                    // Ensure this matches your AndroidManifest provider authority
                    getApplicationContext().getPackageName() + ".provider",
                    photoFile);
            Log.d(TAG, "Launching camera, image URI: " + cameraImageUri);
            takePictureLauncher.launch(cameraImageUri);
        } else {
            Log.e(TAG, "photoFile is null after createImageFileForCamera");
        }
    }

    private File createImageFileForCamera() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir == null) { // Fallback if getExternalFilesDir returns null
            Log.w(TAG, "ExternalFilesDir is null, attempting to use cache directory.");
            storageDir = getCacheDir();
            if (storageDir == null) { // If cache dir is also null, this is a bigger problem
                throw new IOException("Cannot get external or cache directory for images.");
            }
        }

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: " + storageDir.getAbsolutePath());
                // If mkdirs fails even on cache dir, throw error
                if (storageDir.equals(getCacheDir())) {
                    throw new IOException("Failed to create cache directory: " + storageDir.getAbsolutePath());
                }
                // Try cache dir as a last resort if external-files failed
                storageDir = getCacheDir();
                if (!storageDir.exists() && !storageDir.mkdirs()){
                    throw new IOException("Failed to create any directory for images.");
                }
            }
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        Log.d(TAG, "Camera image file created at: " + image.getAbsolutePath());
        return image;
    }


    private void uploadProfileImageToStorage(Uri imageUri) {
        if (imageUri == null || currentUserPojo == null || currentUserPojo.getUid() == null) {
            Toast.makeText(this, "Error: Cannot upload image. User data or image URI is missing.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "uploadProfileImageToStorage: imageUri=" + imageUri + ", currentUserPojo=" + currentUserPojo + (currentUserPojo != null ? ", UID=" + currentUserPojo.getUid() : ""));
            showLoading(false); // Ensure loading is hidden
            return;
        }
        showLoading(true);

        // Use a more consistent naming for profile images, e.g., just the UID or a fixed name like "profile.jpg"
        // Using System.currentTimeMillis() creates many files if user changes pic often.
        // For simplicity, let's stick to your current naming for now.
        final StorageReference profileImageRef = firebaseStorage.getReference()
                .child(PROFILE_IMAGES_PATH + currentUserPojo.getUid() + "/" + "profile_image.jpg"); // Consistent name

        Log.d(TAG, "Uploading to Storage path: " + profileImageRef.getPath() + " from URI: " + imageUri.toString());

        UploadTask uploadTask = profileImageRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String downloadUrl = uri.toString();
            Log.d(TAG, "Image uploaded successfully. Download URL: " + downloadUrl);
            updateUserProfileImageUrlInFirestore(downloadUrl);
        }).addOnFailureListener(e -> {
            Toast.makeText(ProfileActivity.this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to get download URL for " + profileImageRef.getPath(), e);
            showLoading(false);
        })).addOnFailureListener(e -> {
            Toast.makeText(ProfileActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Image upload failed for " + profileImageRef.getPath(), e);
            // Check StorageException details
            if (e instanceof com.google.firebase.storage.StorageException) {
                com.google.firebase.storage.StorageException se = (com.google.firebase.storage.StorageException) e;
                Log.e(TAG, "StorageException Code: " + se.getErrorCode());
                Log.e(TAG, "StorageException HttpResult: " + se.getHttpResultCode());
            }
            showLoading(false);
        }).addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            Log.d(TAG, "Upload is " + progress + "% done");
        });
    }

    private void updateUserProfileImageUrlInFirestore(String imageUrl) {
        Map<String, Object> update = new HashMap<>();
        update.put("profileImageUrl", imageUrl);

        userDao.updateUserSpecificFields(currentUserPojo.getUid(), update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile image updated!", Toast.LENGTH_SHORT).show();
                    currentUserPojo.setProfileImageUrl(imageUrl);
                    loggedInUser.setUser(currentUserPojo);
                    if (!isDestroyed() && !isFinishing()) {
                        Glide.with(ProfileActivity.this).load(imageUrl).circleCrop().placeholder(R.drawable.avatar_1).error(R.drawable.avatar_1).into(imgProfileImage);
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to update profile image URL in database.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to update profileImageUrl in Firestore", e);
                    showLoading(false);
                });
    }

    private void showLoading(boolean isLoading) {
        if (profileProgressBar != null) {
            profileProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (btnSaveProfile != null) btnSaveProfile.setEnabled(!isLoading);
        if (fabUpdateProfileImage != null) fabUpdateProfileImage.setEnabled(!isLoading);
    }

    private int dpToPx(int dp) {
        if (isFinishing()) return dp; // Avoid getResources if activity is finishing
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
