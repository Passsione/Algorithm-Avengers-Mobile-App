package com.pbdvmobile.app;

import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;

public class ProfileActivity extends AppCompatActivity {

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

        DataManager dataManager = DataManager.getInstance(this);
        LogInUser current_user = LogInUser.getInstance(dataManager);

        if(!current_user.isLoggedIn()){
            Intent toLogin = new Intent(ProfileActivity.this, LogInActivity.class);
            startActivity(toLogin);
            finish();
            return;
        }
        TextView email, tutorRating;
        email = findViewById(R.id.txtProfileEmail);
        tutorRating = findViewById(R.id.txtAvgTutorRating);
        Button save = findViewById(R.id.bthSaveProfile);
        Button logout = findViewById(R.id.btnProfileLogOut);
        EditText name, surname, password, repassword, paymentdetails, bio;

        name = findViewById(R.id.edtProfileName);
        surname = findViewById(R.id.edtProfileSurname);
        password = findViewById(R.id.edtProfilePassword);

        repassword = findViewById(R.id.edtProfileRePassword);
        paymentdetails = findViewById(R.id.redProfilePayment);
        Switch tutor = findViewById(R.id.togProfileTutor);


        email.setText(current_user.getUser().getEmail());
        name.setText(current_user.getUser().getFirstName());
        surname.setText(current_user.getUser().getLastName());
        tutor.setChecked(current_user.getUser().isTutor());

        if (tutor.isChecked()){
            bio = findViewById(R.id.redBio);
            bio.setVisibility(VISIBLE);
            bio.setText(current_user.getUser().getBio());
            tutorRating.setVisibility(VISIBLE);
        }
        logout.setOnClickListener(v ->{
            current_user.logOut();
            current_user.message = "Successfully Logged out";
            Intent toLogin = new Intent(ProfileActivity.this, LogInActivity.class);
            startActivity(toLogin);
            finish();

        });

        save.setOnClickListener(v -> {

            current_user.message = "Changes saved";
            Intent toMain = new Intent(ProfileActivity.this, MainActivity.class);
            startActivity(toMain);

        });
    }

}