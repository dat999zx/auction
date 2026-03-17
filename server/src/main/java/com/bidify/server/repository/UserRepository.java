package com.bidify.server.repository;

import com.bidify.server.database.SupabaseClient;
import com.bidify.server.model.User;
import com.bidify.common.util.JsonUtil;

import java.net.http.HttpResponse;

// giao tiếp với database về phần người dùng
public class UserRepository {
    public boolean existsByUsername(String username){ // xét tồn tại username trong database
        try{
            HttpResponse<String> response = SupabaseClient.get("/rest/v1/Users?username=eq." + username + "&select=userId");
            return response.statusCode() == 200 && !response.body().equals("[]");
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public User findByUsername(String username){ // lấy User từ database bằng username
        try{
            HttpResponse<String> response = SupabaseClient.get("/rest/v1/Users?username=eq." + username + "&select=*");
            User user = JsonUtil.fromJsonArray(response.body(), User.class);
            return response.statusCode() == 200 ? user : null;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public User findByUserId(String id){ // lấy User từ database bằng userId
        try{
            HttpResponse<String> response = SupabaseClient.get("/rest/v1/Users?userId=eq." + id + "&select=*");
            User user = JsonUtil.fromJsonArray(response.body(), User.class);
            return response.statusCode() == 200 ? user : null;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public boolean save(User user){ // lưu user vào database
        try{
            String body = JsonUtil.toJson(user);
            HttpResponse<String> response = SupabaseClient.post("/rest/v1/Users", body);
            System.out.println(response.body());
            return response.statusCode() == 201;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
