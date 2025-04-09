package com.pbdvmobile.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.LeaveReviewActivity;
import com.pbdvmobile.app.SessionDetailActivity;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.services.SessionService;
import com.pbdvmobile.app.services.UserService;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private List<Session> sessionList;
    private Context context;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private UserService userService;
    private SessionService sessionService;

    public SessionAdapter(List<Session> sessionList, Context context) {
        this.sessionList = sessionList;
        this.context = context;
        this.dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
        this.timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        this.userService = new UserService();
        this.sessionService = new SessionService();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        Session session = sessionList.get(position);
        User currentUser = userService.getCurrentUser();

        // Format date and time
        String date = dateFormat.format(session.getStartTime());
        String startTime = timeFormat.format(session.getStartTime());
        String endTime = timeFormat.format(session.getEndTime());

        holder.tvSubject.setText(session.getSubject());
        holder.tvDateTime.setText(date + " | " + startTime + " - " + endTime);
        holder.tvLocation.setText("Location: " + session.getLocation());
        holder.tvStatus.setText("Status: " + formatStatus(session.getStatus()));

        // Load other party details based on user type
        String otherPartyId = currentUser.getUserType() == User.UserType.TUTOR ?
                session.getTuteeId() : session.getTutorId();

        userService.getUserById(otherPartyId, new UserService.UserDetailCallback() {
            @Override
            public void onSuccess(User user) {
                if (currentUser.getUserType() == User.UserType.TUTOR) {
                    holder.tvWithPerson.setText("With: " + user.getFullName() + " (Tutee)");
                } else {
                    holder.tvWithPerson.setText("With: " + user.getFullName() + " (Tutor)");
                }
            }

            @Override
            public void onFailure(String error) {
                holder.tvWithPerson.setText("With: Unknown User");
            }
        });

        // Configure action buttons based on session status and timing
        configureActionButtons(holder, session, currentUser);

        // Set click listener for the item
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SessionDetailActivity.class);
                intent.putExtra("session_id", session.getId());
                context.startActivity(intent);
            }
        });
    }

    private void configureActionButtons(SessionViewHolder holder, Session session, User currentUser) {
        Date now = new Date();
        boolean isPastSession = session.getEndTime().before(now);
        boolean hasLeftReview = session.getTuteeReviewId() != null || session.getTutorReviewId() != null;

        // Reset visibility
        holder.btnCancel.setVisibility(View.GONE);
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnDecline.setVisibility(View.GONE);
        holder.btnReview.setVisibility(View.GONE);

        if (isPastSession) {
            // Past session - show review button if not yet reviewed
            if (!hasLeftReview) {
                holder.btnReview.setVisibility(View.VISIBLE);
                holder.btnReview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, LeaveReviewActivity.class);
                        intent.putExtra("session_id", session.getId());
                        context.startActivity(intent);
                    }
                });
            }
        } else {
            // Upcoming session
            if (session.getStatus() == Session.Status.PENDING) {
                // For tutee, show cancel button
                if (currentUser.getId().equals(session.getTuteeId())) {
                    holder.btnCancel.setVisibility(View.VISIBLE);
                    holder.btnCancel.setOnClickListener(v -> cancelSession(session));
                }
                // For tutor, show accept/decline buttons
                else if (currentUser.getId().equals(session.getTutorId())) {
                    holder.btnAccept.setVisibility(View.VISIBLE);
                    holder.btnDecline.setVisibility(View.VISIBLE);

                    holder.btnAccept.setOnClickListener(v -> updateSessionStatus(session, Session.Status.CONFIRMED));
                    holder.btnDecline.setOnClickListener(v -> updateSessionStatus(session, Session.Status.DECLINED));
                }
            } else if (session.getStatus() == Session.Status.CONFIRMED) {
                // Both parties can cancel confirmed sessions
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnCancel.setOnClickListener(v -> cancelSession(session));
            }
        }
    }

    private void cancelSession(Session session) {
        sessionService.updateSessionStatus(session.getId(), Session.Status.CANCELLED, new SessionService.SessionCallback() {
            @Override
            public void onSuccess() {
                session.setStatus(Session.Status.CANCELLED);
                notifyDataSetChanged();
            }

            @Override
            public void onFailure(String error) {
                // Handle error
            }
        });
    }

    private void updateSessionStatus(Session session, Session.Status status) {
        sessionService.updateSessionStatus(session.getId(), status, new SessionService.SessionCallback() {
            @Override
            public void onSuccess() {
                session.setStatus(status);
                notifyDataSetChanged();
            }

            @Override
            public void onFailure(String error) {
                // Handle error
            }
        });
    }

    private String formatStatus(Session.Status status) {
        switch (status) {
            case PENDING:
                return "Pending";
            case CONFIRMED:
                return "Confirmed";
            case CANCELLED:
                return "Cancelled";
            case DECLINED:
                return "Declined";
            case COMPLETED:
                return "Completed";
            default:
                return "Unknown";
        }
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvDateTime, tvLocation, tvWithPerson, tvStatus;
        Button btnCancel, btnAccept, btnDecline, btnReview;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tv_subject);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvWithPerson = itemView.findViewById(R.id.tv_with_person);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
            btnReview = itemView.findViewById(R.id.btn_review);
        }
    }
}
