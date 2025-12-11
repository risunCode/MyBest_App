# 🚀 API Implementation Plan

Proposal lengkap untuk mengintegrasikan BSI E-Learning API ke aplikasi MyBest UBSI.

---

## 📋 Overview

### Tujuan
Mengubah aplikasi dari **dummy data (Guest Mode)** menjadi **fully functional** dengan data real dari BSI E-Learning.

### Scope
| Feature | Current | Target |
|---------|---------|--------|
| Login | Mock login | Real auth dengan captcha |
| Schedule | Dummy data | Parse dari `/sch` |
| Attendance | UI only | Submit ke `/mhs-absen` |
| Assignments | UI only | Parse + submit tugas |
| Profile | Static | Sync dari `/profil` |

### Tech Stack Baru
- **OkHttp 4.12.0** - HTTP client dengan cookie management
- **Jsoup 1.17.2** - HTML parsing
- **Kotlin Coroutines** - Async operations (sudah ada)

---

## 🏗️ Architecture

### New Package Structure

```
com.risuncode.mybest/
├── data/
│   ├── api/                    # [NEW] Network layer
│   │   ├── ApiClient.kt        # OkHttp singleton
│   │   ├── ApiService.kt       # API methods
│   │   ├── SessionManager.kt   # Cookie & token management
│   │   └── HtmlParser.kt       # Jsoup parsing utilities
│   ├── model/                  # [NEW] API response models
│   │   ├── LoginResult.kt
│   │   ├── CourseResponse.kt
│   │   ├── AttendanceResponse.kt
│   │   └── AssignmentResponse.kt
│   ├── repository/
│   │   └── AppRepository.kt    # [MODIFY] Add API calls
│   └── ...existing...
└── ...existing...
```

### Data Flow

```
UI (Activity/Fragment)
       ↓
   ViewModel (optional, bisa langsung)
       ↓
   AppRepository
       ↓
   ┌───┴───┬───────────┐
   ↓       ↓           ↓
Room DB  ApiService  PreferenceManager
           ↓
    ┌──────┴──────┐
    ↓             ↓
ApiClient    HtmlParser
(OkHttp)     (Jsoup)
```

---

## 📦 Dependencies

### build.gradle.kts (app)

```kotlin
// Network
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// HTML Parsing
implementation("org.jsoup:jsoup:1.17.2")
```

---

## 🔧 Implementation Details

### Phase 1: Network Layer Setup

#### 1.1 ApiClient.kt
```kotlin
object ApiClient {
    private const val BASE_URL = "https://elearning.bsi.ac.id"
    
    private val cookieJar = SessionCookieJar()
    
    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)  // Handle redirects manually
        .addInterceptor(loggingInterceptor)
        .build()
    
    fun buildUrl(path: String): String = "$BASE_URL$path"
}
```

#### 1.2 SessionManager.kt
```kotlin
class SessionManager(private val prefs: PreferenceManager) {
    var csrfToken: String
        get() = prefs.csrfToken
        set(value) { prefs.csrfToken = value }
    
    var isLoggedIn: Boolean
        get() = prefs.isLoggedIn
        set(value) { prefs.isLoggedIn = value }
    
    fun clearSession() {
        prefs.clearAuthData()
    }
    
    fun isSessionValid(): Boolean {
        // Check token expiry
        return isLoggedIn && prefs.tokenExpiry > System.currentTimeMillis()
    }
}
```

#### 1.3 SessionCookieJar.kt
```kotlin
class SessionCookieJar : CookieJar {
    private val cookieStore = mutableMapOf<String, List<Cookie>>()
    
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host] = cookies
    }
    
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }
    
    fun clearCookies() {
        cookieStore.clear()
    }
}
```

---

### Phase 2: API Service

