package com.risuncode.mybest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey
    val nim: String,
    
    val name: String,
    val email: String,
    val prodi: String,
    val angkatan: String,
    
    val isGuestMode: Boolean = false,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
