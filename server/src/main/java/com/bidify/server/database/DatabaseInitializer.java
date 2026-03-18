package com.bidify.server.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;

// load database (data.db) hoặc tạo database từ khung xương (architecture.sql) nếu chưa có
public class DatabaseInitializer {
    public static void init(){
        try (
            Connection connection = SQLiteConnection.connect();
            Statement statement = connection.createStatement()){
                InputStream in = DatabaseInitializer.class.getResourceAsStream("/architecture.sql");
                String sql = new String(in.readAllBytes());
                statement.execute(sql);
            }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
