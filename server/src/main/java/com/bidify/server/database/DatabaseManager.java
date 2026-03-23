package com.bidify.server.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// giao tiếp trực tiếp với database để đọc, viết, xóa...
public class DatabaseManager {
    // CREATE / INSERT / UPDATE / DELETE
    public static boolean update(String sql, Object... params){
        try(Connection connection = SQLiteConnection.connect();
            PreparedStatement statement = connection.prepareStatement(sql)
        ){
            setParams(statement, params);
            return statement.executeUpdate() > 0;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    // SELECT
    public static <T> T query(String sql, ResultHandler<T> handler, Object... params){
        try(Connection connection = SQLiteConnection.connect();
            PreparedStatement statement = connection.prepareStatement(sql)
        ){
            setParams(statement, params);
            try (ResultSet rs = statement.executeQuery()){
                return handler.handle(rs);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static void setParams(PreparedStatement statement, Object... params) throws Exception{
        for (int i = 0; i < params.length; i++)
            statement.setObject(i + 1, params[i]);
    }
}
// "CREATE Users SELECT username WHERE "