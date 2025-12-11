# API Reference - BSI E-Learning

**Base URL:** `https://elearning.bsi.ac.id`

---

## 1. Authentication

### 1.1 Get Login Page (Captcha)

```
GET /login
```

**Response:** HTML page containing CSRF token and captcha question.

**Parsing:**
```kotlin
// Extract CSRF token
val csrfToken = doc.select("meta[name=csrf-token]").attr("content")
    .ifEmpty { doc.select("input[name=_token]").attr("value") }

// Extract captcha (math question)
val captchaRegex = """Berapa hasil dari\s*(\d+\s*[+\-*/×÷x]\s*\d+)""".toRegex()
val question = captchaRegex.find(html)?.groupValues?.get(1)
```

---

### 1.2 Login

```
POST /login
Content-Type: application/x-www-form-urlencoded
```

**Request Body:**

| Field | Type | Description |
|-------|------|-------------|
| `_token` | string | CSRF token dari halaman login |
| `username` | string | NIM mahasiswa |
| `password` | string | Password |
| `captcha_answer` | string | Jawaban captcha (angka) |

**Response:**
- **Success:** Redirect ke `/dashboard` atau `/sch`
- **Failed:** HTML dengan pesan error di `.alert-danger`

**Cookies Set:**
- `XSRF-TOKEN` - Laravel XSRF token
- `mybest_session` - Session cookie

---

### 1.3 Logout

```
POST /logout
Content-Type: application/x-www-form-urlencoded
```

**Request Body:**

| Field | Type | Description |
|-------|------|-------------|
| `_token` | string | CSRF token |

---

### 1.4 Check Session

```
GET /dashboard
```

**Response:**
- **Valid:** HTML dengan tombol logout
- **Invalid:** Redirect ke `/login`

**Detection:**
```kotlin
fun isLoginPage(html: String): Boolean {
    val doc = Jsoup.parse(html)
    val hasUsernameInput = doc.select("input[name=username]").isNotEmpty()
    val hasPasswordInput = doc.select("input[name=password]").isNotEmpty()
    val hasCaptcha = html.contains("Berapa hasil", ignoreCase = true)
    return hasUsernameInput && (hasPasswordInput || hasCaptcha)
}
```

---

## 2. Schedule / Courses

### 2.1 Get Schedule

```
GET /sch
Cookie: XSRF-TOKEN=...; mybest_session=...
```

**Response:** HTML page dengan card jadwal

**HTML Structure:**
```html
<div class="pricing-plan">
    <div class="pricing-title">NAMA MATA KULIAH</div>
    <div class="pricing-save">Senin - 08:00-10:30</div>
    <div class="card-body">
        <div class="styled">Kode Dosen: ABC</div>
        <div class="styled">Kode MTK: 123</div>
        <div class="styled">SKS: 3</div>
        <div class="styled">No Ruang: A101</div>
    </div>
    <div class="pricing-footer">
        <a class="btn-primary" href="/absen-mhs/...">Masuk Kelas</a>
        <a href="/diskusi/...">Diskusi</a>
        <a href="/materi/...">Materi</a>
        <a href="/tugas/...">Tugas</a>
    </div>
</div>
```

**Parsed Data:**
```kotlin
data class Course(
    val id: Int,
    val name: String,           // Nama mata kuliah
    val schedule: String,       // "Senin - 08:00-10:30"
    val hari: String,           // "Senin"
    val jamMasuk: String,       // "08:00"
    val jamKeluar: String,      // "10:30"
    val kodeDosen: String,
    val kodeMTK: String,
    val sks: Int,
    val noRuang: String,
    val kelPraktek: String,
    val kodeGabung: String,
    val links: CourseLinks
)

data class CourseLinks(
    val masukKelas: String,     // /absen-mhs/{encrypted_id}
    val diskusi: String,
    val materi: String,
    val tugas: String,
    val absensi: String
)
```

---

### 2.2 Get Kuliah Pengganti

```
GET /kuliah-pengganti
Cookie: XSRF-TOKEN=...; mybest_session=...
```

**Response:** Same structure as `/sch`

---

## 3. Attendance / Presensi

### 3.1 Get Attendance Page

```
GET /absen-mhs/{encrypted_course_id}
Cookie: XSRF-TOKEN=...; mybest_session=...
```

**Response:** HTML page dengan form absensi dan tabel rekap.

**Extract Form Data:**
```kotlin
// CSRF Token
val token = """<input[^>]*name="_token"[^>]*value="([^"]+)"[^>]*>""".toRegex()
    .find(html)?.groupValues?.get(1)

// Pertemuan (encrypted)
val pertemuan = """<input[^>]*name="pertemuan"[^>]*value="([^"]+)"[^>]*>""".toRegex()
    .find(html)?.groupValues?.get(1)
```

---

### 3.2 Get Attendance Records (DataTables AJAX)

```
GET /rekap-side/{encrypted_course_id}?{datatables_params}
Cookie: XSRF-TOKEN=...; mybest_session=...
X-Requested-With: XMLHttpRequest
Accept: application/json
```

