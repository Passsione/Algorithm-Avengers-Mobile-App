package com.pbdvmobile.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

// Android Cookbook pdf page: 485
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

    // User-Subject Join Table Columns
    public static final String KEY_USER_SUBJECT_USER_ID = "user_id";
    public static final String KEY_USER_SUBJECT_SUBJECT_ID = "subject_id";
    public static final String KEY_USER_SUBJECT_MARK = "mark";
    public static final String KEY_USER_SUBJECT_TUTORING = "tutoring";

    // Session Table Columns
    public static final String KEY_SESSION_ID = "session_id";
    public static final String KEY_SESSION_TUTOR_ID = "tutor_id";
    public static final String KEY_SESSION_TUTEE_IDS_JSON = "tutees_id";
//    public static final String KEY_SESSION_TUTEE_ID = "tutee_id";
    public static final String KEY_SESSION_SUBJECT_ID = "subject_id";
    public static final String KEY_SESSION_LOCATION = "location";
    public static final String KEY_SESSION_START_TIME = "start_time";
    public static final String KEY_SESSION_END_TIME = "end_time";
    public static final String KEY_SESSION_STATUS = "status";
    public static final String KEY_SESSION_TUTOR_REVIEW = "tutor_review";
    public static final String KEY_SESSION_TUTOR_RATING= "tutor_rating";
    public static final String KEY_SESSION_TUTEE_REVIEW = "tutee_review";
    public static final String KEY_SESSION_TUTEE_RATING = "tutee_rating";

    // Resources Table Columns
    public static final String KEY_RESOURCE_ID = "resource_id";
    public static final String KEY_RESOURCE_URL = "resource_url";
    public static final String KEY_RESOURCE_TUTOR_ID = "tutor_id";
    public static final String KEY_RESOURCE_SUBJECT_ID = "subject_id";
    public static final String KEY_RESOURCE_NAME = "resource_name";

    // Prize Table Columns
    public static final String KEY_PRIZE_ID = "prize_id";
    public static final String KEY_PRIZE_NAME = "prize_name";
    public static final String KEY_PRIZE_COST = "prize_cost";

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
    public void onCreate(@NonNull SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS +
                "(" +
                KEY_USER_STUDENT_NUM + " INTEGER PRIMARY KEY," +
                KEY_USER_FIRST_NAME + " VARCHAR(50)," +
                KEY_USER_LAST_NAME + " VARCHAR(50)," +
                KEY_USER_EMAIL + " VARCHAR(100) UNIQUE NOT NULL," +
                KEY_USER_PASSWORD + " TEXT NOT NULL," +
                KEY_USER_BIO + " TEXT," +
                KEY_USER_EDUCATION_LEVEL + " VARCHAR(25) NOT NULL," +
                KEY_USER_IS_TUTOR + " INTEGER DEFAULT 0," + // 0 for false, 1 for true
                KEY_USER_TIER_LEVEL + " VARCHAR(25)," +
                KEY_USER_AVERAGE_RATING + " REAL," +
                KEY_USER_PROFILE_IMAGE_URL + " TEXT," +
                KEY_USER_CREDITS + " REAL DEFAULT 10," +
                KEY_USER_SUBJECTS + " TEXT," + // JSON string, Android Cookbook pdf page: 503
                KEY_USER_BANK_DETAILS + " TEXT" + // JSON string
                ")";

        String CREATE_SUBJECTS_TABLE = "CREATE TABLE " + TABLE_SUBJECTS +
                "(" +
                KEY_SUBJECT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_SUBJECT_NAME + " TEXT" +

                ")";

        String CREATE_USER_SUBJECTS_TABLE = "CREATE TABLE " + TABLE_USER_SUBJECTS +
                "(" +
                KEY_USER_SUBJECT_USER_ID + " INTEGER," +
                KEY_USER_SUBJECT_SUBJECT_ID + " INTEGER," +
                KEY_USER_SUBJECT_MARK + " REAL DEFAULT 0," +
                KEY_USER_SUBJECT_TUTORING + " INTEGER DEFAULT 0," +
                "PRIMARY KEY (" + KEY_USER_SUBJECT_USER_ID + ", " + KEY_USER_SUBJECT_SUBJECT_ID + ")," +
                "FOREIGN KEY (" + KEY_USER_SUBJECT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_USER_STUDENT_NUM + ")," +
                "FOREIGN KEY (" + KEY_USER_SUBJECT_SUBJECT_ID + ") REFERENCES " + TABLE_SUBJECTS + "(" + KEY_SUBJECT_ID + ")" +
                ")";

        String CREATE_SESSIONS_TABLE = "CREATE TABLE " + TABLE_SESSIONS +
                "(" +
                KEY_SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_SESSION_TUTOR_ID + " INTEGER NOT NULL," +
                // KEY_SESSION_TUTEE_ID + " INTEGER," + // Optional: Keep for legacy 1-on-1 support? Decide based on needs.
                KEY_SESSION_TUTEE_IDS_JSON + " TEXT," + // Stores JSON array of tutee IDs
                KEY_SESSION_SUBJECT_ID + " INTEGER NOT NULL," +
                KEY_SESSION_LOCATION + " TEXT," +
                KEY_SESSION_START_TIME + " INTEGER NOT NULL," + // Store as Long (milliseconds)
                KEY_SESSION_END_TIME + " INTEGER," + // Store as Long (milliseconds)
                KEY_SESSION_STATUS + " TEXT NOT NULL," + // Changed from VARCHAR(15)
                KEY_SESSION_TUTOR_REVIEW + " TEXT," +
                KEY_SESSION_TUTEE_REVIEW + " TEXT," +
                KEY_SESSION_TUTOR_RATING + " REAL," + // Changed from REAL,
                KEY_SESSION_TUTEE_RATING + " REAL," + // Changed from REAL,
                "FOREIGN KEY (" + KEY_SESSION_TUTOR_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_USER_STUDENT_NUM + ")," +
                // Cannot have FK for a list in tutee_ids_json
                "FOREIGN KEY (" + KEY_SESSION_SUBJECT_ID + ") REFERENCES " + TABLE_SUBJECTS + "(" + KEY_SUBJECT_ID + ")" +
                ")";


        String CREATE_RESOURCES_TABLE = "CREATE TABLE " + TABLE_RESOURCES +
                "(" +
                KEY_RESOURCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_RESOURCE_URL + " TEXT," +
                KEY_RESOURCE_TUTOR_ID + " INTEGER," +
                KEY_RESOURCE_SUBJECT_ID + " INTEGER," +
                KEY_RESOURCE_NAME + " TEXT," +
                "FOREIGN KEY (" + KEY_RESOURCE_TUTOR_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_USER_STUDENT_NUM + ")," +
                "FOREIGN KEY (" + KEY_RESOURCE_SUBJECT_ID + ") REFERENCES " + TABLE_SUBJECTS + "(" + KEY_SUBJECT_ID + ")" +
                ")";

        String CREATE_PRIZES_TABLE = "CREATE TABLE " + TABLE_PRIZES +
                "(" +
                KEY_PRIZE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_PRIZE_NAME + " TEXT," +
                KEY_PRIZE_COST + "REAL DEFAULT 1"+
                ")";

        String CREATE_REDEEM_PRIZES_TABLE = "CREATE TABLE " + TABLE_REDEEM_PRIZES +
                "(" +
                KEY_REDEEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_REDEEM_STUDENT_NUM + " INTEGER," +
                KEY_REDEEM_PRIZE_ID + " INTEGER," +
                KEY_REDEEM_STATUS + " VARCHAR(10) DEFAULT 'PENDING'," +
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

    public void dropAll(@NonNull SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REDEEM_PRIZES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRIZES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESOURCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_SUBJECTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
    public void dropTable(@NonNull SQLiteDatabase db, String name) {
        db.execSQL("DROP TABLE IF EXISTS " + name);
        onCreate(db);

    }
}