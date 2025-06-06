package com.xdearboy.bookkeeper.ui.notifications;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.adapter.NotificationAdapter;
import com.xdearboy.bookkeeper.databinding.FragmentNotificationsBinding;
import com.xdearboy.bookkeeper.model.Notification;
import java.util.ArrayList;
public class NotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {
    private FragmentNotificationsBinding binding;
    private NotificationsViewModel notificationsViewModel;
    private NotificationAdapter notificationAdapter;
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        setupRecyclerView();
        setupFab();
        observeViewModel();
        return root;
    }
    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.notificationsRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationAdapter = new NotificationAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(notificationAdapter);
    }
    private void setupFab() {
        binding.fabMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }
    private void markAllAsRead() {
        notificationsViewModel.markAllAsRead();
        Snackbar.make(binding.getRoot(), R.string.mark_all_as_read, Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.fabMarkAllRead)
                .show();
    }
    private void observeViewModel() {
        notificationsViewModel.getNotifications().observe(getViewLifecycleOwner(), this::updateNotificationsUI);
        notificationsViewModel.getUnreadCount().observe(getViewLifecycleOwner(), this::updateUnreadCount);
    }
    private void updateUnreadCount(Integer count) {
        binding.fabMarkAllRead.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }
    private void updateNotificationsUI(java.util.List<Notification> notifications) {
        notificationAdapter.updateNotifications(notifications);
        if (notifications.isEmpty()) {
            binding.emptyNotificationsView.setVisibility(View.VISIBLE);
            binding.notificationsRecyclerView.setVisibility(View.GONE);
            binding.fabMarkAllRead.setVisibility(View.GONE);
        } else {
            binding.emptyNotificationsView.setVisibility(View.GONE);
            binding.notificationsRecyclerView.setVisibility(View.VISIBLE);
            binding.fabMarkAllRead.setVisibility(
                    notifications.stream().anyMatch(n -> !n.isRead()) ? View.VISIBLE : View.GONE);
        }
    }
    @Override
    public void onNotificationClick(Notification notification) {
        notificationsViewModel.markAsRead(notification.getId());
        if (notification.getBookId() != null) {
            Snackbar.make(binding.getRoot(),
                    getString(R.string.open_book_details, notification.getBookId()),
                    Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.fabMarkAllRead)
                    .show();
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}