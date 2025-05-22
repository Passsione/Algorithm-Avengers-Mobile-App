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

// Import BuildConfig
import com.pbdvmobile.app.BuildConfig;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIQuizActivity extends AppCompatActivity {

    private static final String TAG = "AIQuizActivity";
    // Define a credit cost for quiz generation, adjust as needed in DataManager
    private static final double CREDIT_COST_QUIZ = DataManager.CREDIT_AI_QUIZ; // Example: DataManager.CREDIT_AI_QUIZ = 2.0;

    private Button btnSelectFileQuiz, btnSubmitQuiz;
    private ImageButton btnRemoveFileQuiz;
    private LinearLayout selectedFileContainerQuiz;
    private TextView txtFileNameQuiz, txtFileSizeQuiz; // txtFileSizeQuiz might not be used if we only show name
    private ProgressBar progressBarQuiz;
    private CardView uploadCardQuiz;

    private RadioGroup radioNumQuestions, radioDifficulty;
    private CheckBox checkMcq, checkTrueFalse, checkShortAnswer, checkLongAnswer;

    private Uri selectedFileUri;
    private String originalFileName; // To store the name of the selected file

    private ActivityResultLauncher<Intent> filePickerLauncherQuiz;
    private ActivityResultLauncher<Intent> generatedContentLauncherQuiz;

    private LogInUser loggedInUser;
    private User currentUserPojo;
    private UserDao userDao;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Consider if EdgeToEdge is necessary or causes issues
        setContentView(R.layout.activity_aiquiz);
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
            finish();
            return;
        }

        initializeViewsQuiz();
        setupFilePickerLauncherQuiz();
        setupGeneratedContentLauncherQuiz();

        btnSelectFileQuiz.setOnClickListener(v -> openFilePickerQuiz());
        uploadCardQuiz.setOnClickListener(v -> openFilePickerQuiz());
        btnRemoveFileQuiz.setOnClickListener(v -> clearFileSelectionQuiz());
        btnSubmitQuiz.setOnClickListener(v -> prepareAndPerformQuizGeneration());

        updateSubmitButtonStateQuiz();
        updateSelectedFileUINoQuiz(); // Initial UI state for file selection
    }

    private void initializeViewsQuiz() {
        uploadCardQuiz = findViewById(R.id.uploadCardQuiz);
        btnSelectFileQuiz = findViewById(R.id.btnSelectFileQuiz);
        btnSubmitQuiz = findViewById(R.id.btnSubmitQuiz);
        selectedFileContainerQuiz = findViewById(R.id.selectedFileContainerQuiz); // This is the LinearLayout
        txtFileNameQuiz = findViewById(R.id.txtFileNameQuiz); // TextView for the file name inside the container
        // txtFileSizeQuiz = findViewById(R.id.txtFileSizeQuiz); // If you have this for size
        btnRemoveFileQuiz = findViewById(R.id.btnRemoveFileQuiz); // ImageButton to remove the file
        progressBarQuiz = findViewById(R.id.progressBarQuiz);

        radioNumQuestions = findViewById(R.id.radioNumQuestions);
        radioDifficulty = findViewById(R.id.radioDifficulty);
        checkMcq = findViewById(R.id.checkMcq);
        checkTrueFalse = findViewById(R.id.checkTrueFalse);
        checkLongAnswer = findViewById(R.id.checkLongAnswer);
        checkShortAnswer = findViewById(R.id.checkShortAnswer);
    }

    private void setupFilePickerLauncherQuiz() {
        filePickerLauncherQuiz = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedFileUri = result.getData().getData();
                        if (selectedFileUri != null) {
                            originalFileName = getFileNameFromUri(selectedFileUri); // Use helper
                            updateSelectedFileUINoQuiz();
                        }
                        updateSubmitButtonStateQuiz();
                    }
                });
    }

    private void setupGeneratedContentLauncherQuiz() {
        generatedContentLauncherQuiz = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == GeneratedContentActivity.RESULT_REGENERATE && selectedFileUri != null) {
                        Toast.makeText(this, "Regenerating quiz...", Toast.LENGTH_SHORT).show();
                        performQuizGenerationWithGemini(selectedFileUri, false); // isInitialGeneration = false
                    }
                });
    }

    private void openFilePickerQuiz() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Quizzes are best generated from text-based content. Images are less suitable.
        String[] mimetypes = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain", "text/html"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        intent.setType("*/*"); // Fallback if EXTRA_MIME_TYPES is not respected by all file pickers
        filePickerLauncherQuiz.launch(intent);
    }

    private void updateSelectedFileUINoQuiz() {
        if (selectedFileUri != null && originalFileName != null) {
            txtFileNameQuiz.setText(originalFileName);
            // if (txtFileSizeQuiz != null) { /* Set file size if you extract it */ }
            selectedFileContainerQuiz.setVisibility(View.VISIBLE);
            uploadCardQuiz.setVisibility(View.GONE);
            btnSubmitQuiz.setVisibility(View.VISIBLE); // Show submit button when file is selected
        } else {
            selectedFileContainerQuiz.setVisibility(View.GONE);
            uploadCardQuiz.setVisibility(View.VISIBLE);
            btnSubmitQuiz.setVisibility(View.GONE); // Hide submit button if no file
        }
    }

    private void clearFileSelectionQuiz() {
        selectedFileUri = null;
        originalFileName = null;
        updateSelectedFileUINoQuiz();
        updateSubmitButtonStateQuiz();
    }

    private void updateSubmitButtonStateQuiz() {
        btnSubmitQuiz.setEnabled(selectedFileUri != null);
    }

    private void prepareAndPerformQuizGeneration() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkMcq.isChecked() && !checkTrueFalse.isChecked() && !checkShortAnswer.isChecked() && !checkLongAnswer.isChecked()) {
            Toast.makeText(this, "Please select at least one question type.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserPojo == null) {
            Toast.makeText(this, "User data not available. Please restart.", Toast.LENGTH_LONG).show();
            return;
        }
        if (currentUserPojo.getCredits() < CREDIT_COST_QUIZ) {
            Toast.makeText(this, "Insufficient credits (" + currentUserPojo.getCredits() + ") for quiz. Cost: " + CREDIT_COST_QUIZ, Toast.LENGTH_LONG).show();
            return;
        }

        progressBarQuiz.setVisibility(View.VISIBLE);
        btnSubmitQuiz.setEnabled(false);

        double newCredits = currentUserPojo.getCredits() - CREDIT_COST_QUIZ;
        Map<String, Object> creditUpdate = new HashMap<>();
        creditUpdate.put("credits", newCredits);

        userDao.updateUserSpecificFields(currentUserPojo.getUid(), creditUpdate)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Credits deducted for quiz. New balance: " + newCredits);

                    currentUserPojo.setCredits(newCredits);
                    if (loggedInUser != null) loggedInUser.setUser(currentUserPojo);
                    performQuizGenerationWithGemini(selectedFileUri, true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to deduct credits for quiz", e);
                    Toast.makeText(AIQuizActivity.this, "Failed to update credits. Please try again.", Toast.LENGTH_SHORT).show();
                    progressBarQuiz.setVisibility(View.GONE);
                    btnSubmitQuiz.setEnabled(true);
                });
    }


    private void performQuizGenerationWithGemini(Uri fileUri, boolean isInitialGeneration) {
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.isEmpty() || "YOUR_ACTUAL_API_KEY_HERE".equals(BuildConfig.GEMINI_API_KEY) || "".equals(BuildConfig.GEMINI_API_KEY)) {
            Log.e(TAG, "API Key not found or not set in BuildConfig.");
            runOnUiThread(() -> {
                Toast.makeText(AIQuizActivity.this, "API Key not configured.", Toast.LENGTH_LONG).show();
                progressBarQuiz.setVisibility(View.GONE);
                btnSubmitQuiz.setEnabled(true);
                if (isInitialGeneration) refundCreditsForQuizOnError();
            });
            return;
        }

        String numQuestionsOption; // Default
        if (radioNumQuestions.getCheckedRadioButtonId() != -1){
            numQuestionsOption = ((RadioButton) findViewById(radioNumQuestions.getCheckedRadioButtonId())).getText().toString();
        } else {
            numQuestionsOption = "5";
        }

        String difficultyOption; // Default
        if (radioDifficulty.getCheckedRadioButtonId() != -1) {
            difficultyOption = ((RadioButton) findViewById(radioDifficulty.getCheckedRadioButtonId())).getText().toString();
        } else {
            difficultyOption = "Medium";
        }

        List<String> questionTypesList = new ArrayList<>();
        if (checkMcq.isChecked()) questionTypesList.add("Multiple Choice Questions (MCQ)");
        if (checkTrueFalse.isChecked()) questionTypesList.add("True/False Questions");
        if (checkShortAnswer.isChecked()) questionTypesList.add("Short Answer Questions");
        if(checkLongAnswer.isChecked()) questionTypesList.add("Paragraph / Exam Answer Questions");

        if (questionTypesList.isEmpty()) { // Should be caught by prepareAndPerformQuizGeneration, but double check
            Toast.makeText(this, "Please select at least one question type.", Toast.LENGTH_SHORT).show();
            progressBarQuiz.setVisibility(View.GONE);
            btnSubmitQuiz.setEnabled(true);
            if (isInitialGeneration) refundCreditsForQuizOnError();
            return;
        }
        String questionTypes = String.join(", ", questionTypesList);

        final String currentOriginalFileName = this.originalFileName; // Use the class member

        executorService.execute(() -> {
            String fileContent = extractFileContentOrBase64(fileUri); // Expecting text content
            if (fileContent == null || getMimeType(fileUri).startsWith("image/")) { // Quiz from image is not ideal
                runOnUiThread(() -> {
                    Toast.makeText(AIQuizActivity.this, "Failed to read text content or file is an image (not suitable for quiz).", Toast.LENGTH_LONG).show();
                    progressBarQuiz.setVisibility(View.GONE);
                    btnSubmitQuiz.setEnabled(true);
                    if (isInitialGeneration) refundCreditsForQuizOnError();
                });
                return;
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are an expert quiz generator. Based on the following document content, create a quiz.\n");
            promptBuilder.append("Number of questions: ").append(numQuestionsOption).append(".\n");
            promptBuilder.append("Difficulty level: ").append(difficultyOption).append(".\n");
            promptBuilder.append("Include the following question types: ").append(questionTypes).append(".\n");
            promptBuilder.append("Ensure questions are relevant to the provided text. Format the output clearly, with questions numbered and options clearly labeled for MCQs.\n\n");
            promptBuilder.append("Document Content:\n\"\"\"\n");
            promptBuilder.append(fileContent);
            promptBuilder.append("\n\"\"\"");

            String finalPrompt = promptBuilder.toString();
            Log.d(TAG, "Quiz Generation Prompt: " + finalPrompt.substring(0, Math.min(finalPrompt.length(), 500)) + "...");


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
                JSONObject textPart = new JSONObject();

                textPart.put("text", finalPrompt);
                partsArray.put(textPart);
                userContent.put("parts", partsArray);
                contentsArray.put(userContent);
                payload.put("contents", contentsArray);

                // Log.d(TAG, "Quiz Request Payload: " + payload.toString(2));

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Gemini API Response Code for Quiz: " + responseCode);
                StringBuilder response = new StringBuilder();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                    }
                    // Log.d(TAG, "Gemini API Full Response for Quiz: " + response.toString());
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String generatedQuizText = "Error: Could not parse quiz from API.";
                    if (jsonResponse.has("candidates")) {
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        if (candidates.length() > 0) {
                            JSONObject firstCandidate = candidates.getJSONObject(0);
                            if (firstCandidate.has("content")) {
                                JSONObject candidateContent = firstCandidate.getJSONObject("content");
                                if (candidateContent.has("parts")) {
                                    JSONArray responseParts = candidateContent.getJSONArray("parts");
                                    if (responseParts.length() > 0 && responseParts.getJSONObject(0).has("text")) {
                                        generatedQuizText = responseParts.getJSONObject(0).getString("text");
                                    }
                                }
                            } else if (firstCandidate.has("finishReason") && !"STOP".equals(firstCandidate.getString("finishReason"))) {
                                generatedQuizText = "Quiz generation stopped due to: " + firstCandidate.getString("finishReason");
                            }
                        }
                    } else if (jsonResponse.has("error")) {
                        JSONObject errorObj = jsonResponse.getJSONObject("error");
                        generatedQuizText = "API Error: " + errorObj.optString("message", "Unknown error.");
                    }


                    final String finalGeneratedQuiz = generatedQuizText;
                    runOnUiThread(() -> {
                        progressBarQuiz.setVisibility(View.GONE);
                        btnSubmitQuiz.setEnabled(true);
                        Intent intent = new Intent(AIQuizActivity.this, GeneratedContentActivity.class);
                        intent.putExtra("GENERATED_TEXT", finalGeneratedQuiz);
                        intent.putExtra("ORIGINAL_FILE_NAME", currentOriginalFileName);
                        intent.putExtra("CONTENT_TYPE", "Quiz");
                        intent.setData(fileUri); // Pass URI for regeneration context
                        generatedContentLauncherQuiz.launch(intent);
                    });

                } else {
                    StringBuilder errorResponseBuilder = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            errorResponseBuilder.append(responseLine.trim());
                        }
                    } catch (IOException | NullPointerException ioException) {
                        Log.e(TAG, "IOException or Null reading error stream for quiz", ioException);
                    }
                    String errorResponse = errorResponseBuilder.toString();
                    Log.e(TAG, "Gemini API Error (Quiz) ("+responseCode+"): " + errorResponse);
                    final String finalErrorMsg = "Error ("+responseCode+"): " + conn.getResponseMessage() + (errorResponse.isEmpty() ? "" : " - " + errorResponse.substring(0, Math.min(200, errorResponse.length())));

                    runOnUiThread(() -> {
                        Toast.makeText(AIQuizActivity.this, finalErrorMsg, Toast.LENGTH_LONG).show();
                        progressBarQuiz.setVisibility(View.GONE);
                        btnSubmitQuiz.setEnabled(true);
                        if (isInitialGeneration) refundCreditsForQuizOnError();
                    });
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Exception during Gemini API call for Quiz", e);
                runOnUiThread(() -> {
                    Toast.makeText(AIQuizActivity.this, "Error generating quiz: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBarQuiz.setVisibility(View.GONE);
                    btnSubmitQuiz.setEnabled(true);
                    if (isInitialGeneration) refundCreditsForQuizOnError();
                });
            }
        });
    }

    private void refundCreditsForQuizOnError() {
        if (currentUserPojo == null || currentUserPojo.getUid() == null) return;
        Log.d(TAG, "Attempting to refund credits for Quiz.");
        double refundedCredits = currentUserPojo.getCredits() + CREDIT_COST_QUIZ;
        Map<String, Object> creditUpdate = new HashMap<>();
        creditUpdate.put("credits", refundedCredits);

        userDao.updateUserSpecificFields(currentUserPojo.getUid(), creditUpdate)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Credits refunded for Quiz. New balance: " + refundedCredits);
                    currentUserPojo.setCredits(refundedCredits);
                    if(loggedInUser != null) loggedInUser.setUser(currentUserPojo);
                    Toast.makeText(AIQuizActivity.this, "Credits for failed quiz generation refunded.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Critical: Failed to refund credits for Quiz.", e);
                    Toast.makeText(AIQuizActivity.this, "Critical Error: Failed to refund quiz credits.", Toast.LENGTH_LONG).show();
                });
    }

    // Helper methods (copied from AIBaseActivity, ensure they are suitable or adapt)
    private String extractFileContentOrBase64(Uri fileUri) {
        ContentResolver contentResolver = getContentResolver();
        String mimeType = contentResolver.getType(fileUri);
        Log.d(TAG, "Quiz: Extracting content for URI: " + fileUri + ", MIME type: " + mimeType);

        // For quizzes, we primarily want text. Images are not suitable.
        if (mimeType != null && mimeType.startsWith("image/")) {
            Log.w(TAG, "Quiz: Attempting to process an image file. Quizzes are best generated from text.");
            // Returning null to indicate unsuitability or you could try OCR if you had such a library
            return null;
        }

        try (InputStream inputStream = contentResolver.openInputStream(fileUri)) {
            if (inputStream == null) {
                Log.e(TAG, "Quiz: InputStream is null for URI: " + fileUri);
                return null;
            }
            // Assuming text-based file for quiz generation
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
            }
            Log.d(TAG, "Quiz: Extracted text length: " + stringBuilder.length());
            return stringBuilder.toString();

        } catch (IOException e) {
            Log.e(TAG, "Quiz: Error reading file content: " + fileUri, e);
            return null;
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "Quiz: OutOfMemoryError reading file: " + fileUri, oom);
            runOnUiThread(()-> Toast.makeText(this, "File is too large for quiz processing.", Toast.LENGTH_LONG).show());
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
                Log.w(TAG, "Quiz: Error getting file name: " + e.getMessage());
                String path = uri.getPath(); // Fallback
                if (path != null) {
                    int cut = path.lastIndexOf('/');
                    if (cut != -1) fileName = path.substring(cut + 1);
                }
            }
        } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1) fileName = path.substring(cut + 1);
            }
        }
        if (TextUtils.isEmpty(fileName) || "Unknown_File".equals(fileName)) { // Final fallback
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1) fileName = path.substring(cut + 1);
            }
        }
        return TextUtils.isEmpty(fileName) ? "Unknown_File" : fileName;
    }

    private String getMimeType(Uri uri) { // Added for extractFileContentOrBase64
        String mimeType = null;
        if (uri == null) return "application/octet-stream";
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            mimeType = getContentResolver().getType(uri);
        } else {
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
            executorService.shutdownNow();
        }
    }
}
