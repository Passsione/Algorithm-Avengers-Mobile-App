package com.pbdvmobile.app.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.pbdvmobile.app.PartnerProfileActivity;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.dao.SessionDao;
import com.pbdvmobile.app.data.dao.UserDao;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionDetailsFragment extends Fragment {
    private static final String TAG = "SessionDetailsFragment";

    private TextView subjectTitle, txtStatus, dateView, timeView, locationView,
            partnerNameLabel, partnerInfoLabel, partnerRoleLabel;
    private Button buttonViewPartnerProfile, buttonCancelSession, buttonStartSession, buttonConfirmSession, buttonDeclineSession;
    private ImageView imageViewPartnerProfile;
    private RatingBar ratingBarPartnerOverall;
    private ProgressBar detailsProgressBar;

    private CardView cardMyReviewSection, cardPartnerReviewSectionDisplay;
    private LinearLayout layoutMyReviewInputArea;
    private RatingBar ratingBarMyReviewInput;
    private EditText editTextMyReviewInput;
    private Button buttonSubmitMyReview;
    private TextView textViewNoPartnerReviewYet, textViewPartnerReviewTitleDisplay, textViewPartnerSubmittedReviewText;
    private RatingBar  ratingBarPartnerSubmittedRatingDisplay;
    private Button buttonTogglePartnerReview;


    private User sessionPartnerPojo;
    private LogInUser loggedInUser;
    private User currentUserPojo;
    private Session currentSessionPojo;
    private String currentSessionId;

    private SessionDao sessionDao;
    private UserDao userDao;

    private boolean isCurrentUserTutee;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null) return;

        loggedInUser = LogInUser.getInstance();
        currentUserPojo = loggedInUser.getUser();
        sessionDao = new SessionDao();
        userDao = new UserDao();

        assert getArguments() != null;
        currentSessionId = getArguments().getString("session_id");

        if (currentUserPojo == null || currentSessionId == null || currentSessionId.isEmpty()) {
            Toast.makeText(getContext(), "Error: User or Session data missing.", Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        initializeViews(view);
        loadSessionDetails();
    }

    private void initializeViews(View view) {
        detailsProgressBar = view.findViewById(R.id.detailsProgressBar); // Add to XML
        subjectTitle = view.findViewById(R.id.session_detail_subject);
        txtStatus = view.findViewById(R.id.session_detail_status);
        dateView = view.findViewById(R.id.session_detail_date);
        timeView = view.findViewById(R.id.session_detail_time);
        locationView = view.findViewById(R.id.session_detail_location);

        detailsProgressBar = view.findViewById(R.id.detailsProgressBar);
        subjectTitle = view.findViewById(R.id.session_detail_subject);
        txtStatus = view.findViewById(R.id.session_detail_status);
        dateView = view.findViewById(R.id.session_detail_date);
        timeView = view.findViewById(R.id.session_detail_time);     // Reverted (was session_detail_time)
        locationView = view.findViewById(R.id.session_detail_location); // Reverted (was session_detail_location)

        // Partner Details
        partnerNameLabel = view.findViewById(R.id.session_detail_tutor_name); // Reverted
        partnerRoleLabel = view.findViewById(R.id.partner_title); // Kept new distinct ID
        partnerInfoLabel = view.findViewById(R.id.session_detail_tutor_subjects); // Reverted
        imageViewPartnerProfile = view.findViewById(R.id.session_detail_tutor_image); // Reverted
        ratingBarPartnerOverall = view.findViewById(R.id.session_detail_tutor_rating); // Reverted

        // My Review Section
        cardMyReviewSection = view.findViewById(R.id.card_review_section);
        layoutMyReviewInputArea = view.findViewById(R.id.layout_review_input_area);
        ratingBarMyReviewInput = view.findViewById(R.id.rabDetails);          // Reverted
        editTextMyReviewInput = view.findViewById(R.id.redDetailsReview);      // Reverted
        buttonSubmitMyReview = view.findViewById(R.id.btn_submit_review);     // Reverted

        // Partner's Review Display Section
        cardPartnerReviewSectionDisplay = view.findViewById(R.id.cardPartnerReviewSection); // Reverted
        textViewPartnerReviewTitleDisplay = view.findViewById(R.id.textViewPartnerReviewTitle); // Reverted
        ratingBarPartnerSubmittedRatingDisplay = view.findViewById(R.id.ratingBarPartnerRating); // Reverted
        textViewPartnerSubmittedReviewText = view.findViewById(R.id.textViewPartnerReviewText); // Reverted
        textViewNoPartnerReviewYet = view.findViewById(R.id.textViewNoPartnerReview); // Reverted
        buttonTogglePartnerReview = view.findViewById(R.id.buttonTogglePartnerReview); // Reverted

        // Action Buttons
        buttonViewPartnerProfile = view.findViewById(R.id.btn_view_tutor_profile); // Reverted
        buttonCancelSession = view.findViewById(R.id.btn_cancel_booking);    // Reverted
        buttonStartSession = view.findViewById(R.id.btn_reschedule_session); // Reverted (Functionality changed, but ID reverted to original for "reschedule" concept)
        buttonConfirmSession = view.findViewById(R.id.btn_confirm_booking);   // Reverted
        buttonDeclineSession = view.findViewById(R.id.btn_decline_session); // Kept new distinct ID
    }

    private void showLoading(boolean isLoading) {
        if (detailsProgressBar != null) detailsProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // Hide/show main content view group if you have one
    }

    private void loadSessionDetails() {
        showLoading(true);
        sessionDao.getSessionById(currentSessionId).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentSessionPojo = documentSnapshot.toObject(Session.class);
                if (currentSessionPojo != null) {
                    currentSessionPojo.setId(documentSnapshot.getId()); // Set Firestore ID
                    isCurrentUserTutee = currentUserPojo.getUid().equals(currentSessionPojo.getTuteeUid());
                    String partnerUid = isCurrentUserTutee ? currentSessionPojo.getTutorUid() : currentSessionPojo.getTuteeUid();
                    loadPartnerDetails(partnerUid); // This will then call populateAllUIData
                } else {
                    handleLoadError("Failed to parse session data.");
                }
            } else {
                handleLoadError("Session not found.");
            }
        }).addOnFailureListener(e -> handleLoadError("Error loading session: " + e.getMessage()));
    }

    private void loadPartnerDetails(String partnerUid) {
        userDao.getUserByUid(partnerUid).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                sessionPartnerPojo = doc.toObject(User.class);
                if (sessionPartnerPojo != null) sessionPartnerPojo.setUid(doc.getId());
            } else {
                Log.w(TAG, "Partner (UID: " + partnerUid + ") not found.");
            }
            populateAllUIData(); // Populate UI now that partner (or lack thereof) is known
            showLoading(false);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading partner details", e);
            populateAllUIData(); // Still populate session details even if partner fails
            showLoading(false);
        });
    }

    private void handleLoadError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        showLoading(false);
        if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
    }


    private void populateAllUIData() {
        if (currentSessionPojo == null) return; // Should not happen if loadSessionDetails succeeded

        populateSessionInfo();
        populatePartnerInfo();
        configureReviewSectionVisibility(); // Initial visibility based on status
        loadAndDisplayMyReview();
        loadAndDisplayPartnerReview();
        configureActionButtons();
    }

    private void populateSessionInfo() {
        subjectTitle.setText(currentSessionPojo.getSubjectName() != null ? currentSessionPojo.getSubjectName().split(":")[0] : "Unknown Subject");
        updateStatusUI(currentSessionPojo.getStatus());

        if (currentSessionPojo.getStartTime() != null) {
            dateView.setText(dateFormat.format(currentSessionPojo.getStartTime().toDate()));
            if (currentSessionPojo.getEndTime() != null) {
                timeView.setText(String.format("%s - %s",
                        timeFormat.format(currentSessionPojo.getStartTime().toDate()),
                        timeFormat.format(currentSessionPojo.getEndTime().toDate())));
            } else {
                timeView.setText(timeFormat.format(currentSessionPojo.getStartTime().toDate()) + " - (No end time)");
            }
        } else {
            dateView.setText("Date not set");
            timeView.setText("Time not set");
        }
        locationView.setText(currentSessionPojo.getLocation() != null ? currentSessionPojo.getLocation() : "N/A");
    }

    private void populatePartnerInfo() {
        partnerRoleLabel.setText(isCurrentUserTutee ? "Tutor Details" : "Tutee Details");
        if (sessionPartnerPojo != null) {
            partnerNameLabel.setText(String.format("%s %s", sessionPartnerPojo.getFirstName(), sessionPartnerPojo.getLastName()));
            partnerInfoLabel.setText("Education: " + (sessionPartnerPojo.getEducationLevel() != null ? sessionPartnerPojo.getEducationLevel().name().replace("_", " ") : "N/A"));

            Glide.with(this)
                    .load(sessionPartnerPojo.getProfileImageUrl())
                    .placeholder(R.drawable.avatar_1).error(R.drawable.avatar_1).circleCrop()
                    .into(imageViewPartnerProfile);

            // Display partner's overall rating (as tutor if partner is tutor, as tutee if partner is tutee)
            double partnerOverallRating = isCurrentUserTutee ? sessionPartnerPojo.getAverageRatingAsTutor() : sessionPartnerPojo.getAverageRatingAsTutee();
            if (partnerOverallRating > 0) {
                ratingBarPartnerOverall.setRating((float) partnerOverallRating);
                ratingBarPartnerOverall.setVisibility(View.VISIBLE);
            } else {
                ratingBarPartnerOverall.setVisibility(View.GONE);
            }
            buttonViewPartnerProfile.setVisibility(View.VISIBLE);
            buttonViewPartnerProfile.setOnClickListener(v -> {
                Intent toProfile = new Intent(getContext(), PartnerProfileActivity.class);
                toProfile.putExtra("tutor", sessionPartnerPojo); // PartnerProfileActivity needs to handle generic "partner"
                startActivity(toProfile);
            });
        } else {
            partnerNameLabel.setText("Partner details unavailable");
            partnerInfoLabel.setText("");
            ratingBarPartnerOverall.setVisibility(View.GONE);
            imageViewPartnerProfile.setImageResource(R.drawable.avatar_1);
            buttonViewPartnerProfile.setVisibility(View.GONE);
        }
    }

    private void updateStatusUI(Session.Status status) {
        txtStatus.setText(status.name());
        int colorRes;
        switch (status) {
            case CONFIRMED: colorRes = R.color.status_confirmed; break;
            case COMPLETED: colorRes = R.color.status_completed; break;
            case PENDING:   colorRes = R.color.status_pending;   break;
            case CANCELLED: colorRes = R.color.status_cancelled; break;
            case DECLINED:  colorRes = R.color.status_declined;  break;
            default:        colorRes = R.color.status_generic_gray; break;
        }
        if(getContext() != null) txtStatus.setTextColor(ContextCompat.getColor(getContext(), colorRes));
    }

    private void configureReviewSectionVisibility() {
        boolean canLeaveReview = currentSessionPojo.getStatus() == Session.Status.COMPLETED;
        cardMyReviewSection.setVisibility(canLeaveReview ? View.VISIBLE : View.GONE);
        cardPartnerReviewSectionDisplay.setVisibility(canLeaveReview ? View.VISIBLE : View.GONE);
        buttonTogglePartnerReview.setVisibility(canLeaveReview ? View.VISIBLE : View.GONE);

        if (canLeaveReview) {
            // Default state for partner review display (e.g., initially hidden)
            textViewPartnerSubmittedReviewText.setVisibility(View.GONE);
            ratingBarPartnerSubmittedRatingDisplay.setVisibility(View.GONE);
            textViewNoPartnerReviewYet.setVisibility(View.VISIBLE); // Show this first
            buttonTogglePartnerReview.setText("Show Partner's Review");
        }
    }

    private void loadAndDisplayMyReview() {
        if (currentSessionPojo.getStatus() != Session.Status.COMPLETED) {
            layoutMyReviewInputArea.setVisibility(View.GONE);
            buttonSubmitMyReview.setVisibility(View.GONE);
            return;
        }

        String myExistingReview = isCurrentUserTutee ? currentSessionPojo.getTuteeReview() : currentSessionPojo.getTutorReview();
        Double myExistingRating = isCurrentUserTutee ? currentSessionPojo.getTuteeRating() : currentSessionPojo.getTutorRating();

        if (myExistingReview != null || (myExistingRating != null && myExistingRating > 0)) {
            // Already reviewed: Show submitted review, hide input
            layoutMyReviewInputArea.setVisibility(View.GONE);
            buttonSubmitMyReview.setVisibility(View.GONE);

            editTextMyReviewInput.setText(myExistingReview != null ? myExistingReview : "No written feedback provided.");

            if (myExistingRating != null && myExistingRating > 0) {
                ratingBarMyReviewInput.setRating(myExistingRating.floatValue());
            }
        } else {
            // Not reviewed yet: Show input fields
            layoutMyReviewInputArea.setVisibility(View.VISIBLE);
            editTextMyReviewInput.setText("");
            ratingBarMyReviewInput.setRating(0f);
            buttonSubmitMyReview.setVisibility(View.VISIBLE);
            buttonSubmitMyReview.setText("Submit My Review");
            buttonSubmitMyReview.setOnClickListener(v -> submitMyReview());

        }
    }

    private void loadAndDisplayPartnerReview() {
        if (currentSessionPojo.getStatus() != Session.Status.COMPLETED) {
            cardPartnerReviewSectionDisplay.setVisibility(View.GONE);
            buttonTogglePartnerReview.setVisibility(View.GONE);
            return;
        }
        cardPartnerReviewSectionDisplay.setVisibility(View.VISIBLE); // Card itself is visible
        buttonTogglePartnerReview.setVisibility(View.VISIBLE);


        String partnerReviewText = !isCurrentUserTutee ? currentSessionPojo.getTuteeReview() : currentSessionPojo.getTutorReview();
        Double partnerRatingValue = !isCurrentUserTutee ? currentSessionPojo.getTuteeRating() : currentSessionPojo.getTutorRating();
        String partnerRole = !isCurrentUserTutee ? "Tutee's" : "Tutor's";
        textViewPartnerReviewTitleDisplay.setText(String.format("%s Feedback", partnerRole));

        final boolean hasPartnerReview = (partnerReviewText != null && !partnerReviewText.isEmpty()) || (partnerRatingValue != null && partnerRatingValue > 0);

        if (hasPartnerReview) {
            textViewPartnerSubmittedReviewText.setText(partnerReviewText != null ? partnerReviewText : "No written feedback.");
            if (partnerRatingValue != null && partnerRatingValue > 0) {
                ratingBarPartnerSubmittedRatingDisplay.setRating(partnerRatingValue.floatValue());
            }
            // Initially hide the content, user clicks button to show
            textViewPartnerSubmittedReviewText.setVisibility(View.GONE);
            ratingBarPartnerSubmittedRatingDisplay.setVisibility(View.GONE);
            textViewNoPartnerReviewYet.setVisibility(View.VISIBLE);
            textViewNoPartnerReviewYet.setText(String.format("%s has submitted feedback.", partnerRole));
            buttonTogglePartnerReview.setText(String.format("Show %s Review", partnerRole));
            buttonTogglePartnerReview.setEnabled(true);

        } else {
            textViewNoPartnerReviewYet.setText(String.format("%s has not submitted feedback yet.", partnerRole));
            textViewNoPartnerReviewYet.setVisibility(View.VISIBLE);
            textViewPartnerSubmittedReviewText.setVisibility(View.GONE);
            ratingBarPartnerSubmittedRatingDisplay.setVisibility(View.GONE);
            buttonTogglePartnerReview.setText(String.format("%s Review (Not Submitted)", partnerRole));
            buttonTogglePartnerReview.setEnabled(false);
        }

        buttonTogglePartnerReview.setOnClickListener(v -> {
            if (textViewPartnerSubmittedReviewText.getVisibility() == View.VISIBLE) { // If showing, hide it
                textViewPartnerSubmittedReviewText.setVisibility(View.GONE);
                ratingBarPartnerSubmittedRatingDisplay.setVisibility(View.GONE);
                textViewNoPartnerReviewYet.setVisibility(View.VISIBLE); // Show placeholder again
                if(hasPartnerReview) textViewNoPartnerReviewYet.setText(String.format("%s has submitted feedback.", partnerRole));

                buttonTogglePartnerReview.setText(String.format("Show %s Review", partnerRole));
            } else { // If hidden, show it (only if review exists)
                if (hasPartnerReview) {
                    textViewPartnerSubmittedReviewText.setVisibility(View.VISIBLE);
                    if (partnerRatingValue != null && partnerRatingValue > 0) ratingBarPartnerSubmittedRatingDisplay.setVisibility(View.VISIBLE);
                    textViewNoPartnerReviewYet.setVisibility(View.GONE);
                    buttonTogglePartnerReview.setText(String.format("Hide %s Review", partnerRole));
                }
            }
        });
    }


    private void submitMyReview() {
        float rating = ratingBarMyReviewInput.getRating();
        String reviewText = editTextMyReviewInput.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(getContext(), "Please provide a rating (1-5 stars).", Toast.LENGTH_SHORT).show();
            return;
        }
        buttonSubmitMyReview.setEnabled(false); // Prevent double submission

        Task<Void> updateTask;
        if (isCurrentUserTutee) { // Current user is Tutee, reviewing Tutor
            updateTask = sessionDao.addTuteeReview(currentSessionId, reviewText, rating);
        } else { // Current user is Tutor, reviewing Tutee
            updateTask = sessionDao.addTutorReview(currentSessionId, reviewText, rating);
        }

        updateTask.addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Review submitted successfully!", Toast.LENGTH_LONG).show();
            // Refresh the session data to get the latest review status
            loadSessionDetails(); // This will re-fetch and re-populate everything
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to submit review: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to submit review", e);
            buttonSubmitMyReview.setEnabled(true);
        });
    }

    private void configureActionButtons() {
        buttonCancelSession.setVisibility(View.GONE);
        buttonStartSession.setVisibility(View.GONE);
        buttonConfirmSession.setVisibility(View.GONE);
        buttonDeclineSession.setVisibility(View.GONE);

        switch (currentSessionPojo.getStatus()) {
            case PENDING:
                if (!isCurrentUserTutee) { // Tutor's action for PENDING
                    buttonConfirmSession.setVisibility(View.VISIBLE);
                    buttonConfirmSession.setOnClickListener(v -> updateSessionStatusWithAction(Session.Status.CONFIRMED, "Session Confirmed!"));
                    buttonDeclineSession.setVisibility(View.VISIBLE);
                    buttonDeclineSession.setOnClickListener(v -> updateSessionStatusWithAction(Session.Status.DECLINED, "Session Declined."));
                } else { // Tutee can cancel their PENDING request
                    buttonCancelSession.setVisibility(View.VISIBLE);
                    buttonCancelSession.setText("Cancel Request");
                    buttonCancelSession.setOnClickListener(v -> updateSessionStatusWithAction(Session.Status.CANCELLED, "Request Cancelled."));
                }
                break;
            case CONFIRMED:
                // Allow starting session if it's near start time or ongoing
                Date now = new Date();
                long fiveMinutesInMillis = 5 * 60 * 1000;
                boolean canStart = currentSessionPojo.getStartTime() != null &&
                        (now.getTime() >= (currentSessionPojo.getStartTime().toDate().getTime() - fiveMinutesInMillis)) &&
                        now.before(currentSessionPojo.getEndTime().toDate());

                if (canStart) {
                    buttonStartSession.setVisibility(View.VISIBLE);
                    buttonStartSession.setText("Begin Session");
                    buttonStartSession.setOnClickListener(v -> updateSessionStatusWithAction(Session.Status.COMPLETED, "Session Started (Marked as Completed)!"));
                } else if (currentSessionPojo.getStartTime() != null && now.before(currentSessionPojo.getStartTime().toDate())) {
                    // If not yet time to start, allow cancellation
                    buttonCancelSession.setVisibility(View.VISIBLE);
                    buttonCancelSession.setText("Cancel Session");
                    buttonCancelSession.setOnClickListener(v -> updateSessionStatusWithAction(Session.Status.CANCELLED, "Session Cancelled."));
                }
                // If it's past endTime but still CONFIRMED, it should be updated by a cleanup job.
                // For UI, might show "Session Overdue" or similar.
                break;
            // For COMPLETED, CANCELLED, DECLINED - usually no direct actions from details screen.
        }
    }

    private void updateSessionStatusWithAction(Session.Status newStatus, String toastMessage) {
        showLoading(true);
        sessionDao.updateSessionStatus(currentSessionId, newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), toastMessage, Toast.LENGTH_LONG).show();
                    loadSessionDetails(); // Reload to reflect changes and update buttons/reviews
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to update session status", e);
                    showLoading(false); // Hide loading on failure too
                });
    }
}
