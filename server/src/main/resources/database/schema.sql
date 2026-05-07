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
    category TEXT,
    type TEXT,
    startingPrice REAL NOT NULL CHECK(startingPrice > 0),
    minIncrement REAL DEFAULT 0 CHECK(minIncrement >= 0),
    seller TEXT NOT NULL,
    currentBid REAL DEFAULT 0,
    currentBidder TEXT,
    status TEXT DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'ENDED', 'PAID', 'BANNED', 'UPCOMING', "CANCELED")),
    startAt TEXT NOT NULL,
    endTime TEXT NOT NULL,
    CONSTRAINT time_check CHECK(endTime > startAt),
    FOREIGN KEY (seller) REFERENCES Users(username),
    FOREIGN KEY (currentBidder) REFERENCES Users(username)
);

-- TABLE Bids
CREATE TABLE IF NOT EXISTS Bids (
    id TEXT UNIQUE NOT NULL PRIMARY KEY, 
    createdAt TEXT NOT NULL,
    auctionId TEXT NOT NULL,
    bidder TEXT NOT NULL,
    amount REAL NOT NULL CHECK(amount > 0), -- phải là số dương
    FOREIGN KEY (auctionId) REFERENCES Auctions(id),
    FOREIGN KEY (bidder) REFERENCES Users(username)
);

-- TABLE Transactions
CREATE TABLE IF NOT EXISTS Transactions (
    id TEXT UNIQUE NOT NULL PRIMARY KEY,
    createdAt TEXT NOT NULL,
    username TEXT NOT NULL,
    type TEXT NOT NULL CHECK(type IN ('DEPOSIT', 'WITHDRAW', 'AUCTION_PAY', 'AUCTION_PROFIT')),
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
