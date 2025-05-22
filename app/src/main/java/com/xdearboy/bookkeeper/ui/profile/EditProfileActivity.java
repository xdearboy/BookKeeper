package com.xdearboy.bookkeeper.ui.profile;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.xdearboy.bookkeeper.R;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.repository.UserRepository;
public class EditProfileActivity extends AppCompatActivity {
    private EditText nameEditText, emailEditText, passwordEditText;
    private Button saveButton;
    private UserRepository userRepository;
    private User currentUser;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        nameEditText = findViewById(R.id.edit_profile_name);
        emailEditText = findViewById(R.id.edit_profile_email);
        passwordEditText = findViewById(R.id.edit_profile_password);
        saveButton = findViewById(R.id.edit_profile_save_button);
        userRepository = UserRepository.getInstance(getApplication());
        // TODO: Replace with actual user id from session/auth
        String userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            Toast.makeText(this, "User ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userRepository.getUserById(userId).observe(this, user -> {
            if (user != null) {
                currentUser = user;
                nameEditText.setText(user.getName());
                emailEditText.setText(user.getEmail());
                // Password usually not shown for security reasons
            }
        });
        saveButton.setOnClickListener(v -> saveProfile());
    }
    private void saveProfile() {
        String newName = nameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();
        String newPassword = passwordEditText.getText().toString();
        if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newEmail)) {
            Toast.makeText(this, "Имя и Email не могут быть пустыми", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
            return;
        }
        currentUser.setName(newName);
        currentUser.setEmail(newEmail);
        if (!TextUtils.isEmpty(newPassword)) {
            currentUser.setPassword(newPassword); // В реальном приложении нужно хешировать!
        }
        userRepository.updateUser(currentUser);
        Toast.makeText(this, "Профиль обновлен", Toast.LENGTH_SHORT).show();
        finish();
    }
}