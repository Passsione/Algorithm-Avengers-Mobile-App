package com.pbdvmobile.app.data.dao;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pbdvmobile.app.data.SqlOpenHelper;
import com.pbdvmobile.app.data.model.Resource;

import java.util.ArrayList;
import java.util.List;

public class ResourceDao {
    private final SqlOpenHelper dbHelper;

    public ResourceDao(SqlOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertResource(Resource resource) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_RESOURCE_URL, resource.getResource());
        values.put(SqlOpenHelper.KEY_RESOURCE_TUTOR_ID, resource.getTutorId());
        values.put(SqlOpenHelper.KEY_RESOURCE_SUBJECT_ID, resource.getSubjectId());
        values.put(SqlOpenHelper.KEY_RESOURCE_NAME, resource.getName());

        long id = db.insert(SqlOpenHelper.TABLE_RESOURCES, null, values);
        db.close();
        return id;
    }

    public Resource getResourceById(int resourceId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_RESOURCES,
                null,
                SqlOpenHelper.KEY_RESOURCE_ID + "=?",
                new String[]{String.valueOf(resourceId)},
                null, null, null);

        Resource resource = null;
        if (cursor.moveToFirst()) {
            resource = new Resource();
            resource.setResourcesId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_ID)));
            resource.setResource(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_URL)));
            resource.setTutorId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_TUTOR_ID)));
            resource.setSubjectId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_SUBJECT_ID)));
            resource.setName(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_NAME)));
            cursor.close();
        }
        db.close();
        return resource;
    }

    public List<Resource> getResourcesByTutorId(int tutorId) {
        List<Resource> resources = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_RESOURCES,
                null,
                SqlOpenHelper.KEY_RESOURCE_TUTOR_ID + "=?",
                new String[]{String.valueOf(tutorId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Resource resource = new Resource();
                resource.setResourcesId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_ID)));
                resource.setResource(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_URL)));
                resource.setTutorId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_TUTOR_ID)));
                resource.setSubjectId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_SUBJECT_ID)));
                resource.setName(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_NAME)));
                resources.add(resource);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return resources;
    }

    public int updateResource(Resource resource) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_RESOURCE_URL, resource.getResource());
        values.put(SqlOpenHelper.KEY_RESOURCE_NAME, resource.getName());
        values.put(SqlOpenHelper.KEY_SUBJECT_ID, resource.getSubjectId());

        int rowsAffected = db.update(SqlOpenHelper.TABLE_RESOURCES, values,
                SqlOpenHelper.KEY_RESOURCE_ID + "=?",
                new String[]{String.valueOf(resource.getResourcesId())});
        db.close();
        return rowsAffected;
    }
    public int updateResource(String resource, int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SqlOpenHelper.KEY_RESOURCE_URL, resource);

        int rowsAffected = db.update(SqlOpenHelper.TABLE_RESOURCES, values,
                SqlOpenHelper.KEY_RESOURCE_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return rowsAffected;
    }

    public int deleteResource(int resourceId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsAffected = db.delete(SqlOpenHelper.TABLE_RESOURCES,
                SqlOpenHelper.KEY_RESOURCE_ID + "=?",
                new String[]{String.valueOf(resourceId)});
        db.close();
        return rowsAffected;
    }

    public List<Resource> getAllResources() {
        List<Resource> resources = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(SqlOpenHelper.TABLE_RESOURCES,
                null,
                null,
                null,
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Resource resource = new Resource();
                resource.setResourcesId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_ID)));
                resource.setResource(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_URL)));
                resource.setTutorId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_TUTOR_ID)));
                resource.setSubjectId(cursor.getInt(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_SUBJECT_ID)));
                resource.setName(cursor.getString(cursor.getColumnIndexOrThrow(SqlOpenHelper.KEY_RESOURCE_NAME)));
                resources.add(resource);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return resources;

    }
}