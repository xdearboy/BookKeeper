package com.xdearboy.bookkeeper.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Класс для обработки состояний запросов (загрузка, успех, ошибка)
 * @param <T> Тип данных
 */
public class Resource<T> {
    @NonNull
    public final Status status;
    
    @Nullable
    public final T data;
    
    @Nullable
    public final String message;
    
    private Resource(@NonNull Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }
    
    /**
     * Создает ресурс с состоянием "успех"
     * @param data Данные
     * @param <T> Тип данных
     * @return Ресурс с состоянием "успех"
     */
    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }
    
    /**
     * Создает ресурс с состоянием "ошибка"
     * @param msg Сообщение об ошибке
     * @param data Данные (могут быть null)
     * @param <T> Тип данных
     * @return Ресурс с состоянием "ошибка"
     */
    public static <T> Resource<T> error(String msg, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, msg);
    }
    
    /**
     * Создает ресурс с состоянием "загрузка"
     * @param data Данные (могут быть null)
     * @param <T> Тип данных
     * @return Ресурс с состоянием "загрузка"
     */
    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }
    
    /**
     * Перечисление возможных состояний ресурса
     */
    public enum Status { SUCCESS, ERROR, LOADING }
} 