package com.xdearboy.bookkeeper.database.converters;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Конвертер для преобразования Map<String, Object> в строку и обратно
 */
public class MapConverter {
    
    private static final Gson gson = new Gson();
    
    /**
     * Преобразует Map<String, Object> в строку JSON
     * @param map Карта для преобразования
     * @return Строка JSON
     */
    @TypeConverter
    public static String fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        
        return gson.toJson(map);
    }
    
    /**
     * Преобразует строку JSON в Map<String, Object>
     * @param json Строка JSON
     * @return Карта
     */
    @TypeConverter
    public static Map<String, Object> toMap(String json) {
        if (json == null) {
            return new HashMap<>();
        }
        
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(json, type);
    }
} 