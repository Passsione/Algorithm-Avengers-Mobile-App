package com.pbdvmobile.app; // Make sure this package is correct

import android.app.Application;
import android.util.Log; // For logging
import com.google.firebase.FirebaseApp;
// Optional: For emulator setup, only if you are using them and BuildConfig.DEBUG
// import com.pbdvmobile.app.BuildConfig;
// import com.google.firebase.auth.FirebaseAuth;
// import com.google.firebase.firestore.FirebaseFirestore;
// import com.google.firebase.storage.FirebaseStorage;


public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // **THIS LINE IS CRITICAL**
        FirebaseApp.initializeApp(this);
        Log.d(TAG, "FirebaseApp.initializeApp(this) has been called.");

        // Optional: Emulator setup for DEBUG builds (ensure BuildConfig.DEBUG is valid)
        // Example (make sure your BuildConfig.DEBUG is correctly generated):
        /*
        if (BuildConfig.DEBUG) {
           Log.d(TAG, "DEBUG build detected. Attempting to connect to Firebase Emulators.");
           try {
               // It's often better to get instances *after* initializeApp
               FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);
               FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080);
               FirebaseStorage.getInstance().useEmulator("10.0.2.2", 9199);
               Log.d(TAG, "Firebase Emulators configured.");
           } catch (IllegalStateException e) {
               Log.w(TAG, "Failed to connect emulators. They might already be running for a different project or not set up correctly.", e);
           } catch (Exception e) {
               Log.e(TAG, "Generic exception while trying to connect emulators.", e);
           }
        } else {
           Log.d(TAG, "RELEASE build. Not connecting to Firebase Emulators.");
        }
        */
    }
}