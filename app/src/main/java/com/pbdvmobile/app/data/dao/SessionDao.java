package com.pbdvmobile.app.data.dao;
import static java.lang.Character.getType;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.ListJsonConverter;
import com.pbdvmobile.app.data.Schedule.TimeSlot;
import com.pbdvmobile.app.data.SqlOpenHelper;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class SessionDao {
    private final SqlOpenHelper dbHelper;

    // 8:00 in Milliseconds
    public final long OPEN_TIME = hourToMseconds(8) + minutesToMseconds(0);
    // 17:00 in Milliseconds
    public final long CLOSE_TIME = hourToMseconds(17) + minutesToMseconds(0);
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
        values.put(SqlOpenHelper.KEY_SESSION_TUTEE_IDS_JSON, ListJsonConverter.listToJson(session.getTuteeIds()));
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

        Cursor cursor = db.query(SqlOpenHelper.TABLE_SESSIONS,
                null,
                SqlOpenHelper.KEY_SESSION_TUTOR_ID + "=? AND " + // Filter by tutorId
                SqlOpenHelper.KEY_SESSION_STATUS+ " IN ( 'Confirmed', 'Pending')",    // Filter by status (Comfirmed or Pending)
                new String[]{String.valueOf(tutorId)},
                null, null, SqlOpenHelper.KEY_SESSION_START_TIME);

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

        Cursor sessionCursor = db.query(SqlOpenHelper.TABLE_SESSIONS,
                null,
                null,
                null,
                null, null, null);

        if (sessionCursor.moveToFirst()) {
            do {
                Session session = cursorToSession(sessionCursor);
                if(session.getTuteeIds().contains(tuteeId))
                    sessions.add(session);
            } while (sessionCursor.moveToNext());
        }
        sessionCursor.close();
        db.close();
        return sessions;
    }

    public boolean hasActiveSession(int tuteeId, int tutorId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        boolean hasActive = false;

        try {
            String[] columns = {SqlOpenHelper.KEY_SESSION_ID}; // Only need to check existence
            String selection = SqlOpenHelper.KEY_SESSION_TUTOR_ID + "=? AND (" +
                    SqlOpenHelper.KEY_SESSION_STATUS + "=? OR " +
                    SqlOpenHelper.KEY_SESSION_STATUS + "=?)";
            String[] selectionArgs = {
                    String.valueOf(tuteeId),
                    String.valueOf(tutorId),
                    Session.Status.PENDING.name(),
                    Session.Status.CONFIRMED.name()
            };

            cursor = db.query(SqlOpenHelper.TABLE_SESSIONS,
                    columns,
                    selection,
                    selectionArgs,
                    null, null, null, "1"); // LIMIT 1 optimization

            if (cursor != null && cursor.getCount() > 0) {
                do {
                    Session session = cursorToSession(cursor);
                    if(session.getTuteeIds().contains(tuteeId))
                        hasActive = true; // Found at least one active session
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("SessionDao", "Error checking for active session between " + tuteeId + " and " + tutorId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        Log.d("SessionDao", "Check Active Session: Tutee " + tuteeId + ", Tutor " + tutorId + " -> HasActive: " + hasActive);
        return hasActive;
    }
    // Add this helper method inside SessionDao.java
    /**
     * Converts a Date object (assumed to be in the device's local timezone)
     * into milliseconds representing the time of day in UTC (based on Jan 1, 1970).
     * @param localDate The date/time in the local timezone.
     * @return Milliseconds representing the time of day in UTC.
     */
    private long getUtcTimeOfDayMillis(Date localDate) {
        if (localDate == null) return 0; // Handle null case

        Calendar localCal = Calendar.getInstance(); // Uses default (local) timezone
        localCal.setTime(localDate);

        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // Set UTC calendar to Jan 1 1970, but use the hour/minute extracted from the local date
        utcCal.set(Calendar.YEAR, 1970);
        utcCal.set(Calendar.MONTH, Calendar.JANUARY);
        utcCal.set(Calendar.DAY_OF_MONTH, 1);
        utcCal.set(Calendar.HOUR_OF_DAY, localCal.get(Calendar.HOUR_OF_DAY)); // Use hour from local time
        utcCal.set(Calendar.MINUTE, localCal.get(Calendar.MINUTE));       // Use minute from local time
        utcCal.set(Calendar.SECOND, 0); // Standardize seconds/ms
        utcCal.set(Calendar.MILLISECOND, 0);

        return utcCal.getTimeInMillis();
    }


// Modify your existing requestSession method in SessionDao.java
    /**
     * Attempts to create a new session request.
     * Performs final validation checks for operating hours and availability.
     * @param tuteeId       The ID of the tutee requesting the session.
     * @param tutorId       The ID of the tutor for the session.
     * @param requestedSlot The requested time slot (with local start/end times).
     * @param subjectId     The ID of the subject for the session.
     * @param location      The location for the session.
     * @return True if the session request was successfully created (Status.PENDING), false otherwise.
     */
    public boolean requestSession(int tuteeId, int tutorId, TimeSlot requestedSlot, int subjectId, String location) {
        Log.d("SessionDao", "requestSession called for tutor " + tutorId + " by tutee " + tuteeId);
        Log.d("SessionDao", "Requested Slot (Local): " + requestedSlot.getStartTime() + " - " + requestedSlot.getEndTime());

        // --- Validation 1: Check against Operating Hours (using UTC time-of-day values) ---
        long requestedStartUtcValue = getUtcTimeOfDayMillis(requestedSlot.getStartTime());
        long requestedEndUtcValue = getUtcTimeOfDayMillis(requestedSlot.getEndTime());
        // Handle case where end time wraps past midnight UTC for calculation (rare but possible)
        if (requestedEndUtcValue < requestedStartUtcValue) {
            requestedEndUtcValue += TimeUnit.DAYS.toMillis(1); // Add 24 hours in millis
        }
        long requestedDuration = requestedSlot.getEndTime().getTime() - requestedSlot.getStartTime().getTime();

        // Log the comparison values
        Log.d("SessionDao", "Comparing Req Start UTC Value: " + requestedStartUtcValue + " against OPEN_TIME: " + OPEN_TIME);
        Log.d("SessionDao", "Comparing Req Start UTC Value: " + requestedStartUtcValue + " against CLOSE_TIME: " + CLOSE_TIME);
        // Check if start time is strictly before OPEN_TIME OR start time is at or after CLOSE_TIME
        // (A slot starting exactly at CLOSE_TIME is invalid)
        if (requestedStartUtcValue < OPEN_TIME || requestedStartUtcValue >= CLOSE_TIME) {
            Log.w("SessionDao", "Booking rejected: Start time " + requestedSlot.getStartTime() +
                    " (UTC value " + requestedStartUtcValue + ") is outside UTC operating hours [" +
                    OPEN_TIME + ", " + CLOSE_TIME + ").");
            return false; // Start time is outside operating hours
        }

        // Check if the calculated END time value goes beyond the CLOSE_TIME
        // Allow ending exactly AT CLOSE_TIME. Example: OPEN=8, CLOSE=17, a 15:00-17:00 slot has end value matching CLOSE_TIME and is valid.
        // The start value 15:00 is >= OPEN_TIME and < CLOSE_TIME.
        // The end value derived from start + duration must be <= CLOSE_TIME.
        long calculatedEndUtcValue = requestedStartUtcValue + requestedDuration;
        Log.d("SessionDao", "Comparing Calculated End UTC Value: " + calculatedEndUtcValue + " against CLOSE_TIME: " + CLOSE_TIME);

        if (calculatedEndUtcValue > CLOSE_TIME) {
            Log.w("SessionDao", "Booking rejected: End time " + requestedSlot.getEndTime() +
                    " (calculated UTC value " + calculatedEndUtcValue + ") is after UTC close time (" + CLOSE_TIME + ").");
            return false;
        }
        Log.d("SessionDao", "Operating hours check passed.");


        // --- Validation 2: Double-check Availability (Avoid Race Conditions) ---
        List<Session> currentSessions = getSessionsByTutorIdAndStatus(tutorId,
                new Session.Status[]{Session.Status.CONFIRMED, Session.Status.PENDING});
        boolean conflictFound = false;
        for (Session existingSession : currentSessions) {
            TimeSlot existingSlot = new TimeSlot(existingSession.getStartTime(), existingSession.getEndTime());
            if (requestedSlot.overlaps(existingSlot)) {
                Log.w("SessionDao", "Booking rejected: Requested slot " + requestedSlot.getStartTime() +
                        " conflicts with existing session " + existingSlot.getStartTime());
                conflictFound = true;
                break; // Exit loop once a conflict is found
            }
        }
        if (conflictFound) {
            return false; // Slot conflict detected
        }
        Log.d("SessionDao", "Availability check passed (no conflicts found).");

        // --- If all checks pass, proceed to insert the session request ---
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_SESSION_TUTEE_IDS_JSON, tuteeId);
        values.put(SqlOpenHelper.KEY_SESSION_TUTOR_ID, tutorId);
        // Store Start and End times as milliseconds since epoch (as they are in Date objects)
        values.put(SqlOpenHelper.KEY_SESSION_START_TIME, requestedSlot.getStartTime().getTime());
        values.put(SqlOpenHelper.KEY_SESSION_END_TIME, requestedSlot.getEndTime().getTime());
        values.put(SqlOpenHelper.KEY_SESSION_SUBJECT_ID, subjectId);
        values.put(SqlOpenHelper.KEY_SESSION_LOCATION, location);
        values.put(SqlOpenHelper.KEY_SESSION_STATUS, Session.Status.PENDING.name()); // Initial status is Pending

        long result = -1;
        try {
            result = db.insert(SqlOpenHelper.TABLE_SESSIONS, null, values);
            Log.d("SessionDao", "DB Insert result: " + result);
        } catch (Exception e) {
            Log.e("SessionDao", "Error inserting session request", e);
        } finally {
            db.close(); // Ensure database is closed
        }

        return result != -1; // Return true if insert was successful (result is the row ID or -1 on error)
    }
/*
    public boolean requestSession(int tuteeStudentNum, int tutorStudentNum, TimeSlot requestedSlot, int subjectId, String location) {

        Date currentDate = new Date();

        // 1. Check if the requested start time itself is in the past (critical check)
        if (requestedSlot.getStartTime().before(currentDate)) {
            android.util.Log.e("SessionDAO", "Requested start time is in the past. Request: " + requestedSlot.getStartTime() + ", Current: " + currentDate);
            return false;
        }

        // 2. Check against tutor's operating hours (OPEN_TIME, CLOSE_TIME in UTC)
        // OPEN_TIME and CLOSE_TIME are assumed to be millisecond values for a UTC time of day
        // e.g., OPEN_TIME = 6 AM UTC, CLOSE_TIME = 4 PM UTC (16:00 UTC)
        Calendar reqCalUTC = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));

        reqCalUTC.setTime(requestedSlot.getStartTime());
        long reqStartTimeOfDayMillisUTC = reqCalUTC.get(Calendar.HOUR_OF_DAY) * 3600000L +
                reqCalUTC.get(Calendar.MINUTE) * 60000L;

        reqCalUTC.setTime(requestedSlot.getEndTime());
        long reqEndTimeOfDayMillisUTC = reqCalUTC.get(Calendar.HOUR_OF_DAY) * 3600000L +
                reqCalUTC.get(Calendar.MINUTE) * 60000L;

        // If end time is 00:00 and it's because duration made it wrap, it might be 0.
        // If it strictly means "within the day", then if startTimeOfDay > endTimeOfDay, it's an issue unless endTime is 00:00 of next day.
        // For simple day schedule (e.g. 6AM to 4PM), endTimeOfDay should be > startTimeOfDay.
        if (reqEndTimeOfDayMillisUTC == 0 && reqStartTimeOfDayMillisUTC > 0 && (requestedSlot.getEndTime().getTime() > requestedSlot.getStartTime().getTime())) {
            // This means endTime is midnight of the next day in UTC.
            // For check against CLOSE_TIME (e.g. 16:00 UTC), this slot would be considered as ending at 24:00 UTC for boundary purposes.
            // However, if CLOSE_TIME itself is not 24:00, this needs careful handling.
            // For now, let's assume CLOSE_TIME is within a day (like 16:00 UTC).
            // If a session ends exactly at CLOSE_TIME, reqEndTimeOfDayMillisUTC would be CLOSE_TIME.
        }
        // Check if the session is within the defined OPEN_TIME and CLOSE_TIME (UTC)
        boolean withinOperatingHours = reqStartTimeOfDayMillisUTC >= OPEN_TIME &&
                reqEndTimeOfDayMillisUTC <= CLOSE_TIME &&
                reqStartTimeOfDayMillisUTC < reqEndTimeOfDayMillisUTC; // Ensures positive duration

        // Handle case where session ends exactly at CLOSE_TIME and CLOSE_TIME is represented as 00:00 (unlikely with 16:00)
        // If CLOSE_TIME was, for instance, hourToMseconds(24-2), then reqEndTimeOfDayMillisUTC could be 0.
        // For current values (OPEN_TIME = 6AM UTC, CLOSE_TIME = 4PM UTC), simple check is fine.

        if (!withinOperatingHours) {
            android.util.Log.e("SessionDAO", "Requested slot outside UTC operating hours. " +
                    "ReqStartUTC: " + reqStartTimeOfDayMillisUTC + " (OPEN: " + OPEN_TIME + "), " +
                    "ReqEndUTC: " + reqEndTimeOfDayMillisUTC + " (CLOSE: " + CLOSE_TIME + ")");
            return false;
        }

        // 3. Check for overlaps with existing 'Confirmed' or 'Pending' sessions for the TUTOR
        List<TimeSlot> takenSlots = getTakenTimeSlot(tutorStudentNum); // Fetches for the correct tutor
        for (TimeSlot existingSlot : takenSlots) {
            if (requestedSlot.overlaps(existingSlot)) {
                android.util.Log.d("SessionDAO", "Requested slot overlaps with an existing session: " + existingSlot.getStartTime() + " - " + existingSlot.getEndTime());
                return false; // Conflict found
            }
        }

        // 4. If all checks pass, create and insert the session
        // The Session constructor should be Session(tutorId, tuteeId, subjectId)
        Session session = new Session(tutorStudentNum, tuteeStudentNum, subjectId); // Correct: params are already the actual IDs
        session.setStartTime(requestedSlot.getStartTime());
        session.setEndTime(requestedSlot.getEndTime());
        session.setStatus(Session.Status.PENDING);
        session.setLocation(location);

        long insertResult = insertSession(session);
        if (insertResult != -1) {
            android.util.Log.d("SessionDAO", "Session inserted successfully with ID: " + insertResult);
            return true;
        } else {
            android.util.Log.e("SessionDAO", "Failed to insert session into database.");
            return false;
        }
    }
*/
    /**
     * Retrieves a list of historical sessions (Completed, Cancelled, DECLINED) for a user,
     * with optional filters.
     *
     * @param currentUserId     The ID of the logged-in user.
     * @param isCurrentUserTutor True if the current user is viewing their history as a tutor,
     * false if as a tutee. This determines how partnerId is interpreted.
     * @param filterPartnerId   Optional ID of the other party (tutor or tutee) to filter by. 0 for no filter.
     * @param filterSubjectId   Optional ID of the subject to filter by. 0 for no filter.
     * @param filterStatus      Optional Session.Status to filter by. Null for no status filter (will fetch all historical types).
     * @param filterDateStart   Optional start date for filtering. Null for no start date filter.
     * @param filterDateEnd     Optional end date for filtering. Null for no end date filter.
     * @return A list of Session objects matching the criteria.
     */
    public List<Session> getHistoricalSessions(int currentUserId, boolean isCurrentUserTutor,
                                               int filterPartnerId, int filterSubjectId,
                                               Session.Status filterStatus,
                                               Date filterDateStart, Date filterDateEnd) {
        List<Session> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        StringBuilder selection = new StringBuilder();
        List<String> selectionArgsList = new ArrayList<>();

        // Base selection for the current user
        if (isCurrentUserTutor) {
            selection.append(SqlOpenHelper.KEY_SESSION_TUTOR_ID).append("=?");
        } else {
            selection.append(SqlOpenHelper.KEY_SESSION_TUTEE_IDS_JSON).append("LIKE %?%");
        }
        selectionArgsList.add(String.valueOf(currentUserId));

        // Append status filter (Completed, Cancelled, DECLINED)
        selection.append(" AND (")
                .append(SqlOpenHelper.KEY_SESSION_STATUS).append("=? OR ")
                .append(SqlOpenHelper.KEY_SESSION_STATUS).append("=? OR ")
                .append(SqlOpenHelper.KEY_SESSION_STATUS).append("=?)");
        selectionArgsList.add(Session.Status.COMPLETED.name());
        selectionArgsList.add(Session.Status.CANCELLED.name());
        selectionArgsList.add(Session.Status.DECLINED.name());


        // Apply specific status filter if provided
        if (filterStatus != null) {
            // Ensure the filterStatus is one of the allowed historical types
            if (filterStatus == Session.Status.COMPLETED ||
                    filterStatus == Session.Status.CANCELLED ||
                    filterStatus == Session.Status.DECLINED) {
                // This overrides the broad OR group for status and selects only one
                // We need to reconstruct the selection part for status
                // Remove the general status clause first
                int statusClauseIndex = selection.indexOf(" AND ("); // Find the start of status clause
                if (statusClauseIndex != -1) {
                    selection.setLength(statusClauseIndex); // Truncate before general status clause
                    selectionArgsList.clear(); // Clear and re-add currentUserId
                    if (isCurrentUserTutor) {
                        selection.append(SqlOpenHelper.KEY_SESSION_TUTOR_ID).append("=?");
                    } else {
                        selection.append(SqlOpenHelper.KEY_SESSION_TUTEE_IDS_JSON).append("LIKE %?%");
                    }
                    selectionArgsList.add(String.valueOf(currentUserId));
                }
                selection.append(" AND ").append(SqlOpenHelper.KEY_SESSION_STATUS).append("=?");
                selectionArgsList.add(filterStatus.name());
            }
        }

        // Apply partner filter
        if (filterPartnerId != 0) {
            if (isCurrentUserTutor) { // Current user is tutor, so partner is tutee
                selection.append(" AND ").append(SqlOpenHelper.KEY_SESSION_TUTEE_IDS_JSON).append("LIKE %?%");
            } else { // Current user is tutee, so partner is tutor
                selection.append(" AND ").append(SqlOpenHelper.KEY_SESSION_TUTOR_ID).append("=?");
            }
            selectionArgsList.add(String.valueOf(filterPartnerId));
        }

        // Apply subject filter
        if (filterSubjectId != 0) {
            selection.append(" AND ").append(SqlOpenHelper.KEY_SESSION_SUBJECT_ID).append("=?");
            selectionArgsList.add(String.valueOf(filterSubjectId));
        }

        // Apply date filters (on session start time)
        if (filterDateStart != null) {
            selection.append(" AND ").append(SqlOpenHelper.KEY_SESSION_START_TIME).append(" >= ?");
            selectionArgsList.add(String.valueOf(filterDateStart.getTime()));
        }
        if (filterDateEnd != null) {
            // To include the whole end day, add 24 hours minus 1 ms, or compare with day+1 midnight
            Calendar cal = Calendar.getInstance();
            cal.setTime(filterDateEnd);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            selection.append(" AND ").append(SqlOpenHelper.KEY_SESSION_START_TIME).append(" <= ?");
            selectionArgsList.add(String.valueOf(cal.getTimeInMillis()));
        }

        try {
            cursor = db.query(SqlOpenHelper.TABLE_SESSIONS,
                    null, // all columns
                    selection.toString(),
                    selectionArgsList.toArray(new String[0]),
                    null, null, SqlOpenHelper.KEY_SESSION_START_TIME + " DESC"); // Order by most recent

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    sessions.add(cursorToSession(cursor)); // Assumes cursorToSession exists
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("SessionDao", "Error getting historical sessions", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        Log.d("SessionDao", "Fetched " + sessions.size() + " historical sessions for user " + currentUserId + " with filters.");
        return sessions;
    }


    /**
     * Gets a list of distinct users (partners) with whom the current user has had
     * historical sessions (Completed, Cancelled, DECLINED).
     * @param currentUserId The ID of the logged-in user.
     * @param isCurrentUserTutor True if the current user is a tutor, false otherwise.
     * @return A list of User objects (partners).
     */
    public List<User> getDistinctPartnersForHistory(int currentUserId, boolean isCurrentUserTutor) {
        List<User> partners = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        // Determine which ID column to select for the partner
        String partnerIdColumn = isCurrentUserTutor ? SqlOpenHelper.KEY_SESSION_TUTEE_IDS_JSON : SqlOpenHelper.KEY_SESSION_TUTOR_ID;
        String userIdColumn = isCurrentUserTutor ? SqlOpenHelper.KEY_SESSION_TUTOR_ID : SqlOpenHelper.KEY_SESSION_TUTEE_IDS_JSON;

        String query = "SELECT DISTINCT u." + SqlOpenHelper.KEY_USER_STUDENT_NUM + ", " +
                "u." + SqlOpenHelper.KEY_USER_FIRST_NAME + ", " +
                "u." + SqlOpenHelper.KEY_USER_LAST_NAME + ", " +
                "u." + SqlOpenHelper.KEY_USER_EMAIL + ", " + // Add other fields you need for User object
                "u." + SqlOpenHelper.KEY_USER_IS_TUTOR + ", " +
                "u." + SqlOpenHelper.KEY_USER_PASSWORD + ", " +
                "u." + SqlOpenHelper.KEY_USER_AVERAGE_RATING +
                " FROM " + SqlOpenHelper.TABLE_USERS + " u JOIN " + SqlOpenHelper.TABLE_SESSIONS + " s " +
                "ON u." + SqlOpenHelper.KEY_USER_STUDENT_NUM + " = s." + partnerIdColumn + " " +
                "WHERE s." + userIdColumn + "LIKE %?% AND " +
                "(s." + SqlOpenHelper.KEY_SESSION_STATUS + "=? OR " +
                "s." + SqlOpenHelper.KEY_SESSION_STATUS + "=? OR " +
                "s." + SqlOpenHelper.KEY_SESSION_STATUS + "=?)";
        try {
            cursor = db.rawQuery(query, new String[]{
                    String.valueOf(currentUserId),
                    Session.Status.COMPLETED.name(),
                    Session.Status.CANCELLED.name(),
                    Session.Status.DECLINED.name()
            });

            if (cursor != null && cursor.moveToFirst()) {
                UserDao userDao = new UserDao(dbHelper); // Temporary UserDao instance for cursorToUser
                do {
                    // Assuming you have a cursorToUser method in UserDao or similar utility
                    partners.add(userDao.cursorToUser(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("SessionDao", "Error getting distinct partners for history", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return partners;
    }

    /**
     * Gets a list of distinct subjects from the user's historical sessions.
     * @param currentUserId The ID of the logged-in user.
     * @param isCurrentUserTutor True if the current user is a tutor (affects which sessions are considered 'theirs').
     * @return A list of Subject objects.
     */
    public List<com.pbdvmobile.app.data.model.Subject> getDistinctSubjectsForHistory(int currentUserId, boolean isCurrentUserTutor) {
        List<com.pbdvmobile.app.data.model.Subject> subjects = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        String userIdColumn = isCurrentUserTutor ? SqlOpenHelper.KEY_SESSION_TUTOR_ID : SqlOpenHelper.KEY_SESSION_TUTEE_IDS_JSON;

        String query = "SELECT DISTINCT sub.* FROM " + SqlOpenHelper.TABLE_SUBJECTS + " sub JOIN " +
                SqlOpenHelper.TABLE_SESSIONS + " s ON sub." + SqlOpenHelper.KEY_SUBJECT_ID + " = s." + SqlOpenHelper.KEY_SESSION_SUBJECT_ID + " " +
                "WHERE s." + userIdColumn + "LIKE %?% AND " +
                "(s." + SqlOpenHelper.KEY_SESSION_STATUS + "=? OR " +
                "s." + SqlOpenHelper.KEY_SESSION_STATUS + "=? OR " +
                "s." + SqlOpenHelper.KEY_SESSION_STATUS + "=?)";
        try {
            cursor = db.rawQuery(query, new String[]{
                    String.valueOf(currentUserId),
                    Session.Status.COMPLETED.name(),
                    Session.Status.CANCELLED.name(),
                    Session.Status.DECLINED.name()
            });
            if (cursor != null && cursor.moveToFirst()) {
                SubjectDao subjectDao = new SubjectDao(dbHelper);
                do {
                    // Assuming you have a cursorToSubject method in SubjectDao
                    subjects.add(subjectDao.cursorToSubject(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("SessionDao", "Error getting distinct subjects for history", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return subjects;
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

    private List<Session> getSessionsByTutorIdAndStatus(int tutorId, Session.Status[] statuses) {
        List<Session> sessions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase(); // Use getReadableDatabase for queries
        Cursor cursor = null; // Initialize cursor to null for finally block

        if (statuses == null || statuses.length == 0) {
            db.close(); // Close db if no statuses provided
            return sessions; // Return empty list if no statuses to check
        }

        try {
            // Build the IN clause string dynamically and safely for statuses
            StringBuilder statusPlaceholders = new StringBuilder();
            String[] statusNames = new String[statuses.length];
            for (int i = 0; i < statuses.length; i++) {
                statusNames[i] = statuses[i].name();
                statusPlaceholders.append("?");
                if (i < statuses.length - 1) {
                    statusPlaceholders.append(",");
                }
            }

            String selection = SqlOpenHelper.KEY_SESSION_TUTOR_ID + "=? AND " +
                    SqlOpenHelper.KEY_SESSION_STATUS + " IN (" + statusPlaceholders.toString() + ")";

            // Combine tutorId and status names into selectionArgs
            String[] selectionArgs = new String[1 + statuses.length];
            selectionArgs[0] = String.valueOf(tutorId);
            System.arraycopy(statusNames, 0, selectionArgs, 1, statuses.length);

            cursor = db.query(SqlOpenHelper.TABLE_SESSIONS,
                    null, // columns (null for all)
                    selection, // selection criteria
                    selectionArgs, // selection arguments
                    null, // groupBy
                    null, // having
                    SqlOpenHelper.KEY_SESSION_START_TIME); // orderBy

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Session session = cursorToSession(cursor); // Assumes cursorToSession exists and works
                    sessions.add(session);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            android.util.Log.e("SessionDao", "Error in getSessionsByTutorIdAndStatus", e);
            // Handle error appropriately, maybe rethrow or return empty list
        } finally {
            if (cursor != null) {
                cursor.close(); // Ensure cursor is closed
            }
            db.close(); // Ensure DB connection is closed
        }
        return sessions;
    }

    /**
     * Calculates available time slots for a tutor on a specific date.
     * Considers OPEN_TIME, CLOSE_TIME (as UTC time-of-day), interval, and existing sessions.
     *
     * @param tutorId       The ID of the tutor.
     * @param selectedDate  The specific date for which to find slots (time part is ignored, uses YYYY-MM-DD).
     * @param intervalMillis The duration of each potential slot in milliseconds (e.g., 2 hours).
     * @return A list of Long values representing the start time of available slots.
     * These Long values are UTC milliseconds from epoch (like OPEN_TIME) representing the time of day.
     */
    public List<Long> getAvailableTimeSlots(int tutorId, Date selectedDate, long intervalMillis) {
        List<Long> availableSlotsStartTimes = new ArrayList<>();
        Date now = new Date(); // Current time for comparison

        // 1. Define the start and end of the selected day in the *local* timezone
        Calendar dayStartLocal = Calendar.getInstance(); // Uses local timezone
        dayStartLocal.setTime(selectedDate);
        dayStartLocal.set(Calendar.HOUR_OF_DAY, 0); dayStartLocal.set(Calendar.MINUTE, 0);
        dayStartLocal.set(Calendar.SECOND, 0); dayStartLocal.set(Calendar.MILLISECOND, 0);
        Date startOfDayLocal = dayStartLocal.getTime();

        Calendar dayEndLocal = Calendar.getInstance(); // Uses local timezone
        dayEndLocal.setTime(selectedDate);
        dayEndLocal.set(Calendar.HOUR_OF_DAY, 23); dayEndLocal.set(Calendar.MINUTE, 59);
        dayEndLocal.set(Calendar.SECOND, 59); dayEndLocal.set(Calendar.MILLISECOND, 999);
        Date endOfDayLocal = dayEndLocal.getTime();

        // 2. Get tutor's booked slots (Confirmed/Pending) that overlap with the selected day
        List<TimeSlot> bookedSlotsOnDate = new ArrayList<>();
        List<Session> allBookedSessions = getSessionsByTutorIdAndStatus(tutorId,
                new Session.Status[]{Session.Status.CONFIRMED, Session.Status.PENDING});

        for (Session session : allBookedSessions) {
            // Session start/end times are stored as full timestamps.
            // We need to check if the session interval overlaps the selected day interval.
            TimeSlot bookedSlot = new TimeSlot(session.getStartTime(), session.getEndTime());
            if (bookedSlot.getStartTime().before(endOfDayLocal) && bookedSlot.getEndTime().after(startOfDayLocal)) {
                bookedSlotsOnDate.add(bookedSlot);
                android.util.Log.d("GetAvailableTimes", "Booked slot overlapping date found: " + bookedSlot.getStartTime() + " - " + bookedSlot.getEndTime());
            }
        }
        android.util.Log.d("GetAvailableTimes", "Found " + bookedSlotsOnDate.size() + " booked slots overlapping selected date: " + selectedDate);

        // 3. Iterate through potential slots based on OPEN_TIME, CLOSE_TIME (UTC time-of-day) & interval
        for (long potentialStartTimeOfDayUTC = OPEN_TIME; potentialStartTimeOfDayUTC < CLOSE_TIME; potentialStartTimeOfDayUTC += intervalMillis) {

            // 4. Construct the potential TimeSlot in the *local* timezone for the selected date
            Calendar potentialStartCalLocal = Calendar.getInstance(); // Local TZ
            potentialStartCalLocal.setTime(selectedDate); // Set to YYYY, MM, DD of selected date (local)

            Calendar timePartCalUTC = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            timePartCalUTC.setTimeInMillis(potentialStartTimeOfDayUTC); // Represents the time part in UTC

            // Apply the UTC hour/minute to the local calendar instance for the selected date
            potentialStartCalLocal.set(Calendar.HOUR_OF_DAY, timePartCalUTC.get(Calendar.HOUR_OF_DAY));
            potentialStartCalLocal.set(Calendar.MINUTE, timePartCalUTC.get(Calendar.MINUTE));
            potentialStartCalLocal.set(Calendar.SECOND, 0); potentialStartCalLocal.set(Calendar.MILLISECOND, 0);
            Date potentialStartDateLocal = potentialStartCalLocal.getTime(); // The potential start datetime in local TZ

            Date potentialEndDateLocal = new Date(potentialStartDateLocal.getTime() + intervalMillis); // Potential end datetime in local TZ
            TimeSlot potentialSlot = new TimeSlot(potentialStartDateLocal, potentialEndDateLocal);

            android.util.Log.d("GetAvailableTimes", "Checking potential slot (Local): " + potentialStartDateLocal + " - " + potentialEndDateLocal);

            // 5. Check if potential slot start time is in the future
            if (potentialStartDateLocal.before(now)) {
                android.util.Log.d("GetAvailableTimes", "--> Slot skipped: Start time is in the past.");
                continue;
            }

            // 6. Check for overlaps with booked slots
            boolean conflicts = false;
            for (TimeSlot booked : bookedSlotsOnDate) {
                if (potentialSlot.overlaps(booked)) {
                    conflicts = true;
                    android.util.Log.d("GetAvailableTimes", "--> Slot skipped: Conflicts with booked slot: " + booked.getStartTime() + " - " + booked.getEndTime());
                    break;
                }
            }

            // 7. If no conflict and start time is in the future, add the UTC time-of-day value
            if (!conflicts) {
                availableSlotsStartTimes.add(potentialStartTimeOfDayUTC);
                android.util.Log.d("GetAvailableTimes", "--> Slot Available! Adding UTC Start Time Value: " + potentialStartTimeOfDayUTC);
            }
        }

        android.util.Log.d("GetAvailableTimes", "Finished check. Returning " + availableSlotsStartTimes.size() + " available start times (UTC values).");
        return availableSlotsStartTimes;
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

    /*public int addTuteeReview(int sessionId, String review) {
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
    }*/



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
        session.setTuteeIds(ListJsonConverter.jsonToList(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SESSION_TUTEE_IDS_JSON)),
                new TypeToken<List<Integer>>() {}.getType()));
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

    public int updateSession(Session sessionToBook) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_SESSION_STATUS, sessionToBook.getStatus().name());
        values.put(SqlOpenHelper.KEY_SESSION_TUTOR_REVIEW, sessionToBook.getTutorReview());
        values.put(SqlOpenHelper.KEY_SESSION_TUTEE_REVIEW, sessionToBook.getTuteeReview());
        values.put(SqlOpenHelper.KEY_SESSION_TUTOR_RATING, sessionToBook.getTutorRating());
        values.put(SqlOpenHelper.KEY_SESSION_TUTEE_RATING, sessionToBook.getTuteeRating());
        values.put(SqlOpenHelper.KEY_SESSION_START_TIME, sessionToBook.getStartTime().getTime());
        values.put(SqlOpenHelper.KEY_SESSION_END_TIME, sessionToBook.getEndTime().getTime());
        values.put(SqlOpenHelper.KEY_SESSION_LOCATION, sessionToBook.getLocation());
        values.put(SqlOpenHelper.KEY_SESSION_SUBJECT_ID, sessionToBook.getSubjectId());
        values.put(SqlOpenHelper.KEY_SESSION_TUTEE_IDS_JSON, ListJsonConverter.listToJson(sessionToBook.getTuteeIds()));
        values.put(SqlOpenHelper.KEY_SESSION_TUTOR_ID, sessionToBook.getTutorId());
        values.put(SqlOpenHelper.KEY_SESSION_ID, sessionToBook.getId());



        int rowsAffected = db.update(SqlOpenHelper.TABLE_SESSIONS, values,
                SqlOpenHelper.KEY_SESSION_ID + "=?",
                new String[]{String.valueOf(sessionToBook.getId())});
        db.close();
        return rowsAffected;

    }
}
