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
        if (viewModel.isUserLoggedIn()) {
            navigateToHome();
            return;
        }
        setupListeners();
        observeLoginState();
    }
    private void setupListeners() {
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.email.getText().toString().trim();
            String password = binding.password.getText().toString().trim();
            if (validateInput(email, password)) {
                viewModel.login(email, password);
            }
        });
        binding.registerLink.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment);
        });
    }
    private boolean validateInput(String email, String password) {
        boolean isValid = true;
        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError(getString(R.string.invalid_email));
            isValid = false;
        } else {
            binding.emailLayout.setError(null);
        }
        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.setError(getString(R.string.password_too_short));
            isValid = false;
        } else {
            binding.passwordLayout.setError(null);
        }
        return isValid;
    }
    private void observeLoginState() {
        viewModel.getLoginResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.loginButton.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.loginButton.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.login_success, Toast.LENGTH_SHORT).show();
                    startMainActivity();
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.loginButton.setEnabled(true);
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }
    private void navigateToHome() {
        startMainActivity();
    }
    private void startMainActivity() {
        try {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), com.xdearboy.bookkeeper.MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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