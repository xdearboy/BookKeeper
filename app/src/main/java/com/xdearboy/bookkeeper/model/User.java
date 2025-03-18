package com.xdearboy.bookkeeper.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.xdearboy.bookkeeper.database.converters.DateConverter;
import com.xdearboy.bookkeeper.database.converters.MapConverter;
import com.xdearboy.bookkeeper.database.converters.StringListConverter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Модель данных для пользователя
 */
@Entity(tableName = "users")
@TypeConverters({DateConverter.class, StringListConverter.class, MapConverter.class})
public class User {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String email;
    private String password; // Хранится в хешированном виде
    private String profileImageUrl;
    private Date registrationDate;
    private Date lastLoginDate;
    private List<String> favoriteGenres;
    private List<String> borrowedBookIds;
    private boolean isActive;
    private String photoUrl;
    private Date createdAt;
    private Date lastLoginAt;
    private Map<String, Object> preferences;

    public User() {
        // Пустой конструктор для Room
        favoriteGenres = new ArrayList<>();
        borrowedBookIds = new ArrayList<>();
        preferences = new HashMap<>();
    }

    @Ignore
    public User(@NonNull String id, String name, String email, String password, 
                String profileImageUrl, Date registrationDate, Date lastLoginDate, 
                List<String> favoriteGenres, List<String> borrowedBookIds, boolean isActive) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
        this.registrationDate = registrationDate;
        this.lastLoginDate = lastLoginDate;
        this.favoriteGenres = favoriteGenres != null ? favoriteGenres : new ArrayList<>();
        this.borrowedBookIds = borrowedBookIds != null ? borrowedBookIds : new ArrayList<>();
        this.isActive = isActive;
        this.createdAt = new Date();
        this.lastLoginAt = new Date();
        this.preferences = new HashMap<>();
    }
    
    @Ignore
    public User(@NonNull String id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.favoriteGenres = new ArrayList<>();
        this.borrowedBookIds = new ArrayList<>();
        this.isActive = true;
        this.createdAt = new Date();
        this.lastLoginAt = new Date();
        this.preferences = new HashMap<>();
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public List<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    public void setFavoriteGenres(List<String> favoriteGenres) {
        this.favoriteGenres = favoriteGenres;
    }

    public List<String> getBorrowedBookIds() {
        return borrowedBookIds;
    }

    public void setBorrowedBookIds(List<String> borrowedBookIds) {
        this.borrowedBookIds = borrowedBookIds;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void addBorrowedBook(String bookId) {
        if (borrowedBookIds == null) {
            borrowedBookIds = new ArrayList<>();
        }
        if (!borrowedBookIds.contains(bookId)) {
            borrowedBookIds.add(bookId);
        }
    }

    public void removeBorrowedBook(String bookId) {
        if (borrowedBookIds != null) {
            borrowedBookIds.remove(bookId);
        }
    }

    public void addFavoriteGenre(String genre) {
        if (favoriteGenres == null) {
            favoriteGenres = new ArrayList<>();
        }
        if (!favoriteGenres.contains(genre)) {
            favoriteGenres.add(genre);
        }
    }

    public void removeFavoriteGenre(String genre) {
        if (favoriteGenres != null) {
            favoriteGenres.remove(genre);
        }
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public void setPreference(String key, Object value) {
        if (preferences == null) {
            preferences = new HashMap<>();
        }
        preferences.put(key, value);
    }

    public Object getPreference(String key) {
        if (preferences == null) {
            return null;
        }
        return preferences.get(key);
    }
} 