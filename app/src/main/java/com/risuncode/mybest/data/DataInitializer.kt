package com.risuncode.mybest.data

import android.content.Context
import com.risuncode.mybest.data.repository.AppRepository
import com.risuncode.mybest.util.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Initializer untuk data aplikasi.
 * Mengelola inisialisasi data guest mode dan sinkronisasi data dari server.
 */
object DataInitializer {
    
    // Application-scoped coroutine scope dengan SupervisorJob
    // untuk mencegah cancellation cascade
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Inisialisasi data untuk guest mode.
     * Membuat user dummy, jadwal, dan notifikasi contoh.
     */
    fun initializeGuestData(context: Context) {
        val prefManager = PreferenceManager(context)
        val database = AppDatabase.getDatabase(context)
        val repository = AppRepository(database)
        
        applicationScope.launch {
            try {
                val scheduleCount = repository.getScheduleCount()
                
                if (scheduleCount == 0) {
                    // Generate dan simpan user guest
                    val guestUser = DummyDataGenerator.generateGuestUser()
                    repository.insertUser(guestUser)
                    
                    // Update preferences
                    prefManager.isGuestMode = true
                    prefManager.userName = guestUser.name
                    prefManager.savedNim = guestUser.nim
                    
                    // Generate dan simpan jadwal dummy
                    val dummySchedules = DummyDataGenerator.generateDummySchedules()
                    repository.insertSchedules(dummySchedules)
                    
                    // Generate dan simpan notifikasi dummy
                    val dummyNotifications = DummyDataGenerator.generateDummyNotifications(dummySchedules)
                    dummyNotifications.forEach { notification ->
                        repository.insertNotification(notification)
                    }
                }
            } catch (e: Exception) {
                // Log error, tapi jangan crash aplikasi
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Hapus semua data guest mode.
     */
    fun clearGuestData(context: Context) {
        val prefManager = PreferenceManager(context)
        val database = AppDatabase.getDatabase(context)
        val repository = AppRepository(database)
        
        applicationScope.launch {
            try {
                repository.clearAllData()
                prefManager.clearAllData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Sinkronisasi data dari server BSI.
     * Dipanggil setelah login berhasil.
     */
    suspend fun syncDataFromServer(context: Context, realData: Map<String, Any>): Result<Unit> {
        val prefManager = PreferenceManager(context)
        val database = AppDatabase.getDatabase(context)
        val repository = AppRepository(database)
        
        return try {
            repository.clearAllData()
            
            // TODO: Parse dan simpan data dari server
            // val schedules = parseSchedules(realData)
            // repository.insertSchedules(schedules)
            
            prefManager.isGuestMode = false
            prefManager.lastSyncTime = System.currentTimeMillis()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