**Query Parameters (DataTables):**

| Parameter | Value | Description |
|-----------|-------|-------------|
| `draw` | 1 | DataTables draw counter |
| `start` | 0 | Offset |
| `length` | 100 | Limit (max records) |
| `order[0][column]` | 4 | Sort by column (4 = pertemuan) |
| `order[0][dir]` | desc | Sort direction |
| `columns[0][data]` | 0 | Column 0 = nomer |
| `columns[1][data]` | status_hadir | Column 1 = status |
| `columns[2][data]` | tgl_ajar_masuk | Column 2 = tanggal |
| `columns[3][data]` | nm_mtk | Column 3 = nama MK |
| `columns[4][data]` | pertemuan | Column 4 = pertemuan |
| `columns[5][data]` | berita_acara | Column 5 = berita acara |
| `columns[6][data]` | rangkuman | Column 6 = rangkuman |
| `_` | timestamp | Cache buster |

**Response (JSON):**
```json
{
    "draw": 1,
    "recordsTotal": 11,
    "recordsFiltered": 11,
    "data": [
        [1, "HADIR", "2025-09-24", "PENDIDIKAN AGAMA", "1", "", ""],
        [2, "HADIR", "2025-10-01", "PENDIDIKAN AGAMA", "2", "", ""],
        [3, "TIDAK HADIR", "2025-10-08", "PENDIDIKAN AGAMA", "3", "", ""]
    ]
}
```

**Column Mapping:**

| Index | Field | Description |
|-------|-------|-------------|
| 0 | nomer | Nomor urut baris |
| 1 | status_hadir | Status: HADIR / TIDAK HADIR |
| 2 | tgl_ajar_masuk | Tanggal (YYYY-MM-DD) |
| 3 | nm_mtk | Nama mata kuliah |
| 4 | pertemuan | Pertemuan ke-berapa |
| 5 | berita_acara | Catatan berita acara |
| 6 | rangkuman | Rangkuman materi |

---

### 3.3 Submit Attendance

```
POST /mhs-absen
Content-Type: application/x-www-form-urlencoded
Cookie: XSRF-TOKEN=...; mybest_session=...
Origin: https://elearning.bsi.ac.id
Referer: https://elearning.bsi.ac.id/absen-mhs/{encrypted_id}
```

**Request Body:**

| Field | Type | Description |
|-------|------|-------------|
| `_token` | string | CSRF token dari halaman absensi |
| `pertemuan` | string | Encrypted pertemuan value |
| `id` | string | Encrypted course ID |

**Response:**
- **Success:** Redirect atau HTML dengan "berhasil"
- **Error:** HTML dengan pesan error

**Possible Errors:**
- "Kamu sudah absen untuk pertemuan ini"
- "Kelas belum dimulai"
- "Waktu absensi sudah berakhir"

---

## 4. Assignments / Tugas

### 4.1 Get Assignments

```
GET /tugas/{encrypted_course_id}
Cookie: XSRF-TOKEN=...; mybest_session=...
```

**Response:** HTML page dengan 2 tabel:
1. **Data Penugasan** - Daftar tugas
2. **Data Nilai Tugas** - Nilai tugas yang sudah dikerjakan

**HTML Structure (Penugasan):**
```html
<table>
    <thead>
        <th>No</th>
        <th>Kode MTK</th>
        <th>Kelas</th>
        <th>Judul</th>
        <th>Deskripsi</th>
        <th>Pertemuan</th>
        <th>Mulai</th>
        <th>Selesai</th>
        <th>Created</th>
        <th>Aksi</th>
    </thead>
    <tbody>
        <tr>
            <td>1</td>
            <td>MTK001</td>
            <td>12.1A.01</td>
            <td>Tugas 1</td>
            <td>Deskripsi tugas</td>
            <td><center>5</center></td>
            <td>2025-10-01</td>
            <td>2025-10-15</td>
            <td>2025-09-25</td>
            <td>
                <form action="/download-file-tugas" method="POST">
                    <input name="_token" value="...">
                    <input name="id" value="...">
                    <input name="file" value="tugas.pdf">
                    <button>Unduh</button>
                </form>
                <a href="/assignment/send/...">Kerjakan</a>
            </td>
        </tr>
    </tbody>
</table>
```

**Parsed Data:**
```kotlin
data class Assignment(
    val no: Int,
    val kodeMtk: String,
    val kelas: String,
    val judul: String,
    val deskripsi: String,
    val pertemuan: String,
    val mulai: String,          // Start date
    val selesai: String,        // End date (deadline)
    val created: String,
    val linkUnduh: String,      // "FORM:token|id|filename" atau URL
    val linkKerjakan: String,   // /assignment/send/{id}
    val submittedLink: String   // Link yang sudah disubmit (jika ada)
)

data class AssignmentGrade(
    val no: Int,
    val kodeMtk: String,
    val judul: String,
    val linkTugas: String,
    val komentarDosen: String,
    val nilai: String,
    val created: String,
    val updated: String
)
```

---

### 4.2 Get Assignment Submit Page

