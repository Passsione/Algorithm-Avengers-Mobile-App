package com.pbdvmobile.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.dao.SubjectDao; // Firebase DAO
import com.pbdvmobile.app.data.dao.UserDao;   // Firebase DAO
import com.pbdvmobile.app.data.model.Session; // Firebase model
import com.pbdvmobile.app.data.model.Subject; // Firebase model
import com.pbdvmobile.app.data.model.User;    // Firebase model
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import android.util.Log;

public class PartnerReviewAdapter extends RecyclerView.Adapter<PartnerReviewAdapter.ReviewViewHolder> {

    private static final String ADAPTER_TAG = "PartnerReviewAdapter";
    private Context context;
    private List<Session> reviewedSessions; // Sessions where 'partner' received a review
    private User partnerWhoseProfileIsViewed; // The user whose profile is being viewed (renamed for clarity)
    private SimpleDateFormat dateFormat;

    // DAOs for fetching related data
    private final UserDao userDao;
    private final SubjectDao subjectDao;

    public PartnerReviewAdapter(Context context, List<Session> reviewedSessions, User partnerWhoseProfileIsViewed, SubjectDao subjectDao, UserDao userDao) {
        this.context = context;
        this.reviewedSessions = reviewedSessions;
        this.partnerWhoseProfileIsViewed = partnerWhoseProfileIsViewed;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        this.userDao = userDao; // Use passed DAOs
        this.subjectDao = subjectDao;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_partner_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Session session = reviewedSessions.get(position);
        if (session == null || partnerWhoseProfileIsViewed == null || partnerWhoseProfileIsViewed.getUid() == null) {
            Log.e(ADAPTER_TAG, "Session or Partner data is null/invalid in onBindViewHolder.");
            // Optionally hide the item or show an error state
            holder.itemView.setVisibility(View.GONE);
            return;
        }
        holder.itemView.setVisibility(View.VISIBLE);

        String reviewText = null;
        Double rating = null;
        String reviewerUid;

        // Determine if the partner (whose profile is being viewed) was the tutor or tutee in this session.
        // We want to display the review GIVEN TO this partner.
        if (partnerWhoseProfileIsViewed.getUid().equals(session.getTutorUid())) {
            // Partner was the TUTOR for this session. Show review from Tutee.
            reviewText = session.getTuteeReview();
            rating = session.getTuteeRating();
            reviewerUid = session.getTuteeUid();
        } else if (partnerWhoseProfileIsViewed.getUid().equals(session.getTuteeUid())) {
            // Partner was the TUTEE for this session. Show review from Tutor.
            reviewText = session.getTutorReview();
            rating = session.getTutorRating();
            reviewerUid = session.getTutorUid();
        } else {
            reviewerUid = null;
            // This session doesn't seem to directly involve the partner in a reviewable role.
            // This shouldn't happen if reviewedSessions list is filtered correctly.
            Log.w(ADAPTER_TAG, "Session " + session.getId() + " does not directly involve partner " + partnerWhoseProfileIsViewed.getUid() + " in a reviewable role.");
            holder.itemView.setVisibility(View.GONE); // Hide this item
            return;
        }


        if (reviewText != null && !reviewText.isEmpty()) {
            holder.tvReviewText.setText(reviewText);
            holder.tvReviewText.setVisibility(View.VISIBLE);
        } else {
            holder.tvReviewText.setText("No review comment provided.");
            // holder.tvReviewText.setVisibility(View.GONE); // Or show placeholder
        }

        if (rating != null && rating > 0) {
            holder.rbReviewRating.setRating(rating.floatValue());
            holder.rbReviewRating.setVisibility(View.VISIBLE);
        } else {
            holder.rbReviewRating.setVisibility(View.GONE);
            // If no rating, but there's text, we might still show the text.
            // If both are null/empty, the whole item might be less useful.
            if (reviewText == null || reviewText.isEmpty()){
                holder.tvReviewText.setText("No feedback provided for this session.");
            }
        }

        // Fetch and display reviewer's details
        if (reviewerUid != null && !reviewerUid.isEmpty()) {
            holder.tvReviewerName.setText("Loading reviewer..."); // Placeholder
            holder.ivReviewerImage.setImageResource(R.mipmap.ic_launcher_round); // Default
            userDao.getUserByUid(reviewerUid).addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User reviewer = documentSnapshot.toObject(User.class);
                    if (reviewer != null) {
                        holder.tvReviewerName.setText(String.format(Locale.getDefault(),"Reviewed by: %s %s", reviewer.getFirstName(), reviewer.getLastName()));
                        if (context != null) {
                            Glide.with(context)
                                    .load(reviewer.getProfileImageUrl())
                                    .placeholder(R.mipmap.ic_launcher_round)
                                    .error(R.mipmap.ic_launcher_round)
                                    .circleCrop()
                                    .into(holder.ivReviewerImage);
                        }
                    } else {
                        holder.tvReviewerName.setText("Reviewer: Anonymous");
                    }
                } else {
                    holder.tvReviewerName.setText("Reviewer: User not found");
                }
            }).addOnFailureListener(e -> {
                Log.e(ADAPTER_TAG, "Error fetching reviewer " + reviewerUid, e);
                holder.tvReviewerName.setText("Reviewer: Error");
            });
        } else {
            holder.tvReviewerName.setText("Reviewer: Anonymous");
            if (context != null) holder.ivReviewerImage.setImageResource(R.mipmap.ic_launcher_round);
        }

        // Set Subject Name
        if (session.getSubjectName() != null && !session.getSubjectName().isEmpty()) {
            holder.tvReviewedSessionSubject.setText("Session for: " + session.getSubjectName().split(":")[0]);
        } else if (session.getSubjectId() != null) {
            holder.tvReviewedSessionSubject.setText("Loading Subject...");
            subjectDao.getSubjectById(session.getSubjectId()).addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Subject subject = doc.toObject(Subject.class);
                    if (subject != null && subject.getSubjectName() != null) {
                        holder.tvReviewedSessionSubject.setText("Session for: " + subject.getSubjectName().split(":")[0]);
                    } else {
                        holder.tvReviewedSessionSubject.setText("Session for: Unknown Subject");
                    }
                } else {
                    holder.tvReviewedSessionSubject.setText("Session for: Unknown Subject");
                }
            }).addOnFailureListener(e -> {
                Log.e(ADAPTER_TAG, "Error fetching subject " + session.getSubjectId(), e);
                holder.tvReviewedSessionSubject.setText("Session for: Error Subject");
            });
        } else {
            holder.tvReviewedSessionSubject.setText("Session for: Unknown Subject");
        }


        // Set Review Date (from session's start/end time, or a dedicated review timestamp if available)
        Timestamp sessionTime = session.getStartTime(); // Or endTime, or a specific review timestamp if you add one
        if (sessionTime != null) {
            holder.tvReviewDate.setText("On: " + dateFormat.format(sessionTime.toDate()));
        } else {
            holder.tvReviewDate.setText("Date: N/A");
        }
    }

    @Override
    public int getItemCount() {
        return reviewedSessions != null ? reviewedSessions.size() : 0;
    }

    public void updateList(List<Session> newList) {
        this.reviewedSessions.clear();
        if (newList != null) {
            this.reviewedSessions.addAll(newList);
        }
        notifyDataSetChanged();
    }


    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView ivReviewerImage;
        TextView tvReviewerName, tvReviewText, tvReviewedSessionSubject, tvReviewDate;
        RatingBar rbReviewRating;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivReviewerImage = itemView.findViewById(R.id.ivReviewerImage);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            rbReviewRating = itemView.findViewById(R.id.rbReviewRating);
            tvReviewText = itemView.findViewById(R.id.tvReviewText);
            tvReviewedSessionSubject = itemView.findViewById(R.id.tvReviewedSessionSubject);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
        }
    }
}
