package com.bidify.utility;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.model.Response;
import com.bidify.controller.MissionBarController;
import com.bidify.model.ClientSession;
import com.bidify.network.SocketClient;
import com.bidify.service.AuthClientService;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

// Utility class Ä‘á»ƒ setup mission bar cho cÃ¡c controller khÃ¡c nhau, giÃºp giáº£m thiá»ƒu code trÃ¹ng láº·p vÃ  Ä‘áº£m báº£o tÃ­nh nháº¥t quÃ¡n trong cÃ¡ch thiáº¿t láº­p mission bar trÃªn toÃ n á»©ng dá»¥ng.
public class MissionBarUtil {
    private static final Logger logger = LoggerFactory.getLogger(MissionBarUtil.class);
    private static final AuthClientService authClientService = new AuthClientService();
    private static final ClientSession clientSession = ClientSession.getInstance();

    // dùng để mission bar tiện ích
    private MissionBarUtil() {}

    private static Runnable currentCleanupAction;

/*
3 tham sá»‘:
- activePage: NavPage enum Ä‘á»ƒ xÃ¡c Ä‘á»‹nh page nÃ o Ä‘ang active, giÃºp mission bar highlight Ä‘Ãºng page.
- showSearch: boolean Ä‘á»ƒ quyáº¿t Ä‘á»‹nh cÃ³ hiá»ƒn thá»‹ search bar hay khÃ´ng.
- searchHandler: EventHandler Ä‘á»ƒ xá»­ lÃ½ sá»± kiá»‡n khi ngÆ°á»i dÃ¹ng thá»±c hiá»‡n tÃ¬m kiáº¿m (náº¿u showSearch = true).
*/

    // dùng để cấu hình thanh menu (mission bar) cho màn hình hiện tại
    public static void setup(NavPage activePage, boolean showSearch, EventHandler<ActionEvent> searchHandler) {
        // dùng để setup
        setup(activePage, showSearch, searchHandler, null);
    }

    // dùng để cấu hình thanh menu (mission bar) kèm theo hàm dọn dẹp tài nguyên
    public static void setup(NavPage activePage, boolean showSearch, EventHandler<ActionEvent> searchHandler, Runnable cleanupAction) {
        currentCleanupAction = cleanupAction;
        MissionBarController missionBarController = SceneManager.getMissionBarController();
        if (missionBarController == null) {
            logger.warn("Mission bar was not loaded.");
            return;
        }

        // set cho cÃ¡c thuá»™c tÃ­nh chung cá»§a mission bar
        missionBarController.setShowExplore(true);
        missionBarController.setShowSearch(showSearch);
        missionBarController.setUseInlineLogout(true);
        missionBarController.setShowAdminControls(clientSession.isAdmin());
        missionBarController.setShowCreateAuction(!clientSession.isAdmin());

        // set function cho search bar náº¿u cÃ³, náº¿u khÃ´ng thÃ¬ bá» qua
        if (showSearch && searchHandler != null) {
            missionBarController.getSearchBar().setOnAction(searchHandler);
        } else {
            missionBarController.getSearchBar().setOnAction(null);
        }

        // Xá»­ lÃ½ sá»± kiá»‡n cho cÃ¡c button trÃªn mission bar
        missionBarController.setSelectionHandler(MissionBarUtil::handleNavigation);
        missionBarController.setExploreHandler(event -> missionBarController.toggleSidebar());
        missionBarController.setLogoutHandler(event -> handleLogout());
        missionBarController.setAvatarHandler(event -> {
            if (currentCleanupAction != null) currentCleanupAction.run();
            SceneManager.switchScene("user-profile.fxml", false, true);
        });

        // set avatar text lÃ  chá»¯ Ä‘áº§u cá»§a username
        missionBarController.setAvatarText(resolveAvatarLetter());

        // Ä‘Ã¡nh dáº¥u page hiá»‡n táº¡i Ä‘ang active trÃªn mission bar
        // dùng để thiết lập active trang
        setActivePage(missionBarController, activePage);
    }

    // xá»­ lÃ½ sá»± kiá»‡n khi ngÆ°á»i dÃ¹ng click vÃ o cÃ¡c button trÃªn mission bar
    // dùng để xử lý sự kiện chuyển trang khi người dùng nhấn nút trên thanh menu
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
        else if (selectedButton == controller.getInventoryButton()) {
            if (currentCleanupAction != null) currentCleanupAction.run();
            SceneManager.switchScene("inventory.fxml", false, true);
        } 
        else if (selectedButton == controller.getHistoryButton()) {
            if (currentCleanupAction != null) currentCleanupAction.run();
            SceneManager.switchScene("history.fxml", false, true);
        }
        else if (selectedButton == controller.getSettlementsButton()) {
            if (currentCleanupAction != null) currentCleanupAction.run();
            SceneManager.switchScene("settlements.fxml", false, true);
        }
        else if (selectedButton == controller.getAdminUsersButton()) {
            if (currentCleanupAction != null) currentCleanupAction.run();
            SceneManager.switchScene("admin-users.fxml", false, true);
        }
        else if (selectedButton == controller.getAdminWalletRequestsButton()) {
            if (currentCleanupAction != null) currentCleanupAction.run();
            SceneManager.switchScene("admin-wallet-requests.fxml", false, true);
        }
        else if (selectedButton == controller.getLogoutLinkButton()) handleLogout();
    }

    // dùng để gửi yêu cầu đăng xuất đến server và xóa session client
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

    // dùng để tô sáng (highlight) mục menu tương ứng với trang đang hiển thị
    private static void setActivePage(MissionBarController controller, NavPage activePage) {
        if (activePage == null || activePage == NavPage.NONE) {
            controller.setActiveNavigation(null);
            return;
        }

        // dáº¥u hiá»‡u cá»§a page nÃ o Ä‘ang active sáº½ Ä‘Æ°á»£c highlight trÃªn mission bar
        switch (activePage) {
            case HOME -> controller.setActiveNavigation(controller.getAuctionsButton());
            case CREATE_AUCTION -> controller.setActiveNavigation(controller.getCreateAuctionButton());
            case INVENTORY -> controller.setActiveNavigation(controller.getInventoryButton());
            case HISTORY -> controller.setActiveNavigation(controller.getHistoryButton());
            case SETTLEMENTS -> controller.setActiveNavigation(controller.getSettlementsButton());
            case ADMIN_USERS -> controller.setActiveNavigation(controller.getAdminUsersButton());
            case ADMIN_WALLET_REQUESTS -> controller.setActiveNavigation(controller.getAdminWalletRequestsButton());
            default -> controller.setActiveNavigation(null);
        }
    }

    // dùng để lấy chữ cái đầu tiên của tên đăng nhập làm avatar
    private static String resolveAvatarLetter() {
        String username = SocketClient.getClient().getCurrentUsername();
        if (username == null || username.isBlank()) return "U";
        return username.substring(0, 1).toUpperCase();
    }
}
