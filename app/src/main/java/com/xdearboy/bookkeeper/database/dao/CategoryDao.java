package com.xdearboy.bookkeeper.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.xdearboy.bookkeeper.model.Category;

import java.util.List;

/**
 * DAO для работы с категориями
 */
@Dao
public interface CategoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Category category);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Category> categories);
    
    @Update
    void update(Category category);
    
    @Delete
    void delete(Category category);
    
    @Query("DELETE FROM categories")
    void deleteAll();
    
    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<Category>> getAllCategories();
    
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    LiveData<Category> getCategoryById(String categoryId);
    
    @Query("SELECT * FROM categories WHERE isSelected = 1 ORDER BY name ASC")
    LiveData<List<Category>> getSelectedCategories();
    
    @Query("UPDATE categories SET isSelected = :isSelected WHERE id = :categoryId")
    void updateCategorySelection(String categoryId, boolean isSelected);
} 