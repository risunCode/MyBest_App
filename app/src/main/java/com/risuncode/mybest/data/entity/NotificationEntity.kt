package com.risuncode.mybest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val title: String,
    val message: String,
    val type: String,
    
    val scheduleId: Int? = null,
    val isRead: Boolean = false,
    
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
