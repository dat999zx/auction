CREATE TABLE IF NOT EXISTS Users (
    username TEXT PRIMARY KEY,
    nickname TEXT,
    password TEXT NOT NULL,
    email TEXT,
    phoneNumber TEXT,
    role TEXT DEFAULT 'USER' CHECK(role IN ('USER', 'ADMIN')),
    status TEXT DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'BANNED')),
    inSession INTEGER DEFAULT 0,
    createdAt TEXT,
    lastLogin TEXT
);