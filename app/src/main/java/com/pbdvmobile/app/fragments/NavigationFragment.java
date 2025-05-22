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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth; // Added
import com.pbdvmobile.app.AIBaseActivity;
import com.pbdvmobile.app.AIQuizActivity;
import com.pbdvmobile.app.LogInActivity;
import com.pbdvmobile.app.MainActivity;
import com.pbdvmobile.app.NotificationsActivity; // Assuming this activity exists
import com.pbdvmobile.app.PaymentGatewayActivity; // Assuming this activity exists
import com.pbdvmobile.app.PaymentHistoryActivity; // Assuming this activity exists
import com.pbdvmobile.app.ProfileActivity;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.RedeemCreditActivity; // Assuming this activity exists
import com.pbdvmobile.app.ScheduleActivity;
import com.pbdvmobile.app.data.DataManager; // Still used for CREDIT_AI constants
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.User;

public class NavigationFragment extends Fragment {

    private ImageButton menuSwitchButton;
    private NavigationView navigationView;
    private ConstraintLayout navHostLayout;
    private TextView navHeaderName, navHeaderEmail; // Removed flash TextView
    private ImageView navHeaderImage;

    private LogInUser loggedInUserInstance;
    private User currentUserAppModel;
    private DataManager dataManager; // For AI credit constants

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_navigation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null) return;

        loggedInUserInstance = LogInUser.getInstance();
        currentUserAppModel = loggedInUserInstance.getUser();
        dataManager = DataManager.getInstance(getContext()); // For credit constants

        // Authentication check
        if (!loggedInUserInstance.isLoggedIn() || currentUserAppModel == null) {
            Toast.makeText(getContext(), "User not fully logged in. Redirecting.", Toast.LENGTH_SHORT).show();
            Intent toLogin = new Intent(getActivity(), LogInActivity.class);
            toLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(toLogin);
            if (getActivity() != null) getActivity().finishAffinity();
            return;
        }

        initializeViews(view);
        setupNavigationHeader();
        setupNavigationMenu();
        setupMenuToggle();
    }

    private void initializeViews(View view) {
        navHostLayout = view.findViewById(R.id.nav_host);
        menuSwitchButton = view.findViewById(R.id.imgMenu);
        navigationView = view.findViewById(R.id.nav_view);

        View headerView = navigationView.getHeaderView(0);
        navHeaderName = headerView.findViewById(R.id.nav_header_name);
        navHeaderEmail = headerView.findViewById(R.id.nav_header_email);
        navHeaderImage = headerView.findViewById(R.id.nav_header_image);
    }

    private void setupNavigationHeader() {
        String firstName = currentUserAppModel.getFirstName();
        String lastName = currentUserAppModel.getLastName();
        String email = currentUserAppModel.getEmail();
        String fullName = "";

        if (firstName != null) fullName += firstName;
        if (lastName != null) fullName += (fullName.isEmpty() ? "" : " ") + lastName;

        navHeaderName.setText(fullName.trim().isEmpty() ? "User" : fullName.trim());
        navHeaderEmail.setText(email != null ? email.trim() : "No email");

        Glide.with(requireContext())
                .load(currentUserAppModel.getProfileImageUrl())
                .placeholder(R.mipmap.ic_launcher_round) // Consider a more generic placeholder
                .error(R.mipmap.ic_launcher_round)       // And a specific error image
                .circleCrop()
                .into(navHeaderImage);
    }

    private void setupMenuToggle() {
        navHostLayout.setOnClickListener(v -> navHostLayout.setVisibility(View.GONE));
        menuSwitchButton.setOnClickListener(v -> {
            if (navHostLayout.getVisibility() == View.VISIBLE) {
                navHostLayout.setVisibility(View.GONE);
            } else {
                navHostLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupNavigationMenu() {
        Menu menu = navigationView.getMenu();
        String currentActivityName = getActivity() != null ? getActivity().getClass().getSimpleName() : "";

        // Home
        menu.findItem(R.id.nav_home).setVisible(!currentActivityName.equals(MainActivity.class.getSimpleName()));
        menu.findItem(R.id.nav_home).setOnMenuItemClickListener(item -> navigateTo(MainActivity.class));

        // Profile
        menu.findItem(R.id.nav_profile).setVisible(!currentActivityName.equals(ProfileActivity.class.getSimpleName()));
        menu.findItem(R.id.nav_profile).setOnMenuItemClickListener(item -> navigateTo(ProfileActivity.class));

        // Schedule History
        menu.findItem(R.id.nav_schedule).setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(getActivity(), ScheduleActivity.class);
            intent.putExtra("job_type", "session_history");
            startActivityAndCloseDrawer(intent);
            return true;
        });

        // Notifications
        menu.findItem(R.id.nav_notifications).setOnMenuItemClickListener(item -> navigateTo(NotificationsActivity.class));

        // AI Quiz
        menu.findItem(R.id.nav_quiz_generator).setOnMenuItemClickListener(item -> {
            if (currentUserAppModel.getCredits() >= DataManager.CREDIT_AI_QUIZ) {
                navigateTo(AIQuizActivity.class);
            } else {
                Toast.makeText(getContext(), "You need " + DataManager.CREDIT_AI_QUIZ + " credits.", Toast.LENGTH_LONG).show();
            }
            return true;
        });

        // AI Summarizer
        menu.findItem(R.id.nav_ai_summary).setOnMenuItemClickListener(item -> {
            if (currentUserAppModel.getCredits() >= DataManager.CREDIT_AI_SUMMARIZER) {
                navigateTo(AIBaseActivity.class); // Assuming AIBaseActivity is the summarizer
            } else {
                Toast.makeText(getContext(), "You need " + DataManager.CREDIT_AI_SUMMARIZER + " credits.", Toast.LENGTH_LONG).show();
            }
            return true;
        });

        // Redeem Credits
        menu.findItem(R.id.nav_redeem_credits).setOnMenuItemClickListener(item -> navigateTo(RedeemCreditActivity.class));

        // Payment Details
        menu.findItem(R.id.nav_payments_details).setOnMenuItemClickListener(item -> navigateTo(PaymentGatewayActivity.class));

        // Payment History
        menu.findItem(R.id.nav_payments_history).setOnMenuItemClickListener(item -> navigateTo(PaymentHistoryActivity.class));

        // Logout
        menu.findItem(R.id.nav_logout).setOnMenuItemClickListener(item -> {
            loggedInUserInstance.logOut(); // Clears Firebase Auth & local User POJO
            Toast.makeText(getContext(), "Successfully logged out", Toast.LENGTH_LONG).show();
            Intent toLogin = new Intent(getActivity(), LogInActivity.class);
            toLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(toLogin);
            if (getActivity() != null) getActivity().finishAffinity();
            return true;
        });
    }

    private boolean navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(getActivity(), activityClass);
        startActivityAndCloseDrawer(intent);
        return true;
    }

    private void startActivityAndCloseDrawer(Intent intent) {
        if (navHostLayout != null) navHostLayout.setVisibility(View.GONE);
        startActivity(intent);
        // Consider if current activity should finish for certain navigations
        // if (getActivity() != null && !(getActivity() instanceof MainActivity)) {
        //    getActivity().finish();
        // }
    }
}
