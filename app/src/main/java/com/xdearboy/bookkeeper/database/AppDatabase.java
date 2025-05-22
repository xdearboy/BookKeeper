package com.xdearboy.bookkeeper.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.xdearboy.bookkeeper.database.dao.BookDao;
import com.xdearboy.bookkeeper.database.dao.CategoryDao;
import com.xdearboy.bookkeeper.database.dao.NotificationDao;
import com.xdearboy.bookkeeper.database.dao.UserDao;
import com.xdearboy.bookkeeper.database.dao.UserPreferencesDao;
import com.xdearboy.bookkeeper.model.Book;
import com.xdearboy.bookkeeper.model.Category;
import com.xdearboy.bookkeeper.model.Notification;
import com.xdearboy.bookkeeper.model.User;
import com.xdearboy.bookkeeper.model.UserPreferences;
import com.xdearboy.bookkeeper.util.DatabaseInitializer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = { Book.class, User.class, Notification.class, Category.class,
        UserPreferences.class }, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "bookkeeper_db";

    public abstract BookDao bookDao();

    public abstract UserDao userDao();

    public abstract NotificationDao notificationDao();

    public abstract CategoryDao categoryDao();

    public abstract UserPreferencesDao userPreferencesDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    databaseWriteExecutor.execute(() -> {
                                        DatabaseInitializer.populateDatabase(INSTANCE);
                                    });
                                }
                            })
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static AppDatabase init(Context context) {
        return getInstance(context);
    }
}