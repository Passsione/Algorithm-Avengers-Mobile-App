
package com.pbdvmobile.app.fragments;

import android.annotation.SuppressLint;
import android.content.Intent; // Keep if viewProfile uses it
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.TutorProfile; // Keep if viewProfile uses it
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject; // Import Subject
import com.pbdvmobile.app.data.model.User;

public class SessionDetailsFragment extends Fragment {

    TextView subjectTitle, txtStatus, dateView, time, location, tutorNameLabel, // Renamed for clarity
            tutorSubjectsLabel, partnerLabel; // Renamed for clarity
    Button buttonViewPartnerProfile, buttonCancelSession, buttonRescheduleSession, buttonConfirmSession; // Renamed
    ImageView imageViewPartnerProfile; // Renamed
    RatingBar ratingBarPartnerOverall; // Renamed

    // Review Section Views
    CardView cardMyReviewSection, cardPartnerReviewSectionDisplay; // Renamed and new one added
    LinearLayout layoutMyReviewInputArea;
    RatingBar ratingBarMyReviewInput; // Renamed
    EditText editTextMyReviewInput;   // Renamed
    Button buttonSubmitMyReview;      // Renamed
    TextView textViewMySubmittedReviewText, textViewNoPartnerReviewYet;
    RatingBar ratingBarMySubmittedRating, ratingBarPartnerSubmittedRating;
    TextView textViewPartnerSubmittedReviewText, textViewPartnerReviewTitle;
    Button buttonTogglePartnerReview;


