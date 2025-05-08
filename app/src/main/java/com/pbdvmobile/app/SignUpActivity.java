package com.pbdvmobile.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.User;

public class SignUpActivity extends AppCompatActivity {

    TextView logIn, flash;
    Button signUp;
    EditText email, fName, lName, password, rePassword;
    DataManager dataManager;
    LogInUser current_user;
    CheckBox tutor;
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

        dataManager = DataManager.getInstance(this);
        current_user = LogInUser.getInstance(dataManager);
        Intent toMain = new Intent(SignUpActivity.this, MainActivity.class);

        if(current_user.isLoggedIn()){
            startActivity(toMain);
            finish();
            return;
        }

        // Find in xml
        logIn = findViewById(R.id.txtLogIn);
        signUp = findViewById(R.id.btnSignUp);
        email = findViewById(R.id.edtSignupEmail);
        fName = findViewById(R.id.edtSignUpFName);
        lName = findViewById(R.id.edtSignUpLName);
        password = findViewById(R.id.edtSignUpPassword);
        rePassword = findViewById(R.id.edtSignUpRePassword);
        tutor = findViewById(R.id.chkSignUpTutor);
        flash = findViewById(R.id.txtError);


        // switching pages
        // Sign Up new user
        signUp.setOnClickListener((v) -> {

            // all fields are filled in?
            if(dataManager.required(email, fName, lName, password, rePassword)){

                String sEmail = email.getText().toString();

                // is Valid DUT email?
                if(dataManager.validDut(sEmail)){

                    // does email exist is database?
                    if(dataManager.getUserDao().getUserByEmail(sEmail) == null){
                        // passwords match?
                        if(password.getText().toString().equals(rePassword.getText().toString())) {

                            // gets student number from the email provided
                            int studentNum = dataManager.retrieveStudentNum(sEmail);

                            User user = new User(studentNum, fName.getText().toString(), lName.getText().toString());
                            user.setEmail(sEmail);

                            // random education level
                            user.setEducationLevel(User.EduLevel.values()[dataManager.randomIndex(8)]);

                            user.setPassword(password.getText().toString());
                            user.setTutor(tutor.isChecked());

                            dataManager.getUserDao().insertUser(user);

                            // randomly assign user subjects
                            dataManager.fillUserSubject(studentNum);


                            // go to Login page
                            current_user.message = sEmail;
                            Intent toLogin = new Intent(SignUpActivity.this, LogInActivity.class);
                            startActivity(toLogin);
                            finish();

                        }else{
                            rePassword.setError("Doesn't match password");
                            dataManager.displayError("Passwords don't match");
                        }
                    }else{

                        dataManager.displayError("Email already exists. Try logging in");
                    }
                }else{
                    email.setError("Not a valid DUT email");
                    dataManager.displayError("Not a valid DUT email");

                }
            }else{
                dataManager.displayError("Please fill in all fields");

            }

        });

        // go to login page
        logIn.setOnClickListener((v) -> {
            Intent toLogin = new Intent(SignUpActivity.this, LogInActivity.class);
            startActivity(toLogin);
        });

    }
}