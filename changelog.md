# 🚀 Changelog - v1.0.1

## 🏗️ Tái cấu trúc hạ tầng (Refactoring)
*   **Kiến trúc xử lý Request**: Giới thiệu **Dispatcher Pattern** mới, tách biệt rõ ràng việc điều hướng và xử lý yêu cầu từ Client.
*   **Hệ thống sự kiện**: Cải tiến `EventManager` dựa trên `Router` để quản lý luồng sự kiện tập trung và hiệu quả hơn.
*   **Dịch vụ (Services)**: Tách riêng `TransactionService` để quản lý chuyên sâu các giao dịch tài chính; thay thế `ServiceUtil` bằng `RequestUtil`.
*   **Lớp dữ liệu (DAO)**: Lược bỏ các interface dư thừa, tối giản hóa cấu trúc DAO.

## ✨ Cải tiến tính năng
*   **Quản lý thời gian**: Đồng bộ hóa `AuctionSchedulerService` làm nguồn thời gian chuẩn duy nhất cho hệ thống đấu giá, loại bỏ việc tự cập nhật trong class Auction.
*   **Tài liệu**: Cập nhật sơ đồ cấu trúc mã nguồn (codebase diagram) trong `README.md`.

## 🐞 Sửa lỗi (Bug Fixes)
*   **BidDao**: Khắc phục lỗi trong câu lệnh SQL truy vấn lịch sử đặt giá.
*   **Wallet**: Sửa lỗi khởi tạo `lockedBalance` trong constructor của class Wallet.

## 🧪 Chất lượng & Kiểm thử
*   **Unit Testing**: Bổ sung bộ kiểm thử tự động cho `BidDao` để đảm bảo tính chính xác của dữ liệu đấu giá.