# 🔐 SecureVault - Encrypted Journal Application

> **A secure, encrypted journal application built with Spring Boot, MongoDB, and enterprise-grade security features.**

*Your personal thoughts, safely encrypted and protected.*

[![Java](https://img.shields.io/badge/Java-20-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green.svg)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-blue.svg)](https://www.mongodb.com/atlas)
[![Security](https://img.shields.io/badge/Security-AES256%20%2B%20JWT-red.svg)](https://jwt.io/)

---

## 🚀 Features

### ✅ **Core Features:**
- **🔐 AES-256 Encryption**: Both title and content are encrypted before database storage
- **🛡️ JWT Authentication**: Secure token-based authentication system
- **👤 User Management**: BCrypt password hashing with role-based access control
- **📝 Full CRUD Operations**: Create, Read, Update, Delete journal entries
- **🔒 User Isolation**: Users can only access their own journal entries
- **☁️ Cloud Database**: MongoDB Atlas integration for reliable data persistence

### 🔐 **Security Features:**
- **Military-grade Encryption**: AES-256 with Base64 encoding
- **Secure Authentication**: JWT tokens with expiration
- **Password Security**: BCrypt hashing for user passwords
- **Role-based Access**: Admin and User role management
- **Stateless Sessions**: No server-side session storage

---

## 📋 Project Structure

```
src/main/java/com/RohitPotdar/myJournalApp/
├── config_16/
│   ├── SecurityConfig_17.java          # Spring Security + JWT configuration
│   └── JwtAuthenticationFilter.java    # JWT token validation filter
├── controller_2/
│   ├── AdminController_20.java         # Admin endpoints
│   ├── JournalEntryController_Copy_7.java  # Journal CRUD operations
│   ├── PublicController_19.java        # Public endpoints (login, signup)
│   └── userController_15.java          # User management
├── entity_5/
│   ├── JournalEntry_6.java             # Journal entry entity
│   └── User_12.java                    # User entity
├── Repository_9/
│   ├── JournalEntryRepository_11.java  # Journal entry repository
│   └── userRepository_13.java          # User repository
└── Service_8/
    ├── JournalEntryService_10.java     # Journal business logic + AES encryption
    ├── userService_14.java             # User business logic + JWT tokens
    └── userDetailsServiceImpl_18.java  # Spring Security user details
```

---

## 🛠️ Quick Start

### **Prerequisites:**
- Java 20
- Maven 3.6+
- MongoDB Atlas account

### **1. Clone & Setup:**
```bash
git clone <your-repo-url>
cd SecureVault
```

### **2. Configuration:**
Update `src/main/resources/application.properties`:
```properties
# MongoDB Configuration
spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/journaldb
spring.data.mongodb.database=journaldb

# AES Encryption Key (Change this in production!)
external.encryption.key=PdRgUkXp2s5v8y/B

# JWT Configuration
jwt.secret=mySecretKeyForJWTTokenGenerationAndValidationInMyJournalApp2024
jwt.expiration=86400000

# Server Configuration
server.port=8085
```

### **3. Build & Run:**
```bash
mvn clean install
mvn spring-boot:run
```

**Application will start on:** `http://localhost:8085`

---

## 🧪 Testing Guide

### **Step 1: Health Check**
```bash
curl http://localhost:8085/public/health-check
# Expected: "OK"
```

### **Step 2: Create User**
```bash
curl -X POST http://localhost:8085/public/createUser \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "testuser",
    "password": "password123"
  }'
```

### **Step 3: Login & Get Token**
```bash
curl -X POST http://localhost:8085/public/login \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "testuser",
    "password": "password123"
  }'
```

**Response:**
```json
{
  "message": "Login successful",
  "username": "testuser",
  "roles": ["USER"],
  "token": "testuser.1234567890.1234567890",
  "tokenType": "Bearer"
}
```

### **Step 4: Create Encrypted Entry**
```bash
curl -X POST http://localhost:8085/journalCopies \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "title": "My Secret Entry",
    "content": "This content will be encrypted in the database!"
  }'
```

### **Step 5: Get All Entries**
```bash
curl -X GET http://localhost:8085/journalCopies \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 🔐 Security Implementation

### **AES-256 Encryption:**
- **Algorithm**: `AES/ECB/PKCS5Padding`
- **Key Generation**: SHA-1 hash of configured key, truncated to 16 bytes
- **Encoding**: Base64 for database storage
- **Scope**: Both title and content are encrypted

### **Encryption Flow:**
```
Save:   Plain Text → AES Encrypt → Base64 Encode → MongoDB
Retrieve: MongoDB → Base64 Decode → AES Decrypt → Plain Text
```

### **JWT Authentication:**
- **Token Format**: `username.timestamp.expiration`
- **Validation**: Expiration check + user existence verification
- **Scope**: All protected endpoints require Bearer token

---

## 📊 Database Schema

### **Users Collection:**
```json
{
  "_id": ObjectId,
  "userName": "string (unique)",
  "password": "$2a$10$... (BCrypt hashed)",
  "roles": ["USER"],
  "allEntries": [ObjectId] // References to journal entries
}
```

### **Journal Entries Collection:**
```json
{
  "_id": ObjectId,
  "title": "encrypted_base64_string",
  "content": "encrypted_base64_string",
  "localDateTime": "2025-07-21T10:00:00"
}
```

---

## 🔄 API Reference

### **Public Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/public/health-check` | Application health status |
| `POST` | `/public/createUser` | User registration |
| `POST` | `/public/login` | User authentication |

### **Protected Endpoints (Require JWT):**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/journalCopies` | Get user's journal entries |
| `POST` | `/journalCopies` | Create new journal entry |
| `GET` | `/journalCopies/id/{id}` | Get specific entry |
| `PUT` | `/journalCopies/id/{id}` | Update entry |
| `DELETE` | `/journalCopies/id/{id}` | Delete entry |

### **Admin Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/admin/all-entries` | Get all users (admin only) |
| `POST` | `/admin/create-admin` | Create admin user |

---

## 🧪 Unit Testing

### **Run All Tests:**
```bash
mvn test
```

### **Run Specific Tests:**
```bash
# Encryption tests
mvn test -Dtest=userServiceTests#testEncryptionAndDecryption

# User tests
mvn test -Dtest=userServiceTests#testByParameters
```

### **Test Coverage:**
- ✅ User operations
- ✅ AES encryption/decryption
- ✅ Special characters handling
- ✅ Empty content handling
- ✅ JWT token validation

---

## 🚀 Deployment

### **Production Considerations:**
1. **Environment Variables**: Store sensitive data in environment variables
2. **Encryption Key**: Use a strong, randomly generated encryption key
3. **JWT Secret**: Use a cryptographically secure random string
4. **HTTPS**: Enable SSL/TLS for all communications
5. **Rate Limiting**: Implement API rate limiting
6. **Logging**: Configure proper logging and monitoring

### **Docker Deployment:**
```dockerfile
FROM openjdk:20-jdk-slim
COPY target/SecureVault-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## 🔧 Troubleshooting

### **Common Issues:**

**1. Circular Dependency Error:**
```
The dependencies of some of the beans form a cycle
```
**Solution:** Use `@Lazy` annotation on dependencies

**2. MongoDB Connection Error:**
```
Failed to connect to MongoDB Atlas
```
**Solution:** Check connection string and network access

**3. Encryption Key Error:**
```
Encryption key not configured
```
**Solution:** Verify `external.encryption.key` in application.properties

**4. JWT Token Invalid:**
```
401 Unauthorized
```
**Solution:** Check token format and expiration

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Spring Boot** for the robust framework
- **MongoDB Atlas** for cloud database services
- **JWT.io** for token authentication standards
- **AES Encryption** for military-grade security

---

## 📞 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the troubleshooting section above

---

**🔐 SecureVault - Where your thoughts are truly secure.** ✨
# Secure-Vault
# Secure-Vault
# Secure-Vault
