package com.pbdvmobile.app.data.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pbdvmobile.app.data.SqlOpenHelper;
import com.pbdvmobile.app.data.model.Notification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificationDao {
    private static final String TAG = "NotificationDao";
    private final SqlOpenHelper dbHelper;

    public NotificationDao(SqlOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Inserts a new notification into the database.
     *
     * @param notification The Notification object to insert.
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    public long insertNotification(@NonNull Notification notification) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = -1;
        try {
            ContentValues values = new ContentValues();
            if (notification.getStudentNum() != null) {
                values.put(SqlOpenHelper.KEY_NOTIFICATION_STUDENT_NUM, notification.getStudentNum());
            } else {
                values.putNull(SqlOpenHelper.KEY_NOTIFICATION_STUDENT_NUM);
            }
            values.put(SqlOpenHelper.KEY_NOTIFICATION_TEXT, notification.getText());
            values.put(SqlOpenHelper.KEY_NOTIFICATION_STATUS, notification.getStatus().name());
            values.put(SqlOpenHelper.KEY_NOTIFICATION_DATE, notification.getDate().getTime()); // Store as long (milliseconds)
            values.put(SqlOpenHelper.KEY_NOTIFICATION_REMEMBER, notification.isRemember() ? 1 : 0);

            id = db.insert(SqlOpenHelper.TABLE_NOTIFICATIONS, null, values);
            if (id != -1) {
                notification.setNoteId((int) id); // Set the ID on the object
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inserting notification", e);
        } finally {
            db.close();
        }
        return id;
    }

    /**
     * Retrieves a specific notification by its ID.
     *
     * @param noteId The ID of the notification.
     * @return The Notification object, or null if not found.
     */
    @Nullable
    public Notification getNotificationById(int noteId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Notification notification = null;
        Cursor cursor = null;
        try {
            cursor = db.query(SqlOpenHelper.TABLE_NOTIFICATIONS,
                    null, // All columns
                    SqlOpenHelper.KEY_NOTIFICATION_ID + "=?",
                    new String[]{String.valueOf(noteId)},
                    null, null, null);

            if (cursor.moveToFirst()) {
                notification = cursorToNotification(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting notification by ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return notification;
    }

    /**
     * Retrieves all notifications for a specific student, ordered by date descending.
     *
     * @param studentNum The student number.
     * @return A list of Notifications.
     */
    public List<Notification> getNotificationsByStudentNum(int studentNum) {
        List<Notification> notifications = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(SqlOpenHelper.TABLE_NOTIFICATIONS,
                    null, // All columns
                    SqlOpenHelper.KEY_NOTIFICATION_STUDENT_NUM + "=?",
                    new String[]{String.valueOf(studentNum)},
                    null, null, SqlOpenHelper.KEY_NOTIFICATION_DATE + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    notifications.add(cursorToNotification(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting notifications by student number", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return notifications;
    }

    /**
     * Retrieves all system-wide notifications (where studentNum is NULL), ordered by date descending.
     * @return A list of system-wide Notifications.
     */
    public List<Notification> getSystemNotifications() {
        List<Notification> notifications = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(SqlOpenHelper.TABLE_NOTIFICATIONS,
                    null, // All columns
                    SqlOpenHelper.KEY_NOTIFICATION_STUDENT_NUM + " IS NULL",
                    null,
                    null, null, SqlOpenHelper.KEY_NOTIFICATION_DATE + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    notifications.add(cursorToNotification(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting system notifications", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return notifications;
    }


    /**
     * Retrieves all notifications, ordered by date descending.
     *
     * @return A list of all Notifications.
     */
    public List<Notification> getAllNotifications() {
        List<Notification> notifications = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(SqlOpenHelper.TABLE_NOTIFICATIONS,
                    null, // All columns
                    null, null, // No selection
                    null, null, SqlOpenHelper.KEY_NOTIFICATION_DATE + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    notifications.add(cursorToNotification(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all notifications", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return notifications;
    }

    /**
     * Updates an existing notification.
     *
     * @param notification The Notification object with updated values.
     * @return The number of rows affected.
     */
    public int updateNotification(@NonNull Notification notification) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = 0;
        try {
            ContentValues values = new ContentValues();
            values.put(SqlOpenHelper.KEY_NOTIFICATION_STUDENT_NUM, notification.getStudentNum());
            values.put(SqlOpenHelper.KEY_NOTIFICATION_TEXT, notification.getText());
            values.put(SqlOpenHelper.KEY_NOTIFICATION_STATUS, notification.getStatus().name());
            values.put(SqlOpenHelper.KEY_NOTIFICATION_DATE, notification.getDate().getTime());
            values.put(SqlOpenHelper.KEY_NOTIFICATION_REMEMBER, notification.isRemember() ? 1 : 0);

            rowsAffected = db.update(SqlOpenHelper.TABLE_NOTIFICATIONS, values,
                    SqlOpenHelper.KEY_NOTIFICATION_ID + "=?",
                    new String[]{String.valueOf(notification.getNoteId())});
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification", e);
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    /**
     * Updates the status of a specific notification.
     * @param noteId The ID of the notification to update.
     * @param status The new status.
     * @return The number of rows affected.
     */
    public int updateNotificationStatus(int noteId, Notification.Status status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = 0;
        try {
            ContentValues values = new ContentValues();
            values.put(SqlOpenHelper.KEY_NOTIFICATION_STATUS, status.name());
            rowsAffected = db.update(SqlOpenHelper.TABLE_NOTIFICATIONS, values,
                    SqlOpenHelper.KEY_NOTIFICATION_ID + "=?",
                    new String[]{String.valueOf(noteId)});
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification status", e);
        } finally {
            db.close();
        }
        return rowsAffected;
    }


    /**
     * Deletes a notification by its ID.
     *
     * @param noteId The ID of the notification to delete.
     * @return The number of rows deleted.
     */
    public int deleteNotification(int noteId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = 0;
        try {
            rowsDeleted = db.delete(SqlOpenHelper.TABLE_NOTIFICATIONS,
                    SqlOpenHelper.KEY_NOTIFICATION_ID + "=?",
                    new String[]{String.valueOf(noteId)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting notification", e);
        } finally {
            db.close();
        }
        return rowsDeleted;
    }

    /**
     * Deletes all notifications for a specific student.
     * @param studentNum The student number.
     * @return The number of rows deleted.
     */
    public int deleteNotificationsByStudentNum(int studentNum) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = 0;
        try {
            rowsDeleted = db.delete(SqlOpenHelper.TABLE_NOTIFICATIONS,
                    SqlOpenHelper.KEY_NOTIFICATION_STUDENT_NUM + "=?",
                    new String[]{String.valueOf(studentNum)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting notifications by student number", e);
        } finally {
            db.close();
        }
        return rowsDeleted;
    }

    /**
     * Deletes all notifications.
     * @return The number of rows deleted.
     */
    public int deleteAllNotifications() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = 0;
        try {
            rowsDeleted = db.delete(SqlOpenHelper.TABLE_NOTIFICATIONS, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting all notifications", e);
        } finally {
            db.close();
        }
        return rowsDeleted;
    }


    private Notification cursorToNotification(@NonNull Cursor cursor) {
        Notification notification = new Notification();
        notification.setNoteId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_NOTIFICATION_ID)));

        int studentNumColumnIndex = cursor.getColumnIndex(SqlOpenHelper.KEY_NOTIFICATION_STUDENT_NUM);
        if (!cursor.isNull(studentNumColumnIndex)) {
            notification.setStudentNum(cursor.getInt(studentNumColumnIndex));
        } else {
            notification.setStudentNum(null);
        }

        notification.setText(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_NOTIFICATION_TEXT)));
        notification.setStatus(Notification.Status.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_NOTIFICATION_STATUS))));
        notification.setDate(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_NOTIFICATION_DATE))));
        notification.setRemember(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_NOTIFICATION_REMEMBER)) == 1);
        return notification;
    }
}