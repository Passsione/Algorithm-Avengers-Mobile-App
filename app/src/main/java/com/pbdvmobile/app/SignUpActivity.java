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

    TextView logIn;
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


        // Find in xml
        logIn = findViewById(R.id.txtLogIn);
        signUp = findViewById(R.id.btnSignUp);
        email = findViewById(R.id.edtSignupEmail);
        fName = findViewById(R.id.edtSignUpFName);
        lName = findViewById(R.id.edtSignUpLName);
        password = findViewById(R.id.edtSignUpPassword);
        rePassword = findViewById(R.id.edtSignUpRePassword);
        tutor = findViewById(R.id.chkSignUpTutor);


        // switching pages
        signUp.setOnClickListener((v) -> { // go to sign up

            if(dataManager.required(email, fName, lName, password, rePassword)){
                String sEmail = email.getText().toString();

                if(dataManager.validDut(sEmail)){

                    int studentNum = Integer.parseInt(sEmail.split("@")[0]);

                    User user = new User(studentNum, fName.getText().toString(), lName.getText().toString());
                    user.setEducationLevel(User.EduLevel.BACHELOR);
                    user.setEmail(sEmail);

                    if(password.getText().toString() == rePassword.getText().toString()) {
                        user.setPassword(password.getText().toString());
                        user.setTutor(tutor.isChecked());
                        dataManager.getUserDao().insertUser(user);

                        Intent toMain = new Intent(SignUpActivity.this, LogInActivity.class);
                        startActivity(toMain);
                    }else{
                        dataManager.displayError(v, rePassword, "Passwords don't match");
                    }
                }
            }

        });
        logIn.setOnClickListener((v) -> { // go to login page
            Intent toLogin = new Intent(SignUpActivity.this, LogInActivity.class);
            startActivity(toLogin);
        });

    }
}