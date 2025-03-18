package com.xdearboy.bookkeeper.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.xdearboy.bookkeeper.model.User;

import java.util.List;

@Dao
public interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);
    
    @Update
    void update(User user);
    
    @Delete
    void delete(User user);
    
    @Query("DELETE FROM users WHERE id = :userId")
    void deleteById(String userId);
    
    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<User> getUserById(String userId);
    
    @Query("SELECT * FROM users")
    LiveData<List<User>> getAllUsers();
    
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);
    
    @Query("SELECT * FROM users WHERE email = :email AND password = :passwordHash LIMIT 1")
    User login(String email, String passwordHash);
    
    @Query("UPDATE users SET lastLoginDate = :lastLoginDate WHERE id = :userId")
    void updateLastLoginDate(String userId, long lastLoginDate);
    
    @Query("UPDATE users SET borrowedBookIds = :borrowedBookIds WHERE id = :userId")
    void updateBorrowedBooks(String userId, String borrowedBookIds);
    
    @Query("UPDATE users SET favoriteGenres = :favoriteGenres WHERE id = :userId")
    void updateFavoriteGenres(String userId, String favoriteGenres);
    
    @Query("UPDATE users SET isActive = :isActive WHERE id = :userId")
    void updateActiveStatus(String userId, boolean isActive);
} 