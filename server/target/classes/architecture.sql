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
CREATE INDEX username_idx ON Users(username);
CREATE TABLE IF NOT EXISTS Auctions (
    id TEXT PRIMARY KEY,
    auctionName TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT,
    type TEXT,
    startingPrice REAL NOT NULL,
    minIncrement REAL,
    maxIncrement REAL,
    seller TEXT NOT NULL, 
    currentBidder TEXT,
    status TEXT DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'ENDED', 'PAID', 'BANNED', 'UPCOMING')),
    startAt TEXT NOT NULL,
    endTime TEXT NOT NULL
);
CREATE INDEX auction_id_idx ON Auctions(id);