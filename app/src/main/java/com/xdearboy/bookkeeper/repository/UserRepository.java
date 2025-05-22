package com.xdearboy.bookkeeper.repository;
import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.database.dao.UserDao;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.util.PasswordUtils;
import com.xdearboy.bookkeeper.util.SessionManager;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
    public LiveData<List<User>> getAllUsers() {
        return allUsers;
    }
    public LiveData<User> getUserById(String userId) {
        return userDao.getUserById(userId);
    }
    public User getCurrentUser() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            return null;
        }
        try {
            return AppDatabase.databaseWriteExecutor.submit(() -> userDao.getUserByIdSync(userId)).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public boolean register(String name, String email, String password) {
        Log.d("UserRepository", "register() called with name=" + name + ", email=" + email);
        try {
            String normalizedEmail = email.trim().toLowerCase();
            Log.d("UserRepository", "Попытка регистрации: name=" + name + ", email=" + normalizedEmail);
            User existingUser = AppDatabase.databaseWriteExecutor.submit(() -> userDao.getUserByEmail(normalizedEmail))
                    .get();
            Log.d("UserRepository",
                    "Результат поиска пользователя по email: " + (existingUser != null ? "НАЙДЕН" : "НЕ НАЙДЕН"));
            if (existingUser != null) {
                Log.d("UserRepository", "Пользователь с таким email уже существует: " + normalizedEmail);
                return false;
            }
            User user = new User();
            user.setId("user_" + UUID.randomUUID().toString());
            user.setName(name);
            user.setEmail(normalizedEmail);
            user.setPassword(PasswordUtils.hashPassword(password));
            user.setRegistrationDate(new Date());
            user.setLastLoginDate(new Date());
            user.setActive(true);
             // Сохраняем пользователя в базе данных (СИНХРОННО)
            AppDatabase.databaseWriteExecutor.submit(() -> userDao.insert(user)).get();
            Log.d("UserRepository", "Пользователь успешно создан: " + normalizedEmail);
            sessionManager.createSession(user.getId(), user.getName(), user.getEmail());
            Log.d("UserRepository", "Сессия создана для: " + user.getId());
            return true;
        } catch (Exception e) {
            Log.e("UserRepository", "Ошибка при регистрации", e);
            return false;
        }
    }
    public boolean login(String email, String password) {
        try {
            String normalizedEmail = email.trim().toLowerCase();
            User user = AppDatabase.databaseWriteExecutor.submit(() -> userDao.getUserByEmail(normalizedEmail)).get();
            if (user == null) {
                return false;
            }
            if (!PasswordUtils.verifyPassword(password, user.getPassword())) {
                return false;
            }
            Date now = new Date();
            AppDatabase.databaseWriteExecutor.execute(() -> userDao.updateLastLoginDate(user.getId(), now.getTime()));
            sessionManager.createSession(user.getId(), user.getName(), user.getEmail());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public void logout() {
        sessionManager.logout();
    }
    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }
    public void updateUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> userDao.update(user));
    }
    public void deleteUser(String userId) {
        AppDatabase.databaseWriteExecutor.execute(() -> userDao.deleteById(userId));
    }
    public void addBorrowedBook(String userId, String bookId) {
        User user = getCurrentUser();
        if (user != null) {
            user.addBorrowedBook(bookId);
            updateUser(user);
        }
    }
    public void removeBorrowedBook(String userId, String bookId) {
        User user = getCurrentUser();
        if (user != null) {
            user.removeBorrowedBook(bookId);
            updateUser(user);
        }
    }
    public User getUserByEmail(String email) {
        try {
            return AppDatabase.databaseWriteExecutor.submit(() -> userDao.getUserByEmail(email)).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}