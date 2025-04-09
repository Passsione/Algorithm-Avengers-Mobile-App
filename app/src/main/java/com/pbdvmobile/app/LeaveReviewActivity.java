package com.pbdvmobile.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.model.Review;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.ReviewService;
import com.pbdvmobile.app.data.SessionService;
import com.pbdvmobile.app.data.UserService;

public class LeaveReviewActivity extends AppCompatActivity {

    private TextView tvReviewFor;
    private RatingBar ratingBar;
    private EditText etComment;
    private Button btnSubmit;

    private String sessionId;
    private Session session;
    private User otherUser;

    private SessionService sessionService;
    private UserService userService;
    private ReviewService reviewService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_review);

        sessionId = getIntent().getStringExtra("session_id");
        sessionService = new SessionService();
        userService = new UserService();
        reviewService = new ReviewService();

        tvReviewFor = findViewById(R.id.tv_review_for);
        ratingBar = findViewById(R.id.rating_bar);
        etComment = findViewById(R.id.et_comment);
        btnSubmit = findViewById(R.id.btn_submit);

        // Load session data
        loadSessionData();

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReview();
            }
        });
    }

    private void loadSessionData() {
        sessionService.getSessionById(sessionId, new SessionService.SessionDetailCallback() {
            @Override
            public void onSuccess(Session loadedSession) {
                session = loadedSession;

                // Get current user and determine who to review
                User currentUser = userService.getCurrentUser();
                String otherUserId;

                if (currentUser.getId().equals(session.getTutorId())) {
                    // Current user is tutor, reviewing tutee
                    otherUserId = session.getTuteeId();
                } else {
                    // Current user is tutee, reviewing tutor
                    otherUserId = session.getTutorId();
                }

                userService.getUserById(otherUserId, new UserService.UserDetailCallback() {
                    @Override
                    public void onSuccess(User user) {
                        otherUser = user;
                        String userType = user.getUserType() == User.UserType.TUTOR ? "tutor" : "tutee";
                        tvReviewFor.setText("Leave a review for " + user.getFullName() + " as a " + userType);
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(LeaveReviewActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(LeaveReviewActivity.this, "Error loading session data", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        String comment = etComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        User currentUser = userService.getCurrentUser();

        Review review = new Review();
        review.setSessionId(sessionId);
        review.setRating(rating);
        review.setComment(comment);
        review.setReviewerId(currentUser.getId());
        review.setRevieweeId(otherUser.getId());

        reviewService.submitReview(review, new ReviewService.ReviewCallback() {
            @Override
            public void onSuccess(String reviewId) {
                // Update session with review ID
                if (currentUser.getUserType() == User.UserType.TUTOR) {
                    sessionService.updateTutorReview(sessionId, reviewId, new SessionService.SessionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(LeaveReviewActivity.this, "Review submitted successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(LeaveReviewActivity.this, "Error updating session: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    sessionService.updateTuteeReview(sessionId, reviewId, new SessionService.SessionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(LeaveReviewActivity.this, "Review submitted successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(LeaveReviewActivity.this, "Error updating session: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(LeaveReviewActivity.this, "Error submitting review: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}