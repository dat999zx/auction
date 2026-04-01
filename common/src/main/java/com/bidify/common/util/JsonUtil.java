package com.bidify.common.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

// encode và decode json
public class JsonUtil {
    private static final Gson gson = new Gson();

    private JsonUtil(){}

    public static String toJson(Object obj) { return gson.toJson(obj); } // biến object thành json
    public static <T> T fromJson(String json, Class<T> clazz){ return gson.fromJson(json, clazz); } // biến json thành object
    public static <T> T fromJsonArray(String json, Class<T> clazz){
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        if (arr.size() == 0) return null;
        return gson.fromJson(arr.get(0), clazz);
    }
    public static <T> T fromMap(Object map, Class<T> clazz){ // đổi object thành class tương ứng
        String json = gson.toJson(map);
        return gson.fromJson(json, clazz);
    }
}