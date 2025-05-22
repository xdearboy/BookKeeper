package com.xdearboy.bookkeeper.database.converters;
import androidx.room.TypeConverter;
import com.xdearboy.bookkeeper.model.Notification;
public class NotificationTypeConverter {
    @TypeConverter
    public static Notification.NotificationType fromString(String value) {
        if (value == null) {
            return null;
        }
        return Notification.NotificationType.valueOf(value);
    }
    @TypeConverter
    public static String fromNotificationType(Notification.NotificationType type) {
        if (type == null) {
            return null;
        }
        return type.name();
    }
} 