package com.pbdvmobile.app;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button; // Added
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
// Import BuildConfig
import com.pbdvmobile.app.BuildConfig;
import com.pbdvmobile.app.data.DataManager; // Added for credit cost
import com.pbdvmobile.app.data.LogInUser; // Added
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.SubjectDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import org.json.JSONArray; // Added
import org.json.JSONObject; // Added

import java.io.BufferedReader; // Added
import java.io.IOException; // Added
import java.io.InputStreamReader; // Added
import java.io.OutputStream; // Added
import java.net.HttpURLConnection; // Added
import java.net.URL; // Added
import java.nio.charset.StandardCharsets; // Added
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap; // Added
import java.util.List;
import java.util.Locale;
import java.util.Map; // Added
import java.util.concurrent.ExecutorService; // Added
import java.util.concurrent.Executors; // Added

public class PartnerProfileActivity extends AppCompatActivity {

    private static final String TAG = "PartnerProfileActivity";
    // Define a credit cost for AI Review Summary, adjust in DataManager
    private static final double CREDIT_COST_AI_REVIEW_SUMMARY = DataManager.CREDIT_AI_REVIEW_SUMMARY; // Example: DataManager.CREDIT_AI_REVIEW_SUMMARY = 0.5;


    private ImageView partnerProfileImage;
    private User partnerUserPojo;
    private TextView partnerName, partnerTutorRatingCount, partnerTuteeRatingCount,
            partnerEducationLevel, partnerBio, tvNoReviews, tvTutorAvailability; // Renamed from partnerAvailability
    private RatingBar partnerTutorRating, partnerTuteeRating;
    private ChipGroup partnerSubjectsChipGroup;
    private RecyclerView partnerReviewsRecyclerView;
    private PartnerReviewAdapter reviewAdapter;
    private ProgressBar profileLoadingProgressBar, aiSummaryProgressBar; // Added aiSummaryProgressBar
    private Button btnGenerateReviewSummary; // Added

    private LinearLayout layoutTutorRating, layoutTuteeRating;

    private UserDao userDao;
    private SessionDao sessionDao;
    private SubjectDao subjectDao;
    private List<Subject> allSubjectsList = new ArrayList<>();
    private List<Session> currentPartnerReviews = new ArrayList<>(); // To store fetched reviews

    private LogInUser loggedInUser; // Added
    private User viewingUserPojo; // The user who is viewing this profile
    private ExecutorService executorService; // Added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partner_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userDao = new UserDao();
        sessionDao = new SessionDao();
        subjectDao = new SubjectDao();
        executorService = Executors.newSingleThreadExecutor(); // Added

        loggedInUser = LogInUser.getInstance(); // Added
        if (loggedInUser != null) { // Added
            viewingUserPojo = loggedInUser.getUser();
        }

        if (getIntent().hasExtra("tutor")) {
            partnerUserPojo = (User) getIntent().getSerializableExtra("tutor");
        }

        initializeViews();

