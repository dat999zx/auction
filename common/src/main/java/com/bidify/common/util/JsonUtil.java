package com.bidify.common.util;

import com.google.gson.Gson;

// encode và decode json
public class JsonUtil {
    private static final Gson gson = new Gson();

    public static String toJson(Object obj) { return gson.toJson(obj); } // biến object thành json
    public static <T> T fromJson(String json, Class<T> clazz){ return gson.fromJson(json, clazz); } // biến json thành object
    public static <T> T fromMap(Object map, Class<T> clazz){ // đổi object thành class tương ứng
        String json = gson.toJson(map);
        return gson.fromJson(json, clazz);
    }
}