package com.xdearboy.bookkeeper.ui.notifications;
import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.xdearboy.bookkeeper.model.Notification;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.repository.NotificationRepository;
import com.xdearboy.bookkeeper.repository.UserRepository;
import com.xdearboy.bookkeeper.util.SessionManager;
import java.util.ArrayList;
import java.util.List;
public class NotificationsViewModel extends AndroidViewModel {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<List<Notification>> notifications;
    private final MutableLiveData<Integer> unreadCount;
    private LiveData<List<Notification>> notificationsLiveData;
    private LiveData<Integer> unreadCountLiveData;
    private Observer<List<Notification>> notificationsObserver;
    private Observer<Integer> unreadCountObserver;
    public NotificationsViewModel(Application application) {
        super(application);
        notificationRepository = NotificationRepository.getInstance(application);
        userRepository = UserRepository.getInstance(application);
        notifications = new MutableLiveData<>(new ArrayList<>());
        unreadCount = new MutableLiveData<>(0);
        notificationsObserver = new Observer<List<Notification>>() {
            @Override
            public void onChanged(List<Notification> notificationList) {
                notifications.setValue(notificationList);
            }
        };
        unreadCountObserver = new Observer<Integer>() {
            @Override
            public void onChanged(Integer count) {
                unreadCount.setValue(count);
            }
        };
        loadNotifications();
    }
    private void loadNotifications() {
        String userId = SessionManager.getInstance(getApplication()).getUserId();
        if (userId != null) {
            if (notificationsLiveData != null) {
                notificationsLiveData.removeObserver(notificationsObserver);
            }
            if (unreadCountLiveData != null) {
                unreadCountLiveData.removeObserver(unreadCountObserver);
            }
            notificationsLiveData = notificationRepository.getNotificationsForUser(userId);
            unreadCountLiveData = notificationRepository.getUnreadNotificationCount(userId);
            notificationsLiveData.observeForever(notificationsObserver);
            unreadCountLiveData.observeForever(unreadCountObserver);
        }
    }
    public LiveData<List<Notification>> getNotifications() {
        return notifications;
    }
    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }
    public void markAsRead(String notificationId) {
        notificationRepository.markNotificationAsRead(notificationId);
    }
    public void markAllAsRead() {
        String userId = SessionManager.getInstance(getApplication()).getUserId();
        if (userId != null) {
            notificationRepository.markAllNotificationsAsRead(userId);
        }
    }
    public void deleteNotification(String notificationId) {
        notificationRepository.deleteNotification(notificationId);
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        if (notificationsLiveData != null) {
            notificationsLiveData.removeObserver(notificationsObserver);
        }
        if (unreadCountLiveData != null) {
            unreadCountLiveData.removeObserver(unreadCountObserver);
        }
    }
}