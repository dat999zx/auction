package com.bidify.utility;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

// bộ nhớ tạm thời lưu ảnh trong cache (RAM) của client
public final class ImageCache {
    private static final Logger logger = LoggerFactory.getLogger(ImageCache.class);
    private static final ImageCache instance = new ImageCache();
    
    private final ConcurrentHashMap<String, Image> cache = new ConcurrentHashMap<>();

    // dùng để tạo một đối tượng ImageCache
    private ImageCache() {}

    // dùng để lấy đối tượng Singleton
    public static ImageCache getInstance() { return instance; }

    // lấy ảnh trong cache
    // dùng để lấy
    public Image get(String key, String base64String) {
        if (key == null || base64String == null || base64String.isBlank())
            return null;

        return cache.computeIfAbsent(key, k -> {
            try {
                byte[] bytes = Base64.getDecoder().decode(base64String);
                Image image = new Image(new ByteArrayInputStream(bytes));
                if (image.isError()) {
                    logger.error("Decoded image is in error state for key: " + key);
                    return null;
                }
                return image;
            }
            catch (Exception e) {
                logger.error("Failed to decode image for key: " + key, e);
                return null;
            }
        });
    }

    // lưu ảnh vào cache
    // dùng để put
    public void put(String key, Image image) {
        if (key != null && image != null)
            cache.put(key, image);
    }

    // dùng để xóa sạch
    public void clear() {
        cache.clear();
    }
}
