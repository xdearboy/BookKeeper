package com.xdearboy.bookkeeper.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.xdearboy.bookkeeper.model.Category;
import com.xdearboy.bookkeeper.model.UserPreferences;

import java.util.List;

/**
 * DAO для работы с предпочтениями пользователя
 */
@Dao
public interface UserPreferencesDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserPreferences userPreferences);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UserPreferences> userPreferences);
    
    @Delete
    void delete(UserPreferences userPreferences);
    
    @Query("DELETE FROM user_preferences WHERE userId = :userId")
    void deleteAllForUser(String userId);
    
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    LiveData<List<UserPreferences>> getUserPreferences(String userId);
    
    @Transaction
    @Query("SELECT c.* FROM categories c " +
           "INNER JOIN user_preferences up ON c.id = up.categoryId " +
           "WHERE up.userId = :userId " +
           "ORDER BY c.name ASC")
    LiveData<List<Category>> getUserCategories(String userId);
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_preferences WHERE userId = :userId AND categoryId = :categoryId)")
    boolean hasPreference(String userId, String categoryId);
} 