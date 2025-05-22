package com.pbdvmobile.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class AIQuizActivity extends AppCompatActivity {

    private Button btnSelectFileQuiz, btnSubmitQuiz;
    private ImageButton btnRemoveFileQuiz;
    private LinearLayout selectedFileContainerQuiz;
    private TextView txtFileNameQuiz, txtFileSizeQuiz;
    private ProgressBar progressBarQuiz;
    private CardView uploadCardQuiz;

    private RadioGroup radioNumQuestions, radioDifficulty;
    private CheckBox checkMcq, checkTrueFalse, checkShortAnswer;


    private Uri selectedFileUri;
    private String originalFileName;

    private ActivityResultLauncher<Intent> filePickerLauncherQuiz;
    private ActivityResultLauncher<Intent> generatedContentLauncherQuiz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_aiquiz);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        uploadCardQuiz = findViewById(R.id.uploadCardQuiz);
        btnSelectFileQuiz = findViewById(R.id.btnSelectFileQuiz);
        btnSubmitQuiz = findViewById(R.id.btnSubmitQuiz);
        selectedFileContainerQuiz = findViewById(R.id.selectedFileContainerQuiz);
        txtFileNameQuiz = findViewById(R.id.txtFileNameQuiz);
        txtFileSizeQuiz = findViewById(R.id.txtFileSizeQuiz);
        btnRemoveFileQuiz = findViewById(R.id.btnRemoveFileQuiz);
        progressBarQuiz = findViewById(R.id.progressBarQuiz);

        radioNumQuestions = findViewById(R.id.radioNumQuestions);
        radioDifficulty = findViewById(R.id.radioDifficulty);
        checkMcq = findViewById(R.id.checkMcq);
        checkTrueFalse = findViewById(R.id.checkTrueFalse);
        checkShortAnswer = findViewById(R.id.checkShortAnswer);


        filePickerLauncherQuiz = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedFileUri = result.getData().getData();
                        displayFileInfoQuiz(selectedFileUri);
                    }
                });

        generatedContentLauncherQuiz = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == GeneratedContentActivity.RESULT_REGENERATE && selectedFileUri != null) {
                        Toast.makeText(this, "Regenerating quiz...", Toast.LENGTH_SHORT).show();
                        performQuizGeneration();
                    }
                });

        btnSelectFileQuiz.setOnClickListener(v -> openFilePickerQuiz());
        uploadCardQuiz.setOnClickListener(v -> openFilePickerQuiz());

        btnRemoveFileQuiz.setOnClickListener(v -> clearFileSelectionQuiz());

        btnSubmitQuiz.setOnClickListener(v -> performQuizGeneration());

        updateSubmitButtonStateQuiz();
    }

    private void openFilePickerQuiz() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimetypes = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        filePickerLauncherQuiz.launch(intent);
    }

    private void displayFileInfoQuiz(Uri uri) {
        if (uri == null) return;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

            originalFileName = cursor.getString(nameIndex);
            long fileSize = cursor.getLong(sizeIndex);

            txtFileNameQuiz.setText(originalFileName);
            txtFileSizeQuiz.setText(android.text.format.Formatter.formatShortFileSize(this, fileSize));

            selectedFileContainerQuiz.setVisibility(View.VISIBLE);
            uploadCardQuiz.setVisibility(View.GONE);
            btnSubmitQuiz.setVisibility(View.VISIBLE);

            cursor.close();
        } else {
            originalFileName = "Unknown File";
            txtFileNameQuiz.setText(originalFileName);
            txtFileSizeQuiz.setText("N/A");
            selectedFileContainerQuiz.setVisibility(View.VISIBLE);
            btnSubmitQuiz.setVisibility(View.GONE);
            uploadCardQuiz.setVisibility(View.GONE);
        }
        updateSubmitButtonStateQuiz();
    }

    private void clearFileSelectionQuiz() {
        selectedFileUri = null;
        originalFileName = null;
        selectedFileContainerQuiz.setVisibility(View.GONE);
        uploadCardQuiz.setVisibility(View.VISIBLE);
        updateSubmitButtonStateQuiz();
    }

    private void updateSubmitButtonStateQuiz() {
        btnSubmitQuiz.setEnabled(selectedFileUri != null);
    }

    private void performQuizGeneration() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String numQuestions = ((RadioButton) findViewById(radioNumQuestions.getCheckedRadioButtonId())).getText().toString();
        String difficulty = ((RadioButton) findViewById(radioDifficulty.getCheckedRadioButtonId())).getText().toString();
        List<String> questionTypes = new ArrayList<>();
        if (checkMcq.isChecked()) questionTypes.add("Multiple Choice");
        if (checkTrueFalse.isChecked()) questionTypes.add("True/False");
        if (checkShortAnswer.isChecked()) questionTypes.add("Short Answer");

        if (questionTypes.isEmpty()){
            Toast.makeText(this, "Please select at least one question type.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarQuiz.setVisibility(View.VISIBLE);
        btnSubmitQuiz.setEnabled(false);

        // --- AI QUIZ PROCESSING  ---
        new android.os.Handler().postDelayed(() -> {
            progressBarQuiz.setVisibility(View.GONE);
            btnSubmitQuiz.setEnabled(true);

            // Dummy generated quiz
            StringBuilder generatedQuiz = new StringBuilder("AI Generated Quiz from: " + originalFileName + "\n");
            generatedQuiz.append("Number of Questions: ").append(numQuestions).append("\n");
            generatedQuiz.append("Difficulty: ").append(difficulty).append("\n");
            generatedQuiz.append("Question Types: ").append(String.join(", ", questionTypes)).append("\n\n");

            for (int i = 1; i <= Integer.parseInt(numQuestions); i++) {
                generatedQuiz.append("Q").append(i).append(": This is a sample ").append(questionTypes.get(0)).append(" question (").append(difficulty.toLowerCase()).append(")?\n");
                if (questionTypes.contains("Multiple Choice")) {
                    generatedQuiz.append("   A) Option 1\n   B) Option 2\n   C) Option 3\n   D) Option 4\n");
                }
                generatedQuiz.append("\n");
            }

            Intent intent = new Intent(AIQuizActivity.this, GeneratedContentActivity.class);
            intent.putExtra("GENERATED_TEXT", generatedQuiz.toString());
            intent.putExtra("ORIGINAL_FILE_NAME", originalFileName);
            intent.putExtra("CONTENT_TYPE", "Quiz");
            intent.setData(selectedFileUri);
            generatedContentLauncherQuiz.launch(intent);

        }, 2500); // Simulate 2.5 seconds processing
    }
}