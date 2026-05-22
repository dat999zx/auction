package com.bidify.server.service;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdatePasswordRequest;
import com.bidify.common.model.UpdateProfileRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.UserDao;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.model.Image;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;
import com.bidify.server.utility.ServiceUtil;
import com.bidify.server.utility.UserMapper;

import com.bidify.common.dto.PublicProfileDto;
import com.bidify.common.dto.PublicProfileStatsDto;
import com.bidify.common.dto.AuctionDto;
import com.bidify.common.model.PublicProfileRequest;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Item;
import com.bidify.server.utility.AuctionMapper;
import java.util.ArrayList;
import java.util.List;

public class UserProfileService {
    private static UserProfileService instance = new UserProfileService();
    private final UserDao userDao = UserDao.getInstance();
    private final ImageDao imageDao = ImageDao.getInstance();
    private final ImageService imageService = ImageService.getInstance();

    // dùng để tạo một đối tượng UserProfileService
    private UserProfileService() {}

    // dùng để lấy đối tượng Singleton
    public static UserProfileService getInstance() { return instance; }

    // dùng để khởi tạo
    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.GET_PROFILE, (client, req) -> getProfile(client));
        router.register(RequestType.UPDATE_PROFILE, this::updateProfile);
        router.register(RequestType.UPDATE_PASSWORD, this::updatePassword);
        router.register(RequestType.GET_PUBLIC_PROFILE, this::getPublicProfile);
    }

    public Response getProfile(ClientHandler client) {
        return ServiceUtil.handleRequest(() -> {
            User user = ServiceUtil.getOrLoadUser(client.getCurrentUsername());
            return new Response(RequestStatus.SUCCESS, "Profile loaded successfully", UserMapper.toDto(user));
        });
    }

    // dùng để cập nhật thông tin tài khoản
    public Response updateProfile(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            UpdateProfileRequest data = JsonUtil.fromMap(request.getData(), UpdateProfileRequest.class);
            ServiceUtil.validateRequestData(data);

            User user = ServiceUtil.getOrLoadUser(client.getCurrentUsername());

            boolean hasChange = false;

            String nickname = data.getNickname();
            if (nickname != null) {
                ValidationUtil.validateNickname(nickname);
                user.setNickname(nickname.trim());
                hasChange = true;
            }

            String profileImageBase64 = data.getProfileImageBase64();
            if (profileImageBase64 != null) {
                replaceProfileImage(user, profileImageBase64);
                hasChange = true;
            }

            String email = normalizeOptionalProfileField(data.getEmail());
            if (data.getEmail() != null) {
                if (email != null) {
                    ValidationUtil.validateEmail(email);
                }
                user.setEmail(email);
                hasChange = true;
            }

            String phoneNumber = normalizeOptionalProfileField(data.getPhoneNumber());
            if (data.getPhoneNumber() != null) {
                if (phoneNumber != null) {
                    ValidationUtil.validatePhone(phoneNumber);
                }
                user.setPhoneNumber(phoneNumber);
                hasChange = true;
            }

            if (!hasChange)
                throw new ValidationException("No profile changes were provided");

            userDao.save(user, false);
            return new Response(RequestStatus.SUCCESS, "Profile updated successfully", UserMapper.toDto(user));
        });
    }

    private String normalizeOptionalProfileField(String value) {
        if (value == null) return null;

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    // dùng để thay thế ảnh đại diện
    private void replaceProfileImage(User user, String base64) throws DatabaseException {
        var savedImages = imageService.saveImages(List.of(base64));
        if (savedImages.isEmpty())
            throw new ValidationException("Profile image could not be processed");

        Image newImage = savedImages.getFirst();
        imageDao.create(newImage);

        String oldImageId = user.getProfileImageId();
        user.setProfileImageId(newImage.getId());
        userDao.save(user, false);

        if (oldImageId != null && !oldImageId.isBlank()) {
            Image oldImage = imageDao.findById(oldImageId);
            if (oldImage != null)
                imageService.deleteImageFile(oldImage.getFilePath());
            imageDao.deleteById(oldImageId);
        }
    }

    // dùng để cập nhật mật khẩu
    public Response updatePassword(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            UpdatePasswordRequest data = JsonUtil.fromMap(request.getData(), UpdatePasswordRequest.class);
            ServiceUtil.validateRequestData(data);
            ServiceUtil.requireSession(client);

            User user = ServiceUtil.getOrLoadUser(client.getCurrentUsername());

            String currentPassword = data.getCurrentPassword();
            String newPassword = data.getNewPassword();

            ValidationUtil.validatePassword(currentPassword);
            ValidationUtil.validatePassword(newPassword);

            if (!PasswordUtil.matches(currentPassword, user.getPassword()))
                throw new ValidationException("Incorrect password");

            if (currentPassword.equals(newPassword))
                throw new ValidationException("New password must be different from current password");

            user.setPassword(PasswordUtil.hash(newPassword));
            userDao.save(user, false);

            return new Response(RequestStatus.SUCCESS, "Password updated successfully");
        });
    }

    // dùng để lấy thông tin hồ sơ công khai
    public Response getPublicProfile(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            PublicProfileRequest data = JsonUtil.fromMap(request.getData(), PublicProfileRequest.class);
            ServiceUtil.validateRequestData(data);

            String username = data.getUsername();
            if (username == null || username.isBlank()) {
                throw new ValidationException("Username cannot be empty");
            }

            User user = userDao.findByUsername(username);
            if (user == null) {
                throw new ValidationException("User not found");
            }

            String profileImageBase64 = null;
            if (user.getProfileImageId() != null && !user.getProfileImageId().isBlank()) {
                try {
                    Image image = imageDao.findById(user.getProfileImageId());
                    if (image != null) {
                        profileImageBase64 = imageService.getBase64Image(image.getFilePath());
                    }
                } catch (DatabaseException e) {
                    // ignore
                }
            }

            List<Auction> userAuctions = AuctionDao.getInstance().findBySellerUsername(username);
            int totalAuctions = userAuctions.size();
            int activeAuctions = 0;
            int closedAuctions = 0;
            int soldAuctions = 0;
            double activeVolume = 0.0;

            for (Auction a : userAuctions) {
                if (a.getStatus() == com.bidify.common.enums.AuctionStatus.ACTIVE) {
                    activeAuctions++;
                    activeVolume += (a.getCurrentBid() > 0 ? a.getCurrentBid() : a.getStartingPrice());
                } else if (a.getStatus() != com.bidify.common.enums.AuctionStatus.UPCOMING) {
                    closedAuctions++;
                    if (a.getCurrentBidderUsername() != null && !a.getCurrentBidderUsername().isBlank()) {
                        soldAuctions++;
                    }
                }
            }

            int totalBids = BidDao.getInstance().findByUsername(username).size();
            String sellRate = closedAuctions > 0 ? String.format("%.1f%%", (double) soldAuctions / closedAuctions * 100) : "0.0%";

            PublicProfileStatsDto stats = new PublicProfileStatsDto(
                totalAuctions,
                activeAuctions,
                closedAuctions,
                soldAuctions,
                totalBids,
                activeVolume,
                sellRate
            );

            AuctionDto[] auctionDtos = new AuctionDto[userAuctions.size()];
            for (int i = 0; i < userAuctions.size(); i++) {
                auctionDtos[i] = AuctionService.getInstance().toAuctionDto(userAuctions.get(i), false);
            }

            PublicProfileDto publicProfile = new PublicProfileDto(
                user.getUsername(),
                user.getNickname(),
                profileImageBase64,
                user.getEmail(),
                user.getPhoneNumber(),
                stats,
                auctionDtos
            );

            return new Response(RequestStatus.SUCCESS, "Public profile loaded successfully", publicProfile);
        });
    }
}