        if (partnerUserPojo != null && partnerUserPojo.getUid() != null) {
            showLoading(true, true); // Show main profile loading
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
                displayPartnerData();
                setupRecyclerView();
                loadPartnerReviews(); // This will call showLoading(false, true)
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load all subjects", e);
                Toast.makeText(this, "Could not load subject list.", Toast.LENGTH_SHORT).show();
                displayPartnerData();
                setupRecyclerView();
                loadPartnerReviews();
            });
        } else {
            Toast.makeText(this, "Partner data not found.", Toast.LENGTH_LONG).show();
            if (partnerName != null) partnerName.setText("Partner data not available.");
            if (tvNoReviews != null) tvNoReviews.setVisibility(VISIBLE);
            if (partnerReviewsRecyclerView != null) partnerReviewsRecyclerView.setVisibility(GONE);
            showLoading(false, true); // Hide main profile loading
        }
    }

    private void initializeViews() {
        profileLoadingProgressBar = findViewById(R.id.partner_profile_progress_bar);
        aiSummaryProgressBar = findViewById(R.id.ai_summary_progress_bar); // Added
        btnGenerateReviewSummary = findViewById(R.id.btn_generate_review_summary); // Added

        partnerProfileImage = findViewById(R.id.img_tutor_profile);
        partnerName = findViewById(R.id.tv_tutor_name);

        layoutTutorRating = findViewById(R.id.layout_tutor_rating);
        partnerTutorRating = findViewById(R.id.rating_tutor);
        partnerTutorRatingCount = findViewById(R.id.tv_rating_value);

        layoutTuteeRating = findViewById(R.id.layout_tutee_rating);
        partnerTuteeRating = findViewById(R.id.rating_tutee);
        partnerTuteeRatingCount = findViewById(R.id.tv_tutee_rating_value);

        partnerEducationLevel = findViewById(R.id.tv_tutor_title);
        tvTutorAvailability = findViewById(R.id.tv_tutor_availability); // This is where AI summary will go
        partnerBio = findViewById(R.id.tv_tutor_bio);
        partnerSubjectsChipGroup = findViewById(R.id.chip_group_subjects);
        partnerReviewsRecyclerView = findViewById(R.id.recyclerViewPartnerReviews);
        tvNoReviews = findViewById(R.id.tv_no_reviews);

        btnGenerateReviewSummary.setOnClickListener(v -> prepareAndPerformAIReviewSummary()); // Added
    }

    private void showLoading(boolean isLoading, boolean isMainProfileLoading) {
        if (isMainProfileLoading) {
            if (profileLoadingProgressBar != null) {
                profileLoadingProgressBar.setVisibility(isLoading ? VISIBLE : GONE);
            }
            View contentLayout = findViewById(R.id.partner_profile_content_layout);
            if (contentLayout != null) {
                contentLayout.setVisibility(isLoading ? GONE : VISIBLE);
            }
        } else { // AI Summary loading
            if (aiSummaryProgressBar != null) {
                aiSummaryProgressBar.setVisibility(isLoading ? VISIBLE : GONE);
            }
            if (btnGenerateReviewSummary != null) {
                btnGenerateReviewSummary.setEnabled(!isLoading);
            }
        }
    }


    private void displayPartnerData() {
        if (partnerUserPojo == null) return;

        Glide.with(this)
                .load(partnerUserPojo.getProfileImageUrl())
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .circleCrop()
                .into(partnerProfileImage);

        partnerName.setText(String.format("%s %s", partnerUserPojo.getFirstName(), partnerUserPojo.getLastName()));

        sessionDao.getAverageRatingByFirebaseUid(partnerUserPojo.getUid(), new SessionDao.RatingsCallback() {
            @Override
            public void onRatingsFetched(double averageRatingAsTutee, double averageRatingAsTutor) {
                partnerUserPojo.setAverageRatingAsTutee(averageRatingAsTutee);
                partnerUserPojo.setAverageRatingAsTutor(averageRatingAsTutor);
                updateRatingDisplay();
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching average ratings for partner " + partnerUserPojo.getUid(), e);
                updateRatingDisplay(); // Update with 0 or current POJO values
            }
        });

        partnerEducationLevel.setText(partnerUserPojo.getEducationLevel() != null ?
                partnerUserPojo.getEducationLevel().name().replace("_", " ") : "N/A");
        // Initial text for AI summary field
        tvTutorAvailability.setText("Click 'Generate AI Summary' to get an overview of this partner's reviews.");
        partnerBio.setText(partnerUserPojo.getBio() != null && !partnerUserPojo.getBio().isEmpty() ?
                partnerUserPojo.getBio() : "No bio provided.");

        populatePartnerSubjectsChips();
    }

    private void updateRatingDisplay() {
        if (partnerUserPojo == null) return;
        // Display Tutor Rating
        if (partnerUserPojo.isTutor()) { // Only show tutor rating section if they are a tutor
            layoutTutorRating.setVisibility(VISIBLE);
            if (partnerUserPojo.getAverageRatingAsTutor() > 0) {
                partnerTutorRating.setRating((float) partnerUserPojo.getAverageRatingAsTutor());
                partnerTutorRatingCount.setText(String.format(Locale.getDefault(), "%.1f (as Tutor)", partnerUserPojo.getAverageRatingAsTutor()));
            } else {
                partnerTutorRating.setRating(0);
                partnerTutorRatingCount.setText("Not yet rated as Tutor");
            }
        } else {
            layoutTutorRating.setVisibility(GONE);
        }

        // Display Tutee Rating
        // All users can be tutees, so always show this section, but adapt text if no rating.
        layoutTuteeRating.setVisibility(VISIBLE);
        if (partnerUserPojo.getAverageRatingAsTutee() > 0) {
            partnerTuteeRating.setRating((float) partnerUserPojo.getAverageRatingAsTutee());
            partnerTuteeRatingCount.setText(String.format(Locale.getDefault(), "%.1f (as Tutee)", partnerUserPojo.getAverageRatingAsTutee()));
        } else {
            partnerTuteeRating.setRating(0);
            partnerTuteeRatingCount.setText("Not yet rated as Tutee");
        }
    }


    private void populatePartnerSubjectsChips() {
        if (partnerUserPojo == null || partnerSubjectsChipGroup == null) return;
        partnerSubjectsChipGroup.removeAllViews();
        List<String> tutoredSubjectIds = partnerUserPojo.getTutoredSubjectIds();

        if (!partnerUserPojo.isTutor() || tutoredSubjectIds == null || tutoredSubjectIds.isEmpty()) {
            // If not a tutor, or a tutor with no subjects, don't show "No subjects" chip, just leave empty.
            // Or, you could hide the "Subjects" card view entirely if not a tutor.
            // For now, just leaving ChipGroup empty.
            return;
        }

        for (String subjectId : tutoredSubjectIds) {
            Subject subjectDetails = findSubjectById(subjectId);
            if (subjectDetails != null) {
                Chip subjectChip = new Chip(this);
                String displayName = subjectDetails.getSubjectName();
                if (displayName != null && displayName.contains(": ")) {
                    displayName = displayName.substring(displayName.indexOf(": ") + 2);
                }
                subjectChip.setText(displayName != null ? displayName : "Unknown Subject");
                partnerSubjectsChipGroup.addView(subjectChip);
            } else {
                Log.w(TAG, "Could not find subject details for ID: " + subjectId);
                Chip unknownSubjectChip = new Chip(this);
                unknownSubjectChip.setText("Subject ID: " + subjectId.substring(0, Math.min(5,subjectId.length())) +"..");
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
        partnerReviewsRecyclerView.setNestedScrollingEnabled(false);
    }

    private void loadPartnerReviews() {
        if (partnerUserPojo == null || partnerUserPojo.getUid() == null) {
            showLoading(false, true); // Hide main profile loading
            tvNoReviews.setVisibility(VISIBLE);
            partnerReviewsRecyclerView.setVisibility(GONE);
            btnGenerateReviewSummary.setEnabled(false); // No reviews to summarize
            return;
        }

        showLoading(true, true); // Show main profile loading (as reviews are part of it)
        currentPartnerReviews.clear(); // Clear previous reviews

        // Fetch sessions where partner was a tutor
        sessionDao.getSessionsByTutorUid(partnerUserPojo.getUid(), new SessionDao.SessionsCallback() {
            @Override
            public void onSessionsFetched(List<Session> sessionsAsTutor) {
                for (Session session : sessionsAsTutor) {
                    if (session.getStatus() == Session.Status.COMPLETED &&
                            ((session.getTuteeReview() != null && !session.getTuteeReview().isEmpty()) ||
                                    (session.getTuteeRating() != null && session.getTuteeRating() > 0))) {
                        currentPartnerReviews.add(session);
                    }
                }
                // Chain the next call
                fetchReviewsAsTuteeAndFinalize();
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching sessions as tutor for partner reviews", e);
                fetchReviewsAsTuteeAndFinalize(); // Still try to fetch other reviews
            }
        });
    }

    private void fetchReviewsAsTuteeAndFinalize() {
        sessionDao.getSessionsByTuteeUid(partnerUserPojo.getUid(), new SessionDao.SessionsCallback() {
            @Override
            public void onSessionsFetched(List<Session> sessionsAsTutee) {
                for (Session session : sessionsAsTutee) {
                    if (session.getStatus() == Session.Status.COMPLETED &&
                            ((session.getTutorReview() != null && !session.getTutorReview().isEmpty()) ||
                                    (session.getTutorRating() != null && session.getTutorRating() > 0))) {
                        boolean alreadyAdded = false;
                        for(Session s : currentPartnerReviews) if(s.getId().equals(session.getId())) alreadyAdded = true;
                        if(!alreadyAdded) currentPartnerReviews.add(session);
                    }
                }
                finalizeReviewLoading();
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching sessions as tutee for partner reviews", e);
                finalizeReviewLoading();
            }
        });
    }

    private void finalizeReviewLoading() {
        if (!currentPartnerReviews.isEmpty()) {
            Collections.sort(currentPartnerReviews, (s1, s2) -> {
                Timestamp t1 = s1.getStartTime() != null ? s1.getStartTime() : new Timestamp(0,0);
                Timestamp t2 = s2.getStartTime() != null ? s2.getStartTime() : new Timestamp(0,0);
                return t2.compareTo(t1); // Newest first
            });
            reviewAdapter.updateList(currentPartnerReviews);
            tvNoReviews.setVisibility(GONE);
            partnerReviewsRecyclerView.setVisibility(VISIBLE);
            btnGenerateReviewSummary.setEnabled(true); // Enable AI summary button
        } else {
            reviewAdapter.updateList(new ArrayList<>());
            tvNoReviews.setVisibility(VISIBLE);
            partnerReviewsRecyclerView.setVisibility(GONE);
            btnGenerateReviewSummary.setEnabled(false); // Disable if no reviews
            tvTutorAvailability.setText("No reviews available to generate an AI summary.");
        }
        showLoading(false, true); // Hide main profile loading
    }

    // --- AI Review Summary Methods ---
    private void prepareAndPerformAIReviewSummary() {
        if (currentPartnerReviews.isEmpty()) {
            Toast.makeText(this, "No reviews available to summarize.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (viewingUserPojo == null) { // Check if the viewing user's data is available for credit check
            Toast.makeText(this, "Your user data not available. Cannot perform AI action.", Toast.LENGTH_LONG).show();
            return;
        }
        if (viewingUserPojo.getCredits() < CREDIT_COST_AI_REVIEW_SUMMARY) {
            Toast.makeText(this, "Insufficient credits (" + viewingUserPojo.getCredits() + ") for AI summary. Cost: " + CREDIT_COST_AI_REVIEW_SUMMARY, Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true, false); // Show AI summary progress bar

        // Deduct credits from the viewing user
        double newCredits = viewingUserPojo.getCredits() - CREDIT_COST_AI_REVIEW_SUMMARY;
        Map<String, Object> creditUpdate = new HashMap<>();
        creditUpdate.put("credits", newCredits);

        userDao.updateUserSpecificFields(viewingUserPojo.getUid(), creditUpdate)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Credits deducted successfully for AI Review Summary. New balance: " + newCredits);
                    viewingUserPojo.setCredits(newCredits);
                    if(loggedInUser != null) loggedInUser.setUser(viewingUserPojo); // Update LoggedInUser singleton

                    performAISummaryGeneration();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to deduct credits for AI Review Summary", e);
                    Toast.makeText(PartnerProfileActivity.this, "Failed to update your credits. Please try again.", Toast.LENGTH_SHORT).show();
                    showLoading(false, false); // Hide AI summary progress bar
                });
    }

    private void performAISummaryGeneration() {
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.isEmpty() || "YOUR_ACTUAL_API_KEY_HERE".equals(BuildConfig.GEMINI_API_KEY) || "".equals(BuildConfig.GEMINI_API_KEY)) {
            Log.e(TAG, "API Key not found or not set in BuildConfig.");
            runOnUiThread(() -> {
                Toast.makeText(PartnerProfileActivity.this, "AI Service API Key not configured.", Toast.LENGTH_LONG).show();
                showLoading(false, false);
                refundCreditsForAISummaryOnError(); // Refund if API key is missing
            });
            return;
        }

        StringBuilder reviewsTextBuilder = new StringBuilder();
        for (Session session : currentPartnerReviews) {
            // Check if the review is FOR this partner user
            if (partnerUserPojo.getUid().equals(session.getTutorUid()) && session.getTuteeReview() != null && !session.getTuteeReview().isEmpty()) {
                // Partner was TUTOR, review is from Tutee
                reviewsTextBuilder.append("Review (as Tutor): ").append(session.getTuteeReview()).append("\nRating: ").append(session.getTuteeRating() != null ? session.getTuteeRating() : "N/A").append("\n\n");
            } else if (partnerUserPojo.getUid().equals(session.getTuteeUid()) && session.getTutorReview() != null && !session.getTutorReview().isEmpty()) {
                // Partner was TUTEE, review is from Tutor
                reviewsTextBuilder.append("Review (as Tutee): ").append(session.getTutorReview()).append("\nRating: ").append(session.getTutorRating() != null ? session.getTutorRating() : "N/A").append("\n\n");
            }
        }

        if (reviewsTextBuilder.length() == 0) {
            Toast.makeText(this, "No review text found to summarize.", Toast.LENGTH_SHORT).show();
            showLoading(false, false);
            refundCreditsForAISummaryOnError(); // No actual content was sent, refund
            return;
        }

        String allReviews = reviewsTextBuilder.toString();
        String prompt = "You are an AI assistant. Summarize the following student and tutor reviews for a specific partner. " +
                "Focus on recurring themes regarding their availability, reliability, communication, and teaching/learning style (as applicable). " +
                "Provide a concise overview. Do not invent information. If reviews are sparse or contradictory, mention that. " +
                "Keep the summary to 2-4 sentences.\n\nReviews:\n\"\"\"\n" + allReviews + "\n\"\"\"";

        Log.d(TAG, "AI Review Summary Prompt (first 200 chars): " + prompt.substring(0, Math.min(prompt.length(), 200)));

        executorService.execute(() -> {
            try {
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                JSONArray contentsArray = new JSONArray();
                JSONObject userContent = new JSONObject();
                JSONArray partsArray = new JSONArray();
                JSONObject textPart = new JSONObject();

                textPart.put("text", prompt);
                partsArray.put(textPart);
                userContent.put("parts", partsArray);
                contentsArray.put(userContent);
                payload.put("contents", contentsArray);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                StringBuilder response = new StringBuilder();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                    }
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String generatedSummaryText = "Error: Could not parse AI summary.";
                    if (jsonResponse.has("candidates")) {
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        if (candidates.length() > 0) {
                            JSONObject firstCandidate = candidates.getJSONObject(0);
                            if (firstCandidate.has("content")) {
                                JSONObject candidateContent = firstCandidate.getJSONObject("content");
                                if (candidateContent.has("parts")) {
                                    JSONArray responseParts = candidateContent.getJSONArray("parts");
                                    if (responseParts.length() > 0 && responseParts.getJSONObject(0).has("text")) {
                                        generatedSummaryText = responseParts.getJSONObject(0).getString("text");
                                    }
                                }
                            } else if (firstCandidate.has("finishReason") && !"STOP".equals(firstCandidate.getString("finishReason"))) {
                                generatedSummaryText = "AI summary generation stopped due to: " + firstCandidate.getString("finishReason");
                            }
                        }
                    } else if (jsonResponse.has("error")) {
                        JSONObject errorObj = jsonResponse.getJSONObject("error");
                        generatedSummaryText = "AI Error: " + errorObj.optString("message", "Unknown error.");
                    }

                    final String finalGeneratedSummary = generatedSummaryText;
                    runOnUiThread(() -> {
                        tvTutorAvailability.setText(finalGeneratedSummary);
                        showLoading(false, false);
                    });

                } else {
                    StringBuilder errorResponseBuilder = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) { errorResponseBuilder.append(responseLine.trim()); }
                    } catch (Exception ioEx) { Log.e(TAG, "Error reading error stream", ioEx); }
                    String errorDetails = errorResponseBuilder.toString();
                    Log.e(TAG, "Gemini API Error for AI Summary (" + responseCode + "): " + errorDetails);
                    final String finalErrorMsg = "AI Summary Error (" + responseCode + "): " + conn.getResponseMessage() + (errorDetails.isEmpty() ? "" : " - " + errorDetails.substring(0, Math.min(200, errorDetails.length())));
                    runOnUiThread(() -> {
                        Toast.makeText(PartnerProfileActivity.this, finalErrorMsg, Toast.LENGTH_LONG).show();
                        tvTutorAvailability.setText("Could not generate AI summary at this time.");
                        showLoading(false, false);
                        refundCreditsForAISummaryOnError();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Exception during AI Summary API call", e);
                runOnUiThread(() -> {
                    Toast.makeText(PartnerProfileActivity.this, "Error generating AI summary: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    tvTutorAvailability.setText("Could not generate AI summary due to an error.");
                    showLoading(false, false);
                    refundCreditsForAISummaryOnError();
                });
            }
        });
    }

    private void refundCreditsForAISummaryOnError() {
        if (viewingUserPojo == null || viewingUserPojo.getUid() == null) return;
        Log.d(TAG, "Attempting to refund credits for AI Review Summary.");
        double refundedCredits = viewingUserPojo.getCredits() + CREDIT_COST_AI_REVIEW_SUMMARY;
        Map<String, Object> creditUpdate = new HashMap<>();
        creditUpdate.put("credits", refundedCredits);

        userDao.updateUserSpecificFields(viewingUserPojo.getUid(), creditUpdate)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Credits refunded for AI Review Summary. New balance: " + refundedCredits);
                    viewingUserPojo.setCredits(refundedCredits);
                    if(loggedInUser != null) loggedInUser.setUser(viewingUserPojo);
                    Toast.makeText(PartnerProfileActivity.this, "Credits for failed AI summary refunded.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Critical: Failed to refund credits for AI Summary.", e);
                    Toast.makeText(PartnerProfileActivity.this, "Critical Error: Failed to refund credits.", Toast.LENGTH_LONG).show();
                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}
