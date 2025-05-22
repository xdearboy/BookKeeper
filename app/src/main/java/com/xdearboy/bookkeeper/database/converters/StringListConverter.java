package com.xdearboy.bookkeeper.database.converters;
import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
public class StringListConverter {
    @TypeConverter
    public static List<String> fromString(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<String>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }
    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null) {
            return null;
        }
        return new Gson().toJson(list);
    }
} 