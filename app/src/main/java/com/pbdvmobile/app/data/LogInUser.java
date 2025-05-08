package com.pbdvmobile.app.data;

import com.pbdvmobile.app.data.model.User;

import java.io.Serializable;
import java.util.Objects;

public class LogInUser implements Serializable {

    private final DataManager dataManager;
    private static LogInUser instance;
    private User user;
    public String message;
    private LogInUser(DataManager dataManager){ this.dataManager = dataManager;}
    public static synchronized LogInUser getInstance(DataManager dataManager){
        if(instance == null){
            instance = new LogInUser(dataManager);
        }
        return instance;
    }
    public User getUser(){return user;}
    public void setUser(User user){this.user = user;}

    public boolean logIn(String email, String password){

        User attempt = dataManager.getUserDao().getUserByEmail(email);
        if (attempt != null && Objects.equals(attempt.getPassword(), password)) {
            this.user = attempt;
            return true;
        }
        return false;
    }
    public void logOut(){this.user = null;}
    public boolean isLoggedIn(){return this.user != null;}


}



