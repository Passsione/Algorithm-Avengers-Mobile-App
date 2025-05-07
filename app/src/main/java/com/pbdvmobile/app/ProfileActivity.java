package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.UserSubject;

import java.util.List;

public class ProfileActivity extends AppCompatActivity {

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

        ImageView profileImageView = findViewById(R.id.imgProfileImage);
        String imageUrl = current_user.getUser().getProfileImageUrl();


        Glide.with(this)
        .load(imageUrl)
        .placeholder(R.drawable.avatar_1) // Optional placeholder
        .error(R.drawable.ic_menu_camera) // Optional error image
        .circleCrop() // Optional: if you want circular images
        .into(profileImageView);

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