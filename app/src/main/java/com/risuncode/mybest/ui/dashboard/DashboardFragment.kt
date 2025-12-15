package com.risuncode.mybest.ui.dashboard

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.risuncode.mybest.R
import com.risuncode.mybest.data.AppDatabase
import com.risuncode.mybest.data.entity.ScheduleEntity
import com.risuncode.mybest.data.repository.AppRepository
import com.risuncode.mybest.databinding.FragmentDashboardBinding
import com.risuncode.mybest.ui.presensi.PresensiActivity
import com.risuncode.mybest.util.DateUtils
import com.risuncode.mybest.util.PreferenceManager
import com.risuncode.mybest.util.StringUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var prefManager: PreferenceManager
    private lateinit var repository: AppRepository
    
    // Prevent redundant API calls when switching tabs
    private var isDataLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefManager = PreferenceManager(requireContext())
        val database = AppDatabase.getDatabase(requireContext())
        repository = AppRepository(database)
        
        setupSwipeRefresh()
        
        // Only load data once - skip on tab switches
        if (!isDataLoaded) {
            checkSessionAndLoad()
        }
    }
    
    /**
     * Check if session is valid. If expired, perform auto-relogin and reload data.
     * If valid, just load data normally.
     */
    private fun checkSessionAndLoad() {
        viewLifecycleOwner.lifecycleScope.launch {
            // First, display cached data immediately (if available)
            setupUI()
            
            // Check cache validity (1 minute)
            val lastSync = prefManager.lastSyncTime
            val now = System.currentTimeMillis()
            val CACHE_DURATION = 60 * 1000 // 1 minute
            
            // If data is fresh enough, skip network check
            if (now - lastSync < CACHE_DURATION) {
                // Mark as loaded
                isDataLoaded = true
                return@launch
            }
            
            // Then check session in background
            val isSessionValid = repository.checkSession()
            
            if (!isSessionValid) {
                // Session expired - try auto-relogin
                val nim = prefManager.savedNim
                val password = prefManager.savedPassword
                
                if (nim.isNotEmpty() && password.isNotEmpty()) {
                    // Perform auto-relogin silently
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Sesi berakhir, melakukan auto-login...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    
                    val loginResult = repository.performLogin(nim, password)
                    
                    loginResult.onSuccess {
                        // Refresh data after successful relogin
                        repository.syncScheduleFromServer()
                        prefManager.lastSyncTime = System.currentTimeMillis() // Update access time
                        setupUI()
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Auto-login berhasil",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure { error ->
                        // Auto-login failed - notify user
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Auto-login gagal: ${error.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                // Session valid - update access time
                prefManager.lastSyncTime = System.currentTimeMillis()
            }
            
            // Mark as loaded to prevent redundant calls on tab switch
            isDataLoaded = true
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }



    private fun refreshData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Sync from server using swipe refresh indicator
            val result = repository.syncScheduleFromServer()
            result.onFailure { error ->
                android.widget.Toast.makeText(
                    requireContext(),
                    error.message ?: "Gagal sinkronisasi",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            
            setupUI()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupUI() {
        setupGreeting()
        loadUserData()
        loadAppStatus()
        loadTodayClasses()
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 0..10 -> R.string.greeting_morning
            in 11..14 -> R.string.greeting_afternoon
            in 15..18 -> R.string.greeting_evening
            else -> R.string.greeting_night
        }
        binding.tvGreeting.setText(greetingRes)
    }

    private fun loadUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = repository.getCurrentUser().firstOrNull()
            
            if (user != null) {
                val initials = StringUtils.getInitials(user.name)
                binding.tvAvatar.text = initials
                binding.tvStudentName.text = user.name
                binding.tvNim.text = StringUtils.formatNim(user.nim)
            } else {
                val name = prefManager.userName.ifEmpty { getString(R.string.guest_user) }
                val nim = prefManager.savedNim.ifEmpty { getString(R.string.default_nim) }
                
                val initials = StringUtils.getInitials(name)
                binding.tvAvatar.text = initials
                binding.tvStudentName.text = name
                binding.tvNim.text = StringUtils.formatNim(nim)
            }
            
            // Random quote from array
            loadRandomQuote()
        }
    }

    private fun loadRandomQuote() {
        val quotes = resources.getStringArray(R.array.quotes)
        val randomQuote = quotes.random()
        val parts = randomQuote.split("|")
        
        if (parts.size == 2) {
            binding.tvQuote.text = parts[0]
            binding.tvQuoteAuthor.text = "— ${parts[1]}"
        } else {
            binding.tvQuote.setText(R.string.quote_education)
            binding.tvQuoteAuthor.setText(R.string.quote_author)
        }
    }

    private fun loadAppStatus() {
        // Login Status - check if we have valid user data
        val isLoggedIn = prefManager.isLoggedIn
        binding.tvLoginStatus.text = if (isLoggedIn) getString(R.string.status_valid) else getString(R.string.status_invalid)
        binding.tvLoginStatus.setBackgroundResource(R.drawable.bg_status_badge)
        binding.tvLoginStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), if (isLoggedIn) R.color.status_success else R.color.status_error)
        )

        // Web Connected - check actual network connectivity
        val isWebConnected = isNetworkAvailable()
        binding.tvWebStatus.text = if (isWebConnected) getString(R.string.status_yes) else getString(R.string.status_no)
        binding.tvWebStatus.setBackgroundResource(R.drawable.bg_status_badge)
        binding.tvWebStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), if (isWebConnected) R.color.status_success else R.color.status_error)
        )

        // Holiday Detection - check schedule data
        viewLifecycleOwner.lifecycleScope.launch {
            val isHoliday = checkIfHoliday()
            binding.tvHolidayStatus.text = if (isHoliday) getString(R.string.status_yes) else getString(R.string.status_no)
            binding.tvHolidayStatus.setBackgroundResource(R.drawable.bg_status_badge)
            binding.tvHolidayStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), if (isHoliday) R.color.status_warning else R.color.status_success)
            )
        }
    }

    private suspend fun checkIfHoliday(): Boolean {
        // Check if schedule day names are lowercase (indicates holiday/no class)
        // Normal: "Senin", "Selasa" (capitalized) = masuk
        // Holiday: "senin", "selasa" (lowercase) = kemungkinan libur
        val allSchedules = repository.getAllSchedules().firstOrNull() ?: return false
        
        if (allSchedules.isEmpty()) return false
        
        // Check first schedule's day - if first char is lowercase, likely holiday
        val firstSchedule = allSchedules.firstOrNull() ?: return false
        val dayName = firstSchedule.day
        
        return dayName.isNotEmpty() && dayName[0].isLowerCase()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loadTodayClasses() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllSchedules().collect { allSchedules ->
                val todaySchedules = getTodaySchedules(allSchedules)
                
                binding.todayClassesContainer.removeAllViews()
                
                if (todaySchedules.isEmpty()) {
                    binding.cardNoClasses.visibility = View.VISIBLE
                } else {
                    binding.cardNoClasses.visibility = View.GONE
                    
                    todaySchedules.forEachIndexed { index, schedule ->
                        val cardView = createScheduleCard(schedule)
                        
                        // Add margin between cards
                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        if (index > 0) {
                            layoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.spacing_sm)
                        }
                        cardView.layoutParams = layoutParams
                        
                        binding.todayClassesContainer.addView(cardView)
                    }
                }
                
                // Set tips content
                binding.tvTipsContent.text = if (todaySchedules.isEmpty()) {
                    getString(R.string.tip_no_class_today)
                } else {
                    getString(R.string.tip_class_count, todaySchedules.size)
                }
            }
        }
    }

    private fun createScheduleCard(schedule: ScheduleEntity): View {
        val cardView = layoutInflater.inflate(R.layout.item_dashboard_schedule, binding.todayClassesContainer, false)
        
        cardView.findViewById<TextView>(R.id.tvSubjectName)?.text = schedule.subjectName
        cardView.findViewById<TextView>(R.id.tvTime)?.text = "${schedule.startTime} - ${schedule.endTime}"
        cardView.findViewById<TextView>(R.id.tvRoom)?.text = schedule.room
        
        // Set color indicator based on schedule status
        val colorRes = getScheduleColorRes(schedule)
        cardView.findViewById<View>(R.id.colorIndicator)?.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
        
        cardView.setOnClickListener {
            PresensiActivity.start(requireContext(), schedule.id)
        }
        
        return cardView
    }

    private fun getScheduleColorRes(schedule: ScheduleEntity): Int {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        val dayMap = mapOf(
            "Senin" to Calendar.MONDAY,
            "Selasa" to Calendar.TUESDAY,
            "Rabu" to Calendar.WEDNESDAY,
            "Kamis" to Calendar.THURSDAY,
            "Jumat" to Calendar.FRIDAY,
            "Sabtu" to Calendar.SATURDAY,
            "Minggu" to Calendar.SUNDAY
        )
        
        val scheduleDay = dayMap[schedule.day] ?: Calendar.MONDAY
        
        // Parse end time
        val endTimeParts = schedule.endTime.split(":")
        val endHour = endTimeParts.getOrNull(0)?.toIntOrNull() ?: 23
        val endMinute = endTimeParts.getOrNull(1)?.toIntOrNull() ?: 59
        
        return when {
            scheduleDay != currentDay -> R.color.schedule_future
            currentHour > endHour || (currentHour == endHour && currentMinute > endMinute) -> R.color.schedule_passed
            else -> R.color.schedule_active
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun getTodaySchedules(schedules: List<ScheduleEntity>): List<ScheduleEntity> {
        val today = DateUtils.getCurrentDayInIndonesian()
        return schedules.filter { it.day.equals(today, ignoreCase = true) }
    }
    

}

