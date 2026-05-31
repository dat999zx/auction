package com.bidify.media;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import javafx.application.Platform;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Các ca kiểm thử cho ImageCache trong module client.
 */
class ImageCacheTest {

    private ImageCache imageCache;

    // Chuỗi Base64 đại diện cho ảnh PNG 1x1 pixel hợp lệ
    private static final String VALID_BASE64_IMAGE = 
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";

    @BeforeAll
    static void initJavaFX() {
        try {
            // Khởi chạy JavaFX Platform để tránh lỗi NoClassDefFoundError hoặc Toolkit not initialized khi tạo đối tượng Image
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Bỏ qua nếu nền tảng JavaFX đã khởi chạy từ trước
        }
    }

    @BeforeEach
    void setUp() {
        imageCache = ImageCache.getInstance();
        imageCache.clear(); // Đảm bảo bộ nhớ cache sạch trước mỗi test
    }

    @Test
    void testGetInstance() {
        // Kiểm tra mẫu thiết kế Singleton hoạt động chính xác
        assertNotNull(imageCache);
        assertSame(imageCache, ImageCache.getInstance(), "ImageCache phải là một Singleton duy nhất");
    }

    @Test
    void testDecodeValidBase64() {
        // Giải mã ảnh hợp lệ
        Image image = ImageCache.decode(VALID_BASE64_IMAGE);
        assertNotNull(image, "Ảnh giải mã từ chuỗi Base64 hợp lệ không được null");
        assertFalse(image.isError(), "Ảnh giải mã không được ở trạng thái lỗi");
    }

    @Test
    void testDecodeNullOrBlankReturnsNull() {
        // Đầu vào null hoặc trống trả về null
        assertNull(ImageCache.decode(null));
        assertNull(ImageCache.decode(""));
        assertNull(ImageCache.decode("   "));
    }

    @Test
    void testDecodeInvalidBase64ReturnsNull() {
        // Chuỗi Base64 bị lỗi cấu trúc hoặc không phải định dạng ảnh hợp lệ
        assertNull(ImageCache.decode("not-a-base64-string"));
        
        // Chuỗi base64 hợp lệ về cú pháp nhưng dữ liệu ảnh bị hỏng
        String corruptBase64 = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4, 5});
        assertNull(ImageCache.decode(corruptBase64));
    }

    @Test
    void testGetAndCacheReuse() {
        // Lần đầu gọi get: phải tạo mới ảnh và đưa vào cache
        Image img1 = imageCache.get("test-key", VALID_BASE64_IMAGE);
        assertNotNull(img1);

        // Lần thứ hai gọi get với cùng key: phải trả về đúng đối tượng cũ (tái sử dụng từ cache)
        Image img2 = imageCache.get("test-key", VALID_BASE64_IMAGE);
        assertSame(img1, img2, "Ảnh phải được tái sử dụng từ cache và trả về cùng một instance");
    }

    @Test
    void testGetNullOrBlankKeyOrDataReturnsNull() {
        // Kiểm tra các tham số không hợp lệ
        assertNull(imageCache.get(null, VALID_BASE64_IMAGE));
        assertNull(imageCache.get("test-key", null));
        assertNull(imageCache.get("test-key", ""));
        assertNull(imageCache.get("test-key", "   "));
    }

    @Test
    void testGetCorruptDataReturnsNullAndDoesNotCache() {
        // Dữ liệu ảnh bị hỏng
        String corruptBase64 = Base64.getEncoder().encodeToString(new byte[]{10, 20, 30});
        Image img = imageCache.get("corrupt-key", corruptBase64);
        assertNull(img, "Dữ liệu ảnh hỏng phải trả về null");

        // Xác nhận không lưu vào cache (lần sau gọi lại vẫn null)
        assertNull(imageCache.get("corrupt-key", corruptBase64));
    }

    @Test
    void testPutAndClear() {
        Image image = ImageCache.decode(VALID_BASE64_IMAGE);
        assertNotNull(image);

        // Chủ động lưu ảnh vào cache qua put
        imageCache.put("manual-key", image);
        assertSame(image, imageCache.get("manual-key", VALID_BASE64_IMAGE));

        // Xóa sạch cache
        imageCache.clear();

        // Sau khi clear, lần get tiếp theo sẽ phải giải mã lại và tạo ra đối tượng Image mới (khác instance cũ)
        Image newImg = imageCache.get("manual-key", VALID_BASE64_IMAGE);
        assertNotNull(newImg);
        assertNotSame(image, newImg, "Sau khi clear cache, đối tượng Image cũ phải bị xóa và một đối tượng mới được tạo ra");
    }

    @Test
    void testPutNullDoesNothing() {
        // Đưa key null hoặc image null vào không gây lỗi và không lưu
        imageCache.put(null, null);
        imageCache.put("key", null);
        imageCache.put(null, ImageCache.decode(VALID_BASE64_IMAGE));
        
        // Nhờ cơ chế computeIfAbsent, nếu "key" có trong cache, nó sẽ trả về ảnh đã cache.
        // Nếu không có trong cache, nó sẽ cố gắng decode "invalid-base64" và trả về null.
        assertNull(imageCache.get("key", "invalid-base64"));
    }
}
