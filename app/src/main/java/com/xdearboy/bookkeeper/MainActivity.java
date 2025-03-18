package com.xdearboy.bookkeeper;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.xdearboy.bookkeeper.databinding.ActivityMainBinding;
import com.xdearboy.bookkeeper.repository.FirebaseRepository;
import com.xdearboy.bookkeeper.ui.auth.LoginActivity;
import com.xdearboy.bookkeeper.ui.home.HomeFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseRepository firebaseRepository;
    private NavController navController;
    
    // Статический флаг для отслеживания первого запуска активности
    private static boolean isFirstInit = true;
    // Флаг для текущего экземпляра
    private boolean isInitialized = false;
    // Флаг для отслеживания перезапуска в пределах одной сессии
    private static int initCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Принудительная защита от многократной инициализации
        initCounter++;
        if (initCounter > 1) {
            Log.w(TAG, "Предотвращение многократной инициализации #" + initCounter 
                  + ", возврат без изменений");
            return;
        }
        
        if (isInitialized) {
            Log.w(TAG, "MainActivity уже инициализирована, пропускаем повторную инициализацию");
            return;
        }
        
        // Устанавливаем флаг инициализации в начале, чтобы предотвратить повторные вызовы
        isInitialized = true;
        
        try {
            // При первой инициализации выводим подробные логи
            if (isFirstInit) {
                Log.d(TAG, "Первая инициализация MainActivity");
                isFirstInit = false;
            } else {
                Log.d(TAG, "Повторная инициализация MainActivity");
            }
            
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            // Настройка Toolbar
            setupToolbar();
            
            // Инициализация репозитория Firebase
            firebaseRepository = FirebaseRepository.getInstance();
            
            // Проверка авторизации
            if (!firebaseRepository.isUserLoggedIn()) {
                Log.w(TAG, "Пользователь не авторизован, переходим на LoginActivity");
                navigateToLogin();
                return;
            }
            
            Log.d(TAG, "Пользователь авторизован, настраиваем интерфейс");
            
            // Настройка нижней навигации
            setupBottomNavigation();
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации MainActivity", e);
            Toast.makeText(this, "Ошибка инициализации: " + e.getMessage(), Toast.LENGTH_LONG).show();
            
            // Если не удалось инициализировать активность, показываем временное решение
            showFallbackView();
        }
    }
    
    @Override 
    protected void onStart() {
        super.onStart();
        // Сбрасываем счетчик при нормальном переходе по жизненному циклу
        initCounter = 0;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Проверяем авторизацию пользователя при возобновлении активности
        if (firebaseRepository != null && !firebaseRepository.isUserLoggedIn()) {
            Log.w(TAG, "Пользователь не авторизован (при возобновлении), переходим на LoginActivity");
            navigateToLogin();
        }
    }
    
    /**
     * Настраивает Toolbar в качестве ActionBar
     */
    private void setupToolbar() {
        try {
            Toolbar toolbar = binding.toolbar;
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                Log.d(TAG, "Toolbar настроен успешно");
            } else {
                Log.e(TAG, "Toolbar не найден в макете");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при настройке Toolbar", e);
        }
    }
    
    private void setupBottomNavigation() {
        try {
            BottomNavigationView navView = binding.navView;
            
            // Получаем NavController из NavHostFragment (более надежный способ)
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
            
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                
                // Настройка верхнего уровня навигации
                AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                        .build();
                
                // Настройка UI с NavController - добавляем проверку на наличие ActionBar
                if (getSupportActionBar() != null) {
                    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
                } else {
                    Log.w(TAG, "ActionBar не найден, пропускаем setupActionBarWithNavController");
                }
                
                NavigationUI.setupWithNavController(binding.navView, navController);
                
                Log.d(TAG, "Навигация успешно настроена");
            } else {
                Log.e(TAG, "NavHostFragment не найден!");
                
                // Если NavHostFragment не найден, загружаем HomeFragment вручную
                showHomeFragmentDirectly();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при настройке навигации", e);
            
            // В случае ошибки, загружаем HomeFragment вручную
            showHomeFragmentDirectly();
        }
    }
    
    private void showHomeFragmentDirectly() {
        try {
            // Загружаем HomeFragment вручную, если навигация не работает
            HomeFragment homeFragment = new HomeFragment();
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, homeFragment)
                .commit();
            
            Log.d(TAG, "HomeFragment загружен напрямую");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке HomeFragment", e);
        }
    }
    
    private void showFallbackView() {
        try {
            // Создаем простой интерфейс для отображения, если все остальное не сработало
            setContentView(R.layout.activity_fallback);
        } catch (Exception e) {
            Log.e(TAG, "Критическая ошибка: не удалось показать запасной интерфейс", e);
            
            // Если вообще ничего не удалось загрузить, перезапускаем приложение
            new Handler(Looper.getMainLooper()).postDelayed(this::restartApp, 3000);
        }
    }
    
    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_profile) {
            // Открыть экран профиля (будет реализовано позже)
            Toast.makeText(this, "Профиль", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        firebaseRepository.logout();
        Toast.makeText(this, R.string.logout, Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
    
    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "Изменение конфигурации обработано без пересоздания активности");
        
        // Сбрасываем счетчик инициализации
        initCounter = 0;
        
        // Если необходимо, можно обновить некоторые элементы UI
        // без перезагрузки всей активности
    }
}