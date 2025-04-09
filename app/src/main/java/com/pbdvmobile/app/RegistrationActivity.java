package com.pbdvmobile.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.AuthService;

public class RegistrationActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private RadioGroup rgUserType;
    private Button btnRegister;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        authService = new AuthService();

        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        rgUserType = findViewById(R.id.rg_user_type);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = etFullName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();
                String confirmPassword = etConfirmPassword.getText().toString();

                int selectedId = rgUserType.getCheckedRadioButtonId();
                RadioButton radioButton = findViewById(selectedId);
                String userType = radioButton.getText().toString();

                if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegistrationActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegistrationActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                User newUser = new User();
                newUser.setFullName(fullName);
                newUser.setEmail(email);
                newUser.setUserType(userType.equals("Tutor") ? User.UserType.TUTOR : User.UserType.TUTEE);

                // Register user
                authService.register(newUser, password, new AuthService.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(RegistrationActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegistrationActivity.this, ProfileSetupActivity.class));
                        finish();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(RegistrationActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}