package com.xdearboy.bookkeeper.ui.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.xdearboy.bookkeeper.MainActivity;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.repository.FirebaseRepository;
import com.xdearboy.bookkeeper.ui.auth.LoginActivity;
import com.xdearboy.bookkeeper.ui.onboarding.OnboardingActivity;
import com.xdearboy.bookkeeper.util.SessionManager;

/**
 * Активность-заставка
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    
    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DELAY = 1000; // 1 секунда
    
    private FirebaseRepository firebaseRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Инициализация репозитория
        firebaseRepository = FirebaseRepository.getInstance();
        
        // Задержка для показа заставки
        new Handler(Looper.getMainLooper()).postDelayed(this::checkLoginStatus, SPLASH_DELAY);
    }
    
    /**
     * Проверяет статус авторизации и перенаправляет на соответствующий экран
     */
    private void checkLoginStatus() {
        boolean isFirstLaunch = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("is_first_launch", true);
        
        // Получаем SessionManager для проверки локальной сессии
        SessionManager sessionManager = SessionManager.getInstance(this);
        
        if (isFirstLaunch) {
            // Первый запуск приложения, показываем онбординг
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_first_launch", false)
                    .apply();
            
            startActivity(new Intent(this, OnboardingActivity.class));
        } else if (firebaseRepository.isUserLoggedIn() || sessionManager.isLoggedIn()) {
            // Пользователь авторизован через Firebase или локальную сессию
            if (firebaseRepository.isUserLoggedIn()) {
                Log.d(TAG, "Пользователь авторизован через Firebase, переход на главный экран");
            } else {
                Log.d(TAG, "Пользователь авторизован через локальную сессию, переход на главный экран");
            }
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // Пользователь не авторизован, переходим на экран входа
            Log.d(TAG, "Пользователь не авторизован, переход на экран входа");
            startActivity(new Intent(this, LoginActivity.class));
        }
        
        finish();
    }
} 