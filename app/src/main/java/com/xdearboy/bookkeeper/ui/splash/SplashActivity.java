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
import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.model.User;
import java.util.List;
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DELAY = 1000; // 1 секунда
    private FirebaseRepository firebaseRepository;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        boolean isFirstRun = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("is_first_run", true);
        if (isFirstRun) {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("your_prefs", MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("session_prefs", MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("user_session", MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("SessionManager", MODE_PRIVATE).edit().clear().apply();
            AppDatabase db = AppDatabase.getInstance(this);
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.clearAllTables();
                getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("is_first_run", false).apply();
                new Handler(Looper.getMainLooper()).postDelayed(this::checkLoginStatus, SPLASH_DELAY);
            });
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(this::checkLoginStatus, SPLASH_DELAY);
        }
        firebaseRepository = FirebaseRepository.getInstance();
    }
    private void checkLoginStatus() {
        AppDatabase db = AppDatabase.getInstance(this);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int userCount = db.userDao().getUserCount();
            SessionManager sessionManager = SessionManager.getInstance(this);
            Log.d("SplashActivity", "userCount = " + userCount);
            Log.d("SplashActivity", "sessionManager.isLoggedIn() = " + sessionManager.isLoggedIn());
            runOnUiThread(() -> {
                if (userCount == 0) {
                    startActivity(new Intent(this, com.xdearboy.bookkeeper.ui.auth.AuthActivity.class).putExtra("fragment", "register"));
                } else {
                     // Обычная логика (авторизация)
                    if (sessionManager.isLoggedIn()) {
                        startActivity(new Intent(this, com.xdearboy.bookkeeper.MainActivity.class));
                    } else {
                        startActivity(new Intent(this, com.xdearboy.bookkeeper.ui.auth.LoginActivity.class));
                    }
                }
                finish();
            });
        });
    }
} 