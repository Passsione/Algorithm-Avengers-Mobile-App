package com.pbdvmobile.app.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // Optional for tutor image, not directly for resource icon
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.DataManager;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.Subject;
import com.pbdvmobile.app.data.model.User;
import java.util.List;

public class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder> {

    private List<Resource> resourceList;
    private final Context context;
    private final DataManager dataManager;
    private final int currentUserId;
    private final boolean isCurrentUserTutor;
    private final OnResourceActionsListener listener;

    public interface OnResourceActionsListener {
        void onDownloadResource(Resource resource);
        void onEditResource(Resource resource);
        void onDeleteResource(Resource resource, int position);
    }

    public ResourceAdapter(Context context, List<Resource> resourceList, DataManager dataManager,
                           int currentUserId, boolean isCurrentUserTutor, OnResourceActionsListener listener) {
        this.context = context;
        this.resourceList = resourceList;
        this.dataManager = dataManager;
        this.currentUserId = currentUserId;
        this.isCurrentUserTutor = isCurrentUserTutor;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_resource, parent, false);
        return new ResourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourceViewHolder holder, int position) {
        Resource resource = resourceList.get(position);
        holder.bind(resource);
    }

    @Override
    public int getItemCount() {
        return resourceList.size();
    }

    public void updateList(List<Resource> newList) {
        this.resourceList = newList;
        notifyDataSetChanged();
    }
    public void removeItem(int position) {
        if (position >= 0 && position < resourceList.size()) {
            resourceList.remove(position);
            notifyItemRemoved(position);
        }
    }


    class ResourceViewHolder extends RecyclerView.ViewHolder {
        ImageView resourceTypeIcon;
        TextView resourceTitle, resourceTutor, resourceSubjectName;
        ImageButton optionsMenuButton;

        ResourceViewHolder(View itemView) {
            super(itemView);
            resourceTypeIcon = itemView.findViewById(R.id.resource_type_icon);
            resourceTitle = itemView.findViewById(R.id.resource_title);
            resourceTutor = itemView.findViewById(R.id.resource_tutor);
            resourceSubjectName = itemView.findViewById(R.id.resource_subject_name);
            optionsMenuButton = itemView.findViewById(R.id.button_resource_options_menu);
        }

        void bind(final Resource resource) {
            resourceTitle.setText(resource.getName());

            User tutor = dataManager.getUserDao().getUserByStudentNum(resource.getTutorId());
            resourceTutor.setText("By: " + (tutor != null ? (!isCurrentUserTutor ? tutor.getFirstName() + " " + tutor.getLastName() :  "ME") :"UNKNOWN"));

            Subject subject = dataManager.getSubjectDao().getSubjectById(resource.getSubjectId());
            resourceSubjectName.setText("Subject: " + (subject != null ? subject.getSubjectName() : "N/A"));

            // Determine icon based on file type from resource.getResource() (URI string)
            String resourceUriString = resource.getResource();
            if (resourceUriString != null) {
                if (resourceUriString.toLowerCase().endsWith(".pdf")) {
                    resourceTypeIcon.setImageResource(R.drawable.ic_file_pdf); // Create this drawable
                } else if (resourceUriString.toLowerCase().endsWith(".doc") || resourceUriString.toLowerCase().endsWith(".docx")) {
                    resourceTypeIcon.setImageResource(R.drawable.ic_file_word); // Create this drawable
                } else if (resourceUriString.toLowerCase().endsWith(".txt")) {
                    resourceTypeIcon.setImageResource(R.drawable.ic_file_text); // Create this drawable
                }
                else {
                    resourceTypeIcon.setImageResource(R.drawable.ic_file_text); // Create this drawable
                }
            } else {
                resourceTypeIcon.setImageResource(R.drawable.ic_file_text);
            }

            if(!isCurrentUserTutor) {
                optionsMenuButton.setImageResource(R.drawable.ic_download);
            }
            optionsMenuButton.setOnClickListener(v -> showPopupMenu(v, resource, getBindingAdapterPosition()));

            itemView.setOnClickListener(v -> {
                // Default action on item click could be download for tutees
                if (!isCurrentUserTutor || currentUserId != resource.getTutorId()) {
                    if (listener != null) listener.onDownloadResource(resource);
                } else {
                    // For tutors viewing their own resources, maybe edit is primary? Or just use menu.
                    Toast.makeText(context, "You uploaded this. Use menu for options.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void showPopupMenu(View view, final Resource resource, final int position) {
            PopupMenu popup = new PopupMenu(context, view);
            // Inflate menu based on user type and ownership
            if (isCurrentUserTutor && currentUserId == resource.getTutorId()) {
                popup.getMenuInflater().inflate(R.menu.menu_resource_owner, popup.getMenu());
            } else {
                popup.getMenuInflater().inflate(R.menu.menu_resource_viewer, popup.getMenu());
            }

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_download_resource) {
                    if (listener != null) listener.onDownloadResource(resource);
                    return true;
                } else if (itemId == R.id.action_edit_resource) {
                    if (listener != null) listener.onEditResource(resource);
                    return true;
                } else if (itemId == R.id.action_delete_resource) {
                    if (listener != null) listener.onDeleteResource(resource, position);
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }
}