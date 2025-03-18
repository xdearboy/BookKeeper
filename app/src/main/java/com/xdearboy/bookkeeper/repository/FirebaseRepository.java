package com.xdearboy.bookkeeper.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.util.Resource;
import com.xdearboy.bookkeeper.util.SessionManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с Firebase (аутентификация, Firestore, Storage)
 */
public class FirebaseRepository {
    private static final String TAG = "FirebaseRepository";
    
    private static FirebaseRepository instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final MutableLiveData<User> currentUser;
    private final MutableLiveData<Boolean> isLoading;
    private SessionManager sessionManager;
    private Context context;
    private boolean isGooglePlayServicesAvailable = true;
    
    // Переменные для защиты от многократных вызовов
    private static final long AUTH_CHECK_INTERVAL = 2000; // 2 секунды
    private long lastAuthCheckTime = 0;
    private boolean lastAuthCheckResult = false;
    
    private FirebaseRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
        
        // Отключаем reCAPTCHA для тестирования и разработки
        try {
            auth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при отключении reCAPTCHA", e);
        }
        
        // Проверяем, авторизован ли пользователь
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser != null) {
            fetchUserData(fbUser.getUid());
        } else {
            currentUser.postValue(null);
        }
    }
    
    /**
     * Возвращает экземпляр репозитория
     * @return экземпляр репозитория
     */
    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseRepository();
        }
        return instance;
    }
    
    /**
     * Инициализация с контекстом для доступа к SessionManager
     * Должен быть вызван в Application или Activity
     */
    public void init(Context context) {
        this.context = context.getApplicationContext();
        sessionManager = SessionManager.getInstance(this.context);
        
        // Если пользователь был залогинен ранее через SessionManager,
        // но Firebase сессия истекла, пробуем восстановить
        if (sessionManager != null && sessionManager.isLoggedIn() && auth.getCurrentUser() == null) {
            String email = sessionManager.getUserEmail();
            // В этом случае можно показать UI для повторного входа или использовать token
            Log.d(TAG, "Обнаружена предыдущая сессия пользователя: " + email);
        }
    }
    
    /**
     * Получение данных пользователя из Firestore
     * @param userId ID пользователя
     */
    private void fetchUserData(String userId) {
        isLoading.postValue(true);
        
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    currentUser.postValue(user);
                    Log.d(TAG, "Данные пользователя получены: " + user.getName());
                } else {
                    // Создаем нового пользователя, если данных нет
                    createNewUser(userId);
                }
                isLoading.postValue(false);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Ошибка при получении данных пользователя", e);
                isLoading.postValue(false);
            });
    }
    
    /**
     * Создание нового пользователя в Firestore
     * @param userId ID пользователя
     */
    private void createNewUser(String userId) {
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser != null) {
            User newUser = new User();
            newUser.setId(userId);
            newUser.setEmail(fbUser.getEmail());
            newUser.setName(fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "Пользователь");
            newUser.setCreatedAt(new Date());
            newUser.setLastLoginAt(new Date());
            
            db.collection("users").document(userId)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    currentUser.postValue(newUser);
                    Log.d(TAG, "Новый пользователь создан: " + newUser.getName());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка при создании пользователя", e);
                });
        }
    }
    
    /**
     * Устанавливает статус доступности Google Play Services
     * @param available true, если Google Play Services доступны
     */
    public void setGooglePlayServicesAvailable(boolean available) {
        this.isGooglePlayServicesAvailable = available;
        Log.d(TAG, "Google Play Services доступность: " + available);
    }
    
    /**
     * Проверяет доступность Google Play Services
     * @return true, если Google Play Services доступны
     */
    public boolean isGooglePlayServicesAvailable() {
        return isGooglePlayServicesAvailable;
    }
    
    /**
     * Регистрация нового пользователя
     * @param email Email пользователя
     * @param password Пароль пользователя
     * @param name Имя пользователя
     * @return LiveData с результатом операции
     */
    public LiveData<Resource<User>> register(String email, String password, String name) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        Log.d(TAG, "Начало регистрации пользователя: " + email);
        
        try {
            // Проверяем, инициализирован ли Firebase
            if (auth == null || db == null) {
                Log.e(TAG, "Firebase не инициализирован");
                result.setValue(Resource.error("Ошибка инициализации Firebase", null));
                return result;
            }
            
            // Если Google Play Services недоступны, используем локальную аутентификацию
            if (!isGooglePlayServicesAvailable) {
                Log.w(TAG, "Google Play Services недоступны, используем локальную аутентификацию");
                // Создаем нового пользователя локально
                User newUser = new User();
                newUser.setId("local_" + UUID.randomUUID().toString());
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setCreatedAt(new Date());
                newUser.setLastLoginAt(new Date());
                
                // Сохраняем в SessionManager
                if (sessionManager != null) {
                    sessionManager.createSession(newUser.getId(), newUser.getName(), newUser.getEmail());
                }
                
                // Возвращаем результат
                result.setValue(Resource.success(newUser));
                currentUser.postValue(newUser);
                return result;
            }
            
            // Прямой вызов Firebase Auth для создания пользователя
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Аутентификация Firebase успешна, обновляем профиль");
                    FirebaseUser fbUser = authResult.getUser();
                    if (fbUser != null) {
                        // Обновляем профиль пользователя
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                        
                        fbUser.updateProfile(profileUpdates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Профиль обновлен, создаем запись в Firestore");
                                // Создаем запись в Firestore
                                User newUser = new User(fbUser.getUid(), email, name);
                                newUser.setCreatedAt(new Date());
                                newUser.setLastLoginAt(new Date());
                                
                                // Проверяем права доступа перед записью
                                testFirestorePermissions(fbUser.getUid(), () -> {
                                    // Если права есть, создаем профиль
                                    db.collection("users").document(fbUser.getUid())
                                        .set(newUser)
                                        .addOnSuccessListener(firestoreVoid -> {
                                            Log.d(TAG, "Пользователь успешно зарегистрирован: " + newUser.getName());
                                            result.setValue(Resource.success(newUser));
                                            
                                            // Сохраняем в SessionManager
                                            if (sessionManager != null) {
                                                sessionManager.createSession(newUser.getId(), newUser.getName(), newUser.getEmail());
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Ошибка при создании профиля в Firestore", e);
                                            // Если ошибка в Firestore, но пользователь в Firebase создан,
                                            // все равно считаем регистрацию успешной
                                            result.setValue(Resource.success(newUser));
                                            
                                            // Сохраняем в SessionManager
                                            if (sessionManager != null) {
                                                sessionManager.createSession(newUser.getId(), newUser.getName(), newUser.getEmail());
                                            }
                                            
                                            String errorMsg = getDetailedErrorMessage(e);
                                            Log.w(TAG, "Предупреждение: " + errorMsg + " (но пользователь создан)");
                                        });
                                }, error -> {
                                    // Если прав нет, все равно возвращаем успех с созданным пользователем
                                    Log.e(TAG, "Недостаточно прав для создания профиля в Firestore", error);
                                    
                                    // Создаем пользователя без Firestore
                                    User localUser = new User(fbUser.getUid(), email, name);
                                    localUser.setCreatedAt(new Date());
                                    localUser.setLastLoginAt(new Date());
                                    
                                    result.setValue(Resource.success(localUser));
                                    currentUser.postValue(localUser);
                                    
                                    // Сохраняем в SessionManager
                                    if (sessionManager != null) {
                                        sessionManager.createSession(localUser.getId(), localUser.getName(), localUser.getEmail());
                                    }
                                    
                                    String errorMsg = getDetailedErrorMessage(error);
                                    Log.w(TAG, "Предупреждение: " + errorMsg + " (но пользователь создан)");
                                });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Ошибка при обновлении профиля", e);
                                // Даже если не удалось обновить профиль, считаем регистрацию успешной
                                User basicUser = new User(fbUser.getUid(), email, "Пользователь");
                                result.setValue(Resource.success(basicUser));
                                currentUser.postValue(basicUser);
                                
                                // Сохраняем в SessionManager
                                if (sessionManager != null) {
                                    sessionManager.createSession(basicUser.getId(), basicUser.getName(), basicUser.getEmail());
                                }
                                
                                String errorMsg = getDetailedErrorMessage(e);
                                Log.w(TAG, "Предупреждение: " + errorMsg + " (но базовый пользователь создан)");
                            });
                    } else {
                        Log.e(TAG, "Пользователь Firebase null после успешной регистрации");
                        result.setValue(Resource.error("Ошибка при получении данных пользователя", null));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка при регистрации пользователя", e);
                    String errorMsg = getDetailedErrorMessage(e);
                    result.setValue(Resource.error("Ошибка при регистрации: " + errorMsg, null));
                });
        } catch (Exception e) {
            Log.e(TAG, "Неожиданная ошибка при регистрации", e);
            String errorMsg = getDetailedErrorMessage(e);
            result.setValue(Resource.error("Неожиданная ошибка: " + errorMsg, null));
        }
        
        return result;
    }
    
    /**
     * Тестирует права доступа к Firestore для указанного пользователя
     * @param userId ID пользователя
     * @param onSuccess колбэк в случае успеха
     * @param onError колбэк в случае ошибки
     */
    private void testFirestorePermissions(String userId, Runnable onSuccess, com.google.android.gms.tasks.OnFailureListener onError) {
        // Если Google Play Services недоступны, сразу сообщаем об ошибке
        if (!isGooglePlayServicesAvailable) {
            onError.onFailure(new Exception("Google Play Services недоступны"));
            return;
        }

        // Устанавливаем таймаут для операции
        final boolean[] isCompleted = {false};
        
        // Запускаем таймер для отслеживания таймаута
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 5 секунд таймаут
                if (!isCompleted[0]) {
                    Log.e(TAG, "Таймаут при проверке прав доступа к Firestore");
                    onError.onFailure(new Exception("Таймаут при проверке Firestore"));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        try {
            // Пробуем прочитать свой профиль
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isCompleted[0] = true;
                    // Если документ существует, обновляем его
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Проверка прав доступа успешна: документ существует");
                        onSuccess.run();
                    } else {
                        // Если документа не существует, пробуем создать тестовый документ
                        Log.d(TAG, "Документа нет, проверяем права на запись");
                        db.collection("users").document(userId + "_test")
                            .set(new java.util.HashMap<String, Object>() {{
                                put("test", true);
                                put("timestamp", new Date());
                            }})
                            .addOnSuccessListener(aVoid -> {
                                // Если удалось создать тестовый документ, удаляем его
                                Log.d(TAG, "Тестовый документ создан, удаляем");
                                db.collection("users").document(userId + "_test")
                                    .delete()
                                    .addOnSuccessListener(aVoid2 -> {
                                        Log.d(TAG, "Тестовый документ удален, права доступа подтверждены");
                                        onSuccess.run();
                                    })
                                    .addOnFailureListener(onError);
                            })
                            .addOnFailureListener(onError);
                    }
                })
                .addOnFailureListener(e -> {
                    isCompleted[0] = true;
                    // Проверяем, не связана ли ошибка с отсутствием интернета
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                        com.google.firebase.firestore.FirebaseFirestoreException firestoreEx = 
                            (com.google.firebase.firestore.FirebaseFirestoreException) e;
                        
                        switch (firestoreEx.getCode()) {
                            case UNAVAILABLE:
                            case DEADLINE_EXCEEDED:
                                // Сетевая ошибка, но мы можем продолжить работу локально
                                Log.w(TAG, "Firestore недоступен из-за проблем с сетью: " + firestoreEx.getCode());
                                onSuccess.run();
                                break;
                            default:
                                onError.onFailure(e);
                                break;
                        }
                    } else {
                        onError.onFailure(e);
                    }
                });
        } catch (Exception e) {
            isCompleted[0] = true;
            Log.e(TAG, "Ошибка при проверке прав доступа к Firestore", e);
            onError.onFailure(e);
        }
    }
    
    /**
     * Получает детальное сообщение об ошибке
     * @param e исключение
     * @return детальное сообщение об ошибке
     */
    private String getDetailedErrorMessage(Exception e) {
        String errorMsg = e.getMessage();
        
        if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
            com.google.firebase.firestore.FirebaseFirestoreException firestoreEx = 
                (com.google.firebase.firestore.FirebaseFirestoreException) e;
            
            switch (firestoreEx.getCode()) {
                case PERMISSION_DENIED:
                    return "Недостаточно прав. Проверьте правила безопасности Firestore.";
                case UNAVAILABLE:
                    return "Сервис недоступен. Проверьте подключение к интернету.";
                case UNAUTHENTICATED:
                    return "Пользователь не аутентифицирован. Выполните повторный вход.";
                case NOT_FOUND:
                    return "Документ не найден в базе данных.";
                case ALREADY_EXISTS:
                    return "Документ уже существует.";
                default:
                    return errorMsg != null ? errorMsg : "Неизвестная ошибка Firestore.";
            }
        } else if (e instanceof com.google.firebase.auth.FirebaseAuthException) {
            com.google.firebase.auth.FirebaseAuthException authEx = 
                (com.google.firebase.auth.FirebaseAuthException) e;
            
            switch (authEx.getErrorCode()) {
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Этот email уже используется другим аккаунтом.";
                case "ERROR_WEAK_PASSWORD":
                    return "Пароль слишком слабый. Используйте не менее 6 символов.";
                case "ERROR_INVALID_EMAIL":
                    return "Некорректный формат email.";
                case "ERROR_USER_DISABLED":
                    return "Этот аккаунт был отключен.";
                case "ERROR_OPERATION_NOT_ALLOWED":
                    return "Эта операция недоступна. Проверьте настройки Firebase.";
                default:
                    return errorMsg != null ? errorMsg : "Неизвестная ошибка аутентификации.";
            }
        }
        
        return errorMsg != null ? errorMsg : "Неизвестная ошибка.";
    }
    
    /**
     * Вход пользователя
     * @param email Email пользователя
     * @param password Пароль пользователя
     * @return LiveData с результатом операции
     */
    public LiveData<Resource<User>> login(String email, String password) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        // Если Google Play Services недоступны, используем локальную аутентификацию
        if (!isGooglePlayServicesAvailable && sessionManager != null) {
            Log.w(TAG, "Google Play Services недоступны, пробуем войти по сохраненным данным");
            
            // Пробуем найти пользователя в SessionManager
            if (sessionManager.isLoggedIn() && email.equals(sessionManager.getUserEmail())) {
                User localUser = new User();
                localUser.setId(sessionManager.getUserId());
                localUser.setEmail(sessionManager.getUserEmail());
                localUser.setName(sessionManager.getUserName());
                localUser.setLastLoginAt(new Date());
                
                // Возвращаем результат
                result.setValue(Resource.success(localUser));
                currentUser.postValue(localUser);
                return result;
            } else {
                // Если в SessionManager другой пользователь или никого нет,
                // создаем нового локального пользователя
                User newUser = new User();
                newUser.setId("local_" + UUID.randomUUID().toString());
                newUser.setEmail(email);
                newUser.setName("Пользователь");
                newUser.setCreatedAt(new Date());
                newUser.setLastLoginAt(new Date());
                
                // Сохраняем в SessionManager
                sessionManager.createSession(newUser.getId(), newUser.getName(), newUser.getEmail());
                
                // Возвращаем результат
                result.setValue(Resource.success(newUser));
                currentUser.postValue(newUser);
                return result;
            }
        }
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser fbUser = auth.getCurrentUser();
                    if (fbUser != null) {
                        // Обновляем дату последнего входа
                        db.collection("users").document(fbUser.getUid())
                            .update("lastLoginAt", new Date());
                        
                        // Получаем данные пользователя
                        db.collection("users").document(fbUser.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                User user = documentSnapshot.toObject(User.class);
                                if (user != null) {
                                    // Сохраняем данные пользователя в SharedPreferences
                                    if (sessionManager != null) {
                                        sessionManager.createSession(user.getId(), user.getName(), user.getEmail());
                                        Log.d(TAG, "Сессия пользователя сохранена: " + user.getEmail());
                                    }
                                    
                                    result.setValue(Resource.success(user));
                                    Log.d(TAG, "Пользователь вошел: " + user.getName());
                                } else {
                                    // Если данных нет, создаем нового пользователя
                                    User newUser = new User(fbUser.getUid(), email, 
                                        fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "Пользователь");
                                    
                                    db.collection("users").document(fbUser.getUid())
                                        .set(newUser)
                                        .addOnSuccessListener(aVoid -> {
                                            // Сохраняем данные пользователя в SharedPreferences
                                            if (sessionManager != null) {
                                                sessionManager.createSession(newUser.getId(), newUser.getName(), newUser.getEmail());
                                                Log.d(TAG, "Сессия нового пользователя сохранена: " + newUser.getEmail());
                                            }
                                            
                                            result.setValue(Resource.success(newUser));
                                            Log.d(TAG, "Создан новый профиль при входе: " + newUser.getName());
                                        })
                                        .addOnFailureListener(e -> {
                                            result.setValue(Resource.error("Ошибка при создании профиля", null));
                                            Log.e(TAG, "Ошибка при создании профиля", e);
                                        });
                                }
                            })
                            .addOnFailureListener(e -> {
                                result.setValue(Resource.error("Ошибка при получении данных пользователя", null));
                                Log.e(TAG, "Ошибка при получении данных пользователя", e);
                            });
                    }
                } else {
                    result.setValue(Resource.error("Ошибка входа: " + 
                        (task.getException() != null ? task.getException().getMessage() : "Неизвестная ошибка"), null));
                    Log.e(TAG, "Ошибка входа", task.getException());
                }
            });
        
        return result;
    }
    
    /**
     * Выход пользователя
     */
    public void logout() {
        auth.signOut();
        
        // Удаляем данные сессии из SharedPreferences
        if (sessionManager != null) {
            sessionManager.logout();
            Log.d(TAG, "Сессия пользователя удалена");
        }
        
        Log.d(TAG, "Пользователь вышел");
    }
    
    /**
     * Получение текущего пользователя
     * @return LiveData с текущим пользователем
     */
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Проверка, авторизован ли пользователь
     * @return true, если пользователь авторизован
     */
    public boolean isUserLoggedIn() {
        try {
            // Защита от многократных вызовов (чтобы не спамить логи)
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAuthCheckTime < AUTH_CHECK_INTERVAL) {
                return lastAuthCheckResult;
            }
            lastAuthCheckTime = currentTime;
            
            // Проверяем сессию Firebase
            boolean isFirebaseLoggedIn = false;
            try {
                isFirebaseLoggedIn = auth.getCurrentUser() != null;
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при проверке Firebase авторизации: " + e.getMessage());
            }
            
            // Проверяем локальную сессию
            boolean isLocallyLoggedIn = false;
            if (sessionManager != null) {
                try {
                    isLocallyLoggedIn = sessionManager.isLoggedIn();
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при проверке локальной авторизации: " + e.getMessage());
                }
                
                // Если локальная сессия есть, но Firebase сессии нет, и Google Play Services доступны,
                // пробуем восстановить Firebase-сессию
                if (isLocallyLoggedIn && !isFirebaseLoggedIn && isGooglePlayServicesAvailable) {
                    Log.d(TAG, "Обнаружена локальная сессия, но нет Firebase-сессии. Запоминаем для следующего входа.");
                    
                    // Для автоматического входа нужно заново авторизоваться через Firebase,
                    // здесь мы просто запоминаем, что сессия была
                    String email = sessionManager.getUserEmail();
                    Log.d(TAG, "Email для последующего входа: " + email);
                }
            }
            
            // Пользователь авторизован, если есть сессия Firebase или локальная сессия
            boolean isLoggedIn = isFirebaseLoggedIn || isLocallyLoggedIn;
            
            // Логируем статус авторизации (но не слишком часто)
            if (isLoggedIn != lastAuthCheckResult) {
                Log.d(TAG, "Статус авторизации изменился: Firebase=" + isFirebaseLoggedIn + 
                      ", Локально=" + isLocallyLoggedIn + ", Итого=" + isLoggedIn);
            }
            
            lastAuthCheckResult = isLoggedIn;
            return isLoggedIn;
        } catch (Exception e) {
            Log.e(TAG, "Непредвиденная ошибка при проверке авторизации", e);
            // В случае ошибки, считаем, что пользователь не авторизован
            return false;
        }
    }
    
    /**
     * Получение ID текущего пользователя
     * @return ID пользователя или null, если пользователь не авторизован
     */
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        } else if (sessionManager != null && sessionManager.isLoggedIn()) {
            // Если нет Firebase-сессии, возвращаем ID из локальной сессии
            return sessionManager.getUserId();
        }
        return null;
    }
    
    /**
     * Обновление профиля пользователя
     * @param user Обновленные данные пользователя
     * @return LiveData с результатом операции
     */
    public LiveData<Resource<User>> updateUserProfile(User user) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            result.setValue(Resource.error("Пользователь не авторизован", null));
            return result;
        }
        
        // Обновляем данные в Firestore
        db.collection("users").document(fbUser.getUid())
            .set(user)
            .addOnSuccessListener(aVoid -> {
                result.setValue(Resource.success(user));
                currentUser.postValue(user);
                Log.d(TAG, "Профиль пользователя обновлен: " + user.getName());
            })
            .addOnFailureListener(e -> {
                result.setValue(Resource.error("Ошибка при обновлении профиля", null));
                Log.e(TAG, "Ошибка при обновлении профиля", e);
            });
        
        return result;
    }
    
    /**
     * Загрузка аватара пользователя
     * @param imageUri URI изображения
     * @return LiveData с результатом операции
     */
    public LiveData<Resource<String>> uploadUserAvatar(Uri imageUri) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            result.setValue(Resource.error("Пользователь не авторизован", null));
            return result;
        }
        
        // Создаем ссылку на файл в Storage
        String fileName = "avatars/" + fbUser.getUid() + "_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference().child(fileName);
        
        // Загружаем файл
        UploadTask uploadTask = storageRef.putFile(imageUri);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            
            // Получаем URL загруженного файла
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                String imageUrl = downloadUri.toString();
                
                // Обновляем URL аватара в профиле пользователя
                User user = currentUser.getValue();
                if (user != null) {
                    user.setPhotoUrl(imageUrl);
                    
                    // Обновляем данные в Firestore
                    db.collection("users").document(fbUser.getUid())
                        .update("photoUrl", imageUrl)
                        .addOnSuccessListener(aVoid -> {
                            result.setValue(Resource.success(imageUrl));
                            currentUser.postValue(user);
                            Log.d(TAG, "Аватар пользователя обновлен: " + imageUrl);
                        })
                        .addOnFailureListener(e -> {
                            result.setValue(Resource.error("Ошибка при обновлении аватара", null));
                            Log.e(TAG, "Ошибка при обновлении аватара", e);
                        });
                } else {
                    result.setValue(Resource.error("Ошибка при получении данных пользователя", null));
                }
            } else {
                result.setValue(Resource.error("Ошибка при загрузке аватара", null));
                Log.e(TAG, "Ошибка при загрузке аватара", task.getException());
            }
        });
        
        return result;
    }
    
    /**
     * Синхронизация книг пользователя с Firestore
     * @param books Список книг для синхронизации
     * @return LiveData с результатом операции
     */
    public LiveData<Resource<Boolean>> syncUserBooks(List<Book> books) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        
        // Используем postValue вместо setValue, чтобы работать из любого потока
        result.postValue(Resource.loading(null));
        
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            result.postValue(Resource.error("Пользователь не авторизован", null));
            return result;
        }
        
        try {
            // Создаем транзакцию для обновления книг
            WriteBatch batch = db.batch();
            
            // Добавляем или обновляем книги
            for (Book book : books) {
                DocumentReference bookRef = db.collection("users").document(fbUser.getUid())
                    .collection("books").document(book.getId());
                batch.set(bookRef, book);
            }
            
            // Выполняем транзакцию
            batch.commit()
                .addOnSuccessListener(aVoid -> {
                    result.postValue(Resource.success(true));
                    Log.d(TAG, "Книги успешно синхронизированы: " + books.size());
                })
                .addOnFailureListener(e -> {
                    result.postValue(Resource.error("Ошибка при синхронизации книг", false));
                    Log.e(TAG, "Ошибка при синхронизации книг", e);
                });
        } catch (Exception e) {
            Log.e(TAG, "Исключение при синхронизации книг", e);
            result.postValue(Resource.error("Внутренняя ошибка при синхронизации: " + e.getMessage(), false));
        }
        
        return result;
    }
    
    /**
     * Получение книг пользователя из Firestore
     * @return LiveData со списком книг
     */
    public LiveData<Resource<List<Book>>> getUserBooks() {
        MutableLiveData<Resource<List<Book>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            result.setValue(Resource.error("Пользователь не авторизован", new ArrayList<>()));
            return result;
        }
        
        db.collection("users").document(fbUser.getUid())
            .collection("books")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Book> books = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Book book = doc.toObject(Book.class);
                    if (book != null) {
                        books.add(book);
                    }
                }
                result.setValue(Resource.success(books));
                Log.d(TAG, "Получены книги пользователя: " + books.size());
            })
            .addOnFailureListener(e -> {
                result.setValue(Resource.error("Ошибка при получении книг пользователя", new ArrayList<>()));
                Log.e(TAG, "Ошибка при получении книг пользователя", e);
            });
        
        return result;
    }
    
    /**
     * Добавление книги в Firestore
     * @param book Книга для добавления
     * @return LiveData с результатом операции
     */
    public LiveData<Resource<Book>> addBook(Book book) {
        MutableLiveData<Resource<Book>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            result.setValue(Resource.error("Пользователь не авторизован", null));
            return result;
        }
        
        // Добавляем книгу в Firestore
        db.collection("users").document(fbUser.getUid())
            .collection("books").document(book.getId())
            .set(book)
            .addOnSuccessListener(aVoid -> {
                result.setValue(Resource.success(book));
                Log.d(TAG, "Книга добавлена: " + book.getTitle());
            })
            .addOnFailureListener(e -> {
                result.setValue(Resource.error("Ошибка при добавлении книги", null));
                Log.e(TAG, "Ошибка при добавлении книги", e);
            });
        
        return result;
    }
    
    /**
     * Удаление книги из Firestore
     * @param bookId ID книги для удаления
     * @return LiveData с результатом операции
     */
    public LiveData<Resource<Boolean>> deleteBook(String bookId) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            result.setValue(Resource.error("Пользователь не авторизован", false));
            return result;
        }
        
        // Удаляем книгу из Firestore
        db.collection("users").document(fbUser.getUid())
            .collection("books").document(bookId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                result.setValue(Resource.success(true));
                Log.d(TAG, "Книга удалена: " + bookId);
            })
            .addOnFailureListener(e -> {
                result.setValue(Resource.error("Ошибка при удалении книги", false));
                Log.e(TAG, "Ошибка при удалении книги", e);
            });
        
        return result;
    }
    
    /**
     * Проверка, загружается ли что-то в данный момент
     * @return LiveData с состоянием загрузки
     */
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
} 