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
     */
    fun extractCaptchaQuestion(html: String): String? {
        val regex = """Berapa hasil dari\s*(\d+\s*[+\-*/×÷xX]\s*\d+)""".toRegex()
        return regex.find(html)?.groupValues?.get(1)
    }
    
    /**
     * Solve captcha math question
     */
    fun solveCaptcha(question: String): Int {
        val cleanQuestion = question.replace(" ", "")
        
        // Find operator
        val operatorMatch = Regex("[+\\-*/×÷xX]").find(cleanQuestion)
        val operator = operatorMatch?.value ?: return 0
        
        val parts = cleanQuestion.split(Regex("[+\\-*/×÷xX]"))
        if (parts.size != 2) return 0
        
        val a = parts[0].toIntOrNull() ?: return 0
        val b = parts[1].toIntOrNull() ?: return 0
        
        return when (operator.lowercase()) {
            "+" -> a + b
            "-" -> a - b
            "*", "×", "x" -> a * b
            "/", "÷" -> if (b != 0) a / b else 0
            else -> 0
        }
    }
    
    /**
     * Check if HTML is login page
     */
    fun isLoginPage(html: String): Boolean {
        val doc = Jsoup.parse(html)
        val hasUsernameInput = doc.select("input[name=username]").isNotEmpty()
        val hasCaptcha = html.contains("Berapa hasil", ignoreCase = true)
        return hasUsernameInput || hasCaptcha
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
    
    // ========== SCHEDULE ==========
    
    /**
     * Parse jadwal dari HTML /sch
     */
    fun parseSchedule(html: String): List<ParsedCourse> {
        val doc = Jsoup.parse(html)
        val courses = mutableListOf<ParsedCourse>()
        
        doc.select(".pricing-plan").forEach { card ->
            try {
                val name = card.select(".pricing-title").text()
                val scheduleText = card.select(".pricing-save").text() // "Senin - 08:00-10:30"
                
                // Parse day and time
                val scheduleParts = scheduleText.split(" - ")
                val day = scheduleParts.getOrNull(0) ?: ""
                val timeParts = scheduleParts.getOrNull(1)?.split("-") ?: listOf("", "")
                val jamMasuk = timeParts.getOrNull(0)?.trim() ?: ""
                val jamKeluar = timeParts.getOrNull(1)?.trim() ?: ""
                
                // Parse body details
                val styledItems = card.select(".styled")
                var kodeDosen = ""
                var kodeMtk = ""
                var sks = 0
                var noRuang = ""
                var kelPraktek = ""
                var kodeGabung = ""
                
                styledItems.forEach { item ->
                    val text = item.text()
                    when {
                        text.contains("Kode Dosen:", ignoreCase = true) -> 
                            kodeDosen = text.substringAfter(":").trim()
                        text.contains("Kode MTK:", ignoreCase = true) -> 
                            kodeMtk = text.substringAfter(":").trim()
                        text.contains("SKS:", ignoreCase = true) -> 
                            sks = text.substringAfter(":").trim().toIntOrNull() ?: 0
                        text.contains("No Ruang:", ignoreCase = true) -> 
                            noRuang = text.substringAfter(":").trim()
                        text.contains("Kel Praktek:", ignoreCase = true) -> 
                            kelPraktek = text.substringAfter(":").trim()
                        text.contains("Kode Gabung:", ignoreCase = true) -> 
                            kodeGabung = text.substringAfter(":").trim()
                    }
                }
                
                // Parse links
                val footer = card.select(".pricing-footer")
                val masukKelasLink = footer.select("a.btn-primary").attr("href")
                val diskusiLink = footer.select("a:contains(Diskusi)").attr("href")
                val materiLink = footer.select("a:contains(Materi)").attr("href")
                val tugasLink = footer.select("a:contains(Tugas)").attr("href")
                
                // Extract encrypted ID from masukKelas link
                val encryptedId = masukKelasLink.substringAfterLast("/")
                
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return courses
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
     */
    fun parseAttendanceRecords(json: String): List<AttendanceRecord> {
        try {
            val response = com.google.gson.Gson().fromJson(json, DataTablesResponse::class.java)
            return response.data.mapIndexed { index, row ->
                AttendanceRecord(
                    no = (row.getOrNull(0) as? Double)?.toInt() ?: (index + 1),
                    status = row.getOrNull(1)?.toString() ?: "",
                    date = row.getOrNull(2)?.toString() ?: "",
                    subject = row.getOrNull(3)?.toString() ?: "",
                    pertemuan = (row.getOrNull(4) as? Double)?.toInt()?.toString() 
                        ?: row.getOrNull(4)?.toString() ?: "",
                    beritaAcara = row.getOrNull(5)?.toString() ?: "",
                    rangkuman = row.getOrNull(6)?.toString() ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
    
    // ========== ASSIGNMENTS ==========
    
    /**
     * Parse assignments dari halaman tugas
     */
    fun parseAssignments(html: String): List<ParsedAssignment> {
        val doc = Jsoup.parse(html)
        val assignments = mutableListOf<ParsedAssignment>()
        
        // Find assignment table (first table usually is "Data Penugasan")
        val tables = doc.select("table")
        val assignmentTable = tables.firstOrNull() ?: return assignments
        
        assignmentTable.select("tbody tr").forEach { row ->
            try {
                val cells = row.select("td")
                if (cells.size >= 10) {
                    val no = cells[0].text().toIntOrNull() ?: 0
                    val kodeMtk = cells[1].text()
                    val kelas = cells[2].text()
                    val judul = cells[3].text()
                    val deskripsi = cells[4].text()
                    val pertemuan = cells[5].text()
                    val mulai = cells[6].text()
                    val selesai = cells[7].text()
                    val created = cells[8].text()
                    
                    // Parse action cell for download and submit links
                    val actionCell = cells[9]
                    
                    // Download form
                    val downloadForm = actionCell.select("form[action*=download-file-tugas]")
                    var downloadLink = ""
                    if (downloadForm.isNotEmpty()) {
                        val token = downloadForm.select("input[name=_token]").attr("value")
                        val id = downloadForm.select("input[name=id]").attr("value")
                        val file = downloadForm.select("input[name=file]").attr("value")
                        downloadLink = "FORM:$token|$id|$file"
                    }
                    
                    // Submit link
                    val submitLink = actionCell.select("a[href*=assignment/send]").attr("href")
                    
                    // Already submitted link
                    val submittedLink = actionCell.select("a[href*=drive.google], a[target=_blank]").attr("href")
                    
                    assignments.add(ParsedAssignment(
                        no = no,
                        kodeMtk = kodeMtk,
                        kelas = kelas,
                        judul = judul,
                        deskripsi = deskripsi,
                        pertemuan = pertemuan,
                        mulai = mulai,
                        selesai = selesai,
                        created = created,
                        downloadLink = downloadLink,
                        submitLink = submitLink,
                        submittedLink = submittedLink
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return assignments
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
