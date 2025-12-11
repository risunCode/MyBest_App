package com.risuncode.mybest.data.repository

import com.risuncode.mybest.data.AppDatabase
import com.risuncode.mybest.data.api.ApiService
import com.risuncode.mybest.data.api.AttendanceFormData
import com.risuncode.mybest.data.api.AttendanceRecord
import com.risuncode.mybest.data.api.AttendanceResult
import com.risuncode.mybest.data.api.ParsedAssignment
import com.risuncode.mybest.data.api.ParsedCourse
import com.risuncode.mybest.data.entity.NotificationEntity
import com.risuncode.mybest.data.entity.ScheduleEntity
import com.risuncode.mybest.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository sebagai single source of truth untuk akses data.
 * Menggabungkan akses ke Room Database dan API Service.
 */
class AppRepository(private val database: AppDatabase) {
    
    private val userDao = database.userDao()
    private val scheduleDao = database.scheduleDao()
    private val notificationDao = database.notificationDao()
    
    // API Service
    private val apiService = ApiService()
    
    // ==================== AUTH / LOGIN ====================
    
    /**
     * Perform full login flow: get captcha, solve it, submit login
     */
    suspend fun performLogin(nim: String, password: String): Result<Unit> {
        // 1. Get login page with CSRF and captcha
        val loginPageResult = apiService.getLoginPage()
        val loginPage = loginPageResult.getOrElse { 
            return Result.failure(it) 
        }
        
        // 2. Submit login with auto-solved captcha
        val loginResult = apiService.login(
            nim = nim,
            password = password,
            csrfToken = loginPage.csrfToken,
            captchaAnswer = loginPage.captchaAnswer
        )
        
        loginResult.getOrElse { 
            return Result.failure(it) 
        }
        
        // 3. Sync user data from profile
        syncUserFromServer()
        
        return Result.success(Unit)
    }
    
    /**
     * Logout and clear all data
     */
    suspend fun performLogout(): Result<Unit> {
        apiService.logout()
        clearAllData()
        return Result.success(Unit)
    }
    
    /**
     * Check if session is valid
     */
    suspend fun checkSession(): Boolean {
        return apiService.checkSession().getOrDefault(false)
    }
    
    // ==================== SYNC FROM SERVER ====================
    
    /**
     * Sync schedule from BSI server
     */
    suspend fun syncScheduleFromServer(): Result<List<ScheduleEntity>> {
        val result = apiService.getSchedule()
        val courses = result.getOrElse { 
            return Result.failure(it) 
        }
        
        // Convert to entities
        val entities = courses.map { it.toScheduleEntity() }
        
        // Clear old and insert new
        scheduleDao.deleteAllSchedules()
        scheduleDao.insertSchedules(entities)
        
        return Result.success(entities)
    }
    
    /**
     * Sync user profile from server
     */
    suspend fun syncUserFromServer(): Result<UserEntity> {
        val result = apiService.getProfile()
        val profile = result.getOrElse { 
            return Result.failure(it) 
        }
        
        // Create or update user entity (nim is primary key)
        val userEntity = UserEntity(
            nim = "", // Will be filled from preferences
            name = profile.name,
            email = profile.email,
            prodi = "",
            angkatan = "",
            isGuestMode = false
        )
        
        userDao.deleteAllUsers()
        userDao.insertUser(userEntity)
        
        return Result.success(userEntity)
    }
    
    // ==================== ATTENDANCE API ====================
    
    /**
     * Get attendance records for a course
     */
    suspend fun getAttendanceRecords(encryptedCourseId: String): Result<List<AttendanceRecord>> {
        return apiService.getAttendanceRecords(encryptedCourseId)
    }
    
    /**
     * Submit attendance for a course
     */
    suspend fun submitAttendance(encryptedCourseId: String): Result<AttendanceResult> {
        // Get form data first
        val pageResult = apiService.getAttendancePage(encryptedCourseId)
        val page = pageResult.getOrElse { 
            return Result.failure(it) 
        }
        
        val formData = page.formData 
            ?: return Result.failure(Exception("Data pertemuan tidak tersedia"))
        
        // Submit
        return apiService.submitAttendance(encryptedCourseId, formData)
    }
    
    // ==================== ASSIGNMENT API ====================
    
    /**
     * Get assignments for a course
     */
    suspend fun getAssignments(encryptedCourseId: String): Result<List<ParsedAssignment>> {
        return apiService.getAssignments(encryptedCourseId)
    }
    
