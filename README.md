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

## 🔌 Integrations
- **Firebase** → user authentication & push notifications  
- **MongoDB** → persistent storage for users, sessions, and device data  

---

## 📂 Project Structure
- `config/` → Firebase, MongoDB, and security configuration  
- `security/` → Authentication filter & principal  
- `devices/` → API for managing device tokens  
- `messaging/` → FCM notification gateway  

---
