package com.pbdvmobile.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.TutorDetailActivity;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import java.util.List;

public class TutorAdapter extends RecyclerView.Adapter<TutorAdapter.TutorViewHolder> {

    private List<User> tutorList;
    private Context context;

    public TutorAdapter(List<User> tutorList, Context context) {
        this.tutorList = tutorList;
        this.context = context;
    }

    @NonNull
    @Override
    public TutorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tutor, parent, false);
        return new TutorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorViewHolder holder, int position) {
        User tutor = tutorList.get(position);

        holder.tvName.setText(tutor.getFullName());
        holder.tvEducation.setText(tutor.getEducationLevel());
        holder.ratingBar.setRating((float) tutor.getAverageRating());
        holder.tvPrice.setText("$" + tutor.getHourlyRate() + "/hr");

        // Build subjects text
        StringBuilder subjectsText = new StringBuilder();
        for (Subject subject : tutor.getSubjects()) {
            if (subjectsText.length() > 0) {
                subjectsText.append(", ");
            }
            subjectsText.append(subject.getName());
        }
        holder.tvSubjects.setText(subjectsText.toString());

        // Load profile image
        if (tutor.getProfileImageUrl() != null && !tutor.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(tutor.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.default_profile)
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.default_profile);
        }

        // Set click listener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, TutorDetailActivity.class);
                intent.putExtra("tutor_id", tutor.getId());
                context.startActivity(intent);
            }
        });

        holder.btnBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, BookSessionActivity.class);
                intent.putExtra("tutor_id", tutor.getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tutorList.size();
    }

    public static class TutorViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName, tvEducation, tvSubjects, tvPrice;
        RatingBar ratingBar;
        Button btnBook;

        public TutorViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_profile);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEducation = itemView.findViewById(R.id.tv_education);
            tvSubjects = itemView.findViewById(R.id.tv_subjects);
            tvPrice = itemView.findViewById(R.id.tv_price);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            btnBook = itemView.findViewById(R.id.btn_book);
        }
    }
}