    /**
     * Download assignment file
     */
    suspend fun downloadAssignmentFile(downloadLink: String, destinationFile: java.io.File): Result<java.io.File> {
        // Parse FORM:token|id|filename format
        if (!downloadLink.startsWith("FORM:")) {
            return Result.failure(Exception("Invalid download link format"))
        }
        
        val parts = downloadLink.removePrefix("FORM:").split("|")
        if (parts.size != 3) {
            return Result.failure(Exception("Invalid download link parts"))
        }
        
        val (token, id, filename) = parts
        
        // Check if file already exists in cache (optional check, but good for performance)
        if (destinationFile.exists()) {
            return Result.success(destinationFile)
        }
        
        val result = apiService.downloadFile(token, id, filename)
        
        return result.mapCatching { bytes ->
            try {
                destinationFile.parentFile?.mkdirs()
                destinationFile.writeBytes(bytes)
                destinationFile
            } catch (e: Exception) {
                if (destinationFile.exists()) destinationFile.delete()
                throw e
            }
        }
    }
    
    /**
     * Submit assignment
     */
    suspend fun submitAssignment(encryptedAssignmentId: String, link: String): Result<Unit> {
        val formResult = apiService.getAssignmentForm(encryptedAssignmentId)
        val formData = formResult.getOrElse { 
            return Result.failure(it) 
        }
        
        return apiService.submitAssignment(formData, link)
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(name: String, email: String): Result<Unit> {
        return apiService.updateProfile(name, email)
    }
    
    /**
     * Change password
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return apiService.changePassword(currentPassword, newPassword)
    }
    
    // ==================== USER (LOCAL) ====================
    
    fun getCurrentUser(): Flow<UserEntity?> = userDao.getCurrentUser()
    
    suspend fun insertUser(user: UserEntity): Result<Unit> = runCatching {
        userDao.insertUser(user)
    }
    
    suspend fun deleteAllUsers(): Result<Unit> = runCatching {
        userDao.deleteAllUsers()
    }
    
    // ==================== SCHEDULE (LOCAL) ====================
    
    fun getAllSchedules(): Flow<List<ScheduleEntity>> = scheduleDao.getAllSchedules()
    
    fun getSchedulesByDay(day: String): Flow<List<ScheduleEntity>> = 
        scheduleDao.getSchedulesByDay(day)
    
    suspend fun getUpcomingSchedule(): Result<ScheduleEntity?> = runCatching {
        scheduleDao.getUpcomingSchedule()
    }
    
    suspend fun getScheduleCount(): Int = try {
        scheduleDao.getScheduleCount()
    } catch (e: Exception) {
        0
    }
    
    suspend fun getTotalSks(): Int = try {
        scheduleDao.getTotalSks() ?: 0
    } catch (e: Exception) {
        0
    }
    
    suspend fun insertSchedules(schedules: List<ScheduleEntity>): Result<Unit> = runCatching {
        scheduleDao.insertSchedules(schedules)
    }
    
    suspend fun updateAttendance(
        scheduleId: Int, 
        isAttended: Boolean, 
        attendanceTime: Long?
    ): Result<Unit> = runCatching {
        scheduleDao.updateAttendance(scheduleId, isAttended, attendanceTime)
    }
    
    suspend fun deleteAllSchedules(): Result<Unit> = runCatching {
        scheduleDao.deleteAllSchedules()
    }
    
    // ==================== NOTIFICATION (LOCAL) ====================
    
    fun getAllNotifications(): Flow<List<NotificationEntity>> = 
        notificationDao.getAllNotifications()
    
    fun getUnreadNotifications(): Flow<List<NotificationEntity>> = 
        notificationDao.getUnreadNotifications()
    
    suspend fun getUnreadCount(): Int = try {
        notificationDao.getUnreadCount()
    } catch (e: Exception) {
        0
    }
    
    suspend fun insertNotification(notification: NotificationEntity): Result<Long> = runCatching {
        notificationDao.insertNotification(notification)
    }
    
    suspend fun markAsRead(notificationId: Int): Result<Unit> = runCatching {
        notificationDao.markAsRead(notificationId)
    }
    
    suspend fun markAllAsRead(): Result<Unit> = runCatching {
        notificationDao.markAllAsRead()
    }
    
    suspend fun deleteAllNotifications(): Result<Unit> = runCatching {
        notificationDao.deleteAllNotifications()
    }
    
    // ==================== UTILITY ====================
    
    suspend fun clearAllData() {
        deleteAllUsers()
        deleteAllSchedules()
        deleteAllNotifications()
    }
}

// Extension function to convert API response to entity
fun ParsedCourse.toScheduleEntity(): ScheduleEntity {
    return ScheduleEntity(
        encryptedId = encryptedId,
        subjectName = name,
        subjectCode = kodeMtk,
        dosen = kodeDosen,
        day = day,
        startTime = jamMasuk,
        endTime = jamKeluar,
        room = noRuang,
        sks = sks,
        kelompokPraktek = kelPraktek,
        kodeGabung = kodeGabung,
        masukKelasLink = masukKelasLink,
        tugasLink = tugasLink
    )
}
