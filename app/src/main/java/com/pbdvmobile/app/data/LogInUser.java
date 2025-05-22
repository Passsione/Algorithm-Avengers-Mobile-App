/*
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



*/
package com.pbdvmobile.app.data;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User; // Your User POJO

import java.io.Serializable;

public class LogInUser implements Serializable {

    private static LogInUser instance;
    // This 'user' POJO will now be fetched from Firestore after successful Firebase Auth.
    // It's for holding app-specific user details, not for auth state.
    private User userPojo;
    public String message; // For passing transient messages between activities (consider replacing with ViewModel/LiveData)

    // Constructor is private for Singleton
    private LogInUser() {}

    // No DataManager needed for getInstance if it's only for auth state
    public static synchronized LogInUser getInstance() {
        if (instance == null) {
            instance = new LogInUser();
            SessionDao sDao = new SessionDao();
            sDao.updateSessionStatus("OJoHInlR6ZabvulKghHw", Session.Status.COMPLETED);
        }
        return instance;
    }

    // Get the currently authenticated FirebaseUser
    public FirebaseUser getFirebaseUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    // Check if a user is authenticated with Firebase
    public boolean isLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null && userPojo != null;
    }

    // Get Firebase User ID
    public String getUid() {
        FirebaseUser fbUser = getFirebaseUser();
        return (fbUser != null) ? fbUser.getUid() : null;
    }

    // Get Firebase User Email
    public String getEmail() {
        FirebaseUser fbUser = getFirebaseUser();
        return (fbUser != null) ? fbUser.getEmail() : null;
    }

    // The old logIn(email, password) method is removed.
    // Login is handled by FirebaseAuth.signInWithEmailAndPassword in LogInActivity.

    public void logOut() {
        FirebaseAuth.getInstance().signOut();
        this.userPojo = null; // Clear cached user details
    }

    // Methods to get/set the User POJO (fetched from Firestore)
    public User getUser() { // Renamed from getUserPojo for consistency with your old code
        return userPojo;
    }

    public void setUser(User user) { // Renamed from setUserPojo
        this.userPojo = user;
    }
}
