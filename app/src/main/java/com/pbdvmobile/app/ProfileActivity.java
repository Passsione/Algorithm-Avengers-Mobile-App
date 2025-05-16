package com.pbdvmobile.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ContentResolver; // Added
import android.graphics.Bitmap; // Added
import android.graphics.ImageDecoder; // Added for Android P and above
import android.os.Build; // Added
import android.provider.MediaStore; // Keep for camera, but also for ImageDecoder sometimes

import java.io.File;
import java.io.FileOutputStream; // Added
import java.io.IOException;
import java.io.InputStream; // Added
import java.io.OutputStream; // Added
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.UserSubject;

import java.io.File;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    DataManager dataManager;
    LogInUser current_user;
    ImageView imgProfileImage;
    Button btnSaveProfile, btnProfileLogOut;
    Switch tutor;
    EditText edtFirstName, edtLastName, edtEmail, edtBio;
    boolean suitorable = false;

    // pickImageLauncher - MODIFIED to copy the image
    private ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null && data.getData() != null) {
                                Uri sourceUri = data.getData(); // URI from the gallery/picker
                                try {
                                    // Copy the image to app's internal storage
                                    File internalImageFile = createImageFileInAppDir("profile_gallery_");
                                    copyUriToFile(sourceUri, internalImageFile);

                                    Uri internalFileUri = Uri.fromFile(internalImageFile); // URI for the copied file

                                    imgProfileImage.setImageURI(internalFileUri);
                                    Log.d(TAG, "Gallery image copied to: " + internalFileUri.toString());
                                    updateUserProfileImage(internalFileUri.toString()); // Store URI of the *copied* file

                                } catch (IOException e) {
                                    Log.e(TAG, "Error copying gallery image: ", e);
                                    Toast.makeText(ProfileActivity.this, "Failed to process image.", Toast.LENGTH_SHORT).show();
                                } catch (SecurityException e) {
                                    Log.e(TAG, "SecurityException accessing picked gallery image URI: " + sourceUri, e);
                                    Toast.makeText(this, "Permission denied to access image.", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Log.w(TAG, "pickImageLauncher: Result OK, but no data or URI.");
                                Toast.makeText(ProfileActivity.this, "Failed to get image.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d(TAG, "pickImageLauncher: Image picking cancelled or failed. ResultCode: " + result.getResultCode());
                        }
                    });
    // Ensure pickImageFromGallery is called correctly:
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        // Optionally add FLAG_GRANT_READ_URI_PERMISSION, though ACTION_GET_CONTENT typically handles this.
        // intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImageLauncher.launch(intent);
    }

