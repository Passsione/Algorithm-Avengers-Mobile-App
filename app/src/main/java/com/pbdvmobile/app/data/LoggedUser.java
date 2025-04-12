package com.pbdvmobile.app.data;
import android.content.Context;

import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.model.User;

import java.util.Objects;

public class LoggedUser {

    private DataManager dataManager;
    private static LoggedUser instance;
    private User user;

    private LoggedUser (DataManager dataManager){ this.dataManager = dataManager;}
    public static synchronized LoggedUser getInstance(DataManager dataManager){
        if(instance == null){
            instance = new LoggedUser(dataManager);
        }
        return instance;
    }
    public User getUser(){return user;}
    public void setUser(User user){this.user = user;}
    public boolean logIn(User user){

        User attempt = dataManager.getUserDao().getUserByEmail(user.getEmail());
        if(attempt != null && Objects.equals(attempt.getPassword(), user.getPassword())){
            this.user = user;
            return  true;
        }
        return false;
    }
    public void logOut(){user = null;}
    public boolean isLogged(){return this.user != null;}
}



