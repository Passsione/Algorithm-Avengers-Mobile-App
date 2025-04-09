package com.pbdvmobile.app;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pbdvmobile.app.adapters.SessionAdapter;
import com.pbdvmobile.app.data.model.Session;
import com.pbdvmobile.app.data.model.User;
import com.pbdvmobile.app.data.SessionService;
import com.pbdvmobile.app.data.UserService;
import java.util.ArrayList;
import java.util.List;

public class SessionListFragment extends Fragment {

    private static final String ARG_TYPE = "type";

    public static final int TYPE_UPCOMING = 0;
    public static final int TYPE_PAST = 1;

    private RecyclerView rvSessions;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyMessage;

    private SessionAdapter sessionAdapter;
    private List<Session> sessionList;
    private int type;

    private SessionService sessionService;
    private UserService userService;

    public static SessionListFragment newInstance(int type) {
        SessionListFragment fragment = new SessionListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getInt(ARG_TYPE);
        }

        sessionService = new SessionService();
        userService = new UserService();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session_list, container, false);

        rvSessions = view.findViewById(R.id.rv_sessions);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);

        sessionList = new ArrayList<>();
        sessionAdapter = new SessionAdapter(sessionList, getContext());

        rvSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSessions.setAdapter(sessionAdapter);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadSessions();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSessions();
    }

    private void loadSessions() {
        User currentUser = userService.getCurrentUser();

        // Set appropriate message based on user type and session type
        if (currentUser.getUserType() == User.UserType.TUTOR) {
            tvEmptyMessage.setText(type == TYPE_UPCOMING ?
                    "You have no upcoming tutoring sessions" :
                    "You have no past tutoring sessions");
        } else {
            tvEmptyMessage.setText(type == TYPE_UPCOMING ?
                    "You have no upcoming learning sessions" :
                    "You have no past learning sessions");
        }

        if (type == TYPE_UPCOMING) {
            sessionService.getUpcomingSessions(new SessionService.SessionsCallback() {
                @Override
                public void onSuccess(List<Session> sessions) {
                    updateSessionList(sessions);
                }

                @Override
                public void onFailure(String error) {
                    handleError(error);
                }
            });
        } else {
            sessionService.getPastSessions(new SessionService.SessionsCallback() {
                @Override
                public void onSuccess(List<Session> sessions) {
                    updateSessionList(sessions);
                }

                @Override
                public void onFailure(String error) {
                    handleError(error);
                }
            });
        }
    }

    private void updateSessionList(List<Session> sessions) {
        swipeRefreshLayout.setRefreshing(false);
        sessionList.clear();
        sessionList.addAll(sessions);
        sessionAdapter.notifyDataSetChanged();

        if (sessions.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
        }
    }

    private void handleError(String error) {
        swipeRefreshLayout.setRefreshing(false);
        tvEmptyMessage.setText("Error loading sessions");
        tvEmptyMessage.setVisibility(View.VISIBLE);
    }
}
