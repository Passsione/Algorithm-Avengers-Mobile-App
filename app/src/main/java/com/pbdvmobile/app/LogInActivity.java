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
import com.pbdvmobile.app.data.model.User;

public class LogInActivity extends AppCompatActivity {

    TextView signUp, flash;
    Button logIn;
    EditText email, password;

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


        // Finding the elements on the front end
        signUp = findViewById(R.id.txtSignUp);
        logIn = findViewById(R.id.btnLogIn);
        email = findViewById(R.id.edtSignUpEmail);
        password = findViewById(R.id.edtPassword);
        flash = findViewById(R.id.txtError);


        // Switching to Log In
        logIn.setOnClickListener((v) -> {
            if(dataManager.required(email, password)) { // Check if edits are filled in

                if (current_user.logIn(email.getText().toString(), password.getText().toString())) {
                    // is user in database?
                    Intent toLanding = new Intent(LogInActivity.this, MainActivity.class);
                    startActivity(toLanding);
                } else {
                    Snackbar.make(v, "Log in information incorrect", Snackbar.LENGTH_LONG)
                            .setAnchorView(flash)
                            .setAction("Action", null).show();
                }
            }else{
                Snackbar.make(v, "Please fill all the field", Snackbar.LENGTH_LONG)
                        .setAnchorView(flash)
                        .setAction("Action", null).show();
            }
        });

        // Switching to Sign Up
        signUp.setOnClickListener((v) -> {
            Intent toSignUp = new Intent(LogInActivity.this, SignUpActivity.class);
            startActivity(toSignUp);
        });


    }
}