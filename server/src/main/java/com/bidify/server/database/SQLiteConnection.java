package com.bidify.server.database;

import java.sql.Connection;
import java.sql.DriverManager;

// kết nối với database
public class SQLiteConnection {
    private static final String URL = "jdbc:sqlite:data.db";

    public static Connection connect(){
        try{
            return DriverManager.getConnection(URL);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
