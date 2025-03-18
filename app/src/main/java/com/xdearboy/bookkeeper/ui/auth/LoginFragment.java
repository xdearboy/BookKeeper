package com.xdearboy.bookkeeper.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.databinding.FragmentLoginBinding;
import com.xdearboy.bookkeeper.util.Resource;

/**
 * Фрагмент для входа в систему
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        
        // Если пользователь уже авторизован, переходим на главный экран
        if (viewModel.isUserLoggedIn()) {
            navigateToHome();
            return;
        }
        
        // Настраиваем слушатели
        setupListeners();
        
        // Наблюдаем за состоянием входа
        observeLoginState();
    }
    
    /**
     * Настраивает слушатели для кнопок и полей ввода
     */
    private void setupListeners() {
        // Кнопка входа
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.email.getText().toString().trim();
            String password = binding.password.getText().toString().trim();
            
            // Проверяем введенные данные
            if (validateInput(email, password)) {
                // Выполняем вход
                viewModel.login(email, password);
            }
        });
        
        // Ссылка на регистрацию
        binding.registerLink.setOnClickListener(v -> {
            // Переходим на экран регистрации
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment);
        });
    }
    
    /**
     * Проверяет введенные данные
     * @param email Email пользователя
     * @param password Пароль пользователя
     * @return true, если данные валидны
     */
    private boolean validateInput(String email, String password) {
        boolean isValid = true;
        
        // Проверяем email
        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError(getString(R.string.invalid_email));
            isValid = false;
        } else {
            binding.emailLayout.setError(null);
        }
        
        // Проверяем пароль
        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.setError(getString(R.string.password_too_short));
            isValid = false;
        } else {
            binding.passwordLayout.setError(null);
        }
        
        return isValid;
    }
    
    /**
     * Наблюдает за состоянием входа
     */
    private void observeLoginState() {
        viewModel.getLoginResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            
            // Обрабатываем состояние входа
            switch (result.status) {
                case LOADING:
                    // Показываем индикатор загрузки
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.loginButton.setEnabled(false);
                    break;
                    
                case SUCCESS:
                    // Скрываем индикатор загрузки
                    binding.progressBar.setVisibility(View.GONE);
                    binding.loginButton.setEnabled(true);
                    
                    // Показываем сообщение об успешном входе
                    Toast.makeText(requireContext(), R.string.login_success, Toast.LENGTH_SHORT).show();
                    
                    // Переходим на главный экран
                    startMainActivity();
                    break;
                    
                case ERROR:
                    // Скрываем индикатор загрузки
                    binding.progressBar.setVisibility(View.GONE);
                    binding.loginButton.setEnabled(true);
                    
                    // Показываем сообщение об ошибке
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }
    
    /**
     * Переходит на главный экран
     */
    private void navigateToHome() {
        startMainActivity();
    }
    
    /**
     * Стартует MainActivity и закрывает текущую активность
     */
    private void startMainActivity() {
        try {
            // Проверяем контекст
            if (getActivity() != null) {
                // Создаем интент для запуска MainActivity
                Intent intent = new Intent(getActivity(), com.xdearboy.bookkeeper.MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                
                // Закрываем текущую активность
                getActivity().finish();
            }
        } catch (Exception e) {
            android.util.Log.e("LoginFragment", "Ошибка при запуске MainActivity", e);
            Toast.makeText(requireContext(), "Ошибка навигации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 