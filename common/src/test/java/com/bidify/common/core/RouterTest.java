package com.bidify.common.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Các ca kiểm thử cho lớp Router của module common.
 */
class RouterTest {

    private TestRouter router;

    // Lớp giả lập thừa kế Router để thực hiện các ca kiểm thử
    private static class TestRouter extends Router<String, String> {
        public String lookup(String key) {
            return getHandler(key);
        }
    }

    @BeforeEach
    void setUp() {
        router = new TestRouter();
    }

    @Test
    void testRegisterAndGetHandler() {
        // Đăng ký route và kiểm tra xem có lấy lại đúng handler hay không
        router.register("routeA", "handlerA");
        assertTrue(router.hasRoute("routeA"), "Router phải tồn tại routeA");
        assertEquals("handlerA", router.lookup("routeA"), "Handler trả về phải là handlerA");
    }

    @Test
    void testRegisterDuplicateRouteThrowsException() {
        // Đăng ký trùng route phải ném ra IllegalStateException
        router.register("routeA", "handlerA");
        assertThrows(IllegalStateException.class, () -> router.register("routeA", "handlerB"),
                "Đăng ký trùng routeA phải ném lỗi IllegalStateException");
    }

    @Test
    void testRegisterNullKeyOrHandlerDoesNothing() {
        // Đăng ký key null hoặc handler null thì không làm gì cả
        router.register(null, "handlerA");
        router.register("routeA", null);
        assertFalse(router.hasRoute("routeA"), "Không được phép lưu route có key hoặc handler là null");
        assertNull(router.lookup("routeA"));
    }

    @Test
    void testGetHandlerNullReturnsNull() {
        // Tìm kiếm với key null trả về null
        assertNull(router.lookup(null), "Key null phải trả về null");
    }

    @Test
    void testUnregisterRoute() {
        // Hủy đăng ký route thành công
        router.register("routeA", "handlerA");
        assertTrue(router.hasRoute("routeA"));
        router.unregister("routeA");
        assertFalse(router.hasRoute("routeA"), "routeA phải bị hủy đăng ký");
        assertNull(router.lookup("routeA"));
    }

    @Test
    void testUnregisterNullKeyDoesNothing() {
        // Hủy đăng ký với key null không gây lỗi và không thay đổi gì
        router.register("routeA", "handlerA");
        router.unregister(null);
        assertTrue(router.hasRoute("routeA"), "routeA vẫn phải tồn tại sau khi unregister null");
    }

    @Test
    void testClearRoutes() {
        // Xóa sạch tất cả các route
        router.register("routeA", "handlerA");
        router.register("routeB", "handlerB");
        assertTrue(router.hasRoute("routeA"));
        assertTrue(router.hasRoute("routeB"));
        
        router.clear();
        assertFalse(router.hasRoute("routeA"), "Sau khi clear thì không còn routeA");
        assertFalse(router.hasRoute("routeB"), "Sau khi clear thì không còn routeB");
    }
}
