package com.xdearboy.bookkeeper.ui.auth;
import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.repository.UserRepository;
import com.xdearboy.bookkeeper.util.Resource;
public class AuthViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Resource<User>> loginResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<User>> registerResult = new MutableLiveData<>();
    public AuthViewModel(@NonNull Application application) {
        super(application);
        userRepository = UserRepository.getInstance(application);
    }
    public void login(String email, String password) {
        loginResult.setValue(Resource.loading(null));
        new Thread(() -> {
            boolean success = userRepository.login(email, password);
            if (!success) {
                loginResult.postValue(Resource.error("Неверный email или пароль", null));
                return;
            }
            User user = userRepository.getCurrentUser();
            if (user != null) {
                loginResult.postValue(Resource.success(user));
            } else {
                loginResult.postValue(Resource.error("Ошибка сессии, попробуйте ещё раз", null));
            }
        }).start();
    }
    public void register(String name, String email, String password) {
        Log.d("AuthViewModel", "register() called with name=" + name + ", email=" + email);
        registerResult.setValue(Resource.loading(null));
        new Thread(() -> {
            Log.d("AuthViewModel", "Background thread started for registration");
            boolean success = userRepository.register(name, email, password);
            Log.d("AuthViewModel", "userRepository.register returned: " + success);
            if (!success) {
                Log.d("AuthViewModel", "Registration failed: user already exists");
                registerResult.postValue(Resource.error("Пользователь с таким email уже существует", null));
                return;
            }
            User user = userRepository.getCurrentUser();
            Log.d("AuthViewModel",
                    "userRepository.getCurrentUser returned: " + (user != null ? user.getEmail() : "null"));
            if (user != null) {
                registerResult.postValue(Resource.success(user));
            } else {
                Log.d("AuthViewModel", "Registration failed: session creation error");
                registerResult.postValue(Resource.error("Ошибка создания сессии, попробуйте ещё раз", null));
            }
        }).start();
    }
    public void logout() {
        userRepository.logout();
    }
    public boolean isUserLoggedIn() {
        return userRepository.isLoggedIn();
    }
    public LiveData<Resource<User>> getLoginResult() {
        return loginResult;
    }
    public LiveData<Resource<User>> getRegisterResult() {
        return registerResult;
    }
    public LiveData<User> getCurrentUser() {
        MutableLiveData<User> userLiveData = new MutableLiveData<>();
        new Thread(() -> {
            userLiveData.postValue(userRepository.getCurrentUser());
        }).start();
        return userLiveData;
    }
}