#### 2.1 ApiService.kt
```kotlin
class ApiService(
    private val client: OkHttpClient,
    private val sessionManager: SessionManager
) {
    // ========== AUTH ==========
    
    suspend fun getLoginPage(): Result<LoginPageData> = withContext(Dispatchers.IO) {
        // GET /login → extract CSRF + captcha
    }
    
    suspend fun login(nim: String, password: String, captchaAnswer: String): Result<LoginResult>
    
    suspend fun logout(): Result<Unit>
    
    suspend fun checkSession(): Result<Boolean>
    
    // ========== SCHEDULE ==========
    
    suspend fun getSchedule(): Result<List<Course>>
    
    suspend fun getReplacementClasses(): Result<List<Course>>
    
    // ========== ATTENDANCE ==========
    
    suspend fun getAttendancePage(courseId: String): Result<AttendancePageData>
    
    suspend fun getAttendanceRecords(courseId: String): Result<List<AttendanceRecord>>
    
    suspend fun submitAttendance(courseId: String, pertemuan: String): Result<AttendanceResult>
    
    // ========== ASSIGNMENTS ==========
    
    suspend fun getAssignments(courseId: String): Result<AssignmentPageData>
    
    suspend fun submitAssignment(params: AssignmentSubmitParams): Result<Unit>
    
    suspend fun downloadFile(token: String, id: String, filename: String): Result<ByteArray>
    
    // ========== PROFILE ==========
    
    suspend fun getProfile(): Result<UserProfile>
    
    suspend fun updateProfile(name: String, email: String): Result<Unit>
    
    suspend fun changePassword(current: String, new: String): Result<Unit>
}
```

---

### Phase 3: HTML Parser

#### 3.1 HtmlParser.kt
```kotlin
object HtmlParser {
    
    // ========== LOGIN ==========
    
    fun extractCsrfToken(html: String): String? {
        val doc = Jsoup.parse(html)
        return doc.select("meta[name=csrf-token]").attr("content")
            .ifEmpty { doc.select("input[name=_token]").attr("value") }
            .ifEmpty { null }
    }
    
    fun extractCaptchaQuestion(html: String): String? {
        val regex = """Berapa hasil dari\s*(\d+\s*[+\-*/×÷x]\s*\d+)""".toRegex()
        return regex.find(html)?.groupValues?.get(1)
    }
    
    fun solveCaptcha(question: String): Int {
        // Parse "5 + 3" → 8
        val parts = question.split(Regex("\\s*[+\\-*/×÷x]\\s*"))
        val operator = question.find { it in "+-*/×÷x" }
        val a = parts[0].trim().toInt()
        val b = parts[1].trim().toInt()
        return when (operator) {
            '+' -> a + b
            '-' -> a - b
            '*', '×', 'x' -> a * b
            '/', '÷' -> a / b
            else -> 0
        }
    }
    
    fun isLoginPage(html: String): Boolean {
        return html.contains("input[name=username]") || 
               html.contains("Berapa hasil", ignoreCase = true)
    }
    
    // ========== SCHEDULE ==========
    
    fun parseSchedule(html: String): List<Course> {
        val doc = Jsoup.parse(html)
        val courses = mutableListOf<Course>()
        
        doc.select(".pricing-plan").forEach { card ->
            courses.add(Course(
                name = card.select(".pricing-title").text(),
                schedule = card.select(".pricing-save").text(),
                // ... parse other fields
            ))
        }
        return courses
    }
    
    // ========== ATTENDANCE ==========
    
    fun parseAttendanceForm(html: String): AttendanceFormData? {
        val token = extractCsrfToken(html)
        val pertemuan = Regex("""name="pertemuan"[^>]*value="([^"]+)"""")
            .find(html)?.groupValues?.get(1)
        return if (token != null && pertemuan != null) {
            AttendanceFormData(token, pertemuan)
        } else null
    }
    
    fun parseAttendanceRecords(json: String): List<AttendanceRecord> {
        val response = Gson().fromJson(json, DataTablesResponse::class.java)
        return response.data.map { row ->
            AttendanceRecord(
                no = row[0].toString().toIntOrNull() ?: 0,
                status = row[1].toString(),
                date = row[2].toString(),
                subject = row[3].toString(),
                pertemuan = row[4].toString(),
                beritaAcara = row[5].toString(),
                rangkuman = row[6].toString()
            )
        }
    }
    
    // ========== ASSIGNMENTS ==========
    
    fun parseAssignments(html: String): List<Assignment> {
        val doc = Jsoup.parse(html)
        // Parse table rows...
    }
}
```

