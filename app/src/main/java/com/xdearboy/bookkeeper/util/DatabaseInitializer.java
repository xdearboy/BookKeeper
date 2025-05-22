package com.xdearboy.bookkeeper.util;

import com.xdearboy.bookkeeper.database.AppDatabase;
import com.xdearboy.bookkeeper.model.Category;
import com.xdearboy.bookkeeper.model.User;

import java.util.UUID;

/**
 * Utility class for initializing the database with sample data
 */
public class DatabaseInitializer {

    /**
     * Populates the database with initial data
     * @param db The application database instance
     */
    public static void populateDatabase(AppDatabase db) {
        // Add default categories
        addDefaultCategories(db);
        
        // Add admin user if no users exist
        if (db.userDao().getUserCount() == 0) {
            addAdminUser(db);
        }
    }

    private static void addDefaultCategories(AppDatabase db) {
        String[] defaultCategories = {
            "Fiction", "Non-Fiction", "Science Fiction", "Fantasy", 
            "Mystery", "Thriller", "Romance", "Horror", "Biography",
            "History", "Science", "Technology", "Self-Help", "Business",
            "Travel", "Poetry", "Comics", "Art", "Cooking", "Children's"
        };
        
        for (String categoryName : defaultCategories) {
            Category category = new Category();
            category.setId(UUID.randomUUID().toString());
            category.setName(categoryName);
            category.setDescription(categoryName + " books");
            db.categoryDao().insert(category);
        }
    }

    private static void addAdminUser(AppDatabase db) {
        User adminUser = new User(
            UUID.randomUUID().toString(),
            "admin@bookkeeper.com",
            PasswordUtils.hashPassword("admin123"),
            "Administrator"
        );
        adminUser.setProfileImageUrl("");
        db.userDao().insert(adminUser);
    }
}