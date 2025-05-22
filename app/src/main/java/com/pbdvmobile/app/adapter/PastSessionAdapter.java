package com.pbdvmobile.app.adapter;

import android.content.Context;
import android.util.TypedValue;
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
import com.pbdvmobile.app.data.dao.SubjectDao; // Firebase DAO
import com.pbdvmobile.app.data.dao.UserDao;   // Firebase DAO
import com.pbdvmobile.app.data.model.Session; // Firebase model
import com.pbdvmobile.app.data.model.User;    // Firebase model
import com.pbdvmobile.app.data.model.Subject; // Firebase model
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import android.util.Log;

public class PastSessionAdapter extends RecyclerView.Adapter<PastSessionAdapter.PastSessionViewHolder> {

    private static final String ADAPTER_TAG = "PastSessionAdapter";
    private List<Session> sessionList;
    private final Context context;
    private final String currentLoggedInUserUid; // Changed from int currentUserId
    private final OnSessionClickListener listener;
    private final SimpleDateFormat dateTimeFormatter;

    // DAOs for fetching related data
    private final UserDao userDao;
    private final SubjectDao subjectDao;

    public interface OnSessionClickListener {
        void onSessionClick(Session session);
    }

    public PastSessionAdapter(Context context, List<Session> sessionList, String currentLoggedInUserUid, OnSessionClickListener listener) {
        this.context = context;
        this.sessionList = sessionList;
        this.currentLoggedInUserUid = currentLoggedInUserUid;
        this.listener = listener;
        this.dateTimeFormatter = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
        this.userDao = new UserDao(); // Initialize Firebase DAOs
        this.subjectDao = new SubjectDao();
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
        return sessionList != null ? sessionList.size() : 0;
    }

    public void updateList(List<Session> newList) { // Renamed from filterList for clarity
        this.sessionList.clear();
        if (newList != null) {
            this.sessionList.addAll(newList);
        }
        notifyDataSetChanged(); // Consider using DiffUtil for better performance
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
            // Set Subject Name
            if (session.getSubjectName() != null && !session.getSubjectName().isEmpty()) {
                textViewPastSessionSubject.setText(session.getSubjectName().split(":")[0]); // Use denormalized name
            } else if (session.getSubjectId() != null) {
                textViewPastSessionSubject.setText("Loading Subject..."); // Placeholder
                subjectDao.getSubjectById(session.getSubjectId()).addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Subject subject = doc.toObject(Subject.class);
                        if (subject != null && subject.getSubjectName() != null) {
                            textViewPastSessionSubject.setText(subject.getSubjectName().split(":")[0]);
                        } else {
                            textViewPastSessionSubject.setText("Unknown Subject");
                        }
                    } else {
                        textViewPastSessionSubject.setText("Unknown Subject");
                    }
                }).addOnFailureListener(e -> {
                    Log.e(ADAPTER_TAG, "Error fetching subject " + session.getSubjectId(), e);
                    textViewPastSessionSubject.setText("Error Subject");
                });
            } else {
                textViewPastSessionSubject.setText("Unknown Subject");
            }

            // Determine Partner UID and fetch Partner Details
            String partnerUid = null;
            if (session.getTutorUid() != null && session.getTutorUid().equals(currentLoggedInUserUid)) {
                partnerUid = session.getTuteeUid(); // Current user was tutor, partner is tutee
            } else if (session.getTuteeUid() != null && session.getTuteeUid().equals(currentLoggedInUserUid)) {
                partnerUid = session.getTutorUid(); // Current user was tutee, partner is tutor
            } else {
                // This case means the current user is neither tutor nor tutee for this session,
                // which might be an issue depending on how sessionList is populated.
                // Or, if it's an admin view, you might show both. For now, assume one is current user.
                Log.w(ADAPTER_TAG, "Current user UID " + currentLoggedInUserUid + " not found as tutor or tutee in session " + session.getId());
                // Try to pick one if possible, or default
                if(session.getTutorUid() != null) partnerUid = session.getTutorUid();
                else partnerUid = session.getTuteeUid();
            }


            if (partnerUid != null && !partnerUid.isEmpty()) {
                textViewPastSessionPartnerName.setText("Loading Partner..."); // Placeholder
                imageViewPartner.setImageResource(R.mipmap.ic_launcher_round); // Default placeholder

                String finalPartnerUid = partnerUid;
                userDao.getUserByUid(partnerUid).addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User partner = documentSnapshot.toObject(User.class);
                        if (partner != null) {
                            textViewPastSessionPartnerName.setText("With: " + partner.getFirstName() + " " + partner.getLastName());
                            if (context != null) { // Check context validity for Glide
                                Glide.with(context)
                                        .load(partner.getProfileImageUrl())
                                        .placeholder(R.mipmap.ic_launcher_round)
                                        .error(R.mipmap.ic_launcher_round)
                                        .circleCrop()
                                        .into(imageViewPartner);
                            }
                        } else {
                            textViewPastSessionPartnerName.setText("With: Unknown User");
                        }
                    } else {
                        textViewPastSessionPartnerName.setText("With: User Not Found");
                    }
                }).addOnFailureListener(e -> {
                    Log.e(ADAPTER_TAG, "Error fetching partner " + finalPartnerUid, e);
                    textViewPastSessionPartnerName.setText("With: Error User");
                });
            } else {
                textViewPastSessionPartnerName.setText("With: Unknown User");
                if (context != null) imageViewPartner.setImageResource(R.mipmap.ic_launcher_round);
            }

            // Set Date and Time
            Timestamp startTimeStamp = session.getStartTime();
            if (startTimeStamp != null) {
                textViewPastSessionDate.setText(dateTimeFormatter.format(startTimeStamp.toDate()));
            } else {
                textViewPastSessionDate.setText("Date/Time N/A");
            }

            // Set Status and Location
            if (session.getStatus() != null) {
                textViewPastSessionStatus.setText(session.getStatus().name());
                setStatusStyling(textViewPastSessionStatus, session.getStatus());
            } else {
                textViewPastSessionStatus.setText("Status N/A");
            }
            textViewPastSessionLocation.setText("Loc: " + (session.getLocation() != null ? session.getLocation() : "N/A"));
        }

        private void setStatusStyling(TextView statusTextView, Session.Status status) {
            if (context == null) return;
            int colorResId;
            int backgroundResId;

            switch (status) {
                case COMPLETED:
                    colorResId = R.color.white; // Example: white text
                    backgroundResId = R.drawable.status_background_completed;
                    break;
                case CANCELLED:
                    colorResId = R.color.white;
                    backgroundResId = R.drawable.status_background_cancelled;
                    break;
                case DECLINED:
                    colorResId = R.color.black; // Example: black text
                    backgroundResId = R.drawable.status_background_declined;
                    break;
                default:
                    colorResId = R.color.black;
                    backgroundResId = R.drawable.status_background_generic;
                    break;
            }
            statusTextView.setTextColor(ContextCompat.getColor(context, colorResId));
            statusTextView.setBackgroundResource(backgroundResId);
            // Add padding if your backgrounds don't have it
            int horizPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
            int vertPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());
            statusTextView.setPadding(horizPadding, vertPadding, horizPadding, vertPadding);

        }
    }
}
