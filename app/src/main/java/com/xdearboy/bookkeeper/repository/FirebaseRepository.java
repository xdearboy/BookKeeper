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
    private static final long AUTH_CHECK_INTERVAL = 2000; // 2 секунды
    private long lastAuthCheckTime = 0;
    private boolean lastAuthCheckResult = false;
    private FirebaseRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
        try {
            auth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при отключении reCAPTCHA", e);
        }
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser != null) {
            fetchUserData(fbUser.getUid());
        } else {
            currentUser.postValue(null);
        }
    }
    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseRepository();
        }
        return instance;
    }
    public void init(Context context) {
        this.context = context.getApplicationContext();
        sessionManager = SessionManager.getInstance(this.context);
        if (sessionManager != null && sessionManager.isLoggedIn() && auth.getCurrentUser() == null) {
            String email = sessionManager.getUserEmail();
            Log.d(TAG, "Обнаружена предыдущая сессия пользователя: " + email);
        }
    }
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
                    createNewUser(userId);
                }
                isLoading.postValue(false);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Ошибка при получении данных пользователя", e);
                isLoading.postValue(false);
            });
    }
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
    public void setGooglePlayServicesAvailable(boolean available) {
        this.isGooglePlayServicesAvailable = available;
        Log.d(TAG, "Google Play Services доступность: " + available);
    }
    public boolean isGooglePlayServicesAvailable() {
        return isGooglePlayServicesAvailable;
    }
    public LiveData<Resource<User>> register(String email, String password, String name) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.error("Firebase отключен. Используйте локальную регистрацию.", null));
        return result;
    }
    public LiveData<Resource<User>> login(String email, String password) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.error("Firebase отключен. Используйте локальный логин.", null));
        return result;
    }
    public void logout() {}
    public LiveData<User> getCurrentUser() {
        return new MutableLiveData<>();
    }
    public boolean isUserLoggedIn() {
        return false;
    }
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        } else if (sessionManager != null && sessionManager.isLoggedIn()) {
            return sessionManager.getUserId();
        }
        return null;
    }
    public LiveData<Resource<User>> updateUserProfile(User user) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            result.setValue(Resource.error("Пользователь не авторизован", null));
            return result;
        }
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
    public LiveData<Resource<String>> uploadUserAvatar(Uri imageUri) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            result.setValue(Resource.error("Пользователь не авторизован", null));
            return result;
        }
        String fileName = "avatars/" + fbUser.getUid() + "/" + UUID.randomUUID().toString();
        StorageReference storageRef = storage.getReference().child(fileName);
        
        storageRef.putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    result.setValue(Resource.success(downloadUrl));
                    Log.d(TAG, "Аватар загружен: " + downloadUrl);
                });
            })
            .addOnFailureListener(e -> {
                result.setValue(Resource.error("Ошибка при загрузке аватара", null));
                Log.e(TAG, "Ошибка при загрузке аватара", e);
            });
        return result;
    }
    
    public LiveData<Resource<Boolean>> deleteBook(String bookId) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            result.setValue(Resource.error("Пользователь не авторизован", false));
            return result;
        }
        
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
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
} 