    User sessionPartner; // Either tutor or tutee
    DataManager dataManager;
    LogInUser currentUser; // Renamed
    Session currentSession;
    boolean isTutee;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_details, container, false);
    }

    @SuppressLint({"SetTextI18n", "ResourceAsColor"}) // Keep ResourceAsColor if you use it directly
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataManager = DataManager.getInstance(getContext());
        currentUser = LogInUser.getInstance(dataManager);

        assert getArguments() != null;
        currentSession = (Session) getArguments().getSerializable("session");
        if (currentSession == null) {
            // Handle error, session not passed
            Toast.makeText(getContext(), "Error: Session data missing.", Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        isTutee = currentUser.getUser().getStudentNum() == currentSession.getTuteeId();
        // Determine session partner
        if (isTutee) {
            sessionPartner = dataManager.getUserDao().getUserByStudentNum(currentSession.getTutorId());
        } else {
            sessionPartner = dataManager.getUserDao().getUserByStudentNum(currentSession.getTuteeId());
        }

        initializeViews(view); // Initialize all views

        populateSessionDetails();
        populatePartnerDetails();
        configureReviewSections();
        configureActionButtons(currentSession);
    }

    private void initializeViews(View view) {
        // Session Details
        subjectTitle = view.findViewById(R.id.session_detail_subject);
        txtStatus = view.findViewById(R.id.session_detail_status);
        dateView = view.findViewById(R.id.session_detail_date);
        time = view.findViewById(R.id.session_detail_time);
        location = view.findViewById(R.id.session_detail_location);

        // Partner Details (renamed from tutor specific)
        tutorNameLabel = view.findViewById(R.id.session_detail_tutor_name); // Assuming this is partner name
        partnerLabel = view.findViewById(R.id.partner_title); // Assuming this is partner name
        tutorSubjectsLabel = view.findViewById(R.id.session_detail_tutor_subjects); // Assuming this is partner related info
        imageViewPartnerProfile = view.findViewById(R.id.session_detail_tutor_image);
        ratingBarPartnerOverall = view.findViewById(R.id.session_detail_tutor_rating);

        // My Review Section
        cardMyReviewSection = view.findViewById(R.id.card_review_section); // Your existing review card
        layoutMyReviewInputArea = view.findViewById(R.id.layout_review_input_area);
        ratingBarMyReviewInput = view.findViewById(R.id.rabDetails); // Assuming this is your rating input
        editTextMyReviewInput = view.findViewById(R.id.redDetailsReview); // Assuming this is your text input
        buttonSubmitMyReview = view.findViewById(R.id.btn_submit_review);
        // Views to display own submitted review (if you separate display from input)
        // textViewMySubmittedReviewText = view.findViewById(R.id.textViewMySubmittedReviewText);
        // ratingBarMySubmittedRating = view.findViewById(R.id.ratingBarMySubmittedRating);


        // Partner's Review Display Section
        cardPartnerReviewSectionDisplay = view.findViewById(R.id.cardPartnerReviewSection);
        textViewPartnerReviewTitle = view.findViewById(R.id.textViewPartnerReviewTitle);
        ratingBarPartnerSubmittedRating = view.findViewById(R.id.ratingBarPartnerRating);
        textViewPartnerSubmittedReviewText = view.findViewById(R.id.textViewPartnerReviewText);
        textViewNoPartnerReviewYet = view.findViewById(R.id.textViewNoPartnerReview);
        buttonTogglePartnerReview = view.findViewById(R.id.buttonTogglePartnerReview);


        // Action Buttons
        buttonViewPartnerProfile = view.findViewById(R.id.btn_view_tutor_profile); // This is "View Partner Profile"
        buttonCancelSession = view.findViewById(R.id.btn_cancel_booking);
        buttonRescheduleSession = view.findViewById(R.id.btn_reschedule_session); // Not implemented yet
        buttonConfirmSession = view.findViewById(R.id.btn_confirm_booking);       // Not implemented yet
    }


    private void populateSessionDetails() {
        Subject subject = dataManager.getSubjectDao().getSubjectById(currentSession.getSubjectId());
        subjectTitle.setText(subject != null ? subject.getSubjectName() : "Unknown Subject");

        updateStatusUI(currentSession.getStatus());

        String[] dateTimeStart = dataManager.formatDateTime(currentSession.getStartTime().toString());
        String[] dateTimeEnd = dataManager.formatDateTime(currentSession.getEndTime().toString());
        dateView.setText(dateTimeStart[0]);
        time.setText(String.format("%s - %s", dateTimeStart[1], dateTimeEnd[1]));
        location.setText(currentSession.getLocation() != null ? currentSession.getLocation() : "N/A");
    }

    private void populatePartnerDetails() {
        if (sessionPartner != null) {

            tutorNameLabel.setText(String.format("%s %s", sessionPartner.getFirstName(), sessionPartner.getLastName()));
            partnerLabel.setText(isTutee ? "Tutor" : "Tutee");
            double partnerRating = dataManager.getSessionDao().getAverageRatingByStudentNum(
                    sessionPartner.getStudentNum())[isTutee ? 1 : 0];
            if (partnerRating > 0) {
                ratingBarPartnerOverall.setRating((float) partnerRating);
                ratingBarPartnerOverall.setVisibility(View.VISIBLE);
            } else {
                ratingBarPartnerOverall.setVisibility(View.GONE);
            }
            // For tutorSubjectsLabel, display partner's relevant info like Education Level
            tutorSubjectsLabel.setText("Education: " + sessionPartner.getEducationLevel().name());
            Glide.with(this)
                    .load(sessionPartner.getProfileImageUrl())
                    .placeholder(R.mipmap.ic_launcher_round) // Your placeholder
                    .error(R.mipmap.ic_launcher_round)      // Your error placeholder
                    .circleCrop()
                    .into(imageViewPartnerProfile);

            buttonViewPartnerProfile.setOnClickListener(v -> {
                Intent toProfile = new Intent(getContext(), TutorProfile.class);
                // Pass the 'sessionPartner' which could be a tutor or tutee
                // TutorProfile activity might need to handle displaying a generic user profile
                toProfile.putExtra("tutor", sessionPartner); // Assuming TutorProfile can handle any User object
                startActivity(toProfile);
            });

        } else {
            tutorNameLabel.setText(getString(Integer.parseInt("partner_details_unavailable")));
            tutorSubjectsLabel.setText("");
            ratingBarPartnerOverall.setVisibility(View.GONE);
            imageViewPartnerProfile.setImageResource(R.mipmap.ic_launcher_round);
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
        txtStatus.setTextColor(ContextCompat.getColor(getContext(), colorRes));
        // Consider using a background drawable for the status TextView for better visual separation
        // txtStatus.setBackgroundResource(R.drawable.status_background_generic); // and specific ones
    }


    private void configureReviewSections() {

        // --- Configure My Review Section ---
        String myExistingReview = isTutee ? currentSession.getTuteeReview() : currentSession.getTutorReview();
        Double myExistingRating = dataManager.getSessionDao().getAverageRatingByStudentNum(
                sessionPartner.getStudentNum())[isTutee ? 1 : 0];
        boolean canLeaveReview = currentSession.getStatus() == Session.Status.COMPLETED;

        cardMyReviewSection.setVisibility(canLeaveReview ? View.VISIBLE : View.GONE);

        if (canLeaveReview) {
            if (myExistingReview != null && !myExistingReview.isEmpty() || myExistingRating != null && myExistingRating > 0) {
                // Already reviewed
                layoutMyReviewInputArea.setVisibility(View.VISIBLE); // Hide input fields
                buttonSubmitMyReview.setText("Update Review"); // Or hide submit and show a different layout
                // You would ideally have separate TextViews to display the existing review and rating if hiding input.
                // For now, we just prefill and allow update
                editTextMyReviewInput.setText(myExistingReview != null ? myExistingReview : "");
                ratingBarMyReviewInput.setRating(myExistingRating != null ? myExistingRating.floatValue() : 0f);
                // If you want it truly read-only and have separate display views:
                // textViewMySubmittedReviewText.setText(myExistingReview);
                // ratingBarMySubmittedRating.setRating(myExistingRating.floatValue());
                // textViewMySubmittedReviewText.setVisibility(View.VISIBLE);
                // ratingBarMySubmittedRating.setVisibility(View.VISIBLE);


            } else {
                // Not reviewed yet, show input fields
                layoutMyReviewInputArea.setVisibility(View.VISIBLE);
                editTextMyReviewInput.setText("");
                ratingBarMyReviewInput.setRating(0f);
                buttonSubmitMyReview.setText("Submit Review");
            }
            buttonSubmitMyReview.setOnClickListener(v -> submitMyReview());
        }


        // --- Configure Partner's Review Display Section ---
        if (currentSession.getStatus() == Session.Status.COMPLETED) {
            String partnerReview = !isTutee ? currentSession.getTuteeReview() : currentSession.getTutorReview();
            Double partnerRating = dataManager.getSessionDao().getAverageRatingByStudentNum(
                    currentUser.getUser().getStudentNum())[!isTutee ? 1 : 0];
            String partnerRole = !isTutee ? "Tutee's" : "Tutor's";
            textViewPartnerReviewTitle.setText(String.format("%s Feedback", partnerRole));

            if (partnerReview != null && !partnerReview.isEmpty() || partnerRating != null && partnerRating > 0) {
                cardPartnerReviewSectionDisplay.setVisibility(View.VISIBLE);
                buttonTogglePartnerReview.setVisibility(View.VISIBLE); // Show toggle button

                textViewPartnerSubmittedReviewText.setText(partnerReview != null ? partnerReview : "No written feedback.");
                ratingBarPartnerSubmittedRating.setRating(partnerRating != null ? partnerRating.floatValue() : 0f);
                ratingBarPartnerSubmittedRating.setVisibility(partnerRating != null && partnerRating > 0 ? View.VISIBLE : View.GONE);
                textViewNoPartnerReviewYet.setVisibility(View.GONE);

                // Toggle logic
                buttonTogglePartnerReview.setOnClickListener(v -> {
                    if (cardPartnerReviewSectionDisplay.getVisibility() == View.VISIBLE) {
                        cardPartnerReviewSectionDisplay.setVisibility(View.GONE);
                        buttonTogglePartnerReview.setText(String.format("Show %s Review", partnerRole));
                    } else {
                        cardPartnerReviewSectionDisplay.setVisibility(View.VISIBLE);
                        buttonTogglePartnerReview.setText(String.format("Hide %s Review", partnerRole));
                    }
                });
                // Initially hide partner review if you want it toggled
                cardPartnerReviewSectionDisplay.setVisibility(View.GONE);
                buttonTogglePartnerReview.setText(String.format("Show %s Review", partnerRole));


            } else {
                buttonTogglePartnerReview.setVisibility(View.VISIBLE); // Still show button
                cardPartnerReviewSectionDisplay.setVisibility(View.GONE); // Hide card
                buttonTogglePartnerReview.setText(String.format("Show %s Review (Not Yet Submitted)", partnerRole));
                buttonTogglePartnerReview.setEnabled(false); // Disable if no review to show

                // Alternative: Show a "Not reviewed yet" message within the card if card is always visible.
                // cardPartnerReviewSectionDisplay.setVisibility(View.VISIBLE);
                // textViewNoPartnerReviewYet.setVisibility(View.VISIBLE);
                // textViewPartnerSubmittedReviewText.setVisibility(View.GONE);
                // ratingBarPartnerSubmittedRating.setVisibility(View.GONE);
            }
        } else {
            cardPartnerReviewSectionDisplay.setVisibility(View.GONE);
            buttonTogglePartnerReview.setVisibility(View.GONE);
        }
    }

    private void submitMyReview() {
        float rating = ratingBarMyReviewInput.getRating();
        String reviewText = editTextMyReviewInput.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(getContext(), "Please provide a rating (1-5 stars).", Toast.LENGTH_SHORT).show();
            return;
        }
        // Optional: require review text if rating is low, or always require some text
        // if (reviewText.isEmpty()) {
        //    Toast.makeText(getContext(), "Please write a short review.", Toast.LENGTH_SHORT).show();
        //    return;
        // }

        boolean isCurrentUserTutee = currentUser.getUser().getStudentNum() == currentSession.getTuteeId();
        boolean dbSuccess = false;

        if (isCurrentUserTutee) {
            currentSession.setTuteeRating((double) rating); // Update local session object
            currentSession.setTuteeReview(reviewText);
            // Update in DB
            dataManager.getSessionDao().updateTuteeRating(currentSession.getId(), rating);
            dbSuccess = dataManager.getSessionDao().addTuteeReview(currentSession.getId(), reviewText) > 0;
        } else { // Current user is Tutor
            currentSession.setTutorRating((double) rating); // Update local session object
            currentSession.setTutorReview(reviewText);
            // Update in DB
            dataManager.getSessionDao().updateTutorRating(currentSession.getId(), rating);
            dbSuccess = dataManager.getSessionDao().addTutorReview(currentSession.getId(), reviewText) > 0;
        }

        if (dbSuccess) {
            Toast.makeText(getContext(), getString(R.string.review_submitted_toast), Toast.LENGTH_LONG).show();
            configureReviewSections(); // Re-configure to show submitted review and potentially hide input
        } else {
            Toast.makeText(getContext(), "Failed to submit review. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void configureActionButtons(Session session) {
        // Visibility based on session status and user role
        boolean isCurrentUserTutee = currentUser.getUser().getStudentNum() == session.getTuteeId();
        boolean isCurrentUserTutor = currentUser.getUser().getStudentNum() == session.getTutorId();

        buttonCancelSession.setVisibility(View.GONE);
        buttonRescheduleSession.setVisibility(View.GONE);
        buttonConfirmSession.setVisibility(View.GONE);

        switch (session.getStatus()) {
            case PENDING:
                if (isCurrentUserTutor) { // Only tutor can confirm/decline a pending request
                    buttonConfirmSession.setVisibility(View.VISIBLE);
                    buttonConfirmSession.setText("Confirm Session");
                    buttonConfirmSession.setOnClickListener(v -> updateSessionStatus(Session.Status.CONFIRMED, "Session Confirmed!"));

                    buttonCancelSession.setVisibility(View.VISIBLE); // Tutor can also "decline"
                    buttonCancelSession.setText("Decline Session");
                    buttonCancelSession.setOnClickListener(v -> updateSessionStatus(Session.Status.DECLINED, "Session Declined by Tutor."));

                } else if (isCurrentUserTutee) { // Tutee can cancel their pending request
                    buttonCancelSession.setVisibility(View.VISIBLE);
                    buttonCancelSession.setText("Cancel Request");
                    buttonCancelSession.setOnClickListener(v -> updateSessionStatus(Session.Status.CANCELLED, "Session Request Cancelled by You."));
                }
                break;
            case CONFIRMED:
                // Both can cancel a confirmed session (up to a certain point, logic not implemented here)
                buttonCancelSession.setVisibility(View.VISIBLE);
                buttonCancelSession.setText("Cancel Session");
                buttonCancelSession.setOnClickListener(v -> updateSessionStatus(Session.Status.CANCELLED, "Session Cancelled."));
                // Reschedule might also be an option here
                 buttonRescheduleSession.setVisibility(View.VISIBLE);
                 buttonRescheduleSession.setText("Begin Session");
                break;
            case COMPLETED:
            case CANCELLED:
            case DECLINED:
                // No actions for these final states usually, or perhaps "Book Again"
                break;
        }
    }

    private void updateSessionStatus(Session.Status newStatus, String toastMessage) {
        int rowsAffected = dataManager.getSessionDao().updateSessionStatus(currentSession.getId(), newStatus);
        if (rowsAffected > 0) {
            currentSession.setStatus(newStatus); // Update local object
            updateStatusUI(newStatus); // Update the status TextView
            configureActionButtons(currentSession); // Re-configure buttons based on new status
            configureReviewSections(); // Re-check if review section should appear
            Toast.makeText(getContext(), toastMessage, Toast.LENGTH_LONG).show();
            // Optionally, navigate back or refresh a list if needed
            if (newStatus == Session.Status.CANCELLED || newStatus == Session.Status.DECLINED || newStatus == Session.Status.CONFIRMED) {
                // If a final action is taken, consider popping backstack
                // getParentFragmentManager().popBackStack();
            }
        } else {
            Toast.makeText(getContext(), "Failed to update session status.", Toast.LENGTH_SHORT).show();
        }
    }
}
