# Bidify - Hệ thống Đấu giá Trực tuyến

Bidify là một hệ thống đấu giá trực tuyến trên máy tính được xây dựng với client-server architecture. Người dùng có thể quản lý vật phẩm, tạo cuộc đấu giá, đặt thầu (bid) trong thời gian thực, hoàn tất thanh toán đấu giá và quản lý các yêu cầu nạp/rút tiền ví qua ứng dụng JavaFX.

Server nắm giữ business logic, dữ liệu bền vững (persistent data), trạng thái đấu giá lúc thực thi (runtime auction state) và phân phối sự kiện thời gian thực. Client cung cấp giao diện đồ họa và giao tiếp với server qua các TLS sockets.

## Phạm vi Hệ thống

Bidify hỗ trợ quy trình làm việc chính của một nền tảng đấu giá trực tuyến:

1. Người dùng đăng ký tài khoản và đăng nhập.
2. Người bán tạo các vật phẩm trong kho hàng kèm hình ảnh.
3. Người bán tạo một cuộc đấu giá cho một vật phẩm.
4. Những người dùng khác duyệt các cuộc đấu giá và đặt thầu (place bids).
5. Server xác thực các lượt bid và gửi cập nhật thời gian thực tới các client đang kết nối.
6. Server tự động kết thúc (settle) cuộc đấu giá khi hết thời gian.
7. Người thắng đấu giá và người bán hoàn tất quy trình thanh toán và giao hàng.
8. Quản trị viên quản lý người dùng, các cuộc đấu giá và yêu cầu ví.

## Công nghệ

| Lĩnh vực | Công nghệ |
| --- | --- |
| Ngôn ngữ lập trình | Java |
| Giao diện máy tính | JavaFX, FXML, CSS |
| Hệ thống build | Maven |
| Cơ sở dữ liệu | SQLite |
| Giao tiếp Client-server | TLS sockets |
| Định dạng tin nhắn | JSON với Gson |
| Logging | SLF4J và Logback |
| Testing | JUnit 5 |

## Yêu cầu Môi trường

Cài đặt các phần mềm sau trước khi build hoặc chạy dự án:

- JDK 21+
- Maven 3.9+ khi build từ mã nguồn
- Windows, Linux, hoặc macOS để chạy file JAR thủ công
- Windows khi sử dụng trình khởi chạy `START.bat` đi kèm

Đối với các client từ xa, hãy cho phép truy cập mạng tới máy chủ server và cổng TCP đã cấu hình. Cổng mặc định là `5000`.

## Các Module Dự án

