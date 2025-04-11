package com.pbdvmobile.app.ui;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.databinding.ActivityMainBinding;
import com.pbdvmobile.app.data.DataManager;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);


        // Get DataManager instance
        DataManager dataManager = DataManager.getInstance(this);

        // Now you can use the DAOs for database operations
        // For example, to add a new user:
        /*User newUser = new User(22323801, "Mogale", "Tshehla");
        newUser.setAverageRating(10);
        newUser.setBio("Testing that the database works");
        newUser.setCredits(14);
        newUser.setEmail("22323801@dut4life.ac.za");
        newUser.setPassword("password1");
        newUser.setEducationLevel(User.EduLevel.BACHELOR);
        long result = dataManager.getUserDao().insertUser(newUser);
        if (result > 0) {
            // User added successfully
        }*/
        //testing updating
        /*

        User mogaleUser = dataManager.getUserDao().getUserById(22323809);
        int mogaleUpdate = dataManager.getUserDao().updateUserEmail(22323809, "22323809@dut4life.ac.za");
        User mogaleUser1 = dataManager.getUserDao().getUserById(22323809);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Teaster ", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
            }
        });
        if (mogaleUpdate != -1) {
            // User found successfully
            binding.fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Welcome "+mogaleUser.getStudentNum()+
                                    " old email: "+mogaleUser.getEmail()+
                                    " new email: " + mogaleUser1.getEmail(), Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.fab)
                            .setAction("Action", null).show();
                }
            });
        }else{
            binding.fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Not found ", Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.fab)
                            .setAction("Action", null).show();
                }
            });
        }
*/
        // Deleting works too
        /*
        int index = dataManager.getUserDao().deleteUser(22323801);
        if(index != -1){
            // User deleted successfully
            binding.fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "deleted user at index "+ index, Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.fab)
                            .setAction("Action", null).show();
                }
            });
        }*/



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


}