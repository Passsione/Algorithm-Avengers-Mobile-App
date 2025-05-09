package com.pbdvmobile.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.model.Subject;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PastSessionAdapter extends RecyclerView.Adapter<PastSessionAdapter.PastSessionViewHolder> {

    private List<Session> sessionList;
    private final Context context;
    private final DataManager dataManager;
    private final int currentUserId;
    private final OnSessionClickListener listener;
    private final SimpleDateFormat dateTimeFormatter;

    public interface OnSessionClickListener {
        void onSessionClick(Session session);
    }

    public PastSessionAdapter(Context context, List<Session> sessionList, DataManager dataManager, int currentUserId, OnSessionClickListener listener) {
        this.context = context;
        this.sessionList = sessionList;
        this.dataManager = dataManager;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.dateTimeFormatter = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public PastSessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_past_session, parent, false);
        return new PastSessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PastSessionViewHolder holder, int position) {
        Session session = sessionList.get(position);
        holder.bind(session);
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public void filterList(List<Session> filteredList) {
        this.sessionList = filteredList;
        notifyDataSetChanged();
    }

    class PastSessionViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPartner;
        TextView textViewPastSessionSubject, textViewPastSessionPartnerName,
                textViewPastSessionDate, textViewPastSessionStatus, textViewPastSessionLocation;

        PastSessionViewHolder(View itemView) {
            super(itemView);
            imageViewPartner = itemView.findViewById(R.id.imageViewPartner);
            textViewPastSessionSubject = itemView.findViewById(R.id.textViewPastSessionSubject);
            textViewPastSessionPartnerName = itemView.findViewById(R.id.textViewPastSessionPartnerName);
            textViewPastSessionDate = itemView.findViewById(R.id.textViewPastSessionDate);
            textViewPastSessionStatus = itemView.findViewById(R.id.textViewPastSessionStatus);
            textViewPastSessionLocation = itemView.findViewById(R.id.textViewPastSessionLocation);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSessionClick(sessionList.get(position));
                }
            });
        }

        void bind(Session session) {
            Subject subject = dataManager.getSubjectDao().getSubjectById(session.getSubjectId());
            textViewPastSessionSubject.setText(subject != null ? subject.getSubjectName() : "Unknown Subject");

            int partnerId = (session.getTutorId() == currentUserId) ? session.getTuteeId() : session.getTutorId();
            User partner = dataManager.getUserDao().getUserByStudentNum(partnerId);
            if (partner != null) {
                textViewPastSessionPartnerName.setText("With: " + partner.getFirstName() + " " + partner.getLastName());
                Glide.with(context)
                        .load(partner.getProfileImageUrl())
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .circleCrop()
                        .into(imageViewPartner);
            } else {
                textViewPastSessionPartnerName.setText("With: Unknown User");
                imageViewPartner.setImageResource(R.mipmap.ic_launcher_round);
            }

            textViewPastSessionDate.setText(dateTimeFormatter.format(session.getStartTime()));
            textViewPastSessionStatus.setText(session.getStatus().name());
            textViewPastSessionLocation.setText("Loc: " + (session.getLocation() != null ? session.getLocation() : "N/A"));

            // Set status background color
            int statusColorRes;
            switch (session.getStatus()) {
                case COMPLETED:
                    statusColorRes = R.color.status_completed; // Define these in colors.xml
                    break;
                case CANCELLED:
                    statusColorRes = R.color.status_cancelled;
                    break;
                case DECLINED:
                    statusColorRes = R.color.status_declined;
                    break;
                default: // PENDING, CONFIRMED (though history might only show final states)
                    statusColorRes = R.color.status_generic_gray;
                    break;
            }
            // Create a drawable with this color or use a pre-defined background
            // For simplicity, just setting text color based on status for now
            this.textViewPastSessionStatus.setBackgroundResource(getStatusBackground(session.getStatus())); // Using a helper
            this.textViewPastSessionStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white));


        }
        private int getStatusBackground(Session.Status status) {
            switch (status) {
                case COMPLETED: return R.drawable.avatar_2; // Greenish
                case CANCELLED: return R.drawable.avatar_1; // Reddish
                case DECLINED: return R.drawable.avatar_3;   // Orange/Yellowish
                default: return R.drawable.avatar_16; // Grayish
            }
        }
    }
}