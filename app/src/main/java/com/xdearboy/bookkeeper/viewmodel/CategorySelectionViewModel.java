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
        categoryRepository.initializeDefaultCategories();
    }
    public void loadCategories() {
        isLoading.setValue(true);
        isLoading.setValue(false);
    }
    public LiveData<List<Category>> getCategories() {
        return categoryRepository.getAllCategories();
    }
    public void saveSelectedCategories(List<Category> selectedCategories) {
        isLoading.setValue(true);
        try {
            userPreferencesRepository.clearPreferences();
            for (Category category : selectedCategories) {
                userPreferencesRepository.addPreference(category.getId());
                categoryRepository.updateCategorySelection(category.getId(), true);
            }
            isSaved.setValue(true);
        } catch (Exception e) {
            error.setValue("Ошибка при сохранении предпочтений: " + e.getMessage());
        } finally {
            isLoading.setValue(false);
        }
    }
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
    public LiveData<String> getError() {
        return error;
    }
    public LiveData<Boolean> isSaved() {
        return isSaved;
    }
} 