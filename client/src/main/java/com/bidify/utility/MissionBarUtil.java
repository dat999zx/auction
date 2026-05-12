package com.bidify.utility;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.model.Response;
import com.bidify.controller.MissionBarController;
import com.bidify.network.SocketClient;
import com.bidify.service.AuthClientService;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

// Utility class để setup mission bar cho các controller khác nhau, giúp giảm thiểu code trùng lặp và đảm bảo tính nhất quán trong cách thiết lập mission bar trên toàn ứng dụng.
public class MissionBarUtil {
    private static final Logger logger = LoggerFactory.getLogger(MissionBarUtil.class);
    private static final AuthClientService authClientService = new AuthClientService();

    private MissionBarUtil() {}

    private static Runnable currentCleanupAction;

/*
3 tham số:
- activePage: NavPage enum để xác định page nào đang active, giúp mission bar highlight đúng page.
- showSearch: boolean để quyết định có hiển thị search bar hay không.
- searchHandler: EventHandler để xử lý sự kiện khi người dùng thực hiện tìm kiếm (nếu showSearch = true).
*/

    public static void setup(NavPage activePage, boolean showSearch, EventHandler<ActionEvent> searchHandler) {
        setup(activePage, showSearch, searchHandler, null);
    }

    public static void setup(NavPage activePage, boolean showSearch, EventHandler<ActionEvent> searchHandler, Runnable cleanupAction) {
        currentCleanupAction = cleanupAction;
        MissionBarController missionBarController = SceneManager.getMissionBarController();
        if (missionBarController == null) {
            logger.warn("Mission bar was not loaded.");
            return;
        }

        // set cho các thuộc tính chung của mission bar
        missionBarController.setShowExplore(true);
        missionBarController.setShowSearch(showSearch);
        missionBarController.setUseInlineLogout(true);

        // set function cho search bar nếu có, nếu không thì bỏ qua
        if (showSearch && searchHandler != null) {
            missionBarController.getSearchBar().setOnAction(searchHandler);
        } else {
            missionBarController.getSearchBar().setOnAction(null);
        }

        // Xử lý sự kiện cho các button trên mission bar
        missionBarController.setSelectionHandler(MissionBarUtil::handleNavigation);
        missionBarController.setExploreHandler(event -> missionBarController.toggleSidebar());
        missionBarController.setLogoutHandler(event -> handleLogout());
        missionBarController.setAvatarHandler(event -> {
            if (currentCleanupAction != null) currentCleanupAction.run();
            SceneManager.switchScene("user-profile.fxml", false, true);
        });

        // set avatar text là chữ đầu của username
        missionBarController.setAvatarText(resolveAvatarLetter());

        // Set Active Page highlighting
        setActivePage(missionBarController, activePage);
    }

    // xử lý sự kiện khi người dùng click vào các button trên mission bar
    private static void handleNavigation(ActionEvent event) {
        if (!(event.getSource() instanceof Button selectedButton)) return;

        MissionBarController controller = SceneManager.getMissionBarController();
        if (controller == null) return;

        if (selectedButton == controller.getAuctionsButton()) {
            if (currentCleanupAction != null) currentCleanupAction.run();
            SceneManager.switchScene("hub.fxml", false, true);
        } 
        else if (selectedButton == controller.getCreateAuctionButton()) {
            if (currentCleanupAction != null) currentCleanupAction.run();
            SceneManager.switchScene("create-auction.fxml", false, true);
        } 
        else if (selectedButton == controller.getHistoryButton()) {
            if (currentCleanupAction != null) currentCleanupAction.run();
            SceneManager.switchScene("history.fxml", false, true);
        } 
        else if (selectedButton == controller.getLogoutLinkButton()) handleLogout();
    }

    private static void handleLogout() {
        try {
            Response response = authClientService.logout();
            if (response.getStatus() == RequestStatus.SUCCESS) {
                NotificationUtil.success("Logged out successfully.");
                if (currentCleanupAction != null) currentCleanupAction.run();
                SceneManager.clearAllCache();
                SceneManager.switchScene("login.fxml", true, false);
                return;
            }
            NotificationUtil.error(response.getMessage() == null ? "Logout failed." : response.getMessage());
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Logout exception", e);
        } catch (Exception e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    private static void setActivePage(MissionBarController controller, NavPage activePage) {
        if (activePage == null || activePage == NavPage.NONE) {
            controller.setActiveNavigation(null);
            return;
        }

        switch (activePage) {
            case HOME -> controller.setActiveNavigation(controller.getAuctionsButton());
            case CREATE_AUCTION -> controller.setActiveNavigation(controller.getCreateAuctionButton());
            case HISTORY -> controller.setActiveNavigation(controller.getHistoryButton());
            default -> controller.setActiveNavigation(null);
        }
    }

    // Lấy chữ cái đầu tiên của username để hiển thị trên avatar, nếu không có username thì hiển thị "U" mặc định.
    private static String resolveAvatarLetter() {
        String username = SocketClient.getClient().getCurrentUsername();
        if (username == null || username.isBlank()) {
            return "U";
        }
        return username.substring(0, 1).toUpperCase();
    }
}
