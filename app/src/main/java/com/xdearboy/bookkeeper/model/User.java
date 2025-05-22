package com.xdearboy.bookkeeper.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Entity(tableName = "users")
public class User {
    @PrimaryKey
    @NonNull
    private String id;
    
    @NonNull
    private String email;
    
    @NonNull
    private String password;
    
    private String name;
    private String profileImageUrl;
    private long registrationDate;
    private long lastLoginDate;
    private String borrowedBookIds; // Comma-separated list of book IDs
    private String favoriteGenres; // Comma-separated list of genre IDs
    private boolean isActive;
    
    public User() {
        // Required empty constructor for Room
    }
    
    @Ignore
    public User(@NonNull String id, @NonNull String email, @NonNull String password, String name) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.registrationDate = System.currentTimeMillis();
        this.lastLoginDate = System.currentTimeMillis();
        this.isActive = true;
    }
    
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    @NonNull
    public String getEmail() {
        return email;
    }
    
    public void setEmail(@NonNull String email) {
        this.email = email;
    }
    
    @NonNull
    public String getPassword() {
        return password;
    }
    
    public void setPassword(@NonNull String password) {
        this.password = password;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    
    public long getRegistrationDate() {
        return registrationDate;
    }
    
    public void setRegistrationDate(long registrationDate) {
        this.registrationDate = registrationDate;
    }
    
    // For compatibility with Date objects
    public void setRegistrationDate(Date date) {
        if (date != null) {
            this.registrationDate = date.getTime();
        }
    }
    
    public long getLastLoginDate() {
        return lastLoginDate;
    }
    
    public void setLastLoginDate(long lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }
    
    // For compatibility with Date objects
    public void setLastLoginDate(Date date) {
        if (date != null) {
            this.lastLoginDate = date.getTime();
        }
    }
    
    // For Firebase compatibility
    public void setCreatedAt(Date date) {
        setRegistrationDate(date);
    }
    
    // For Firebase compatibility
    public void setLastLoginAt(Date date) {
        setLastLoginDate(date);
    }
    
    public String getBorrowedBookIds() {
        return borrowedBookIds;
    }
    
    public void setBorrowedBookIds(String borrowedBookIds) {
        this.borrowedBookIds = borrowedBookIds;
    }
    
    public List<String> getBorrowedBookIdsList() {
        if (borrowedBookIds == null || borrowedBookIds.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(borrowedBookIds.split(","));
    }
    
    public void addBorrowedBook(String bookId) {
        if (bookId == null || bookId.isEmpty()) {
            return;
        }
        
        List<String> bookIds = getBorrowedBookIdsList();
        if (!bookIds.contains(bookId)) {
            List<String> newList = new ArrayList<>(bookIds);
            newList.add(bookId);
            setBorrowedBookIds(String.join(",", newList));
        }
    }
    
    public void removeBorrowedBook(String bookId) {
        if (bookId == null || bookId.isEmpty() || borrowedBookIds == null || borrowedBookIds.isEmpty()) {
            return;
        }
        
        List<String> bookIds = getBorrowedBookIdsList();
        if (bookIds.contains(bookId)) {
            List<String> newList = new ArrayList<>(bookIds);
            newList.remove(bookId);
            setBorrowedBookIds(newList.isEmpty() ? "" : String.join(",", newList));
        }
    }
    
    public String getFavoriteGenres() {
        return favoriteGenres;
    }
    
    public void setFavoriteGenres(String favoriteGenres) {
        this.favoriteGenres = favoriteGenres;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id.equals(user.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}