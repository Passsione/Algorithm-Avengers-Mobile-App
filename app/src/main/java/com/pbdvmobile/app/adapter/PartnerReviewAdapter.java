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
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PartnerReviewAdapter extends RecyclerView.Adapter<PartnerReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Session> reviewedSessions;
    private User partner; // The user whose profile is being viewed
    private DataManager dataManager;
    private SimpleDateFormat dateFormat;

    public PartnerReviewAdapter(Context context, List<Session> reviewedSessions, User partner, DataManager dataManager) {
        this.context = context;
        this.reviewedSessions = reviewedSessions;
        this.partner = partner;
        this.dataManager = dataManager;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
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
        if (session == null || partner == null) return;

        String reviewText = null;
        Double rating = null;
        User reviewer = null;
        int reviewerId = -1;

        // Determine if the partner was the tutor or tutee in this session,
        // and thus which review/rating to display.
        if (session.getTutorId() == partner.getStudentNum()) {
            // Partner was the tutor, so we show the tutee's review of the partner.
            reviewText = session.getTuteeReview();
            rating = session.getTuteeRating();
            reviewerId = session.getTuteeId();
        } else if (session.getTuteeId() == partner.getStudentNum()) {
            // Partner was the tutee, so we show the tutor's review of the partner.
            reviewText = session.getTutorReview();
            rating = session.getTutorRating();
            reviewerId = session.getTutorId();
        }

        if (reviewerId != -1) {
            reviewer = dataManager.getUserDao().getUserByStudentNum(reviewerId);
        }

        if (reviewText != null && !reviewText.isEmpty()) {
            holder.tvReviewText.setText(reviewText);
            holder.tvReviewText.setVisibility(View.VISIBLE);
        } else {
            holder.tvReviewText.setText("No review comment provided.");
            // holder.tvReviewText.setVisibility(View.GONE); // Or show a placeholder
        }

        if (rating != null) {
            holder.rbReviewRating.setRating(rating.floatValue());
            holder.rbReviewRating.setVisibility(View.VISIBLE);
        } else {
            holder.rbReviewRating.setVisibility(View.GONE);
        }

        if (reviewer != null) {
            holder.tvReviewerName.setText(String.format("Reviewed by: %s %s", reviewer.getFirstName(), reviewer.getLastName()));
            Glide.with(context)
                    .load(reviewer.getProfileImageUrl())
                    .placeholder(R.mipmap.ic_launcher_round) // Default placeholder
                    .error(R.mipmap.ic_launcher_round)       // Error placeholder
                    .circleCrop()
                    .into(holder.ivReviewerImage);
        } else {
            holder.tvReviewerName.setText("Reviewer: Anonymous");
            holder.ivReviewerImage.setImageResource(R.mipmap.ic_launcher_round); // Default image
        }

        Subject subject = dataManager.getSubjectDao().getSubjectById(session.getSubjectId());
        if (subject != null) {
            holder.tvReviewedSessionSubject.setText("Session for: " + subject.getSubjectName());
        } else {
            holder.tvReviewedSessionSubject.setText("Session for: Unknown Subject");
        }

        holder.tvReviewDate.setText("On: " + dateFormat.format(session.getStartTime()));
    }

    @Override
    public int getItemCount() {
        return reviewedSessions.size();
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
