package com.pbdvmobile.app.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.Timestamp; // For upload date
import java.io.Serializable;

public class Resource implements Serializable {

    @Exclude // Firestore document ID will be the ID
    private String id;

    private String name;        // Display name of the resource
    private String description; // Optional description
    private String tutorUid;    // Firebase UID of the tutor who uploaded
    private String subjectId;   // Firestore Document ID of the Subject it relates to
    private String subjectName; // Denormalized subject name for easier display
    private String fileUrl;     // Download URL from Firebase Storage
    private String storagePath; // Path in Firebase Storage (for deletion)
    private String fileType;    // MIME type or extension (e.g., "application/pdf", "image/jpeg")
    private long fileSize;      // Size of the file in bytes
    private Timestamp uploadedAt; // When the resource was uploaded

    // Firestore requires a public no-argument constructor
    public Resource() {
    }

    public Resource(String name, String description, String tutorUid, String subjectId, String subjectName, String fileUrl, String storagePath, String fileType, long fileSize) {
        this.name = name;
        this.description = description;
        this.tutorUid = tutorUid;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.fileUrl = fileUrl;
        this.storagePath = storagePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedAt = Timestamp.now(); // Set upload time on creation
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTutorUid() { return tutorUid; }
    public void setTutorUid(String tutorUid) { this.tutorUid = tutorUid; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public Timestamp getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Timestamp uploadedAt) { this.uploadedAt = uploadedAt; }

    // Helper to get uploadedAt as java.util.Date if needed by UI
    @Exclude
    public java.util.Date getUploadedAtAsDate() {
        return uploadedAt != null ? uploadedAt.toDate() : null;
    }
}
