package com.bidify.common.utility;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ImageUtil {
    private ImageUtil() {}

    public static String extensionFor(byte[] imageData) {
        if (imageData != null
            && imageData.length >= 8
            && (imageData[0] & 0xFF) == 0x89
            && imageData[1] == 'P'
            && imageData[2] == 'N'
            && imageData[3] == 'G')
            return "png";
        if (imageData != null
            && imageData.length >= 3
            && (imageData[0] & 0xFF) == 0xFF
            && (imageData[1] & 0xFF) == 0xD8
            && (imageData[2] & 0xFF) == 0xFF)
            return "jpg";
        return "png";
    }

    // Tự động resize ảnh nếu kích thước vượt quá giới hạn tối đa.
    public static byte[] resizeImage(byte[] imageData, int maxDimension) throws IOException {
        if (imageData == null || imageData.length == 0) return imageData;

        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        BufferedImage originalImage = ImageIO.read(bais);
        if (originalImage == null) return imageData;

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Bỏ qua nếu ảnh đã nhỏ hơn kích thước giới hạn.
        if (width <= maxDimension && height <= maxDimension) return imageData;

        double ratio = (double) width / height;
        int newWidth, newHeight;

        if (width > height) {
            newWidth = maxDimension;
            newHeight = (int) (maxDimension / ratio);
        }
        else {
            newHeight = maxDimension;
            newWidth = (int) (maxDimension * ratio);
        }

        // Xác định hệ màu (RGB hoặc ARGB) để giữ độ trong suốt.
        int type = (originalImage.getTransparency() == Transparency.OPAQUE) ?
            BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, type);
        Graphics2D g = resizedImage.createGraphics();
        
        if (type == BufferedImage.TYPE_INT_RGB) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, newWidth, newHeight);
        }

        // Vẽ lại ảnh với chất lượng tốt nhất.
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String format = (type == BufferedImage.TYPE_INT_RGB) ? "jpg" : "png";
        ImageIO.write(resizedImage, format, baos);
        return baos.toByteArray();
    }
}
