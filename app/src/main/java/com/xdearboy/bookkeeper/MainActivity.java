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
import com.xdearboy.bookkeeper.util.SessionManager;
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseRepository firebaseRepository;
    private NavController navController;
    private SessionManager sessionManager;
    private static boolean isFirstInit = true;
    private boolean isInitialized = false;
    private static int initCounter = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
        setupToolbar();
        sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }
        setupBottomNavigation();
    }
    @Override
    protected void onStart() {
        super.onStart();
        initCounter = 0;
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager != null && !sessionManager.isLoggedIn()) {
            Log.w(TAG, "Пользователь не авторизован (при возобновлении), переходим на LoginActivity");
            navigateToLogin();
        }
    }
    private void setupToolbar() {
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
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
                AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                        .build();
                if (getSupportActionBar() != null) {
                    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
                } else {
                    Log.w(TAG, "ActionBar не найден, пропускаем setupActionBarWithNavController");
                }
                NavigationUI.setupWithNavController(binding.navView, navController);
                Log.d(TAG, "Навигация успешно настроена");
            } else {
                Log.e(TAG, "NavHostFragment не найден!");
                showHomeFragmentDirectly();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при настройке навигации", e);
            showHomeFragmentDirectly();
        }
    }
    private void showHomeFragmentDirectly() {
        try {
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
            setContentView(R.layout.activity_fallback);
        } catch (Exception e) {
            Log.e(TAG, "Критическая ошибка: не удалось показать запасной интерфейс", e);
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
        sessionManager.logout();
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
        initCounter = 0;
    }
}