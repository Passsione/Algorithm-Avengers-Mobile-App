package com.pbdvmobile.app;

import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar; // For Undo
import com.pbdvmobile.app.adapter.NotificationAdapter;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.LogInUser;
import com.pbdvmobile.app.data.model.Notification; // Your model

import java.util.ArrayList;
import java.util.Collections; // For sorting
import java.util.List;
// Import RecyclerViewSwipeDecorator if you use it
// import com.github.xabaras.RecyclerViewSwipeDecorator;


// Implement the listener
public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationDataChangedListener {

    private RecyclerView recyclerViewNotifications;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationDataList; // This list will be managed by the activity
    private TextView textViewNoNotifications, textViewNotificationTitle;

    DataManager dataManager;
    LogInUser current_user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dataManager = DataManager.getInstance(this.getApplicationContext()); // Use application context for DataManager if it's a singleton
        current_user = LogInUser.getInstance();

        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        textViewNoNotifications = findViewById(R.id.textViewNoNotifications);
        textViewNotificationTitle = findViewById(R.id.delete_notification);

        notificationDataList = new ArrayList<>(); // Initialize the list
        // Pass DataManager and 'this' (as listener) to the adapter
        notificationAdapter = new NotificationAdapter(this, notificationDataList, dataManager, this);

        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(notificationAdapter);

        // Setup ItemTouchHelper for swipe gestures
        setupSwipeActions();

        loadAndUpdateNotifications(); // Renamed for clarity
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh notifications when the activity resumes, in case status changed elsewhere
        loadAndUpdateNotifications();
    }

    private void loadAndUpdateNotifications() {
        // Fetch fresh data from DB
        List<Notification> freshNotifications = new ArrayList<>();
        if (current_user != null && current_user.getUser() != null) {
            freshNotifications.addAll(dataManager.getNotificationDao().getNotificationsByStudentNum(current_user.getUser().getStudentNum()));
        }
        // Optionally, add system-wide notifications
        freshNotifications.addAll(dataManager.getNotificationDao().getSystemNotifications());
        // Add a more robust way to combine and avoid duplicates if necessary

        // Sort notifications by date, newest first
        // Correct date comparison:
        Collections.sort(freshNotifications, (n1, n2) -> {
            if (n1.getDate() == null && n2.getDate() == null) return 0;
            if (n1.getDate() == null) return 1; // Null dates last
            if (n2.getDate() == null) return -1;
            return n2.getDate().compareTo(n1.getDate());
        });

        // Update the adapter's list
        notificationDataList.clear();
        notificationDataList.addAll(freshNotifications);
        notificationAdapter.notifyDataSetChanged(); // Tell the adapter the data has changed

        updateNoNotificationsView(); // Update visibility of "no notifications" text
    }

    @Override
    public void onDataChanged() {
        // This callback is from the adapter after it modifies its internal list
        // (e.g., after a successful delete and item removal)
        updateNoNotificationsView();
    }

    private void updateNoNotificationsView() {
        if (notificationAdapter.getItemCount() == 0) {
            recyclerViewNotifications.setVisibility(View.GONE);
            textViewNoNotifications.setVisibility(View.VISIBLE);
            textViewNotificationTitle.setVisibility(View.GONE);
        } else {
            recyclerViewNotifications.setVisibility(View.VISIBLE);
            textViewNoNotifications.setVisibility(View.GONE);
            textViewNotificationTitle.setVisibility(View.VISIBLE);

        }
    }

    private void setupSwipeActions() {
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < notificationDataList.size()) { // Boundary check
                    if (direction == ItemTouchHelper.LEFT) {
                        // Swipe Left: Delete with Undo
                        showUndoDeleteSnackbar(position, notificationDataList.get(position));
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        // Swipe Right: Toggle Read/Unread
                        notificationAdapter.toggleNotificationStatus(position);
                    }
                } else {
                    // Invalid position, might happen if list changes rapidly.
                    // Can notify adapter to refresh the specific item or whole list.
                    if (position != RecyclerView.NO_POSITION) notificationAdapter.notifyItemChanged(position);
                }
            }

            // Optional: Visual feedback during swipe
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                // Example using RecyclerViewSwipeDecorator (add dependency if you want to use it)
                // new com.github.xabaras.RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                //         .addSwipeLeftBackgroundColor(ContextCompat.getColor(NotificationsActivity.this, android.R.color.holo_red_light))
                //         .addSwipeLeftActionIcon(R.drawable.ic_delete) // Make sure you have this drawable
                //         .addSwipeRightBackgroundColor(ContextCompat.getColor(NotificationsActivity.this, android.R.color.holo_green_light))
                //         .addSwipeRightActionIcon(R.drawable.ic_mark_read) // And this one
                //         .create()
                //         .decorate();
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerViewNotifications);
    }

    private void showUndoDeleteSnackbar(final int position, final Notification deletedNotification) {
        // This notification object is the one from the list *before* visual removal
        String notificationTextPreview = deletedNotification.getText().length() > 30 ?
                deletedNotification.getText().substring(0, 27) + "..." :
                deletedNotification.getText();

        // 1. Visually remove from adapter
        notificationDataList.remove(position);
        notificationAdapter.notifyItemRemoved(position);
        updateNoNotificationsView(); // Update "no items" view

        Snackbar snackbar = Snackbar.make(recyclerViewNotifications, "Deleted: " + notificationTextPreview, Snackbar.LENGTH_LONG);
        snackbar.setAction("UNDO", view -> {
            // 2. Add back if undone
            notificationDataList.add(position, deletedNotification);
            notificationAdapter.notifyItemInserted(position);
            recyclerViewNotifications.scrollToPosition(position);
            updateNoNotificationsView();
            // No DB action needed for UNDO, as we haven't deleted from DB yet
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    // 3. If not undone, confirm delete from DB
                    // Use the adapter's method which calls DAO on background thread (IDEALLY)
                    // For now, directly calling:
                    dataManager.getNotificationDao().deleteNotification(deletedNotification.getNoteId());
                    // No need to call loadAndUpdateNotifications() unless you want to absolutely ensure list consistency
                    // from DB, but visually it's already handled.
                    Toast.makeText(NotificationsActivity.this, "Notification permanently deleted.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        snackbar.show();
    }
}