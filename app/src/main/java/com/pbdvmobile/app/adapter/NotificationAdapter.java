package com.pbdvmobile.app.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.DataManager; // Already present
import com.pbdvmobile.app.data.dao.NotificationDao; // Import NotificationDao
import com.pbdvmobile.app.data.model.Notification;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;
    private Context context;
    private SimpleDateFormat dateFormat;
    private DataManager dataManager; // Keep DataManager
    private NotificationDao notificationDao; // Add NotificationDao instance

    // Listener interface
    public interface OnNotificationDataChangedListener {
        void onDataChanged();
    }
    private OnNotificationDataChangedListener dataChangedListener;

    // Modify constructor to accept DataManager and the listener
    public NotificationAdapter(Context context, List<Notification> notificationList, DataManager dataManager, OnNotificationDataChangedListener listener) {
        this.context = context;
        this.notificationList = notificationList;
        this.dataManager = dataManager; // Store DataManager
        this.notificationDao = dataManager.getNotificationDao(); // Get DAO from DataManager
        this.dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
        this.dataChangedListener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_notification, parent, false);
        return new NotificationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.textViewNotificationText.setText(notification.getText()); // Use getText()

        if (notification.getDate() != null) {
            holder.textViewNotificationDate.setText(dateFormat.format(notification.getDate()));
        } else {
            holder.textViewNotificationDate.setText("");
        }

        if (notification.getStatus() == Notification.Status.SEALED) {
            holder.imageViewUnreadDot.setVisibility(View.VISIBLE);
            holder.textViewNotificationText.setTypeface(null, Typeface.BOLD);
        } else {
            holder.imageViewUnreadDot.setVisibility(View.GONE);
            holder.textViewNotificationText.setTypeface(null, Typeface.NORMAL);
        }

        if (notification.isRemember()) {
            holder.imageViewRememberPin.setVisibility(View.VISIBLE);
        } else {
            holder.imageViewRememberPin.setVisibility(View.GONE);
        }

        // Use a consistent default icon, or logic to change it based on notification type if you add it
        holder.imageViewNotificationIcon.setImageResource(android.R.drawable.ic_popup_reminder); // Use your ic_notification_default

        holder.itemView.setOnClickListener(v -> {
            if (notification.getStatus() == Notification.Status.SEALED) {
                // Directly call toggle or just update the specific notification
                // For simplicity, let's assume toggleNotificationStatus updates DB and UI
                toggleNotificationStatus(holder.getAdapterPosition()); // Use adapter position
            }
            Toast.makeText(context, "Clicked: " + notification.getText(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    // This method is fine if NotificationsActivity manages the list directly
    // and calls notifyDataSetChanged on the adapter.
    // However, if adapter manages its own list internally:
    public void setNotifications(List<Notification> newNotifications) {
        this.notificationList = newNotifications; // Directly replace the list
        notifyDataSetChanged();
        if (dataChangedListener != null) {
            dataChangedListener.onDataChanged();
        }
    }

    public void removeNotification(int position) {
        if (position >= 0 && position < notificationList.size()) {
            Notification notificationToRemove = notificationList.get(position);
            // Perform DB operation FIRST
            int rowsDeleted = notificationDao.deleteNotification(notificationToRemove.getNoteId());
            if (rowsDeleted > 0) {
                notificationList.remove(position); // Update internal list
                notifyItemRemoved(position);
                // Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show(); // Moved to Snackbar
            } else {
                Toast.makeText(context, "Failed to delete from DB", Toast.LENGTH_SHORT).show();
                notifyItemChanged(position); // Revert UI change if DB op failed
            }
            if (dataChangedListener != null) {
                dataChangedListener.onDataChanged();
            }
        }
    }

    // Method to be called by swipe action for undo, actually deletes from DB
    public void confirmDeleteNotificationFromDb(int noteId) {
        int rowsDeleted = notificationDao.deleteNotification(noteId);
        if (rowsDeleted > 0) {
            Toast.makeText(context, "Notification permanently deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Error confirming deletion", Toast.LENGTH_SHORT).show();
            // Potentially need to re-fetch or re-add if visual removal happened but DB failed.
            // This scenario is tricky with undo; better to ensure DB ops are robust.
        }
        // The visual removal is handled by the Snackbar logic in the Activity
    }


    public void toggleNotificationStatus(int position) {
        if (position >= 0 && position < notificationList.size()) {
            Notification notification = notificationList.get(position);
            Notification.Status newStatus = (notification.getStatus() == Notification.Status.SEALED) ?
                    Notification.Status.OPENED : Notification.Status.SEALED;

            // Perform DB operation FIRST
            int rowsUpdated = notificationDao.updateNotificationStatus(notification.getNoteId(), newStatus);

            if (rowsUpdated > 0) {
                notification.setStatus(newStatus); // Update object in the list
                notifyItemChanged(position); // Refresh this item's view
                String statusMessage = (newStatus == Notification.Status.OPENED) ? "Marked as read" : "Marked as unread";
                Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to update status in DB", Toast.LENGTH_SHORT).show();
                notifyItemChanged(position); // Revert UI if DB op failed
            }
            // No need to call dataChangedListener, item count is the same
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewNotificationIcon;
        ImageView imageViewUnreadDot;
        TextView textViewNotificationText;
        TextView textViewNotificationDate;
        ImageView imageViewRememberPin;

        NotificationViewHolder(View view) {
            super(view);
            imageViewNotificationIcon = view.findViewById(R.id.imageViewNotificationIcon);
            imageViewUnreadDot = view.findViewById(R.id.imageViewUnreadDot);
            textViewNotificationText = view.findViewById(R.id.textViewNotificationText);
            textViewNotificationDate = view.findViewById(R.id.textViewNotificationDate);
            imageViewRememberPin = view.findViewById(R.id.imageViewRememberPin);
        }
    }
}