```
GET /assignment/send/{encrypted_assignment_id}
Cookie: XSRF-TOKEN=...; mybest_session=...
```

**Response:** HTML form dengan hidden fields:
- `_token` - CSRF token
- `kd_mtk` - Kode mata kuliah
- `id_tugas` - ID tugas
- `nim` - NIM mahasiswa
- `kd_lokal` - Kode lokal/kelas
- Input field untuk link tugas

---

### 4.3 Submit Assignment

```
POST /assignment
Content-Type: application/x-www-form-urlencoded
Cookie: XSRF-TOKEN=...; mybest_session=...
```

**Request Body:**

| Field | Type | Description |
|-------|------|-------------|
| `_token` | string | CSRF token |
| `kd_mtk` | string | Kode mata kuliah |
| `id_tugas` | string | ID tugas |
| `nim` | string | NIM mahasiswa |
| `kd_lokal` | string | Kode lokal/kelas |
| `isi` | string | Link tugas (Google Drive, dll) |

---

## 5. Profile

### 5.1 Get Profile

```
GET /profil
Cookie: XSRF-TOKEN=...; mybest_session=...
```

**Response:** HTML page dengan form profil

**Parsed Data:**
```kotlin
data class UserProfile(
    val name: String,   // input[name=name]
    val email: String,  // input[name=email]
    val nim: String     // dari session
)
```

---

### 5.2 Update Profile

```
POST /foto-profil/update
Content-Type: application/x-www-form-urlencoded
Cookie: XSRF-TOKEN=...; mybest_session=...
X-CSRF-TOKEN: {csrf_token}
```

**Request Body:**

| Field | Type | Description |
|-------|------|-------------|
| `_method` | string | "patch" |
| `_token` | string | CSRF token |
| `name` | string | Nama baru |
| `email` | string | Email baru |

---

### 5.3 Change Password

```
POST /profil/update
Content-Type: application/x-www-form-urlencoded
Cookie: XSRF-TOKEN=...; mybest_session=...
X-CSRF-TOKEN: {csrf_token}
```

**Request Body:**

| Field | Type | Description |
|-------|------|-------------|
| `_method` | string | "patch" |
| `_token` | string | CSRF token |
| `current_password` | string | Password lama |
| `password` | string | Password baru |
| `password_confirmation` | string | Konfirmasi password baru |

---

## 6. File Download

### 6.1 Download Assignment File

```
POST /download-file-tugas
Content-Type: application/x-www-form-urlencoded
Cookie: XSRF-TOKEN=...; mybest_session=...
```

**Request Body:**

| Field | Type | Description |
|-------|------|-------------|
| `_token` | string | CSRF token |
| `id` | string | File/assignment ID |
| `file` | string | Filename |

**Response:** Binary file content

**Internal Format:**
Aplikasi menyimpan link download dalam format:
```
FORM:{token}|{id}|{filename}
```
Contoh: `FORM:abc123|456|tugas.pdf`

---

## 7. Data Models

### Session State
```kotlin
data class SessionState(
    var cookies: String = "",
    var xsrfToken: String = "",
    var csrfToken: String = "",
    var username: String = "",
    var isLoggedIn: Boolean = false,
    var loginTime: Long? = null
)
```

### API Result
```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}
```

### Attendance Status
```kotlin
data class AttendanceStatus(
    val currentPertemuan: Int?,
    val totalRecords: Int,
    val alreadyAttended: Boolean,
    val latestPertemuan: Int?
)
```

---

## 8. Error Handling

### HTTP Error Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Parse response |
| 302 | Redirect | Follow redirect / check login |
| 419 | CSRF Expired | Refresh token, retry |
| 422 | Validation Error | Show error message |
| 500 | Server Error | Retry later |

### Error Messages

```kotlin
// Network errors
"Tidak ada koneksi internet"
"Koneksi timeout - server BSI lambat merespon"

// Session errors
"Sesi habis, silakan login ulang"
"Session expired, silakan login ulang"

// Attendance errors
"Kamu sudah absen untuk pertemuan ini"
"Kelas belum dimulai"
"Waktu absensi sudah berakhir"
"Data pertemuan tidak tersedia - kelas mungkin belum dimulai"
```

---

## 9. Important Notes

1. **Encrypted IDs**: Semua ID (course, assignment, dll) di-encrypt oleh server menggunakan Laravel encryption. Format: Base64 encoded JSON dengan `iv`, `value`, `mac`, `tag`.

2. **CSRF Protection**: Semua POST request membutuhkan `_token` yang valid. Token bisa expired, jadi perlu di-refresh jika dapat error 419.

3. **DataTables**: Endpoint rekap menggunakan DataTables server-side processing. Response dalam format JSON dengan array `data`.

4. **Cookies**: Session dikelola via cookies `XSRF-TOKEN` dan `mybest_session`. Harus disertakan di setiap request.

5. **User-Agent**: Gunakan User-Agent browser yang valid untuk menghindari blocking.

6. **Request Headers**: Untuk AJAX requests, tambahkan:
   ```
   X-Requested-With: XMLHttpRequest
   Accept: application/json
   ```
