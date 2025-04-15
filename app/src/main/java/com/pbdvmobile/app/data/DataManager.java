package com.pbdvmobile.app.data;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.EditText;

import com.pbdvmobile.app.data.dao.PrizeDao;
import com.pbdvmobile.app.data.dao.ResourceDao;
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.dao.UserDao;

import java.util.regex.Pattern;

public class DataManager {
    private static DataManager instance;
    private final SqlOpenHelper dbHelper;
    private final UserDao userDao;
    private final SubjectDao subjectDao;
    private final SessionDao sessionDao;
    private final ResourceDao resourceDao;
    private final PrizeDao prizeDao;

    private DataManager(Context context) {
        dbHelper = SqlOpenHelper.getInstance(context);
        userDao = new UserDao(dbHelper);
        subjectDao = new SubjectDao(dbHelper);
        sessionDao = new SessionDao(dbHelper);
        resourceDao = new ResourceDao(dbHelper);
        prizeDao = new PrizeDao(dbHelper);
    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context.getApplicationContext());
        }
        return instance;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public SubjectDao getSubjectDao() {
        return subjectDao;
    }

    public SessionDao getSessionDao() {
        return sessionDao;
    }

    public ResourceDao getResourceDao() {
        return resourceDao;
    }

    public PrizeDao getPrizeDao() {
        return prizeDao;
    }

    // Sample usage in activity
    public void initialize() {

        // This method would be called once when the app starts
        // It can handle any initial database setup or data insertion


    }

    public boolean required(EditText...args){
        for (EditText et: args) {
            if(et.getText().toString().isEmpty()){
//                et.setError("Fill in");
                return false;
            }
        }
        return true;
    }
    public boolean validDut(String email){
        Pattern pattern = Pattern.compile("[0-9]{8}+@dut4life.ac.za");
        return pattern.matcher(email).matches();
    }

}