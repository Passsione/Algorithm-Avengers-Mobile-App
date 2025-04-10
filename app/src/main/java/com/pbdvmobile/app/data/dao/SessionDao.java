package com.pbdvmobile.app.data.dao;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import com.pbdvmobile.app.data.SqlOpenHelper;
import com.pbdvmobile.app.data.model.Session;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SessionDao {
    private final SqlOpenHelper dbHelper;

    public SessionDao(SqlOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

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
