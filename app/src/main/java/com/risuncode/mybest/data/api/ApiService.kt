package com.risuncode.mybest.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.IOException

/**
 * API Service untuk BSI E-Learning
 */
class ApiService {
    
    private val client = ApiClient.client
    
    // ========== AUTH ==========
    
    /**
     * Get login page - returns CSRF token and captcha question
     * Auto-retries with HTTP if HTTPS fails
     */
    suspend fun getLoginPage(): Result<LoginPageData> = withContext(Dispatchers.IO) {
        try {
            val html = getPageHtmlWithFallback("/login")
                ?: return@withContext Result.failure(IOException("Tidak dapat terhubung ke server BSI"))
            
            val csrfToken = HtmlParser.extractCsrfToken(html)
                ?: return@withContext Result.failure(IOException("CSRF token not found"))
            
            val captchaQuestion = HtmlParser.extractCaptchaQuestion(html)
                ?: return@withContext Result.failure(IOException("Captcha not found"))
            
            Result.success(LoginPageData(
                csrfToken = csrfToken,
                captchaQuestion = captchaQuestion,
                captchaAnswer = HtmlParser.solveCaptcha(captchaQuestion)
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Login with NIM, password, and captcha
     */
    suspend fun login(nim: String, password: String, csrfToken: String, captchaAnswer: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("_token", csrfToken)
                .add("username", nim)
                .add("password", password)
                .add("captcha_answer", captchaAnswer.toString())
                .build()
            
            val request = Request.Builder()
                .url(ApiClient.buildUrl("/login"))
                .post(formBody)
                .header("Referer", ApiClient.buildUrl("/login"))
                .build()
            
            val response = client.newCall(request).execute()
            
            // Check for redirect (successful login redirects to dashboard/sch)
            if (response.code == 302) {
                val location = response.header("Location") ?: ""
                if (location.contains("login")) {
                    // Redirected back to login = failed
                    return@withContext Result.failure(IOException("Login gagal - kredensial salah"))
                }
                // Follow redirect to complete session
                followRedirect(location)
                return@withContext Result.success(Unit)
            }
            
            // Check response body for error
            val html = response.body?.string() ?: ""
            if (HtmlParser.isLoginPage(html)) {
                val errorMsg = HtmlParser.extractLoginError(html) ?: "Login gagal"
                return@withContext Result.failure(IOException(errorMsg))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Follow redirect URL
     */
    private suspend fun followRedirect(location: String) {
        try {
            val url = if (location.startsWith("http")) location else ApiClient.buildUrl(location)
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            // Ignore redirect errors
        }
    }
    
    /**
     * Logout
     */
    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get CSRF token from any page
            val dashboardHtml = getPageHtml("/dashboard")
            val csrfToken = HtmlParser.extractCsrfToken(dashboardHtml ?: "") ?: ""
            
            val formBody = FormBody.Builder()
                .add("_token", csrfToken)
                .build()
            
            val request = Request.Builder()
                .url(ApiClient.buildUrl("/logout"))
                .post(formBody)
                .build()
            
            client.newCall(request).execute().close()
            ApiClient.clearSession()
            
            Result.success(Unit)
        } catch (e: Exception) {
            ApiClient.clearSession()
            Result.failure(e)
        }
    }
    
    /**
     * Check if session is still valid
     */
    suspend fun checkSession(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val html = getPageHtml("/dashboard") ?: return@withContext Result.success(false)
            val isValid = !HtmlParser.isLoginPage(html)
            Result.success(isValid)
        } catch (e: Exception) {
            Result.success(false)
        }
    }
    
    // ========== SCHEDULE ==========
    
    /**
     * Get schedule from /sch
     */
    suspend fun getSchedule(): Result<List<ParsedCourse>> = withContext(Dispatchers.IO) {
        try {
            val html = getPageHtml("/sch") 
                ?: return@withContext Result.failure(IOException("Failed to get schedule page"))
            
            if (HtmlParser.isLoginPage(html)) {
                return@withContext Result.failure(SessionExpiredException())
            }
            
            val courses = HtmlParser.parseSchedule(html)
            Result.success(courses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get replacement classes
     */
    suspend fun getReplacementClasses(): Result<List<ParsedCourse>> = withContext(Dispatchers.IO) {
        try {
            val html = getPageHtml("/kuliah-pengganti")
                ?: return@withContext Result.failure(IOException("Failed to get replacement classes"))
            
            if (HtmlParser.isLoginPage(html)) {
                return@withContext Result.failure(SessionExpiredException())
            }
            
            val courses = HtmlParser.parseSchedule(html)
            Result.success(courses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== ATTENDANCE ==========
    
    /**
     * Get attendance page
     */
    suspend fun getAttendancePage(encryptedCourseId: String): Result<AttendancePageData> = withContext(Dispatchers.IO) {
        try {
            val html = getPageHtml("/absen-mhs/$encryptedCourseId")
                ?: return@withContext Result.failure(IOException("Failed to get attendance page"))
            
            if (HtmlParser.isLoginPage(html)) {
                return@withContext Result.failure(SessionExpiredException())
            }
            
            val formData = HtmlParser.extractAttendanceFormData(html)
            
            Result.success(AttendancePageData(
                html = html,
                formData = formData,
                canAttend = formData != null
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get attendance records via DataTables AJAX
     */
    suspend fun getAttendanceRecords(encryptedCourseId: String): Result<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        try {
            val url = "${ApiClient.BASE_URL}/rekap-side/$encryptedCourseId?draw=1&start=0&length=100"
            
            val request = Request.Builder()
                .url(url)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/json")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val json = response.body?.string()
            response.close()
            
            if (json == null) return@withContext Result.success(emptyList())
            
            val records = HtmlParser.parseAttendanceRecords(json)
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Submit attendance
     */
    suspend fun submitAttendance(encryptedCourseId: String, formData: AttendanceFormData): Result<AttendanceResult> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("_token", formData.token)
                .add("pertemuan", formData.pertemuan)
                .add("id", encryptedCourseId)
                .build()
            
            val request = Request.Builder()
                .url(ApiClient.buildUrl("/mhs-absen"))
                .post(formBody)
                .header("Referer", ApiClient.buildUrl("/absen-mhs/$encryptedCourseId"))
                .build()
            
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            
            // Check for success/error messages
            val success = html.contains("berhasil", ignoreCase = true) ||
                         response.code == 302
            
            val message = when {
                html.contains("sudah absen", ignoreCase = true) -> "Kamu sudah absen untuk pertemuan ini"
                html.contains("belum dimulai", ignoreCase = true) -> "Kelas belum dimulai"
                html.contains("berakhir", ignoreCase = true) -> "Waktu absensi sudah berakhir"
                success -> "Berhasil absen!"
                else -> "Gagal absen"
            }
            
            Result.success(AttendanceResult(success = success, message = message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== ASSIGNMENTS ==========
    
    /**
     * Get assignments for a course
     */
    suspend fun getAssignments(encryptedCourseId: String): Result<List<ParsedAssignment>> = withContext(Dispatchers.IO) {
        try {
            val html = getPageHtml("/tugas/$encryptedCourseId")
                ?: return@withContext Result.failure(IOException("Failed to get assignments"))
            
            if (HtmlParser.isLoginPage(html)) {
                return@withContext Result.failure(SessionExpiredException())
            }
            
            val assignments = HtmlParser.parseAssignments(html)
            Result.success(assignments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get assignment submit form
     */
    suspend fun getAssignmentForm(encryptedAssignmentId: String): Result<AssignmentFormData> = withContext(Dispatchers.IO) {
        try {
            val html = getPageHtml("/assignment/send/$encryptedAssignmentId")
                ?: return@withContext Result.failure(IOException("Failed to get assignment form"))
            
            val formData = HtmlParser.extractAssignmentFormData(html)
                ?: return@withContext Result.failure(IOException("Form data not found"))
            
            Result.success(formData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Submit assignment
     */
    suspend fun submitAssignment(formData: AssignmentFormData, link: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("_token", formData.token)
                .add("kd_mtk", formData.kdMtk)
                .add("id_tugas", formData.idTugas)
                .add("nim", formData.nim)
                .add("kd_lokal", formData.kdLokal)
                .add("isi", link)
                .build()
            
            val request = Request.Builder()
                .url(ApiClient.buildUrl("/assignment"))
                .post(formBody)
                .build()
            
            client.newCall(request).execute().close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Download assignment file
     */
    suspend fun downloadFile(token: String, id: String, filename: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("_token", token)
                .add("id", id)
                .add("file", filename)
                .build()
            
            val request = Request.Builder()
                .url(ApiClient.buildUrl("/download-file-tugas"))
                .post(formBody)
                .build()
            
            val response = client.newCall(request).execute()
            val bytes = response.body?.bytes()
                ?: return@withContext Result.failure(IOException("Empty file"))
            
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== PROFILE ==========
    
    /**
     * Get profile
     */
    suspend fun getProfile(): Result<ParsedProfile> = withContext(Dispatchers.IO) {
        try {
            val html = getPageHtml("/profil")
                ?: return@withContext Result.failure(IOException("Failed to get profile"))
            
            if (HtmlParser.isLoginPage(html)) {
                return@withContext Result.failure(SessionExpiredException())
            }
            
            val profile = HtmlParser.parseProfile(html)
                ?: return@withContext Result.failure(IOException("Profile not found"))
            
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update profile (name and email)
     * POST /foto-profil/update
     */
    suspend fun updateProfile(name: String, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // First get CSRF token from profile page
            val profileHtml = getPageHtml("/profil")
                ?: return@withContext Result.failure(IOException("Failed to get profile page"))
            
            if (HtmlParser.isLoginPage(profileHtml)) {
                return@withContext Result.failure(SessionExpiredException())
            }
            
            val csrfToken = HtmlParser.extractCsrfToken(profileHtml)
                ?: return@withContext Result.failure(IOException("CSRF token not found"))
            
            val formBody = FormBody.Builder()
                .add("_method", "patch")
                .add("_token", csrfToken)
                .add("name", name)
                .add("email", email)
                .build()
            
            val request = Request.Builder()
                .url(ApiClient.buildUrl("/foto-profil/update"))
                .post(formBody)
                .header("Referer", ApiClient.buildUrl("/profil"))
                .header("X-CSRF-TOKEN", csrfToken)
                .build()
            
            val response = client.newCall(request).execute()
            response.close()
            
            if (response.code in 200..302) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to update profile: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Change password
     * POST /profil/update
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // First get CSRF token from profile page
            val profileHtml = getPageHtml("/profil")
                ?: return@withContext Result.failure(IOException("Failed to get profile page"))
            
            if (HtmlParser.isLoginPage(profileHtml)) {
                return@withContext Result.failure(SessionExpiredException())
            }
            
            val csrfToken = HtmlParser.extractCsrfToken(profileHtml)
                ?: return@withContext Result.failure(IOException("CSRF token not found"))
            
            val formBody = FormBody.Builder()
                .add("_method", "patch")
                .add("_token", csrfToken)
                .add("current_password", currentPassword)
                .add("password", newPassword)
                .add("password_confirmation", newPassword)
                .build()
            
            val request = Request.Builder()
                .url(ApiClient.buildUrl("/profil/update"))
                .post(formBody)
                .header("Referer", ApiClient.buildUrl("/profil"))
                .header("X-CSRF-TOKEN", csrfToken)
                .build()
            
            val response = client.newCall(request).execute()
            val html = response.body?.string()
            response.close()
            
            // Check for errors in response
            if (html != null && html.contains("error", ignoreCase = true)) {
                return@withContext Result.failure(IOException("Password change failed - check current password"))
            }
            
            if (response.code in 200..302) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to change password: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== HELPER ==========
    
    /**
     * Get page HTML with automatic HTTP/HTTPS fallback
     * Tries HTTPS first, falls back to HTTP if SSL fails
     */
    private fun getPageHtmlWithFallback(path: String): String? {
        // Try current protocol first
        var html = getPageHtml(path)
        
        if (html != null) return html
        
        // If failed and using HTTPS, try HTTP
        if (ApiClient.BASE_URL == ApiClient.BASE_URL_HTTPS) {
            ApiClient.switchToHttp()
            html = getPageHtml(path)
            
            // If HTTP works, stay with it
            if (html != null) return html
            
            // If both fail, switch back to HTTPS for next attempt
            ApiClient.switchToHttps()
        } else {
            // If using HTTP failed, try HTTPS
            ApiClient.switchToHttps()
            html = getPageHtml(path)
            
            if (html != null) return html
            
            // Switch back to HTTP
            ApiClient.switchToHttp()
        }
        
        return null
    }
    
    /**
     * Get page HTML content
     */
    private fun getPageHtml(path: String): String? {
        return try {
            val request = Request.Builder()
                .url(ApiClient.buildUrl(path))
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            // Handle redirects
            if (response.code == 302) {
                val location = response.header("Location")
                response.close()
                if (location == null) return null
                return getPageHtml(location)
            }
            
            val content = response.body?.string()
            response.close()
            content
        } catch (e: javax.net.ssl.SSLHandshakeException) {
            // SSL error - will trigger fallback
            null
        } catch (e: Exception) {
            null
        }
    }
}

// ========== DATA CLASSES ==========

data class LoginPageData(
    val csrfToken: String,
    val captchaQuestion: String,
    val captchaAnswer: Int
)

data class AttendancePageData(
    val html: String,
    val formData: AttendanceFormData?,
    val canAttend: Boolean
)

data class AttendanceResult(
    val success: Boolean,
    val message: String
)

class SessionExpiredException : IOException("Session expired - silakan login ulang")
