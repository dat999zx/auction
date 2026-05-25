package com.bidify.common.utility;

import com.google.gson.Gson;

// encode và decode json
public class JsonUtil {
    private static final Gson gson = new Gson();

    private JsonUtil(){}

    public static String toJson(Object obj) { return gson.toJson(obj); }
    public static <T> T fromJson(String json, Class<T> clazz){ return gson.fromJson(json, clazz); }
    
    // Gson deserialize response data thành LinkedTreeMap, nên cần convert lại về class đích.
    public static <T> T fromMap(Object map, Class<T> clazz){
        String json = gson.toJson(map);
        return gson.fromJson(json, clazz);
    }
}
