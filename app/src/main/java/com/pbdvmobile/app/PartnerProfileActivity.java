package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.pbdvmobile.app.adapter.PartnerReviewAdapter; // Import the new adapter
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser; // Assuming you might need this for context, though not directly used for partner's data
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.UserSubject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PartnerProfileActivity extends AppCompatActivity {

    private DataManager dataManager;
    // private LogInUser current_user; // Keep if needed for other logic
    private ImageView partnerProfileImage;
    private User partner;
    private TextView partnerName, partnerTutorRatingCount, partnerTuteeRatingCount, partnerTitle, partnerAvailability, partnerBio, tvNoReviews;
    private RatingBar partnerTutorRating, partnerTuteeRating;
    private ChipGroup partnerSubjects;
    private RecyclerView partnerReviewsRecyclerView;
    private PartnerReviewAdapter reviewAdapter;
    private List<Session> allSessionsForPartner; // All sessions involving the partner

    private LinearLayout layoutTutorRating, layoutTuteeRating;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_partner_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dataManager = DataManager.getInstance(this);
        // current_user = LogInUser.getInstance(dataManager); // Keep if needed

        partner = (User) getIntent().getSerializableExtra("tutor"); // Assuming "tutor" key holds the partner User object

        initial_element();

        if (partner != null) {
            set_data(partner);
            setupRecyclerView();
            loadPartnerReviews();
        } else {
            // Handle partner not found - e.g., show error and finish
            partnerName.setText("Partner data not available.");
            tvNoReviews.setVisibility(VISIBLE);
            partnerReviewsRecyclerView.setVisibility(GONE);
        }
    }

    private void initial_element() {
        partnerProfileImage = findViewById(R.id.img_tutor_profile);
        partnerName = findViewById(R.id.tv_tutor_name);

        layoutTutorRating = findViewById(R.id.layout_tutor_rating);
        partnerTutorRating = findViewById(R.id.rating_tutor);
        partnerTutorRatingCount = findViewById(R.id.tv_rating_value);

        layoutTuteeRating = findViewById(R.id.layout_tutee_rating);
        partnerTuteeRating = findViewById(R.id.rating_tutee);
        partnerTuteeRatingCount = findViewById(R.id.tv_tutee_rating_value);

        partnerTitle = findViewById(R.id.tv_tutor_title);
        partnerAvailability = findViewById(R.id.tv_tutor_availability);
        partnerBio = findViewById(R.id.tv_tutor_bio);
        partnerSubjects = findViewById(R.id.chip_group_subjects);
        partnerReviewsRecyclerView = findViewById(R.id.recyclerViewPartnerReviews);
        tvNoReviews = findViewById(R.id.tv_no_reviews);
    }

    private void set_data(User partner) {
        Glide.with(this)
                .load(partner.getProfileImageUrl())
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .circleCrop()
                .into(partnerProfileImage);

        partnerName.setText(String.format("%s %s", partner.getFirstName(), partner.getLastName()));

        // Get average ratings and counts
        // You might need a more sophisticated way to get review counts if SessionDao.getAverageRatingByStudentNum
        // doesn't directly provide counts or if you want counts of actual reviews submitted.
        // For now, we'll use the size of session lists where the partner was tutor/tutee.

        double[] avgRatings = dataManager.getSessionDao().getAverageRatingByStudentNum(partner.getStudentNum());
        List<Session> sessionsAsTutor = dataManager.getSessionDao().getSessionsByTutorId(partner.getStudentNum());
        List<Session> sessionsAsTutee = dataManager.getSessionDao().getSessionsByTuteeId(partner.getStudentNum());

        long actualTutorReviewsCount = sessionsAsTutor.stream().filter(s -> s.getTuteeReview() != null && !s.getTuteeReview().isEmpty()).count();
        long actualTuteeReviewsCount = sessionsAsTutee.stream().filter(s -> s.getTutorReview() != null && !s.getTutorReview().isEmpty()).count();


        if (partner.isTutor()) { // Check if the partner *can* be a tutor
            if (avgRatings[1] > 0 && actualTutorReviewsCount > 0) {
                layoutTutorRating.setVisibility(VISIBLE);
                partnerTutorRating.setRating((float) avgRatings[1]);
                partnerTutorRatingCount.setText(String.format(Locale.getDefault(), "%.1f (%d reviews as tutor)", avgRatings[1], actualTutorReviewsCount));
            } else {
                layoutTutorRating.setVisibility(GONE); // Hide if no rating or no reviews
            }
        } else {
            layoutTutorRating.setVisibility(GONE);
        }


        if (avgRatings[0] > 0 && actualTuteeReviewsCount > 0) {
            layoutTuteeRating.setVisibility(VISIBLE);
            partnerTuteeRating.setRating((float) avgRatings[0]);
            partnerTuteeRatingCount.setText(String.format(Locale.getDefault(), "%.1f (%d reviews as tutee)", avgRatings[0], actualTuteeReviewsCount));
        } else {
            layoutTuteeRating.setVisibility(GONE); // Hide if no rating or no reviews
        }


        partnerTitle.setText(partner.getEducationLevel() != null ? partner.getEducationLevel().name() : "N/A");
        partnerAvailability.setText("Availability not specified.");
        partnerBio.setText(partner.getBio() != null && !partner.getBio().isEmpty() ? partner.getBio() : "No bio provided.");

        populateSubjects();
    }

    private void populateSubjects() {
        partnerSubjects.removeAllViews(); // Clear existing chips before adding new ones
        List<UserSubject> userSubjects = dataManager.getSubjectDao().getUserSubjects(partner.getStudentNum());

        if (userSubjects.isEmpty()) {
            // Optionally, show a message if no subjects are listed
            Chip noSubjectsChip = new Chip(this);
            noSubjectsChip.setText("No subjects listed");
            noSubjectsChip.setEnabled(false);
            partnerSubjects.addView(noSubjectsChip);
            return;
        }

        for (UserSubject userSub : userSubjects) {
            Subject subject = dataManager.getSubjectDao().getSubjectById(userSub.getSubjectId());
            if (subject != null) {
                Chip subjectChip = new Chip(this);
                // Extract only the subject name part if it contains a colon
                String subjectName = subject.getSubjectName();
                if (subjectName.contains(": ")) {
                    subjectChip.setText(subjectName.substring(subjectName.indexOf(": ") + 2));
                } else {
                    subjectChip.setText(subjectName);
                }
                // Add styling or click listeners to chips if needed
                partnerSubjects.addView(subjectChip);
            }
        }
    }

    private void setupRecyclerView() {
        allSessionsForPartner = new ArrayList<>();
        reviewAdapter = new PartnerReviewAdapter(this, allSessionsForPartner, partner, dataManager);
        partnerReviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        partnerReviewsRecyclerView.setAdapter(reviewAdapter);
        // Important for NestedScrollView:
        partnerReviewsRecyclerView.setNestedScrollingEnabled(false);
    }

    private void loadPartnerReviews() {
        if (partner == null) return;

        List<Session> sessionsAsTutor = dataManager.getSessionDao().getSessionsByTutorId(partner.getStudentNum());
        List<Session> sessionsAsTutee = dataManager.getSessionDao().getSessionsByTuteeId(partner.getStudentNum());

        List<Session> relevantSessionsWithReviews = new ArrayList<>();

        // Collect sessions where partner was tutor and tutee left a review
        for (Session session : sessionsAsTutor) {
            if (session.getTuteeReview() != null && !session.getTuteeReview().isEmpty()) {
                relevantSessionsWithReviews.add(session);
            }
        }

        // Collect sessions where partner was tutee and tutor left a review
        for (Session session : sessionsAsTutee) {
            // Avoid adding duplicates if a session somehow appears in both lists (shouldn't happen with current logic)
            // And ensure this session wasn't already added (e.g. if partner tutored themselves)
            boolean alreadyAdded = false;
            for(Session s : relevantSessionsWithReviews){
                if(s.getId() == session.getId()){
                    alreadyAdded = true;
                    break;
                }
            }
            if(!alreadyAdded && session.getTutorReview() != null && !session.getTutorReview().isEmpty()){
                relevantSessionsWithReviews.add(session);
            }
        }

        // Sort by date, newest first
        relevantSessionsWithReviews.sort((s1, s2) -> s2.getStartTime().compareTo(s1.getStartTime()));


        allSessionsForPartner.clear();
        allSessionsForPartner.addAll(relevantSessionsWithReviews);
        reviewAdapter.notifyDataSetChanged();

        if (allSessionsForPartner.isEmpty()) {
            tvNoReviews.setVisibility(VISIBLE);
            partnerReviewsRecyclerView.setVisibility(GONE);
        } else {
            tvNoReviews.setVisibility(GONE);
            partnerReviewsRecyclerView.setVisibility(VISIBLE);
        }
    }
}

