package com.risuncode.mybest.data.dao

import androidx.room.*
import com.risuncode.mybest.data.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    
    @Query("SELECT * FROM schedules ORDER BY day, startTime")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>
    
    @Query("SELECT * FROM schedules WHERE day = :day ORDER BY startTime")
    fun getSchedulesByDay(day: String): Flow<List<ScheduleEntity>>
    
    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Int): ScheduleEntity?
    
    @Query("SELECT * FROM schedules WHERE isAttended = 0 ORDER BY day, startTime LIMIT 1")
    suspend fun getUpcomingSchedule(): ScheduleEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ScheduleEntity>)
    
    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)
    
    @Query("UPDATE schedules SET isAttended = :isAttended, attendanceTime = :attendanceTime WHERE id = :scheduleId")
    suspend fun updateAttendance(scheduleId: Int, isAttended: Boolean, attendanceTime: Long?)
    
    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)
    
    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()
    
    @Query("SELECT COUNT(*) FROM schedules")
    suspend fun getScheduleCount(): Int
    
    @Query("SELECT SUM(sks) FROM schedules")
    suspend fun getTotalSks(): Int?
}
