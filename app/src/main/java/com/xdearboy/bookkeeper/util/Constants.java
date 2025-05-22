package com.xdearboy.bookkeeper.util;
public class Constants {
    public static final String GOOGLE_BOOKS_API_KEY = "AIzaSyD6_1lnyVk4--CbiyJlkq2yBRHgYuKtHFI";
    public static final String GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1/";
    public static final int PAGE_SIZE = 10; // Уменьшаем размер страницы для снижения нагрузки на API и согласно запросу
    public static final int IMAGE_CACHE_SIZE = 10 * 1024 * 1024; // 10 МБ
    public static final int NETWORK_TIMEOUT = 30;
}