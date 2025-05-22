package com.pbdvmobile.app.data.dao;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions; // For merging updates
import com.pbdvmobile.app.data.model.User;
import java.util.Map; // For partial updates

public class UserDao {
    private static final String TAG = "UserDaoFirebase";
    public static final String COLLECTION_NAME = "users"; // Made public for easier access
    private final FirebaseFirestore db;

    public UserDao() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Creates or updates a user's profile in Firestore.
     * The document ID will be the user's Firebase Auth UID.
     * @param user The User object to save.
     * @return A Task that completes when the operation is finished.
     */
    public Task<Void> createUserProfile(@NonNull User user) {
        if (user.getUid() == null || user.getUid().isEmpty()) {
            Log.e(TAG, "User UID cannot be null or empty for creating/updating profile.");
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("User UID required."));
        }
        // Use set without merge to ensure the document is created or fully overwritten.
        // If you want to merge (update existing fields and add new ones without deleting others),
        // use SetOptions.merge(). For creating a new profile, a full set is often desired.
        return db.collection(COLLECTION_NAME).document(user.getUid()).set(user);
    }

    /**
     * Fetches a user document by their Firebase Auth UID.
     * @param uid The Firebase Auth UID of the user.
     * @return A Task containing the DocumentSnapshot.
     */
    public Task<DocumentSnapshot> getUserByUid(String uid) {
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "User UID cannot be null or empty for fetching.");
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("User UID cannot be null or empty."));
        }
        return db.collection(COLLECTION_NAME).document(uid).get();
    }

    /**
     * Fetches users by their student number.
     * Note: Ensure 'studentNum' field is indexed in Firestore for efficient querying if used frequently.
     * @param studentNum The student number to search for.
     * @return A Task containing the QuerySnapshot. Expect 0 or 1 result if studentNum is unique.
     */
    public Task<QuerySnapshot> getUserByStudentNum(int studentNum) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("studentNum", studentNum)
                .limit(1) // Assuming studentNum should be unique or you only want the first match
                .get();
    }

    /**
     * Fetches users by their email address.
     * Note: Ensure 'email' field is indexed in Firestore for efficient querying.
     * @param email The email address to search for.
     * @return A Task containing the QuerySnapshot. Expect 0 or 1 result if email is unique.
     */
    public Task<QuerySnapshot> getUserByEmail(String email) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("email", email)
                .limit(1) // Assuming email should be unique
                .get();
    }

    /**
     * Fetches all user documents from the 'users' collection.
     * Use with caution for very large user bases. Consider pagination or more specific queries.
     * @return A Task containing the QuerySnapshot with all users.
     */
    public Task<QuerySnapshot> getAllUsers() {
        return db.collection(COLLECTION_NAME).get();
    }

    /**
     * Fetches all users who are marked as tutors.
     * Note: Ensure 'tutor' field (boolean isTutor) is indexed in Firestore.
     * @return A Task containing the QuerySnapshot with all tutors.
     */
    public Task<QuerySnapshot> getAllTutors() {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("tutor", true) // Field in Firestore is 'tutor' due to isTutor() getter
                .get();
    }

    /**
     * Updates an existing user document. This performs a merge, so only fields present in the
     * User object will be updated. Fields not in the object will remain untouched in Firestore.
     * @param user The User object with updated information. UID must be set.
     * @return A Task that completes when the update is finished.
     */
    public Task<Void> updateUser(@NonNull User user) {
        if (user.getUid() == null || user.getUid().isEmpty()) {
            Log.e(TAG, "User UID required for update.");
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("User UID required for update."));
        }
        return db.collection(COLLECTION_NAME).document(user.getUid()).set(user, SetOptions.merge());
    }

    /**
     * Updates specific fields of a user document using a Map.
     * @param uid The UID of the user to update.
     * @param updates A Map where keys are field names and values are the new field values.
     * @return A Task that completes when the update is finished.
     */
    public Task<Void> updateUserSpecificFields(String uid, Map<String, Object> updates) {
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "User UID required for specific field update.");
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("User UID required for specific field update."));
        }
        if (updates == null || updates.isEmpty()) {
            Log.w(TAG, "Update map is null or empty. No update will be performed.");
            return com.google.android.gms.tasks.Tasks.forResult(null); // Or throw an error
        }
        if(updates.containsKey("tutor") && !(boolean)updates.get("tutor")){

        }
        return db.collection(COLLECTION_NAME).document(uid).update(updates);
    }

    /**
     * Deletes a user document from Firestore.
     * IMPORTANT: This does NOT delete the Firebase Auth user. Auth deletion must be handled separately.
     * @param uid The UID of the user to delete.
     * @return A Task that completes when the deletion is finished.
     */
    public Task<Void> deleteUser(String uid) {
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "User UID required for delete.");
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("User UID required for delete."));
        }
        return db.collection(COLLECTION_NAME).document(uid).delete();
    }
}
