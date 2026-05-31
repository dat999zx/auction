package com.bidify.controller;

import java.io.IOException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.UserDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Event;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.event.EventManager;
import com.bidify.model.ClientSession;
import com.bidify.service.UserProfileClientService;
import com.bidify.navigation.CleanableController;
import com.bidify.navigation.MissionBarUtil;
import com.bidify.navigation.NavPage;
import com.bidify.ui.NotificationUtil;
import com.bidify.navigation.SceneManager;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.bidify.media.ImageCache;
import javafx.scene.shape.Circle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class UserProfileController implements CleanableController {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);
    private final ClientSession clientSession = ClientSession.getInstance();

    @FXML
    private Label usernameValueLabel;



    @FXML
    private Label memberStatusLabel;

    @FXML
    private Label profileImageHintLabel;

    @FXML
    private Label profileAvatarLabel;

    @FXML
    private ImageView profileAvatarImageView;

    @FXML
    private ImageView heroAvatarImageView;

    @FXML
    private TextField nicknameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneNumberField;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    private final UserProfileClientService userProfileClientService = new UserProfileClientService();

    // Store listener references so unsubscribe can remove the exact same object.
    private final Consumer<Event> onServerNotice = e -> Platform.runLater(() -> NotificationUtil.info(e.getMessage()));

    @FXML
    private void initialize() {
        Circle profileClip = new Circle(57, 57, 57);
        profileAvatarImageView.setClip(profileClip);

        Circle heroClip = new Circle(46, 46, 46);
        heroAvatarImageView.setClip(heroClip);

        Platform.runLater(() -> {
            bindTopBar();
            populateProfile();
        });

        EventManager.getInstance().subscribe(EventType.SERVER_NOTICE, onServerNotice);
    }

    public void cleanup() {
        EventManager.getInstance().unsubscribe(EventType.SERVER_NOTICE, onServerNotice);
    }

    @FXML
    private void handleSaveProfile() {
        try {
            UserDto updatedUser = userProfileClientService.updateProfile(
                nicknameField.getText(),
                emailField.getText(),
                phoneNumberField.getText(),
                null
            );
            refreshProfile(updatedUser);
            NotificationUtil.success("Profile updated successfully.");
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }



    @FXML
    private void handleChangePassword() {
        try {
            userProfileClientService.changePassword(
                currentPasswordField.getText(),
                newPasswordField.getText(),
                confirmPasswordField.getText()
            );
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            NotificationUtil.success("Password updated successfully.");
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    @FXML
    private void handleProfileImagePlaceholder() {
        NotificationUtil.info("Profile image upload is a UI placeholder right now.");
    }

    @FXML
    private void handleProfileImageUpload() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(
            nicknameField != null && nicknameField.getScene() != null ? nicknameField.getScene().getWindow() : null
        );
        if (file == null)
            return;

        try {
            String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
            UserDto updatedUser = userProfileClientService.updateProfile(
                nicknameField.getText(),
                emailField.getText(),
                phoneNumberField.getText(),
                base64
            );
            refreshProfile(updatedUser);
            profileImageHintLabel.setText("Profile image updated");
            NotificationUtil.success("Profile image updated successfully.");
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        } catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    private void populateProfile() {
        try {
            refreshProfile(userProfileClientService.getCurrentProfile());
        } catch (IOException e) {
            refreshProfile(userProfileClientService.getCachedProfile());
            NotificationUtil.error("Cannot connect to server.");
        } catch (ValidationException e) {
            refreshProfile(userProfileClientService.getCachedProfile());
            NotificationUtil.error(e.getMessage());
        }

        profileImageHintLabel.setText("Add photo");
    }



    private void refreshProfile(UserDto user) {
        usernameValueLabel.setText(DisplayUtil.defaultText(user.getUsername(), "Unknown"));
        nicknameField.setText(DisplayUtil.defaultText(user.getNickname(), user.getUsername()));
        emailField.setText(DisplayUtil.defaultText(user.getEmail(), ""));
        phoneNumberField.setText(DisplayUtil.defaultText(user.getPhoneNumber(), ""));
        
        memberStatusLabel.setText(clientSession.isAdmin() ? "Administrator" : "Active bidder");
        String avatarLetter = resolveAvatarLetter(user.getNickname(), user.getUsername());
        profileAvatarLabel.setText(avatarLetter);
        
        renderProfileAvatar(user);
        
        var controller = SceneManager.getMissionBarController();
        if (controller != null) {
            String base64 = user.getProfileImageBase64();
            String cacheKey = "mission_avatar_" + user.getUsername() + "_" + (base64 == null ? 0 : base64.hashCode());
            Image avatarImage = ImageCache.getInstance().get(cacheKey, base64);
            controller.setAvatarImage(avatarImage);
            controller.setAvatarText(avatarLetter);
        }
    }

    private void renderProfileAvatar(UserDto user) {
        String base64 = user == null ? null : user.getProfileImageBase64();
        String cacheKey = "profile_" + (user == null ? "guest" : user.getUsername()) + "_" + (base64 == null ? 0 : base64.hashCode());
        Image image = ImageCache.getInstance().get(cacheKey, base64);
        boolean hasImage = image != null;

        profileAvatarImageView.setImage(hasImage ? image : null);
        heroAvatarImageView.setImage(hasImage ? image : null);
        profileAvatarImageView.setVisible(hasImage);
        heroAvatarImageView.setVisible(hasImage);
        profileAvatarLabel.setVisible(!hasImage);
        profileImageHintLabel.setVisible(!hasImage);
    }

    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.PROFILE, false, null, this::cleanup);
    }

    private String resolveAvatarLetter(String nickname, String username) {
        String source = nickname;
        if (source == null || source.isBlank()) {
            source = username;
        }
        if (source == null || source.isBlank()) {
            return "U";
        }
        return source.substring(0, 1).toUpperCase();
    }
}
