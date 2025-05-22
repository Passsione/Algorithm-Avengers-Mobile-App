package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Switch;
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
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
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
    private static final String PROFILE_IMAGES_PATH = "profile_images";

    private LogInUser loggedInUser;
    private UserDao userDao;
    private SubjectDao subjectDao;
    private SessionDao sessionDao;
    private FirebaseStorage firebaseStorage;
    private FirebaseAuth firebaseAuth; // Added FirebaseAuth

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

    private Uri cameraImageUri;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        loggedInUser = LogInUser.getInstance();
        userDao = new UserDao();
        subjectDao = new SubjectDao();
        sessionDao = new SessionDao();
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseAuth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth

        currentUserPojo = loggedInUser.getUser();

        FirebaseUser fbAuthUser = firebaseAuth.getCurrentUser();
        if (fbAuthUser == null) {
            Log.e(TAG, "Firebase Auth user is NULL. User is not authenticated for Storage operations.");
        } else {
            Log.d(TAG, "Firebase Auth User UID for Storage check: " + fbAuthUser.getUid());
        }


        if (currentUserPojo == null || currentUserPojo.getUid() == null || currentUserPojo.getUid().isEmpty()) {
            Toast.makeText(this, "User data not found or invalid. Please log in again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "currentUserPojo or its UID is null/empty in onCreate. UID: " + (currentUserPojo != null ? currentUserPojo.getUid() : "null user or UID"));
            logoutUser();
            return;
        }
        Log.d(TAG, "ProfileActivity onCreate - Current User POJO UID: " + currentUserPojo.getUid());


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
        if (currentUserPojo == null) {
            Toast.makeText(this, "User data became unavailable. Please try again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "currentUserPojo is null in loadUserData.");
            logoutUser();
            showLoading(false);
            return;
        }

        if (!isFinishing() && !isDestroyed()) {
            Glide.with(this)
                    .load(currentUserPojo.getProfileImageUrl())
                    .placeholder(R.drawable.avatar_1)
                    .error(R.drawable.avatar_1)
                    .circleCrop()
                    .into(imgProfileImage);
        }


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
        if (currentUserPojo != null && currentUserPojo.getTutoredSubjectIds() != null) {
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
        if (currentUserPojo == null || currentUserPojo.getUid() == null || currentUserPojo.getUid().isEmpty()) {
            Toast.makeText(this, "Cannot save profile: User data invalid.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "saveProfileChanges: currentUserPojo or UID is null/empty.");
            return;
        }
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


    private void updateUserProfileImageUrlInFirestore(String imageUrl) {
        if (currentUserPojo == null || currentUserPojo.getUid() == null || currentUserPojo.getUid().isEmpty()) {
            Log.e(TAG, "Cannot update Firestore, currentUserPojo or UID is null/empty.");
            showLoading(false);
            return;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("profileImageUrl", imageUrl);

        userDao.updateUserSpecificFields(currentUserPojo.getUid(), update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile image updated!", Toast.LENGTH_SHORT).show();
                    currentUserPojo.setProfileImageUrl(imageUrl);
                    loggedInUser.setUser(currentUserPojo);
                    if (!isFinishing() && !isDestroyed()) {
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
// Replace these methods in your ProfileActivity.java

    private void initializeActivityLaunchers() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            Log.d(TAG, "Gallery image selected: " + imageUri);
                            uploadProfileImageToStorage(imageUri);
                        }
                    }
                });

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        Log.d(TAG, "Camera image captured: " + cameraImageUri);
                        uploadProfileImageToStorage(cameraImageUri);
                    } else {
                        Log.d(TAG, "Camera capture failed or cancelled");
                        Toast.makeText(this, "Camera capture failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void launchCamera() {
        try {
            // Create image file
            File photoFile = createImageFile();
            if (photoFile != null) {
                String authority = getPackageName() + ".provider";
                cameraImageUri = FileProvider.getUriForFile(this, authority, photoFile);

                Log.d(TAG, "Camera file created: " + photoFile.getAbsolutePath());
                Log.d(TAG, "Camera URI: " + cameraImageUri);

                takePictureLauncher.launch(cameraImageUri);
            }
        } catch (IOException ex) {
            Log.e(TAG, "Error creating camera file", ex);
            Toast.makeText(this, "Error preparing camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";

        // Use app's private external files directory
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Fallback to cache if external not available
        if (storageDir == null) {
            storageDir = new File(getCacheDir(), "images");
        }

        // Create directory if it doesn't exist
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // Create the file
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        Log.d(TAG, "Created image file: " + image.getAbsolutePath());
        return image;
    }

    private void uploadProfileImageToStorage(Uri imageUri) {
        if (imageUri == null || currentUserPojo == null) {
            Toast.makeText(this, "Cannot upload: missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Create storage reference
        String fileName = "profile_image_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = firebaseStorage.getReference()
                .child("profile_images")
                .child(currentUserPojo.getUid())
                .child(fileName);

        Log.d(TAG, "Starting upload to: " + storageRef.getPath());
        Log.d(TAG, "From URI: " + imageUri);

        // Upload file
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Upload successful!");
                    // Get download URL
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                Log.d(TAG, "Got download URL: " + downloadUri);
                                updateProfileImageInDatabase(downloadUri.toString());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to get download URL", e);
                                Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                                showLoading(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload failed", e);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showLoading(false);
                })
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "Upload progress: " + (int) progress + "%");
                });
    }

    private void updateProfileImageInDatabase(String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", imageUrl);

        userDao.updateUserSpecificFields(currentUserPojo.getUid(), updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile image updated!", Toast.LENGTH_SHORT).show();

                    // Update local user object
                    currentUserPojo.setProfileImageUrl(imageUrl);
                    loggedInUser.setUser(currentUserPojo);

                    // Update UI
                    if (!isFinishing() && !isDestroyed()) {
                        Glide.with(this)
                                .load(imageUrl)
                                .circleCrop()
                                .placeholder(R.drawable.avatar_1)
                                .error(R.drawable.avatar_1)
                                .into(imgProfileImage);
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update database", e);
                    Toast.makeText(this, "Failed to update database", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    // Add this method to verify file existence
    private boolean verifyFileExists(Uri uri) {
        if (uri == null) return false;

        try {
            if ("file".equals(uri.getScheme())) {
                File file = new File(uri.getPath());
                boolean exists = file.exists();
                Log.d(TAG, "File exists check for " + uri.getPath() + ": " + exists +
                        " (Size: " + (exists ? file.length() : "N/A") + " bytes)");
                return exists;
            } else if ("content".equals(uri.getScheme())) {
                // For content URIs, try to open input stream
                try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri)) {
                    boolean exists = inputStream != null;
                    Log.d(TAG, "Content URI exists check: " + exists);
                    return exists;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying file existence for URI: " + uri, e);
        }
        return false;
    }


    private File createImageFileForCamera() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "CAMERA_" + timeStamp + "_";

        // Try external files directory first
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir == null) {
            Log.w(TAG, "ExternalFilesDir is null, using cache directory.");
            storageDir = getCacheDir();
        }

        if (storageDir == null) {
            throw new IOException("Cannot get any directory for images.");
        }

        // Ensure directory exists
        if (!storageDir.exists()) {
            boolean dirCreated = storageDir.mkdirs();
            Log.d(TAG, "Directory creation result for " + storageDir.getAbsolutePath() + ": " + dirCreated);
            if (!dirCreated) {
                throw new IOException("Failed to create directory: " + storageDir.getAbsolutePath());
            }
        }

        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        Log.d(TAG, "Camera image file created: " + imageFile.getAbsolutePath());
        Log.d(TAG, "File exists after creation: " + imageFile.exists());

        return imageFile;
    }

    private int dpToPx(int dp) {
        if (isFinishing() || isDestroyed()) return dp;
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
