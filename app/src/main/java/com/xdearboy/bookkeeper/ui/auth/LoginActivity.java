package com.xdearboy.bookkeeper.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.xdearboy.bookkeeper.MainActivity;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.databinding.ActivityLoginBinding;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.util.Resource;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Наблюдаем за результатом входа
        viewModel.getLoginResult().observe(this, result -> {
            if (result.status == Resource.Status.LOADING) {
                // Показываем индикатор загрузки (если есть)
                // binding.progressBar.setVisibility(View.VISIBLE);
                binding.loginButton.setEnabled(false);
            } else if (result.status == Resource.Status.SUCCESS) {
                // Скрываем индикатор загрузки
                // binding.progressBar.setVisibility(View.GONE);
                binding.loginButton.setEnabled(true);
                
                User user = result.data;
                Log.d(TAG, "Успешный вход: " + user.getName());
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (result.status == Resource.Status.ERROR) {
                // Скрываем индикатор загрузки
                // binding.progressBar.setVisibility(View.GONE);
                binding.loginButton.setEnabled(true);
                
                String error = result.message;
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Ошибка входа: " + error);
            }
        });

        // Обработчик нажатия кнопки входа
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailInput.getText().toString().trim();
            String password = binding.passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                binding.emailLayout.setError(getString(R.string.error_email_empty));
                return;
            }

            if (TextUtils.isEmpty(password)) {
                binding.passwordLayout.setError(getString(R.string.error_password_empty));
                return;
            }

            viewModel.login(email, password);
        });

        // Обработчик нажатия на ссылку регистрации
        binding.registerLink.setOnClickListener(v -> {
            // Переходим на экран с фрагментом регистрации
            Intent intent = new Intent(this, AuthActivity.class);
            intent.putExtra("fragment", "register");
            startActivity(intent);
            finish();
        });
    }
} 