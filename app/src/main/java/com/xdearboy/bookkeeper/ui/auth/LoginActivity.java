package com.xdearboy.bookkeeper.ui.auth;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
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
        viewModel.getLoginResult().observe(this, result -> {
            if (result.status == Resource.Status.LOADING) {
                binding.loginButton.setEnabled(false);
            } else if (result.status == Resource.Status.SUCCESS) {
                binding.loginButton.setEnabled(true);
                User user = result.data;
                Log.d(TAG, "Успешный вход: " + user.getName());
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else if (result.status == Resource.Status.ERROR) {
                binding.loginButton.setEnabled(true);
                String error = result.message;
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Ошибка входа: " + error);
            }
        });

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

        binding.registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, AuthActivity.class);
            intent.putExtra("fragment", "register");
            startActivity(intent);
            finish();
        });
    }
}