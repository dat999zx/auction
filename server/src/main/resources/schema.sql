CREATE TABLE IF NOT EXISTS Users (
    username TEXT PRIMARY KEY,
    nickname TEXT,
    password TEXT NOT NULL,
    email TEXT,
    phoneNumber TEXT,
    status TEXT DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'BANNED')),
    createdAt TEXT,
    lastLogin TEXT,
    wallet REAL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS Auctions (
    id TEXT PRIMARY KEY,
    auctionName TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT,
    type TEXT,
    startingPrice REAL NOT NULL,
    minIncrement REAL DEFAULT 0,
    seller TEXT NOT NULL,
    currentBid REAL DEFAULT 0,
    currentBidder TEXT,
    status TEXT DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'ENDED', 'PAID', 'BANNED', 'UPCOMING')),
    startAt TEXT NOT NULL,
    endTime TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS auction_status_idx ON Auctions(status);
