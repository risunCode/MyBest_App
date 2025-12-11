package com.risuncode.mybest.data.dao

import androidx.room.*
import com.risuncode.mybest.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Query("SELECT * FROM user LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>
    
    @Query("SELECT * FROM user WHERE nim = :nim")
    suspend fun getUserByNim(nim: String): UserEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("DELETE FROM user")
    suspend fun deleteAllUsers()
}
