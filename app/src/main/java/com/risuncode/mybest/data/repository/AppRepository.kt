package com.risuncode.mybest.data.repository

import com.risuncode.mybest.data.AppDatabase
import com.risuncode.mybest.data.entity.NotificationEntity
import com.risuncode.mybest.data.entity.ScheduleEntity
import com.risuncode.mybest.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository sebagai single source of truth untuk akses data.
 * Menggabungkan akses ke Room Database dan operasi data lainnya.
 */
class AppRepository(private val database: AppDatabase) {
    
    private val userDao = database.userDao()
    private val scheduleDao = database.scheduleDao()
    private val notificationDao = database.notificationDao()
    
    // ==================== USER ====================
    
    fun getCurrentUser(): Flow<UserEntity?> = userDao.getCurrentUser()
    
    suspend fun insertUser(user: UserEntity): Result<Unit> = runCatching {
        userDao.insertUser(user)
    }
    
    suspend fun deleteAllUsers(): Result<Unit> = runCatching {
        userDao.deleteAllUsers()
    }
    
    // ==================== SCHEDULE ====================
    
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
    
    // ==================== NOTIFICATION ====================
    
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
