# Bidify - Online Auction System

## Project Info
- Detailed specification:
  https://docs.google.com/document/d/1Sj1TxuoD_zeq0wu-CCMGqSefZQMXZrzrDEWELx7WQUA/edit?tab=t.0

---

## Overview
Bidify is a client-server online auction system built with Java.  
It allows multiple users to create auctions, place bids in real time, and automatically determines winners when auctions end.

The system is designed using:
- Client–Server architecture
- MVC (JavaFX client)
- Event-driven realtime updates

---

## System Design
- Codebase diagram:
  https://mermaid.ai/d/8b65df02-73ad-478f-b330-718d6eb72ca5

---

## Features

### Core Features
- User roles: `User` (for both `Bidder` / `Seller`), `Admin`
- Auction management (create, update, delete)
- Real-time bidding with validation
- Automatic auction lifecycle (`UPCOMING` → `ACTIVE` → `ENDED`)
- Winner selection
- Concurrency-safe bidding
- Exception handling

### Technical Features
- JavaFX GUI
- Socket-based communication
- Observer pattern for realtime updates
- Singleton and Factory patterns
- In-memory runtime state with persistence
- Maven-based build system

---

## Project Structure

```
root
├── common   # shared DTOs, enums, request/response models
├── server   # business logic, runtime state, scheduler, persistence
├── client   # JavaFX UI, controllers, socket client
```

---

## Requirements
- OpenJDK 21+
- Maven 3.9+

---

## How to Run

### Option 1 (Windows)
```bash
./START.bat
```

### Option 2 (Manual)

**Run server:**
```bash
mvn -pl server exec:java
```

**Run client:**
```bash
mvn -pl client javafx:run
```

---

## Architecture Overview

```
Client (JavaFX)
   ↓
Request → Server → Service Layer → Runtime State / Database
   ↑
Response + Realtime Events
```

- Server is the source of truth
- Runtime state manages active auctions
- Scheduler controls lifecycle transitions
- Clients receive realtime updates via observer pattern 

---

## Contributors
- Đỗ Giang Thành Đạt
- Phạm Quang Minh
- Nguyễn Quốc Bảo
- Lưu Quỳnh Phương

---