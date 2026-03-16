package com.bidify.server.database;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/*
phương thức giao tiếp của server với database (Supabase)
url = "/rest/v1/table_name?col_name[=eq.value]&select=[col_name or *]"
phần trong [] là có thể có hoặc ko
* là chọn tất cả
table_name là tên table vd: Users
col_name là cột trong table
value là giá trị cụ thể trong cột đó muốn tìm: vd tìm username tên Alice thì value = Alice
*/
public class SupabaseClient {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static HttpResponse<String> get(String url) throws Exception{ // lấy thông tin từ database
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.BASE_URL + url))
                .header("apikey", Config.API_KEY)
                .header("Authorization", "Bearer " + Config.API_KEY)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> post(String url, String body) throws Exception{ // gửi thông tin đến database
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.BASE_URL + url))
                .header("apikey", Config.API_KEY)
                .header("Authorization", "Bearer " + Config.API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> patch(String url, String body) throws Exception{ // cập nhật 1 phần data
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.BASE_URL + url))
                .header("apikey", Config.API_KEY)
                .header("Authorization", "Bearer " + Config.API_KEY)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> delete(String url) throws Exception{ // xóa thông tin trong database
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.BASE_URL + url))
                .header("apikey", Config.API_KEY)
                .header("Authorization", "Bearer " + Config.API_KEY)
                .DELETE()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}