/*
package com.pbdvmobile.app;

import static android.view.View.GONE;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.UserSubject;

import java.util.List;

public class PartnerProfileActivity extends AppCompatActivity {


    DataManager dataManager = DataManager.getInstance(this);
    LogInUser current_user = LogInUser.getInstance(dataManager);
    ImageView partnerProfileImage;
    User partner;
    TextView partnerName, partnerTutorRatingCount, partnerTuteeRatingCount, partnerTitle, partnerAvailability, partnerBio;
    RatingBar partnerTutorRating, partnerTuteeRating;
    ChipGroup partnerSubjects;
    RecyclerView partnerReviews;
    List<Session> ratingsAsTutor, ratingsAsTutee;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_partner_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        partner = (User) getIntent().getSerializableExtra("tutor");

        initial_element();

        if(partner != null)set_data(partner);

    }

    private void set_data(User partner) {

        Glide.with(this)
                .load(partner.getProfileImageUrl())
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .circleCrop()
                .into(partnerProfileImage);

        partnerName.setText(partner.getFirstName() + " " + partner.getLastName());
        double[] avgRatings = dataManager.getSessionDao().getAverageRatingByStudentNum(partner.getStudentNum());


        if(partner.isTutor()){
            ratingsAsTutor = dataManager.getSessionDao().getSessionsByTutorId(partner.getStudentNum());
            if(!ratingsAsTutor.isEmpty() && avgRatings[1] != 0) {
                partnerTutorRating.setRating((float) avgRatings[1]);
                partnerTutorRatingCount.setText(partnerTutorRating.getRating() + " (" + ratingsAsTutor.size() + " reviews as a tutor)");
            }else{
                partnerTutorRating.setVisibility(GONE);
                partnerTutorRatingCount.setText("No tutor rated yet");
            }
        }else{
            partnerTutorRating.setVisibility(GONE);
            partnerTutorRatingCount.setVisibility(GONE);
        }
        ratingsAsTutee = dataManager.getSessionDao().getSessionsByTuteeId(partner.getStudentNum());

        if(!ratingsAsTutee.isEmpty() && avgRatings[0] != 0) {
            partnerTuteeRating.setRating((float) avgRatings[0]);
            partnerTuteeRatingCount.setText(partnerTuteeRating.getRating() + " (" + ratingsAsTutee.size() + " reviews as a tutee)");
        }else{
            partnerTuteeRating.setVisibility(GONE);
            partnerTuteeRatingCount.setText("No tutee rated yet");
        }

        partnerTitle.setText(partner.getEducationLevel().name());
        partnerAvailability.setText("partner.getAvailability()");
        partnerBio.setText(partner.getBio().isEmpty() ? "No Bio provided": partner.getBio());

        populateSubjects();

    }
    private void populateSubjects(){
        List<UserSubject> userSubjects = dataManager.getSubjectDao().getUserSubjects(partner.getStudentNum());

        for(UserSubject subject : userSubjects){
            Chip subjectChip = new Chip(this);
            subjectChip.setText(dataManager.getSubjectDao()
                    .getSubjectById(subject.getSubjectId())
                    .getSubjectName().split(": ")[1]);

            partnerSubjects.addView(subjectChip);
        }
    }
    private void initial_element(){

        partnerProfileImage = findViewById(R.id.img_tutor_profile);
        partnerName = findViewById(R.id.tv_tutor_name);
        partnerTutorRating = findViewById(R.id.rating_tutor);
        partnerTutorRatingCount = findViewById(R.id.tv_rating_value);
        partnerTuteeRating = findViewById(R.id.rating_tutee);
        partnerTuteeRatingCount = findViewById(R.id.tv_tutee_rating_value);
        partnerTitle = findViewById(R.id.tv_tutor_title);
        partnerAvailability = findViewById(R.id.tv_tutor_availability);
        partnerBio = findViewById(R.id.tv_tutor_bio);
        partnerSubjects = findViewById(R.id.chip_group_subjects);
        partnerReviews = findViewById(R.id.recyclerViewPartnerReviews);

    }
}*/
