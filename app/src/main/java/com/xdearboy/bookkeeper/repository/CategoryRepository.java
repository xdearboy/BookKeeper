package com.xdearboy.bookkeeper.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.database.dao.CategoryDao;
import com.xdearboy.bookkeeper.model.Category;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с категориями
 */
public class CategoryRepository {
    
    private final CategoryDao categoryDao;
    private final LiveData<List<Category>> allCategories;
    private final LiveData<List<Category>> selectedCategories;
    
    private static CategoryRepository instance;
    
    private CategoryRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        categoryDao = db.categoryDao();
        allCategories = categoryDao.getAllCategories();
        selectedCategories = categoryDao.getSelectedCategories();
    }
    
    public static synchronized CategoryRepository getInstance(Application application) {
        if (instance == null) {
            instance = new CategoryRepository(application);
        }
        return instance;
    }
    
    /**
     * Возвращает все категории
     * @return LiveData со списком всех категорий
     */
    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }
    
    /**
     * Возвращает выбранные категории
     * @return LiveData со списком выбранных категорий
     */
    public LiveData<List<Category>> getSelectedCategories() {
        return selectedCategories;
    }
    
    /**
     * Возвращает категорию по ID
     * @param categoryId ID категории
     * @return LiveData с категорией
     */
    public LiveData<Category> getCategoryById(String categoryId) {
        return categoryDao.getCategoryById(categoryId);
    }
    
    /**
     * Добавляет новую категорию
     * @param name Название категории
     * @param description Описание категории
     * @param iconUrl URL иконки категории
     * @return ID новой категории
     */
    public String addCategory(String name, String description, String iconUrl) {
        String id = UUID.randomUUID().toString();
        Category category = new Category(id, name, description, iconUrl, false);
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.insert(category);
        });
        
        return id;
    }
    
    /**
     * Обновляет категорию
     * @param category Категория для обновления
     */
    public void updateCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.update(category);
        });
    }
    
    /**
     * Удаляет категорию
     * @param category Категория для удаления
     */
    public void deleteCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.delete(category);
        });
    }
    
    /**
     * Обновляет выбор категории
     * @param categoryId ID категории
     * @param isSelected Выбрана ли категория
     */
    public void updateCategorySelection(String categoryId, boolean isSelected) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.updateCategorySelection(categoryId, isSelected);
        });
    }
    
    /**
     * Инициализирует базу данных предустановленными категориями
     */
    public void initializeDefaultCategories() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Проверяем, есть ли уже категории в базе
            if (allCategories.getValue() == null || allCategories.getValue().isEmpty()) {
                // Добавляем предустановленные категории
                addCategory("Фантастика", "Научная фантастика, фэнтези и другие фантастические жанры", "https://example.com/icons/fantasy.png");
                addCategory("Детективы", "Криминальные и детективные истории", "https://example.com/icons/detective.png");
                addCategory("Романы", "Любовные и романтические истории", "https://example.com/icons/romance.png");
                addCategory("Наука", "Научно-популярная литература", "https://example.com/icons/science.png");
                addCategory("История", "Исторические книги и биографии", "https://example.com/icons/history.png");
                addCategory("Бизнес", "Книги о бизнесе, финансах и саморазвитии", "https://example.com/icons/business.png");
                addCategory("Приключения", "Приключенческая литература", "https://example.com/icons/adventure.png");
                addCategory("Психология", "Книги по психологии и самопомощи", "https://example.com/icons/psychology.png");
            }
        });
    }
} 