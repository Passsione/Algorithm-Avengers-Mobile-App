package com.pbdvmobile.app.data.dao;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.Schedule.TimeSlot;
import com.pbdvmobile.app.data.SqlOpenHelper;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class SessionDao {
    private final SqlOpenHelper dbHelper;

    // 8:00 in Milliseconds
    public final long OPEN_TIME = hourToMseconds(8) + minutesToMseconds(0);
    // 17:00 in Milliseconds
    public final long CLOSE_TIME = hourToMseconds(18) + minutesToMseconds(0);
    private String lastError = "Session request failed."; // Default error

    private final int SESSION_LIMIT = 3;
    public SessionDao(SqlOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
    public long hourToMseconds(int hour) {return (long) hour * 60 * 60 * 1000;}
    public int mSecondsToHour(long i) {
        return (int) (i / (60 * 60 * 1000));
    }
    public long minutesToMseconds(int minutes) {return (long) minutes * 60 * 1000;}
    public long insertSession(@NonNull Session session) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_SESSION_TUTOR_ID, session.getTutorId());
        values.put(SqlOpenHelper.KEY_SESSION_TUTEE_ID, session.getTuteeId());
        values.put(SqlOpenHelper.KEY_SESSION_SUBJECT_ID, session.getSubjectId());
        values.put(SqlOpenHelper.KEY_SESSION_LOCATION, session.getLocation());
        values.put(SqlOpenHelper.KEY_SESSION_START_TIME, session.getStartTime().getTime());
        values.put(SqlOpenHelper.KEY_SESSION_END_TIME, session.getEndTime().getTime());
        values.put(SqlOpenHelper.KEY_SESSION_STATUS, session.getStatus().name());
        values.put(SqlOpenHelper.KEY_SESSION_TUTOR_REVIEW, session.getTutorReview());
        values.put(SqlOpenHelper.KEY_SESSION_TUTEE_REVIEW, session.getTuteeReview());

        long id = db.insert(SqlOpenHelper.TABLE_SESSIONS, null, values);

        db.close();
        return id;
    }

    public Session getSessionById(int sessionId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_SESSIONS,
                null,
                SqlOpenHelper.KEY_SESSION_ID + "=?",
                new String[]{String.valueOf(sessionId)},
                null, null, null);

        Session session = null;
        if (cursor != null && cursor.moveToFirst()) {
            session = cursorToSession(cursor);
            cursor.close();
        }
        db.close();
        return session;
    }

    public List<Session> getSessionsByTutorId(int tutorId) {
        List<Session> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_SESSIONS,
                null,
                SqlOpenHelper.KEY_SESSION_TUTOR_ID + "=?",
                new String[]{String.valueOf(tutorId)},
                null, null, SqlOpenHelper.KEY_SESSION_START_TIME);

        if (cursor.moveToFirst()) {
            do {
                Session session = cursorToSession(cursor);
                sessions.add(session);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return sessions;
    }
    public List<TimeSlot> getTakenTimeSlot(int tutorId) {
        List<TimeSlot> sessionTimes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = SqlOpenHelper.KEY_SESSION_TUTOR_ID + "=? AND (" +
                SqlOpenHelper.KEY_SESSION_STATUS + " =? OR " +
                SqlOpenHelper.KEY_SESSION_STATUS + " =?)";
        String[] selectionArgs = new String[]{
                String.valueOf(tutorId),
                Session.Status.CONFIRMED.name(),
                Session.Status.PENDING.name()
        };
        Cursor cursor = db.query(SqlOpenHelper.TABLE_SESSIONS,
                null, // columns
                selection,
                selectionArgs,
                null, null, SqlOpenHelper.KEY_SESSION_START_TIME);
        /*Cursor cursor = db.query(SqlOpenHelper.TABLE_SESSIONS,
                null,
                SqlOpenHelper.KEY_SESSION_TUTOR_ID + "=? AND (" + // Filter by tutorId
                SqlOpenHelper.KEY_SESSION_STATUS+ "=Confirmed OR " +
                SqlOpenHelper.KEY_SESSION_STATUS + "=Pending) AND",    // Filter by status (Comfirmed or Pending)
                new String[]{String.valueOf(tutorId)},
                null, null, SqlOpenHelper.KEY_SESSION_START_TIME);
*/
        if (cursor.moveToFirst()) {
            do {
                Session session = cursorToSession(cursor);
                sessionTimes.add(new TimeSlot(session.getStartTime(), session.getEndTime()));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return sessionTimes;
    }


    public List<Session> getSessionsByTuteeId(int tuteeId) {
        List<Session> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_SESSIONS,
                null,
                SqlOpenHelper.KEY_SESSION_TUTEE_ID + "=?",
                new String[]{String.valueOf(tuteeId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Session session = cursorToSession(cursor);
                sessions.add(session);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return sessions;
    }

    public double[] getAverageRatingByStudentNum(int studentNum) {
        double[] ratings = {0.0, 0.0};
        int te = 0;
        int tr = 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        // and we average the KEY_TUTOR_RATING column.
        String avgTutorRatingQuery = "SELECT AVG(" + SqlOpenHelper.KEY_SESSION_TUTOR_RATING + ") FROM " +
                SqlOpenHelper.TABLE_SESSIONS + " WHERE " +
                SqlOpenHelper.KEY_SESSION_TUTEE_ID + " = ?";
        Log.d("DB_QUERY", "Query for avg rating given by tutor: " + avgTutorRatingQuery);

        cursor = db.rawQuery(avgTutorRatingQuery, new String[]{String.valueOf(studentNum)});

        if (cursor.moveToFirst()) {
            // AVG() returns NULL if there are no matching rows or all values are NULL.
            // If it's NULL, getDouble(0) will return 0.0, which is a good default.
            ratings[0] = cursor.getDouble(0);
        }

        String avgTuteeRatingQuery = "SELECT AVG(" + SqlOpenHelper.KEY_SESSION_TUTEE_RATING + ") FROM " +
                SqlOpenHelper.TABLE_SESSIONS + " WHERE " +
                SqlOpenHelper.KEY_SESSION_TUTOR_ID + " = ?";
        Log.d("DB_QUERY", "Query for avg rating given by tutee: " + avgTuteeRatingQuery);


        cursor = db.rawQuery(avgTuteeRatingQuery, new String[]{String.valueOf(studentNum)});

        if (cursor != null && cursor.moveToFirst()) {
            // AVG() returns NULL if there are no matching rows or all values are NULL.
            // If it's NULL, getDouble(0) will return 0.0.
            ratings[1] = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return ratings;
    }

    public void updatePastSessionsToCompleted() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_SESSION_STATUS, Session.Status.COMPLETED.name());

        String whereClause = SqlOpenHelper.KEY_SESSION_END_TIME + " < ? AND " +
                SqlOpenHelper.KEY_SESSION_STATUS + " = ?";
        String[] whereArgs = new String[]{String.valueOf(new Date().getTime()), Session.Status.CONFIRMED.name()};

        db.update(SqlOpenHelper.TABLE_SESSIONS, values, whereClause, whereArgs);
        db.close();
    }
    public boolean requestSession(int tuteeStudentNum, int tutorStudentNum,
                                  TimeSlot requestedActualSlot, // This is now UNPADDED
                                  int subjectId, String location,
                                  Date actualDbStartTime, Date actualDbEndTime) {
/*public boolean requestSession(int tuteeStudentNum, int tutorStudentNum, TimeSlot requestedSlot, int subjectId, String location, Date actualStartTime, Date actualEndTime) {
    // 0. Check if requestedSlot is within OPEN_TIME and CLOSE_TIME (on the specific date)
    // This requires comparing the time part of requestedSlot.getStartTime() and requestedSlot.getEndTime()
    // with OPEN_TIME and CLOSE_TIME.
    Calendar reqStartCal = Calendar.getInstance();
    reqStartCal.setTime(requestedSlot.getStartTime());
    long reqStartTimeOfDayMillis = reqStartCal.get(Calendar.HOUR_OF_DAY) * 3600000L +
            reqStartCal.get(Calendar.MINUTE) * 60000L;

    Calendar reqEndCal = Calendar.getInstance();
    reqEndCal.setTime(requestedSlot.getEndTime());
    long reqEndTimeOfDayMillis = reqEndCal.get(Calendar.HOUR_OF_DAY) * 3600000L +
            reqEndCal.get(Calendar.MINUTE) * 60000L;
    if (reqEndCal.get(Calendar.MINUTE) == 0 && reqEndCal.get(Calendar.HOUR_OF_DAY) == 0) { // Midnight edge case
        reqEndTimeOfDayMillis = 24 * 3600000L;
    }


    if (reqStartTimeOfDayMillis < OPEN_TIME || reqEndTimeOfDayMillis > CLOSE_TIME) {
        Log.e("SessionDao", "Requested time is outside service hours.");
        return false; // Outside service hours
    }


    // 1. Check for conflicts with tutor's schedule
    List<TimeSlot> tutorTakenSlots = getTakenTimeSlot(tutorStudentNum); // Ensure this uses the corrected query
    for (TimeSlot taken : tutorTakenSlots) {
        if (requestedSlot.overlaps(taken)) {
            Log.e("SessionDao", "Requested time overlaps with tutor's schedule.");
            return false; // Conflict with tutor
        }
    }

    // 2. Check for conflicts with tutee's schedule
    // You'll need a method like getTakenTimeSlot but for any role (tutor or tutee)
    // or call getSessionsByTuteeId and getSessionsByTutorId for the tutee.
    List<TimeSlot> tuteeTakenSlotsAsTutee = getTakenTimeSlotForUser(tuteeStudentNum, true); // true for tutee role
    List<TimeSlot> tuteeTakenSlotsAsTutor = getTakenTimeSlotForUser(tuteeStudentNum, false); // false for tutor role

    for (TimeSlot taken : tuteeTakenSlotsAsTutee) {
        if (requestedSlot.overlaps(taken)) {
            Log.e("SessionDao", "Requested time overlaps with tutee's schedule (as tutee).");
            return false; // Conflict with tutee as tutee
        }
    }
    for (TimeSlot taken : tuteeTakenSlotsAsTutor) {
        if (requestedSlot.overlaps(taken)) {
            Log.e("SessionDao", "Requested time overlaps with tutee's schedule (as tutor).");
            return false; // Conflict with tutee as tutor
        }
    }


    // 3. Check for duplicate pending/confirmed sessions (same tutor, tutee, subject before old one passed)
    List<Session> existingSessions = getSessionsByUsersAndSubject(tuteeStudentNum, tutorStudentNum, subjectId);
    Date now = new Date();
    for (Session existing : existingSessions) {
        if ((existing.getStatus() == Session.Status.PENDING || existing.getStatus() == Session.Status.CONFIRMED) &&
                existing.getEndTime().after(now)) {
            Log.e("SessionDao", "Duplicate active session already exists.");
            return false; // A duplicate, active session already exists
        }
    }


    // If all checks pass, create and insert the session
    Session session = new Session(tutorStudentNum, tuteeStudentNum, subjectId);
    session.setStartTime(actualStartTime); // Store UNPADDED time
    session.setEndTime(actualEndTime);   // Store UNPADDED time
    session.setStatus(Session.Status.PENDING);
    session.setLocation(location);

    return insertSession(session) != -1;*/

    // 0. Check if requested ACTUAL slot is within OPEN_TIME and CLOSE_TIME
    Calendar reqStartCal = Calendar.getInstance();
    reqStartCal.setTime(requestedActualSlot.getStartTime()); // Use unpadded start
    long reqStartTimeOfDayMillis = reqStartCal.get(Calendar.HOUR_OF_DAY) * 3600000L +
            reqStartCal.get(Calendar.MINUTE) * 60000L;

    Calendar reqEndCal = Calendar.getInstance();
    reqEndCal.setTime(requestedActualSlot.getEndTime()); // Use unpadded end
    long reqEndTimeOfDayMillis = reqEndCal.get(Calendar.HOUR_OF_DAY) * 3600000L +
            reqEndCal.get(Calendar.MINUTE) * 60000L;

    // Handle midnight correctly for end time (if a session ends exactly at midnight)
    if (reqEndCal.get(Calendar.HOUR_OF_DAY) == 0 && reqEndCal.get(Calendar.MINUTE) == 0) {
        // If selected date is same as start date, means it's end of the day.
        if (reqEndCal.get(Calendar.DAY_OF_YEAR) == reqStartCal.get(Calendar.DAY_OF_YEAR) &&
                reqEndCal.get(Calendar.YEAR) == reqStartCal.get(Calendar.YEAR) ){
            reqEndTimeOfDayMillis = CLOSE_TIME; // Treat as service close time
        }
        // If it rolls over to next day, and it's 00:00, then reqEndTimeOfDayMillis is fine as 0.
    }


    if (reqStartTimeOfDayMillis < OPEN_TIME || reqEndTimeOfDayMillis > CLOSE_TIME || reqStartTimeOfDayMillis >= reqEndTimeOfDayMillis) {
        Log.e("SessionDao", "Requested time " + actualDbStartTime + " to " + actualDbEndTime +
                " (offsets: " + reqStartTimeOfDayMillis + "-" + reqEndTimeOfDayMillis +
                ") is outside service hours (" + OPEN_TIME + "-" + CLOSE_TIME + ") or invalid duration.");
        lastError = "Requested time is outside service hours or duration is invalid.";
        return false;
    }

    // 1. Check for conflicts with tutor's schedule
    // getTakenTimeSlot should return TimeSlots representing actual booked times (unpadded)
    List<TimeSlot> tutorTakenSlots = getTakenTimeSlot(tutorStudentNum);
    for (TimeSlot taken : tutorTakenSlots) {
        if (requestedActualSlot.overlaps(taken)) {
            Log.e("SessionDao", "Requested time overlaps with tutor's schedule: " + taken);
            lastError = "This time slot conflicts with the tutor's schedule.";
            return false;
        }
    }

    // 2. Check for conflicts with tutee's schedule
    List<TimeSlot> tuteeTakenSlotsAsTutee = getTakenTimeSlotForUser(tuteeStudentNum, true);
    for (TimeSlot taken : tuteeTakenSlotsAsTutee) {
        if (requestedActualSlot.overlaps(taken)) {
            Log.e("SessionDao", "Requested time overlaps with your schedule (as tutee): " + taken);
            lastError = "This time slot conflicts with your existing schedule.";
            return false;
        }
    }
    List<TimeSlot> tuteeTakenSlotsAsTutor = getTakenTimeSlotForUser(tuteeStudentNum, false);
    for (TimeSlot taken : tuteeTakenSlotsAsTutor) {
        if (requestedActualSlot.overlaps(taken)) {
            Log.e("SessionDao", "Requested time overlaps with your schedule (as tutor): " + taken);
            lastError = "This time slot conflicts with your existing schedule as a tutor.";
            return false;
        }
    }

    // 3. Check for duplicate pending/confirmed sessions
    List<com.pbdvmobile.app.data.model.Session> existingSessions = getSessionsByUsersAndSubject(tuteeStudentNum, tutorStudentNum, subjectId);
    Date now = new Date();
    for (com.pbdvmobile.app.data.model.Session existing : existingSessions) {
        if ((existing.getStatus() == com.pbdvmobile.app.data.model.Session.Status.PENDING ||
                existing.getStatus() == com.pbdvmobile.app.data.model.Session.Status.CONFIRMED) &&
                existing.getEndTime().after(now)) {
            // Check if the times actually overlap for this specific check
            TimeSlot existingSlot = new TimeSlot(existing.getStartTime(), existing.getEndTime());
            if (requestedActualSlot.overlaps(existingSlot)) { // More precise check
                Log.e("SessionDao", "An active or pending session for this subject with this tutor already exists at a conflicting time.");
                lastError = "An active or pending session for this subject and tutor conflicts with this time.";
                return false;
            }
        }
    }

    Session session = new Session(tutorStudentNum, tuteeStudentNum, subjectId);
    session.setStartTime(actualDbStartTime);
    session.setEndTime(actualDbEndTime);
    session.setStatus(com.pbdvmobile.app.data.model.Session.Status.PENDING);
    session.setLocation(location);

    if (insertSession(session) != -1) {
        lastError = "Session request created successfully!"; // Should not be needed if success
        return true;
    } else {
        lastError = "Failed to save session to database.";
        return false;
    }
}

    public String getLastError() {
        return lastError;
    }
    // Helper method in SessionDao to get taken slots for a user (either as tutor or tutee)
    // Or more simply, a method that returns all PENDING/CONFIRMED sessions for a user.
    public List<TimeSlot> getTakenTimeSlotForUser(int userId, boolean asTutee) {
        List<TimeSlot> sessionTimes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String userColumn = asTutee ? SqlOpenHelper.KEY_SESSION_TUTEE_ID : SqlOpenHelper.KEY_SESSION_TUTOR_ID;

        String selection = userColumn + " = ? AND (" +
                SqlOpenHelper.KEY_SESSION_STATUS + " = ? OR " +
                SqlOpenHelper.KEY_SESSION_STATUS + " = ?)";
        String[] selectionArgs = new String[]{
                String.valueOf(userId),
                Session.Status.CONFIRMED.name(),
                Session.Status.PENDING.name()
        };

        Cursor cursor = db.query(SqlOpenHelper.TABLE_SESSIONS,
                null, selection, selectionArgs, null, null, SqlOpenHelper.KEY_SESSION_START_TIME);

        if (cursor.moveToFirst()) {
            do {
                Session session = cursorToSession(cursor);
                sessionTimes.add(new TimeSlot(session.getStartTime(), session.getEndTime()));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return sessionTimes;
    }

    // Helper method in SessionDao
    public List<Session> getSessionsByUsersAndSubject(int tuteeId, int tutorId, int subjectId) {
        List<Session> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = SqlOpenHelper.KEY_SESSION_TUTEE_ID + " = ? AND " +
                SqlOpenHelper.KEY_SESSION_TUTOR_ID + " = ? AND " +
                SqlOpenHelper.KEY_SESSION_SUBJECT_ID + " = ?";
        String[] selectionArgs = new String[]{
                String.valueOf(tuteeId),
                String.valueOf(tutorId),
                String.valueOf(subjectId)
        };
        Cursor cursor = db.query(SqlOpenHelper.TABLE_SESSIONS, null, selection, selectionArgs, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                sessions.add(cursorToSession(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return sessions;
    }
    public int updateSessionStatus(int sessionId, @NonNull Session.Status status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_SESSION_STATUS, status.name());

        int rowsAffected = db.update(SqlOpenHelper.TABLE_SESSIONS, values,
                SqlOpenHelper.KEY_SESSION_ID + "=?",
                new String[]{String.valueOf(sessionId)});
        db.close();
        return rowsAffected;
    }

    public int addTutorReview(int sessionId, String review) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_SESSION_TUTOR_REVIEW, review);

        int rowsAffected = db.update(SqlOpenHelper.TABLE_SESSIONS, values,
                SqlOpenHelper.KEY_SESSION_ID + "=?",
                new String[]{String.valueOf(sessionId)});
        db.close();
        return rowsAffected;
    }
    public int updateTutorRating(int sessionId, double rating) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_SESSION_TUTOR_RATING, rating); // Average the rating

        int rowsAffected = db.update(SqlOpenHelper.TABLE_SESSIONS, values,
                SqlOpenHelper.KEY_SESSION_ID + "=?",
                new String[]{String.valueOf(sessionId)});
        db.close();
        return rowsAffected;
    }
    public int addTuteeReview(int sessionId, String review) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_SESSION_TUTEE_REVIEW, review);

        int rowsAffected = db.update(SqlOpenHelper.TABLE_SESSIONS, values,
                SqlOpenHelper.KEY_SESSION_ID + "=?",
                new String[]{String.valueOf(sessionId)});
        db.close();
        return rowsAffected;
    }
    public int updateTuteeRating(int sessionId, double rating) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_SESSION_TUTEE_RATING, rating); // Average the rating

        int rowsAffected = db.update(SqlOpenHelper.TABLE_SESSIONS, values,
                SqlOpenHelper.KEY_SESSION_ID + "=?",
                new String[]{String.valueOf(sessionId)});
        db.close();
        return rowsAffected;
    }


    public int deleteSession(int sessionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete(SqlOpenHelper.TABLE_SESSIONS,
                SqlOpenHelper.KEY_SESSION_ID + "=?",
                new String[]{String.valueOf(sessionId)});
        db.close();
        return rowsAffected;
    }

    private Session cursorToSession(Cursor cursor) {
        Session session = new Session();
        session.setId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SESSION_ID)));
        session.setTutorId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SESSION_TUTOR_ID)));
        session.setTuteeId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SESSION_TUTEE_ID)));
        session.setSubjectId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SESSION_SUBJECT_ID)));
        session.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SESSION_LOCATION)));

        long startTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SESSION_START_TIME));
        session.setStartTime(new Date(startTimeMillis));

        long endTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SESSION_END_TIME));
        session.setEndTime(new Date(endTimeMillis));

        String statusString = cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SESSION_STATUS));
        session.setStatus(Session.Status.valueOf(statusString));

        session.setTutorReview(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SESSION_TUTOR_REVIEW)));
        session.setTuteeReview(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SESSION_TUTEE_REVIEW)));

        return session;
    }

}
