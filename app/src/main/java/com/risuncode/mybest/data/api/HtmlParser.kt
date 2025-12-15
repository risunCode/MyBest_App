package com.risuncode.mybest.data.api

import org.jsoup.Jsoup

/**
 * Utility untuk parsing HTML response dari BSI E-Learning
 */
object HtmlParser {
    
    // ========== LOGIN ==========
    
    /**
     * Extract CSRF token dari HTML
     */
    fun extractCsrfToken(html: String): String? {
        val doc = Jsoup.parse(html)
        
        // Try meta tag first
        val metaToken = doc.select("meta[name=csrf-token]").attr("content")
        if (metaToken.isNotEmpty()) return metaToken
        
        // Try hidden input
        val inputToken = doc.select("input[name=_token]").attr("value")
        if (inputToken.isNotEmpty()) return inputToken
        
        return null
    }
    
    /**
     * Extract captcha question dari HTML (format: "Berapa hasil dari X + Y?")
     * Multiple patterns with fallback for BSI page variations
     */
    fun extractCaptchaQuestion(html: String): String? {
        val patterns = listOf(
            """Berapa hasil dari\s*(\d+\s*[+\-*/×÷xX]\s*\d+)""".toRegex(RegexOption.IGNORE_CASE),
            """(\d+\s*[+\-*/×÷xX]\s*\d+)\s*\?""".toRegex()
        )
        
        for (pattern in patterns) {
            pattern.find(html)?.let { match ->
                return match.groupValues.getOrNull(1) ?: match.value
            }
        }
        
        // Fallback: find any math expression in the HTML
        val mathPattern = """(\d+)\s*([+\-*/×÷xX])\s*(\d+)""".toRegex()
        return mathPattern.find(html)?.value
    }
    
