package com.pbdvmobile.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class GeneratedContentActivity extends AppCompatActivity {

    public static final int RESULT_REGENERATE = Activity.RESULT_FIRST_USER + 1;
    private static final int CREATE_FILE_REQUEST_CODE = 124; // Request code for SAF

    private TextView txtGeneratedContent, txtContentTitle;
    private Button btnSaveAsPdf, btnRegenerate, btnCloseContent;
    private String generatedText;
    private String originalFileName;
    private String contentType; // "Summary" or "Quiz"
    private Uri sourceDocumentUri;

    private ActivityResultLauncher<Intent> createFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_generated_content);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtContentTitle = findViewById(R.id.txtContentTitle);
        txtGeneratedContent = findViewById(R.id.txtGeneratedContent);
        txtGeneratedContent.setMovementMethod(new ScrollingMovementMethod()); // Enable scrolling
        btnSaveAsPdf = findViewById(R.id.btnSaveAsPdf);
        btnRegenerate = findViewById(R.id.btnRegenerate);
        btnCloseContent = findViewById(R.id.btnCloseContent);

        Intent intent = getIntent();
        generatedText = intent.getStringExtra("GENERATED_TEXT");
        originalFileName = intent.getStringExtra("ORIGINAL_FILE_NAME");
        contentType = intent.getStringExtra("CONTENT_TYPE");
        sourceDocumentUri = intent.getData();

        txtContentTitle.setText("Generated " + (contentType != null ? contentType : "Content"));
        txtGeneratedContent.setText(generatedText != null ? generatedText : "No content generated.");

        createFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            saveTextToPdf(uri);
                        } else {
                            Toast.makeText(this, "Save failed: No URI obtained.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Save cancelled.", Toast.LENGTH_SHORT).show();
                    }
                });


        btnSaveAsPdf.setOnClickListener(v -> createFile());

        btnRegenerate.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            if (sourceDocumentUri != null) {
                resultIntent.setData(sourceDocumentUri);
            }
            setResult(RESULT_REGENERATE, resultIntent);
            finish();
        });

        btnCloseContent.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    private void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, (contentType != null ? contentType : "Document") + "_" + (originalFileName != null ? originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_") : "Generated") + ".pdf");
        createFileLauncher.launch(intent);
    }

    private void saveTextToPdf(Uri uri) {
        if (generatedText == null || generatedText.isEmpty()) {
            Toast.makeText(this, "No content to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        PrintAttributes printAttrs = new PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("zooey", "android", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
        PdfDocument document = new PrintedPdfDocument(this, printAttrs);
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        TextPaint paint = new TextPaint();
        paint.setColor(android.graphics.Color.BLACK);
        paint.setTextSize(12); // Adjust text size as needed

        // Use StaticLayout for multi-line text and pagination
        StaticLayout staticLayout = new StaticLayout(generatedText, paint, canvas.getWidth() - 40, // Width minus margins
                Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        float textHeight = staticLayout.getHeight();
        float y = 20; // Initial Y position (top margin)
        int lines = staticLayout.getLineCount();

        for (int i = 0; i < lines; i++) {
            float lineBottom = y + staticLayout.getLineBottom(i) - staticLayout.getLineTop(i);
            if (lineBottom > pageInfo.getPageHeight() - 20) { // Check if line exceeds page bottom
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageInfo.getPageNumber() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 20; // Reset Y position for new page
            }
            staticLayout.draw(canvas);
            y += staticLayout.getLineBottom(i) - staticLayout.getLineTop(i);
        }

        document.finishPage(page);

        try (FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(uri)) {
            document.writeTo(fos);
            Toast.makeText(this, "PDF saved successfully.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }

    }
}