```text
auction/
|-- common/     Shared DTOs, enums, request và response models, cùng các tiện ích (utilities)
|-- server/     Ứng dụng Server, business logic, scheduler, trạng thái runtime, và SQLite persistence
|-- client/     Ứng dụng JavaFX, scenes, controllers, navigation, media, và các service phía client
|-- pom.xml     Cấu hình Maven cha
|-- START.bat   Trình khởi chạy Windows để thực thi tại chỗ
`-- README.md
```

### `common`

Chứa giao thức (protocol) dùng chung giữa client và server:

- DTOs
- Enums
- Request và response models
- Realtime event models
- JSON utilities

### `server`

Chứa ứng dụng backend:

- TLS socket server
- Client handler thread pool
- Request dispatcher
- Business services
- Auction scheduler
- Runtime auction và session state
- SQLite DAOs và persistence

### `client`

Chứa ứng dụng JavaFX trên máy tính:

- FXML scenes và CSS stylesheets
- Controllers
- Các lớp service phía client
- Socket client
- Scene navigation và caching
- Image caching và âm thanh thông báo

## Build

Từ thư mục gốc của kho lưu trữ, chạy:

```bash
mvn clean package
```

## Vị trí file JAR

Sau khi build thành công, sử dụng các file thực thi fat JAR:

| Ứng dụng | File JAR |
| --- | --- |
| Server | `server/target/server-1.0-SNAPSHOT-fat.jar` |
| Client | `client/target/client-1.0-SNAPSHOT-universal.jar` |

## Hướng dẫn Chạy

### Lựa chọn 1: Chạy các file JAR đã đóng gói

Khởi động các ứng dụng theo thứ tự sau.

#### Bước 1: Khởi động Server

```bash
java -jar server/target/server-1.0-SNAPSHOT-fat.jar
```

Server lắng nghe trên cổng `5000` theo mặc định.

#### Bước 2: Khởi động Client

Mở một terminal khác:

```bash
java -jar client/target/client-1.0-SNAPSHOT-universal.jar
```

Khởi chạy thêm các instance client để kiểm tra tính năng đấu giá thời gian thực giữa nhiều người dùng.

### Lựa chọn 2: Chạy với Maven

Khởi động các ứng dụng theo thứ tự sau.

#### Bước 1: Cài đặt các phụ thuộc dùng chung

```bash
mvn clean install
```

#### Bước 2: Khởi động Server

```bash
mvn -pl server exec:java
```

#### Bước 3: Khởi động Client

Mở một terminal khác từ thư mục gốc của dự án:

```bash
mvn -pl client javafx:run
```

### Lựa chọn 3: Trình khởi chạy Windows Local

Từ thư mục gốc của dự án:

```bat
START.bat
```

Script này sẽ build module chung, khởi động server nếu cổng `5000` chưa được sử dụng, chờ server sẵn sàng và mở một instance client.

## Cấu hình

Các giá trị cấu hình được truyền dưới dạng JVM system properties trước `-jar`.

| Thuộc tính | Ứng dụng | Mặc định | Mô tả |
| --- | --- | --- | --- |
| `server.port` | Server | `5000` | Cổng TCP được server sử dụng. |
| `db.path` | Server | `data.db` | Đường dẫn tới file cơ sở dữ liệu SQLite. |
| `server.host` | Client | `localhost` | Hostname hoặc địa chỉ IP của server. |
| `server.port` | Client | `5000` | Cổng TCP được sử dụng bởi kết nối client. |

Ví dụ: chạy server với đường dẫn cơ sở dữ liệu và cổng tùy chỉnh:

```bash
java -Ddb.path=/var/lib/bidify/data.db -Dserver.port=5001 -jar server/target/server-1.0-SNAPSHOT-fat.jar
```

Ví dụ: kết nối một client tới một server từ xa:

```bash
java -Dserver.host=192.168.1.10 -Dserver.port=5001 -jar client/target/client-1.0-SNAPSHOT-universal.jar
```

## Các Tính năng Đã Hoàn Thành

### Xác thực và Hồ sơ (Authentication & Profiles)

- Đăng ký người dùng
- Đăng nhập và đăng xuất
- Xem và chỉnh sửa hồ sơ
- Cập nhật mật khẩu
- Hồ sơ người bán công khai
- Vai trò người dùng (user) và quản trị viên (admin)
- Ép buộc đăng xuất sau các hành động quản trị liên quan

### Kho hàng (Inventory)

- Tạo, cập nhật, xem và xóa các vật phẩm trong kho
- Tải lên tối đa năm hình ảnh cho mỗi vật phẩm
- Nén hình ảnh phía client trước khi tải lên
- Caching hình ảnh phía client
- Thư viện hình ảnh đấu giá

### Đấu giá (Auctions)

- Tạo, cập nhật, xem, tìm kiếm và xóa các cuộc đấu giá
- Duyệt các cuộc đấu giá đang diễn ra và sắp tới
- Đấu giá thủ công (Manual bidding)
- Đấu giá tự động (Automatic bidding)
- Tắt tính năng đấu giá tự động
- Hỗ trợ Anti-sniping
- Cập nhật đấu giá thời gian thực
- Hiển thị hoạt động đấu giá và biểu đồ đấu giá
- Tự động lập lịch vòng đời đấu giá (lifecycle scheduling)

### Thanh toán (Settlement)

- Tự động thanh toán khi cuộc đấu giá hết hạn
- Quy trình thanh toán của người thắng cuộc
- Xác nhận giao hàng của người bán
- Quy trình giải quyết thanh toán
- Lịch sử thanh toán của người dùng

### Ví và Lịch sử (Wallet & History)

- Yêu cầu nạp tiền
- Yêu cầu rút tiền
- Hiển thị số dư ví
- Theo dõi số dư bị khóa
- Lịch sử giao dịch
- Lịch sử yêu cầu ví
- Cập nhật ví thời gian thực

### Quản trị (Administration)

- Danh sách người dùng
- Thăng chức và giáng chức quản trị viên
- Cấm và bỏ cấm người dùng
- Xóa người dùng
- Quản lý đấu giá
- Xem xét yêu cầu ví

### Giao diện Client

- Giao diện đồ họa JavaFX
- Load scene từ bộ nhớ cache và tải trước trong nền (background preloading)
- Điều hướng thanh nhiệm vụ (mission bar navigation)
- Logo ứng dụng và icon cửa sổ
- Âm thanh thông báo thành công và lỗi quan trọng
- Phản hồi trạng thái chi tiết đấu giá khi cuộc đấu giá thay đổi hoặc kết thúc

## Tài nguyên Dự án

- [Báo cáo + Video](https://drive.google.com/drive/folders/1GvHM5FN2qlcXY7qHNMp9OoFgLu0qooCq?usp=sharing)
- [Sơ đồ codebase](https://mermaid.ai/d/8b65df02-73ad-478f-b330-718d6eb72ca5)

## Người tham giatest

- Đỗ Giang Thành Đạt
- Phạm Quang Minh
- Nguyễn Quốc Bảo
- Lưu Quỳnh Phương
