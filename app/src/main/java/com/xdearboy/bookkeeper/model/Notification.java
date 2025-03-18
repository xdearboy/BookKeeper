package com.xdearboy.bookkeeper.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.xdearboy.bookkeeper.database.converters.DateConverter;
import com.xdearboy.bookkeeper.database.converters.NotificationTypeConverter;

import java.util.Date;

/**
 * Модель данных для уведомлений
 */
@Entity(tableName = "notifications")
@TypeConverters({DateConverter.class, NotificationTypeConverter.class})
public class Notification {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private Date createdAt;
    private String bookId;

    public Notification() {
        // Пустой конструктор для Room
    }

    @Ignore
    public Notification(@NonNull String id, String userId, String title, String message, 
                        NotificationType type, boolean isRead, Date createdAt, String bookId) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.bookId = bookId;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public enum NotificationType {
        RETURN_REMINDER,  // Напоминание о возврате книги
        NEW_BOOK,         // Уведомление о новой книге
        RECOMMENDATION    // Рекомендация книги
    }
} 