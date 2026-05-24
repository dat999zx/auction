package com.bidify.utility;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

final class SceneCache {
    private final Map<String, Parent> cache = new ConcurrentHashMap<>();

    Parent load(String fxml, boolean remember) throws Exception {
        Parent cached = cache.get(fxml);
        if (remember && cached != null) return cached;

        Parent root = loadFresh(fxml);
        if (remember) cache.put(fxml, root);
        return root;
    }

    void clear(String fxml) {
        if (fxml != null) cache.remove(fxml);
    }

    void clearAll() {
        cache.clear();
    }

    boolean contains(String fxml) {
        return fxml != null && cache.containsKey(fxml);
    }

    private Parent loadFresh(String fxml) throws Exception {
        var location = SceneManager.class.getResource("/fxml/" + fxml);
        if (location == null) throw new IllegalArgumentException("FXML not found: /fxml/" + fxml);
        FXMLLoader loader = new FXMLLoader(location);
        return loader.load();
    }
}
