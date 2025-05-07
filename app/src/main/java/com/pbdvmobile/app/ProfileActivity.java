package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Layout;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.UserSubject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    // Request codes for permissions (can be any unique integers)
    private static final int REQUEST_CODE_PERMISSIONS = 101;

    private ImageView ivProfileImage;
    private Button btnChangeProfileImage;

    private Uri currentImageUri; // To store URI from camera or gallery

    // ActivityResultLaunchers for picking image from gallery and capturing from camera
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    DataManager dataManager;
    LogInUser current_user;
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

        dataManager = DataManager.getInstance(this);
        current_user = LogInUser.getInstance(dataManager);

        if(!current_user.isLoggedIn()){
            Intent toLogin = new Intent(ProfileActivity.this, LogInActivity.class);
            startActivity(toLogin);
            finish();
            return;
        }
        boolean isTutor = current_user.getUser().isTutor();

        // ---- Start - User Info ----
        LinearLayout profileInfoCard = findViewById(R.id.ProfileInfoCard);
        LinearLayout subjectLayout = findViewById(R.id.profile_subjects);

        TextView email,tutorRating, eduLvl, tier, credits, tuteeRating;
        email = findViewById(R.id.txtProfileEmail);
        tutorRating = findViewById(R.id.txtAvgTutorRating);
        eduLvl = findViewById(R.id.txtProfileEduLvl);
//        tier = findViewById(R.id.txtProfileTier);
        credits = findViewById(R.id.txtCredit);
        tuteeRating = findViewById(R.id.txtAvgRating);

        ImageView ivProfileImage = findViewById(R.id.imgProfileImage);
        String imageUrl = current_user.getUser().getProfileImageUrl();


        // Use the class member ivProfileImage with Glide
        Glide.with(this)
                .load(imageUrl) // Make sure imageUrl is valid or provides a fallback
                .placeholder(R.drawable.avatar_1)
                .error(R.drawable.ic_menu_camera)
                .circleCrop()
                .into(ivProfileImage); // Use the class member here

        setupResultLaunchers(); // Now this is fine

        // Use the class member for the click listener too
        ivProfileImage.setOnClickListener(view -> showImagePickDialog());

        email.setText("Email: "+ current_user.getUser().getEmail());
        eduLvl.setText("Education Level: "+current_user.getUser().getEducationLevel().name());
//        tier.setText("Tier Level: " + current_user.getUser().getTierLevel().name());
        credits.setText("Credits: "+ current_user.getUser().getCredits());
