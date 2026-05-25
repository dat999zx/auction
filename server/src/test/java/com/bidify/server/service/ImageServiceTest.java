package com.bidify.server.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.server.model.Image;

/**
 * Lớp kiểm thử (Unit Test) cho {@link ImageService}.
 * Kiểm tra các tính năng:
 * - Lưu ảnh từ chuỗi Base64 thành công xuống đĩa cứng (hệ thống tập tin).
 * - Bỏ qua danh sách rỗng, null, hoặc các phần tử không hợp lệ khi lưu ảnh.
 * - Đọc ảnh và chuyển đổi ngược thành chuỗi Base64 thành công.
 * - Xóa file ảnh trên đĩa cứng thành công và xử lý ngoại lệ xóa file không tồn tại.
 * Có chú thích chi tiết bằng tiếng Việt.
 */
class ImageServiceTest {

    private final ImageService imageService = ImageService.getInstance();

    // Chuỗi Base64 giả lập đại diện cho ảnh PNG kích thước 1x1 pixel hợp lệ
    private static final String VALID_BASE64_IMAGE = 
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";

    // Theo dõi các file ảnh đã tạo trong suốt quá trình test để dọn dẹp sau khi chạy xong
    private final List<String> createdFilePaths = new ArrayList<>();

    @BeforeEach
    void setUp() {
        createdFilePaths.clear();
    }

    @AfterEach
    void tearDown() {
        // Dọn dẹp tất cả các file ảnh giả lập được ghi xuống đĩa cứng trong lúc test
        for (String filePath : createdFilePaths) {
            try {
                Path path = Paths.get(filePath);
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // Bỏ qua lỗi nếu không xóa được file
            }
        }
        createdFilePaths.clear();
    }

    /**
     * Ca kiểm thử: Lưu thành công chuỗi Base64 hình ảnh hợp lệ.
     * Xác thực:
     * - Kết quả trả về một đối tượng Image chứa ID và đường dẫn.
     * - File ảnh thực tế được ghi thành công xuống đĩa cứng.
     */
    @Test
    void saveImagesSuccessfully() {
        List<String> base64Images = new ArrayList<>();
        base64Images.add(VALID_BASE64_IMAGE);

        List<Image> savedImages = imageService.saveImages(base64Images);

        assertNotNull(savedImages);
        assertEquals(1, savedImages.size());

        Image image = savedImages.get(0);
        assertNotNull(image.getId());
        assertNotNull(image.getFilePath());
        assertNotNull(image.getCreatedAt());

        // Theo dõi file để dọn dẹp sau test
        createdFilePaths.add(image.getFilePath());

        // Xác thực file thực sự tồn tại trên hệ thống tập tin
        Path path = Paths.get(image.getFilePath());
        assertTrue(Files.exists(path), "File ảnh phải được lưu trữ thực tế trên đĩa cứng.");
    }

    /**
     * Ca kiểm thử: Lưu ảnh thất bại khi danh sách đầu vào là null hoặc rỗng.
     * Xác thực: Kết quả trả về danh sách trống thay vì bắn ra lỗi.
     */
    @Test
    void saveImagesFailsForNullOrEmpty() {
        // Trường hợp danh sách null
        List<Image> savedImagesNull = imageService.saveImages(null);
        assertNotNull(savedImagesNull);
        assertTrue(savedImagesNull.isEmpty());

        // Trường hợp danh sách rỗng
        List<Image> savedImagesEmpty = imageService.saveImages(new ArrayList<>());
        assertNotNull(savedImagesEmpty);
        assertTrue(savedImagesEmpty.isEmpty());
    }

    /**
     * Ca kiểm thử: Hệ thống bỏ qua các phần tử null hoặc trống trong danh sách Base64.
     */
    @Test
    void saveImagesSkipsNullOrBlankBase64() {
        List<String> base64Images = new ArrayList<>();
        base64Images.add(null);
        base64Images.add("   ");
        base64Images.add(VALID_BASE64_IMAGE);

        List<Image> savedImages = imageService.saveImages(base64Images);

        // Chỉ lưu thành công phần tử hợp lệ thứ 3
        assertNotNull(savedImages);
        assertEquals(1, savedImages.size());

        createdFilePaths.add(savedImages.get(0).getFilePath());
    }

    /**
     * Ca kiểm thử: Chuyển đổi ngược file ảnh trên đĩa thành chuỗi Base64 thành công.
     */
    @Test
    void getBase64ImageSuccessfully() {
        List<String> base64Images = new ArrayList<>();
        base64Images.add(VALID_BASE64_IMAGE);

        List<Image> savedImages = imageService.saveImages(base64Images);
        Image image = savedImages.get(0);
        createdFilePaths.add(image.getFilePath());

        // Đọc lại file ảnh dưới dạng chuỗi Base64
        String base64Result = imageService.getBase64Image(image.getFilePath());

        assertNotNull(base64Result);
        assertFalse(base64Result.isBlank());
        // Do có tích hợp chức năng resize ảnh trong ImageService nên chuỗi trả về có thể khác chuỗi gốc một chút,
        // nhưng phải đảm bảo chuyển đổi thành công không trả về null.
    }

    /**
     * Ca kiểm thử: Trả về null khi đọc chuỗi Base64 của một đường dẫn không tồn tại.
     */
    @Test
    void getBase64ImageReturnsNullForNonExistentFile() {
        String nonExistentPath = "uploads/non_existent_image_12345.png";
        String base64Result = imageService.getBase64Image(nonExistentPath);
        assertNull(base64Result, "Yêu cầu đọc file không tồn tại phải trả về null.");
    }

    /**
     * Ca kiểm thử: Xóa thành công file ảnh trên đĩa.
     */
    @Test
    void deleteImageFileSuccessfully() {
        List<String> base64Images = new ArrayList<>();
        base64Images.add(VALID_BASE64_IMAGE);

        List<Image> savedImages = imageService.saveImages(base64Images);
        Image image = savedImages.get(0);
        Path path = Paths.get(image.getFilePath());
        assertTrue(Files.exists(path));

        // Thực hiện xóa file
        imageService.deleteImageFile(image.getFilePath());

        // Xác thực file không còn tồn tại trên đĩa cứng
        assertFalse(Files.exists(path), "File phải được xóa hoàn toàn khỏi đĩa cứng.");
    }

    /**
     * Ca kiểm thử: Gọi xóa một file không tồn tại trên đĩa cứng.
     * Xác thực: Xử lý ngoại lệ trơn tru, không gây lỗi treo chương trình.
     */
    @Test
    void deleteImageFileHandlesNonExistentGracefully() {
        String nonExistentPath = "uploads/non_existent_image_12345.png";
        assertDoesNotThrow(() -> {
            imageService.deleteImageFile(nonExistentPath);
        }, "Xóa file không tồn tại phải xử lý an toàn không bắn ra ngoại lệ.");
    }
}
