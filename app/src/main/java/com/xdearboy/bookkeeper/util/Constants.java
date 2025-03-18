package com.xdearboy.bookkeeper.util;

/**
 * Константы приложения
 */
public class Constants {
    
    /**
     * API ключ для Google Books API
     * Примечание: В реальном приложении ключ должен храниться в защищенном месте,
     * например, в gradle.properties или с использованием NDK
     * 
     * ВАЖНО: Текущий ключ недействителен (ошибка 403).
     * Необходимо создать собственный ключ API в Google Cloud Console:
     * 1. Перейдите на https://console.cloud.google.com/
     * 2. Создайте новый проект
     * 3. Включите Google Books API
     * 4. Создайте ключ API без ограничений
     * 5. Вставьте ключ ниже
     * 
     * Если ключ не указан, приложение будет использовать тестовые данные
     */
    public static final String GOOGLE_BOOKS_API_KEY = "AIzaSyDTfQZn2JiDuRhkGytxsChYAtvRUrAT1Qg";
    
    /**
     * Базовый URL для Google Books API
     */
    public static final String GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1/";
    
    /**
     * Максимальное количество результатов на страницу
     */
    public static final int PAGE_SIZE = 20; // Уменьшаем размер страницы для снижения нагрузки на API
    
    /**
     * Размер кэша для изображений (в байтах)
     */
    public static final int IMAGE_CACHE_SIZE = 10 * 1024 * 1024; // 10 МБ
    
    /**
     * Тайм-аут для сетевых запросов (в секундах)
     */
    public static final int NETWORK_TIMEOUT = 30;
} 