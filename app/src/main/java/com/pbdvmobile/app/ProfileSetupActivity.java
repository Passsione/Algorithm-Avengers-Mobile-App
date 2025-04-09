package com.pbdvmobile.app;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.UserService;
import com.pbdvmobile.app.utils.SubjectTokenizer;
import java.util.ArrayList;
import java.util.List;

public class ProfileSetupActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView ivProfileImage;
    private EditText etBio, etStudentId, etHourlyRate;
    private MultiAutoCompleteTextView tvSubjects;
    private Spinner spinnerEducationLevel;
    private Button btnSave;

    private Uri selectedImageUri;
    private UserService userService;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        userService = new UserService();
        currentUser = userService.getCurrentUser();

        ivProfileImage = findViewById(R.id.iv_profile_image);
        etBio = findViewById(R.id.et_bio);
        etStudentId = findViewById(R.id.et_student_id);
        etHourlyRate = findViewById(R.id.et_hourly_rate);
        tvSubjects = findViewById(R.id.tv_subjects);
        spinnerEducationLevel = findViewById(R.id.spinner_education_level);
        btnSave = findViewById(R.id.btn_save);

        // Setup educational level spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.education_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEducationLevel.setAdapter(adapter);

        // Setup subjects autocomplete
        List<Subject> subjectList = userService.getAvailableSubjects();
        List<String> subjectNames = new ArrayList<>();
        for (Subject subject : subjectList) {
            subjectNames.add(subject.getName());
        }

        ArrayAdapter<String> subjectsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, subjectNames);
        tvSubjects.setAdapter(subjectsAdapter);
        tvSubjects.setTokenizer(new SubjectTokenizer(", "));

        // If user is a tutee, hide hourly rate
        if (currentUser.getUserType() == User.UserType.TUTEE) {
            etHourlyRate.setVisibility(View.GONE);
            findViewById(R.id.tv_hourly_rate_label).setVisibility(View.GONE);
        }

        ivProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bio = etBio.getText().toString().trim();
                String studentId = etStudentId.getText().toString().trim();
                String educationLevel = spinnerEducationLevel.getSelectedItem().toString();
                String subjectsText = tvSubjects.getText().toString().trim();

                if (bio.isEmpty() || studentId.isEmpty() || subjectsText.isEmpty()) {
                    Toast.makeText(ProfileSetupActivity.this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                currentUser.setBio(bio);
                currentUser.setStudentId(studentId);
                currentUser.setEducationLevel(educationLevel);

                // Parse subjects
                String[] subjectNames = subjectsText.split(", ");
                List<Subject> selectedSubjects = new ArrayList<>();
                for (String name : subjectNames) {
                    Subject subject = userService.findSubjectByName(name);
                    if (subject != null) {
                        selectedSubjects.add(subject);
                    }
                }
                currentUser.setSubjects(selectedSubjects);

                // Set hourly rate if tutor
                if (currentUser.getUserType() == User.UserType.TUTOR) {
                    String hourlyRateStr = etHourlyRate.getText().toString().trim();
                    if (!hourlyRateStr.isEmpty()) {
                        double hourlyRate = Double.parseDouble(hourlyRateStr);
                        currentUser.setHourlyRate(hourlyRate);
                    } else {
                        Toast.makeText(ProfileSetupActivity.this, "Please enter hourly rate", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Save profile
                userService.updateProfile(currentUser, selectedImageUri, new UserService.UserCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ProfileSetupActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ProfileSetupActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(ProfileSetupActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            ivProfileImage.setImageURI(selectedImageUri);
        }
    }
}