---

### Phase 4: Repository Integration

#### 4.1 AppRepository.kt (Modified)
```kotlin
class AppRepository(
    private val database: AppDatabase,
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    // ========== SYNC METHODS ==========
    
    suspend fun syncScheduleFromServer(): Result<Unit> {
        return apiService.getSchedule().mapCatching { courses ->
            // Convert API response to entities
            val entities = courses.map { it.toEntity() }
            // Clear old and insert new
            scheduleDao.deleteAll()
            scheduleDao.insertAll(entities)
        }
    }
    
    suspend fun syncUserFromServer(): Result<Unit> {
        return apiService.getProfile().mapCatching { profile ->
            val entity = profile.toEntity()
            userDao.deleteAll()
            userDao.insert(entity)
        }
    }
    
    // ========== LOGIN FLOW ==========
    
    suspend fun performLogin(nim: String, password: String): Result<Unit> {
        // 1. Get login page (CSRF + captcha)
        val loginPage = apiService.getLoginPage().getOrElse { return Result.failure(it) }
        
        // 2. Solve captcha
        val answer = HtmlParser.solveCaptcha(loginPage.captchaQuestion)
        
        // 3. Submit login
        val loginResult = apiService.login(nim, password, answer.toString())
            .getOrElse { return Result.failure(it) }
        
        // 4. Update session
        sessionManager.isLoggedIn = true
        sessionManager.csrfToken = loginResult.newCsrfToken
        
        // 5. Sync data
        syncScheduleFromServer()
        syncUserFromServer()
        
        return Result.success(Unit)
    }
    
    // ========== ATTENDANCE ==========
    
    suspend fun submitAttendance(scheduleId: Long): Result<AttendanceResult> {
        val schedule = scheduleDao.getById(scheduleId) ?: return Result.failure(
            Exception("Schedule not found")
        )
        
        // Get attendance form data
        val formData = apiService.getAttendancePage(schedule.courseId).getOrElse {
            return Result.failure(it)
        }
        
        // Submit
        val result = apiService.submitAttendance(
            courseId = schedule.courseId,
            pertemuan = formData.pertemuan
        ).getOrElse { return Result.failure(it) }
        
        // Update local DB
        if (result.success) {
            scheduleDao.updateAttendance(scheduleId, true, System.currentTimeMillis())
        }
        
        return Result.success(result)
    }
}
```

---

### Phase 5: UI Integration

#### 5.1 LoginActivity Changes
```kotlin
// Replace mock login with real login
binding.btnLogin.setOnClickListener {
    val nim = binding.etNim.text.toString()
    val password = binding.etPassword.text.toString()
    
    showLoading(true)
    
    lifecycleScope.launch {
        repository.performLogin(nim, password)
            .onSuccess {
                showToast("Login berhasil!")
                navigateToMain()
            }
            .onFailure { error ->
                showError(error.message ?: "Login gagal")
            }
        showLoading(false)
    }
}
```

#### 5.2 Dashboard Sync
```kotlin
// DashboardFragment
private fun refreshData() {
    binding.swipeRefresh.isRefreshing = true
    
    lifecycleScope.launch {
        repository.syncScheduleFromServer()
            .onFailure { showError(it.message) }
        
        binding.swipeRefresh.isRefreshing = false
    }
}
```

#### 5.3 Presensi Submit
```kotlin
// PresensiActivity
private fun submitAttendance() {
    showLoading(true)
    
    lifecycleScope.launch {
        repository.submitAttendance(currentScheduleId)
            .onSuccess { result ->
                when (result.code) {
                    200 -> {
                        showSuccessDialog("Berhasil absen!")
                        refreshAttendanceList()
                    }
                    419 -> {
                        showError("Session expired")
                        navigateToLogin()
                    }
                    else -> showError(result.message)
                }
            }
            .onFailure { showError(it.message) }
        
        showLoading(false)
    }
}
```

