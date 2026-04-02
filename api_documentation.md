# SecureLearn Backend API Documentation

All secure endpoints require the `Authorization` header to be sent with a valid JWT.
**Format**: `Authorization: Bearer <your_jwt_token>`

---

## 1. Authentication Flow

### User Login
Authenticates users of all roles (SUPER_ADMIN, ADMIN (Company/Tutor), STUDENT) and returns a JWT token.
* **URL**: `/api/auth/login`
* **Method**: `POST`
* **Headers**: `Content-Type: application/json`
* **Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```
* **Response**:
  ```json
  {
    "token": "eyJhb...",
    "refreshToken": "eyJhb...",
    "user": { ... }
  }
  ```

### Super Admin Login
Dedicated login route for Super Admins.
* **URL**: `/api/super-admin/login`
* **Method**: `POST`
* **Headers**: `Content-Type: application/json`
* **Body**:
  ```json
  {
    "email": "superadmin@example.com",
    "password": "superpassword"
  }
  ```

---

## 2. Super Admin Panel (Manage Companies/Tenants)

*Requires role: `SUPER_ADMIN`*

Super Admins can create isolated tenants, which act as **Companies** or institutions.

### Create Company (Tenant)
* **URL**: `/api/super-admin/tenant`
* **Method**: `POST`
* **Body**:
  ```json
  {
    "tenantName": "Innogreen Academy",
    "adminEmail": "admin@innogreen.com",
    "adminPassword": "securepassword",
    "adminName": "Innogreen Admin"
  }
  ```
* **Description**: Creates a new tenant and an associated `ADMIN` user.

---

## 3. Admin / Company / Tutor Management

*Requires role: `ADMIN` or `COMPANY` or `TUTOR`*

Admins (Company Owners) can create sub-users (Students) and manage content.

### Create Student
* **URL**: `/api/admin/users`
* **Method**: `POST`
* **Body**:
  ```json
  {
    "name": "John Doe",
    "email": "john@student.com",
    "password": "studentpassword"
  }
  ```
* **Description**: Creates a user with the `STUDENT` role automatically mapped to the calling Admin's `adminId` and `tenantId`.

*(Note: To create Tutors, an Admin could use `PUT /api/admin/manage-users/{id}/role` on an existing student with `?role=TUTOR`, if allowed by business logic)*

### Upload Content (To Cloudinary)
* **URL**: `/api/admin/content/upload`
* **Method**: `POST`
* **Headers**: `Content-Type: multipart/form-data`
* **Form Data Parameters**:
  * `title` (String): Title of the content.
  * `description` (String, Optional): Rich description.
  * `file` (File, Optional): The PDF or Video file.
  * `videoUrl` (String, Optional): Provide this instead of `file` for external video links (YouTube, Vimeo, etc.).
* **Description**: Uploads the video or PDF to Cloudinary and saves the metadata. Returns a success message.

---

## 4. Student Content Access

*Requires role: `STUDENT`*

Students can list and access content (PDF/Video) that belongs to their assigned Admin/Company.

### List Available Content
* **URL**: `/api/student/content`
* **Method**: `GET`
* **Query Params**: `page` (default 0), `size` (default 20)
* **Response**: Returns a paginated list of content metadata (`title`, `description`, `type`, etc.) available to the student.

### Access Cloudinary Video (HLS / External URLs)
Videos can either be securely streamed via HLS (using Cloudinary conversions) or accessed via secured external URLs.

**Option A: Get Short-lived HLS Streaming Token & Stream File**
1. **Get Token**:
   * **URL**: `/api/student/video/token/{contentId}`
   * **Method**: `GET`
   * **Headers**: `X-Device-Fingerprint: <device_hash>`
   * **Response**: Returns a short-lived token string.
2. **Stream Playlist**:
   * **URL**: `/api/student/video/{contentId}/playlist?token={generated_token}`
   * **Method**: `GET`
   * **Headers**: `X-Device-Fingerprint: <device_hash>`
   * **Description**: Returns a 302 Redirect to the secure Cloudinary `fileUrl` (which client players can use as an `.m3u8` source).

**Option B: Get External Secure Video URL (Mighty Networks style)**
* **URL**: `/api/student/video/{contentId}/secure-url`
* **Method**: `GET`
* **Response**: Returns the external `http...` video URL string directly for `VIDEO_URL` type content.

### Access Secured PDF
PDF files use signed, time-limited URLs to prevent sharing.

**1. Generate Signed URL**
* **URL**: `/api/student/pdf/url/{contentId}`
* **Method**: `GET`
* **Response**: Returns a fully constructed signed URL containing the `token`, `ts`, and `email` query parameters.

**2. View/Download Watermarked PDF**
* **URL**: `(The URL generated from the step above)` 
  `e.g., /api/student/pdf/{contentId}?token=...&ts=...&email=...`
* **Method**: `GET`
* **Description**: Authenticates the token natively, generates dynamic watermarks, and streams the PDF document inline to the client.
