package com.pbdvmobile.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout; // For Google Pay Button Layout
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.material.textfield.TextInputLayout;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.util.PaymentsUtil; // You will create this utility class

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Optional;

public class PaymentGatewayActivity extends AppCompatActivity {

    private EditText lastNameEditText, initialsEditText, cardNumberEditText;
    private EditText cvEditText, cardExpiryEditText, amountEditText;
    private Button payButton; // For manual card
    private ProgressBar progressBar;
    private TextInputLayout lastNameLayout, initialsLayout, cardNumberLayout;
    private TextInputLayout cvLayout, expiryLayout, amountLayout;

    // Google Pay
    private PaymentsClient paymentsClient;
    private RelativeLayout googlePayButtonLayout; // Using RelativeLayout as a clickable container
    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;
    private static final String TAG = "PaymentGatewayActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment_gateway);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        DataManager dataManager = DataManager.getInstance(this);
        LogInUser currentUser = LogInUser.getInstance();

        initializeViews();
        setupFormValidation();
        setupAutoFormatting();
        setupPaymentButton(); // For manual card

        // Initialize Google Pay client
        paymentsClient = PaymentsUtil.createPaymentsClient(this);
        possiblyShowGooglePayButton();

        googlePayButtonLayout.setOnClickListener(view -> requestPayment());
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

