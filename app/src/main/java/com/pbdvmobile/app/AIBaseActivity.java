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
import android.widget.ImageView;
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

public class AIBaseActivity extends AppCompatActivity {

    private static final int GENERATE_REQUEST_CODE = 101; // For "Regenerate"

    private Button btnSelectFile, btnSubmit;
    private ImageButton btnRemoveFile;
    private LinearLayout selectedFileContainer;
    private TextView txtFileName, txtFileSize;
    private ProgressBar progressBar;
    private RadioGroup radioSummaryLength;
    private CheckBox checkKeyPoints, checkBulletFormat;
    private CardView uploadCard;


    private Uri selectedFileUri;
    private String originalFileName; // To pass to GeneratedContentActivity

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> generatedContentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_aibase);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        uploadCard = findViewById(R.id.uploadCard);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnSubmit = findViewById(R.id.btnSubmit);
        selectedFileContainer = findViewById(R.id.selectedFileContainer);
        txtFileName = findViewById(R.id.txtFileName);
        txtFileSize = findViewById(R.id.txtFileSize);
        btnRemoveFile = findViewById(R.id.btnRemoveFile);
        progressBar = findViewById(R.id.progressBar);
        radioSummaryLength = findViewById(R.id.radioSummaryLength);
        checkKeyPoints = findViewById(R.id.checkKeyPoints);
        checkBulletFormat = findViewById(R.id.checkBulletFormat);

        // Initialize file picker launcher
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedFileUri = result.getData().getData();
                        displayFileInfo(selectedFileUri);
                    }
                });

        // Initialize launcher for GeneratedContentActivity
        generatedContentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == GeneratedContentActivity.RESULT_REGENERATE && selectedFileUri != null) {
                        // User wants to regenerate, trigger generation again
                        Toast.makeText(this, "Regenerating summary...", Toast.LENGTH_SHORT).show();
                        performGeneration();
                    } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        // User closed the content view
                        // You might want to clear the selection or allow further edits
                    }
                });


        btnSelectFile.setOnClickListener(v -> openFilePicker());
        uploadCard.setOnClickListener(v -> openFilePicker()); // Allow clicking whole card

        btnRemoveFile.setOnClickListener(v -> clearFileSelection());

        btnSubmit.setOnClickListener(v -> performGeneration());

        updateSubmitButtonState();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Or be more specific e.g. "application/pdf", "text/plain"
        String[] mimetypes = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        filePickerLauncher.launch(intent);
    }

    private void displayFileInfo(Uri uri) {
        if (uri == null) return;

        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

            originalFileName = cursor.getString(nameIndex);
            long fileSize = cursor.getLong(sizeIndex);

            txtFileName.setText(originalFileName);
            txtFileSize.setText(android.text.format.Formatter.formatShortFileSize(this, fileSize));

            selectedFileContainer.setVisibility(View.VISIBLE);
            btnSubmit.setVisibility(View.VISIBLE);

            uploadCard.setVisibility(View.GONE); // Hide upload card once file is selected
            cursor.close();
        } else {
            originalFileName = "Unknown File";
            txtFileName.setText(originalFileName);
            txtFileSize.setText("N/A");
            selectedFileContainer.setVisibility(View.VISIBLE);
            btnSubmit.setVisibility(View.GONE);
            uploadCard.setVisibility(View.GONE);
        }
        updateSubmitButtonState();
    }

    private void clearFileSelection() {
        selectedFileUri = null;
        originalFileName = null;
        selectedFileContainer.setVisibility(View.GONE);
        uploadCard.setVisibility(View.VISIBLE); // Show upload card again
        updateSubmitButtonState();
    }

    private void updateSubmitButtonState() {
        btnSubmit.setEnabled(selectedFileUri != null);
    }

    private void performGeneration() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file first.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        // --- AI PROCESSING SIMULATION ---
        // In a real app, you'd read the file content from selectedFileUri,
        // send it to your AI backend, and get the summary.
        // For now, we'll simulate a delay and generate dummy content.
        new android.os.Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);

            String summaryLength = ((RadioButton) findViewById(radioSummaryLength.getCheckedRadioButtonId())).getText().toString();
            boolean includeKeyPoints = checkKeyPoints.isChecked();
            boolean bulletFormat = checkBulletFormat.isChecked();

            // Dummy generated summary
            String generatedSummary = "This is a " + summaryLength.toLowerCase() + " AI summary for the document: " + originalFileName + ".\n"
                    + (includeKeyPoints ? "\nKey Points:\n- Point 1\n- Point 2\n" : "")
                    + (bulletFormat ? "\nSummary Details (bullets):\n* Detail A\n* Detail B" : "\nSummary Details (paragraph): Some detailed text about the summary.");

            Intent intent = new Intent(AIBaseActivity.this, GeneratedContentActivity.class);
            intent.putExtra("GENERATED_TEXT", generatedSummary);
            intent.putExtra("ORIGINAL_FILE_NAME", originalFileName);
            intent.putExtra("CONTENT_TYPE", "Summary");
            intent.setData(selectedFileUri); // Pass URI for potential "Save As" or context
            generatedContentLauncher.launch(intent);

        }, 2000); // Simulate 2 seconds processing
    }
}