-- 1 số ràng buộc:
-- + PRIMARY KEY: khóa chính, định danh duy nhất cho 1 hàng
-- + UNIQUE: giá trị trong cột là duy nhất (có thể NULL)
-- + FOREIGN KEY: liên kết 2 bảng với nhau, đảm bảo giá trị ở bảng này phải tồn tại ở bảng kia
-- + DEFAULT: giá trị mặc định nếu ko khai báo cụ thể
-- + CHECK: kiểm tra điều kiện của 1 cột
-- + CONSTRAINT: ràng buộc tùy chỉnh, có thể đặt tên cho ràng buộc

-- TABLE Users
CREATE TABLE IF NOT EXISTS Users (
    username TEXT UNIQUE NOT NULL PRIMARY KEY,
    nickname TEXT,
    password TEXT NOT NULL,
    email TEXT,
    phoneNumber TEXT,
    status TEXT DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'BANNED')),
    role TEXT NOT NULL DEFAULT 'USER' CHECK(role IN ('USER', 'ADMIN')),
    createdAt TEXT NOT NULL,
    lastLogin TEXT,
    balance REAL DEFAULT 0 CHECK(balance >= 0)
);

-- TABLE Auctions
CREATE TABLE IF NOT EXISTS Auctions (
    id TEXT UNIQUE NOT NULL PRIMARY KEY,
    createdAt TEXT NOT NULL,
    auctionName TEXT NOT NULL,
    description TEXT NOT NULL,
    itemId TEXT NOT NULL,
    startingPrice REAL NOT NULL CHECK(startingPrice > 0),
    minIncrement REAL DEFAULT 0 CHECK(minIncrement >= 0),
    seller TEXT NOT NULL,
    currentBid REAL DEFAULT 0,
    currentBidder TEXT,
    status TEXT DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'ENDED', 'PAID', 'BANNED', 'UPCOMING', 'CANCELED', 'AWAITING_PAYMENT', 'AWAITING_DELIVERY', 'COMPLETED')),
    startAt TEXT NOT NULL,
    endTime TEXT NOT NULL,
    antiSnipingTriggerTime INTEGER DEFAULT 0,
    antiSnipingExtensionTime INTEGER DEFAULT 0,
    maxEndTime TEXT,
    CONSTRAINT time_check CHECK(endTime > startAt),
    FOREIGN KEY (itemId) REFERENCES Items(id),
    FOREIGN KEY (seller) REFERENCES Users(username),
    FOREIGN KEY (currentBidder) REFERENCES Users(username)
);

-- TABLE Items
CREATE TABLE IF NOT EXISTS Items (
    id TEXT UNIQUE NOT NULL PRIMARY KEY,
    createdAt TEXT NOT NULL,
    ownerUsername TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT,
    productType TEXT,
    availabilityStatus TEXT NOT NULL DEFAULT 'AVAILABLE'
        CHECK(availabilityStatus IN ('AVAILABLE', 'LOCKED_IN_AUCTION')),
    FOREIGN KEY (ownerUsername) REFERENCES Users(username)
);

-- TABLE Images
CREATE TABLE IF NOT EXISTS Images (
    id TEXT UNIQUE NOT NULL PRIMARY KEY,
    createdAt TEXT NOT NULL,
    filePath TEXT NOT NULL
);

-- TABLE ItemImageLinks
CREATE TABLE IF NOT EXISTS ItemImageLinks (
    id TEXT UNIQUE NOT NULL PRIMARY KEY,
    createdAt TEXT NOT NULL,
    itemId TEXT NOT NULL,
    imageId TEXT NOT NULL,
    displayOrder INTEGER NOT NULL DEFAULT 0,
    isPrimary INTEGER DEFAULT 0 CHECK(isPrimary IN (0, 1)),
    FOREIGN KEY (itemId) REFERENCES Items(id),
    FOREIGN KEY (imageId) REFERENCES Images(id)
);

