package com.pbdvmobile.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.pbdvmobile.app.adapters.ReviewAdapter;
import com.pbdvmobile.app.adapters.ResourceAdapter;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.Review;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.services.ResourceService;
import com.pbdvmobile.app.services.ReviewService;
import com.pbdvmobile.app.services.UserService;
import java.util.ArrayList;
import java.util.List;

public class TutorDetailActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private TextView tvName, tvEducation, tvSubjects, tvPrice, tvBio;
    private RatingBar ratingBar;
    private Button btnBook, btnMessage;
    private RecyclerView rvReviews, rvResources;

    private ReviewAdapter reviewAdapter;
    private ResourceAdapter resourceAdapter;
    private List<Review> reviewList;
    private List<Resource> resourceList;

    private UserService userService;
    private ReviewService reviewService;
    private ResourceService resourceService;
    private User tutor;
    private String tutorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_detail);

        tutorId = getIntent().getStringExtra("tutor_id");
        userService = new UserService();
        reviewService = new ReviewService();
        resourceService = new ResourceService();

        ivProfile = findViewById(R.id.iv_profile);
        tvName = findViewById(R.id.tv_name);
        tvEducation = findViewById(R.id.tv_education);
        tvSubjects = findViewById(R.id.tv_subjects);
        tvPrice = findViewById(R.id.tv_price);
        tvBio = findViewById(R.id.tv_bio);
        ratingBar = findViewById(R.id.rating_bar);
        btnBook = findViewById(R.id.btn_book);
        btnMessage = findViewById(R.id.btn_message);
        rvReviews = findViewById(R.id.rv_reviews);
        rvResources = findViewById(R.id.rv_resources);

        // Initialize lists and adapters
        reviewList = new ArrayList<>();
        resourceList = new ArrayList<>();

        reviewAdapter = new ReviewAdapter(reviewList);
        resourceAdapter = new ResourceAdapter(resourceList, this);

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);

        rvResources.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvResources.setAdapter(resourceAdapter);

        // Load tutor data
        userService.getUserById(tutorId, new UserService.UserDetailCallback() {
            @Override
            public void onSuccess(User user) {
                tutor = user;
                displayTutorDetails();
            }

            @Override
            public void onFailure(String error) {
                // Handle error
            }
        });

        // Load reviews
        reviewService.getTutorReviews(tutorId, new ReviewService.ReviewsCallback() {
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

        // Load resources
        resourceService.getTutorResources(tutorId, new ResourceService.ResourcesCallback() {
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

        btnBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TutorDetailActivity.this, BookSessionActivity.class);
                intent.putExtra("tutor_id", tutorId);
                startActivity(intent);
            }
        });

        btnMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TutorDetailActivity.this, ChatActivity.class);
                intent.putExtra("recipient_id", tutorId);
                startActivity(intent);
            }
        });
    }

    private void displayTutorDetails() {
        tvName.setText(tutor.getFullName());
        tvEducation.setText(tutor.getEducationLevel());
        tvPrice.setText("$" + tutor.getHourlyRate() + "/hr");
        tvBio.setText(tutor.getBio());
        ratingBar.setRating((float) tutor.getAverageRating());

        // Build subjects text
        StringBuilder subjectsText = new StringBuilder();
        for (Subject subject : tutor.getSubjects()) {
            if (subjectsText.length() > 0) {
                subjectsText.append(", ");
            }
            subjectsText.append(subject.getName());
        }
        tvSubjects.setText(subjectsText.toString());

        // Load profile image
        if (tutor.getProfileImageUrl() != null && !tutor.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(tutor.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.default_profile)
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.default_profile);
        }
    }
}