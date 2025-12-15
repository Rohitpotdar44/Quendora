## Secure Document Upload & Encryption – Quick Guide

This guide explains, in simple terms, how to add “upload → encrypt → store → download → decrypt” for PDF / DOC files in our Spring Boot project.

---

### 1. What We Want
1. User uploads any document (e.g., PDF, DOCX).
2. Backend encrypts the raw bytes with the user’s existing **unique key**.
3. Encrypted file is saved securely (MongoDB/GridFS).
4. User can later download the file; backend decrypts on the fly.
5. Everything can be tested through Postman before wiring the frontend.

---

### 2. Building Blocks & Concepts

| Concept | Why we need it |
| --- | --- |
| `MultipartFile` | Lets Spring accept binary uploads (PDF/DOC) |
| AES encryption on byte arrays | Same approach as text, but applied to file bytes |
| Storage (MongoDB GridFS or collection) | Holds encrypted bytes + metadata |
| Download endpoint | Streams decrypted bytes when user needs the file |
| Unique key validation | Ensures only the owner can encrypt/decrypt |

---

### 3. Technologies & Libraries
- **Spring Boot Web** – to build REST controllers (`MultipartFile` support).
- **Spring Data MongoDB** – to store encrypted documents (`GridFS` recommended).
- **Java Cryptography API** (`javax.crypto.Cipher`) – AES encryption/decryption.
- **Postman** – to test upload & download APIs before frontend.

---

### 4. Data Model
Create a simple document entity or GridFS metadata record:

```java
@Document("encrypted_documents")
public class EncryptedDocument {
    private ObjectId id;
    private String userName;
    private String originalFileName;
    private String contentType;
    private long size;
    private LocalDateTime uploadedAt;
    private ObjectId gridFsId; // or byte[] encryptedData
}
```

If you choose GridFS:
- Store encrypted bytes in GridFS.
- Keep metadata (username, filename, upload time) in Mongo collection for quick listing.

---

### 5. Encryption / Decryption Helpers

Reuse the AES helper in `JournalEntryService_10`, but adapt it for byte arrays:

```java
public byte[] encryptBytes(byte[] data, String uniqueKey) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, getKey(uniqueKey));
    return cipher.doFinal(data);
}

public byte[] decryptBytes(byte[] encrypted, String uniqueKey) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, getKey(uniqueKey));
    return cipher.doFinal(encrypted);
}
```

`getKey(uniqueKey)` is the same helper you already have (SHA-1 → first 16 bytes → SecretKeySpec).

---

### 6. Upload API (POST)

**Endpoint:** `POST /journalCopies/uploadDocument`

**Inputs:**
- Headers: `Authorization: Bearer <uniqueKey or username>`
- Body (form-data):
  | Key | Type | Description |
  | --- | --- | --- |
  | `file` | File | PDF/DOC upload |
  | `uniqueKey` | Text | User’s secret key |

**Controller flow:**
1. Authenticate user via `Bearer` token (already handled).
2. Validate unique key using `userService.matchesUniqueKey(...)`.
3. Read file bytes from `MultipartFile`.
4. Encrypt bytes with `encryptBytes(data, uniqueKey)`.
5. Store encrypted bytes (GridFS or collection).
6. Save metadata (file name, size, upload date, reference ID).
7. Return JSON: `{ "documentId": "...", "message": "Document saved securely." }`

---

### 7. Download API (GET)

**Endpoint:** `GET /journalCopies/document/{docId}`

**Inputs:**
- Headers: `Authorization: Bearer <token>`
- Query param or header: `uniqueKey=<saved key>`

**Controller flow:**
1. Verify user owns the document.
2. Validate unique key.
3. Fetch encrypted bytes from storage.
4. Decrypt using `decryptBytes`.
5. Return decrypted file as response:
   ```java
   return ResponseEntity.ok()
       .contentType(MediaType.parseMediaType(doc.getContentType()))
       .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + doc.getOriginalFileName())
       .body(new ByteArrayResource(decryptedBytes));
   ```

---

### 8. Optional: List Documents

**Endpoint:** `GET /journalCopies/documents`

Return metadata (id, name, size, upload date). No encryption needed here.

---

### 9. Testing with Postman

#### A. Upload
1. Select `POST /journalCopies/uploadDocument`.
2. Auth tab → Bearer token = uniqueKey or username.
3. Body → form-data:
   - `file` → choose a PDF/DOC from disk.
   - `uniqueKey` → same string provided at signup.
4. Send → expect 200 + documentId.

#### B. Download
1. `GET /journalCopies/document/{docId}?uniqueKey=...`
2. Ensure same Bearer token as upload.
3. Postman → click “Save Response” to file → open with PDF/DOC viewer; it should match original.

#### C. List
1. `GET /journalCopies/documents`
2. Verifies metadata appears correctly.

---

### 10. Frontend Plan (after APIs work)
1. Add a file picker in the dashboard (React).
2. Submit file + uniqueKey using Axios (form-data).
3. Show list of uploaded docs; each row has a “Download” button that calls the decrypt endpoint.
4. Reuse existing uniqueKey stored in localStorage so user doesn’t retype it.

---

### 11. Common Gotchas
- **Large files** → prefer GridFS instead of byte[] in a single document.
- **Unique key mismatch** → always validate before encrypt/decrypt.
- **Content type** → store and reuse so downloads open correctly.
- **Encryption errors** → wrap in try/catch and return user-friendly messages.

---

### 12. Summary
1. Upload endpoint accepts `MultipartFile`, reads bytes.
2. Encrypt bytes with user’s unique key.
3. Store encrypted bytes + metadata.
4. Download endpoint decrypts bytes on demand.
5. Test via Postman; then wire up React UI.

Once these steps are complete, users can securely store any document, not just text notes. When you’re ready, we’ll start coding the upload endpoint together.

