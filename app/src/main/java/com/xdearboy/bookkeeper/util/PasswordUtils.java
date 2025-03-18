package com.xdearboy.bookkeeper.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Утилита для работы с паролями
 */
public class PasswordUtils {
    
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;
    
    /**
     * Хеширует пароль с использованием SHA-256
     * @param password Пароль для хеширования
     * @return Хешированный пароль
     */
    public static String hashPassword(String password) {
        try {
            // Генерируем соль
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            
            // Хешируем пароль с солью
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // Объединяем соль и хеш
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);
            
            // Кодируем в Base64
            return android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Проверяет, соответствует ли пароль хешу
     * @param password Пароль для проверки
     * @param hashedPassword Хешированный пароль
     * @return true, если пароль соответствует хешу
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        try {
            // Декодируем из Base64
            byte[] combined = android.util.Base64.decode(hashedPassword, android.util.Base64.DEFAULT);
            
            // Извлекаем соль
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
            
            // Хешируем введенный пароль с той же солью
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedInput = md.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // Извлекаем оригинальный хеш
            byte[] originalHash = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, SALT_LENGTH, originalHash, 0, originalHash.length);
            
            // Сравниваем хеши
            return MessageDigest.isEqual(hashedInput, originalHash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }
} 