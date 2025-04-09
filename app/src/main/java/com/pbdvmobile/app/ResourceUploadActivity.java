package com.pbdvmobile.app;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.ResourceService;
import com.pbdvmobile.app.data.UserService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ResourceUploadActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 0;

    private EditText etTitle, etDescription;
    private Spinner spinnerSubject, spinnerResourceType;
    private Button btnSelectFile, btnUpload;
    private TextView tvFileName;

    private Uri selectedFileUri;
    private String selectedFileName;

    private UserService userService;
    private ResourceService resourceService;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource_upload);

        userService = new UserService();
        resourceService = new ResourceService();
        currentUser = userService.getCurrentUser();

        // Check if user is a tutor
        if (currentUser.getUserType() != User.UserType.TUTOR) {
            Toast.makeText(this, "Only tutors can upload resources", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etTitle = findViewById(R.id.et_title);
        etDescription = findViewById(R.id.et_description);
        spinnerSubject = findViewById(R.id.spinner_subject);
        spinnerResourceType = findViewById(R.id.spinner_resource_type);
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnUpload = findViewById(R.id.btn_upload);
        tvFileName = findViewById(R.id.tv_file_name);

        // Setup subject spinner
        List<Subject> subjectList = userService.getAvailableSubjects();
        List<String> subjectNames = new ArrayList<>();
        for (Subject subject : subjectList) {
            subjectNames.add(subject.getName());
        }

        ArrayAdapter<String> subjectsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, subjectNames);
        subjectsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subjectsAdapter);

        // Setup resource type spinner
        ArrayAdapter<CharSequence> resourceTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.resource_types, android.R.layout.simple_spinner_item);
        resourceTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerResourceType.setAdapter(resourceTypeAdapter);

        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadResource();
            }
        });
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            selectedFileName = getFileName(selectedFileUri);
            tvFileName.setText("Selected file: " + selectedFileName);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }

    private void uploadResource() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String subject = spinnerSubject.getSelectedItem().toString();
        String resourceType = spinnerResourceType.getSelectedItem().toString();

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create resource object
        Resource resource = new Resource();
        resource.setTitle(title);
        resource.setDescription(description);
        resource.setSubject(subject);
        resource.setResourceType(resourceType);
        resource.setUploaderId(currentUser.getId());
        resource.setFileName(selectedFileName);

        // Upload resource
        resourceService.uploadResource(resource, selectedFileUri, new ResourceService.ResourceCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ResourceUploadActivity.this, "Resource uploaded successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ResourceUploadActivity.this, "Error uploading resource: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}