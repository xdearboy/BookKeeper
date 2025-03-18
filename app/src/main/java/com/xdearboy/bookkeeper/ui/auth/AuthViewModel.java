package com.xdearboy.bookkeeper.ui.auth;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.repository.BookRepository;
import com.xdearboy.bookkeeper.repository.FirebaseRepository;
import com.xdearboy.bookkeeper.repository.UserRepository;
import com.xdearboy.bookkeeper.util.Resource;

/**
 * ViewModel для аутентификации
 */
public class AuthViewModel extends AndroidViewModel {

    private final FirebaseRepository firebaseRepository;
    private final BookRepository bookRepository;
    private final MutableLiveData<Resource<User>> loginResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<User>> registerResult = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        firebaseRepository = FirebaseRepository.getInstance();
        bookRepository = BookRepository.getInstance(application);
    }

    /**
     * Выполняет вход пользователя
     * @param email Email пользователя
     * @param password Пароль пользователя
     */
    public void login(String email, String password) {
        loginResult.setValue(Resource.loading(null));
        
        firebaseRepository.login(email, password).observeForever(result -> {
            loginResult.setValue(result);
            
            // Если вход успешен, синхронизируем книги
            if (result.status == Resource.Status.SUCCESS) {
                syncBooks();
            }
        });
    }

    /**
     * Выполняет регистрацию пользователя
     * @param email Email пользователя
     * @param password Пароль пользователя
     * @param name Имя пользователя
     */
    public void register(String email, String password, String name) {
        registerResult.setValue(Resource.loading(null));
        Log.d("AuthViewModel", "Начинаем процесс регистрации через Firebase для: " + email);
        
        // Регистрируем пользователя через Firebase
        firebaseRepository.register(email, password, name).observeForever(result -> {
            registerResult.setValue(result);
            
            // Если регистрация успешна, синхронизируем книги
            if (result.status == Resource.Status.SUCCESS) {
                Log.d("AuthViewModel", "Регистрация через Firebase успешна");
                syncBooks();
            } else {
                Log.e("AuthViewModel", "Ошибка при регистрации через Firebase: " + 
                    (result.message != null ? result.message : "неизвестная ошибка"));
            }
        });
    }

    /**
     * Выполняет выход пользователя
     */
    public void logout() {
        firebaseRepository.logout();
    }

    /**
     * Проверяет, авторизован ли пользователь
     * @return true, если пользователь авторизован
     */
    public boolean isUserLoggedIn() {
        return firebaseRepository.isUserLoggedIn();
    }

    /**
     * Возвращает результат входа
     * @return LiveData с результатом входа
     */
    public LiveData<Resource<User>> getLoginResult() {
        return loginResult;
    }

    /**
     * Возвращает результат регистрации
     * @return LiveData с результатом регистрации
     */
    public LiveData<Resource<User>> getRegisterResult() {
        return registerResult;
    }

    /**
     * Возвращает текущего пользователя
     * @return LiveData с текущим пользователем
     */
    public LiveData<User> getCurrentUser() {
        return firebaseRepository.getCurrentUser();
    }

    /**
     * Синхронизирует книги с Firebase
     */
    private void syncBooks() {
        bookRepository.syncWithFirebase();
    }
} 