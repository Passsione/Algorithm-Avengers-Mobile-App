package com.pbdvmobile.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.ui.AppBarConfiguration;

import com.google.android.material.snackbar.Snackbar;
import com.pbdvmobile.app.data.LoggedUser;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.databinding.ActivityMainBinding;
import com.pbdvmobile.app.data.DataManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    DataManager dataManager;
    private LoggedUser current_user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dataManager = DataManager.getInstance(this);
        current_user = LoggedUser.getInstance(dataManager);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);


//        Button btnNavigate = findViewById(R.id.btnNavigate);
        User mogale = new User(22323809, "Mogale", "Tshehla");
        mogale.setEmail("22323809@dut4life.ac.za");
        mogale.setPassword("password1");



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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean LoggedIn(User user){
        if (current_user.getUser() != null) {
            Intent intent = new Intent(MainActivity.this, LogInActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

}