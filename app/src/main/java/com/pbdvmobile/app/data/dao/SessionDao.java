package com.pbdvmobile.app.data.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.pbdvmobile.app.data.Schedule.TimeSlot; // Assuming TimeSlot is updated or compatible
import com.pbdvmobile.app.data.model.Session;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;


public class SessionDao {
    private static final String TAG = "SessionDaoFirebase";
    public static final String COLLECTION_NAME = "sessions";
    private static final long OPEN_TIME_HOUR = 8; // 8 AM
    private static final long CLOSE_TIME_HOUR = 18; // 6 PM (exclusive for end time, so sessions can end at 18:00)

    private final FirebaseFirestore db;

    public SessionDao() {
        this.db = FirebaseFirestore.getInstance();
    }

    // Callback for requestSession result
    public interface SessionRequestListener {
        void onSuccess(DocumentReference documentReference);
        void onFailure(String errorMessage, Exception e);
    }

    // Callback for fetching ratings
    public interface RatingsCallback {
        void onRatingsFetched(double averageRatingAsTutee, double averageRatingAsTutor);
        void onError(Exception e);
    }

    // Callback for fetching list of sessions
    public interface SessionsCallback {
        void onSessionsFetched(List<Session> sessions);
        void onError(Exception e);
    }


    private long getMillisForHour(int hour) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // We only care about the time part for OPEN/CLOSE, date doesn't matter for this constant
        return cal.get(Calendar.HOUR_OF_DAY) * 3600000L + cal.get(Calendar.MINUTE) * 60000L;
    }


    public void getAverageRatingByFirebaseUid(String userFirebaseUid, @NonNull RatingsCallback callback) {
        final double[] ratings = {0.0, 0.0}; // [0] = as tutee, [1] = as tutor
        final int[] completedTasks = {0};
        final int TOTAL_TASKS = 2;

        // Rating as Tutee (ratings given by tutors TO this user)
        db.collection(COLLECTION_NAME)
                .whereEqualTo("tuteeUid", userFirebaseUid)
                .whereGreaterThan("tutorRating", 0) // Sessions where the tutor rated this tutee
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalSum = 0;
                    int count = 0;
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Session session = doc.toObject(Session.class);
                            if (session != null && session.getTutorRating() != null && session.getTutorRating() > 0) {
                                totalSum += session.getTutorRating();
                                count++;
                            }
                        }
                    }
                    ratings[0] = (count > 0) ? totalSum / count : 0.0;
                    completedTasks[0]++;
                    if (completedTasks[0] == TOTAL_TASKS) {
                        callback.onRatingsFetched(ratings[0], ratings[1]);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching ratings as tutee for UID: " + userFirebaseUid, e);
                    completedTasks[0]++;
                    if (completedTasks[0] == TOTAL_TASKS) { // If both failed or this was the second
                        callback.onRatingsFetched(ratings[0], ratings[1]); // Report what we have
                    } else if (completedTasks[0] == TOTAL_TASKS -1 && ratings[1] != 0.0) {
                        // This was the first to fail, the other succeeded
                        callback.onRatingsFetched(ratings[0], ratings[1]);
                    } else if (completedTasks[0] == TOTAL_TASKS -1 && ratings[1] == 0.0) {
                        // This was the first to fail, waiting for the other.
                    } else { // This was the only task and it failed, or both failed.
                        callback.onError(e);
                    }
                });

        // Rating as Tutor (ratings given by tutees TO this user)
        db.collection(COLLECTION_NAME)
                .whereEqualTo("tutorUid", userFirebaseUid)
                .whereGreaterThan("tuteeRating", 0) // Sessions where the tutee rated this tutor
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalSum = 0;
                    int count = 0;
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Session session = doc.toObject(Session.class);
                            if (session != null && session.getTuteeRating() != null && session.getTuteeRating() > 0) {
                                totalSum += session.getTuteeRating();
                                count++;
                            }
                        }
                    }
                    ratings[1] = (count > 0) ? totalSum / count : 0.0;
                    completedTasks[0]++;
                    if (completedTasks[0] == TOTAL_TASKS) {
                        callback.onRatingsFetched(ratings[0], ratings[1]);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching ratings as tutor for UID: " + userFirebaseUid, e);
                    completedTasks[0]++;
                    if (completedTasks[0] == TOTAL_TASKS) { // If both failed or this was the second
                        callback.onRatingsFetched(ratings[0], ratings[1]); // Report what we have
                    } else if (completedTasks[0] == TOTAL_TASKS -1 && ratings[0] != 0.0) {
                        // This was the first to fail, the other succeeded
                        callback.onRatingsFetched(ratings[0], ratings[1]);
                    } else if (completedTasks[0] == TOTAL_TASKS -1 && ratings[0] == 0.0) {
                        // This was the first to fail, waiting for the other.
                    } else { // This was the only task and it failed, or both failed.
                        callback.onError(e);
                    }
                });
    }


    public void requestSession(@NonNull String tuteeUserUid,
                               @NonNull String tutorUserUid,
                               @NonNull String subjectDocId, // Firestore doc ID for subject
                               @NonNull String subjectName, // Denormalized subject name
                               @NonNull String location,
                               @NonNull Date proposedStartTimeJavaUtil, // java.util.Date from UI
                               @NonNull Date proposedEndTimeJavaUtil,   // java.util.Date from UI
                               @NonNull SessionRequestListener callback) {

        Timestamp proposedStartTimeFs = new Timestamp(proposedStartTimeJavaUtil);
        Timestamp proposedEndTimeFs = new Timestamp(proposedEndTimeJavaUtil);

        // --- 1. Validate against Business Hours ---
        Calendar reqStartCal = Calendar.getInstance();
        reqStartCal.setTime(proposedStartTimeJavaUtil);
        int reqStartHour = reqStartCal.get(Calendar.HOUR_OF_DAY);

        Calendar reqEndCal = Calendar.getInstance();
        reqEndCal.setTime(proposedEndTimeJavaUtil);
        int reqEndHour = reqEndCal.get(Calendar.HOUR_OF_DAY);
        int reqEndMinute = reqEndCal.get(Calendar.MINUTE);

        // Session must start on or after OPEN_TIME_HOUR
        // Session must end by CLOSE_TIME_HOUR:00 (e.g., if CLOSE_TIME_HOUR is 18, it can end at 18:00, not start at 18:00)
        if (reqStartHour < OPEN_TIME_HOUR ||
                reqEndHour > CLOSE_TIME_HOUR ||
                (reqEndHour == CLOSE_TIME_HOUR && reqEndMinute > 0) ||
                proposedStartTimeFs.compareTo(proposedEndTimeFs) >= 0) { // Start must be before end

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String openTimeStr = String.format(Locale.getDefault(), "%02d:00", OPEN_TIME_HOUR);
            String closeTimeStr = String.format(Locale.getDefault(), "%02d:00", CLOSE_TIME_HOUR);

            String businessHoursError = "Requested time is outside service hours (" +
                    openTimeStr + " - " + closeTimeStr + ") or duration is invalid.";
            Log.e(TAG, businessHoursError + " Start: " + reqStartHour + ", End: " + reqEndHour + ":" + reqEndMinute);
            callback.onFailure(businessHoursError, null);
            return;
        }

        // --- 2. Check Tutor's Availability ---
        // Query for sessions that could overlap:
        // existing.startTime < proposed.endTime AND existing.endTime > proposed.startTime
        Query tutorAvailabilityQuery = db.collection(COLLECTION_NAME)
                .whereEqualTo("tutorUid", tutorUserUid)
                .whereIn("status", Arrays.asList(Session.Status.PENDING.name(), Session.Status.CONFIRMED.name()))
                .whereLessThan("startTime", proposedEndTimeFs); // existing.startTime < proposed.endTime

        tutorAvailabilityQuery.get().addOnSuccessListener(tutorSessionsSnapshot -> {
            for (DocumentSnapshot doc : tutorSessionsSnapshot) {
                Session existingSession = doc.toObject(Session.class);
                if (existingSession != null && existingSession.getEndTime() != null &&
                        existingSession.getEndTime().compareTo(proposedStartTimeFs) > 0) { // existing.endTime > proposed.startTime
                    Log.w(TAG, "Tutor Conflict: Proposed session overlaps with existing session ID: " + doc.getId());
                    callback.onFailure("Tutor is unavailable at the selected time.", null);
                    return; // Exit early on first conflict
                }
            }
            Log.d(TAG, "Tutor availability check passed.");

            // --- 3. Check Tutee's Availability ---
            Query tuteeAvailabilityQuery = db.collection(COLLECTION_NAME)
                    .whereEqualTo("tuteeUid", tuteeUserUid)
                    .whereIn("status", Arrays.asList(Session.Status.PENDING.name(), Session.Status.CONFIRMED.name()))
                    .whereLessThan("startTime", proposedEndTimeFs);

            tuteeAvailabilityQuery.get().addOnSuccessListener(tuteeSessionsSnapshot -> {
                for (DocumentSnapshot doc : tuteeSessionsSnapshot) {
                    Session existingSession = doc.toObject(Session.class);
                    if (existingSession != null && existingSession.getEndTime() != null &&
                            existingSession.getEndTime().compareTo(proposedStartTimeFs) > 0) {
                        Log.w(TAG, "Tutee Conflict: Proposed session overlaps with tutee's existing session ID: " + doc.getId());
                        callback.onFailure("You are unavailable at the selected time due to another session.", null);
                        return;
                    }
                }
                Log.d(TAG, "Tutee availability check passed.");

                // --- 4. Check for Duplicate Pending/Confirmed Sessions for this specific pair/subject (optional, stricter rule) ---
                // This prevents multiple PENDING requests for the exact same tutor/tutee/subject if desired.
                // For simplicity, we might skip this if the time conflict check is sufficient.
                // If implementing, query for sessions with same tutorUid, tuteeUid, subjectId, and PENDING/CONFIRMED status.

                // --- 5. All checks passed, create the session ---
                Session newSession = new Session();
                newSession.setTutorUid(tutorUserUid);
                newSession.setTuteeUid(tuteeUserUid);
                newSession.setSubjectId(subjectDocId);
                newSession.setSubjectName(subjectName); // Store denormalized name
                newSession.setLocation(location);
                newSession.setStartTime(proposedStartTimeFs);
                newSession.setEndTime(proposedEndTimeFs);
                newSession.setStatus(Session.Status.PENDING);
                newSession.setCreatedAt(new Timestamp(new Date())); // Record creation time

                insertSession(newSession).addOnSuccessListener(documentReference -> {
                    Log.i(TAG, "Session request successful. New session ID: " + documentReference.getId());
                    callback.onSuccess(documentReference);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to insert new session into Firestore.", e);
                    callback.onFailure("Failed to create session in database.", e);
                });

            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error checking tutee availability.", e);
                callback.onFailure("Could not verify your availability.", e);
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking tutor availability.", e);
            callback.onFailure("Could not verify tutor's availability.", e);
        });
    }


    public Task<DocumentReference> insertSession(@NonNull Session session) {
        return db.collection(COLLECTION_NAME).add(session);
    }

    public Task<DocumentSnapshot> getSessionById(String sessionId) {
        return db.collection(COLLECTION_NAME).document(sessionId).get();
    }

    public void getSessionsByTutorUid(String tutorUid, @NonNull SessionsCallback callback) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("tutorUid", tutorUid)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Session> sessions = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Session session = doc.toObject(Session.class);
                            if (session != null) {
                                session.setId(doc.getId());
                                sessions.add(session);
                            }
                        }
                    }
                    callback.onSessionsFetched(sessions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching sessions for tutor UID: " + tutorUid, e);
                    callback.onError(e);
                });
    }

    public void getSessionsByTuteeUid(String tuteeUid, @NonNull SessionsCallback callback) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("tuteeUid", tuteeUid)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Session> sessions = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Session session = doc.toObject(Session.class);
                            if (session != null) {
                                session.setId(doc.getId());
                                sessions.add(session);
                            }
                        }
                    }
                    callback.onSessionsFetched(sessions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching sessions for tutee UID: " + tuteeUid, e);
                    callback.onError(e);
                });
    }


    /**
     * Fetches sessions for a user (either as tutor or tutee) that are PENDING or CONFIRMED.
     * This is useful for checking for conflicts or displaying active bookings.
     * @param userUid The UID of the user.
     * @param callback Callback to handle the list of TimeSlot objects or an error.
     */
    public void getActiveTimeSlotsForUser(String userUid, @NonNull final TimeSlotsCallback callback) {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        // Sessions where user is tutor
        tasks.add(db.collection(COLLECTION_NAME)
                .whereEqualTo("tutorUid", userUid)
                .whereIn("status", Arrays.asList(Session.Status.PENDING.name(), Session.Status.CONFIRMED.name()))
                .get());

        // Sessions where user is tutee
        tasks.add(db.collection(COLLECTION_NAME)
                .whereEqualTo("tuteeUid", userUid)
                .whereIn("status", Arrays.asList(Session.Status.PENDING.name(), Session.Status.CONFIRMED.name()))
                .get());

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(list -> {
            List<TimeSlot> timeSlots = new ArrayList<>();
            for (Object snapshotObject : list) {
                QuerySnapshot snapshot = (QuerySnapshot) snapshotObject;
                if (snapshot != null) {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Session session = doc.toObject(Session.class);
                        if (session != null && session.getStartTimeAsDate() != null && session.getEndTimeAsDate() != null) {
                            // Assuming TimeSlot constructor takes java.util.Date
                            timeSlots.add(new TimeSlot(session.getStartTimeAsDate(), session.getEndTimeAsDate()));
                        }
                    }
                }
            }
            callback.onTimeSlotsFetched(timeSlots);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching active time slots for user: " + userUid, e);
            callback.onError(e);
        });
    }

    public interface TimeSlotsCallback {
        void onTimeSlotsFetched(List<TimeSlot> timeSlots);
        void onError(Exception e);
    }


    public Task<Void> updateSessionStatus(String sessionId, Session.Status status) {
        return db.collection(COLLECTION_NAME).document(sessionId).update("status", status.name());
    }

    public Task<Void> addTutorReview(String sessionId, String review, double rating) {
        return db.collection(COLLECTION_NAME).document(sessionId)
                .update("tuteeReview", review, "tuteeRating", rating); // Tutee reviews the Tutor
    }

    public Task<Void> addTuteeReview(String sessionId, String review, double rating) {
        return db.collection(COLLECTION_NAME).document(sessionId)
                .update("tutorReview", review, "tutorRating", rating); // Tutor reviews the Tutee
    }

    public Task<Void> deleteSession(String sessionId) {
        return db.collection(COLLECTION_NAME).document(sessionId).delete();
    }

    /**
     * Marks past PENDING or CONFIRMED sessions as COMPLETED or DECLINED respectively.
     * Typically run periodically or on app start.
     * @param userUid The UID of the user to update sessions for (can be null to update all).
     * @param roleField "tutorUid" or "tuteeUid" if specific to a user, or null for system-wide.
     */
    public Task<Void> updatePastSessions(@Nullable String userUid, @Nullable String roleField) {
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();

        // Query for PENDING sessions that have ended -> mark DECLINED (missed)
        Query pendingQuery = db.collection(COLLECTION_NAME)
                .whereEqualTo("status", Session.Status.PENDING.name())
                .whereLessThan("endTime", now);
        if (userUid != null && roleField != null) {
            pendingQuery = pendingQuery.whereEqualTo(roleField, userUid);
        }

        Task<QuerySnapshot> pendingTask = pendingQuery.get().addOnSuccessListener(snapshots -> {
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots) {
                    batch.update(doc.getReference(), "status", Session.Status.DECLINED.name());
                }
            }
        });

        // Query for CONFIRMED sessions that have ended -> mark COMPLETED
        Query confirmedQuery = db.collection(COLLECTION_NAME)
                .whereEqualTo("status", Session.Status.CONFIRMED.name())
                .whereLessThan("endTime", now);
        if (userUid != null && roleField != null) {
            confirmedQuery = confirmedQuery.whereEqualTo(roleField, userUid);
        }
        Task<QuerySnapshot> confirmedTask = confirmedQuery.get().addOnSuccessListener(snapshots -> {
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots) {
                    batch.update(doc.getReference(), "status", Session.Status.COMPLETED.name());
                }
            }
        });

        return Tasks.whenAll(pendingTask, confirmedTask).continueWithTask(task -> {
            if (task.isSuccessful()) {
                return batch.commit();
            } else {
                Log.e(TAG, "Failed to query past sessions for update.", task.getException());
                return Tasks.forException(task.getException());
            }
        });
    }
}
