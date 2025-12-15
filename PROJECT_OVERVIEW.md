## Secure Journal App — Simple Project Guide

This document explains the whole project in very easy language so that anyone on the team can understand it quickly. Keep this file open while preparing for demos, reviews, or vivas.

---

### 1. Introduction
- **Goal:** Give every user a private digital diary that is safe even if someone hacks our database.
- **Key Idea:** We encrypt every journal entry using a secret “unique key” that only the owner knows. Without that key the text is unreadable.
- **Users:** Normal users who write journals, and (optionally) one admin user who can monitor the system.

---

### 2. Problem We Solve
| Problem in normal diary apps | How we solve it |
| --- | --- |
| Password leaks expose the full diary | Passwords are hashed with BCrypt and never stored in plain text |
| Hackers can read database records | We encrypt title & content using AES before saving |
| Anyone can log in from unknown devices | Each user owns a device-specific **unique key** |
| Hard to trust cloud storage | Encryption happens before data reaches MongoDB |

---

### 3. Tech Stack (Why we picked each tool)
| Part | Technology | Reason |
| --- | --- | --- |
| Frontend | **React + Recoil** | Fast UI, easy state management |
| Backend | **Spring Boot (Java)** | Stable, secure, built-in security filters |
| Database | **MongoDB** | Stores JSON-like documents, good for text entries |
| Security | **AES-256** for data, **BCrypt** for passwords, **JWT-style filter** for auth | Proven cryptography standards |
| Tools | **Maven**, **npm**, **Postman** | Build automation, UI development, API testing |

---

### 4. Project Workflow (Step by step)
1. **Sign Up:** User creates an account → backend generates a **unique key** → we show it once → user must save it.
2. **Login:** User enters username + password (and optionally their unique key).  
   - Password is checked using BCrypt.  
   - Unique key is only validated, never regenerated.
3. **Write Entry:**  
   - Frontend sends title, content & unique key.  
   - Backend encrypts both fields with AES using that key.  
   - Encrypted text is stored in MongoDB.
4. **View Entries:**  
   - Backend returns encrypted records.  
   - Frontend shows them as “🔒 Encrypted Entry”.
5. **Decrypt Entry:**  
   - User clicks “Unlock” and types their unique key.  
   - Backend decrypts and sends plain text back.

---

### 5. Folder & File Guide (What each part does)

#### Backend (`src/main/java/com/RohitPotdar/myJournalApp/`)
| Folder/File | Plain-English Purpose |
| --- | --- |
| `controller_2/PublicController_19.java` | Handles **public APIs**: create user, login, validate key, etc. |
| `controller_2/JournalEntryController_Copy_7.java` | Handles **journal APIs**: create entry, list entries, decrypt entry. |
| `Service_8/userService_14.java` | User-related helper functions: save user, hash passwords, generate unique key. |
| `Service_8/JournalEntryService_10.java` | Encrypt/decrypt text and save entries with AES. |
| `config_16/JwtAuthenticationFilter.java` | Intercepts every request and checks the Authorization header. |
| `config_16/SecurityConfig_17.java` | Sets which URLs need login, admin rights, etc. |
| `entity_5/User_12.java` | MongoDB document for users (username, password hash, key hash, roles). |
| `entity_5/JournalEntry_6.java` | MongoDB document for each journal entry (encrypted title & content). |
| `application.properties` | Connection details (MongoDB URI, encryption key, JWT secret). |

#### Frontend (`frontend/src/`)
| Folder/File | Plain-English Purpose |
| --- | --- |
| `components/Login/LoginForm.js` | Sign-in / Sign-up screen + unique key modal. |
| `components/Dashboard/Dashboard.js` | Main page after login, shows entries and quick actions. |
| `components/JournalEntry/EntryCard.js` | One entry card (lock/unlock/decrypt UI). |
| `components/JournalEntry/CreateEntry.js` | Form to add a new entry. |
| `services/api.js` | Axios setup and all API calls. |
| `state/atoms.js` | Recoil atoms for auth state and cached entries. |
| `App.js` | React Router configuration (routes for login, dashboard, etc.). |

---

### 6. Important Behaviors (Keep in mind)
- **Unique Key**  
  - Generated once during sign-up.  
  - User must save it.  
  - Not shown again during normal login.  
  - Needed for encrypting/decrypting entries.

- **Encryption**  
  - Algorithm: AES/ECB/PKCS5Padding (key derived from unique key).  
  - Happens inside `JournalEntryService_10.java` before saving.  
  - Decryption only when user provides the same unique key.

- **Authentication**  
  - Passwords hashed with BCrypt.  
  - Spring Security + custom JWT filter checks every request.  
  - `Authorization: Bearer <token>` can be a JWT, the unique key, or the username (for trusted devices).

---

### 7. API Quick View
| Endpoint | Method | Description |
| --- | --- | --- |
| `/public/createUser` | POST | Register new user. Returns unique key one time. |
| `/public/login` | POST | Login with username & password (plus optional unique key). |
| `/journalCopies` | GET | List encrypted entries (requires login). |
| `/journalCopies` | POST | Create entry (needs unique key for encryption). |
| `/journalCopies/decrypt` | POST | Decrypt one entry by sending encrypted text + unique key. |

---

### 8. How to Explain the Project (Sample Answer)
> “Our Secure Journal App lets users write entries that only they can read. Each user gets a secret unique key during sign-up. Every entry is encrypted on the server using AES and that unique key, so even if someone steals the database they only see gibberish. Passwords are stored using BCrypt, and Spring Security filters protect all journal APIs. The frontend is built in React and simply talks to our APIs.”

---

### 9. How to Run / Demo (Short)
1. **Start Backend:** `mvn spring-boot:run`
2. **Start Frontend:** `cd frontend && npm install && npm start`
3. **Test APIs with Postman** (optional)
4. **Demo Flow:** Sign up → copy unique key → login → create entry → decrypt entry.

---

### 10. Final Notes
- Never regenerate unique keys for existing users.  
- Do not log or store plain text passwords/keys.  
- MongoDB must run with replica set (`rs0`) for transactions.  
- Keep `.env`/secrets outside of version control in production.

> **Tip:** Practice explaining unique key + AES encryption, because this is the most unique part of the project.

---

*Last updated: 2025-12-15*

