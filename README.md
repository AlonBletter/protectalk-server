# ProtecTalk Server

The backend service powering ProtecTalk.  
Built with **Java + Spring**, it provides authentication, messaging, and real-time data synchronization for the client app.

---

## 🚀 Tech Stack
- **Language**: Java  
- **Framework**: Spring Boot  
- **Database**: MongoDB  
- **Authentication & Messaging**: Firebase  

---

## 📦 Key Features

- 🔐 **Authentication & Authorization** → Firebase integration for secure user identity (email/password sign-in).  
- 📲 **Device Token Management** → Store and update FCM tokens per device for reliable push delivery.  
- 📡 **Push Notifications** → Server-side integration with Firebase Cloud Messaging (FCM) for sending alerts.  
- 📊 **Alert Processing** → Receives, validates, and persists alerts triggered by the client app.  
- 🗄️ **Database Persistence** → MongoDB integration for users, sessions, device tokens, and alerts.  
- ⚙️ **RESTful APIs** → Endpoints for client communication (registration, alerts, contact requests, etc.).  
- 🧩 **Modular Architecture** → Organized into clear packages (`config`, `security`, `devices`, `messaging`) for maintainability.  
- 📝 **Auditing & Logging** → Mongo audit + structured logs for traceability of events and data changes.  

---

## 🚀 Deployment

You can run the ProtecTalk Server using **Docker Compose**, which starts both the application and MongoDB.

### 1. Create `docker-compose.yml`

```yaml
version: "3.8"

services:
  server:
    build: .
    container_name: protectalk-server
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/protectalk
      - SPRING_PROFILES_ACTIVE=prod
      - FIREBASE_CONFIG_PATH=/app/config/firebase-service-account.json
    volumes:
      - ./config/firebase-service-account.json:/app/config/firebase-service-account.json:ro
    depends_on:
      - mongo

  mongo:
    image: mongo:6.0
    container_name: protectalk-mongo
    restart: always
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db

volumes:
  mongo-data:
```

### 2. Build & Run

```bash
docker compose up --build -d
```

### 3. Verify

- API available at → [http://localhost:8080](http://localhost:8080)  
- MongoDB available at → `mongodb://localhost:27017`  

---

## 📝 Notes
- Make sure to provide your **Firebase service account JSON** in `./config/firebase-service-account.json`.  
- For production, update `SPRING_PROFILES_ACTIVE` and configure secrets in a safe way.  

---

## 📜 License
MIT License © ProtecTalk