---

## 📁 New Files Summary

| File | Purpose |
|------|---------|
| `data/api/ApiClient.kt` | OkHttp singleton with cookie jar |
| `data/api/ApiService.kt` | All API method implementations |
| `data/api/SessionManager.kt` | Token & session management |
| `data/api/SessionCookieJar.kt` | Cookie persistence |
| `data/api/HtmlParser.kt` | Jsoup parsing utilities |
| `data/model/LoginResult.kt` | Login response model |
| `data/model/CourseResponse.kt` | Schedule parsing model |
| `data/model/AttendanceResponse.kt` | Attendance models |
| `data/model/AssignmentResponse.kt` | Assignment models |

---

## 🔄 Modified Files

| File | Changes |
|------|---------|
| `build.gradle.kts` | Add OkHttp + Jsoup dependencies |
| `AppRepository.kt` | Add sync methods, login flow, API integration |
| `PreferenceManager.kt` | Already has auth properties ✅ |
| `LoginActivity.kt` | Replace mock login with real API |
| `DashboardFragment.kt` | Add refresh/sync |
| `JadwalFragment.kt` | Add refresh/sync |
| `PresensiActivity.kt` | Add submit attendance |
| `TugasActivity.kt` | Add submit assignment |
| `ProfilFragment.kt` | Add profile sync |

---

## ⚠️ Error Handling Strategy

### Network Errors
```kotlin
sealed class ApiError : Exception() {
    object NetworkError : ApiError()           // No internet
    object TimeoutError : ApiError()           // Request timeout
    object SessionExpired : ApiError()         // 419 CSRF expired
    object ServerError : ApiError()            // 500
    data class HttpError(val code: Int) : ApiError()
    data class ParseError(val msg: String) : ApiError()
}
```

### Session Recovery
```kotlin
// Interceptor untuk auto-refresh session
class SessionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        
        // Check if redirected to login
        if (response.isRedirect && response.header("Location")?.contains("login") == true) {
            // Clear session, notify UI
            sessionManager.clearSession()
            throw ApiError.SessionExpired
        }
        
        return response
    }
}
```

---

## 📅 Implementation Timeline

| Phase | Tasks | Est. Time |
|-------|-------|-----------|
| **Phase 1** | Network layer (ApiClient, SessionManager, CookieJar) | 2-3 hours |
| **Phase 2** | ApiService methods (all endpoints) | 4-5 hours |
| **Phase 3** | HtmlParser (all parsing functions) | 3-4 hours |
| **Phase 4** | Repository integration + sync logic | 3-4 hours |
| **Phase 5** | UI integration (all activities/fragments) | 4-5 hours |
| **Phase 6** | Testing & error handling polish | 2-3 hours |

**Total Estimate:** 18-24 hours of development

---

## ✅ Checklist

- [ ] Add OkHttp + Jsoup dependencies
- [ ] Create `data/api/` package
- [ ] Implement ApiClient
- [ ] Implement SessionCookieJar
- [ ] Implement SessionManager
- [ ] Implement HtmlParser
- [ ] Implement ApiService (all methods)
- [ ] Create response models
- [ ] Modify AppRepository
- [ ] Update LoginActivity
- [ ] Update DashboardFragment
- [ ] Update JadwalFragment
- [ ] Update PresensiActivity
- [ ] Update TugasActivity
- [ ] Update ProfilFragment
- [ ] Add error handling
- [ ] Test all flows
- [ ] Remove Guest Mode dummy switching (optional)

---

## 🎯 Success Criteria

1. ✅ User dapat login dengan NIM + password + captcha auto-solve
2. ✅ Jadwal kuliah sync dari server
3. ✅ Submit presensi berfungsi dengan feedback
4. ✅ Submit tugas berfungsi
5. ✅ Profile sync dari server
6. ✅ Session management (auto-logout on 419)
7. ✅ Error messages yang jelas
8. ✅ Pull-to-refresh sync data