// The updateUserProfileImage method should just store the URI string.
// Loading the image from this string into an ImageView elsewhere (e.g., in loadUserData
// or other activities/fragments) is where you might encounter issues if the URI
// was temporary and permissions weren't persisted.

    private void updateUserProfileImage(String imageUriString) {
        if (current_user != null && current_user.getUser() != null) {
            current_user.getUser().setProfileImageUrl(imageUriString);
            if (dataManager != null && dataManager.getUserDao() != null) {
                int rowsAffected = dataManager.getUserDao().updateUser(current_user.getUser());
                Log.d(TAG, "Profile image URI update attempt. Rows affected: " + rowsAffected + ". URI: " + imageUriString);
                if (rowsAffected > 0) {
                    // Update the LogInUser instance as well, as it might be cached
                    LogInUser.getInstance(dataManager).getUser().setProfileImageUrl(imageUriString);
                }
            } else {
                Log.e(TAG, "DataManager or UserDao is null, cannot update profile image URI in DB.");
            }
        } else {
            Log.e(TAG, "Current user or user object is null, cannot update profile image URI.");
        }
    }

    // In loadUserData, when you load the image:
    private void loadUserData() {
        if (current_user != null && current_user.getUser() != null) {
            String imageUriString = current_user.getUser().getProfileImageUrl();
            if (imageUriString != null && !imageUriString.isEmpty()) {
                Uri imageUri = Uri.parse(imageUriString);
                try {
                    // If this URI was from ACTION_GET_CONTENT and its permissions were temporary
                    // and not persisted, loading it here (after activity restart, for example)
                    // WILL FAIL with a SecurityException.
                    // URIs from FileProvider (camera) are generally fine as your app owns the file.
                    imgProfileImage.setImageURI(imageUri);
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException loading profile image URI from DB: " + imageUriString, e);
                    Toast.makeText(this, "Failed to load profile image. Please select again.", Toast.LENGTH_LONG).show();
                    imgProfileImage.setImageResource(R.drawable.avatar_1); // Fallback
                }
            } else {
                imgProfileImage.setImageResource(R.drawable.avatar_1); // Use your default icon
            }

            edtFirstName.setText(current_user.getUser().getFirstName());
            edtLastName.setText(current_user.getUser().getLastName());
//            edtEmail.setText(current_user.getUser().getEmail());
            edtBio.setText(current_user.getUser().getBio());
            tutor.setChecked(current_user.getUser().isTutor());
        } else {
            Log.e(TAG, "loadUserData: current_user or getUser() is null");
            // Handle this case, maybe navigate to login or show an error
        }
    }
    private ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Bundle extras = result.getData().getExtras();
                            if (extras != null) {
                                // For simplicity, we're not saving the full image to a file here.
                                // In a real app, you'd save the image and get its URI.
                                // This is just a placeholder to show the image.
                                // Bitmap imageBitmap = (Bitmap) extras.get("data");
                                // imgProfileImage.setImageBitmap(imageBitmap);

                                // Placeholder: Assume we have a URI (replace with actual saving logic)
                                Uri tempImageUri = Uri.parse("content://media/temp/local");
                                imgProfileImage.setImageURI(tempImageUri);
                                current_user.getUser().setProfileImageUrl(tempImageUri.toString());
                                dataManager.getUserDao().updateUser(current_user.getUser());
                            }
                        }
                    });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

       /* dataManager = DataManager.getInstance(this);
        current_user = LogInUser.getInstance(dataManager);

        if(!current_user.isLoggedIn()){
            Intent toLogin = new Intent(ProfileActivity.this, LogInActivity.class);
            startActivity(toLogin);
            finish();
            return;
        }
        dataManager.getSessionDao().updatePastSessions(Session.Status.DECLINED);

        boolean isTutor = current_user.getUser().isTutor();

        // ---- Start - User Info ----
        LinearLayout profileInfoCard = findViewById(R.id.ProfileInfoCard);
        LinearLayout subjectLayout = findViewById(R.id.profile_subjects);

        TextView email,tutorRating, eduLvl, credits, tuteeRating;
        email = findViewById(R.id.txtProfileEmail);
        tuteeRating = findViewById(R.id.txtAvgRating);
        tutorRating = findViewById(R.id.txtAvgTutorRating);
        eduLvl = findViewById(R.id.txtProfileEduLvl);

        credits = findViewById(R.id.txtCredit);

        double[] ratingUser = dataManager.getSessionDao().getAverageRatingByStudentNum(current_user.getUser().getStudentNum());

        email.setText("Email: "+ current_user.getUser().getEmail());
        eduLvl.setText("Education Level: "+current_user.getUser().getEducationLevel().name());

        credits.setText("Credits: "+ current_user.getUser().getCredits());

        tuteeRating.setText(ratingUser[0] > 0?
                "Average Rating as a tutee: " +  ratingUser[0]: "No ratings yet (as a tutee)");


        if(isTutor){
            subjectLayout.setVisibility(VISIBLE);
            tutorRating.setVisibility(VISIBLE);
            tutorRating.setText(ratingUser[1] > 0?
                    "Average Rating as a tutee: " +  ratingUser[1]: "No ratings yet (as a tutor)");
            displaySubjects(subjectLayout);
        }
        // ---- End - User Info ----

        // ---- Start - User Edits ----
        Button save, logout, changePassword, changePaymentDetails;
        EditText name, surname, password, repassword, bio;
        name = findViewById(R.id.edtProfileName);
        surname = findViewById(R.id.edtProfileSurname);
        password = findViewById(R.id.edtProfilePassword);
        repassword = findViewById(R.id.edtProfileRePassword);


        changePassword = findViewById(R.id.btnChangePassword);
        changePaymentDetails = findViewById(R.id.btnChangePaymentDetails);

        if (isTutor){
            bio = findViewById(R.id.redBio);
            bio.setVisibility(VISIBLE);
            bio.setText(current_user.getUser().getBio());
        }else
            bio = null;
        changePassword.setOnClickListener(l ->{
            if(password.getVisibility() != GONE)
                changePassword.setText("Change Password");
            else
                changePassword.setText("Close");


            password.setVisibility(password.getVisibility() != GONE ? GONE : VISIBLE);
            repassword.setVisibility(repassword.getVisibility() != GONE ? GONE : VISIBLE);
        });
        changePaymentDetails.setOnClickListener(l ->{
            Intent i = new Intent(ProfileActivity.this, PaymentGatewayActivity.class);
            startActivity(i);
        });

        tutor = findViewById(R.id.togProfileTutor);


        name.setText(current_user.getUser().getFirstName());
        surname.setText(current_user.getUser().getLastName());
        tutor.setChecked(isTutor);

        tutor.setOnClickListener(l ->{
            if(subjectLayout.getVisibility() != GONE){
                subjectLayout.setVisibility(GONE);
                tutorRating.setVisibility(GONE);
            }else{
                subjectLayout.setVisibility(VISIBLE);
                tutorRating.setVisibility(VISIBLE);
                displaySubjects(subjectLayout);
                Toast.makeText(this, "Choose subjects to tutor", Toast.LENGTH_LONG).show();
            }
        });



        // ---- Start - Controller Buttons ----
        save = findViewById(R.id.bthSaveProfile);
        logout = findViewById(R.id.btnProfileLogOut);

        // ---- Logout ----
        logout.setOnClickListener(v ->{
            current_user.logOut();
            Toast.makeText(this, "Logged Out", Toast.LENGTH_LONG).show();

            Intent toLogin = new Intent(ProfileActivity.this, LogInActivity.class);
            startActivity(toLogin);
            finish();
        });

        */
        /* ---- Save changes to the database */
        /*
        save.setOnClickListener(v -> {
            if(!name.getText().toString().isEmpty())current_user.getUser().setFirstName(name.getText().toString());
            if(!surname.getText().toString().isEmpty())current_user.getUser().setLastName(surname.getText().toString());
            if(!password.getText().toString().isEmpty())current_user.getUser().setPassword(password.getText().toString());
            if(suitorable){
                current_user.getUser().setTutor(tutor.isChecked());
            }else
                if(tutor.isChecked()){
                    dataManager.displayError("You don't qualify for tutoring");
                    current_user.getUser().setTutor(false);
                }
            if(bio != null && !bio.getText().toString().isEmpty())current_user.getUser().setBio(bio.getText().toString());
            dataManager.getUserDao().updateUser(current_user.getUser());
            Toast.makeText(this, "Changes saved", Toast.LENGTH_LONG).show();
            Intent toLanding = new Intent(ProfileActivity.this, MainActivity.class);
            startActivity(toLanding);
            finish();
        });
    }

    private void displaySubjects(LinearLayout subjectLayout) {
        subjectLayout.removeAllViews();
        TextView textView = new TextView(this);
        textView.setText("Check subjects you would like to tutor (Disabled subjects mean you don't quailfy)");
        textView.setTextSize(16);
        subjectLayout.addView(textView);
        for(UserSubject subject : dataManager.getSubjectDao().getUserSubjects(current_user.getUser().getStudentNum())){
            CheckBox subjectName = new CheckBox(this);
            Subject dUserSubject = dataManager.getSubjectDao().getSubjectById(subject.getSubjectId());
            subjectName.setText(dUserSubject.getSubjectName() + ", Grade: "+subject.getMark());
            subjectName.setChecked(subject.getTutoring());
            subjectName.setOnClickListener(l ->{
                subject.setTutoring(!subject.getTutoring());
                dataManager.getSubjectDao().updateUserSubject(subject);
            });
            boolean test = dataManager.qualifies(subject, current_user.getUser());
            if(test)suitorable = true;
//            else tutor.setChecked(false);
            subjectName.setEnabled(test);
            subjectLayout.addView(subjectName);
        }
    }

}*/


        dataManager = DataManager.getInstance(this);
        current_user = LogInUser.getInstance(dataManager);

        // Initialize Views
        imgProfileImage = findViewById(R.id.imgProfileImage);
        btnSaveProfile = findViewById(R.id.bthSaveProfile);
        btnProfileLogOut = findViewById(R.id.btnProfileLogOut);
        tutor = findViewById(R.id.togProfileTutor);
        edtFirstName = findViewById(R.id.edtProfileFirstName);
        edtLastName = findViewById(R.id.edtProfileLastName);
