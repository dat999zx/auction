package com.bidify.server.repository;

import com.bidify.server.database.SupabaseClient;

import java.net.http.HttpResponse;

import com.bidify.common.util.JsonUtil;
import com.bidify.server.model.User;

// giao tiếp với database về phần người dùng
public class UserRepository {
    public boolean existsByUsername(String username){
        try{
            HttpResponse<String> response = SupabaseClient.get("/rest/v1/Users?username=eq." + username + "&select=userId");
            return response.statusCode() == 200 && !response.body().equals("[]");
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public User findByUsername(String username){
        try{
            HttpResponse<String> response = SupabaseClient.get("/rest/v1/Users?username=eq." + username + "&select=*");
            User user = JsonUtil.fromJson(response.body(), User.class);
            return response.statusCode() == 200 ? user : null;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public boolean save(User user){
        try{
            String body = JsonUtil.toJson(user);
            HttpResponse<String> response = SupabaseClient.post("/rest/v1/Users", body);
            System.out.println(response.body());
            return response.statusCode() == 200;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
