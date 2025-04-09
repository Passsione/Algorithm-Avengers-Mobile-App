package com.pbdvmobile.app.fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.pbdvmobile.app.R;
import com.pbdvmobile.app.adapters.ResourceAdapter;
import com.pbdvmobile.app.adapters.SessionAdapter;
import com.pbdvmobile.app.data.model.Resource;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.ResourceService;
import com.pbdvmobile.app.data.SessionService;
import com.pbdvmobile.app.data.UserService;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private TextView tvWelcome, tvNextSession, tvCredits, tvStatistic1, tvStatistic2;
    private RecyclerView rvRecentSessions, rvRecommendedResources;

    private SessionAdapter sessionAdapter;
    private ResourceAdapter resourceAdapter;
    private List<Session> sessionList;
    private List<Resource> resourceList;

    private UserService userService;
    private SessionService sessionService;
    private ResourceService resourceService;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        userService = new UserService();
        sessionService = new SessionService();
        resourceService = new ResourceService();
        currentUser = userService.getCurrentUser();

        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvNextSession = view.findViewById(R.id.tv_next_session);
        tvCredits = view.findViewById(R.id.tv_credits);
        tvStatistic1 = view.findViewById(R.id.tv_statistic1);
        tvStatistic2 = view.findViewById(R.id.tv_statistic2);
        rvRecentSessions = view.findViewById(R.id.rv_recent_sessions);
        rvRecommendedResources = view.findViewById(R.id.rv_recommended_resources);

        // Initialize lists and adapters
        sessionList = new ArrayList<>();
        resourceList = new ArrayList<>();

        sessionAdapter = new SessionAdapter(sessionList, getContext());
        resourceAdapter = new ResourceAdapter(resourceList, getContext());

        rvRecentSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentSessions.setAdapter(sessionAdapter);

        rvRecommendedResources.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecommendedResources.setAdapter(resourceAdapter);

        // Setup UI based on user type
        setupUserTypeSpecificUI();

        // Load user data
        loadDashboardData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh dashboard data
        loadDashboardData();
    }

    private void setupUserTypeSpecificUI() {
        if (currentUser.getUserType() == User.UserType.TUTOR) {
            tvWelcome.setText("Welcome, Tutor " + currentUser.getFullName());
            tvStatistic1.setText("Total Tutoring Sessions: --");
            tvStatistic2.setText("Average Rating: --");
        } else {
            tvWelcome.setText("Welcome, " + currentUser.getFullName());
            tvStatistic1.setText("Total Learning Sessions: --");
            tvStatistic2.setText("Subjects Explored: --");
        }
    }

    private void loadDashboardData() {
        // Refresh user data to get updated credits
        userService.refreshUserData(new UserService.UserDetailCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                tvCredits.setText("Credits: " + currentUser.getCredits());

                // Load upcoming sessions for next session display
                sessionService.getUpcomingSessions(new SessionService.SessionsCallback() {
                    @Override
                    public void onSuccess(List<Session> sessions) {
                        if (!sessions.isEmpty()) {
                            Session nextSession = sessions.get(0); // Assuming sorted by date
                            displayNextSession(nextSession);

                            // For recent sessions display
                            int displayCount = Math.min(3, sessions.size());
                            sessionList.clear();
                            sessionList.addAll(sessions.subList(0, displayCount));
                            sessionAdapter.notifyDataSetChanged();
                        } else {
                            tvNextSession.setText("No upcoming sessions");

                            // If no upcoming, try showing recent past sessions
                            loadRecentPastSessions();
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        tvNextSession.setText("Error loading sessions");
                        loadRecentPastSessions();
                    }
                });

                // Load recommended resources
                resourceService.getRecommendedResources(currentUser.getId(), new ResourceService.ResourcesCallback() {
                    @Override
                    public void onSuccess(List<Resource> resources) {
                        resourceList.clear();
                        resourceList.addAll(resources);
                        resourceAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(String error) {
                        // Handle error
                    }
                });

                // Load statistics
                loadUserStatistics();
            }

            @Override
            public void onFailure(String error) {
                // Handle error
            }
        });
    }

    private void loadRecentPastSessions() {
        sessionService.getPastSessions(new SessionService.SessionsCallback() {
            @Override
            public void onSuccess(List<Session> sessions) {
                if (!sessions.isEmpty()) {
                    int displayCount = Math.min(3, sessions.size());
                    sessionList.clear();
                    sessionList.addAll(sessions.subList(0, displayCount));
                    sessionAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(String error) {
                // Handle error
            }
        });
    }

    private void displayNextSession(Session session) {
        String role = currentUser.getUserType() == User.UserType.TUTOR ? "tutee" : "tutor";
        String otherUserId = currentUser.getUserType() == User.UserType.TUTOR ?
                session.getTuteeId() : session.getTutorId();

        userService.getUserById(otherUserId, new UserService.UserDetailCallback() {
            @Override
            public void onSuccess(User otherUser) {
                // Format next session text
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

                String date = dateFormat.format(session.getStartTime());
                String time = timeFormat.format(session.getStartTime());

                String nextSessionText = String.format("Next session: %s with %s (%s)\n%s at %s",
                        session.getSubject(),
                        otherUser.getFullName(),
                        role,
                        date,
                        time);

                tvNextSession.setText(nextSessionText);
            }

            @Override
            public void onFailure(String error) {
                // Simplified version without user details
                SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault());
                String nextSessionText = String.format("Next session: %s\n%s",
                        session.getSubject(),
                        dateTimeFormat.format(session.getStartTime()));

                tvNextSession.setText(nextSessionText);
            }
        });
    }

    private void loadUserStatistics() {
        if (currentUser.getUserType() == User.UserType.TUTOR) {
            // For tutors
            sessionService.getTutorSessionCount(currentUser.getId(), new SessionService.CountCallback() {
                @Override
                public void onSuccess(int count) {
                    tvStatistic1.setText("Total Tutoring Sessions: " + count);
                }

                @Override
                public void onFailure(String error) {
                    // Handle error
                }
            });

            userService.getTutorAverageRating(currentUser.getId(), new UserService.RatingCallback() {
                @Override
                public void onSuccess(double rating) {
                    tvStatistic2.setText(String.format(Locale.getDefault(), "Average Rating: %.1f", rating));
                }

                @Override
                public void onFailure(String error) {
                    // Handle error
                }
            });

        } else {
            // For tutees
            sessionService.getTuteeSessionCount(currentUser.getId(), new SessionService.CountCallback() {
                @Override
                public void onSuccess(int count) {
                    tvStatistic1.setText("Total Learning Sessions: " + count);
                }

                @Override
                public void onFailure(String error) {
                    // Handle error
                }
            });

            sessionService.getTuteeSubjectCount(currentUser.getId(), new SessionService.CountCallback() {
                @Override
                public void onSuccess(int count) {
                    tvStatistic2.setText("Subjects Explored: " + count);
                }

                @Override
                public void onFailure(String error) {
                    // Handle error
                }
            });
        }
    }
}