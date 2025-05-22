package com.pbdvmobile.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.User; // Needed if displaying tutor info directly from User obj
import com.pbdvmobile.app.data.dao.UserDao; // To fetch tutor details if only UID is in Resource
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder> {

    private List<Resource> resourceList;
    private final Context context;
    private final String currentLoggedInUserUid;
    private final OnResourceActionsListener listener;
    private final UserDao userDao; // To fetch tutor names if needed

    public interface OnResourceActionsListener {
        void onDownloadResource(Resource resource);
        void onEditResource(Resource resource); // If editing metadata
        void onDeleteResource(Resource resource, int position);
        // void onViewResource(Resource resource); // If you have an in-app viewer
    }

    public ResourceAdapter(Context context, List<Resource> resourceList, String currentLoggedInUserUid, OnResourceActionsListener listener) {
        this.context = context;
        this.resourceList = resourceList;
        this.currentLoggedInUserUid = currentLoggedInUserUid;
        this.listener = listener;
        this.userDao = new UserDao(); // Initialize UserDao
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
        return resourceList != null ? resourceList.size() : 0;
    }

    public void updateList(List<Resource> newList) {
        this.resourceList.clear();
        if (newList != null) {
            this.resourceList.addAll(newList);
        }
        notifyDataSetChanged(); // Consider DiffUtil for better performance
    }

    class ResourceViewHolder extends RecyclerView.ViewHolder {
        ImageView resourceTypeIcon;
        TextView resourceTitle, resourceTutorName, resourceSubjectName, resourceUploadDate, resourceFileSize;
        ImageButton optionsMenuButton;

        ResourceViewHolder(View itemView) {
            super(itemView);
            resourceTypeIcon = itemView.findViewById(R.id.resource_type_icon);
            resourceTitle = itemView.findViewById(R.id.resource_title);
            resourceTutorName = itemView.findViewById(R.id.resource_tutor); // Changed from resource_tutor
            resourceSubjectName = itemView.findViewById(R.id.resource_subject_name);
            resourceUploadDate = itemView.findViewById(R.id.resource_upload_date);
            resourceFileSize = itemView.findViewById(R.id.resource_file_size);
            optionsMenuButton = itemView.findViewById(R.id.button_resource_options_menu);
        }

        void bind(final Resource resource) {
            resourceTitle.setText(resource.getName());
            resourceSubjectName.setText("Subject: " + (resource.getSubjectName() != null ? resource.getSubjectName() : "N/A"));

            // Set Tutor Name (asynchronously if not denormalized in Resource model)
            // For this example, assuming Resource model has tutorUid, and we fetch name.
            // If tutorName is denormalized in Resource, this fetch isn't needed here.
            if (resource.getTutorUid() != null) {
                if (resource.getTutorUid().equals(currentLoggedInUserUid)) {
                    resourceTutorName.setText("Uploaded by: You");
                } else {
                    // Fetch tutor's name using UserDao (can be slow for lists, consider denormalization)
                    userDao.getUserByUid(resource.getTutorUid()).addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User tutor = documentSnapshot.toObject(User.class);
                            if (tutor != null) {
                                resourceTutorName.setText("By: " + tutor.getFirstName() + " " + tutor.getLastName());
                            } else {
                                resourceTutorName.setText("By: Unknown Tutor");
                            }
                        } else {
                            resourceTutorName.setText("By: Unknown Tutor");
                        }
                    }).addOnFailureListener(e -> resourceTutorName.setText("By: Error loading tutor"));
                }
            } else {
                resourceTutorName.setText("By: Unknown");
            }


            // Set File Type Icon
            String fileType = resource.getFileType();
            if (fileType != null) {
                if (fileType.startsWith("application/pdf")) {
                    resourceTypeIcon.setImageResource(R.drawable.ic_file_pdf);
                } else if (fileType.startsWith("application/msword") || fileType.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                    resourceTypeIcon.setImageResource(R.drawable.ic_file_word);
                } else if (fileType.startsWith("text/plain")) {
                    resourceTypeIcon.setImageResource(R.drawable.ic_file_text);
                } else if (fileType.startsWith("image/")) {
                    resourceTypeIcon.setImageResource(R.drawable.ic_file_image); // Generic image icon
                } else {
                    resourceTypeIcon.setImageResource(R.drawable.ic_file_generic); // Generic file icon
                }
            } else {
                resourceTypeIcon.setImageResource(R.drawable.ic_file_generic);
            }

            // Set Upload Date
            if (resource.getUploadedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                resourceUploadDate.setText("Uploaded: " + sdf.format(resource.getUploadedAt().toDate()));
                resourceUploadDate.setVisibility(View.VISIBLE);
            } else {
                resourceUploadDate.setVisibility(View.GONE);
            }

            // Set File Size
            if (resource.getFileSize() > 0) {
                resourceFileSize.setText(formatFileSize(resource.getFileSize()));
                resourceFileSize.setVisibility(View.VISIBLE);
            } else {
                resourceFileSize.setVisibility(View.GONE);
            }


            // Configure options menu based on ownership
            boolean isOwner = resource.getTutorUid() != null && resource.getTutorUid().equals(currentLoggedInUserUid);
            optionsMenuButton.setVisibility(View.VISIBLE); // Always show menu, content changes
            optionsMenuButton.setOnClickListener(v -> showPopupMenu(v, resource, getBindingAdapterPosition(), isOwner));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    // Default action: download or view
                    listener.onDownloadResource(resource); // Or a new onViewResource(resource)
                }
            });
        }

        private String formatFileSize(long size) {
            if (size <= 0) return "0 B";
            final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
        }

        private void showPopupMenu(View view, final Resource resource, final int position, boolean isOwner) {
            PopupMenu popup = new PopupMenu(context, view);
            if (isOwner) {
                popup.getMenuInflater().inflate(R.menu.menu_resource_owner, popup.getMenu());
            } else {
                popup.getMenuInflater().inflate(R.menu.menu_resource_viewer, popup.getMenu());
            }

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_download_resource) {
                    if (listener != null) listener.onDownloadResource(resource);
                    return true;
                } else if (itemId == R.id.action_edit_resource) { // Ensure this ID exists in menu_resource_owner.xml
                    if (listener != null && isOwner) listener.onEditResource(resource);
                    else Toast.makeText(context, "You cannot edit this resource.", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.action_delete_resource) { // Ensure this ID exists in menu_resource_owner.xml
                    if (listener != null && isOwner) listener.onDeleteResource(resource, position);
                    else Toast.makeText(context, "You cannot delete this resource.", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }
}
