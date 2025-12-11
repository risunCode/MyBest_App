package com.risuncode.mybest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val encryptedId: String = "", // BSI API encrypted course ID
    val subjectName: String,
    val subjectCode: String,
    val dosen: String,
    val day: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val sks: Int,
    val kelompokPraktek: String,
    val kodeGabung: String,
    
    // Links for navigation
    val masukKelasLink: String = "",
    val tugasLink: String = "",
    
    val isAttended: Boolean = false,
    val attendanceTime: Long? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

