package com.pbdvmobile.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pbdvmobile.app.data.DataManager; // Keep for DUT email validation & student num retrieval for now
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.User; // Your User POJO

import java.util.ArrayList; // For tutoredSubjectIds

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    private TextView logInLink;
    private Button signUpButton;
    private EditText edtEmail, edtFName, edtLName, edtPassword, edtRePassword;
    private CheckBox chkTutor;
    private TextView flashTextView; // For displaying errors/messages

    private FirebaseAuth mAuth;
    private FirebaseFirestore dbFirestore;
    private DataManager dataManager; // Still used for DUT email validation and student number

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        dbFirestore = FirebaseFirestore.getInstance();
        dataManager = DataManager.getInstance(this); // For DUT validation

        /*if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();
            return;
        }*/

        logInLink = findViewById(R.id.txtLogIn);
        signUpButton = findViewById(R.id.btnSignUp);
        edtEmail = findViewById(R.id.edtSignupEmail);
        edtFName = findViewById(R.id.edtSignUpFName);
        edtLName = findViewById(R.id.edtSignUpLName);
        edtPassword = findViewById(R.id.edtSignUpPassword);
        edtRePassword = findViewById(R.id.edtSignUpRePassword);
        chkTutor = findViewById(R.id.chkSignUpTutor);
        flashTextView = findViewById(R.id.txtError);

        signUpButton.setOnClickListener(v -> attemptSignUp());
        logInLink.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LogInActivity.class));
            // finish(); // Optional: finish if you don't want user to come back here with back button
        });
    }

    private void displayFlashMessage(String message, boolean isError) {
        // You might want to color errors differently
        flashTextView.setText(message);
        flashTextView.setVisibility(View.VISIBLE);
    }

    private void attemptSignUp() {
        flashTextView.setVisibility(View.GONE); // Clear previous messages
        String email = edtEmail.getText().toString().trim();
        String firstName = edtFName.getText().toString().trim();
        String lastName = edtLName.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String rePassword = edtRePassword.getText().toString().trim();
        boolean isTutor = chkTutor.isChecked();

        if (!dataManager.required(edtEmail, edtFName, edtLName, edtPassword, edtRePassword)) {
            displayFlashMessage("Please fill in all fields.", true);
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Enter a valid email address.");
            edtEmail.requestFocus();
            return;
        }
        if (!dataManager.validDut(email)) { //
            edtEmail.setError("Not a valid DUT email.");
            displayFlashMessage("Please use your DUT4LIFE email.", true);
            return;
        }
        if (password.length() < 6) {
            edtPassword.setError("Password must be at least 6 characters.");
            edtPassword.requestFocus();
            return;
        }
        if (!password.equals(rePassword)) {
            edtRePassword.setError("Passwords do not match.");
            edtRePassword.requestFocus();
            return;
        }

        // Check if email (as username) already exists in Firestore users collection (optional, Auth handles this too)
        // This is a good practice if you want to provide a specific error before hitting Auth.
        // For simplicity, we'll let Firebase Auth handle the "email already in use" error.

        signUpButton.setEnabled(false); // Prevent multiple clicks
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    signUpButton.setEnabled(true); // Re-enable button
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth: createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Update Firebase Auth profile display name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(firstName + " " + lastName)
                                    .build();
                            firebaseUser.updateProfile(profileUpdates);

                            createNewUserInFirestore(firebaseUser, firstName, lastName, email, isTutor);

                        } else {
                            displayFlashMessage("Sign up successful, but failed to get user session.", true);
                        }
                    } else {
                        Log.w(TAG, "Firebase Auth: createUserWithEmail:failure", task.getException());
                        displayFlashMessage("Sign up failed: " + task.getException().getMessage(), true);
                    }
                });
    }

    private void createNewUserInFirestore(FirebaseUser firebaseUser, String firstName, String lastName, String email, boolean isTutor) {
        String uid = firebaseUser.getUid();
        int studentNum = dataManager.retrieveStudentNum(email); //

        User newUserAppModel = new User();
        newUserAppModel.setUid(uid);
        newUserAppModel.setStudentNum(studentNum);
        newUserAppModel.setFirstName(firstName);
        newUserAppModel.setLastName(lastName);
        newUserAppModel.setEmail(email); // Storing email in Firestore doc as well
        newUserAppModel.setTutor(isTutor);
        // Set default education level or get from UI if you add a spinner for it
        newUserAppModel.setEducationLevel(User.EduLevel.values()[dataManager.randomIndex(7)]); // Example default
        newUserAppModel.setTierLevel(User.TierLevel.BASIC);
        newUserAppModel.setCredits(10.0); // Default starting credits
        newUserAppModel.setProfileImageUrl(null); // Set later
        newUserAppModel.setBio("");
        newUserAppModel.setTutoredSubjectIds(new ArrayList<>()); // Initialize with empty list



        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(newUserAppModel) // Using the POJO
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created in Firestore for UID: " + uid);

                    // If you still use DataManager to assign initial subjects locally (this will be removed)

                    dataManager.fillUserSubject(studentNum, chkTutor.isChecked());

                    Toast.makeText(SignUpActivity.this, "Sign up successful! Please log in.", Toast.LENGTH_LONG).show();
                    LogInUser.getInstance().message = email; // For pre-filling login form

                    Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finishAffinity(); // Finishes this and all parent activities in the task
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user profile in Firestore", e);
                    displayFlashMessage("Error saving user details: " + e.getMessage(), true);
                    // Critical error: Auth user created but Firestore doc failed.
                    // Consider deleting the Auth user to allow re-registration or implement retry.
                    // firebaseUser.delete().addOnCompleteListener(...);
                });
    }
}