//        edtEmail = findViewById(R.id.edtProfileEmail);
        edtBio = findViewById(R.id.edtProfileBio);

        // Load user data
        loadUserData();

        // Set up listeners
        imgProfileImage.setOnClickListener(v -> showImagePickerDialog());
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
        btnProfileLogOut.setOnClickListener(v -> logout());

        // Display Subjects
        LinearLayout subjectLayout = findViewById(R.id.ProfileSubjectCard);
        displaySubjects(subjectLayout);
    }


    // NEW HELPER: Creates an image file in the app's internal files directory (e.g., /data/data/com.yourapp/files/Pictures)
    // Or could use getCacheDir() if temporary storage is preferred.
    // Using getFilesDir() makes it more persistent until app is uninstalled or data cleared.
    private File createImageFileInAppDir(String prefix) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = prefix + timeStamp + "_";
        File storageDir = new File(getFilesDir(), "profile_images"); // Subdirectory in internal storage

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "Failed to create internal profile_images directory");
                throw new IOException("Failed to create internal directory: " + storageDir.getAbsolutePath());
            }
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        Log.d(TAG, "Created internal image file: " + image.getAbsolutePath());
        return image;
    }


    // NEW HELPER: Copies content from a source URI to a destination File
    private void copyUriToFile(Uri sourceUri, File destinationFile) throws IOException {
        ContentResolver resolver = getContentResolver();
        try (InputStream inputStream = resolver.openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(destinationFile)) {

            if (inputStream == null) {
                throw new IOException("Unable to open input stream from URI: " + sourceUri);
            }

            byte[] buf = new byte[8192]; // Or any other reasonable buffer size
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            Log.d(TAG, "Successfully copied from " + sourceUri + " to " + destinationFile.getAbsolutePath());
        }
    }


    private void showImagePickerDialog() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Add Photo!")
                .setItems(options, (dialog, item) -> {
                    if (options[item].equals("Take Photo")) {
                        checkCameraPermissionAndLaunch();
                    } else if (options[item].equals("Choose from Gallery")) {
                        pickImageFromGallery();
                    } else if (options[item].equals("Cancel")) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            takePicture();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureLauncher.launch(takePictureIntent);
    }


    private void saveProfileChanges() {
        current_user.getUser().setFirstName(edtFirstName.getText().toString());
        current_user.getUser().setLastName(edtLastName.getText().toString());
//        current_user.getUser().setEmail(edtEmail.getText().toString());
        current_user.getUser().setBio(edtBio.getText().toString());
        current_user.getUser().setTutor(tutor.isChecked());
        dataManager.getUserDao().updateUser(current_user.getUser());

        Toast.makeText(this, "Changes saved", Toast.LENGTH_LONG).show();
        Intent toLanding = new Intent(ProfileActivity.this, MainActivity.class);
        startActivity(toLanding);
        finish();
    }

    private void logout() {
        current_user.logOut();
        Toast.makeText(this, "Successfully logged out", Toast.LENGTH_LONG).show();
        Intent toLogin = new Intent(ProfileActivity.this, LogInActivity.class);
        startActivity(toLogin);
        finish();
    }

    private void displaySubjects(LinearLayout subjectLayout) {
        subjectLayout.removeAllViews();
        TextView textView = new TextView(this);
        textView.setText("Check subjects you would like to tutor (Disabled subjects mean you don't qualify)");
        textView.setTextSize(16);
        subjectLayout.addView(textView);
        for (UserSubject subject : dataManager.getSubjectDao().getUserSubjects(current_user.getUser().getStudentNum())) {
            CheckBox subjectName = new CheckBox(this);
            Subject dUserSubject = dataManager.getSubjectDao().getSubjectById(subject.getSubjectId());
            subjectName.setText(dUserSubject.getSubjectName() + ", Grade: " + subject.getMark());
            subjectName.setChecked(subject.getTutoring());
            subjectName.setOnClickListener(l -> {
                subject.setTutoring(!subject.getTutoring());
                dataManager.getSubjectDao().updateUserSubject(subject);
            });
            boolean test = dataManager.qualifies(subject, current_user.getUser());
            if (test) suitorable = true;
            subjectName.setEnabled(test);
            subjectLayout.addView(subjectName);
        }
    }
}