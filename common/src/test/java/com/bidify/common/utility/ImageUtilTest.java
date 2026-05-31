package com.bidify.common.utility;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * Lớp kiểm thử (Unit Test) cho ImageUtil.
 * Các bình luận giải thích bằng tiếng Việt.
 */
public class ImageUtilTest {

    /**
     * Ca kiểm thử: Nhận diện định dạng phần mở rộng ảnh từ bytes.
     */
    @Test
    public void testExtensionFor() {
        // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
        byte[] pngBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        assertEquals("png", ImageUtil.extensionFor(pngBytes));

        // JPG magic bytes: FF D8 FF
        byte[] jpgBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00};
        assertEquals("jpg", ImageUtil.extensionFor(jpgBytes));

        // Định dạng khác hoặc không khớp -> mặc định "png"
        byte[] otherBytes = new byte[]{0x00, 0x01, 0x02, 0x03};
        assertEquals("png", ImageUtil.extensionFor(otherBytes));

        // Null hoặc mảng rỗng -> mặc định "png"
        assertEquals("png", ImageUtil.extensionFor(null));
        assertEquals("png", ImageUtil.extensionFor(new byte[0]));
    }

    /**
     * Ca kiểm thử: Xử lý an toàn khi đầu vào resize là null hoặc rỗng.
     */
    @Test
    public void testResizeImageWithNullOrEmpty() throws IOException {
        assertNull(ImageUtil.resizeImage(null, 100));
        byte[] empty = new byte[0];
        assertSame(empty, ImageUtil.resizeImage(empty, 100));
    }

    /**
     * Ca kiểm thử: Bỏ qua không resize nếu kích thước ảnh nhỏ hơn giới hạn tối đa (Passthrough).
     */
    @Test
    public void testResizeImagePassthroughForSmallImage() throws IOException {
        // Tạo ảnh nhỏ 50x50
        BufferedImage smallImg = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        byte[] smallBytes = toByteArray(smallImg, "png");

        // Gọi resize với giới hạn 100 (lớn hơn 50)
        byte[] result = ImageUtil.resizeImage(smallBytes, 100);

        // Đảm bảo trả về mảng bytes nguyên gốc (hoặc có cùng kích thước)
        assertNotNull(result);
        BufferedImage resultImg = ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(50, resultImg.getWidth());
        assertEquals(50, resultImg.getHeight());
    }

    /**
     * Ca kiểm thử: Thay đổi kích thước ảnh lớn hơn giới hạn tối đa và bảo toàn tỉ lệ khung hình (Aspect Ratio).
     */
    @Test
    public void testResizeImageWithScalingAndAspectRatio() throws IOException {
        // Tạo ảnh lớn nằm ngang 800x400 (Tỉ lệ 2:1)
        BufferedImage landscapeImg = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
        byte[] landscapeBytes = toByteArray(landscapeImg, "png");

        // Resize với giới hạn tối đa là 200
        byte[] resultLandscape = ImageUtil.resizeImage(landscapeBytes, 200);
        BufferedImage resLandscape = ImageIO.read(new ByteArrayInputStream(resultLandscape));

        // Chiều rộng lớn hơn chiều cao -> chiều rộng mới sẽ là 200, chiều cao là 100 (tỉ lệ 2:1)
        assertEquals(200, resLandscape.getWidth());
        assertEquals(100, resLandscape.getHeight());

        // Tạo ảnh lớn thẳng đứng 300x900 (Tỉ lệ 1:3)
        BufferedImage portraitImg = new BufferedImage(300, 900, BufferedImage.TYPE_INT_RGB);
        byte[] portraitBytes = toByteArray(portraitImg, "png");

        // Resize với giới hạn tối đa là 300
        byte[] resultPortrait = ImageUtil.resizeImage(portraitBytes, 300);
        BufferedImage resPortrait = ImageIO.read(new ByteArrayInputStream(resultPortrait));

        // Chiều cao lớn hơn chiều rộng -> chiều cao mới sẽ là 300, chiều rộng là 100 (tỉ lệ 1:3)
        assertEquals(100, resPortrait.getWidth());
        assertEquals(300, resPortrait.getHeight());
    }

    /**
     * Ca kiểm thử: Bảo toàn kênh trong suốt (transparency) ARGB cho ảnh PNG sau khi resize.
     */
    @Test
    public void testResizeImagePreservesTransparency() throws IOException {
        // Tạo một ảnh ARGB kích thước 800x600 có màu nền trong suốt
        BufferedImage argbImg = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argbImg.createGraphics();
        g.setColor(new Color(0, 0, 0, 0)); // trong suốt
        g.fillRect(0, 0, 800, 600);
        g.dispose();

        byte[] originalBytes = toByteArray(argbImg, "png");

        // Gọi resize về giới hạn 400
        byte[] resizedBytes = ImageUtil.resizeImage(originalBytes, 400);
        BufferedImage resImg = ImageIO.read(new ByteArrayInputStream(resizedBytes));

        // Xác nhận ảnh mới vẫn giữ định dạng ARGB có hỗ trợ alpha trong suốt
        assertTrue(resImg.getColorModel().hasAlpha(), "Ảnh kết quả phải giữ kênh alpha trong suốt");
        assertEquals(400, resImg.getWidth());
        assertEquals(300, resImg.getHeight());
    }

    /**
     * Ca kiểm thử: Trả về bytes nguyên bản nếu bytes đầu vào bị hỏng hoặc không phải cấu trúc ảnh.
     */
    @Test
    public void testResizeImageHandlesInvalidBytesGracefully() throws IOException {
        byte[] invalidBytes = new byte[]{1, 2, 3, 4, 5};
        byte[] result = ImageUtil.resizeImage(invalidBytes, 100);
        // Do không parse được thành BufferedImage, hàm phải trả về chính mảng byte đầu vào
        assertSame(invalidBytes, result);
    }

    // Helper method
    private byte[] toByteArray(BufferedImage img, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }
}
