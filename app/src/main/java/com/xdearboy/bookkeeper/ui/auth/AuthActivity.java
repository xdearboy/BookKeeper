package com.xdearboy.bookkeeper.ui.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.Navigation;

import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.databinding.ActivityAuthBinding;

/**
 * Активность для работы с фрагментами аутентификации
 */
public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;
    private NavController navController;
    private static final String KEY_HAS_NAVIGATED = "has_navigated_to_register";
    private boolean hasNavigatedToRegister = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Восстанавливаем состояние, если активность пересоздается
        if (savedInstanceState != null) {
            hasNavigatedToRegister = savedInstanceState.getBoolean(KEY_HAS_NAVIGATED, false);
        }

        // Получаем NavController правильным способом
        NavHostFragment navHostFragment = 
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_auth);
        
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            
            // Проверяем, нужно ли открыть определенный фрагмент
            // Выполняем навигацию только если это первое создание активности
            // и еще не было перехода на фрагмент регистрации
            if (!hasNavigatedToRegister) {
                String fragmentToOpen = getIntent().getStringExtra("fragment");
                if (fragmentToOpen != null && fragmentToOpen.equals("register")) {
                    // Переходим на фрагмент регистрации, если текущий фрагмент - loginFragment
                    if (navController.getCurrentDestination() != null && 
                            navController.getCurrentDestination().getId() == R.id.loginFragment) {
                        navController.navigate(R.id.action_loginFragment_to_registerFragment);
                        hasNavigatedToRegister = true;
                    }
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Сохраняем состояние перед уничтожением активности
        outState.putBoolean(KEY_HAS_NAVIGATED, hasNavigatedToRegister);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }
} 