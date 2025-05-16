package com.pbdvmobile.app.util;

import android.app.Activity;

import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;

public class PaymentsUtil {
    // TODO: IMPORTANT - Replace with your actual merchant info and gateway details
    public static final String MERCHANT_NAME = "Your App Name"; // Displayed to user
    public static final String GATEWAY_MERCHANT_ID = "your_gateway_merchant_id"; // From your payment gateway (e.g., Stripe, Braintree)
    public static final String PAYMENT_GATEWAY_TOKENIZATION_NAME = "yourgateway"; // e.g., "stripe", "braintree"

    // Supported card networks
    private static final JSONArray SUPPORTED_NETWORKS = new JSONArray()
            .put("AMEX")
            .put("DISCOVER")
            .put("JCB")
            .put("MASTERCARD")
            .put("VISA");

    // Supported authentication methods for cards
    private static final JSONArray SUPPORTED_METHODS = new JSONArray()
            .put("PAN_ONLY") // Card number only
            .put("CRYPTOGRAM_3DS"); // Tokenized card with 3D Secure if available

    public static PaymentsClient createPaymentsClient(Activity activity) {
        Wallet.WalletOptions walletOptions = new Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST) // Use ENVIRONMENT_PRODUCTION for live
                .build();
        return Wallet.getPaymentsClient(activity, walletOptions);
    }

    private static JSONObject getBaseRequest() throws JSONException {
        return new JSONObject()
                .put("apiVersion", 2)
                .put("apiVersionMinor", 0);
    }

    private static JSONObject getTokenizationSpecification() throws JSONException {
        JSONObject tokenizationSpecification = new JSONObject();
        tokenizationSpecification.put("type", "PAYMENT_GATEWAY");
        tokenizationSpecification.put("parameters", new JSONObject()
                .put("gateway", PAYMENT_GATEWAY_TOKENIZATION_NAME)
                .put("gatewayMerchantId", GATEWAY_MERCHANT_ID));
        // For Stripe, it would be something like:
        // .put("stripe:version", "2020-08-27")
        // .put("stripe:publishableKey", "pk_test_YOUR_STRIPE_PUBLISHABLE_KEY"));
        return tokenizationSpecification;
    }

    private static JSONObject getAllowedPaymentMethods() throws JSONException {
        JSONObject cardPaymentMethod = new JSONObject();
        cardPaymentMethod.put("type", "CARD");
        cardPaymentMethod.put("parameters", new JSONObject()
                        .put("allowedAuthMethods", SUPPORTED_METHODS)
                        .put("allowedCardNetworks", SUPPORTED_NETWORKS)
                // .put("billingAddressRequired", true) // Optional: if you need billing address
                // .put("billingAddressParameters", new JSONObject().put("format", "FULL").put("phoneNumberRequired", true))
        );
        cardPaymentMethod.put("tokenizationSpecification", getTokenizationSpecification());

        return cardPaymentMethod;
    }

    public static Optional<JSONObject> getIsReadyToPayRequest() {
        try {
            JSONObject isReadyToPayRequest = getBaseRequest();
            isReadyToPayRequest.put("allowedPaymentMethods", new JSONArray().put(getAllowedPaymentMethods()));
            return Optional.of(isReadyToPayRequest);
        } catch (JSONException e) {
            return Optional.empty();
        }
    }

    private static JSONObject getTransactionInfo(String priceCents) throws JSONException {
        return new JSONObject()
                .put("totalPrice", priceCents) // Price in the smallest currency unit (e.g., cents)
                .put("totalPriceStatus", "FINAL")
                .put("currencyCode", "ZAR"); // Or your currency
    }

    private static JSONObject getMerchantInfo() throws JSONException {
        return new JSONObject().put("merchantName", MERCHANT_NAME);
        // .put("merchantId", "01234567890123456789"); // Optional: Your Google Merchant ID if registered
    }

    public static Optional<JSONObject> getPaymentDataRequest(String priceCents) {
        try {
            JSONObject paymentDataRequest = getBaseRequest();
            paymentDataRequest.put("allowedPaymentMethods", new JSONArray().put(getAllowedPaymentMethods()));
            paymentDataRequest.put("transactionInfo", getTransactionInfo(priceCents));
            paymentDataRequest.put("merchantInfo", getMerchantInfo());

            // Add shipping address request if needed
            // paymentDataRequest.put("shippingAddressRequired", true);
            // paymentDataRequest.put("shippingAddressParameters", getShippingAddressParameters());

            // Add email request if needed
            paymentDataRequest.put("emailRequired", true);

            return Optional.of(paymentDataRequest);
        } catch (JSONException e) {
            return Optional.empty();
        }
    }
}