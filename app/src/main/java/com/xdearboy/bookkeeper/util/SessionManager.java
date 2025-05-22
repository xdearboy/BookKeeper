package com.xdearboy.bookkeeper.util;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Date;
public class SessionManager {
    private static final String PREF_NAME = "BookKeeperSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_LOGIN_TIME = "login_time";
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;
    private static SessionManager instance;
    private SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }
    public void createSession(String userId, String name, String email) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_LOGIN_TIME, new Date().getTime());
        editor.apply();
    }
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }
    public String getUserName() {
        return pref.getString(KEY_USER_NAME, null);
    }
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }
    public long getLoginTime() {
        return pref.getLong(KEY_LOGIN_TIME, 0);
    }
    public void logout() {
        editor.clear();
        editor.apply();
    }
} 