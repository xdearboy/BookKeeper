package com.xdearboy.bookkeeper.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.database.dao.UserPreferencesDao;
import com.xdearboy.bookkeeper.model.Category;
import com.xdearboy.bookkeeper.model.UserPreferences;
import com.xdearboy.bookkeeper.util.SessionManager;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Репозиторий для работы с предпочтениями пользователя
 */
public class UserPreferencesRepository {
    
    private final UserPreferencesDao userPreferencesDao;
    private final SessionManager sessionManager;
    
    private static UserPreferencesRepository instance;
    
    private UserPreferencesRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        userPreferencesDao = db.userPreferencesDao();
        sessionManager = SessionManager.getInstance(application);
    }
    
    public static synchronized UserPreferencesRepository getInstance(Application application) {
        if (instance == null) {
            instance = new UserPreferencesRepository(application);
        }
        return instance;
    }
    
    /**
     * Возвращает предпочтения текущего пользователя
     * @return LiveData со списком предпочтений
     */
    public LiveData<List<UserPreferences>> getCurrentUserPreferences() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            return null;
        }
        return userPreferencesDao.getUserPreferences(userId);
    }
    
    /**
     * Возвращает категории текущего пользователя
     * @return LiveData со списком категорий
     */
    public LiveData<List<Category>> getCurrentUserCategories() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            return null;
        }
        return userPreferencesDao.getUserCategories(userId);
    }
    
    /**
     * Добавляет предпочтение для текущего пользователя
     * @param categoryId ID категории
     * @return true, если операция выполнена успешно
     */
    public boolean addPreference(String categoryId) {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            return false;
        }
        
        UserPreferences userPreferences = new UserPreferences(userId, categoryId);
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            userPreferencesDao.insert(userPreferences);
        });
        
        return true;
    }
    
    /**
     * Удаляет предпочтение для текущего пользователя
     * @param categoryId ID категории
     * @return true, если операция выполнена успешно
     */
    public boolean removePreference(String categoryId) {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            return false;
        }
        
        UserPreferences userPreferences = new UserPreferences(userId, categoryId);
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            userPreferencesDao.delete(userPreferences);
        });
        
        return true;
    }
    
    /**
     * Проверяет, есть ли у пользователя предпочтение для категории
     * @param categoryId ID категории
     * @return true, если предпочтение существует
     */
    public boolean hasPreference(String categoryId) {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            return false;
        }
        
        try {
            return AppDatabase.databaseWriteExecutor.submit(() -> 
                    userPreferencesDao.hasPreference(userId, categoryId)).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Удаляет все предпочтения текущего пользователя
     * @return true, если операция выполнена успешно
     */
    public boolean clearPreferences() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            return false;
        }
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            userPreferencesDao.deleteAllForUser(userId);
        });
        
        return true;
    }
} 