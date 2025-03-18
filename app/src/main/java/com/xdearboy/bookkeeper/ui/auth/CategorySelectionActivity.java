package com.xdearboy.bookkeeper.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.xdearboy.bookkeeper.MainActivity;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.adapter.CategoryAdapter;
import com.xdearboy.bookkeeper.model.Category;
import com.xdearboy.bookkeeper.viewmodel.CategorySelectionViewModel;

import java.util.ArrayList;
import java.util.List;

public class CategorySelectionActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {

    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private MaterialButton continueButton;
    private ProgressBar loadingProgress;
    private CategorySelectionViewModel viewModel;
    private List<Category> selectedCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_selection);

        // Инициализация ViewModel
        viewModel = new ViewModelProvider(this).get(CategorySelectionViewModel.class);

        // Инициализация UI компонентов
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        
        // Наблюдение за данными
        observeViewModel();
        
        // Загрузка категорий
        viewModel.loadCategories();
    }

    private void initViews() {
        categoriesRecyclerView = findViewById(R.id.categories_recycler_view);
        continueButton = findViewById(R.id.continue_button);
        loadingProgress = findViewById(R.id.loading_progress);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter(this, new ArrayList<>(), this);
        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        categoriesRecyclerView.setAdapter(categoryAdapter);
    }

    private void setupListeners() {
        continueButton.setOnClickListener(v -> {
            if (selectedCategories.isEmpty()) {
                Toast.makeText(this, R.string.select_at_least_one_category, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Сохраняем выбранные категории
            viewModel.saveSelectedCategories(selectedCategories);
            
            // Переходим на главный экран
            navigateToMainActivity();
        });
    }

    private void observeViewModel() {
        // Наблюдение за списком категорий
        viewModel.getCategories().observe(this, categories -> {
            categoryAdapter.updateCategories(categories);
            loadingProgress.setVisibility(View.GONE);
        });
        
        // Наблюдение за состоянием загрузки
        viewModel.isLoading().observe(this, isLoading -> {
            loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // Наблюдение за ошибками
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Наблюдение за успешным сохранением
        viewModel.isSaved().observe(this, isSaved -> {
            if (isSaved) {
                navigateToMainActivity();
            }
        });
    }

    @Override
    public void onCategoryClick(Category category, boolean isSelected) {
        if (isSelected) {
            selectedCategories.add(category);
        } else {
            selectedCategories.remove(category);
        }
        
        // Обновляем состояние кнопки продолжения
        updateContinueButton();
    }
    
    private void updateContinueButton() {
        continueButton.setEnabled(!selectedCategories.isEmpty());
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
} 