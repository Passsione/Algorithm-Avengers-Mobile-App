package com.pbdvmobile.app.data.dao;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.pbdvmobile.app.data.model.Subject;
// UserSubject related operations are more complex and depend on your data model.
// If UserSubject is a separate collection:
// import com.pbdvmobile.app.data.model.UserSubject;
// import java.util.Map;

public class SubjectDao {
    private static final String TAG = "SubjectDaoFirebase";
    public static final String SUBJECTS_COLLECTION = "subjects";
    // If you have a separate collection for User-Subject mapping:
    // private static final String USER_SUBJECTS_COLLECTION = "user_subject_relations";
    private final FirebaseFirestore db;

    public SubjectDao() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Inserts a new subject into the 'subjects' collection.
     * Firestore will auto-generate the document ID.
     * @param subject The Subject object to insert. The 'id' field in the POJO will not be used for insertion.
     * @return A Task containing the DocumentReference of the newly created subject.
     */
    public Task<DocumentReference> insertSubject(@NonNull Subject subject) {
        // The Subject POJO's 'id' field is typically set after fetching or not used for insertion.
        return db.collection(SUBJECTS_COLLECTION).add(subject);
    }

    /**
     * Fetches a subject by its Firestore document ID.
     * @param subjectDocumentId The document ID of the subject.
     * @return A Task containing the DocumentSnapshot.
     */
    public Task<DocumentSnapshot> getSubjectById(String subjectDocumentId) {
        if (subjectDocumentId == null || subjectDocumentId.isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("Subject document ID cannot be null or empty."));
        }
        return db.collection(SUBJECTS_COLLECTION).document(subjectDocumentId).get();
    }

    /**
     * Fetches all subjects from the 'subjects' collection, ordered by subjectName.
     * @return A Task containing the QuerySnapshot.
     */
    public Task<QuerySnapshot> getAllSubjects() {
        return db.collection(SUBJECTS_COLLECTION).orderBy("subjectName", Query.Direction.ASCENDING).get();
    }

    /**
     * Fetches a subject by its name.
     * Note: Requires an exact match on 'subjectName' and that this field is indexed for efficiency if used often.
     * @param subjectName The name of the subject to find.
     * @return A Task containing the QuerySnapshot. Usually 0 or 1 result.
     */
    public Task<QuerySnapshot> getSubjectByName(String subjectName) {
        if (subjectName == null || subjectName.isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("Subject name cannot be null or empty."));
        }
        return db.collection(SUBJECTS_COLLECTION).whereEqualTo("subjectName", subjectName).limit(1).get();
    }


    // --- UserSubject section ---
    // Operations related to UserSubject (linking users to subjects they tutor or take)
    // or via a more complex "user_subject_relations" collection if detailed properties like marks are needed.

    // If UserSubject is a separate collection (example, not fully integrated here as User model uses List<String> tutoredSubjectIds):
    /*
    public Task<DocumentReference> addUserSubjectRelation(@NonNull UserSubject userSubjectRelation) {
        return db.collection(USER_SUBJECTS_COLLECTION).add(userSubjectRelation);
    }

    public Task<QuerySnapshot> getUserSubjectRelations(String userId) {
        return db.collection(USER_SUBJECTS_COLLECTION)
                .whereEqualTo("userId", userId) // Assuming UserSubject POJO has a 'userId' field (Firebase UID)
                .get();
    }

    public Task<QuerySnapshot> getTutoredUserSubjectRelations(String userId) {
        return db.collection(USER_SUBJECTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("tutoring", true) // Assuming UserSubject POJO has a 'tutoring' boolean field
                .get();
    }

    public Task<Void> updateUserSubjectRelation(String userSubjectRelationDocId, Map<String, Object> updates) {
        return db.collection(USER_SUBJECTS_COLLECTION).document(userSubjectRelationDocId).update(updates);
    }

    public Task<Void> deleteUserSubjectRelation(String userSubjectRelationDocId) {
        return db.collection(USER_SUBJECTS_COLLECTION).document(userSubjectRelationDocId).delete();
    }
    */
    // The stubs like `addUserSubject(UserSubject userSubject)` from your original Firebase SubjectDao
    // would need to be implemented if you are using a separate UserSubject collection.
    // Given the User model has `tutoredSubjectIds: List<String>`, managing this list
    // within `UserDao.updateUser` or `UserDao.updateUserSpecificFields` is more direct.
    // For example, to add a subject to a user's tutored list:
    // FieldValue.arrayUnion("subjectIdToAd") in updateUserSpecificFields.
}
