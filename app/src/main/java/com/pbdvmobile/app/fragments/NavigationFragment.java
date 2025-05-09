package com.pbdvmobile.app.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import android.view.Menu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.pbdvmobile.app.AIBaseActivity;
import com.pbdvmobile.app.AIQuizActivity;
import com.pbdvmobile.app.LogInActivity;
import com.pbdvmobile.app.MainActivity;
import com.pbdvmobile.app.NotificationsActivity;
import com.pbdvmobile.app.PaymentGatewayActivity;
import com.pbdvmobile.app.PaymentHistoryActivity;
import com.pbdvmobile.app.ProfileActivity;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.RedeemCreditActivity;
import com.pbdvmobile.app.ScheduleActivity;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;

public class NavigationFragment extends Fragment {

    ImageButton menuswitch;
    NavigationView nav;
    TextView nav_header, nav_header_email, flash;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_navigation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DataManager dataManager = DataManager.getInstance(getContext());
        LogInUser current_user = LogInUser.getInstance(dataManager);

        // ---- Go to log-in page if not logged in
        if(!current_user.isLoggedIn()){
            Intent toLogin = new Intent(getActivity(), LogInActivity.class);
            startActivity(toLogin);
        }


        /* ---- Start - side navigation section */

        ConstraintLayout host = view.findViewById(R.id.nav_host);
        // Side navigation Switch
        menuswitch = view.findViewById(R.id.imgMenu);


        flash = view.findViewById(R.id.txtError);

        host.setOnClickListener(v -> {
            host.setVisibility(View.GONE);

        });
        menuswitch.setOnClickListener(v -> {
            host.setVisibility(View.VISIBLE);
        });

        // Side navigation
        nav = view.findViewById(R.id.nav_view);
        var headerView = nav.getHeaderView(0);
        nav_header = headerView.findViewById(R.id.nav_header_name);
        nav_header_email = headerView.findViewById(R.id.nav_header_email);

        // Handle potential null values for first and last name
        String firstName = current_user.getUser().getFirstName();
        String lastName = current_user.getUser().getLastName();
        String email = current_user.getUser().getEmail();
        String fullName = "";
        if(email == null) email = "";
        if(firstName != null) fullName += firstName;
        if(lastName != null) fullName += " " + lastName;

        nav_header.setText(fullName.trim());
        nav_header_email.setText(email.trim());

        //navigation menu
        Menu menu = nav.getMenu();
        // To Home page
        menu.findItem(R.id.nav_home).setVisible(!getActivity().getClass().getSimpleName().equals("MainActivity"));
        menu.findItem(R.id.nav_home).setOnMenuItemClickListener(v ->{
            Intent toHome = new Intent(getActivity(), MainActivity.class);
            startActivity(toHome);
            return false;
        });

        // To profile page
        menu.findItem(R.id.nav_profile).setVisible(!getActivity().getClass().getSimpleName().equals("ProfileActivity"));
        menu.findItem(R.id.nav_profile).setOnMenuItemClickListener(v ->{
            Intent toProfile = new Intent(getActivity(), ProfileActivity.class);
            startActivity(toProfile);
            return false;
        });

        // To Schedule History page
        menu.findItem(R.id.nav_schedule).setOnMenuItemClickListener(v ->{
            Intent toHistory = new Intent(getActivity(), ScheduleActivity.class);
            toHistory.putExtra("job_type", "session_history");
            startActivity(toHistory);
            return false;
        });

        // To Notifications page
        menu.findItem(R.id.nav_notifications).setOnMenuItemClickListener(v ->{
            Intent toNotifications = new Intent(getActivity(), NotificationsActivity.class);
            startActivity(toNotifications);
            return false;
        });
        // To AI Quiz page
        menu.findItem(R.id.nav_quiz_generator).setOnMenuItemClickListener(v ->{
            Intent toAIQuiz = new Intent(getActivity(), AIQuizActivity.class);
            startActivity(toAIQuiz);
            return false;
        });
        // To AI Summarizer page
        menu.findItem(R.id.nav_ai_summary).setOnMenuItemClickListener(v ->{
            Intent toAISummarizer = new Intent(getActivity(), AIBaseActivity.class);
            startActivity(toAISummarizer);
            return false;
        });
        // To Redeem Credits
        menu.findItem(R.id.nav_redeem_credits).setOnMenuItemClickListener(v ->{
            Intent toRedeem = new Intent(getActivity(), RedeemCreditActivity.class);
            startActivity(toRedeem);
            return false;
        });
        // To payments gateway
        menu.findItem(R.id.nav_payments_details).setOnMenuItemClickListener(v ->{
            Intent toPayment = new Intent(getActivity(), PaymentGatewayActivity.class);
            startActivity(toPayment);
            return false;
        });
        // To payments history
        menu.findItem(R.id.nav_payments_history).setOnMenuItemClickListener(v ->{
            Intent toPayHistory = new Intent(getActivity(), PaymentHistoryActivity.class);
            startActivity(toPayHistory);
            return false;
        });

        // Logout
        menu.findItem(R.id.nav_logout).setOnMenuItemClickListener(v ->{
            current_user.logOut();
            Toast.makeText(getContext(), "Successfully logged out", Toast.LENGTH_LONG).show();
            Intent toLogin = new Intent(getActivity(), LogInActivity.class);
            startActivity(toLogin);
            getActivity().finish();
            return false;
        });

        /* ---- End - side navigation section */

    }
}