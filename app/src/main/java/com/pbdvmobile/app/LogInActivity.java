package com.pbdvmobile.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser; // Your updated LogInUser
import com.pbdvmobile.app.data.model.User; // Your User POJO


public class LogInActivity extends AppCompatActivity {

    private static final String TAG = "LogInActivity";

    private TextView signUpLink, flashTextView;
    private Button logInButton;
    private EditText edtEmail, edtPassword;

    private FirebaseAuth mAuth;
    private DataManager dataManger;
    private LogInUser localLogInUserInstance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        localLogInUserInstance = LogInUser.getInstance();
        dataManger = DataManager.getInstance(this);

        signUpLink = findViewById(R.id.txtSignUp);
        logInButton = findViewById(R.id.btnLogIn);
        edtEmail = findViewById(R.id.edtLoginEmail);
        edtPassword = findViewById(R.id.edtLoginPassword);
        flashTextView = findViewById(R.id.txtError);

        // Check if user is already logged in
//        if (mAuth.getCurrentUser() != null) {
//            // If already logged in, fetch user data and go to Main
//            fetchUserDataAndProceed(mAuth.getCurrentUser());
//            return; // Prevent further execution of onCreate if already logged in
//        }


        // Handle message from SignUpActivity (prefill email)
        if (localLogInUserInstance.message != null) {
            edtEmail.setText(localLogInUserInstance.message);
            flashTextView.setText("Sign up successful. Please log in.");
            flashTextView.setVisibility(View.VISIBLE);
            localLogInUserInstance.message = null; // Clear message
        }

        logInButton.setOnClickListener(v -> attemptLogIn());
        signUpLink.setOnClickListener(v -> startActivity(new Intent(LogInActivity.this, SignUpActivity.class)));
    }

    private void displayFlashMessage(String message, boolean isError) {
        flashTextView.setText(message);
        flashTextView.setVisibility(View.VISIBLE);
        // You can add color changes for errors here if desired
    }

    private void attemptLogIn() {
        flashTextView.setVisibility(View.GONE); // Clear previous messages
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (!isValidInput(email, password)) {
            return;
        }

        logInButton.setEnabled(false); // Prevent multiple clicks
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    logInButton.setEnabled(true); // Re-enable button
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth: signInWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Fetch user data from Firestore and then proceed
                            fetchUserDataAndProceed(firebaseUser);
                        } else {
                            displayFlashMessage("Login successful, but failed to get user session.", true);
                        }
                    } else {
                        Log.w(TAG, "Firebase Auth: signInWithEmail:failure", task.getException());
                        displayFlashMessage("Login failed: " + task.getException().getMessage(), true);
                    }
                });
    }

    private boolean isValidInput(String email, String password) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Enter a valid email.");
            edtEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Password required.");
            edtPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void fetchUserDataAndProceed(FirebaseUser firebaseUser) {
        FirebaseFirestore.getInstance().collection("users").document(firebaseUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            User userPojo = document.toObject(User.class);
                            if (userPojo != null) {
                                userPojo.setUid(firebaseUser.getUid()); // Ensure UID is set if not mapped
                                LogInUser.getInstance().setUser(userPojo); // Cache the fetched User POJO
                                Log.d(TAG, "User data fetched from Firestore: " + userPojo.getFirstName());
                            } else {
                                Log.e(TAG, "Failed to parse user data from Firestore for UID: " + firebaseUser.getUid());
                            }
                        } else {
                            Log.e(TAG, "No user document found in Firestore for UID: " + firebaseUser.getUid());
                            // This is a critical state: Auth user exists but no Firestore profile.
                            // You might want to create a default profile here or log them out.
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch user document from Firestore: ", task.getException());
                    }
                    // Proceed to MainActivity regardless of Firestore fetch success for now,
                    // MainActivity will handle cases where userPojo might be null.
                    // A more robust solution might wait or show an error before proceeding if Firestore data is essential.
                    Toast.makeText(LogInActivity.this, "Log In Successful.", Toast.LENGTH_SHORT).show();
                    Intent toLanding = new Intent(LogInActivity.this, MainActivity.class);
                    toLanding.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(toLanding);
                    finishAffinity(); // Finishes this and all parent activities
                });
    }
}