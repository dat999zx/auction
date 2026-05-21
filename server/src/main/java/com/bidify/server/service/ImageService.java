package com.bidify.server.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.utility.IdGenerator;
import com.bidify.common.utility.ImageUtil;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.model.Image;

public class ImageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    private static final String UPLOAD_DIR = resolveUploadPath();
    private static final ImageService instance = new ImageService();

    // dùng để tạo một đối tượng ImageService
    private ImageService() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        }
        catch (IOException e) {
            logger.error("Could not create upload directory", e);
        }
    }

    // dùng để lấy đối tượng Singleton
    public static ImageService getInstance() { return instance; }

    // dùng để giải quyết upload path
    private static String resolveUploadPath() {
        Path curPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path serverDir = curPath.resolve("server");

        if (Files.exists(serverDir) && Files.isDirectory(serverDir))
            return serverDir.resolve("uploads").toString();

        return curPath.resolve("uploads").toString();
    }

    // dùng để lưu images
    public List<Image> saveImages(List<String> base64Images) {
        if (base64Images == null || base64Images.isEmpty())
            return new ArrayList<>();

        List<Image> savedImages = new ArrayList<>();
        Path uploadDir = Paths.get(UPLOAD_DIR);

        try {
            Files.createDirectories(uploadDir);
            for (String base64 : base64Images) {
                if (base64 == null || base64.isBlank())
                    continue;

                byte[] imageBytes = Base64.getDecoder().decode(base64);
                imageBytes = ImageUtil.resizeImage(imageBytes, 800);
                String imageId = IdGenerator.genImageId();
                Path filePath = uploadDir.resolve(imageId + ".png");
                Files.write(filePath, imageBytes);
                savedImages.add(new Image(imageId, TimeUtil.nowInVietnam(), filePath.toString()));
            }
        }
        catch (IOException e) {
            logger.error("Error saving images", e);
        }
        catch (IllegalArgumentException e) {
            logger.error("Error decoding images", e);
        }

        return savedImages;
    }

    public String getBase64Image(String filePath) {
        if (filePath == null) return null;
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                byte[] bytes = Files.readAllBytes(path);
                return Base64.getEncoder().encodeToString(bytes);
            }
        }
        catch (IOException e) {
            logger.error("Error reading image file: " + filePath, e);
        }
        return null;
    }

    // dùng để xóa hình ảnh file
    public void deleteImageFile(String filePath) {
        if (filePath == null || filePath.isBlank())
            return;

        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path))
                Files.delete(path);
        }
        catch (IOException e) {
            logger.error("Error deleting image file: " + filePath, e);
        }
    }
}
