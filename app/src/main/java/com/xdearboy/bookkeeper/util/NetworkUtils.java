package com.xdearboy.bookkeeper.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * Утилиты для работы с сетью
 */
public class NetworkUtils {
    
    /**
     * Проверяет доступность интернета
     * @param context Контекст приложения
     * @return true, если интернет доступен
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }
    
    /**
     * Проверяет, подключен ли Wi-Fi
     * @param context Контекст приложения
     * @return true, если Wi-Fi подключен
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) return false;
        
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiNetworkInfo != null && wifiNetworkInfo.isConnected();
        }
    }
} 