CREATE TABLE IF NOT EXISTS Users (
    userId TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    nickname TEXT,
    password TEXT NOT NULL,
    email TEXT,
    phoneNumber TEXT,
    role TEXT DEFAULT 'USER' CHECK(role IN ('USER', 'ADMIN')),
    status TEXT DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'BANNED', 'SUSPENDED')),
    inSession INTEGER DEFAULT 0,
    createdAt TEXT,
    lastLogin TEXT
);