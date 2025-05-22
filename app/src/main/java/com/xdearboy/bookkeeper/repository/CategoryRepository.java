package com.xdearboy.bookkeeper.repository;
import android.app.Application;
import androidx.lifecycle.LiveData;
import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.database.dao.CategoryDao;
import com.xdearboy.bookkeeper.model.Category;
import java.util.List;
import java.util.UUID;
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
    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }
    public LiveData<List<Category>> getSelectedCategories() {
        return selectedCategories;
    }
    public LiveData<Category> getCategoryById(String categoryId) {
        return categoryDao.getCategoryById(categoryId);
    }
    public String addCategory(String name, String description, String iconUrl) {
        String id = UUID.randomUUID().toString();
        Category category = new Category(id, name, description, iconUrl, false);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.insert(category);
        });
        return id;
    }
    public void updateCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.update(category);
        });
    }
    public void deleteCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.delete(category);
        });
    }
    public void updateCategorySelection(String categoryId, boolean isSelected) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categoryDao.updateCategorySelection(categoryId, isSelected);
        });
    }
    public void initializeDefaultCategories() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (allCategories.getValue() == null || allCategories.getValue().isEmpty()) {
                addCategory("Фантастика", "Научная фантастика, фэнтези и другие фантастические жанры", "https:example.comfantasy.png");
                addCategory("Детективы", "Криминальные и детективные истории", "https:example.comdetective.png");
                addCategory("Романы", "Любовные и романтические истории", "https:example.comromance.png");
                addCategory("Наука", "Научно-популярная литература", "https:example.comscience.png");
                addCategory("История", "Исторические книги и биографии", "https:example.comhistory.png");
                addCategory("Бизнес", "Книги о бизнесе, финансах и саморазвитии", "https:example.combusiness.png");
                addCategory("Приключения", "Приключенческая литература", "https:example.comadventure.png");
                addCategory("Психология", "Книги по психологии и самопомощи", "https:example.compsychology.png");
            }
        });
    }
} 