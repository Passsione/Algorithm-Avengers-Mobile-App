package com.pbdvmobile.app.data;

import android.widget.EditText;

import com.pbdvmobile.app.data.model.User;

import java.util.Objects;
import java.util.regex.Pattern;

public class LogInUser {

    private DataManager dataManager;
    private static LogInUser instance;
    private User user;

    private LogInUser(DataManager dataManager){ this.dataManager = dataManager;}
    public static synchronized LogInUser getInstance(DataManager dataManager){
        if(instance == null){
            instance = new LogInUser(dataManager);
        }
        return instance;
    }
    public User getUser(){return user;}
    public void setUser(User user){this.user = user;}

    public boolean logIn(User user){
        if(user != null) {
            User attempt = dataManager.getUserDao().getUserByEmail(user.getEmail());
            if (attempt != null && Objects.equals(attempt.getPassword(), user.getPassword())) {
                this.user = user;
                return true;
            }
            return false;
        }return false;
    }
    public void logOut(){this.user = null;}
    public boolean isLoggedIn(){return this.user != null;}


}



