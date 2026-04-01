package com.bidify.utility;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

/*
chuyển đổi sang các scene khác dễ dàng hơn
SceneManager.switchScene(fxml) để đổi sang scene mới và tự ghi nhớ
SceneManager.switchScene(fxml, false) để đổi sang scene mới mà ko ghi nhớ lại
mục đích của việc ghi nhớ là để lần sau nếu có mở lại scene đó thì load nhanh hơn
*/
public class SceneManager {
    private static Stage stage;
    private static final Map<String, Parent> cache = new HashMap<>(); // chứa các scene đã ghi nhớ

    private SceneManager(){}

    public static void setStage(Stage s){ stage = s; } // chỉ gọi 1 lần trong MainApp

    public static void switchScene(String fxml){ switchScene(fxml, true); } // mặc định ghi nhớ

    public static void switchScene(String fxml, boolean remember){
        try {
            Parent root;
            if (remember && cache.containsKey(fxml)) root = cache.get(fxml);
            else{
                FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/" + fxml));
                root = loader.load();
                if (remember) cache.put(fxml, root);
            }
            Scene scene = stage.getScene();
            if (scene == null){
                scene = new Scene(root);
                stage.setScene(scene);
            }
            else scene.setRoot(root);
            
            // load css
            String cssName = fxml.replace(".fxml", ".css");
            var cssUrl = SceneManager.class.getResource("/css/" + cssName);
            scene.getStylesheets().clear();
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        catch (Exception e){ e.printStackTrace(); }
    }

    public static void clearCache(String fxml){ cache.remove(fxml); } // xóa scene khỏi bộ nhớ
    public static void clearAllCache(){ cache.clear(); } // xóa tất cả scene trong bộ nhớ
}