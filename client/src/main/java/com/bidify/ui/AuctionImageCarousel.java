package com.bidify.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * Manages multi-image carousel state and rendering for auction detail view.
 * Owns the image list and current index; delegates display updates to the
 * FXML-bound UI nodes provided at bind time.
 */
public class AuctionImageCarousel {
    private final List<Image> images = new ArrayList<>();
    private int currentIndex = 0;

    private ImageView previewImage;
    private Button prevButton;
    private Button nextButton;
    private HBox thumbnailContainer;
    private String defaultImagePath;

    public void bind(ImageView previewImage, Button prevButton, Button nextButton,
                     HBox thumbnailContainer, String defaultImagePath) {
        this.previewImage = previewImage;
        this.prevButton = prevButton;
        this.nextButton = nextButton;
        this.thumbnailContainer = thumbnailContainer;
        this.defaultImagePath = defaultImagePath;
    }

    public void reset() {
        images.clear();
        currentIndex = 0;
    }

    public void addImage(Image image) {
        if (image != null) images.add(image);
    }

    public List<Image> getImages() {
        return images;
    }

    public void navigatePrev() {
        currentIndex--;
        updateDisplay();
    }

    public void navigateNext() {
        currentIndex++;
        updateDisplay();
    }

    public void updateDisplay() {
        if (previewImage == null) return;

        if (images.isEmpty()) {
            setFallbackImage();
            setNavVisible(false);
            renderThumbnails();
            return;
        }

        if (currentIndex < 0) currentIndex = images.size() - 1;
        else if (currentIndex >= images.size()) currentIndex = 0;

        previewImage.setImage(images.get(currentIndex));

        boolean showNav = images.size() > 1;
        setNavVisible(showNav);
        renderThumbnails();
    }

    private void setFallbackImage() {
        if (previewImage == null || defaultImagePath == null) return;
        try {
            var resource = getClass().getResource(defaultImagePath.startsWith("/") ? defaultImagePath : "/" + defaultImagePath);
            if (resource != null) {
                previewImage.setImage(new Image(resource.toExternalForm(), true));
            } else {
                previewImage.setImage(new Image(getClass().getResourceAsStream(defaultImagePath)));
            }
        } catch (Exception ignored) {}
    }

    private void setNavVisible(boolean visible) {
        if (prevButton != null) {
            prevButton.setVisible(visible);
            prevButton.setManaged(visible);
        }
        if (nextButton != null) {
            nextButton.setVisible(visible);
            nextButton.setManaged(visible);
        }
    }

    private void renderThumbnails() {
        if (thumbnailContainer == null) return;
        thumbnailContainer.getChildren().clear();

        if (images.size() <= 1) {
            thumbnailContainer.setVisible(false);
            thumbnailContainer.setManaged(false);
            return;
        }

        thumbnailContainer.setVisible(true);
        thumbnailContainer.setManaged(true);

        for (int i = 0; i < images.size(); i++) {
            final int index = i;
            Image img = images.get(i);

            StackPane thumbPane = new StackPane();
            thumbPane.getStyleClass().add("thumb-card");
            thumbPane.setPrefSize(80, 80);

            if (i == currentIndex) {
                thumbPane.setStyle("-fx-border-color: #00458f; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px;");
            } else {
                thumbPane.setStyle("-fx-border-color: #d8e3fb; -fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px;");
            }

            ImageView thumbView = new ImageView(img);
            thumbView.setFitHeight(72);
            thumbView.setFitWidth(72);
            thumbView.setPreserveRatio(true);
            thumbView.setSmooth(true);

            thumbPane.getChildren().add(thumbView);
            thumbPane.setOnMouseClicked(e -> {
                currentIndex = index;
                updateDisplay();
            });
            thumbPane.setStyle(thumbPane.getStyle() + " -fx-cursor: hand;");

            thumbnailContainer.getChildren().add(thumbPane);
        }
    }
}
