
package com.pbdvmobile.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.dao.ResourceDao;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.UserSubject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ResourceUploadActivity extends AppCompatActivity {

    private static final int PDF_SELECTION_REQUEST_CODE = 100;
    private Button selectPdfButton, uploadPdfButton, cancel;
    private Uri selectedPdfUri;
    TextInputEditText txtFileName;
    TextView docName;
    Spinner subjects;
    private DataManager db;
    private LogInUser current_user;
    ResourceDao pdfDao;
    AtomicInteger subjectId = new AtomicInteger(0);

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String savedFilePath; // To store the path of the saved file

    private final ActivityResultLauncher<Intent> pdfSelectionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            selectedPdfUri = result.getData().getData();
                            if (selectedPdfUri != null) {
                                Toast.makeText(this, "PDF Selected: " + selectedPdfUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                                uploadPdfButton.setEnabled(true);
                            String fileName = new File(selectedPdfUri.getPath()).getName();
                                docName.setText(fileName);
                            } else {
                                Toast.makeText(this, "Error selecting PDF", Toast.LENGTH_SHORT).show();
                                uploadPdfButton.setEnabled(false);
                            }
                        } else {
                            Toast.makeText(this, "PDF selection cancelled", Toast.LENGTH_SHORT).show();
                            uploadPdfButton.setEnabled(false);
                        }
                    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resource_upload);

        selectPdfButton = findViewById(R.id.selectPdfButton);
        uploadPdfButton = findViewById(R.id.uploadPdfButton);
        txtFileName = findViewById(R.id.resourceTitle);
        docName = findViewById(R.id.upload_doc_name);
        uploadPdfButton.setEnabled(false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        db = DataManager.getInstance(this);
        current_user = LogInUser.getInstance(db);
        pdfDao = db.getResourceDao();

        subjects = findViewById(R.id.subjectDropdown);

//        String mode = getIntent().getStringExtra("mode");
        createSubjectDropDown();

        selectPdfButton.setOnClickListener(v -> {
            selectPdfFile();
        });

        uploadPdfButton.setOnClickListener(v -> {
            if (selectedPdfUri != null) {
                uploadPdf(selectedPdfUri);
            } else {
                Toast.makeText(this, "No PDF selected for upload", Toast.LENGTH_SHORT).show();
            }
        });

        cancel = findViewById(R.id.upload_cancel_button);
        cancel.setOnClickListener(v -> {
            finish();
            Toast.makeText(this, "Upload cancelled", Toast.LENGTH_SHORT).show();
        });

    }

    private void createSubjectDropDown() {
        List<UserSubject> userSubjects = db.getSubjectDao().getUserSubjects(current_user.getUser().getStudentNum());
        List<Integer> added = new ArrayList<>();
        List<String> subjectsName = new ArrayList<>();
        for(UserSubject subject : userSubjects){

            if(!subject.getTutoring()) continue;
            int subId = subject.getSubjectId();
            added.add(subId);
            Subject sub = db.getSubjectDao().getSubjectById(subId);
            subjectsName.add(sub.getSubjectName());
        }
        if(added.isEmpty()){
            finish();
            Toast.makeText(this, "No subjects for you to tutor", Toast.LENGTH_SHORT).show();
        }
        ArrayAdapter<String> subjectsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                subjectsName
        );
        // Specify dropdown layout style
        subjectsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter
        subjects.setAdapter(subjectsAdapter);
        // Handle item selection
        subjects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selectedItem = parent.getItemAtPosition(position).toString();
                subjectId.set(added.get(position));
//                Toast.makeText(getContext(), "Selected: " + selectedItem, Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }


    private void selectPdfFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        pdfSelectionLauncher.launch(intent);
    }

    private void uploadPdf(Uri pdfUri) {

        String fileName;
        if(!txtFileName.getText().toString().isEmpty()) {
            fileName = txtFileName.getText().toString();
        }else{
            // Get the file name
            fileName = new File(pdfUri.getPath()).getName();
        }

            // Save the PDF to a file in the app's private storage.
//            File savedFile = savePdfToInternalStorage(inputStream, fileName);

            savedFilePath = pdfUri.toString(); // Store the URI as a string

            // Create a PdfData object with the file path.

        if(subjectId.get() >= 1) {
            Resource pdf = new Resource(current_user.getUser().getStudentNum(), subjectId.get(), savedFilePath, fileName);  // Store the path, NOT the bytes
            // Insert the PDF data into the database using a background thread
            executorService.execute(() -> {
                pdfDao.insertResource(pdf);

                runOnUiThread(() -> {
                    Toast.makeText(ResourceUploadActivity.this, "PDF saved to database.", Toast.LENGTH_LONG).show();
                    selectedPdfUri = null;
                    uploadPdfButton.setEnabled(false);
                    finish();
                });
            });
        }else{
            Toast.makeText(this, "No subject selected", Toast.LENGTH_SHORT).show();
        }
    }
}