package com.pbdvmobile.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Import BuildConfig (ensure your package name is correct)
import com.google.android.material.textfield.TextInputEditText;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64; // Requires API 26+
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIBaseActivity extends AppCompatActivity {

    private static final String TAG = "AIBaseActivity";
    private static final double CREDIT_COST_SUMMARY = DataManager.CREDIT_AI_SUMMARIZER; // Ensure DataManager.CREDIT_AI_SUMMARIZER is defined

    private Button btnSelectFiles, btnSubmitSummary, btnRemoveAllFiles;

    private LinearLayout selectedFilesListLayout;
    private ProgressBar progressBarSummary;
    private RadioGroup radioSummaryLength;
    private CheckBox checkKeyPoints, checkBulletFormat;
    private CardView uploadCard;
    private TextView tvNoFilesSelected;
    TextInputEditText txtEtras;

    private ArrayList<Uri> selectedFileUris = new ArrayList<>();
    private ArrayList<String> selectedFileNames = new ArrayList<>(); // For display

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> generatedContentLauncher;

    private LogInUser loggedInUser;
    private User currentUserPojo;
    private UserDao userDao;
    private ExecutorService executorService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aibase);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loggedInUser = LogInUser.getInstance();
        if (loggedInUser != null) {
            currentUserPojo = loggedInUser.getUser();
        }
        userDao = new UserDao();
        executorService = Executors.newSingleThreadExecutor();

        if (currentUserPojo == null || currentUserPojo.getUid() == null) {
            Toast.makeText(this, "User not logged in or data incomplete. Please log in again.", Toast.LENGTH_LONG).show();
            // Optionally, redirect to login screen
            // Intent loginIntent = new Intent(this, LoginActivity.class);
            // startActivity(loginIntent);
            finish();
            return;
        }

        initializeViews();
        setupFilePickerLauncher();
        setupGeneratedContentLauncher();

        btnSelectFiles.setOnClickListener(v -> openFilePicker());
        uploadCard.setOnClickListener(v -> openFilePicker()); // Allow clicking the card
        btnRemoveAllFiles.setOnClickListener(v -> clearAllFileSelections());
        btnSubmitSummary.setOnClickListener(v -> prepareAndPerformGeneration());

        updateSubmitButtonState();
        updateSelectedFilesUI();
    }

    private void initializeViews() {
        uploadCard = findViewById(R.id.uploadCard);
        btnSelectFiles = findViewById(R.id.btnSelectFile);
        btnSubmitSummary = findViewById(R.id.btnSubmit);
        selectedFilesListLayout = findViewById(R.id.selectedFilesListLayout);
        tvNoFilesSelected = findViewById(R.id.tvNoFilesSelected);
        btnRemoveAllFiles = findViewById(R.id.btnRemoveAllFiles);
        progressBarSummary = findViewById(R.id.progressBar);
        radioSummaryLength = findViewById(R.id.radioSummaryLength);
        checkKeyPoints = findViewById(R.id.checkKeyPoints);
        checkBulletFormat = findViewById(R.id.checkBulletFormat);
        txtEtras = findViewById(R.id.aiBaseExtra);
        selectedFilesListLayout.setVisibility(View.GONE);
        btnRemoveAllFiles.setVisibility(View.GONE);
        tvNoFilesSelected.setVisibility(View.VISIBLE);
    }

    private void setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) { // Multiple files selected
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri fileUri = data.getClipData().getItemAt(i).getUri();
                                if (fileUri != null && !uriListContains(selectedFileUris, fileUri)) {
                                    selectedFileUris.add(fileUri);
                                    selectedFileNames.add(getFileNameFromUri(fileUri));
                                }
                            }
                        } else if (data.getData() != null) { // Single file selected
                            Uri fileUri = data.getData();
                            if (fileUri != null && !uriListContains(selectedFileUris, fileUri)) {
                                selectedFileUris.add(fileUri);
                                selectedFileNames.add(getFileNameFromUri(fileUri));
                            }
                        }
                        updateSelectedFilesUI();
                        updateSubmitButtonState();
                    }
                });
    }

    private boolean uriListContains(List<Uri> list, Uri uri) {
        for (Uri item : list) {
            if (item.equals(uri)) return true;
        }
        return false;
    }

    private void setupGeneratedContentLauncher() {
        generatedContentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Check if the result is for regeneration and if there's a file to regenerate from
                    if (result.getResultCode() == GeneratedContentActivity.RESULT_REGENERATE && !selectedFileUris.isEmpty()) {
                        Toast.makeText(this, "Regenerating summary...", Toast.LENGTH_SHORT).show();
                        // Pass the original list of URIs (or the relevant one if your logic changes)
                        // For now, assuming we regenerate based on the same set of files initially selected.
                        // If you only process one file, you might need to pass the specific URI that was processed.
                        performGenerationWithGemini(new ArrayList<>(selectedFileUris), false); // isInitialGeneration = false
                    }
                });
    }


    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Allow all file types initially
        // Define specific MIME types you want to support
        String[] mimetypes = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain", "image/jpeg", "image/png", "image/webp"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple file selection
        filePickerLauncher.launch(intent);
    }

    private void updateSelectedFilesUI() {
        selectedFilesListLayout.removeAllViews(); // Clear previous file views
        if (selectedFileUris.isEmpty()) {
            tvNoFilesSelected.setVisibility(View.VISIBLE);
            selectedFilesListLayout.setVisibility(View.GONE);
            btnRemoveAllFiles.setVisibility(View.GONE);
            txtEtras.setVisibility(View.GONE);
            uploadCard.setVisibility(View.VISIBLE); // Show upload card if no files
        } else {
            tvNoFilesSelected.setVisibility(View.GONE);
            selectedFilesListLayout.setVisibility(View.VISIBLE);
            txtEtras.setVisibility(View.VISIBLE);
            btnRemoveAllFiles.setVisibility(View.VISIBLE);
            uploadCard.setVisibility(View.GONE); // Hide upload card if files are selected

            LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < selectedFileUris.size(); i++) {
                String fileName = selectedFileNames.get(i);
                // Ensure R.layout.item_selected_file exists and has R.id.tvItemFileName and R.id.btnRemoveItemFile
                View fileItemView = inflater.inflate(R.layout.item_selected_file, selectedFilesListLayout, false);

                TextView tvItemFileName = fileItemView.findViewById(R.id.tvItemFileName);
                ImageButton btnRemoveItemFile = fileItemView.findViewById(R.id.btnRemoveItemFile);

                tvItemFileName.setText(fileName);
                final int indexToRemove = i; // Must be final for use in lambda
                btnRemoveItemFile.setOnClickListener(v -> {
                    if (indexToRemove < selectedFileUris.size()) { // Boundary check
                        selectedFileUris.remove(indexToRemove);
                        selectedFileNames.remove(indexToRemove);
                        updateSelectedFilesUI(); // Refresh the list
                        updateSubmitButtonState(); // Update submit button visibility/state
                    }
                });
                selectedFilesListLayout.addView(fileItemView);
            }
        }
    }


    private void clearAllFileSelections() {
        selectedFileUris.clear();
        selectedFileNames.clear();
        updateSelectedFilesUI();
        updateSubmitButtonState();
    }

    private void updateSubmitButtonState() {
        boolean hasFiles = !selectedFileUris.isEmpty();
        btnSubmitSummary.setEnabled(hasFiles);
        btnSubmitSummary.setVisibility(hasFiles ? View.VISIBLE : View.GONE);
    }

    private void prepareAndPerformGeneration() {
        if (selectedFileUris.isEmpty()) {
            Toast.makeText(this, "Please select at least one file.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserPojo == null) { // Should not happen if initial check passes, but good practice
            Toast.makeText(this, "User data not available. Please restart the app.", Toast.LENGTH_LONG).show();
            return;
        }

        if (currentUserPojo.getCredits() < CREDIT_COST_SUMMARY) {
            Toast.makeText(this, "Insufficient credits (" + currentUserPojo.getCredits() + ") to generate summary. Cost: " + CREDIT_COST_SUMMARY, Toast.LENGTH_LONG).show();
            return;
        }

        progressBarSummary.setVisibility(View.VISIBLE);
        btnSubmitSummary.setEnabled(false);

        // Deduct credits
        double newCredits = currentUserPojo.getCredits() - CREDIT_COST_SUMMARY;
        Map<String, Object> creditUpdate = new HashMap<>();
        creditUpdate.put("credits", newCredits);

        userDao.updateUserSpecificFields(currentUserPojo.getUid(), creditUpdate)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Credits deducted successfully for summary. New balance: " + newCredits);
                    currentUserPojo.setCredits(newCredits); // Update local POJO
                    if(loggedInUser != null) loggedInUser.setUser(currentUserPojo); // Update LoggedInUser singleton

                    // IMPORTANT: Currently, this processes only the FIRST file for summarization.
                    // If you want to summarize multiple files, you'll need to loop or concatenate content.
                    ArrayList<Uri> urisToProcess = new ArrayList<>();
                    if (!selectedFileUris.isEmpty()) {
                        urisToProcess.add(selectedFileUris.get(0)); // Process only the first file
                    }
                    performGenerationWithGemini(urisToProcess, true); // isInitialGeneration = true
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to deduct credits for summary", e);
                    Toast.makeText(AIBaseActivity.this, "Failed to update credits. Please try again.", Toast.LENGTH_SHORT).show();
                    progressBarSummary.setVisibility(View.GONE);
                    btnSubmitSummary.setEnabled(true);
                });
    }

    private void performGenerationWithGemini(ArrayList<Uri> fileUris, boolean isInitialGeneration) {
        // API Key Check
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.isEmpty() || "YOUR_ACTUAL_API_KEY_HERE".equals(BuildConfig.GEMINI_API_KEY) || "".equals(BuildConfig.GEMINI_API_KEY)) {
            Log.e(TAG, "API Key not found or not set in BuildConfig. Please check local.properties and rebuild.");
            runOnUiThread(() -> {
                Toast.makeText(AIBaseActivity.this, "API Key not configured. Cannot connect to AI service.", Toast.LENGTH_LONG).show();
                progressBarSummary.setVisibility(View.GONE);
                btnSubmitSummary.setEnabled(true);
                if (isInitialGeneration) refundCreditsOnError(); // Refund if it was an initial attempt
            });
            return;
        }

        if (fileUris.isEmpty()) {
            Toast.makeText(this, "No file selected for summary.", Toast.LENGTH_SHORT).show();
            progressBarSummary.setVisibility(View.GONE);
            btnSubmitSummary.setEnabled(true);
            if (isInitialGeneration) refundCreditsOnError();
            return;
        }

        // This implementation will process only the first URI in the list.
        // Modify if you intend to process multiple files (e.g., concatenate text, send multiple images).
        Uri fileToProcessUri = fileUris.get(0);
        String fileNameForDisplay = getFileNameFromUri(fileToProcessUri);
        String mimeTypeForFile = getMimeType(fileToProcessUri);


        String summaryLengthOption = "Medium"; // Default
        if (radioSummaryLength.getCheckedRadioButtonId() != -1) {
            summaryLengthOption = ((RadioButton) findViewById(radioSummaryLength.getCheckedRadioButtonId())).getText().toString();
        }
        boolean includeKeyPoints = checkKeyPoints.isChecked();
        boolean useBulletFormat = checkBulletFormat.isChecked();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an expert academic summarizer. Summarize the key information from the provided content. ");
        promptBuilder.append("The desired summary length is: ").append(summaryLengthOption).append(". ");
        if (includeKeyPoints) {
            promptBuilder.append("Please also identify and list the main key points. ");
        }
        if (useBulletFormat) {
            promptBuilder.append("Format the main summary body using bullet points where appropriate for clarity. ");
        }

        String extra = txtEtras.getText().toString();
        if(!extra.isEmpty())
            promptBuilder.append(extra);

        final String instructionPromptPart = promptBuilder.toString();


        executorService.execute(() -> {
            String fileContentOrBase64 = extractFileContentOrBase64(fileToProcessUri);
            if (fileContentOrBase64 == null) {
                runOnUiThread(() -> {
                    Toast.makeText(AIBaseActivity.this, "Failed to read file content for: " + fileNameForDisplay, Toast.LENGTH_LONG).show();
                    progressBarSummary.setVisibility(View.GONE);
                    btnSubmitSummary.setEnabled(true);
                    if (isInitialGeneration) refundCreditsOnError();
                });
                return;
            }

            try {
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                JSONArray contentsArray = new JSONArray();
                JSONObject userContent = new JSONObject();
                JSONArray partsArray = new JSONArray();

                // Part 1: Instructions / Text prompt
                JSONObject instructionTextPart = new JSONObject();
                // If it's a text document, append its content to the instruction prompt
                if (mimeTypeForFile != null && !mimeTypeForFile.startsWith("image/")) {
                    instructionTextPart.put("text", instructionPromptPart + "\n\nDocument Content:\n\"\"\"\n" + fileContentOrBase64 + "\n\"\"\"");
                } else {
                    instructionTextPart.put("text", instructionPromptPart); // For images, instruction is separate
                }
                partsArray.put(instructionTextPart);


                // Part 2: Image data (if applicable)
                if (mimeTypeForFile != null && mimeTypeForFile.startsWith("image/")) {
                    JSONObject inlineData = new JSONObject();
                    inlineData.put("mimeType", mimeTypeForFile);
                    inlineData.put("data", fileContentOrBase64); // This is Base64 image data
                    JSONObject imagePart = new JSONObject();
                    imagePart.put("inlineData", inlineData);
                    partsArray.put(imagePart);
                }

                userContent.put("parts", partsArray);
                contentsArray.put(userContent);
                payload.put("contents", contentsArray);

                // Optional: Add generationConfig if needed (e.g., for temperature, topK, topP)
                // JSONObject generationConfig = new JSONObject();
                // generationConfig.put("temperature", 0.7);
                // payload.put("generationConfig", generationConfig);

                Log.d(TAG, "Request Payload: " + payload.toString(2)); // Log with indentation for readability

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Gemini API Response Code: " + responseCode);

                StringBuilder response = new StringBuilder();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                    }
                    Log.d(TAG, "Gemini API Full Response: " + response.toString());

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String generatedSummary = "Error: Could not parse summary from API response."; // Default error
                    if (jsonResponse.has("candidates")) {
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        if (candidates.length() > 0) {
                            JSONObject firstCandidate = candidates.getJSONObject(0);
                            if (firstCandidate.has("content")) {
                                JSONObject candidateContent = firstCandidate.getJSONObject("content");
                                if (candidateContent.has("parts")) {
                                    JSONArray responseParts = candidateContent.getJSONArray("parts");
                                    if (responseParts.length() > 0 && responseParts.getJSONObject(0).has("text")) {
                                        generatedSummary = responseParts.getJSONObject(0).getString("text");
                                    }
                                }
                            } else if (firstCandidate.has("finishReason") && !"STOP".equals(firstCandidate.getString("finishReason"))) {
                                // Handle safety ratings or other finish reasons
                                generatedSummary = "Content generation stopped due to: " + firstCandidate.getString("finishReason") + ". Please try a different file or prompt.";
                                if (firstCandidate.has("safetyRatings")) {
                                    Log.w(TAG, "Safety Ratings: " + firstCandidate.getJSONArray("safetyRatings").toString());
                                }
                            }
                        }
                    }  else if (jsonResponse.has("error")) {
                        JSONObject errorObj = jsonResponse.getJSONObject("error");
                        generatedSummary = "API Error: " + errorObj.optString("message", "Unknown error from API.");
                        Log.e(TAG, "Gemini API Error (in response body): " + errorObj.toString());
                    }


                    final String finalGeneratedSummary = generatedSummary;
                    runOnUiThread(() -> {
                        progressBarSummary.setVisibility(View.GONE);
                        btnSubmitSummary.setEnabled(true);
                        Intent intent = new Intent(AIBaseActivity.this, GeneratedContentActivity.class);
                        intent.putExtra("GENERATED_TEXT", finalGeneratedSummary);
                        intent.putExtra("ORIGINAL_FILE_NAME", fileNameForDisplay);
                        intent.putExtra("CONTENT_TYPE", "Summary");
                        // Pass the URI of the processed file for potential regeneration context
                        intent.setData(fileToProcessUri); // Changed from intent.putExtra(Intent.EXTRA_STREAM, fileToProcessUri);
                        generatedContentLauncher.launch(intent);
                    });

                } else { // HTTP Error (not 200 OK)
                    StringBuilder errorResponseBuilder = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            errorResponseBuilder.append(responseLine.trim());
                        }
                    } catch (IOException | NullPointerException ioException) { // Catch NullPointerException if getErrorStream is null
                        Log.e(TAG, "IOException or Null reading error stream", ioException);
                    }
                    String errorResponse = errorResponseBuilder.toString();
                    Log.e(TAG, "Gemini API Error Response (" + responseCode + "): " + errorResponse);
                    final String finalErrorMsg = "Error (" + responseCode + "): " + conn.getResponseMessage() +
                            (errorResponse.isEmpty() ? "" : " - Details: " + errorResponse.substring(0, Math.min(200, errorResponse.length())));
                    runOnUiThread(() -> {
                        Toast.makeText(AIBaseActivity.this, finalErrorMsg, Toast.LENGTH_LONG).show();
                        progressBarSummary.setVisibility(View.GONE);
                        btnSubmitSummary.setEnabled(true);
                        if (isInitialGeneration) refundCreditsOnError();
                    });
                }
                conn.disconnect();

            } catch (Exception e) { // Catch all other exceptions during API call
                Log.e(TAG, "Exception during Gemini API call", e);
                runOnUiThread(() -> {
                    Toast.makeText(AIBaseActivity.this, "Error generating summary: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBarSummary.setVisibility(View.GONE);
                    btnSubmitSummary.setEnabled(true);
                    if (isInitialGeneration) refundCreditsOnError();
                });
            }
        });
    }

    private void refundCreditsOnError() {
        if (currentUserPojo == null || currentUserPojo.getUid() == null) return;
        Log.d(TAG, "Attempting to refund credits due to API call error for Summary.");
        double refundedCredits = currentUserPojo.getCredits() + CREDIT_COST_SUMMARY;
        Map<String, Object> creditUpdate = new HashMap<>();
        creditUpdate.put("credits", refundedCredits);

        userDao.updateUserSpecificFields(currentUserPojo.getUid(), creditUpdate)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Credits refunded successfully for Summary. New balance: " + refundedCredits);
                    currentUserPojo.setCredits(refundedCredits);
                    if(loggedInUser != null) loggedInUser.setUser(currentUserPojo);
                    Toast.makeText(AIBaseActivity.this, "Credits for failed summary generation have been refunded.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Critical: Failed to refund credits after Summary API error.", e);
                    // Consider how to handle this critical failure, e.g., logging to a remote server
                    Toast.makeText(AIBaseActivity.this, "Critical Error: Failed to refund credits. Please contact support.", Toast.LENGTH_LONG).show();
                });
    }

    private String extractFileContentOrBase64(Uri fileUri) {
        ContentResolver contentResolver = getContentResolver();
        String mimeType = contentResolver.getType(fileUri);
        Log.d(TAG, "Extracting content for URI: " + fileUri + ", MIME type: " + mimeType);

        try (InputStream inputStream = contentResolver.openInputStream(fileUri)) {
            if (inputStream == null) {
                Log.e(TAG, "InputStream is null for URI: " + fileUri);
                return null;
            }
            if (mimeType != null && mimeType.startsWith("image/")) {
                // Check for API level for Base64 encoding
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024 * 4]; // 4KB buffer
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    byte[] imageData = baos.toByteArray();
                    Log.d(TAG, "Image size: " + imageData.length + " bytes");
                    // Gemini has a limit on request size (e.g. 2MB for inline data in gemini-pro-vision)
                    // You might need to check image size and potentially resize/compress if too large
                    // For gemini-2.0-flash, the limit is higher, but good to be mindful.
                    return Base64.getEncoder().encodeToString(imageData);
                } else {
                    Log.e(TAG, "Base64 encoding for images requires Android API 26 (Oreo) or higher.");
                    runOnUiThread(()-> Toast.makeText(this, "Image processing for AI requires Android Oreo (API 26) or higher.", Toast.LENGTH_LONG).show());
                    return null;
                }
            } else if (mimeType != null && (mimeType.equals("text/plain") || mimeType.equals("application/rtf") || mimeType.equals("text/html") || mimeType.equals("application/json") || mimeType.endsWith("xml"))) {
                // For plain text and similar simple text formats
                StringBuilder stringBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                }
                Log.d(TAG, "Extracted text length: " + stringBuilder.length());
                return stringBuilder.toString();
            } else {
                // For PDF, DOCX, etc., true parsing is complex and requires libraries.
                // This basic attempt will likely get garbled text or fail for complex formats.
                // The Gemini API itself can handle PDF, DOC, PPT, etc. if you send the file data directly
                // using "fileData" part instead of "inlineData" or "text".
                // However, that requires a different API structure (multipart request) not implemented here.
                // For now, we'll log a warning and attempt a basic read, which might not be useful.
                Log.w(TAG, "Attempting basic read for unsupported/complex MIME type: " + mimeType + ". For robust results, use a dedicated parsing library or send raw file data to a capable API endpoint.");
                StringBuilder stringBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    int linesRead = 0;
                    // Limit reading for potentially binary files to avoid OOM
                    while ((line = reader.readLine()) != null && linesRead < 500) {
                        stringBuilder.append(line).append("\n");
                        linesRead++;
                    }
                }
                if (stringBuilder.length() > 0) {
                    Log.d(TAG, "Basic text extraction attempt for " + mimeType + " resulted in " + stringBuilder.length() + " chars.");
                    // You might want to return null or a specific message if this is not reliable
                    // For now, returning the (potentially garbled) text.
                    return stringBuilder.toString();
                } else {
                    Log.w(TAG, "Basic text extraction for " + mimeType + " yielded no text.");
                    return null; // Or a specific error message string
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading file content: " + fileUri, e);
            return null;
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "OutOfMemoryError reading file: " + fileUri + ". File might be too large for direct processing.", oom);
            runOnUiThread(()-> Toast.makeText(this, "File is too large to process directly.", Toast.LENGTH_LONG).show());
            return null;
        }
    }


    private String getFileNameFromUri(Uri uri) {
        String fileName = "Unknown_File";
        if (uri == null) return fileName;

        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                // Catch SecurityException or others if permission is denied or cursor fails
                Log.w(TAG, "Error getting file name from content URI: " + e.getMessage());
                // Fallback if cursor method fails
                String path = uri.getPath();
                if (path != null) {
                    int cut = path.lastIndexOf('/');
                    if (cut != -1) {
                        fileName = path.substring(cut + 1);
                    }
                }
            }
        } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1) {
                    fileName = path.substring(cut + 1);
                }
            }
        }
        // Final check if still unknown
        if (TextUtils.isEmpty(fileName) || "Unknown_File".equals(fileName)) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1) {
                    fileName = path.substring(cut + 1);
                }
            }
        }
        return TextUtils.isEmpty(fileName) ? "Unknown_File" : fileName;
    }

    private String getMimeType(Uri uri) {
        String mimeType = null;
        if (uri == null) return "application/octet-stream"; // Default MIME type

        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            mimeType = getContentResolver().getType(uri);
        } else { // For file:// URIs or others
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (fileExtension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase(Locale.US));
            }
        }
        return mimeType != null ? mimeType : "application/octet-stream";
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Attempt to stop all actively executing tasks
        }
    }
}
