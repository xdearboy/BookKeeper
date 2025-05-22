package com.xdearboy.bookkeeper.repository;
import android.app.Application;
import androidx.lifecycle.LiveData;
import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.database.dao.NotificationDao;
import com.xdearboy.bookkeeper.model.Notification;
import java.util.Date;
import java.util.List;
import java.util.UUID;
public class NotificationRepository {
    private final NotificationDao notificationDao;
    private final LiveData<List<Notification>> allNotifications;
    private static NotificationRepository instance;
    private NotificationRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        notificationDao = db.notificationDao();
        allNotifications = notificationDao.getAllNotifications();
    }
    public static synchronized NotificationRepository getInstance(Application application) {
        if (instance == null) {
            instance = new NotificationRepository(application);
        }
        return instance;
    }
    public LiveData<List<Notification>> getAllNotifications() {
        return allNotifications;
    }
    public LiveData<List<Notification>> getNotificationsForUser(String userId) {
        return notificationDao.getNotificationsForUser(userId);
    }
    public LiveData<List<Notification>> getUnreadNotificationsForUser(String userId) {
        return notificationDao.getUnreadNotificationsForUser(userId);
    }
    public LiveData<Integer> getUnreadNotificationCount(String userId) {
        return notificationDao.getUnreadNotificationCount(userId);
    }
    public void addNotification(Notification notification) {
        if (notification.getId() == null || notification.getId().isEmpty()) {
            notification.setId("notification_" + UUID.randomUUID().toString());
        }
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(new Date());
        }
        AppDatabase.databaseWriteExecutor.execute(() -> notificationDao.insert(notification));
    }
    public void createReturnReminder(String userId, String bookId, String bookTitle, Date returnDate) {
        Notification notification = new Notification();
        notification.setId("notification_" + UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setTitle("Напоминание о возврате");
        notification.setMessage("Пожалуйста, верните книгу '" + bookTitle + "' до " + returnDate);
        notification.setType(Notification.NotificationType.RETURN_REMINDER);
        notification.setRead(false);
        notification.setCreatedAt(new Date());
        notification.setBookId(bookId);
        addNotification(notification);
    }
    public void createNewBookNotification(String userId, String bookId, String bookTitle, String bookAuthor) {
        Notification notification = new Notification();
        notification.setId("notification_" + UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setTitle("Новая книга в библиотеке");
        notification.setMessage("В нашей библиотеке появилась новая книга: '" + bookTitle + "' от " + bookAuthor);
        notification.setType(Notification.NotificationType.NEW_BOOK);
        notification.setRead(false);
        notification.setCreatedAt(new Date());
        notification.setBookId(bookId);
        addNotification(notification);
    }
    public void createRecommendationNotification(String userId, String bookId, String bookTitle, String bookAuthor) {
        Notification notification = new Notification();
        notification.setId("notification_" + UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setTitle("Рекомендация книги");
        notification.setMessage("Рекомендуем вам прочитать книгу: '" + bookTitle + "' от " + bookAuthor);
        notification.setType(Notification.NotificationType.RECOMMENDATION);
        notification.setRead(false);
        notification.setCreatedAt(new Date());
        notification.setBookId(bookId);
        addNotification(notification);
    }
    public void markNotificationAsRead(String notificationId) {
        AppDatabase.databaseWriteExecutor.execute(() -> notificationDao.markAsRead(notificationId));
    }
    public void markAllNotificationsAsRead(String userId) {
        AppDatabase.databaseWriteExecutor.execute(() -> notificationDao.markAllAsRead(userId));
    }
    public void deleteNotification(String notificationId) {
        AppDatabase.databaseWriteExecutor.execute(() -> notificationDao.deleteById(notificationId));
    }
    public void deleteAllReadNotifications(String userId) {
        AppDatabase.databaseWriteExecutor.execute(() -> notificationDao.deleteAllReadNotifications(userId));
    }
} 