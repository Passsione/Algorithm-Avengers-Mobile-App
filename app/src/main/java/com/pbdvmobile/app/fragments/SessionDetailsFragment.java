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
    private RatingBar ratingBarPartnerSubmittedRatingDisplay;
    private Button buttonTogglePartnerReview;


    private User sessionPartnerPojo;
    private LogInUser loggedInUser;
    private User currentUserPojo;
    private Session currentSessionPojo;
    private String currentSessionId;

    private SessionDao sessionDao;
    private UserDao userDao;

    private boolean isCurrentUserTutee;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() == null) {
            Log.e(TAG, "Context is null in onViewCreated, cannot proceed.");
            return;
        }

        loggedInUser = LogInUser.getInstance();
        currentUserPojo = loggedInUser.getUser(); // Using the method from your provided LogInUser
        sessionDao = new SessionDao();
        userDao = new UserDao();

        if (getArguments() != null) {
            currentSessionId = getArguments().getString("session_id");
        } else {
            Toast.makeText(getContext(), "Error: Session ID missing.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "getArguments() is null or session_id is missing.");
            if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
            return;
        }


        if (currentUserPojo == null || currentUserPojo.getUid() == null ) {
            Toast.makeText(getContext(), "Error: Current user data missing or invalid. Please re-login.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "currentUserPojo or its UID is null. currentUserPojo: " + currentUserPojo);
            if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
            return;
        }
        if (currentSessionId == null || currentSessionId.isEmpty()){
            Toast.makeText(getContext(), "Error: Session ID is invalid.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "currentSessionId is null or empty.");
            if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
            return;
        }

        initializeViews(view);
        loadSessionDetails();
    }

    private void initializeViews(View view) {
        detailsProgressBar = view.findViewById(R.id.detailsProgressBar);
        subjectTitle = view.findViewById(R.id.session_detail_subject);
        txtStatus = view.findViewById(R.id.session_detail_status);
        dateView = view.findViewById(R.id.session_detail_date);
        timeView = view.findViewById(R.id.session_detail_time);
        locationView = view.findViewById(R.id.session_detail_location);

        partnerNameLabel = view.findViewById(R.id.session_detail_tutor_name);
        partnerRoleLabel = view.findViewById(R.id.partner_title);
        partnerInfoLabel = view.findViewById(R.id.session_detail_tutor_subjects);
        imageViewPartnerProfile = view.findViewById(R.id.session_detail_tutor_image);
        ratingBarPartnerOverall = view.findViewById(R.id.session_detail_tutor_rating);

        cardMyReviewSection = view.findViewById(R.id.card_review_section);
        layoutMyReviewInputArea = view.findViewById(R.id.layout_review_input_area);
        ratingBarMyReviewInput = view.findViewById(R.id.rabDetails);
        editTextMyReviewInput = view.findViewById(R.id.redDetailsReview);
        buttonSubmitMyReview = view.findViewById(R.id.btn_submit_review);

        cardPartnerReviewSectionDisplay = view.findViewById(R.id.cardPartnerReviewSection);
        textViewPartnerReviewTitleDisplay = view.findViewById(R.id.textViewPartnerReviewTitle);
        ratingBarPartnerSubmittedRatingDisplay = view.findViewById(R.id.ratingBarPartnerRating);
        textViewPartnerSubmittedReviewText = view.findViewById(R.id.textViewPartnerReviewText);
        textViewNoPartnerReviewYet = view.findViewById(R.id.textViewNoPartnerReview);
        buttonTogglePartnerReview = view.findViewById(R.id.buttonTogglePartnerReview);

        buttonViewPartnerProfile = view.findViewById(R.id.btn_view_tutor_profile);
        buttonCancelSession = view.findViewById(R.id.btn_cancel_booking);
        buttonStartSession = view.findViewById(R.id.btn_reschedule_session);
        buttonConfirmSession = view.findViewById(R.id.btn_confirm_booking);
        buttonDeclineSession = view.findViewById(R.id.btn_decline_session);
    }

    private void showLoading(boolean isLoading) {
        if (detailsProgressBar != null) detailsProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        View mainContent = getView() != null ? getView().findViewById(R.id.session_details_content) : null;
        if (mainContent != null) {
            mainContent.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void loadSessionDetails() {
        showLoading(true);
        Log.d(TAG, "Loading session details for ID: " + currentSessionId);
        sessionDao.getSessionById(currentSessionId).addOnSuccessListener(documentSnapshot -> {
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "Fragment not attached or context null during session load success.");
                return;
            }
            if (documentSnapshot.exists()) {
                currentSessionPojo = documentSnapshot.toObject(Session.class);
                if (currentSessionPojo != null) {
                    currentSessionPojo.setId(documentSnapshot.getId());
                    Log.d(TAG, "Session data fetched: " + currentSessionPojo.getSubjectName());

                    String myUid = currentUserPojo.getUid();
                    String tutorUid = currentSessionPojo.getTutorUid();
                    String tuteeUid = currentSessionPojo.getTuteeUid();
                    String partnerUid = null;

                    if (myUid == null) {
                        handleLoadError("Current user UID is null. Cannot determine role.");
                        return;
                    }
                    if (tutorUid == null || tuteeUid == null) {
                        handleLoadError("Session data is incomplete (missing tutor/tutee UID).");
                        return;
                    }

                    if (myUid.equals(tuteeUid)) {
                        isCurrentUserTutee = true;
                        partnerUid = tutorUid;
                    } else if (myUid.equals(tutorUid)) {
                        isCurrentUserTutee = false;
                        partnerUid = tuteeUid;
                    } else {
                        Log.e(TAG, "Critical: Current user (" + myUid + ") is neither tutor (" + tutorUid + ") nor tutee (" + tuteeUid + ") for session " + currentSessionId);
                        handleLoadError("You are not authorized to view this session's details.");
                        return;
                    }

                    Log.d(TAG, "Current user isTutee: " + isCurrentUserTutee + ", Partner UID: " + partnerUid);

                    if (partnerUid == null || partnerUid.isEmpty()) {
                        Log.e(TAG, "Partner UID is missing or empty for session: " + currentSessionId);
                        sessionPartnerPojo = null;
                        populateAllUIData();
                        showLoading(false);
                    } else {
                        loadPartnerDetails(partnerUid);
                    }
                } else {
                    handleLoadError("Failed to parse session data.");
                }
            } else {
                handleLoadError("Session with ID '" + currentSessionId + "' not found.");
            }
        }).addOnFailureListener(e -> {
            if (!isAdded() || getContext() == null) return;
            Log.e(TAG, "Error loading session " + currentSessionId, e);
            handleLoadError("Error loading session: " + e.getMessage());
        });
    }

    private void loadPartnerDetails(String partnerUid) {
        Log.d(TAG, "Loading partner details for UID: " + partnerUid);
        userDao.getUserByUid(partnerUid).addOnSuccessListener(doc -> {
            if (!isAdded() || getContext() == null) return;
            if (doc.exists()) {
                sessionPartnerPojo = doc.toObject(User.class);
                if (sessionPartnerPojo != null) {
                    sessionPartnerPojo.setUid(doc.getId());
                    Log.d(TAG, "Partner details loaded: " + sessionPartnerPojo.getFirstName());
                } else {
                    Log.w(TAG, "Partner document exists but failed to parse for UID: " + partnerUid);
                }
            } else {
                Log.w(TAG, "Partner document (UID: " + partnerUid + ") not found.");
                sessionPartnerPojo = null;
            }
            populateAllUIData();
            showLoading(false);
        }).addOnFailureListener(e -> {
            if (!isAdded() || getContext() == null) return;
            Log.e(TAG, "Error loading partner details for UID: " + partnerUid, e);
            sessionPartnerPojo = null;
            populateAllUIData();
            showLoading(false);
        });
    }

    private void handleLoadError(String message) {
        if (getContext() != null && isAdded()) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            Log.e(TAG, "handleLoadError: " + message);
        }
        showLoading(false);
        if (getParentFragmentManager() != null && isAdded()) {
            try {
                getParentFragmentManager().popBackStack();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error popping backstack: " + e.getMessage());
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void populateAllUIData() {
        if (currentSessionPojo == null || !isAdded() || getContext() == null) {
            Log.e(TAG, "Cannot populate UI: session or context is null or fragment not added.");
            return;
        }
        Log.d(TAG, "Populating all UI data for session: " + currentSessionPojo.getId());
        populateSessionInfo();
        populatePartnerInfo();
        configureReviewSectionVisibility();
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
                timeView.setText(String.format(Locale.getDefault(),"%s - %s",
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
        partnerRoleLabel.setText(!isCurrentUserTutee ? "Tutor Details" : "Tutee Details");
        if (sessionPartnerPojo != null) {
            partnerNameLabel.setText(String.format(Locale.getDefault(),"%s %s", sessionPartnerPojo.getFirstName(), sessionPartnerPojo.getLastName()));
            partnerInfoLabel.setText("Education: " + (sessionPartnerPojo.getEducationLevel() != null ? sessionPartnerPojo.getEducationLevel().name().replace("_", " ") : "N/A"));

            if (getContext() != null && isAdded()) {
                Glide.with(this)
                        .load(sessionPartnerPojo.getProfileImageUrl())
                        .placeholder(R.drawable.avatar_1).error(R.drawable.avatar_1).circleCrop()
                        .into(imageViewPartnerProfile);
            }

            double partnerOverallRating = isCurrentUserTutee ? sessionPartnerPojo.getAverageRatingAsTutor() : sessionPartnerPojo.getAverageRatingAsTutee();
            if (partnerOverallRating > 0) {
                ratingBarPartnerOverall.setRating((float) partnerOverallRating);
                ratingBarPartnerOverall.setVisibility(View.VISIBLE);
            } else {
                ratingBarPartnerOverall.setVisibility(View.GONE);
            }
            buttonViewPartnerProfile.setVisibility(View.VISIBLE);
            buttonViewPartnerProfile.setOnClickListener(v -> {
                if (getContext() != null && isAdded()) {
                    Intent toProfile = new Intent(getContext(), PartnerProfileActivity.class);
                    toProfile.putExtra("tutor", sessionPartnerPojo);
                    startActivity(toProfile);
                }
            });
        } else {
            partnerNameLabel.setText("Partner details unavailable");
            partnerInfoLabel.setText("");
            ratingBarPartnerOverall.setVisibility(View.GONE);
            if (getContext() != null && isAdded()) {
                imageViewPartnerProfile.setImageResource(R.drawable.avatar_1);
            }
            buttonViewPartnerProfile.setVisibility(View.GONE);
        }
    }

    private void updateStatusUI(Session.Status status) {
        if (status == null || !isAdded() || getContext() == null) {
            if (txtStatus != null) {
                txtStatus.setText("UNKNOWN");
                if(getContext() != null) txtStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.status_generic_gray));
            }
            return;
        }
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
        txtStatus.setTextColor(ContextCompat.getColor(getContext(), colorRes));
    }

    private void configureReviewSectionVisibility() {
        if (currentSessionPojo == null || currentSessionPojo.getStatus() == null || !isAdded()) return;
        boolean canLeaveReview = currentSessionPojo.getStatus() == Session.Status.COMPLETED;
        cardMyReviewSection.setVisibility(canLeaveReview ? View.VISIBLE : View.GONE);
        cardPartnerReviewSectionDisplay.setVisibility(canLeaveReview ? View.VISIBLE : View.GONE);
        buttonTogglePartnerReview.setVisibility(canLeaveReview ? View.VISIBLE : View.GONE);

        if (canLeaveReview) {
            textViewPartnerSubmittedReviewText.setVisibility(View.GONE);
            ratingBarPartnerSubmittedRatingDisplay.setVisibility(View.GONE);
            textViewNoPartnerReviewYet.setVisibility(View.VISIBLE);
            buttonTogglePartnerReview.setText("Show Partner's Review");
        }
    }

    private void loadAndDisplayMyReview() {
        if (currentSessionPojo == null || currentSessionPojo.getStatus() == null ||
                currentSessionPojo.getStatus() != Session.Status.COMPLETED || !isAdded()) {
            layoutMyReviewInputArea.setVisibility(View.GONE);

            buttonSubmitMyReview.setVisibility(View.GONE);
            return;
        }

        String myExistingReview = isCurrentUserTutee ? currentSessionPojo.getTutorReview() : currentSessionPojo.getTuteeReview();
        Double myExistingRating = isCurrentUserTutee ? currentSessionPojo.getTutorRating() : currentSessionPojo.getTuteeRating();

        if (myExistingReview != null || (myExistingRating != null && myExistingRating > 0)) {
            layoutMyReviewInputArea.setVisibility(View.GONE);
            buttonSubmitMyReview.setVisibility(View.GONE);
            editTextMyReviewInput.setText(myExistingReview != null ? myExistingReview : "No written feedback provided.");

            if (myExistingRating != null && myExistingRating > 0) {
                ratingBarMyReviewInput.setRating(myExistingRating.floatValue());
            } else {

            }
        } else {
            layoutMyReviewInputArea.setVisibility(View.VISIBLE);
            editTextMyReviewInput.setText("");
            ratingBarMyReviewInput.setRating(0f);
            buttonSubmitMyReview.setVisibility(View.VISIBLE);
            buttonSubmitMyReview.setText("Submit My Review");
            buttonSubmitMyReview.setOnClickListener(v -> submitMyReview());

        }
    }

    private void loadAndDisplayPartnerReview() {
        if (currentSessionPojo == null || currentSessionPojo.getStatus() == null ||
                currentSessionPojo.getStatus() != Session.Status.COMPLETED || !isAdded()) {
            cardPartnerReviewSectionDisplay.setVisibility(View.GONE);
            buttonTogglePartnerReview.setVisibility(View.GONE);
            return;
        }
        cardPartnerReviewSectionDisplay.setVisibility(View.VISIBLE);
        buttonTogglePartnerReview.setVisibility(View.VISIBLE);

        String partnerReviewText = isCurrentUserTutee ? currentSessionPojo.getTuteeReview() : currentSessionPojo.getTutorReview();
        Double partnerRatingValue = isCurrentUserTutee ? currentSessionPojo.getTuteeRating() : currentSessionPojo.getTutorRating();
        String partnerRole = isCurrentUserTutee ? "Tutee's" : "Tutor's";
        textViewPartnerReviewTitleDisplay.setText(String.format(Locale.getDefault(),"%s Feedback", partnerRole));

        final boolean hasPartnerReview = (partnerReviewText != null && !partnerReviewText.isEmpty()) || (partnerRatingValue != null && partnerRatingValue > 0);

        if (hasPartnerReview) {
            textViewPartnerSubmittedReviewText.setText(partnerReviewText != null ? partnerReviewText : "No written feedback.");
            if (partnerRatingValue != null && partnerRatingValue > 0) {
                ratingBarPartnerSubmittedRatingDisplay.setRating(partnerRatingValue.floatValue());
            }
            textViewPartnerSubmittedReviewText.setVisibility(View.GONE);
            ratingBarPartnerSubmittedRatingDisplay.setVisibility(View.GONE);
            textViewNoPartnerReviewYet.setVisibility(View.VISIBLE);
            textViewNoPartnerReviewYet.setText(String.format(Locale.getDefault(),"%s has submitted feedback.", partnerRole));
            buttonTogglePartnerReview.setText(String.format(Locale.getDefault(),"Show %s Review", partnerRole));
            buttonTogglePartnerReview.setEnabled(true);
        } else {
            textViewNoPartnerReviewYet.setText(String.format(Locale.getDefault(),"%s has not submitted feedback yet.", partnerRole));
            textViewNoPartnerReviewYet.setVisibility(View.VISIBLE);
            textViewPartnerSubmittedReviewText.setVisibility(View.GONE);
            ratingBarPartnerSubmittedRatingDisplay.setVisibility(View.GONE);
            buttonTogglePartnerReview.setText(String.format(Locale.getDefault(),"%s Review (Not Submitted)", partnerRole));
            buttonTogglePartnerReview.setEnabled(false);
        }

        buttonTogglePartnerReview.setOnClickListener(v -> {
            if (textViewPartnerSubmittedReviewText.getVisibility() == View.VISIBLE) {
                textViewPartnerSubmittedReviewText.setVisibility(View.GONE);
                ratingBarPartnerSubmittedRatingDisplay.setVisibility(View.GONE);
                textViewNoPartnerReviewYet.setVisibility(View.VISIBLE);
                if(hasPartnerReview) textViewNoPartnerReviewYet.setText(String.format(Locale.getDefault(),"%s has submitted feedback.", partnerRole));
                buttonTogglePartnerReview.setText(String.format(Locale.getDefault(),"Show %s Review", partnerRole));
            } else {
                if (hasPartnerReview) {
                    textViewPartnerSubmittedReviewText.setVisibility(View.VISIBLE);
                    if (partnerRatingValue != null && partnerRatingValue > 0) ratingBarPartnerSubmittedRatingDisplay.setVisibility(View.VISIBLE);
                    textViewNoPartnerReviewYet.setVisibility(View.GONE);
                    buttonTogglePartnerReview.setText(String.format(Locale.getDefault(),"Hide %s Review", partnerRole));
                }
            }
        });
    }


    private void submitMyReview() {
        if (getContext() == null || !isAdded()) return;
        float rating = ratingBarMyReviewInput.getRating();
        String reviewText = editTextMyReviewInput.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(getContext(), "Please provide a rating (1-5 stars).", Toast.LENGTH_SHORT).show();
            return;
        }
        buttonSubmitMyReview.setEnabled(false);
        showLoading(true);

        com.google.android.gms.tasks.Task<Void> updateTask;
        if (isCurrentUserTutee) {
            updateTask = sessionDao.addTutorReview(currentSessionId, reviewText, rating);
        } else {
            updateTask = sessionDao.addTuteeReview(currentSessionId, reviewText, rating);
        }

        updateTask.addOnSuccessListener(aVoid -> {
            if (getContext() != null && isAdded()) Toast.makeText(getContext(), "Review submitted successfully!", Toast.LENGTH_LONG).show();
            loadSessionDetails();
        }).addOnFailureListener(e -> {
            if (getContext() != null && isAdded()) Toast.makeText(getContext(), "Failed to submit review: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to submit review", e);
            if(isAdded()) {
                buttonSubmitMyReview.setEnabled(true);
                showLoading(false);
            }
        });
    }

    private void configureActionButtons() {
        if (currentSessionPojo == null || currentSessionPojo.getStatus() == null || !isAdded()) return;
        buttonCancelSession.setVisibility(View.GONE);
        buttonStartSession.setVisibility(View.GONE);
        buttonConfirmSession.setVisibility(View.GONE);
        buttonDeclineSession.setVisibility(View.GONE);

        switch (currentSessionPojo.getStatus()) {
            case PENDING:
                if (!isCurrentUserTutee) {
                    buttonConfirmSession.setVisibility(View.VISIBLE);
                    buttonConfirmSession.setOnClickListener(v -> updateSessionStatusWithAction(Session.Status.CONFIRMED, "Session Confirmed!"));
                    buttonDeclineSession.setVisibility(View.VISIBLE);
                    buttonDeclineSession.setOnClickListener(v -> updateSessionStatusWithAction(Session.Status.DECLINED, "Session Declined."));
                } else {
                    buttonCancelSession.setVisibility(View.VISIBLE);
                    buttonCancelSession.setText("Cancel Request");
                    buttonCancelSession.setOnClickListener(v -> updateSessionStatusWithAction(Session.Status.CANCELLED, "Request Cancelled."));
                }
                break;
            case CONFIRMED:
                Date now = new Date();
                long fiveMinutesInMillis = 5 * 60 * 1000;
                Timestamp startTimeTs = currentSessionPojo.getStartTime();
                Timestamp endTimeTs = currentSessionPojo.getEndTime();

                if (startTimeTs != null && endTimeTs != null) {
                    Date startTime = startTimeTs.toDate();
                    Date endTime = endTimeTs.toDate();
                    boolean canStart = (now.getTime() >= (startTime.getTime() - fiveMinutesInMillis)) && now.before(endTime) && !isCurrentUserTutee;

                    if (canStart) {
                        buttonStartSession.setVisibility(View.VISIBLE);
                        buttonStartSession.setText("Begin Session");
                        buttonStartSession.setOnClickListener(v -> updateSessionStatusWithAction(Session.Status.COMPLETED, "Session Started (Marked as Completed)!"));
                    } else if (now.before(startTime)) {
                        buttonCancelSession.setVisibility(View.VISIBLE);
                        buttonCancelSession.setText("Cancel Session");
                        buttonCancelSession.setOnClickListener(v -> updateSessionStatusWithAction(Session.Status.CANCELLED, "Session Cancelled."));
                    }
                }
                break;
        }
    }

    private void updateSessionStatusWithAction(Session.Status newStatus, String toastMessage) {
        if (getContext() == null || !isAdded()) return;
        showLoading(true);
        sessionDao.updateSessionStatus(currentSessionId, newStatus)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null && isAdded()) Toast.makeText(getContext(), toastMessage, Toast.LENGTH_LONG).show();
                    if(newStatus == Session.Status.COMPLETED){
                        sessionPartnerPojo.setCredits(sessionPartnerPojo.getCredits() + 6);
                        userDao.updateUser(sessionPartnerPojo);
                    }
                    loadSessionDetails();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null && isAdded()) Toast.makeText(getContext(), "Failed to update session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to update session status", e);
                    if(isAdded()) showLoading(false);
                });
    }
}
