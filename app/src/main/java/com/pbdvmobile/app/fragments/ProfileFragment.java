package com.pbdvmobile.app.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.EditProfileActivity;
import com.pbdvmobile.app.LoginActivity;
import com.pbdvmobile.app.ResourceUploadActivity;
import com.pbdvmobile.app.adapters.ResourceAdapter;
import com.pbdvmobile.app.adapters.ReviewAdapter;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.Review;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.AuthService;
import com.pbdvmobile.app.data.ResourceService;
import com.pbdvmobile.app.data.ReviewService;
import com.pbdvmobile.app.data.UserService;
import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private ImageView ivProfile;
    private TextView tvName, tvEducation, tvSubjects, tvPrice, tvBio, tvType, tvCredits;
    private Button btnEdit, btnAddResource, btnLogout;
    private RecyclerView rvReviews, rvResources;

    private ReviewAdapter reviewAdapter;
    private ResourceAdapter resourceAdapter;
    private List<Review> reviewList;
    private List<Resource> resourceList;

    private UserService userService;
    private ReviewService reviewService;
    private ResourceService resourceService;
    private AuthService authService;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        userService = new UserService();
        reviewService = new ReviewService();
        resourceService = new ResourceService();
        authService = new AuthService();
        currentUser = userService.getCurrentUser();

        ivProfile = view.findViewById(R.id.iv_profile);
        tvName = view.findViewById(R.id.tv_name);
        tvEducation = view.findViewById(R.id.tv_education);
        tvSubjects = view.findViewById(R.id.tv_subjects);
        tvPrice = view.findViewById(R.id.tv_price);
        tvBio = view.findViewById(R.id.tv_bio);
        tvType = view.findViewById(R.id.tv_type);
        tvCredits = view.findViewById(R.id.tv_credits);
        btnEdit = view.findViewById(R.id.btn_edit);
        btnAddResource = view.findViewById(R.id.btn_add_resource);
        btnLogout = view.findViewById(R.id.btn_logout);
        rvReviews = view.findViewById(R.id.rv_reviews);
        rvResources = view.findViewById(R.id.rv_resources);

        // Initialize lists and adapters
        reviewList = new ArrayList<>();
        resourceList = new ArrayList<>();

        reviewAdapter = new ReviewAdapter(reviewList);
        resourceAdapter = new ResourceAdapter(resourceList, getContext());

        rvReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        rvReviews.setAdapter(reviewAdapter);

        rvResources.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvResources.setAdapter(resourceAdapter);

        // Setup UI based on user type
        setupUserTypeSpecificUI();

        // Load user data
        loadUserData();

        // Button click listeners
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), EditProfileActivity.class));
            }
        });

        btnAddResource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), ResourceUploadActivity.class));
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authService.logout();
                startActivity(new Intent(getContext(), LoginActivity.class));
                getActivity().finish();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user data in case it was updated
        loadUserData();
    }

    private void setupUserTypeSpecificUI() {
        if (currentUser.getUserType() == User.UserType.TUTOR) {
            tvType.setText("Tutor Profile");
            tvPrice.setVisibility(View.VISIBLE);
        } else {
            tvType.setText("Tutee Profile");
            tvPrice.setVisibility(View.GONE);
            btnAddResource.setVisibility(View.GONE);
        }
    }

    private void loadUserData() {
        // Refresh current user data
        userService.refreshUserData(new UserService.UserDetailCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                displayUserDetails();

                // Load reviews
                reviewService.getUserReviews(currentUser.getId(), new ReviewService.ReviewsCallback() {
                    @Override
                    public void onSuccess(List<Review> reviews) {
                        reviewList.clear();
                        reviewList.addAll(reviews);
                        reviewAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(String error) {
                        // Handle error
                    }
                });

                // Load resources (for tutors only)
                if (currentUser.getUserType() == User.UserType.TUTOR) {
                    resourceService.getUserResources(currentUser.getId(), new ResourceService.ResourcesCallback() {
                        @Override
                        public void onSuccess(List<Resource> resources) {
                            resourceList.clear();
                            resourceList.addAll(resources);
                            resourceAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailure(String error) {
                            // Handle error
                        }
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                // Handle error
            }
        });
    }

    private void displayUserDetails() {
        tvName.setText(currentUser.getFullName());
        tvEducation.setText(currentUser.getEducationLevel());
        tvBio.setText(currentUser.getBio());
        tvCredits.setText("Credits: " + currentUser.getCredits());

        if (currentUser.getUserType() == User.UserType.TUTOR) {
            tvPrice.setText("$" + currentUser.getHourlyRate() + "/hr");
        }

        // Build subjects text
        StringBuilder subjectsText = new StringBuilder();
        for (Subject subject : currentUser.getSubjects()) {
            if (subjectsText.length() > 0) {
                subjectsText.append(", ");
            }
            subjectsText.append(subject.getName());
        }
        tvSubjects.setText(subjectsText.toString());

        // Load profile image
        if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentUser.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.default_profile)
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.default_profile);
        }
    }
}
