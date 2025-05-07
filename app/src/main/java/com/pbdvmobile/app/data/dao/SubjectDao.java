package com.pbdvmobile.app.data.dao;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import com.pbdvmobile.app.data.SqlOpenHelper;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.UserSubject;
import java.util.ArrayList;
import java.util.List;

public class SubjectDao {
    private final SqlOpenHelper dbHelper;

    public SubjectDao(SqlOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // Subject section
    public long insertSubject(@NonNull Subject subject) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_SUBJECT_NAME, subject.getSubjectName());

        long id = db.insert(SqlOpenHelper.TABLE_SUBJECTS, null, values);
        db.close();
        return id;
    }

    public Subject getSubjectById(int subjectId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_SUBJECTS,
                null,
                SqlOpenHelper.KEY_SUBJECT_ID + "=?",
                new String[]{String.valueOf(subjectId)},
                null, null, null);

        Subject subject = null;
        if (cursor.moveToFirst()) {
            subject = new Subject();
            subject.setSubjectId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SUBJECT_ID)));
            subject.setSubjectName(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SUBJECT_NAME)));
            cursor.close();
        }
        db.close();
        return subject;
    }

    public List<Subject> getAllSubjects() {
        List<Subject> subjects = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_SUBJECTS,
                null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Subject subject = new Subject();
                subject.setSubjectId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SUBJECT_ID)));
                subject.setSubjectName(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SUBJECT_NAME)));
                subjects.add(subject);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return subjects;
    }


    // User Subject section
    public long addUserSubject(@NonNull UserSubject userSubject) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_USER_SUBJECT_USER_ID, userSubject.getUserId());
        values.put(SqlOpenHelper.KEY_USER_SUBJECT_SUBJECT_ID, userSubject.getSubjectId());
        values.put(SqlOpenHelper.KEY_USER_SUBJECT_MARK, userSubject.getMark());
        values.put(SqlOpenHelper.KEY_USER_SUBJECT_TUTORING, userSubject.getTutoring() ? 1 : 0);

        long id = db.insert(SqlOpenHelper.TABLE_USER_SUBJECTS, null, values);
        db.close();
        return id;
    }
    public List<UserSubject> getUserSubjects(int userId) {
        List<UserSubject> userSubjects = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_USER_SUBJECTS,
                null,
                SqlOpenHelper.KEY_USER_SUBJECT_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                UserSubject userSubject = new UserSubject();
                userSubject.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_USER_ID)));
                userSubject.setSubjectId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_SUBJECT_ID)));
                userSubject.setMark(cursor.getDouble(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_MARK)));
                userSubject.setTutoring(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_TUTORING)) == 1);
                userSubjects.add(userSubject);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return userSubjects;
    }
    public List<UserSubject> getTutoredUserSubjects(int userId) {
        List<UserSubject> userSubjects = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_USER_SUBJECTS,
                null,
                SqlOpenHelper.KEY_USER_SUBJECT_USER_ID + "=?"+
                        " AND " + SqlOpenHelper.KEY_USER_SUBJECT_TUTORING + "=1",
                new String[]{String.valueOf(userId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                UserSubject userSubject = new UserSubject();
                userSubject.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_USER_ID)));
                userSubject.setSubjectId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_SUBJECT_ID)));
                userSubject.setMark(cursor.getDouble(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_MARK)));
                userSubject.setTutoring(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_TUTORING)) == 1);
                userSubjects.add(userSubject);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return userSubjects;
    }
    public List<UserSubject> getAllUserSubjects() {
        List<UserSubject> subjects = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_USER_SUBJECTS,
                null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                UserSubject subject = new UserSubject();
                subject.setSubjectId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_SUBJECT_ID)));
                subject.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_USER_ID)));
                subject.setMark(cursor.getDouble(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_MARK)));
                subject.setTutoring(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECT_TUTORING)) == 1);
                subjects.add(subject);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return subjects;
    }

    public int updateUserSubject(UserSubject userSubject){
        int userId = userSubject.getUserId();
        int subjectId = userSubject.getSubjectId();
        double mark = userSubject.getMark();
        boolean tutoring = userSubject.getTutoring();

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_USER_SUBJECT_MARK, mark);
        values.put(SqlOpenHelper.KEY_USER_SUBJECT_TUTORING, tutoring ? 1 : 0);
        int rowsAffected = db.update(SqlOpenHelper.TABLE_USER_SUBJECTS, values,
                SqlOpenHelper.KEY_USER_SUBJECT_USER_ID + "=? AND " +
                        SqlOpenHelper.KEY_USER_SUBJECT_SUBJECT_ID + "=?",
                new String[]{String.valueOf(userId), String.valueOf(subjectId)});
        db.close();
        return rowsAffected;
    }
    public int deleteUserSubject(int userId, int subjectId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete(SqlOpenHelper.TABLE_USER_SUBJECTS,
                SqlOpenHelper.KEY_USER_SUBJECT_USER_ID + "=? AND " +
                        SqlOpenHelper.KEY_USER_SUBJECT_SUBJECT_ID + "=?",
                new String[]{String.valueOf(userId), String.valueOf(subjectId)});
        db.close();
        return rowsAffected;
    }


    public Subject cursorToSubject(Cursor cursor) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Subject subject = null;
        if (cursor.moveToFirst()) {
            subject = new Subject();
            subject.setSubjectId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SUBJECT_ID)));
            subject.setSubjectName(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_SUBJECT_NAME)));
            cursor.close();
        }
        db.close();
        return subject;

    }
}