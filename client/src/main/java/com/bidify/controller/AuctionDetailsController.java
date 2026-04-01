package com.bidify.controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.GetAuctionDetailRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.network.SocketClient;
import com.bidify.utility.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AuctionDetailsController {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    private static final String DEFAULT_PREVIEW_IMAGE = "/images/bidify-logo.png";

    private static String selectedAuctionId;

    @FXML
    private Label name;
    @FXML
    private Label description;
    @FXML
    private Label startprice;
    @FXML
    private Label currentprice;
    @FXML
    private Label enddate;
    @FXML
    private TextField inputprice;
    @FXML
    private Button placebid;
    @FXML
    private ImageView previewimage;
    @FXML
    private Label messageLabel;

    private double currentDisplayedPrice;

    public static void openAuctionDetails(String auctionId) {
        selectedAuctionId = auctionId;
        SceneManager.clearCache("auctiondetail.fxml");
        SceneManager.switchScene("auctiondetail.fxml", false);
    }

    @FXML
    private void initialize() {
        setPreviewImage(DEFAULT_PREVIEW_IMAGE);
        resetView();
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) {
            showMessage("No auction selected.", false);
            placebid.setDisable(true);
            return;
        }
        loadAuctionDetails(selectedAuctionId);
    }

    @FXML
    private void attemptplacebid() {
        String rawBid = inputprice.getText() == null ? "" : inputprice.getText().trim().replace(",", "");
        if (rawBid.isBlank()) {
            showMessage("Enter a bid amount first.", false);
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(rawBid);
        } catch (NumberFormatException e) {
            showMessage("Bid amount must be a valid number.", false);
            return;
        }

        if (bidAmount <= currentDisplayedPrice) {
            showMessage("Your bid must be higher than the current price.", false);
            return;
        }

        currentDisplayedPrice = bidAmount;
        currentprice.setText(formatCurrency(bidAmount));
        inputprice.clear();
        showMessage("Bid validated on the client. Server-side PLACE_BID is not wired in this repo yet.", true);
    }

    @FXML
    private void tomenu() {
        SceneManager.switchScene("hub.fxml");
    }

    private void loadAuctionDetails(String auctionId) {
        try {
            Response response = SocketClient.getClient().send(
                new Request(RequestType.GET_AUCTION_DETAIL, new GetAuctionDetailRequest(auctionId))
            );

            if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
                showMessage(response.getMessage() == null ? "Cannot load auction details." : response.getMessage(), false);
                placebid.setDisable(true);
                return;
            }

            Map<?, ?> data = response.getData() instanceof Map<?, ?> map ? map : null;
            if (data == null) {
                showMessage("Auction details came back in an unexpected format.", false);
                placebid.setDisable(true);
                return;
            }

            bindAuctionData(data);
            placebid.setDisable(false);
            showMessage("Auction loaded.", true);
        } catch (IOException e) {
            showMessage("Cannot connect to server.", false);
            placebid.setDisable(true);
            e.printStackTrace();
        }
    }

    private void bindAuctionData(Map<?, ?> data) {
        name.setText(stringValue(data.get("auctionName"), "Untitled auction"));
        description.setText(stringValue(data.get("description"), "No description."));

        double startingValue = doubleValue(data.get("startingPrice"));
        double currentValue = doubleValue(data.get("currentBid"));
        currentDisplayedPrice = currentValue > 0 ? currentValue : startingValue;

        startprice.setText(formatCurrency(startingValue));
        currentprice.setText(formatCurrency(currentDisplayedPrice));
        enddate.setText(formatDate(stringValue(data.get("endTime"), "Unknown")));

        Object imageValue = data.get("previewImage");
        if (imageValue == null) {
            imageValue = data.get("imageUrl");
        }
        setPreviewImage(stringValue(imageValue, DEFAULT_PREVIEW_IMAGE));
    }

    private void resetView() {
        name.setText("Loading auction...");
        description.setText("Please wait while the auction details are fetched.");
        startprice.setText(formatCurrency(0));
        currentprice.setText(formatCurrency(0));
        enddate.setText("Loading...");
        messageLabel.setText("");
        currentDisplayedPrice = 0;
        placebid.setDisable(true);
    }

    private void setPreviewImage(String imagePath) {
        if (previewimage == null) {
            return;
        }

        String resolvedPath = (imagePath == null || imagePath.isBlank()) ? DEFAULT_PREVIEW_IMAGE : imagePath;
        try {
            Image image;
            if (resolvedPath.startsWith("http://") || resolvedPath.startsWith("https://")) {
                image = new Image(resolvedPath, true);
            } else {
                var resource = getClass().getResource(resolvedPath.startsWith("/") ? resolvedPath : "/" + resolvedPath);
                image = resource == null
                    ? new Image(getClass().getResourceAsStream(DEFAULT_PREVIEW_IMAGE))
                    : new Image(resource.toExternalForm(), true);
            }
            previewimage.setImage(image);
        } catch (Exception e) {
            previewimage.setImage(new Image(getClass().getResourceAsStream(DEFAULT_PREVIEW_IMAGE)));
        }
    }

    private void showMessage(String message, boolean success) {
        if (messageLabel == null) {
            return;
        }
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        if (!message.isBlank()) {
            messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
        }
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank() || "Unknown".equals(rawDate)) {
            return "Unknown";
        }
        try {
            return LocalDateTime.parse(rawDate).toString().replace('T', ' ');
        } catch (DateTimeParseException e) {
            return rawDate;
        }
    }
}
