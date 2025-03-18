package com.xdearboy.bookkeeper.ui.auth;

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
import com.xdearboy.bookkeeper.databinding.FragmentRegisterBinding;
import com.xdearboy.bookkeeper.util.Resource;

/**
 * Фрагмент для регистрации в системе
 */
public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        
        // Настраиваем слушатели
        setupListeners();
        
        // Наблюдаем за состоянием регистрации
        observeRegisterState();
    }
    
    /**
     * Настраивает слушатели для кнопок и полей ввода
     */
    private void setupListeners() {
        // Кнопка регистрации
        binding.registerButton.setOnClickListener(v -> {
            String name = binding.name.getText().toString().trim();
            String email = binding.email.getText().toString().trim();
            String password = binding.password.getText().toString().trim();
            String confirmPassword = binding.confirmPassword.getText().toString().trim();
            
            // Проверяем введенные данные
            if (validateInput(name, email, password, confirmPassword)) {
                // Выполняем регистрацию
                viewModel.register(email, password, name);
            }
        });
        
        // Ссылка на вход
        binding.loginLink.setOnClickListener(v -> {
            // Переходим на экран входа
            Navigation.findNavController(v).navigate(R.id.action_registerFragment_to_loginFragment);
        });
    }
    
    /**
     * Проверяет введенные данные
     * @param name Имя пользователя
     * @param email Email пользователя
     * @param password Пароль пользователя
     * @param confirmPassword Подтверждение пароля
     * @return true, если данные валидны
     */
    private boolean validateInput(String name, String email, String password, String confirmPassword) {
        boolean isValid = true;
        
        // Проверяем имя
        if (TextUtils.isEmpty(name)) {
            binding.nameLayout.setError(getString(R.string.name) + " " + getString(R.string.required));
            isValid = false;
        } else {
            binding.nameLayout.setError(null);
        }
        
        // Проверяем email
        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError(getString(R.string.invalid_email));
            isValid = false;
        } else {
            binding.emailLayout.setError(null);
        }
        
        // Проверяем пароль
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.passwordLayout.setError(getString(R.string.password_too_short));
            isValid = false;
        } else {
            binding.passwordLayout.setError(null);
        }
        
        // Проверяем подтверждение пароля
        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordLayout.setError(getString(R.string.passwords_not_match));
            isValid = false;
        } else {
            binding.confirmPasswordLayout.setError(null);
        }
        
        return isValid;
    }
    
    /**
     * Наблюдает за состоянием регистрации
     */
    private void observeRegisterState() {
        viewModel.getRegisterResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            
            // Обрабатываем состояние регистрации
            switch (result.status) {
                case LOADING:
                    // Показываем индикатор загрузки
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.registerButton.setEnabled(false);
                    break;
                    
                case SUCCESS:
                    // Скрываем индикатор загрузки
                    binding.progressBar.setVisibility(View.GONE);
                    binding.registerButton.setEnabled(true);
                    
                    // Показываем сообщение об успешной регистрации
                    Toast.makeText(requireContext(), R.string.register_success, Toast.LENGTH_SHORT).show();
                    
                    // Переходим на главный экран
                    startMainActivity();
                    break;
                    
                case ERROR:
                    // Скрываем индикатор загрузки
                    binding.progressBar.setVisibility(View.GONE);
                    binding.registerButton.setEnabled(true);
                    
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
                android.content.Intent intent = new android.content.Intent(getActivity(), com.xdearboy.bookkeeper.MainActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                
                // Закрываем текущую активность
                getActivity().finish();
            }
        } catch (Exception e) {
            android.util.Log.e("RegisterFragment", "Ошибка при запуске MainActivity", e);
            Toast.makeText(requireContext(), "Ошибка навигации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 