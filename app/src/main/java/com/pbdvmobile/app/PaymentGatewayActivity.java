package com.pbdvmobile.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;

import java.util.Calendar;

public class PaymentGatewayActivity extends AppCompatActivity {

    // View declarations
    private EditText lastNameEditText, initialsEditText, cardNumberEditText;
    private EditText cvEditText, cardExpiryEditText, amountEditText;
    private Button payButton;
    private ProgressBar progressBar;
    private TextInputLayout lastNameLayout, initialsLayout, cardNumberLayout;
    private TextInputLayout cvLayout, expiryLayout, amountLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment_gateway);

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize data manager and current user
        DataManager dataManager = DataManager.getInstance(this);
        LogInUser currentUser = LogInUser.getInstance(dataManager);

        // Initialize views and setup functionality
        initializeViews();
        setupFormValidation();
        setupAutoFormatting();
        setupPaymentButton();
    }

    private void initializeViews() {
        lastNameLayout = findViewById(R.id.lastNameLayout);
        initialsLayout = findViewById(R.id.initialsLayout);
        cardNumberLayout = findViewById(R.id.cardNumberLayout);
        cvLayout = findViewById(R.id.cvLayout);
        expiryLayout = findViewById(R.id.expiryLayout);
        amountLayout = findViewById(R.id.amountLayout);

        lastNameEditText = findViewById(R.id.etLastName);
        initialsEditText = findViewById(R.id.etInitials);
        cardNumberEditText = findViewById(R.id.etCardNumber);
        cvEditText = findViewById(R.id.etCV);
        cardExpiryEditText = findViewById(R.id.etCardExpiry);
        amountEditText = findViewById(R.id.etAmount);

        payButton = findViewById(R.id.btnPay);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupFormValidation() {
        lastNameEditText.addTextChangedListener(new ClearErrorTextWatcher(lastNameLayout));
        initialsEditText.addTextChangedListener(new ClearErrorTextWatcher(initialsLayout));
        cardNumberEditText.addTextChangedListener(new ClearErrorTextWatcher(cardNumberLayout));
        cvEditText.addTextChangedListener(new ClearErrorTextWatcher(cvLayout));
        cardExpiryEditText.addTextChangedListener(new ClearErrorTextWatcher(expiryLayout));
        amountEditText.addTextChangedListener(new ClearErrorTextWatcher(amountLayout));
    }

    private void setupAutoFormatting() {
        cardNumberEditText.addTextChangedListener(new TextWatcher() {
            private static final char DIVIDER = ' ';

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String original = s.toString().replaceAll(String.valueOf(DIVIDER), "");
                if (original.isEmpty() || original.length() > 16) return;

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < original.length(); i++) {
                    formatted.append(original.charAt(i));
                    if ((i + 1) % 4 == 0 && i != original.length() - 1) {
                        formatted.append(DIVIDER);
                    }
                }

                if (!s.toString().equals(formatted.toString())) {
                    cardNumberEditText.removeTextChangedListener(this);
                    cardNumberEditText.setText(formatted.toString());
                    cardNumberEditText.setSelection(formatted.length());
                    cardNumberEditText.addTextChangedListener(this);
                }
            }
        });

        cardExpiryEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 2 && before == 0 && !s.toString().contains("/")) {
                    cardExpiryEditText.setText(s + "/");
                    cardExpiryEditText.setSelection(3);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupPaymentButton() {
        payButton.setOnClickListener(v -> {
            if (validateForm()) {
                processPayment();
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (lastNameEditText.getText().toString().trim().isEmpty()) {
            lastNameLayout.setError("Please enter your last name");
            isValid = false;
        }

        if (initialsEditText.getText().toString().trim().isEmpty()) {
            initialsLayout.setError("Please enter your initials");
            isValid = false;
        }

        String cardNumber = cardNumberEditText.getText().toString().replace(" ", "");
        if (cardNumber.isEmpty() || cardNumber.length() != 16 || !cardNumber.matches("\\d+")) {
            cardNumberLayout.setError("Please enter a valid 16-digit card number");
            isValid = false;
        }

        String cv = cvEditText.getText().toString().trim();
        if (cv.isEmpty() || cv.length() != 3 || !cv.matches("\\d+")) {
            cvLayout.setError("Please enter a valid CVV (3 digits)");
            isValid = false;
        }

        String cardExpiry = cardExpiryEditText.getText().toString().trim();
        if (!isValidExpiryDate(cardExpiry)) {
            expiryLayout.setError("Please enter a valid expiry date (MM/YY)");
            isValid = false;
        }

        String amountStr = amountEditText.getText().toString().trim();
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                amountLayout.setError("Amount must be greater than 0");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            amountLayout.setError("Please enter a valid amount");
            isValid = false;
        }

        return isValid;
    }

    private boolean isValidExpiryDate(String expiryDate) {
        if (expiryDate.length() != 5 || expiryDate.charAt(2) != '/') {
            return false;
        }

        try {
            int month = Integer.parseInt(expiryDate.substring(0, 2));
            int year = Integer.parseInt(expiryDate.substring(3));

            Calendar calendar = Calendar.getInstance();
            int currentYear = calendar.get(Calendar.YEAR) % 100;
            int currentMonth = calendar.get(Calendar.MONTH) + 1;

            if (year < currentYear || (year == currentYear && month < currentMonth)) {
                return false;
            }

            return month >= 1 && month <= 12;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void processPayment() {
        payButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        new android.os.Handler().postDelayed(() -> runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            payButton.setEnabled(true);
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_LONG).show();
            clearForm();
        }), 2000);
    }

    private void clearForm() {
        lastNameEditText.setText("");
        initialsEditText.setText("");
        cardNumberEditText.setText("");
        cvEditText.setText("");
        cardExpiryEditText.setText("");
        amountEditText.setText("");

        lastNameLayout.setError(null);
        initialsLayout.setError(null);
        cardNumberLayout.setError(null);
        cvLayout.setError(null);
        expiryLayout.setError(null);
        amountLayout.setError(null);

        lastNameEditText.requestFocus();
    }

    private static class ClearErrorTextWatcher implements TextWatcher {
        private final TextInputLayout textInputLayout;

        ClearErrorTextWatcher(TextInputLayout textInputLayout) {
            this.textInputLayout = textInputLayout;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            textInputLayout.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}
