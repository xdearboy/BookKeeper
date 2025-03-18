package com.xdearboy.bookkeeper.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.database.dao.UserDao;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.util.PasswordUtils;
import com.xdearboy.bookkeeper.util.SessionManager;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с пользователями
 */
public class UserRepository {
    
    private final UserDao userDao;
    private final LiveData<List<User>> allUsers;
    private final SessionManager sessionManager;
    
    private static UserRepository instance;
    
    private UserRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        userDao = db.userDao();
        allUsers = userDao.getAllUsers();
        sessionManager = SessionManager.getInstance(application);
    }
    
    public static synchronized UserRepository getInstance(Application application) {
        if (instance == null) {
            instance = new UserRepository(application);
        }
        return instance;
    }
    
    /**
     * Возвращает всех пользователей
     * @return LiveData со списком всех пользователей
     */
    public LiveData<List<User>> getAllUsers() {
        return allUsers;
    }
    
    /**
     * Возвращает пользователя по ID
     * @param userId ID пользователя
     * @return LiveData с пользователем
     */
    public LiveData<User> getUserById(String userId) {
        return userDao.getUserById(userId);
    }
    
    /**
     * Возвращает текущего пользователя
     * @return Текущий пользователь или null, если пользователь не авторизован
     */
    public User getCurrentUser() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            return null;
        }
        
        try {
            return AppDatabase.databaseWriteExecutor.submit(() -> {
                LiveData<User> userLiveData = userDao.getUserById(userId);
                // Ждем, пока LiveData получит значение
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return userLiveData.getValue();
            }).get(); // Используем get() вместо join()
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Регистрирует нового пользователя
     * @param name Имя пользователя
     * @param email Email пользователя
     * @param password Пароль пользователя
     * @return true, если регистрация прошла успешно
     */
    public boolean register(String name, String email, String password) {
        try {
            // Проверяем, существует ли пользователь с таким email
            User existingUser = AppDatabase.databaseWriteExecutor.submit(() -> 
                    userDao.getUserByEmail(email)).get();
            
            if (existingUser != null) {
                return false;
            }
            
            // Создаем нового пользователя
            User user = new User();
            user.setId("user_" + UUID.randomUUID().toString());
            user.setName(name);
            user.setEmail(email);
            user.setPassword(PasswordUtils.hashPassword(password));
            user.setRegistrationDate(new Date());
            user.setLastLoginDate(new Date());
            user.setActive(true);
            
            // Сохраняем пользователя в базе данных
            AppDatabase.databaseWriteExecutor.execute(() -> userDao.insert(user));
            
            // Создаем сессию пользователя
            sessionManager.createSession(user.getId(), user.getName(), user.getEmail());
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Авторизует пользователя
     * @param email Email пользователя
     * @param password Пароль пользователя
     * @return true, если авторизация прошла успешно
     */
    public boolean login(String email, String password) {
        try {
            // Получаем пользователя по email
            User user = AppDatabase.databaseWriteExecutor.submit(() -> 
                    userDao.getUserByEmail(email)).get();
            
            if (user == null) {
                return false;
            }
            
            // Проверяем пароль
            if (!PasswordUtils.verifyPassword(password, user.getPassword())) {
                return false;
            }
            
            // Обновляем дату последнего входа
            Date now = new Date();
            AppDatabase.databaseWriteExecutor.execute(() -> 
                    userDao.updateLastLoginDate(user.getId(), now.getTime()));
            
            // Создаем сессию пользователя
            sessionManager.createSession(user.getId(), user.getName(), user.getEmail());
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Выход из системы
     */
    public void logout() {
        sessionManager.logout();
    }
    
    /**
     * Проверяет, авторизован ли пользователь
     * @return true, если пользователь авторизован
     */
    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }
    
    /**
     * Обновляет пользователя
     * @param user Пользователь для обновления
     */
    public void updateUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> userDao.update(user));
    }
    
    /**
     * Удаляет пользователя
     * @param userId ID пользователя для удаления
     */
    public void deleteUser(String userId) {
        AppDatabase.databaseWriteExecutor.execute(() -> userDao.deleteById(userId));
    }
    
    /**
     * Добавляет книгу в список заимствованных пользователем
     * @param userId ID пользователя
     * @param bookId ID книги
     */
    public void addBorrowedBook(String userId, String bookId) {
        User user = getCurrentUser();
        if (user != null) {
            user.addBorrowedBook(bookId);
            updateUser(user);
        }
    }
    
    /**
     * Удаляет книгу из списка заимствованных пользователем
     * @param userId ID пользователя
     * @param bookId ID книги
     */
    public void removeBorrowedBook(String userId, String bookId) {
        User user = getCurrentUser();
        if (user != null) {
            user.removeBorrowedBook(bookId);
            updateUser(user);
        }
    }
    
    /**
     * Получает пользователя по email
     * @param email Email пользователя
     * @return Пользователь или null, если пользователь не найден
     */
    public User getUserByEmail(String email) {
        try {
            return AppDatabase.databaseWriteExecutor.submit(() -> 
                    userDao.getUserByEmail(email)).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
} 