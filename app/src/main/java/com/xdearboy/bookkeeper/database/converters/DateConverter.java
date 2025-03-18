package com.xdearboy.bookkeeper.database.converters;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Конвертер для преобразования Date в Long и обратно для Room
 */
public class DateConverter {
    
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
} 