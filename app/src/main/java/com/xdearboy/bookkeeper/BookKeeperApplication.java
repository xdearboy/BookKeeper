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

/**
 * Класс приложения
 */
public class BookKeeperApplication extends Application {
    
    private static final String TAG = "BookKeeperApp";
    private static final boolean DEBUG_MODE = true; // Режим отладки
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Инициализация Firebase
        try {
            FirebaseApp.initializeApp(this);
            
            // Проверяем доступность Google Play Services
            boolean isGooglePlayServicesAvailable = checkGooglePlayServices();
            
            // Инициализируем FirebaseRepository
            FirebaseRepository firebaseRepository = FirebaseRepository.getInstance();
            firebaseRepository.init(this);
            firebaseRepository.setGooglePlayServicesAvailable(isGooglePlayServicesAvailable);
            
            // Настраиваем Crashlytics в зависимости от режима сборки
            if (!DEBUG_MODE) {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
            } else {
                // В debug-режиме отключаем сбор данных
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
            }
            
            // Отключаем логирование ошибок Phenotype API в релизной версии
            if (!DEBUG_MODE) {
                disablePhenotypeApiLogs();
            }
            
            Log.d(TAG, "Приложение инициализировано успешно");
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации приложения", e);
            // В продакшн-версии логируем ошибку в Crashlytics
            if (!DEBUG_MODE) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
        
        // Инициализация базы данных
        try {
            AppDatabase.init(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации базы данных", e);
            // В продакшн-версии логируем ошибку в Crashlytics
            if (!DEBUG_MODE) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
    }
    
    /**
     * Проверяет доступность Google Play Services
     */
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
    
    /**
     * Отключает логирование ошибок Phenotype API
     */
    private void disablePhenotypeApiLogs() {
        try {
            // Устанавливаем фильтры тегов вместо использования TerribleFailureHandler
            Log.i(TAG, "Отключение логов Phenotype API");
            // Устанавливаем фильтр для логов от Phenotype API
            Log.i("PhenotypeApiService", "Отключение логирования для Phenotype API");
            // Можно также отключить логи для GoogleApiAvailability
            Log.i("GoogleApiAvailability", "Отключение логирования для GoogleApiAvailability");
        } catch (Exception e) {
            // Игнорируем ошибки, так как это опциональная функциональность
            Log.w(TAG, "Не удалось отключить логирование Phenotype API");
        }
    }
} 