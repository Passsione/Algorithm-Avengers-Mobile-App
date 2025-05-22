package com.pbdvmobile.app.data.dao;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pbdvmobile.app.data.model.Resource;
import java.util.UUID;
import android.util.Log;

public class ResourceDao {

    public static final String STORAGE_RESOURCES_PATH = "study_resources";
    private static final String TAG = "ResourceDaoFirebase";
    public static final String RESOURCES_COLLECTION = "resources";

    private final FirebaseFirestore dbFirestore;
    private final FirebaseStorage firebaseStorage;

    public ResourceDao() {
        this.dbFirestore = FirebaseFirestore.getInstance();
        this.firebaseStorage = FirebaseStorage.getInstance();
    }

    /**
     * Uploads a file to Firebase Storage.
     * @param fileUri The local URI of the file to upload.
     * @param tutorUid The UID of the tutor uploading the resource.
     * @param originalFileName The original name of the file.
     * @return UploadTask which can be used to monitor the upload.
     */
    public UploadTask uploadResourceFile(@NonNull Uri fileUri, @NonNull String tutorUid, @NonNull String originalFileName) {
        // Sanitize originalFileName or use a generic one if it contains invalid characters for a path
        String safeOriginalFileName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String uniqueFileName = UUID.randomUUID().toString() + "_" + safeOriginalFileName;

        StorageReference fileRef = firebaseStorage.getReference()
                .child(STORAGE_RESOURCES_PATH)
                .child(tutorUid) // Organize by tutor
                .child(uniqueFileName);

        Log.d(TAG, "Uploading file to: " + fileRef.getPath());
        return fileRef.putFile(fileUri);
    }

    /**
     * Saves the metadata of a resource (including its download URL from Storage) to Firestore.
     * @param resourceMetadata The Resource object containing metadata.
     * @return Task for adding the document.
     */
    public Task<DocumentReference> saveResourceMetadata(@NonNull Resource resourceMetadata) {
        if (resourceMetadata.getFileUrl() == null || resourceMetadata.getFileUrl().isEmpty()) {
            Log.e(TAG, "File URL is missing in resource metadata. Cannot save.");
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("File URL is required in resource metadata."));
        }
        return dbFirestore.collection(RESOURCES_COLLECTION).add(resourceMetadata);
    }

    public Task<DocumentSnapshot> getResourceById(String resourceId) {
        return dbFirestore.collection(RESOURCES_COLLECTION).document(resourceId).get();
    }

    public Task<QuerySnapshot> getResourcesByTutorUid(String tutorUid) {
        return dbFirestore.collection(RESOURCES_COLLECTION)
                .whereEqualTo("tutorUid", tutorUid)
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> getResourcesBySubjectId(String subjectId) {
        return dbFirestore.collection(RESOURCES_COLLECTION)
                .whereEqualTo("subjectId", subjectId)
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> getAllResources() {
        return dbFirestore.collection(RESOURCES_COLLECTION)
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Updates the metadata of an existing resource in Firestore.
     * If the file itself changed, it should be re-uploaded to Storage first,
     * and the new fileUrl and storagePath should be in the resource object.
     * @param resourceId The ID of the resource document in Firestore.
     * @param resource The Resource object with updated metadata.
     * @return Task for setting the document.
     */
    public Task<Void> updateResourceMetadata(String resourceId, @NonNull Resource resource) {
        return dbFirestore.collection(RESOURCES_COLLECTION).document(resourceId).set(resource); // Overwrites
    }

    /**
     * Deletes a resource from Firestore and its corresponding file from Firebase Storage.
     * @param resourceId The Firestore document ID of the resource.
     * @param storagePath The path of the file in Firebase Storage (e.g., "study_resources/tutorUid/fileName.pdf").
     * This should be stored in the Resource object.
     * @return Task that completes when both operations are done or one fails.
     */
    public Task<Void> deleteResource(String resourceId, @Nullable String storagePath) {
        if (resourceId == null || resourceId.isEmpty()) {
            Log.e(TAG, "Resource ID is null or empty. Cannot delete.");
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("Resource ID cannot be null or empty."));
        }

        // Step 1: Delete Firestore document
        Task<Void> deleteFirestoreDocTask = dbFirestore.collection(RESOURCES_COLLECTION).document(resourceId).delete();

        return deleteFirestoreDocTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                // Firestore deletion failed, propagate the error
                Log.e(TAG, "Failed to delete resource metadata from Firestore: " + resourceId, task.getException());
                throw task.getException();
            }

            // Step 2: If Firestore delete is successful, delete file from Storage (if path provided)
            if (storagePath != null && !storagePath.isEmpty()) {
                Log.d(TAG, "Deleting file from Storage at path: " + storagePath);
                StorageReference fileRef = firebaseStorage.getReference().child(storagePath); // Use getReference().child(path)
                return fileRef.delete().addOnFailureListener(e_storage ->
                        Log.w(TAG, "Failed to delete file from Storage: " + storagePath + ". Firestore doc was deleted. Manual cleanup may be needed.", e_storage)
                ); // Log if storage deletion fails but don't make the whole task fail if Firestore succeeded.
                // Or, if atomicity is critical, re-add Firestore doc on storage failure (complex).
            } else {
                Log.d(TAG, "No storage path provided for resource " + resourceId + ". Skipping storage deletion.");
                return com.google.android.gms.tasks.Tasks.forResult(null); // Nothing to delete from storage
            }
        });
    }
}
