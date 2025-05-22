package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.pbdvmobile.app.adapter.PartnerReviewAdapter;
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PartnerProfileActivity extends AppCompatActivity {

    private static final String TAG = "PartnerProfileActivity";

    private ImageView partnerProfileImage;
    private User partnerUserPojo; // Renamed from partner
    private TextView partnerName, partnerTutorRatingCount, partnerTuteeRatingCount,
            partnerEducationLevel, /*partnerAvailability,*/ partnerBio, tvNoReviews; // Removed partnerTitle, partnerAvailability
    private RatingBar partnerTutorRating, partnerTuteeRating;
    private ChipGroup partnerSubjectsChipGroup; // Renamed from partnerSubjects
    private RecyclerView partnerReviewsRecyclerView;
    private PartnerReviewAdapter reviewAdapter;
    private ProgressBar profileLoadingProgressBar; // Added ProgressBar

    private LinearLayout layoutTutorRating, layoutTuteeRating;

    private UserDao userDao;
    private SessionDao sessionDao;
    private SubjectDao subjectDao;
    private List<Subject> allSubjectsList = new ArrayList<>(); // To map subject IDs to names

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Consider if needed
        setContentView(R.layout.activity_partner_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userDao = new UserDao();
        sessionDao = new SessionDao();
        subjectDao = new SubjectDao();

        // Get User object passed from the previous activity/fragment
        // Assuming the key is "tutor" but it's actually the "partner" being viewed
        if (getIntent().hasExtra("tutor")) {
            partnerUserPojo = (User) getIntent().getSerializableExtra("tutor");
        }

        initializeViews();

        if (partnerUserPojo != null && partnerUserPojo.getUid() != null) {
            showLoading(true);
            // Load all subjects first to help display tutored subjects by name
            subjectDao.getAllSubjects().addOnSuccessListener(subjectSnapshots -> {
                allSubjectsList.clear();
                if (subjectSnapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : subjectSnapshots.getDocuments()) {
                        Subject s = doc.toObject(Subject.class);
                        if (s != null) {
                            s.setId(doc.getId());
                            allSubjectsList.add(s);
                        }
                    }
                }
                // Now that subjects are loaded (or not), proceed to display partner data
                displayPartnerData();
                setupRecyclerView();
                loadPartnerReviews(); // This will also call showLoading(false) when done
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load all subjects", e);
                Toast.makeText(this, "Could not load subject list.", Toast.LENGTH_SHORT).show();
                // Proceed without full subject name resolution for chips if this fails
                displayPartnerData();
                setupRecyclerView();
                loadPartnerReviews();
            });
        } else {
            Toast.makeText(this, "Partner data not found.", Toast.LENGTH_LONG).show();
            partnerName.setText("Partner data not available.");
            tvNoReviews.setVisibility(VISIBLE);
            partnerReviewsRecyclerView.setVisibility(GONE);
            // Consider finishing the activity if partnerUserPojo is essential and null
            // finish();
        }
    }

    private void initializeViews() {
        profileLoadingProgressBar = findViewById(R.id.partner_profile_progress_bar); // Add to XML
        partnerProfileImage = findViewById(R.id.img_tutor_profile); // Assuming this is img_partner_profile
        partnerName = findViewById(R.id.tv_tutor_name); // Assuming this is tv_partner_name

        layoutTutorRating = findViewById(R.id.layout_tutor_rating);
        partnerTutorRating = findViewById(R.id.rating_tutor);
        partnerTutorRatingCount = findViewById(R.id.tv_rating_value);

        layoutTuteeRating = findViewById(R.id.layout_tutee_rating);
        partnerTuteeRating = findViewById(R.id.rating_tutee);
        partnerTuteeRatingCount = findViewById(R.id.tv_tutee_rating_value);

        partnerEducationLevel = findViewById(R.id.tv_tutor_title); // Assuming this is tv_partner_education
        // partnerAvailability = findViewById(R.id.tv_tutor_availability); // This view seems to have been removed or not used
        partnerBio = findViewById(R.id.tv_tutor_bio); // Assuming this is tv_partner_bio
        partnerSubjectsChipGroup = findViewById(R.id.chip_group_subjects);
        partnerReviewsRecyclerView = findViewById(R.id.recyclerViewPartnerReviews);
        tvNoReviews = findViewById(R.id.tv_no_reviews);
    }

    private void showLoading(boolean isLoading) {
        if (profileLoadingProgressBar != null) {
            profileLoadingProgressBar.setVisibility(isLoading ? VISIBLE : GONE);
        }
        // You might want to hide/show the main content layout as well
        View contentLayout = findViewById(R.id.partner_profile_content_layout); // Add an ID to your main content group
        if (contentLayout != null) {
            contentLayout.setVisibility(isLoading ? GONE : VISIBLE);
        }
    }


    private void displayPartnerData() {
        if (partnerUserPojo == null) return;

        Glide.with(this)
                .load(partnerUserPojo.getProfileImageUrl())
                .placeholder(R.mipmap.ic_launcher_round) // Use a generic placeholder
                .error(R.mipmap.ic_launcher_round)       // And an error image
                .circleCrop()
                .into(partnerProfileImage);

        partnerName.setText(String.format("%s %s", partnerUserPojo.getFirstName(), partnerUserPojo.getLastName()));

        // Fetch and display partner's average ratings (as tutor and as tutee)
        // These should ideally be fields on the User object itself, updated periodically.
        // If not, SessionDao.getAverageRatingByFirebaseUid is used.
        sessionDao.getAverageRatingByFirebaseUid(partnerUserPojo.getUid(), new SessionDao.RatingsCallback() {
            @Override
            public void onRatingsFetched(double averageRatingAsTutee, double averageRatingAsTutor) {
                // Update the local POJO if these are the most up-to-date values
                partnerUserPojo.setAverageRatingAsTutee(averageRatingAsTutee);
                partnerUserPojo.setAverageRatingAsTutor(averageRatingAsTutor);

                updateRatingDisplay(); // Update UI with fetched/updated ratings
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching average ratings for partner " + partnerUserPojo.getUid(), e);
                Toast.makeText(PartnerProfileActivity.this, "Could not load partner ratings.", Toast.LENGTH_SHORT).show();
                updateRatingDisplay(); // Update with 0 or current POJO values
            }
        });


        partnerEducationLevel.setText(partnerUserPojo.getEducationLevel() != null ?
                partnerUserPojo.getEducationLevel().name().replace("_", " ") : "N/A");
        // partnerAvailability.setText("Availability: Not specified"); // If you re-add this field
        partnerBio.setText(partnerUserPojo.getBio() != null && !partnerUserPojo.getBio().isEmpty() ?
                partnerUserPojo.getBio() : "No bio provided.");

        populatePartnerSubjectsChips();
    }

    private void updateRatingDisplay() {
        // Display Tutor Rating (rating partner received as a tutor)
        if (partnerUserPojo.isTutor() && partnerUserPojo.getAverageRatingAsTutor() > 0) {
            layoutTutorRating.setVisibility(VISIBLE);
            partnerTutorRating.setRating((float) partnerUserPojo.getAverageRatingAsTutor());
            // For review count, you'd need another query or denormalized count on User object
            partnerTutorRatingCount.setText(String.format(Locale.getDefault(), "%.1f (as Tutor)", partnerUserPojo.getAverageRatingAsTutor()));
        } else if (partnerUserPojo.isTutor()) {
            layoutTutorRating.setVisibility(VISIBLE);
            partnerTutorRating.setRating(0);
            partnerTutorRatingCount.setText("Not yet rated as Tutor");
        }
        else {
            layoutTutorRating.setVisibility(GONE);
        }

        // Display Tutee Rating (rating partner received as a tutee)
        if (partnerUserPojo.getAverageRatingAsTutee() > 0) {
            layoutTuteeRating.setVisibility(VISIBLE);
            partnerTuteeRating.setRating((float) partnerUserPojo.getAverageRatingAsTutee());
            partnerTuteeRatingCount.setText(String.format(Locale.getDefault(), "%.1f (as Tutee)", partnerUserPojo.getAverageRatingAsTutee()));
        } else {
            layoutTuteeRating.setVisibility(VISIBLE);
            partnerTuteeRating.setRating(0);
            partnerTuteeRatingCount.setText("Not yet rated as Tutee");
        }
    }


    private void populatePartnerSubjectsChips() {
        partnerSubjectsChipGroup.removeAllViews();
        List<String> tutoredSubjectIds = partnerUserPojo.getTutoredSubjectIds();

        if (tutoredSubjectIds == null || tutoredSubjectIds.isEmpty()) {
            if (partnerUserPojo.isTutor()) { // Only show "no subjects" if they are marked as a tutor
                Chip noSubjectsChip = new Chip(this);
                noSubjectsChip.setText("No subjects listed for tutoring");
                noSubjectsChip.setEnabled(false);
                partnerSubjectsChipGroup.addView(noSubjectsChip);
            }
            return;
        }

        for (String subjectId : tutoredSubjectIds) {
            Subject subjectDetails = findSubjectById(subjectId); // Find from preloaded allSubjectsList
            if (subjectDetails != null) {
                Chip subjectChip = new Chip(this);
                String displayName = subjectDetails.getSubjectName();
                if (displayName.contains(": ")) { // Show only part after colon for brevity
                    displayName = displayName.substring(displayName.indexOf(": ") + 2);
                }
                subjectChip.setText(displayName);
                partnerSubjectsChipGroup.addView(subjectChip);
            } else {
                Log.w(TAG, "Could not find subject details for ID: " + subjectId);
                Chip unknownSubjectChip = new Chip(this);
                unknownSubjectChip.setText("Unknown Subject (ID: " + subjectId.substring(0, Math.min(5,subjectId.length())) +"..)");
                partnerSubjectsChipGroup.addView(unknownSubjectChip);
            }
        }
    }

    private Subject findSubjectById(String subjectId) {
        for (Subject s : allSubjectsList) {
            if (s.getId() != null && s.getId().equals(subjectId)) {
                return s;
            }
        }
        return null;
    }


    private void setupRecyclerView() {
        reviewAdapter = new PartnerReviewAdapter(this, new ArrayList<>(), partnerUserPojo, subjectDao, userDao);
        partnerReviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        partnerReviewsRecyclerView.setAdapter(reviewAdapter);
        partnerReviewsRecyclerView.setNestedScrollingEnabled(false); // Important for use inside ScrollView
    }

    private void loadPartnerReviews() {
        if (partnerUserPojo == null || partnerUserPojo.getUid() == null) {
            showLoading(false);
            tvNoReviews.setVisibility(VISIBLE);
            partnerReviewsRecyclerView.setVisibility(GONE);
            return;
        }

        showLoading(true); // Show loading before fetching reviews

        // Fetch sessions where partner was a tutor (to get reviews from tutees)
        sessionDao.getSessionsByTutorUid(partnerUserPojo.getUid(), new SessionDao.SessionsCallback() {
            @Override
            public void onSessionsFetched(List<Session> sessionsAsTutor) {
                List<Session> reviewsForPartner = new ArrayList<>();
                for (Session session : sessionsAsTutor) {
                    // We want reviews GIVEN TO this partner when they were a TUTOR
                    if (session.getStatus() == Session.Status.COMPLETED &&
                            (session.getTuteeReview() != null || (session.getTuteeRating() != null && session.getTuteeRating() > 0))) {
                        reviewsForPartner.add(session);
                    }
                }

                // Fetch sessions where partner was a tutee (to get reviews from tutors)
                sessionDao.getSessionsByTuteeUid(partnerUserPojo.getUid(), new SessionDao.SessionsCallback() {
                    @Override
                    public void onSessionsFetched(List<Session> sessionsAsTutee) {
                        for (Session session : sessionsAsTutee) {
                            // We want reviews GIVEN TO this partner when they were a TUTEE
                            if (session.getStatus() == Session.Status.COMPLETED &&
                                    (session.getTutorReview() != null || (session.getTutorRating() != null && session.getTutorRating() > 0))) {
                                // Avoid duplicates if a session somehow appears in both roles for the same user (unlikely)
                                boolean alreadyAdded = false;
                                for(Session s : reviewsForPartner) if(s.getId().equals(session.getId())) alreadyAdded = true;
                                if(!alreadyAdded) reviewsForPartner.add(session);
                            }
                        }

                        if (!reviewsForPartner.isEmpty()) {
                            Collections.sort(reviewsForPartner, (s1, s2) -> {
                                Timestamp t1 = s1.getStartTime() != null ? s1.getStartTime() : new Timestamp(0,0);
                                Timestamp t2 = s2.getStartTime() != null ? s2.getStartTime() : new Timestamp(0,0);
                                return t2.compareTo(t1); // Newest first
                            });
                            reviewAdapter.updateList(reviewsForPartner);
                            tvNoReviews.setVisibility(GONE);
                            partnerReviewsRecyclerView.setVisibility(VISIBLE);
                        } else {
                            reviewAdapter.updateList(new ArrayList<>()); // Clear adapter
                            tvNoReviews.setVisibility(VISIBLE);
                            partnerReviewsRecyclerView.setVisibility(GONE);
                        }
                        showLoading(false);
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error fetching sessions as tutee for partner reviews", e);
                        // Process with whatever was fetched from sessionsAsTutor
                        if (!reviewsForPartner.isEmpty()) {
                            Collections.sort(reviewsForPartner, (s1, s2) -> s2.getStartTime().compareTo(s1.getStartTime())); // Newest first
                            reviewAdapter.updateList(reviewsForPartner);
                        } else {
                            tvNoReviews.setVisibility(VISIBLE);
                            partnerReviewsRecyclerView.setVisibility(GONE);
                        }
                        showLoading(false);
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching sessions as tutor for partner reviews", e);
                tvNoReviews.setVisibility(VISIBLE);
                partnerReviewsRecyclerView.setVisibility(GONE);
                showLoading(false);
            }
        });
    }
}
