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

import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.User;

public class SignUpActivity extends AppCompatActivity {

    TextView logIn;
    Button signUp;
    EditText email, fName, lName, password, rePassword;
    DataManager dataManager;
    LogInUser current_user;
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
        email = findViewById(R.id.edtSignUpEmail);
        int studentNum = 22323809; //Integer.parseInt(email.getText().toString().split("@")[0]);
        fName = findViewById(R.id.edtSignUpFName);
        lName = findViewById(R.id.edtSignUpLName);
        password = findViewById(R.id.edtSignUpPassword);
        rePassword = findViewById(R.id.edtSignUpRePassword);





        // switching pages
        signUp.setOnClickListener((v) -> { // go to sign up
            current_user.setUser(new User(studentNum, fName.getText().toString(), lName.getText().toString()));
            Intent toMain = new Intent(SignUpActivity.this, MainActivity.class);
            startActivity(toMain);
        });
        logIn.setOnClickListener((v) -> { // go to login page
            Intent toLogin = new Intent(SignUpActivity.this, LogInActivity.class);
            startActivity(toLogin);
        });

    }
}