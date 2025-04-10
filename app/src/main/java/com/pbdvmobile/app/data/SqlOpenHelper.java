package com.pbdvmobile.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SqlOpenHelper extends SQLiteOpenHelper {
    // Database Info
    private static final String DATABASE_NAME = "PeerTutorDB";
    private static final int DATABASE_VERSION = 1;

    // Singleton instance
    private static SqlOpenHelper instance;

    // Table Names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_SUBJECTS = "subjects";
    public static final String TABLE_SESSIONS = "sessions";
    public static final String TABLE_RESOURCES = "resources";
    public static final String TABLE_PRIZES = "prizes";
    public static final String TABLE_REDEEM_PRIZES = "redeem_prizes";
    public static final String TABLE_USER_SUBJECTS = "user_subjects";
    public static final String TABLE_CHAT = "chats";


    // User Table Columns
    public static final String KEY_USER_STUDENT_NUM = "student_num";
    public static final String KEY_USER_FIRST_NAME = "first_name";
    public static final String KEY_USER_LAST_NAME = "last_name";
    public static final String KEY_USER_EMAIL = "email";
    public static final String KEY_USER_PASSWORD = "password";
    public static final String KEY_USER_BIO = "bio";
    public static final String KEY_USER_EDUCATION_LEVEL = "education_level";
    public static final String KEY_USER_IS_TUTOR = "is_tutor";
    public static final String KEY_USER_TIER_LEVEL = "tier_level";
    public static final String KEY_USER_AVERAGE_RATING = "average_rating";
    public static final String KEY_USER_PROFILE_IMAGE_URL = "profile_image_url";
    public static final String KEY_USER_CREDITS = "credits";
    public static final String KEY_USER_SUBJECTS = "subjects";
    public static final String KEY_USER_BANK_DETAILS = "bank_details";

    // Subject Table Columns
    public static final String KEY_SUBJECT_ID = "subject_id";
    public static final String KEY_SUBJECT_NAME = "subject_name";
    public static final String KEY_SUBJECT_MARK = "mark";

    // User-Subject Join Table Columns
    public static final String KEY_USER_SUBJECT_USER_ID = "user_id";
    public static final String KEY_USER_SUBJECT_SUBJECT_ID = "subject_id";
    public static final String KEY_USER_SUBJECT_MARK = "mark";

    // Session Table Columns
    public static final String KEY_SESSION_ID = "session_id";
    public static final String KEY_SESSION_TUTOR_ID = "tutor_id";
    public static final String KEY_SESSION_TUTEE_ID = "tutee_id";
    public static final String KEY_SESSION_SUBJECT_ID = "subject_id";
    public static final String KEY_SESSION_LOCATION = "location";
    public static final String KEY_SESSION_START_TIME = "start_time";
    public static final String KEY_SESSION_END_TIME = "end_time";
    public static final String KEY_SESSION_STATUS = "status";
    public static final String KEY_SESSION_TUTOR_REVIEW = "tutor_review";
    public static final String KEY_SESSION_TUTEE_REVIEW = "tutee_review";

    // Resources Table Columns
    public static final String KEY_RESOURCE_ID = "resource_id";
    public static final String KEY_RESOURCE_URL = "resource_url";
    public static final String KEY_RESOURCE_TUTOR_ID = "tutor_id";

    // Prize Table Columns
    public static final String KEY_PRIZE_ID = "prize_id";
    public static final String KEY_PRIZE_NAME = "prize_name";

    // RedeemPrize Table Columns
    public static final String KEY_REDEEM_ID = "redeem_id";
    public static final String KEY_REDEEM_STUDENT_NUM = "student_num";
    public static final String KEY_REDEEM_PRIZE_ID = "prize_id";
    public static final String KEY_REDEEM_STATUS = "status";

    // Constructor should be private to prevent direct instantiation.
    // Make a call to the static method "getInstance()" instead.
    private SqlOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Singleton pattern
    public static synchronized SqlOpenHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SqlOpenHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS +
                "(" +
                KEY_USER_STUDENT_NUM + " INTEGER PRIMARY KEY," +
                KEY_USER_FIRST_NAME + " TEXT," +
                KEY_USER_LAST_NAME + " TEXT," +
                KEY_USER_EMAIL + " TEXT UNIQUE," +
                KEY_USER_PASSWORD + " TEXT," +
                KEY_USER_BIO + " TEXT," +
                KEY_USER_EDUCATION_LEVEL + " TEXT," +
                KEY_USER_IS_TUTOR + " INTEGER," + // 0 for false, 1 for true
                KEY_USER_TIER_LEVEL + " TEXT," +
                KEY_USER_AVERAGE_RATING + " REAL," +
                KEY_USER_PROFILE_IMAGE_URL + " TEXT," +
                KEY_USER_CREDITS + " REAL," +
                KEY_USER_SUBJECTS + " TEXT," + // JSON string
                KEY_USER_BANK_DETAILS + " TEXT" +
                ")";

        String CREATE_SUBJECTS_TABLE = "CREATE TABLE " + TABLE_SUBJECTS +
                "(" +
                KEY_SUBJECT_ID + " INTEGER PRIMARY KEY," +
                KEY_SUBJECT_NAME + " TEXT" +

                ")";

        String CREATE_USER_SUBJECTS_TABLE = "CREATE TABLE " + TABLE_USER_SUBJECTS +
                "(" +
                KEY_USER_SUBJECT_USER_ID + " INTEGER," +
                KEY_USER_SUBJECT_SUBJECT_ID + " INTEGER," +
                KEY_USER_SUBJECT_MARK + " REAL," +
                "PRIMARY KEY (" + KEY_USER_SUBJECT_USER_ID + ", " + KEY_USER_SUBJECT_SUBJECT_ID + ")," +
                "FOREIGN KEY (" + KEY_USER_SUBJECT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_USER_STUDENT_NUM + ")," +
                "FOREIGN KEY (" + KEY_USER_SUBJECT_SUBJECT_ID + ") REFERENCES " + TABLE_SUBJECTS + "(" + KEY_SUBJECT_ID + ")" +
                ")";

        String CREATE_SESSIONS_TABLE = "CREATE TABLE " + TABLE_SESSIONS +
                "(" +
                KEY_SESSION_ID + " INTEGER PRIMARY KEY," +
                KEY_SESSION_TUTOR_ID + " INTEGER," +
                KEY_SESSION_TUTEE_ID + " INTEGER," +
                KEY_SESSION_SUBJECT_ID + " INTEGER," +
                KEY_SESSION_LOCATION + " TEXT," +
                KEY_SESSION_START_TIME + " INTEGER," + // Stored as Unix timestamp (milliseconds)
                KEY_SESSION_END_TIME + " INTEGER," + // Stored as Unix timestamp (milliseconds)
                KEY_SESSION_STATUS + " TEXT," +
                KEY_SESSION_TUTOR_REVIEW + " TEXT," +
                KEY_SESSION_TUTEE_REVIEW + " TEXT," +
                "FOREIGN KEY (" + KEY_SESSION_TUTOR_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_USER_STUDENT_NUM + ")," +
                "FOREIGN KEY (" + KEY_SESSION_TUTEE_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_USER_STUDENT_NUM + ")," +
                "FOREIGN KEY (" + KEY_SESSION_SUBJECT_ID + ") REFERENCES " + TABLE_SUBJECTS + "(" + KEY_SUBJECT_ID + ")" +
                ")";

        String CREATE_RESOURCES_TABLE = "CREATE TABLE " + TABLE_RESOURCES +
                "(" +
                KEY_RESOURCE_ID + " INTEGER PRIMARY KEY," +
                KEY_RESOURCE_URL + " TEXT," +
                KEY_RESOURCE_TUTOR_ID + " INTEGER," +
                "FOREIGN KEY (" + KEY_RESOURCE_TUTOR_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_USER_STUDENT_NUM + ")" +
                ")";

        String CREATE_PRIZES_TABLE = "CREATE TABLE " + TABLE_PRIZES +
                "(" +
                KEY_PRIZE_ID + " INTEGER PRIMARY KEY," +
                KEY_PRIZE_NAME + " TEXT" +
                ")";

        String CREATE_REDEEM_PRIZES_TABLE = "CREATE TABLE " + TABLE_REDEEM_PRIZES +
                "(" +
                KEY_REDEEM_ID + " INTEGER PRIMARY KEY," +
                KEY_REDEEM_STUDENT_NUM + " INTEGER," +
                KEY_REDEEM_PRIZE_ID + " INTEGER," +
                KEY_REDEEM_STATUS + " TEXT," +
                "FOREIGN KEY (" + KEY_REDEEM_STUDENT_NUM + ") REFERENCES " + TABLE_USERS + "(" + KEY_USER_STUDENT_NUM + ")," +
                "FOREIGN KEY (" + KEY_REDEEM_PRIZE_ID + ") REFERENCES " + TABLE_PRIZES + "(" + KEY_PRIZE_ID + ")" +
                ")";

        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_SUBJECTS_TABLE);
        db.execSQL(CREATE_USER_SUBJECTS_TABLE);
        db.execSQL(CREATE_SESSIONS_TABLE);
        db.execSQL(CREATE_RESOURCES_TABLE);
        db.execSQL(CREATE_PRIZES_TABLE);
        db.execSQL(CREATE_REDEEM_PRIZES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            // Simplest implementation is to drop all old tables and recreate them
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_REDEEM_PRIZES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRIZES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESOURCES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_SUBJECTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
    }
}