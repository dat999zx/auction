package com.bidify.common.utility;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ImageUtil {
    private ImageUtil() {}

    // resize ảnh nếu như quá to
    public static byte[] resizeImage(byte[] imageData, int maxDimension) throws IOException {
        if (imageData == null || imageData.length == 0) return imageData;

        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        BufferedImage originalImage = ImageIO.read(bais);
        if (originalImage == null) return imageData;

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // nếu ảnh đủ nhỏ thì skip
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

        // lấy loại transparency (độ mờ)
        int type = (originalImage.getTransparency() == Transparency.OPAQUE) ?
            BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, type);
        Graphics2D g = resizedImage.createGraphics();
        
        if (type == BufferedImage.TYPE_INT_RGB) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, newWidth, newHeight);
        }

        // render ảnh lên
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
