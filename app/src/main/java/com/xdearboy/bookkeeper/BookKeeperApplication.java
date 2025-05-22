package com.xdearboy.bookkeeper;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import androidx.room.Room;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.repository.FirebaseRepository;
public class BookKeeperApplication extends Application {
    private static final String TAG = "BookKeeperApp";
    private static final boolean DEBUG_MODE = true; // Режим отладки
    // @Override
    public void onCreate() {
        super.onCreate();
        try {
            FirebaseApp.initializeApp(this);
            boolean isGooglePlayServicesAvailable = checkGooglePlayServices();
            FirebaseRepository firebaseRepository = FirebaseRepository.getInstance();
            firebaseRepository.init(this);
            firebaseRepository.setGooglePlayServicesAvailable(isGooglePlayServicesAvailable);
            if (!DEBUG_MODE) {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
            } else {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
            }
            if (!DEBUG_MODE) {
                disablePhenotypeApiLogs();
            }
            Log.d(TAG, "Приложение инициализировано успешно");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации приложения", e);
            if (!DEBUG_MODE) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
        try {
            AppDatabase.init(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации базы данных", e);
            if (!DEBUG_MODE) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
    }
    private boolean checkGooglePlayServices() {
        try {
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
            if (resultCode == ConnectionResult.SUCCESS) {
                Log.d(TAG, "Google Play Services доступны");
                return true;
            } else {
                Log.w(TAG, "Google Play Services недоступны, код: " + resultCode);
                if (apiAvailability.isUserResolvableError(resultCode)) {
                    Log.d(TAG, "Ошибка Google Play Services может быть исправлена пользователем");
                } else {
                    Log.e(TAG, "Устройство не поддерживает Google Play Services");
                }
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке Google Play Services", e);
            return false;
        }
    }
    private void disablePhenotypeApiLogs() {
        try {
            Log.i(TAG, "Отключение логов Phenotype API");
            Log.i("PhenotypeApiService", "Отключение логирования для Phenotype API");
            Log.i("GoogleApiAvailability", "Отключение логирования для GoogleApiAvailability");
        } catch (Exception e) {
            Log.w(TAG, "Не удалось отключить логирование Phenotype API");
        }
    }
} 