package com.pbdvmobile.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;

public class LogInActivity extends AppCompatActivity {

    TextView signUp, flash;
    Button logIn;
    public EditText email, password;

    DataManager dataManager;

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
        // database connection
        dataManager = DataManager.getInstance(this);
        LogInUser current_user = LogInUser.getInstance(dataManager);

        Intent toLanding = new Intent(LogInActivity.this, MainActivity.class);
        if(current_user.isLoggedIn())startActivity(toLanding);

        // Finding the elements on the front end
        signUp = findViewById(R.id.txtSignUp);
        logIn = findViewById(R.id.btnLogIn);
        email = findViewById(R.id.edtLoginEmail);
        password = findViewById(R.id.edtLoginPassword);
        flash = findViewById(R.id.txtError);

        if(current_user.signedIn != null){ // just signed up
            email.setText(current_user.signedIn);
            dataManager.displayError(flash, flash, "Successful Sign Up. Please log in");
            current_user.signedIn = null;
        }

        // Switching to Main
        logIn.setOnClickListener((v) -> {

            // Check if edits are filled in
            if(dataManager.required(email, password)) {

                // is user in database?
                if (current_user.logIn(email.getText().toString(), password.getText().toString())) {
                    startActivity(toLanding);
                } else {
                    dataManager.displayError(v, flash, "Login information is incorrect");
                }
            }else{
                dataManager.displayError(v, flash, "Please fill all the field");
            }
        });

        // Switching to Sign Up
        signUp.setOnClickListener((v) -> {
            Intent toSignUp = new Intent(LogInActivity.this, SignUpActivity.class);
            startActivity(toSignUp);
        });
    }
}