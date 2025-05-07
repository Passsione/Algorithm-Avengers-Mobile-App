package com.pbdvmobile.app.data.dao;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import com.pbdvmobile.app.data.SqlOpenHelper;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.UserSubject;

import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private static final String TAG = "UserDao";
    private final SqlOpenHelper dbHelper;

    public UserDao(SqlOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertUser(@NonNull User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_USER_STUDENT_NUM, user.getStudentNum());
        values.put(SqlOpenHelper.KEY_USER_FIRST_NAME, user.getFirstName());
        values.put(SqlOpenHelper.KEY_USER_LAST_NAME, user.getLastName());
        values.put(SqlOpenHelper.KEY_USER_EMAIL, user.getEmail());
        values.put(SqlOpenHelper.KEY_USER_PASSWORD, user.getPassword());
        values.put(SqlOpenHelper.KEY_USER_BIO, user.getBio());
        values.put(SqlOpenHelper.KEY_USER_EDUCATION_LEVEL, user.getEducationLevel().name());
        values.put(SqlOpenHelper.KEY_USER_IS_TUTOR, user.isTutor() ? 1 : 0);
        values.put(SqlOpenHelper.KEY_USER_TIER_LEVEL, user.getTierLevel().name());
        values.put(SqlOpenHelper.KEY_USER_AVERAGE_RATING, user.getAverageRating());
        values.put(SqlOpenHelper.KEY_USER_PROFILE_IMAGE_URL, user.getProfileImageUrl());
        values.put(SqlOpenHelper.KEY_USER_CREDITS, user.getCredits());
        values.put(SqlOpenHelper.KEY_USER_SUBJECTS, user.getSubjects());
        values.put(SqlOpenHelper.KEY_USER_BANK_DETAILS, user.getBankDetails());

        long id = db.insert(SqlOpenHelper.TABLE_USERS, null, values);
        db.close();
        return id;
    }

    public User getUserByStudentNum(int studentNum) {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_USERS,
                null,
                SqlOpenHelper.KEY_USER_STUDENT_NUM + "=?",
                new String[]{String.valueOf(studentNum)},
                null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        db.close();
        return user;
    }

    public User getUserByEmail(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_USERS,
                null,
                SqlOpenHelper.KEY_USER_EMAIL + "=?",
                new String[]{email},
                null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        db.close();
        return user;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_USERS,
                null,null, null,
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                User user = cursorToUser(cursor);
                users.add(user);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return users;
    }
    public List<User> getAllTutors() {
        List<User> tutors = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_USERS,
                null,
                SqlOpenHelper.KEY_USER_IS_TUTOR + "=?",
                new String[]{"1"},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                User user = cursorToUser(cursor);
                tutors.add(user);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tutors;
    }

    public int updateUser(@NonNull User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_USER_FIRST_NAME, user.getFirstName());
        values.put(SqlOpenHelper.KEY_USER_LAST_NAME, user.getLastName());
        values.put(SqlOpenHelper.KEY_USER_EMAIL, user.getEmail());
        values.put(SqlOpenHelper.KEY_USER_PASSWORD, user.getPassword());
        values.put(SqlOpenHelper.KEY_USER_BIO, user.getBio());
        values.put(SqlOpenHelper.KEY_USER_EDUCATION_LEVEL, user.getEducationLevel().name());
        values.put(SqlOpenHelper.KEY_USER_IS_TUTOR, user.isTutor() ? 1 : 0);
        values.put(SqlOpenHelper.KEY_USER_TIER_LEVEL, user.getTierLevel().name());
        values.put(SqlOpenHelper.KEY_USER_AVERAGE_RATING, user.getAverageRating());
        values.put(SqlOpenHelper.KEY_USER_PROFILE_IMAGE_URL, user.getProfileImageUrl());
        values.put(SqlOpenHelper.KEY_USER_CREDITS, user.getCredits());
        values.put(SqlOpenHelper.KEY_USER_SUBJECTS, user.getSubjects());
        values.put(SqlOpenHelper.KEY_USER_BANK_DETAILS, user.getBankDetails());

        int rowsAffected = db.update(SqlOpenHelper.TABLE_USERS, values,
                SqlOpenHelper.KEY_USER_STUDENT_NUM + "=?",
                new String[]{String.valueOf(user.getStudentNum())});
        db.close();
        return rowsAffected;
    }

    public int updateUserEmail(@NonNull int id, String email) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_USER_EMAIL, email);

        int rowsAffected = db.update(SqlOpenHelper.TABLE_USERS, values,
                SqlOpenHelper.KEY_USER_STUDENT_NUM + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return rowsAffected;
    }

    public int deleteUser(int studentNum) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete(SqlOpenHelper.TABLE_USERS,
                SqlOpenHelper.KEY_USER_STUDENT_NUM + "=?",
                new String[]{String.valueOf(studentNum)});
        db.close();
        return rowsAffected;
    }

    @NonNull
    public User cursorToUser(@NonNull Cursor cursor) {
        User user = new User();
        user.setStudentNum(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_STUDENT_NUM)));
        user.setFirstName(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_FIRST_NAME)));
        user.setLastName(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_LAST_NAME)));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_EMAIL)));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_PASSWORD)));
        user.setBio(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_BIO)));

        String eduLevelString = cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_EDUCATION_LEVEL));
        try {
            user.setEducationLevel(User.EduLevel.valueOf(eduLevelString));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid education level: " + eduLevelString);
            user.setEducationLevel(User.EduLevel.BACHELOR); // Default value
        }

        user.setTutor(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_IS_TUTOR)) == 1);

        String tierLevelString = cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_TIER_LEVEL));
        try {
            user.setTierLevel(User.TierLevel.valueOf(tierLevelString));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid tier level: " + tierLevelString);
            user.setTierLevel(User.TierLevel.BASIC); // Default value
        }

        user.setAverageRating(cursor.getDouble(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_AVERAGE_RATING)));
        user.setProfileImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_PROFILE_IMAGE_URL)));
        user.setCredits(cursor.getDouble(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_CREDITS)));
        user.setSubjects(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_SUBJECTS)));
        user.setBankDetails(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_USER_BANK_DETAILS)));

        return user;
    }

}