        googlePayButtonLayout = findViewById(R.id.googlePayButtonLayout);
    }


    // --- Google Pay Integration ---
    private void possiblyShowGooglePayButton() {
        final Optional<JSONObject> isReadyToPayJson = PaymentsUtil.getIsReadyToPayRequest();
        if (!isReadyToPayJson.isPresent()) {
            googlePayButtonLayout.setVisibility(View.GONE); // Hide if config is wrong
            return;
        }

        IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.get().toString());
        paymentsClient.isReadyToPay(request)
                .addOnCompleteListener(this, task -> {
                    try {
                        if (task.isSuccessful()) {
                            googlePayButtonLayout.setVisibility(View.VISIBLE);
                        } else {
                            Log.w(TAG, "isReadyToPay failed: ", task.getException());
                            googlePayButtonLayout.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "isReadyToPay task failed with exception.", e);
                        googlePayButtonLayout.setVisibility(View.GONE);
                    }
                });
    }

    private void requestPayment() {
        // Validate amount before proceeding with Google Pay
        String amountStr = amountEditText.getText().toString().trim();
        double amountCents;
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                amountLayout.setError("Amount must be greater than 0 for Google Pay");
                amountLayout.requestFocus();
                return;
            }
            amountLayout.setError(null);
            amountCents = amount * 100; // Google Pay expects amount in micros/cents
        } catch (NumberFormatException e) {
            amountLayout.setError("Please enter a valid amount for Google Pay");
            amountLayout.requestFocus();
            return;
        }


        Optional<JSONObject> paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(String.valueOf((long)amountCents));
        if (!paymentDataRequestJson.isPresent()) {
            Log.e(TAG, "Cannot fetch payment data request JSON.");
            Toast.makeText(this, "Google Pay Error: Could not initialize payment.", Toast.LENGTH_SHORT).show();
            return;
        }

        PaymentDataRequest request = PaymentDataRequest.fromJson(paymentDataRequestJson.get().toString());
        AutoResolveHelper.resolveTask(
                paymentsClient.loadPaymentData(request),
                this,
                LOAD_PAYMENT_DATA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    if (data != null) {
                        PaymentData paymentData = PaymentData.getFromIntent(data);
                        handleGooglePaySuccess(paymentData);
                    }
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(this, "Google Pay cancelled.", Toast.LENGTH_SHORT).show();
                    break;
                case AutoResolveHelper.RESULT_ERROR:
                    // Autoresolve error could happen if the user has an out of date GServices etc.
                    // Refer to AutoresolveHelper documentation for more details.
//                    ApiException apiException = AutoResolveHelper.getStatusFromIntent(data).asApiException();
//                    Log.e(TAG, "Google Pay loadPaymentData failed: " + apiException.getStatusCode() + " " + apiException.getMessage());
//                    Toast.makeText(this, "Google Pay error: " + apiException.getMessage(), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private void handleGooglePaySuccess(PaymentData paymentData) {
        String paymentInformation = paymentData.toJson();
        Log.d(TAG, "Google Pay Success: " + paymentInformation);

        try {
            JSONObject paymentMethodData = new JSONObject(paymentInformation).getJSONObject("paymentMethodData");
            String token = paymentMethodData.getJSONObject("tokenizationData").getString("token");

            // TODO: Send this token to your backend server to process the payment
            // with your payment processor (e.g., Stripe, Braintree, Adyen etc.)
            // Do NOT process the payment on the client-side with the raw token.

            Toast.makeText(this, "Google Pay successful! Token: " + token.substring(0,20) + "...", Toast.LENGTH_LONG).show();
            // For demo, simulate processing and clear form
            processManualPayment(); // Simulate success for now
        } catch (JSONException e) {
            Log.e(TAG, "Could not parse Google Pay JSON response.", e);
            Toast.makeText(this, "Error processing Google Pay response.", Toast.LENGTH_SHORT).show();
        }
    }


    // --- Manual Card Payment Logic (existing) ---
    private void setupFormValidation() {
        // ... (existing code)
    }
    private void setupAutoFormatting() {
        // ... (existing code)
    }
    private void setupPaymentButton() {
        payButton.setOnClickListener(v -> {
            if (validateManualCardForm()) { // Renamed for clarity
                processManualPayment();
            }
        });
    }
    private boolean validateManualCardForm() { // Renamed
        // ... (existing validation logic for manual card fields)
        // Amount validation needs to be here too if not handled before calling this
        boolean isValid = true;
        // ... (rest of your manual card validation)
        if (lastNameEditText.getText().toString().trim().isEmpty()) {
            lastNameLayout.setError("Please enter your last name");
            isValid = false;
        } else {
            lastNameLayout.setError(null);
        }

        if (initialsEditText.getText().toString().trim().isEmpty()) {
            initialsLayout.setError("Please enter your initials");
            isValid = false;
        } else {
            initialsLayout.setError(null);
        }

        String cardNumber = cardNumberEditText.getText().toString().replace(" ", "");
        if (cardNumber.isEmpty() || cardNumber.length() != 16 || !cardNumber.matches("\\d+")) {
            cardNumberLayout.setError("Please enter a valid 16-digit card number");
            isValid = false;
        } else {
            cardNumberLayout.setError(null);
        }

        String cv = cvEditText.getText().toString().trim();
        if (cv.isEmpty() || cv.length() != 3 || !cv.matches("\\d+")) {
            cvLayout.setError("Please enter a valid CVV (3 digits)");
            isValid = false;
        } else {
            cvLayout.setError(null);
        }

        String cardExpiry = cardExpiryEditText.getText().toString().trim();
        if (!isValidExpiryDate(cardExpiry)) {
            expiryLayout.setError("Please enter a valid expiry date (MM/YY)");
            isValid = false;
        } else {
            expiryLayout.setError(null);
        }

        String amountStr = amountEditText.getText().toString().trim();
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                amountLayout.setError("Amount must be greater than 0");
                isValid = false;
            } else {
                amountLayout.setError(null);
            }
        } catch (NumberFormatException e) {
            amountLayout.setError("Please enter a valid amount");
            isValid = false;
        }
        return isValid;
    }

    private boolean isValidExpiryDate(String expiryDate) {
        // ... (existing code)
        if (expiryDate.length() != 5 || expiryDate.charAt(2) != '/') {
            return false;
        }

        try {
            int month = Integer.parseInt(expiryDate.substring(0, 2));
            int year = Integer.parseInt(expiryDate.substring(3));

            Calendar calendar = Calendar.getInstance();
            int currentYear = calendar.get(Calendar.YEAR) % 100; // Last two digits of current year
            int currentMonth = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-indexed

            if (month < 1 || month > 12) return false;
            if (year < currentYear || (year == currentYear && month < currentMonth)) {
                return false; // Card has expired
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void processManualPayment() { // Renamed
        payButton.setEnabled(false);
        googlePayButtonLayout.setEnabled(false); // Disable Google Pay button during processing
        progressBar.setVisibility(View.VISIBLE);

        new android.os.Handler().postDelayed(() -> runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            payButton.setEnabled(true);
            googlePayButtonLayout.setEnabled(true); // Re-enable
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_LONG).show();

            // TODO: Update user credits in DB
            // LogInUser currentUser = LogInUser.getInstance(this);
            // double amountPaid = Double.parseDouble(amountEditText.getText().toString());
            // User user = currentUser.getUser();
            // user.setCredits(user.getCredits() + (amountPaid * 10)); // Example: R1 = 10 credits
            // DataManager.getInstance(this).getUserDao().updateUser(user);

            clearForm();
        }), 2000);
    }

    private void clearForm() {
        // ... (existing code)
        lastNameEditText.setText("");
        initialsEditText.setText("");
        cardNumberEditText.setText("");
        cvEditText.setText("");
        cardExpiryEditText.setText("");
        amountEditText.setText(""); // Keep amount if they might want to retry with same amount? Or clear.

        lastNameLayout.setError(null);
        initialsLayout.setError(null);
        cardNumberLayout.setError(null);
        cvLayout.setError(null);
        expiryLayout.setError(null);
        amountLayout.setError(null);

        amountEditText.requestFocus(); // Or lastNameEditText
    }

    // ... ClearErrorTextWatcher class (existing) ...
    private static class ClearErrorTextWatcher implements TextWatcher {
        private final TextInputLayout textInputLayout;

        ClearErrorTextWatcher(TextInputLayout textInputLayout) {
            this.textInputLayout = textInputLayout;
        }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (textInputLayout.getError() != null) {
                textInputLayout.setError(null);
            }
        }
        @Override public void afterTextChanged(Editable s) {}
    }
}