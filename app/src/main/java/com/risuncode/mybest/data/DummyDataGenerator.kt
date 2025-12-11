package com.risuncode.mybest.data

import com.risuncode.mybest.data.entity.NotificationEntity
import com.risuncode.mybest.data.entity.ScheduleEntity
import com.risuncode.mybest.data.entity.UserEntity
import java.util.Calendar
import kotlin.random.Random

object DummyDataGenerator {
    
    // Pool nama mahasiswa
    private val firstNames = listOf(
        "Ahmad", "Budi", "Citra", "Dewi", "Eko", "Fitri", "Galih", "Hana",
        "Irfan", "Joko", "Kartika", "Lina", "Maya", "Nanda", "Oki", "Putri",
        "Rizky", "Sari", "Tono", "Umi", "Vina", "Wahyu", "Yanti", "Zahra"
    )
    
    private val lastNames = listOf(
        "Santoso", "Wijaya", "Pratama", "Kusuma", "Hidayat", "Saputra", "Rahayu",
        "Wibowo", "Nugroho", "Purnama", "Setiawan", "Hartono", "Susanto", "Gunawan"
    )
    
    // Pool program studi
    private val prodiList = listOf(
        "Teknik Informatika",
        "Sistem Informasi", 
        "Manajemen Informatika",
        "Komputer Akuntansi",
        "Administrasi Bisnis",
        "Sekretari"
    )
    
    // Pool mata kuliah dengan SKS
    private val subjectPool = listOf(
        Pair("Pemrograman Web", 3),
        Pair("Basis Data", 3),
        Pair("Algoritma dan Struktur Data", 4),
        Pair("Jaringan Komputer", 3),
        Pair("Sistem Operasi", 3),
        Pair("Pemrograman Berorientasi Objek", 4),
        Pair("Matematika Diskrit", 2),
        Pair("Statistika", 2),
        Pair("Bahasa Inggris", 2),
        Pair("Pendidikan Kewarganegaraan", 2),
        Pair("Kalkulus", 3),
        Pair("Fisika Dasar", 2),
        Pair("Etika Profesi", 2),
        Pair("Manajemen Proyek", 3),
        Pair("Rekayasa Perangkat Lunak", 4),
        Pair("Keamanan Sistem Informasi", 3),
        Pair("Mobile Programming", 4),
        Pair("Cloud Computing", 3),
        Pair("Kecerdasan Buatan", 3),
        Pair("Data Mining", 3)
    )
    
    // Pool dosen (kode 3 huruf)
    private val dosenCodes = listOf(
        "STZ", "RRI", "DWI", "FRD", "AGS", "BDY", "CKR", "DNI",
        "EKO", "FTR", "GNW", "HRY", "IRW", "JKO", "KRT", "LNA"
    )
    
    // Pool ruangan
    private val roomPool = listOf(
        "EL1-P1", "EL2-P1", "EL3-P1", "EL4-P1",
        "LA1-P2", "LA2-P2", "LA3-P2",
        "LB1-P3", "LB2-P3", "LB3-P3",
        "R301", "R302", "R303", "R401", "R402"
    )
    
    private val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat")
    
    fun generateGuestUser(): UserEntity {
        val firstName = firstNames.random()
        val lastName = lastNames.random()
        val fullName = "$firstName $lastName"
        
        val randomNim = "4${Random.nextInt(1, 5)}22${Random.nextInt(10000000, 99999999)}"
        val email = "${firstName.lowercase()}.${lastName.lowercase()}@students.ubsi.ac.id"
        val angkatan = listOf("2020", "2021", "2022", "2023", "2024").random()
        
        return UserEntity(
            nim = randomNim,
            name = fullName.uppercase(),
            email = email,
            prodi = prodiList.random(),
            angkatan = angkatan,
            isGuestMode = true
        )
    }
    
    fun generateDummySchedules(): List<ScheduleEntity> {
        val schedules = mutableListOf<ScheduleEntity>()
        
        // Pilih 4-6 mata kuliah secara random
        val numberOfSubjects = Random.nextInt(4, 7)
        val selectedSubjects = subjectPool.shuffled().take(numberOfSubjects)
        
        selectedSubjects.forEachIndexed { index, (name, sks) ->
            // Generate kode mata kuliah random
            val subjectCode = "${Random.nextInt(100, 999)}"
            
            // Generate waktu kuliah
            val startHour = listOf(7, 8, 9, 10, 13, 14, 15, 16).random()
            val duration = if (sks >= 4) 3 else 2
            val endHour = startHour + duration
            
            // Generate kelompok praktek
            val kelPraktek = listOf("A", "B", "C", "D", "-").random()
            
            // Generate kode gabung
            val kodeGabung = if (Random.nextBoolean()) {
                "KG.${subjectCode}.${Random.nextInt(10, 40)}.$kelPraktek"
            } else {
                "-"
            }
            
            schedules.add(
                ScheduleEntity(
                    subjectName = name.uppercase(),
                    subjectCode = subjectCode,
                    dosen = dosenCodes.random(),
                    day = days[index % days.size],
                    startTime = String.format("%02d:20", startHour),
                    endTime = String.format("%02d:00", endHour),
                    room = roomPool.random(),
                    sks = sks,
                    kelompokPraktek = kelPraktek,
                    kodeGabung = kodeGabung
                )
            )
        }
        
        return schedules
    }
    
    fun generateDummyNotifications(schedules: List<ScheduleEntity>): List<NotificationEntity> {
        val notifications = mutableListOf<NotificationEntity>()
        val now = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000L
        
        // Notifikasi selamat datang
        notifications.add(
            NotificationEntity(
                title = "Selamat Datang!",
                message = "Kamu menggunakan mode Guest. Login untuk sinkronisasi data jadwal asli.",
                type = "welcome",
                timestamp = now - (oneHour * 24)
            )
        )
        
        // Notifikasi jadwal dari data random
        schedules.take(2).forEachIndexed { index, schedule ->
            notifications.add(
                NotificationEntity(
                    title = "Jadwal Kuliah",
                    message = "${schedule.subjectName} dimulai pukul ${schedule.startTime} di ${schedule.room}",
                    type = "schedule",
                    scheduleId = schedule.id,
                    timestamp = now - (oneHour * (index + 1)),
                    isRead = index > 0
                )
            )
        }
        
        // Notifikasi pengingat
        schedules.firstOrNull()?.let { firstSchedule ->
            notifications.add(
                NotificationEntity(
                    title = "Pengingat Presensi",
                    message = "Jangan lupa melakukan presensi untuk ${firstSchedule.subjectName}",
                    type = "reminder",
                    scheduleId = firstSchedule.id,
                    timestamp = now - (oneHour * 5)
                )
            )
        }
        
        return notifications
    }
    
    fun getCurrentDayInIndonesian(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Senin"
        }
    }
    
    fun getTodaySchedules(allSchedules: List<ScheduleEntity>): List<ScheduleEntity> {
        val today = getCurrentDayInIndonesian()
        return allSchedules.filter { it.day == today }
    }
}
