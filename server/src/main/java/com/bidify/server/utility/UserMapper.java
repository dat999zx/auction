package com.bidify.server.utility;

import com.bidify.common.dto.UserDto;
import com.bidify.server.model.User;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.model.Image;
import com.bidify.server.service.ImageService;
import com.bidify.server.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Chuyển đổi object User (server) → UserDto (gửi cho client), lọc bỏ dữ liệu nhạy cảm
public class UserMapper {
    private static final Logger logger = LoggerFactory.getLogger(UserMapper.class);

    private UserMapper() {}

    public static UserDto toDto(User user) {
        if (user == null) return null;

        String profileImageBase64 = null;
        if (user.getProfileImageId() != null && !user.getProfileImageId().isBlank()) {
            try {
                Image image = ImageDao.getInstance().findById(user.getProfileImageId());
                if (image != null) {
                    profileImageBase64 = ImageService.getInstance().getBase64Image(image.getFilePath());
                }
            } catch (DatabaseException e) {
                logger.error("Error loading profile image for user: " + user.getUsername(), e);
            }
        }

        return new UserDto(
            user.getUsername(),
            user.getNickname(),
            WalletMapper.toDto(user.getWallet()),
            user.getRole(),
            profileImageBase64,
            user.getEmail(),
            user.getPhoneNumber()
        );
    }
}
