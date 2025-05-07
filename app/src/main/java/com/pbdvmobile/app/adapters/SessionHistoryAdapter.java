package com.pbdvmobile.app.adapters; // Or your appropriate package

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.fragments.SessionDetailsFragment; // Assuming this activity exists
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder> {

    private List<Session> sessions;
    private Context context;
    private DataManager dataManager;
    private User currentUser;
    private boolean isCurrentUserTutor;

    public interface OnRatingButtonClickListener {
        void onRatingButtonClick(int sessionId);
    }
    private OnRatingButtonClickListener ratingButtonClickListener;


    public SessionHistoryAdapter(Context context, List<Session> sessions, OnRatingButtonClickListener listener) {
        this.context = context;
        this.sessions = sessions;
        this.dataManager = DataManager.getInstance(context);
        this.currentUser = LogInUser.getInstance(dataManager).getUser();
        this.isCurrentUserTutor = currentUser.isTutor();
        this.ratingButtonClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Session session = sessions.get(position);

        User partnerUser = new User();
        List<User> partnersUser = new ArrayList<>();
        String partnerLabel;

        if (isCurrentUserTutor) {
            for (int tuteeId : session.getTuteeIds()) {
                partnersUser.add(dataManager.getUserDao().getUserByStudentNum(tuteeId));
            }
            partnerLabel = "Tutee(s): ";
        } else {
            partnerUser = dataManager.getUserDao().getUserByStudentNum(session.getTutorId());
            partnerLabel = "Tutor: ";
        }

        holder.tvPartnerLabel.setText(partnerLabel);
        if(isCurrentUserTutor)
            if (partnerUser.getEmail() != null) {
                holder.tvPartnerName.setText(partnerUser.getFirstName() + " " + partnerUser.getLastName());
            } else {
                holder.tvPartnerName.setText("N/A");
            }
        else {
            for(User partner : partnersUser)
                holder.tvPartnerName.setText(holder.tvPartnerName.getText().toString()+ ", " + partner.getFirstName() + " " + partner.getLastName());
        }
        com.pbdvmobile.app.data.model.Subject subject = dataManager.getSubjectDao().getSubjectById(session.getSubjectId());
        if (subject != null) {
            holder.tvSubjectName.setText("Subject: " + subject.getSubjectName());
        } else {
            holder.tvSubjectName.setText("Subject: N/A");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String dateTimeString = "Date: " + dateFormat.format(session.getStartTime()) +
                ", " + timeFormat.format(session.getStartTime()) +
                " - " + timeFormat.format(session.getEndTime());
        holder.tvSessionDateTime.setText(dateTimeString);

        holder.tvSessionLocation.setText("Location: " + session.getLocation());
        holder.tvSessionStatus.setText(session.getStatus().name());
        setStatusBackground(holder.tvSessionStatus, session.getStatus());


        if (session.getStatus() == Session.Status.COMPLETED && !isCurrentUserTutor) { // Only tutees can rate
            holder.btnLeaveRating.setVisibility(View.VISIBLE);
            holder.btnLeaveRating.setOnClickListener(v -> {
                if(ratingButtonClickListener != null){
                    ratingButtonClickListener.onRatingButtonClick(session.getId());
                }
            });
        } else {
            holder.btnLeaveRating.setVisibility(View.GONE);
        }
    }

    private void setStatusBackground(TextView textView, Session.Status status) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(10f); // Adjust corner radius as needed

        int colorRes;
        int textColor = Color.WHITE;

        switch (status) {
            case COMPLETED:
                colorRes = R.color.status_completed; // Define in colors.xml e.g. <color name="status_completed">#4CAF50</color>
                break;
            case CANCELLED:
                colorRes = R.color.status_cancelled; // e.g. <color name="status_cancelled">#F44336</color>
                break;
            case DECLINED:
                colorRes = R.color.status_denied; // e.g. <color name="status_DECLINED">#FF9800</color>
                break;
            default:
                colorRes = R.color.status_other; // e.g. <color name="status_other">#9E9E9E</color>
                break;
        }
        try {
            background.setColor(ContextCompat.getColor(context, colorRes));
            textView.setTextColor(textColor);
        } catch (Exception e){
            // Fallback if color resource not found
            background.setColor(Color.GRAY);
        }
        textView.setBackground(background);
    }


    @Override
    public int getItemCount() {
        return sessions.size();
    }

    public void updateSessions(List<Session> newSessions) {
        this.sessions.clear();
        this.sessions.addAll(newSessions);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPartnerLabel, tvPartnerName, tvSubjectName, tvSessionDateTime, tvSessionStatus, tvSessionLocation;
        Button btnLeaveRating;

        ViewHolder(View itemView) {
            super(itemView);
            tvPartnerLabel = itemView.findViewById(R.id.tvPartnerLabel);
            tvPartnerName = itemView.findViewById(R.id.tvPartnerName);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvSessionDateTime = itemView.findViewById(R.id.tvSessionDateTime);
            tvSessionStatus = itemView.findViewById(R.id.tvSessionStatus);
            tvSessionLocation = itemView.findViewById(R.id.tvSessionLocation);
            btnLeaveRating = itemView.findViewById(R.id.btnLeaveRating);
        }
    }
}