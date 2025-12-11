package com.risuncode.mybest.data.dao

import androidx.room.*
import com.risuncode.mybest.data.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: Int): NotificationEntity?
    
    @Query("SELECT * FROM notifications WHERE scheduleId = :scheduleId ORDER BY timestamp DESC")
    fun getNotificationsBySchedule(scheduleId: Int): Flow<List<NotificationEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long
    
    @Update
    suspend fun updateNotification(notification: NotificationEntity)
    
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Int)
    
    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
    
    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)
    
    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    suspend fun getUnreadCount(): Int
}
