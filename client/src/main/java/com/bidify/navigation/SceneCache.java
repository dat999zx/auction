package com.bidify.navigation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

final class SceneCache {
    private final Map<String, Parent> cache = new ConcurrentHashMap<>();
    private final Map<Parent, Object> controllers = new ConcurrentHashMap<>();

    Parent load(String fxml, boolean remember) throws Exception {
        Parent cached = cache.get(fxml);
        if (remember && cached != null) return cached;

        Parent root = loadFresh(fxml);
        if (remember) cache.put(fxml, root);
        return root;
    }

    void clear(String fxml) {
        if (fxml == null) return;
        Parent removed = cache.remove(fxml);
        if (removed != null)
            controllers.remove(removed);
    }

    void clearAll() {
        cache.clear();
        controllers.clear();
    }

    boolean contains(String fxml) {
        return fxml != null && cache.containsKey(fxml);
    }

    Object getController(Parent root) {
        return root == null ? null : controllers.get(root);
    }

    void forgetUncached(Parent root) {
        if (root != null && !cache.containsValue(root))
            controllers.remove(root);
    }

    private Parent loadFresh(String fxml) throws Exception {
        var location = SceneManager.class.getResource("/fxml/" + fxml);
        if (location == null) throw new IllegalArgumentException("FXML not found: /fxml/" + fxml);
        FXMLLoader loader = new FXMLLoader(location);
        Parent root = loader.load();
        Object controller = loader.getController();
        if (controller != null)
            controllers.put(root, controller);
        return root;
    }
}