-- TABLE Bids
CREATE TABLE IF NOT EXISTS Bids (
    id TEXT UNIQUE NOT NULL PRIMARY KEY, 
    createdAt TEXT NOT NULL,
    auctionId TEXT NOT NULL,
    bidder TEXT NOT NULL,
    amount REAL NOT NULL CHECK(amount > 0), -- phải là số dương
    autoBidGenerated INTEGER NOT NULL DEFAULT 0 CHECK(autoBidGenerated IN (0, 1)),
    FOREIGN KEY (auctionId) REFERENCES Auctions(id),
    FOREIGN KEY (bidder) REFERENCES Users(username)
);

-- TABLE Transactions
CREATE TABLE IF NOT EXISTS Transactions (
    id TEXT UNIQUE NOT NULL PRIMARY KEY,
    createdAt TEXT NOT NULL,
    username TEXT NOT NULL,
    type TEXT NOT NULL CHECK(type IN ('DEPOSIT', 'WITHDRAW', 'AUCTION_PAY', 'AUCTION_PROFIT', 'AUCTION_REFUND')),
    amount REAL NOT NULL,
    auctionId TEXT,
    FOREIGN KEY (username) REFERENCES Users(username),
    FOREIGN KEY (auctionId) REFERENCES Auctions(id)
);

-- CREATE INDEX dùng để tạo index ở cột đó -> tăng tốc độ truy vấn khi gọi WHERE
-- những cột là PRIMARY KEY thì mặc định đã có INDEX sẵn nên ko cần tạo nữa

-- tìm kiếm nhanh theo AuctionStatus
CREATE INDEX IF NOT EXISTS auction_status_idx ON Auctions(status);

-- tìm kiếm lịch sử bid theo auctionId
CREATE INDEX IF NOT EXISTS bid_auction_id_idx ON Bids(auctionId);

-- tìm kiếm những sản phẩm mà một User đã từng tham gia bid (dùng cho profile)
CREATE INDEX IF NOT EXISTS bid_bidder_idx ON Bids(bidder);

-- tìm kiếm lịch sử giao dịch theo username
CREATE INDEX IF NOT EXISTS transaction_username_idx ON Transactions(username);

-- tìm inventory theo owner
CREATE INDEX IF NOT EXISTS item_owner_username_idx ON Items(ownerUsername);

-- tìm inventory theo trạng thái khả dụng
CREATE INDEX IF NOT EXISTS item_availability_status_idx ON Items(availabilityStatus);

-- tìm inventory theo owner và trạng thái
CREATE INDEX IF NOT EXISTS item_owner_status_idx ON Items(ownerUsername, availabilityStatus);

-- tìm inventory theo category và productType
CREATE INDEX IF NOT EXISTS item_category_product_type_idx ON Items(category, productType);

-- lấy liên kết ảnh theo item
CREATE INDEX IF NOT EXISTS item_image_link_item_id_idx ON ItemImageLinks(itemId);

-- lấy item link theo image
CREATE INDEX IF NOT EXISTS item_image_link_image_id_idx ON ItemImageLinks(imageId);

-- TABLE WalletRequests
CREATE TABLE IF NOT EXISTS WalletRequests (
    id TEXT UNIQUE NOT NULL PRIMARY KEY,
    createdAt TEXT NOT NULL,
    reviewedAt TEXT,
    username TEXT NOT NULL,
    type TEXT NOT NULL CHECK(type IN ('DEPOSIT', 'WITHDRAW')),
    amount REAL NOT NULL CHECK(amount > 0),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'APPROVED', 'DENIED')),
    reviewedBy TEXT,
    FOREIGN KEY (username) REFERENCES Users(username),
    FOREIGN KEY (reviewedBy) REFERENCES Users(username)
);

CREATE INDEX IF NOT EXISTS wallet_request_username_created_idx ON WalletRequests(username, createdAt);
CREATE INDEX IF NOT EXISTS wallet_request_status_created_idx ON WalletRequests(status, createdAt);
