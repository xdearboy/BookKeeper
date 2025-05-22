package com.xdearboy.bookkeeper.database.dao;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.xdearboy.bookkeeper.model.Notification;
import java.util.List;
@Dao
public interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Notification notification);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Notification> notifications);
    @Update
    void update(Notification notification);
    @Delete
    void delete(Notification notification);
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    void deleteById(String notificationId);
    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    LiveData<Notification> getNotificationById(String notificationId);
    @Query("SELECT * FROM notifications")
    LiveData<List<Notification>> getAllNotifications();
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<Notification>> getNotificationsForUser(String userId);
    @Query("SELECT * FROM notifications WHERE userId = :userId AND isRead = 0 ORDER BY createdAt DESC")
    LiveData<List<Notification>> getUnreadNotificationsForUser(String userId);
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    LiveData<Integer> getUnreadNotificationCount(String userId);
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    void markAsRead(String notificationId);
    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    void markAllAsRead(String userId);
    @Query("DELETE FROM notifications WHERE userId = :userId AND isRead = 1")
    void deleteAllReadNotifications(String userId);
} 