package com.bidify.server.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.utility.ImageUtil;

public class ImageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    private static final String UPLOAD_DIR = resolveUploadPath();
    private static final ImageService instance = new ImageService();

    private ImageService() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        }
        catch (IOException e) {
            logger.error("Could not create upload directory", e);
        }
    }

    public static ImageService getInstance() { return instance; }

    private static String resolveUploadPath() {
        Path curPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path serverDir = curPath.resolve("server");

        if (Files.exists(serverDir) && Files.isDirectory(serverDir))
            return serverDir.resolve("uploads").toString();

        return curPath.resolve("uploads").toString();
    }

    public List<String> saveImages(String auctionId, List<String> base64Images) {
        if (base64Images == null || base64Images.isEmpty())
            return new ArrayList<>();

        List<String> savedPaths = new ArrayList<>();
        Path auctionDir = Paths.get(UPLOAD_DIR, auctionId);

        try {
            Files.createDirectories(auctionDir);
            for (int i = 0; i < base64Images.size(); i++) {
                String base64 = base64Images.get(i);
                String fileName = "img_" + i + ".png";
                Path filePath = auctionDir.resolve(fileName);
                
                byte[] imageBytes = Base64.getDecoder().decode(base64);
                imageBytes = ImageUtil.resizeImage(imageBytes, 800);
                Files.write(filePath, imageBytes);
                savedPaths.add(filePath.toString());
            }
        }
        catch (IOException e) {
            logger.error("Error saving images for auction: " + auctionId, e);
        }

        return savedPaths;
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
}
