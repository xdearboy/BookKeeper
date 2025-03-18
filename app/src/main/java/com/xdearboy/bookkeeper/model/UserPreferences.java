package com.xdearboy.bookkeeper.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Модель данных для хранения предпочтений пользователя
 */
@Entity(tableName = "user_preferences",
        foreignKeys = {
                @ForeignKey(entity = User.class,
                        parentColumns = "id",
                        childColumns = "userId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Category.class,
                        parentColumns = "id",
                        childColumns = "categoryId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index("userId"),
                @Index("categoryId")
        })
public class UserPreferences {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @NonNull
    private String userId;
    @NonNull
    private String categoryId;

    public UserPreferences() {
        // Пустой конструктор для Room
    }

    @Ignore
    public UserPreferences(@NonNull String userId, @NonNull String categoryId) {
        this.userId = userId;
        this.categoryId = categoryId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    @NonNull
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(@NonNull String categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPreferences that = (UserPreferences) o;
        return userId.equals(that.userId) && categoryId.equals(that.categoryId);
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + categoryId.hashCode();
        return result;
    }
} 