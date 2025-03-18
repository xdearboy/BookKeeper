package com.xdearboy.bookkeeper.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.xdearboy.bookkeeper.model.Category;
import com.xdearboy.bookkeeper.repository.CategoryRepository;
import com.xdearboy.bookkeeper.repository.UserPreferencesRepository;

import java.util.List;

public class CategorySelectionViewModel extends AndroidViewModel {

    private final CategoryRepository categoryRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSaved = new MutableLiveData<>(false);

    public CategorySelectionViewModel(@NonNull Application application) {
        super(application);
        categoryRepository = CategoryRepository.getInstance(application);
        userPreferencesRepository = UserPreferencesRepository.getInstance(application);
        
        // Инициализация категорий при первом запуске
        categoryRepository.initializeDefaultCategories();
    }

    /**
     * Загружает список категорий
     */
    public void loadCategories() {
        isLoading.setValue(true);
        // Категории загружаются автоматически через LiveData
        isLoading.setValue(false);
    }

    /**
     * Возвращает список всех категорий
     * @return LiveData со списком категорий
     */
    public LiveData<List<Category>> getCategories() {
        return categoryRepository.getAllCategories();
    }

    /**
     * Сохраняет выбранные пользователем категории
     * @param selectedCategories Список выбранных категорий
     */
    public void saveSelectedCategories(List<Category> selectedCategories) {
        isLoading.setValue(true);
        
        try {
            // Сначала очищаем все предыдущие предпочтения
            userPreferencesRepository.clearPreferences();
            
            // Затем сохраняем новые предпочтения
            for (Category category : selectedCategories) {
                userPreferencesRepository.addPreference(category.getId());
                
                // Обновляем состояние выбора в базе данных
                categoryRepository.updateCategorySelection(category.getId(), true);
            }
            
            isSaved.setValue(true);
        } catch (Exception e) {
            error.setValue("Ошибка при сохранении предпочтений: " + e.getMessage());
        } finally {
            isLoading.setValue(false);
        }
    }

    /**
     * Возвращает состояние загрузки
     * @return LiveData с состоянием загрузки
     */
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    /**
     * Возвращает сообщение об ошибке
     * @return LiveData с сообщением об ошибке
     */
    public LiveData<String> getError() {
        return error;
    }

    /**
     * Возвращает состояние сохранения
     * @return LiveData с состоянием сохранения
     */
    public LiveData<Boolean> isSaved() {
        return isSaved;
    }
} 