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
public class RegisterFragment extends Fragment {
    private FragmentRegisterBinding binding;
    private AuthViewModel viewModel;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        setupListeners();
        observeRegisterState();
    }
    private void setupListeners() {
        binding.registerButton.setOnClickListener(v -> {
            android.util.Log.d("RegisterFragment", "Register button clicked");
            String name = binding.name.getText().toString().trim();
            String email = binding.email.getText().toString().trim();
            String password = binding.password.getText().toString().trim();
            String confirmPassword = binding.confirmPassword.getText().toString().trim();
            if (validateInput(name, email, password, confirmPassword)) {
                android.util.Log.d("RegisterFragment",
                        "Calling viewModel.register with name=" + name + ", email=" + email);
                viewModel.register(name, email, password);
                android.util.Log.d("RegisterFragment", "Called viewModel.register");
            } else {
                android.util.Log.d("RegisterFragment", "Validation failed");
            }
        });
        binding.loginLink.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_registerFragment_to_loginFragment);
        });
    }
    private boolean validateInput(String name, String email, String password, String confirmPassword) {
        boolean isValid = true;
        if (TextUtils.isEmpty(name)) {
            binding.nameLayout.setError(getString(R.string.name) + " " + getString(R.string.required));
            isValid = false;
        } else {
            binding.nameLayout.setError(null);
        }
        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError(getString(R.string.invalid_email));
            isValid = false;
        } else {
            binding.emailLayout.setError(null);
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.passwordLayout.setError(getString(R.string.password_too_short));
            isValid = false;
        } else {
            binding.passwordLayout.setError(null);
        }
        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordLayout.setError(getString(R.string.passwords_not_match));
            isValid = false;
        } else {
            binding.confirmPasswordLayout.setError(null);
        }
        return isValid;
    }
    private void observeRegisterState() {
        viewModel.getRegisterResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null)
                return;
            switch (result.status) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.registerButton.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.registerButton.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.register_success, Toast.LENGTH_SHORT).show();
                    startMainActivity();
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.registerButton.setEnabled(true);
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
                android.content.Intent intent = new android.content.Intent(getActivity(),
                        com.xdearboy.bookkeeper.MainActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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