//        tuteeRating.setText(current_user.getUser().get() > 0?"Rating: " + current_user.getUser().getAverageRating() : "No ratings yet");

        List<UserSubject> userSubjects = dataManager.getSubjectDao().getUserSubjects(current_user.getUser().getStudentNum());

        if(isTutor){
            subjectLayout.setVisibility(VISIBLE);
            tutorRating.setVisibility(VISIBLE);
            tutorRating.setText(current_user.getUser().getAverageRating() > 0 ?"Rating: " + current_user.getUser().getAverageRating() : "No ratings yet");

            displaySubjects(subjectLayout, userSubjects);
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

        Switch tutor = findViewById(R.id.togProfileTutor);


        name.setText(current_user.getUser().getFirstName());
        surname.setText(current_user.getUser().getLastName());
        tutor.setChecked(isTutor);

        tutor.setOnClickListener(l ->{
            if(subjectLayout.getVisibility() != GONE){
                subjectLayout.setVisibility(GONE);
            }else{
                subjectLayout.setVisibility(VISIBLE);
                displaySubjects(subjectLayout, userSubjects);
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

        /* ---- Save changes to the database */
        save.setOnClickListener(v -> {
            if(!name.getText().toString().isEmpty())current_user.getUser().setFirstName(name.getText().toString());
            if(!surname.getText().toString().isEmpty())current_user.getUser().setLastName(surname.getText().toString());
            if(!password.getText().toString().isEmpty())current_user.getUser().setPassword(password.getText().toString());
            int count = userSubjects.size();
            for(UserSubject subject : userSubjects){
                dataManager.getSubjectDao().updateUserSubject(subject);
                if(!subject.getTutoring()){
                    count--;
                }
            }
            current_user.getUser().setTutor(count > 0 ? tutor.isChecked() : false);
            if(bio != null && !bio.getText().toString().isEmpty())current_user.getUser().setBio(bio.getText().toString());
            dataManager.getUserDao().updateUser(current_user.getUser());
            Toast.makeText(this, "Changes saved", Toast.LENGTH_LONG).show();
            Intent toLanding = new Intent(ProfileActivity.this, MainActivity.class);
            startActivity(toLanding);
            finish();

        });
    }

    private void setupResultLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        currentImageUri = result.getData().getData();
                        if (currentImageUri != null) {
                            Log.d(TAG, "Gallery image URI: " + currentImageUri.toString());
                            ivProfileImage.setImageURI(currentImageUri); // Display selected image
                            // Now you can proceed to upload currentImageUri to your server/cloud storage
                            // and then save the URL to the user's profile in the database.
                            uploadImageAndSavePath(currentImageUri);
                        }
                    } else {
                        Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_SHORT).show();
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // The image URI is already in currentImageUri (if you used MediaStore or FileProvider)
                        if (currentImageUri != null) {
                            Log.d(TAG, "Camera image URI: " + currentImageUri.toString());
                            ivProfileImage.setImageURI(currentImageUri); // Display captured image
                            // Now you can proceed to upload currentImageUri to your server/cloud storage
                            uploadImageAndSavePath(currentImageUri);
                        } else {
                            // Fallback: if URI is null, try to get bitmap from intent (less reliable)
                            Bundle extras = result.getData() != null ? result.getData().getExtras() : null;
                            if (extras != null && extras.containsKey("data")) {
                                Bitmap imageBitmap = (Bitmap) extras.get("data");
                                ivProfileImage.setImageBitmap(imageBitmap);
                                // You'd need to save this bitmap to a file first to get a URI for uploading
                                Log.w(TAG, "Camera returned data as Bitmap, not URI directly.");
                            } else {
                                Toast.makeText(this, "Failed to retrieve camera image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Camera capture cancelled", Toast.LENGTH_SHORT).show();
                    }
                });

        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Boolean granted : permissions.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        Log.d(TAG, "All permissions granted.");
                        // Now you can proceed with the action that required permissions
                        // e.g., re-trigger showImagePickDialog() or a specific launcher
                    } else {
                        Toast.makeText(this, "Permissions denied. Cannot access media or camera.", Toast.LENGTH_LONG).show();
                        // Handle permission denial (e.g., show a rationale, disable features)
                    }
                });
    }


    private void showImagePickDialog() {
        // Options to display in the dialog
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image From");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) { // Camera
                if (checkAndRequestPermissions(true)) {
                    openCamera();
                }
            } else if (which == 1) { // Gallery
                if (checkAndRequestPermissions(false)) {
                    openGallery();
                }
            }
        });
        builder.show();
    }

    private boolean checkAndRequestPermissions(boolean forCamera) {
        List<String> permissionsNeeded = new ArrayList<>();

        if (forCamera) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.CAMERA);
            }
            // For saving camera image, you might need storage permissions or use MediaStore API
            // For simplicity, this example might rely on camera app returning a thumbnail or URI it manages.
            // For full control, you need write access or MediaStore.
        }

        // Storage permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else { // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            // WRITE_EXTERNAL_STORAGE might be needed for camera if saving to public dirs on older APIs,
            // but it's better to use app-specific storage or MediaStore.
        }


        if (!permissionsNeeded.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + permissionsNeeded);
            requestPermissionsLauncher.launch(permissionsNeeded.toArray(new String[0]));
            return false; // Permissions are being requested, action will proceed if granted
        }
        return true; // All necessary permissions are already granted
    }


    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*"); // Ensure only images are selectable
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Create a file to save the image (Method 1: Using FileProvider for better control)
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
//            Log.e(TAG, "Error creating image file", ex);
            Toast.makeText(this, "Error preparing camera", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            // authority must match the one in AndroidManifest.xml and file_paths.xml
            Uri photoURI = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider", // Or your specific authority string
                    photoFile);
            currentImageUri = photoURI; // Store the URI where the full-resolution image will be saved
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//            Log.d(TAG, "Launching camera with output URI: " + photoURI);
            cameraLauncher.launch(takePictureIntent);
        }

        // --- OR ---
        // (Method 2: Using MediaStore for simpler storage, image goes to gallery)
        /*
        String fileName = "profile_image_" + System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, "Profile Image captured by app");
        currentImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            Log.d(TAG, "Launching camera with MediaStore output URI: " + currentImageUri);
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
        */
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalCacheDir(); // Or getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        // currentPhotoPath = image.getAbsolutePath(); // If you need the absolute path string
        return image;
    }


    private void uploadImageAndSavePath(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "No image to upload.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Simulating image upload for: " + imageUri.toString(), Toast.LENGTH_LONG).show();
        //Log.i(TAG, "Image ready for upload: " + imageUri.toString());

        // --- ACTUAL UPLOAD LOGIC WOULD GO HERE ---
        // 1. Get an instance of your DataManager and LogInUser
        //    DataManager dataManager = DataManager.getInstance(this);
        //    LogInUser currentUser = LogInUser.getInstance(dataManager);

        // 2. If the user is logged in:
        //    if (currentUser.isLoggedIn()) {
        //        User user = currentUser.getUser();

        //        // Convert URI to a File or InputStream if needed by your upload mechanism
        //        // Example: File fileToUpload = new File(imageUri.getPath()); (May not work for all URIs)
        //        // Or use ContentResolver to open an InputStream:
        //        // try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
        //        //     Upload to Firebase Storage, your own server, etc.
        //        //     This is an asynchronous operation.
        //        // } catch (IOException e) {
        //        //     Log.e(TAG, "Error opening image URI stream", e);
        //        //     Toast.makeText(this, "Upload failed: could not read image", Toast.LENGTH_SHORT).show();
        //        //     return;
        //        // }

        //        // --- Placeholder for actual upload ---
        //        // Simulate getting a URL after upload
        //        String uploadedImageUrl = "https://example.com/path/to/user_" + user.getStudentNum() + "_profile.jpg";
        //        Log.d(TAG, "Simulated upload complete. Image URL: " + uploadedImageUrl);

        //        // 3. Update the User object and save to database
        //        user.setProfileImageUrl(uploadedImageUrl); // Assuming User model has this setter
        //
        //        // Run database update on a background thread
        //        // new Thread(() -> {
        //        //    int rowsAffected = dataManager.getUserDao().updateUser(user);
        //        //    runOnUiThread(() -> {
        //        //        if (rowsAffected > 0) {
        //        //            Toast.makeText(ProfileActivity.this, "Profile image updated!", Toast.LENGTH_SHORT).show();
        //        //            // Optionally, reload user data or just update the UI if Glide is already using the URL
        //        //            Glide.with(this).load(uploadedImageUrl).into(ivProfileImage);
        //        //        } else {
        //        //            Toast.makeText(ProfileActivity.this, "Failed to save image path.", Toast.LENGTH_SHORT).show();
        //        //        }
        //        //    });
        //        // }).start();
        //
        //    } else {
        //        Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
        //    }
        // --- END OF ACTUAL UPLOAD LOGIC ---
    }

    // You don't need to override onActivityResult if you are solely using ActivityResultLauncher
    // The logic is handled in the lambda functions passed to registerForActivityResult.

    private void displaySubjects(LinearLayout subjectLayout, List<UserSubject> userSubjects) {
        subjectLayout.removeAllViews();
        TextView textView = new TextView(this);
        textView.setText("Check subjects you would like to tutor (Disabled subjects mean you don't quailfy)");
        textView.setTextSize(16);
        subjectLayout.addView(textView);
        for(UserSubject subject : userSubjects){
            CheckBox subjectName = new CheckBox(this);
            Subject dUserSubject = dataManager.getSubjectDao().getSubjectById(subject.getSubjectId());
            subjectName.setText(dUserSubject.getSubjectName() + ", Grade: "+subject.getMark());
            subjectName.setChecked(subject.getTutoring());
            subjectName.setOnClickListener(l ->{
                subject.setTutoring(!subject.getTutoring());
            });
            subjectName.setEnabled(dataManager.qualifies(subject, current_user.getUser()));
            subjectLayout.addView(subjectName);
        }
    }

}