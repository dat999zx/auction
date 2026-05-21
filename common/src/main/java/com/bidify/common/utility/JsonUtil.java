package com.bidify.common.utility;

import com.google.gson.Gson;

// encode và decode json
public class JsonUtil {
    private static final Gson gson = new Gson();

    // dùng để tạo một đối tượng JsonUtil
    private JsonUtil(){}

    // dùng để chuyển thành json
    public static String toJson(Object obj) { return gson.toJson(obj); } // biến object thành json
    // dùng để từ json
    public static <T> T fromJson(String json, Class<T> clazz){ return gson.fromJson(json, clazz); } // biến json thành object
    // dùng để từ chuyển đổi
    public static <T> T fromMap(Object map, Class<T> clazz){ // đổi object thành class tương ứng
        String json = gson.toJson(map);
        return gson.fromJson(json, clazz);
    }
}
