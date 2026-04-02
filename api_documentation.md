# SecureLearn Backend API Documentation & Workflows

All secure endpoints require the `Authorization` header to be sent with a valid JWT.
**Format**: `Authorization: Bearer <your_jwt_token>`

Below is the step-by-step workflow representing the role hierarchy: **SUPER_ADMIN → COMPANY → TUTOR → STUDENT**

---

## Step 1: System Orchestration (Super Admin)

The Super Admin sets up the platform and creates isolated organizations ("Tenants"). 

### 1a. Super Admin Login
*   **URL**: `/api/super-admin/login`
*   **Method**: `POST`
*   **Headers**: `Content-Type: application/json`
*   **Body**:
    ```json
    {
      "email": "sarathyofficial90@gmail.com",
      "password": "your_secure_password"
    }
    ```
*   **Response**: Returns an access token (`{ "accessToken": "eyJhb..." }`). Use this token in the `Authorization: Bearer` header for the next step.

### 1b. Create Company (Tenant) & Auto-Create Company Admin
*   **URL**: `/api/super-admin/tenant`
*   **Method**: `POST`
*   **Headers**: `Authorization: Bearer <super_admin_jwt>`
*   **Body**:
    ```json
    {
      "name": "Global Tech Academy",
      "type": "EDTECH_COMPANY",
      "adminName": "John Global",
      "adminEmail": "admin@globaltech.com",
      "adminPassword": "adminPassword123!"
    }
    ```
*   **Action**: This creates a new Tenant database entry AND automatically provisions an `ADMIN` user holding those credentials tied to the tenant.

---

## Step 2: Company Administrative Setup

The newly created Company Admin logs in and begins adding staff (Tutors) or direct students.

### 2a. Company Admin Login
*   **URL**: `/api/auth/login`
*   **Method**: `POST`
*   **Body**:
    ```json
    {
      "email": "admin@globaltech.com",
      "password": "adminPassword123!"
    }
    ```
*   **Response**: Returns `{ "accessToken": "...", "refreshToken": "..." }`. Both `tenantId` and `ADMIN` role are embedded in the token.

### 2b. Company Creates a Tutor
*   **URL**: `/api/admin/users`
*   **Method**: `POST`
*   **Headers**: `Authorization: Bearer <admin_jwt>`
*   **Body**:
    ```json
    {
      "name": "Prof. Smith",
      "email": "smith@globaltech.com",
      "password": "tutorPassword!",
      "role": "TUTOR"
    }
    ```

---

## Step 3: Tutor Management (Add Students & Content)

The Tutor logs in (`/api/auth/login` with their credentials) and builds out the classroom by adding students and uploading Cloudinary content.

### 3a. Tutor Creates a Student
*   **URL**: `/api/admin/users`  *(Note: Tutors use the same endpoint, limited by their authority)*
*   **Method**: `POST`
*   **Headers**: `Authorization: Bearer <tutor_jwt>`
*   **Body**:
    ```json
    {
      "name": "Alice Student",
      "email": "alice@student.com",
      "password": "studentPassword!",
      "role": "STUDENT"
    }
    ```

### 3b. Tutor/Company Upload Content (Cloudinary)
*   **URL**: `/api/admin/content/upload`
*   **Method**: `POST`
*   **Headers**: `Authorization: Bearer <tutor_jwt>`, `Content-Type: multipart/form-data`
*   **Form Data Fields**:
    *   `title` (String): "Chapter 1: Intro"
    *   `description` (String): "Course Introduction"
    *   `file` (File, Optional): Physical PDF or Video upload (sent directly to Cloudinary).
    *   `videoUrl` (String, Optional): An external link instead of uploading a file.

---

## Step 4: Student Access (View Content)

The Student logs in (`/api/auth/login`) and retrieves secure access to the Cloudinary assets.

### 4a. List Content
*   **URL**: `/api/student/content`
*   **Method**: `GET`
*   **Headers**: `Authorization: Bearer <student_jwt>`
*   **Response**: JSON Array of available video/PDF objects.

### 4b. Access Cloudinary Video (Direct Token Stream)
1. **Get Short-lived Stream Token:**
    *   **URL**: `/api/video/stream/{contentId}/token`
    *   **Method**: `GET`
    *   **Headers**: `Authorization: Bearer <student_jwt>`
    *   **Response**: Returns the textual token string.

2. **Access Video (Used in `<video src="...">`):**
    *   **URL**: `/api/video/stream/{contentId}?streamToken=<the_token_string>`
    *   **Method**: `GET` *(Authorization Header NOT required physically, validated by token parameter)*
    *   **Result**: Secure 302 redirect directly to Cloudinary's Video CDN.

### 4c. Access Cloudinary PDF (Signed URL)
1. **Generate Signed Access URL:**
    *   **URL**: `/api/student/pdf/url/{contentId}`
    *   **Method**: `GET`
    *   **Headers**: `Authorization: Bearer <student_jwt>`
    *   **Response**: Returns a full URL string (e.g. `http://.../api/student/pdf/123?token=XXX&ts=YYY&email=ZZZ`)

2. **View PDF (Used in `<iframe>` or PDF.js):**
    *   **URL**: *(The exact string returned from step 1)*
    *   **Method**: `GET`
    *   **Result**: Secure 302 redirect to Cloudinary's PDF CDN or internal watermarked render.
