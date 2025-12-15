package com.risuncode.mybest.util

import java.util.Calendar

/**
 * Utility class for date and time operations
 */
object DateUtils {
    
    /**
     * Get current day name in Indonesian (Senin, Selasa, etc.)
     */
    fun getCurrentDayInIndonesian(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            Calendar.SUNDAY -> "Minggu"
            else -> "Senin"
        }
    }
}