    /**
     * Solve captcha math question
     * Returns null if cannot parse (instead of 0 to avoid login issues)
     */
    fun solveCaptcha(question: String): Int? {
        val normalized = question
            .replace("×", "*")
            .replace("x", "*", ignoreCase = true)
            .replace("÷", "/")
            .replace(":", "/")
            .replace(" ", "")
        
        val match = """(\d+)\s*([+\-*/])\s*(\d+)""".toRegex().find(normalized)
            ?: return null
        
        val (_, num1, operator, num2) = match.groupValues
        val a = num1.toIntOrNull() ?: return null
        val b = num2.toIntOrNull() ?: return null
        
        return when (operator) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b != 0) a / b else null
            else -> null
        }
    }
    
    /**
     * Check if HTML is login page (session expired)
     * Stricter detection to avoid false positives
     */
    fun isLoginPage(html: String): Boolean {
        val doc = Jsoup.parse(html)
        
        // Login page MUST have username input - key differentiator
        val hasUsernameInput = doc.select("input[name=username]").isNotEmpty()
        if (!hasUsernameInput) {
            return false // Not a login page if no username field
        }
        
        // Additional checks: password or captcha must also exist
        val hasPasswordInput = doc.select("input[name=password]").isNotEmpty()
        val hasCaptcha = doc.select("input[name=captcha_answer]").isNotEmpty() || 
                         html.contains("Berapa hasil", ignoreCase = true)
        
        // Must have username + (password or captcha) to be login page
        return hasUsernameInput && (hasPasswordInput || hasCaptcha)
    }
    
    /**
     * Extract login error message
     */
    fun extractLoginError(html: String): String? {
        val doc = Jsoup.parse(html)
        val alertDanger = doc.select(".alert-danger").text()
        if (alertDanger.isNotEmpty()) return alertDanger
        
        val error = doc.select(".text-danger").text()
        if (error.isNotEmpty()) return error
        
        return null
    }
    
    /**
     * Extract error message from HTML response
     */
    fun extractErrorMessage(html: String, defaultMessage: String = "Terjadi kesalahan"): String {
        val doc = Jsoup.parse(html)
        
        // Try various error selectors
        doc.select(".alert-danger").text().takeIf { it.isNotEmpty() }?.let { return it }
        doc.select(".text-danger").text().takeIf { it.isNotEmpty() }?.let { return it }
        doc.select(".error").text().takeIf { it.isNotEmpty() }?.let { return it }
        
        return defaultMessage
    }
    
    /**
     * Extract success message from HTML response
     */
    fun extractSuccessMessage(html: String): String? {
        val doc = Jsoup.parse(html)
        
        doc.select(".alert-success").text().takeIf { it.isNotEmpty() }?.let { return it }
        doc.select(".text-success").text().takeIf { it.isNotEmpty() }?.let { return it }
        doc.select(".success").text().takeIf { it.isNotEmpty() }?.let { return it }
        
        return null
    }
    
    // ========== SCHEDULE ==========
    
    /**
     * Parse jadwal dari HTML /sch
     */
    fun parseSchedule(html: String): List<ParsedCourse> {
        val doc = Jsoup.parse(html)
        val courses = mutableListOf<ParsedCourse>()
        
        doc.select(".pricing-plan").forEach { card ->
            try {
                val name = card.select(".pricing-title").text().trim()
                val scheduleText = card.select(".pricing-save").text().trim()
                
                // Parse day and time using regex patterns
                // Formats: "Senin, 08:00 - 10:30" OR "Senin - 08:00-10:30"
                val commaRegex = """(\w+),\s*(\d{2}:\d{2})\s*-\s*(\d{2}:\d{2})""".toRegex()
                val dashRegex = """(\w+)\s*-\s*(\d{2}:\d{2})-(\d{2}:\d{2})""".toRegex()
                
                val (day, jamMasuk, jamKeluar) = when {
                    commaRegex.find(scheduleText) != null -> {
                        val match = commaRegex.find(scheduleText)!!
                        Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                    }
                    dashRegex.find(scheduleText) != null -> {
                        val match = dashRegex.find(scheduleText)!!
                        Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                    }
                    else -> {
                        // Final fallback: split method
                        val scheduleParts = scheduleText.split(" - ")
                        val timeParts = scheduleParts.getOrNull(1)?.split("-") ?: listOf("", "")
                        Triple(
                            scheduleParts.getOrNull(0)?.trim() ?: "",
                            timeParts.getOrNull(0)?.trim() ?: "",
                            timeParts.getOrNull(1)?.trim() ?: ""
                        )
                    }
                }
                
                // Parse body details using extractCourseInfo helper
                val cardBody = card.select(".card-body")
                val kodeDosen = extractCourseInfo(cardBody, "Kode Dosen")
                val kodeMtk = extractCourseInfo(cardBody, "Kode MTK")
                val sks = extractCourseInfo(cardBody, "SKS").toIntOrNull() ?: 0
                val noRuang = extractCourseInfo(cardBody, "No Ruang")
                val kelPraktek = extractCourseInfo(cardBody, "Kel Praktek")
                val kodeGabung = extractCourseInfo(cardBody, "Kode Gabung")
                
                // Parse links
                val footer = card.select(".pricing-footer")
                val masukKelasLink = footer.select("a.btn-primary").attr("href")
                    .ifEmpty { footer.select("a[href*=absen-mhs]").attr("href") }
                val diskusiLink = footer.select("a[title=Ruang Diskusi]").attr("href")
                    .ifEmpty { footer.select("a:contains(Diskusi)").attr("href") }
                val materiLink = footer.select("a[title=Ruang Materi]").attr("href")
                    .ifEmpty { footer.select("a:contains(Materi)").attr("href") }
                val tugasLink = footer.select("a[title=Ruang Tugas]").attr("href")
                    .ifEmpty { footer.select("a:contains(Tugas)").attr("href") }
                
                // Extract encrypted ID from masukKelas link
                val encryptedId = masukKelasLink.substringAfterLast("/")
                
                if (name.isNotEmpty()) {
                    courses.add(ParsedCourse(
                        encryptedId = encryptedId,
                        name = name,
                        day = day,
                        jamMasuk = jamMasuk,
                        jamKeluar = jamKeluar,
                        kodeDosen = kodeDosen,
                        kodeMtk = kodeMtk,
                        sks = sks,
                        noRuang = noRuang,
                        kelPraktek = kelPraktek,
                        kodeGabung = kodeGabung,
                        masukKelasLink = masukKelasLink,
                        diskusiLink = diskusiLink,
                        materiLink = materiLink,
                        tugasLink = tugasLink
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Sort by day order
        val dayOrder = mapOf(
            "Senin" to 1, "Selasa" to 2, "Rabu" to 3,
            "Kamis" to 4, "Jumat" to 5, "Sabtu" to 6, "Minggu" to 7
        )
        return courses.sortedBy { dayOrder[it.day] ?: 99 }
    }
    
    /**
     * Extract course info dari card body with multiple fallback strategies
     */
    private fun extractCourseInfo(cardBody: org.jsoup.select.Elements, label: String): String {
        // Strategy 1: Try .styled:contains("label")
        val styledElement = cardBody.select(".styled:contains($label)")
        if (styledElement.isNotEmpty()) {
            val text = styledElement.text()
            val match = """$label\s*:\s*(.+)""".toRegex(RegexOption.IGNORE_CASE).find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        
        // Strategy 2: Try finding in all text content
        val allText = cardBody.text()
        val pattern = """$label\s*:\s*([^\s]+)""".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(allText)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        // Strategy 3: Try finding in individual elements
        cardBody.select("*").forEach { element ->
            val text = element.ownText().trim()
            if (text.contains(label, ignoreCase = true)) {
                val valueMatch = """$label\s*:\s*(.+)""".toRegex(RegexOption.IGNORE_CASE).find(text)
                if (valueMatch != null) {
                    return valueMatch.groupValues[1].trim()
                }
            }
        }
        
        return ""
    }
    
    // ========== ATTENDANCE ==========
    
    /**
     * Extract attendance form data dari halaman presensi
     */
    fun extractAttendanceFormData(html: String): AttendanceFormData? {
        val token = extractCsrfToken(html) ?: return null
        
        // Extract encrypted pertemuan
        val pertemuanMatch = Regex("""name="pertemuan"[^>]*value="([^"]+)"""").find(html)
        val pertemuan = pertemuanMatch?.groupValues?.get(1) ?: return null
        
        return AttendanceFormData(token = token, pertemuan = pertemuan)
    }
    
    /**
     * Parse attendance records dari DataTables JSON response
     * Handles both array format and object format responses
     */
    fun parseAttendanceRecords(json: String): List<AttendanceRecord> {
        val records = mutableListOf<AttendanceRecord>()
        try {
            val gson = com.google.gson.Gson()
            val jsonObject = gson.fromJson(json, com.google.gson.JsonObject::class.java)
            val dataArray = jsonObject.getAsJsonArray("data") ?: return records
            
            dataArray.forEachIndexed { index, element ->
                try {
                    if (element.isJsonArray) {
                        // Array format: [nomer, status, date, subject, pertemuan, beritaAcara, rangkuman]
                        val arr = element.asJsonArray
                        records.add(
                            AttendanceRecord(
                                no = arr.getOrNull(0)?.asInt ?: (index + 1),
                                status = arr.getOrNull(1)?.asString ?: "",
                                date = arr.getOrNull(2)?.asString ?: "",
                                subject = arr.getOrNull(3)?.asString ?: "",
                                pertemuan = arr.getOrNull(4)?.asString ?: "",
                                beritaAcara = arr.getOrNull(5)?.asString ?: "",
                                rangkuman = arr.getOrNull(6)?.asString ?: ""
                            )
                        )
                    } else if (element.isJsonObject) {
                        // Object format with named keys
                        val item = element.asJsonObject
                        records.add(
                            AttendanceRecord(
                                no = item.get("nomer")?.asInt 
                                    ?: item.get("0")?.asInt 
                                    ?: (index + 1),
                                status = item.get("status_hadir")?.asString 
                                    ?: item.get("1")?.asString 
                                    ?: "",
                                date = item.get("tgl_ajar_masuk")?.asString 
                                    ?: item.get("2")?.asString 
                                    ?: "",
                                subject = item.get("nm_mtk")?.asString 
                                    ?: item.get("3")?.asString 
                                    ?: "",
                                pertemuan = item.get("pertemuan")?.asString 
                                    ?: item.get("4")?.asString 
                                    ?: "",
                                beritaAcara = item.get("berita_acara")?.asString 
                                    ?: item.get("5")?.asString 
                                    ?: "",
                                rangkuman = item.get("rangkuman")?.asString 
                                    ?: item.get("6")?.asString 
                                    ?: ""
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Skip invalid record
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return records
    }
    
    // Extension function for JsonArray safe access
    private fun com.google.gson.JsonArray.getOrNull(index: Int): com.google.gson.JsonElement? {
        return if (index in 0 until size()) get(index) else null
    }
    
    private val com.google.gson.JsonElement.asInt: Int
        get() = try {
            if (isJsonPrimitive) asJsonPrimitive.asInt else 0
        } catch (e: Exception) { 0 }
    
    private val com.google.gson.JsonElement.asString: String
        get() = try {
            when {
                isJsonPrimitive -> asJsonPrimitive.asString
                isJsonNull -> ""
                else -> toString()
            }
        } catch (e: Exception) { "" }
    
    // ========== ASSIGNMENTS ==========
    
    /**
     * Parse assignments dari halaman tugas
     * Matches reference implementation from MyBestUBSI
     */
    fun parseAssignments(html: String): List<ParsedAssignment> {
        val doc = Jsoup.parse(html)
        val assignments = mutableListOf<ParsedAssignment>()
        
        // Find assignment table by checking header text
        doc.select("table").forEach { table ->
            val headerText = table.select("thead th").text().lowercase()
            
            // Assignment table has "judul" and "mulai" in header
            if (headerText.contains("judul") && headerText.contains("mulai")) {
                table.select("tbody tr").forEachIndexed { index, row ->
                    try {
                        val cols = row.select("td")
                        // Reference uses >= 8 columns
                        if (cols.size >= 8) {
                            val actionCol = cols.last()
                            
                            // Extract kerjakan/submit link
                            val linkKerjakan = actionCol?.select("a[href*=/assignment/send/]")?.attr("href") ?: ""
                            
                            // Extract download form data
                            val unduhForm = actionCol?.select("form[action*=download-file-tugas]")?.first()
                            val linkUnduh = if (unduhForm != null) {
                                val token = unduhForm.select("input[name=_token]").attr("value")
                                val id = unduhForm.select("input[name=id]").attr("value")
                                val file = unduhForm.select("input[name=file]").attr("value")
                                if (token.isNotEmpty() && id.isNotEmpty() && file.isNotEmpty()) {
                                    "FORM:$token|$id|$file"
                                } else ""
                            } else ""
                            
                            // Extract pertemuan text
                            val pertemuanText = cols.getOrNull(5)?.select("center")?.text()?.trim()
                                ?: cols.getOrNull(5)?.text()?.trim() ?: ""
                            
                            // Extract submitted link
                            val submittedLink = actionCol?.select("a[href*=drive.google], a[href*=docs.google], a[target=_blank]")
                                ?.firstOrNull()?.attr("href") ?: ""
                            
                            assignments.add(ParsedAssignment(
                                no = cols.getOrNull(0)?.text()?.toIntOrNull() ?: (index + 1),
                                kodeMtk = cols.getOrNull(1)?.text() ?: "",
                                kelas = cols.getOrNull(2)?.text() ?: "",
                                judul = cols.getOrNull(3)?.text() ?: "",
                                deskripsi = cols.getOrNull(4)?.text() ?: "",
                                pertemuan = pertemuanText,
                                mulai = cols.getOrNull(6)?.text() ?: "",
                                selesai = cols.getOrNull(7)?.text() ?: "",
                                created = cols.getOrNull(8)?.text() ?: "",
                                downloadLink = linkUnduh,
                                submitLink = linkKerjakan,
                                submittedLink = submittedLink
                            ))
                        }
                    } catch (e: Exception) {
                        // Skip invalid row
                    }
                }
            }
        }
        
        return assignments
    }
    
    /**
     * Parse assignment grades dari halaman tugas
     * Looks for table with "nilai" and "komentar" in header
     */
    fun parseAssignmentGrades(html: String): List<ParsedAssignmentGrade> {
        val doc = Jsoup.parse(html)
        val grades = mutableListOf<ParsedAssignmentGrade>()
        
        doc.select("table").forEach { table ->
            val headerText = table.select("thead th").text().lowercase()
            
            // Grades table has "nilai" and "komentar" in header
            if (headerText.contains("nilai") && headerText.contains("komentar")) {
                table.select("tbody tr").forEachIndexed { index, row ->
                    try {
                        val cols = row.select("td")
                        if (cols.size >= 6) {
                            grades.add(ParsedAssignmentGrade(
                                no = cols.getOrNull(0)?.text()?.toIntOrNull() ?: (index + 1),
                                kodeMtk = cols.getOrNull(1)?.text() ?: "",
                                judul = cols.getOrNull(2)?.text() ?: "",
                                linkTugas = cols.getOrNull(3)?.text() ?: "",
                                komentarDosen = cols.getOrNull(4)?.text() ?: "",
                                nilai = cols.getOrNull(5)?.text() ?: "0",
                                created = cols.getOrNull(6)?.text() ?: "",
                                updated = cols.getOrNull(7)?.text() ?: ""
                            ))
                        }
                    } catch (e: Exception) {
                        // Skip invalid row
                    }
                }
            }
        }
        
        return grades
    }
    
    /**
     * Extract assignment submit form data
     */
    fun extractAssignmentFormData(html: String): AssignmentFormData? {
        val doc = Jsoup.parse(html)
        
        val token = extractCsrfToken(html) ?: return null
        val kdMtk = doc.select("input[name=kd_mtk]").attr("value")
        val idTugas = doc.select("input[name=id_tugas]").attr("value")
        val nim = doc.select("input[name=nim]").attr("value")
        val kdLokal = doc.select("input[name=kd_lokal]").attr("value")
        
        if (kdMtk.isEmpty() || idTugas.isEmpty()) return null
        
        return AssignmentFormData(
            token = token,
            kdMtk = kdMtk,
            idTugas = idTugas,
            nim = nim,
            kdLokal = kdLokal
        )
    }
    
    // ========== PROFILE ==========
    
    /**
     * Parse profile dari halaman profil
     */
    fun parseProfile(html: String): ParsedProfile? {
        val doc = Jsoup.parse(html)
        
        val name = doc.select("input[name=name]").attr("value")
        val email = doc.select("input[name=email]").attr("value")
        
        if (name.isEmpty()) return null
        
        return ParsedProfile(name = name, email = email)
    }
}

// ========== DATA CLASSES ==========

data class ParsedCourse(
    val encryptedId: String,
    val name: String,
    val day: String,
    val jamMasuk: String,
    val jamKeluar: String,
    val kodeDosen: String,
    val kodeMtk: String,
    val sks: Int,
    val noRuang: String,
    val kelPraktek: String,
    val kodeGabung: String,
    val masukKelasLink: String,
    val diskusiLink: String,
    val materiLink: String,
    val tugasLink: String
)

data class AttendanceFormData(
    val token: String,
    val pertemuan: String
)

data class AttendanceRecord(
    val no: Int,
    val status: String,
    val date: String,
    val subject: String,
    val pertemuan: String,
    val beritaAcara: String,
    val rangkuman: String
)

data class DataTablesResponse(
    val draw: Int,
    val recordsTotal: Int,
    val recordsFiltered: Int,
    val data: List<List<Any>>
)

data class ParsedAssignment(
    val no: Int,
    val kodeMtk: String,
    val kelas: String,
    val judul: String,
    val deskripsi: String,
    val pertemuan: String,
    val mulai: String,
    val selesai: String,
    val created: String,
    val downloadLink: String,
    val submitLink: String,
    val submittedLink: String
)

data class AssignmentFormData(
    val token: String,
    val kdMtk: String,
    val idTugas: String,
    val nim: String,
    val kdLokal: String
)

data class ParsedProfile(
    val name: String,
    val email: String
)

data class ParsedAssignmentGrade(
    val no: Int,
    val kodeMtk: String,
    val judul: String,
    val linkTugas: String,
    val komentarDosen: String,
    val nilai: String,
    val created: String,
    val updated: String
)
