package com.risuncode.mybest.util

/**
 * Utility class for string operations
 */
object StringUtils {
    
    /**
     * Extract initials from a name (e.g., "John Doe" -> "JD")
     */
    fun getInitials(name: String): String {
        val words = name.trim().split(" ")
        return when {
            words.size >= 2 -> "${words[0].first()}${words[1].first()}"
            words.isNotEmpty() -> words[0].take(2).uppercase()
            else -> "?"
        }
    }
    
    /**
     * Format NIM with prefix (e.g., "12345678" -> "NIM: 12345678")
     */
    fun formatNim(nim: String): String {
        return "NIM: $nim"
    }
}
