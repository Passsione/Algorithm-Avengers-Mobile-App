package com.pbdvmobile.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        DataManager dataManager = DataManager.getInstance(this);
        LogInUser current_user = LogInUser.getInstance(dataManager);

        if(!current_user.isLoggedIn()){ // Go to log-in page
            Intent toLogin = new Intent(MainActivity.this, LogInActivity.class);
            startActivity(toLogin);
        }

        // Now you can use the DAOs for database operations
        // For example, to add a new user:
        /*User newUser = new User(22323800, "Mohale", "Tshehla");
        newUser.setAverageRating(5);
        newUser.setBio("I like yellow");
        newUser.setCredits(14);
        newUser.setEmail("22323800@dut4life.ac.za");
        newUser.setPassword("password1");
        newUser.setEducationLevel(User.EduLevel.DIP);
        long result = dataManager.getUserDao().insertUser(newUser);
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, result > 0? "USer added" : "Failed add", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
            }
        });*/

        //testing updating
        /*
        User mogaleUser = dataManager.getUserDao().getUserById(22323809);
        int updateId = dataManager.getUserDao().updateUserEmail(22323809, "22323809@dut4life.ac.za");
        User mogaleUser1 = dataManager.getUserDao().getUserById(22323809);

        // User found successfully
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, updateId > 0 ? "Welcome "+mogaleUser.getStudentNum()+
                                " old email: "+mogaleUser.getEmail()+
                                " new email: " + mogaleUser1.getEmail() : "Update fail", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
            }
        });
*/

        // Deleting works too
        // User deleted successfully
        /*int index = dataManager.getUserDao().deleteUser(22323800);
     binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, index > 0 ?"deleted user at index "+ index : "Error deleteing" , Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
            }
        });*/
    }
}